package cn.tricheer.canbox.client;

import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import javax.net.SocketFactory;


public class SocketClientThread extends Thread implements SocketCloseInterface {
    private static final String TAG = "SocketClientThread";

    private volatile String name;

    private boolean isLongConnection = true;
    private boolean isReConnect = true;
    private SocketSendThread mSocketSendThread;
    private SocketReceiveThread mSocketReceiveThread;
    private SocketHeartBeater mSocketHeartBeat;
    private Socket mSocket;

    private boolean isSocketAvailable;

    private SocketClientResponseInterface socketClientResponseInterface;
    private ReceiveThread receiveThread;

    public SocketClientThread(String name, SocketClientResponseInterface socketClientResponseInterface) {
        this.name = name;
        this.socketClientResponseInterface = socketClientResponseInterface;
    }

    @Override
    public void run() {
        if(Looper.myLooper()==null){
            Looper.prepare();
        }
        final Thread currentThread = Thread.currentThread();
        final String oldName = currentThread.getName();
        currentThread.setName("Processing-" + name);
        try {
            initSocket();
            Log.i(TAG, "run: SocketClientThread end");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            currentThread.setName(oldName);
        }
        Looper.loop();
    }


    /**
     * 判断本地socket连接状态
     */
    private boolean isConnected() {
        if (mSocket.isClosed() || !mSocket.isConnected()) {
            Log.d(TAG,"socket closed...");
            return false;
        }
        return true;
    }


    /**
     * 数据接收线程
     */
    public class ReceiveThread extends Thread {

        private DataInputStream dataInputStream;
        private boolean isCancel;

        @Override
        public void run() {
            try {
                while (!isCancel) {
                    if (!isConnected()) {
                        isCancel = true;
                        break;
                    }
                    byte[] bytes = new byte[1]; // 一次读取一个byte

                    while (dataInputStream.read(bytes) != -1) {
                        String ret = "";
                        ret += SocketUtil.bytesToHexString(bytes);
                        Log.d(TAG,"dataInputStream  isclose: " + dataInputStream + "  ret:  " + ret);
                        if (dataInputStream.available() == 0) { //一个请求
                            Log.d(TAG,"收到服务端数据： ret: " + ret);
                            if(ret.equals("00")||ret.equals("01")){
                                onSocketReceive(ret);
                            }
                        }
                    }

                }

                Log.d(TAG,"ReceiveThread is finish");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 初始化socket客户端
     */
    private void initSocket() {
        try {
            mSocket = SocketFactory.getDefault().createSocket();
            SocketAddress socketAddress = new InetSocketAddress(SocketUtil.getIP(), SocketUtil.PORT);
            mSocket.connect(socketAddress);

            isSocketAvailable = true;


            //开启接收线程
            receiveThread = new ReceiveThread();

            // 装饰流BufferedReader封装输入流（接收服务端的流）
            BufferedInputStream bis = new BufferedInputStream(
                    mSocket.getInputStream());

            DataInputStream dis = new DataInputStream(bis);
            receiveThread.dataInputStream = dis;
            receiveThread.start();


            //开启发送线程 发送CAN数据包
            OutputStream outputStream = mSocket.getOutputStream();
            Log.i(TAG, "initSocket: " + outputStream);
            mSocketSendThread = new SocketSendThread("SocketSendThread", outputStream);
            mSocketSendThread.setCloseSendTask(false);
            mSocketSendThread.start();

            //开启心跳线程 执行心跳检测
            if (isLongConnection) {
                mSocketHeartBeat = new SocketHeartBeater("SocketHeartBeatThread",
                        outputStream, mSocket, this);
            }

            if (socketClientResponseInterface != null) {
                if(mSocket.isConnected()){
                    socketClientResponseInterface.onSocketConnect();
                }
            }
        } catch (ConnectException e) {
            failedMessage("客户端与服务端连接异常，请检查连接", SocketUtil.FAILED);
            Log.d(TAG,"failedMessage: " + e.getMessage());
            //e.printStackTrace();
            stopThread();
        } catch (IOException e) {
            failedMessage("IO发生异常，请稍后重试", SocketUtil.FAILED);
            Log.d(TAG,"failedMessage: " + e.getMessage());
            stopThread();
        }
    }

    /**
     * 发送消息
     */
    public void sendMsg(byte[] canSignalData) {
        if (mSocketSendThread != null) {
            mSocketSendThread.sendMsg(canSignalData);
        }
    }


    /**
     * 接收到服务端的消息
     * 2050项目服务端只回复握手和心跳数据
     */
    public void onSocketReceive(String socketResult){
        if (mSocketHeartBeat != null) {
            mSocketHeartBeat.onSocketReceiveHeartBeatData(socketResult);
        }
    }

    /**
     * 关闭socket客户端
     */
    public synchronized void stopThread() {
        Log.d(TAG,"stopThread()");
        //关闭接收线程
        closeReceiveTask();
        //唤醒发送线程并关闭
        wakeSendTask();
        //关闭心跳线程
        closeHeartBeatTask();
        //关闭socket
        closeSocket();
        //清除数据
        clearData();
        failedMessage("断开连接", SocketUtil.FAILED);
        if (isReConnect) {
            SocketUtil.toWait(this, 15000);
            initSocket();
            Log.i(TAG, "stopThread: " + Thread.currentThread().getName());
        }
    }

    /**
     * 唤醒后关闭发送线程
     */
    private void wakeSendTask() {
        if (mSocketSendThread != null) {
            mSocketSendThread.wakeSendTask();
        }
    }

    /**
     * 关闭接收线程
     */
    private void closeReceiveTask() {
        if (mSocketReceiveThread != null) {
            mSocketReceiveThread.close();
            mSocketReceiveThread = null;
        }
    }

    /**
     * 关闭心跳线程
     */
    private void closeHeartBeatTask() {
        if (mSocketHeartBeat != null) {
            mSocketHeartBeat.close();
        }
    }

    /**
     * 关闭socket
     */
    private void closeSocket() {
        if (mSocket != null) {
            if (mSocket.isConnected()) {
                /*try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }
            isSocketAvailable = false;
            mSocket = null;
        }
    }

    /**
     * 清除数据
     */
    private void clearData() {
        if (mSocketSendThread != null) {
            mSocketSendThread.clearData();
        }
    }

    /**
     * 连接失败回调
     */
    private void failedMessage(String msg, int code) {
        if (socketClientResponseInterface != null) {
            socketClientResponseInterface.onSocketDisable(msg, code);
        }
    }

    @Override
    public void onSocketShutdownInput() {
        if (isSocketAvailable) {
            SocketUtil.inputStreamShutdown(mSocket);
        }
    }

    @Override
    public void onSocketDisconnection() {
        isSocketAvailable = false;
        stopThread();
    }

    /**
     * 设置是否断线重连
     */
    public void setReConnect(boolean reConnect) {
        isReConnect = reConnect;
    }


}
