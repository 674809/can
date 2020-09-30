package cn.tricheer.canbox.client;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.OutputStream;
import java.net.Socket;

public class SocketHeartBeater {
    private static final String TAG = "SocketHeartBeatThread";
    private SocketHeartBeater.mSocketHeartBeatThread mSocketHeartBeatThread;
    private  OutputStream outputStream;

    private volatile String name;

    private long mSendHeartBeatDataTime;
    private long mReceiveHeartBeatDataTime;
    private static final long REPEAT_TIME = 10000;
    private static final int MSG_CHECK_HEARTBEAT_DATA = 1001;
    private static final int MSG_REPEATE = 1002;
    private static final String CONNECTION_NORMAL = "00"; //正常
    private static final String CONNECTION_ABNORMAL = "01"; //异常
    private boolean hasSendHeartBeatData = false;
    private boolean isConnected = false;
    private Socket mSocket;

    private SocketCloseInterface socketCloseInterface;


    private class HeartBeatDataCheckHander extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == MSG_CHECK_HEARTBEAT_DATA){
                Log.d(TAG,"handle msg: MSG_CHECK_HEARTBEAT_DATA ");
                removeMessages(MSG_CHECK_HEARTBEAT_DATA);
                if(isConnected){
                    sendEmptyMessageDelayed(MSG_REPEATE,REPEAT_TIME-(mReceiveHeartBeatDataTime - mSendHeartBeatDataTime));
                }else {
                    if (socketCloseInterface != null) {
                        socketCloseInterface.onSocketDisconnection();
                    }
                }
            }else if(msg.what == MSG_REPEATE){
                Log.d(TAG,"handle msg: MSG_REPEATE ");
                removeMessages(MSG_REPEATE);
                if(null!=mSocketHeartBeatThread){
                    if(mSocketHeartBeatThread.isAlive()){
                        mSocketHeartBeatThread.run();
                    }
                }
            }
        }
    }

    private Handler mHeartBeatDataCheckHander = null;


    public SocketHeartBeater(String name, OutputStream outputStream, Socket mSocket, SocketClientThread socketCloseInterface) {
        this.name = name;
        this.mSocket = mSocket;
        this.outputStream = outputStream;
        this.socketCloseInterface = socketCloseInterface;
        mSocketHeartBeatThread = new mSocketHeartBeatThread();
        mSocketHeartBeatThread.start();
    }


    private class mSocketHeartBeatThread extends Thread{

        @Override
        public void run() {
            if(Looper.myLooper() == null){
                Looper.prepare();
                if(mHeartBeatDataCheckHander == null){
                    mHeartBeatDataCheckHander = new HeartBeatDataCheckHander();
                }
            }
            final Thread currentThread = Thread.currentThread();
            final String oldName = currentThread.getName();
            currentThread.setName("Processing-" + name);
            try {
                if (!isConnected()) {
                    return;
                }
                if (mSocket != null) {
                    synchronized (outputStream) {
                        mSendHeartBeatDataTime = System.currentTimeMillis();
                        byte[] heartBeatData = {0x00,0x02};
                        SocketUtil.write2Stream(heartBeatData,outputStream);
                        /*byte[] heartBeatData = new byte[1];
                        heartBeatData[0] = (byte) 0x02;
                        mSocket.getOutputStream().write(heartBeatData);*/
                        mHeartBeatDataCheckHander.removeCallbacksAndMessages(null);
                        mHeartBeatDataCheckHander.sendEmptyMessageDelayed(MSG_CHECK_HEARTBEAT_DATA,5000);
                        Log.i(TAG, "run: SocketHeartBeatThread111");
                    }
                }
                Log.i(TAG, "run: SocketHeartBeatThread222");
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                //结束则退出输入流
               /* if (outputStream != null) {
                    synchronized (outputStream) {
                        SocketUtil.closeOutStream(outputStream);
                    }
                }*/
                currentThread.setName(oldName);
                Log.i(TAG, "SocketHeartBeatThread finish");
            }
            Looper.loop();
        }
    }


    /**
     * 判断本地socket连接状态
     */
    private boolean isConnected() {
        if ((null==mSocket)||mSocket.isClosed() || !mSocket.isConnected() ||
                mSocket.isInputShutdown() || mSocket.isOutputShutdown()) {
            if (socketCloseInterface != null) {
                socketCloseInterface.onSocketDisconnection();
            }
            return false;
        }
        return true;
    }

    public void close() {
        Log.d(TAG,"close()!!!!!");
        if (outputStream != null) {
            synchronized (outputStream) {
                SocketUtil.closeOutStream(outputStream);
            }
        }
        if(mHeartBeatDataCheckHander != null){
            mHeartBeatDataCheckHander.removeCallbacksAndMessages(null);
        }
        mSocket = null;
        if(null!=mSocketHeartBeatThread){
            mSocketHeartBeatThread.interrupt();
        }
    }

    public void onSocketReceiveHeartBeatData(String socketResult) {
        mReceiveHeartBeatDataTime = System.currentTimeMillis();
        if(null!=socketResult){
            if(socketResult.equals(CONNECTION_NORMAL)){
                Log.d(TAG,"connection normal!");
                isConnected = true;
                mHeartBeatDataCheckHander.removeCallbacksAndMessages(null);
                mHeartBeatDataCheckHander.sendEmptyMessage(MSG_CHECK_HEARTBEAT_DATA);
            }else if(socketResult.equals(CONNECTION_ABNORMAL)){
                Log.d(TAG,"Communication abnormal!");
                isConnected = false;
                mHeartBeatDataCheckHander.removeCallbacksAndMessages(null);
                mHeartBeatDataCheckHander.sendEmptyMessage(MSG_CHECK_HEARTBEAT_DATA);
            }else {
                Log.d(TAG,"Receive other data! ==> " + socketResult);
            }

        }
    }
}
