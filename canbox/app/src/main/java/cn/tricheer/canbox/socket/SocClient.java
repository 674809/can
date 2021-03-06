package cn.tricheer.canbox.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import cn.tricheer.canbox.DataUtils;

/**
 * Created by dawish on 2017/7/24.
 *  由于移动设备的网络的复杂性，经常会出现网络断开，如果没有心跳包的检测，
 *  客户端只会在需要发送数据的时候才知道自己已经断线，会延误，甚至丢失服务器发送过来的数据。
 */

public class SocClient {
    private static final String TAG = "SocketClient";
    /**心跳频率*/
    private static final long HEART_BEAT_RATE = 3 * 1000;
    /**服务器ip地址*/
    public static final String HOST = "127.0.0.1";
    /**服务器端口号*/
    public static final int PORT = 10001;
    /**服务器消息回复广播*/
    public static final String MESSAGE_ACTION="message_ACTION";
    /**服务器心跳回复广播*/
    public static final String HEART_BEAT_ACTION="heart_beat_ACTION";
    /**读线程*/
    private ReadThread mReadThread;

    private LocalBroadcastManager mLocalBroadcastManager;
    /***/
    private WeakReference<Socket> mSocket;

    private Context mContext;

    private  int   BEATOK = 0x00;

    public SocClient(Context mcontext){
        this.mContext = mcontext;
        init();
    }

    private void init() {
        new InitSocketThread().start();
        mLocalBroadcastManager=LocalBroadcastManager.getInstance(mContext);
    }


    // For heart Beat
    private Handler mHandler = new Handler();
    /**心跳任务，不断重复调用自己*/
    private Runnable heartBeatRunnable = new Runnable() {

        @Override
        public void run() {
            if (System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {
                boolean isSuccess = sendMsg();//就发送一个\r\n过去 如果发送失败，就重新初始化一个socket
                if (!isSuccess) {
                    mHandler.removeCallbacks(heartBeatRunnable);
                    mReadThread.release();
                    releaseLastSocket(mSocket);
                    new InitSocketThread().start();
                }
            }
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    private long sendTime = 0L;



    public boolean sendMsg() {
        Log.e(TAG, "send 心跳");
        if (null == mSocket || null == mSocket.get()) {
            return false;
        }
        final Socket soc = mSocket.get();
            if (!soc.isClosed() && !soc.isOutputShutdown()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            OutputStream os = soc.getOutputStream();
                            byte[] heartBeatData = {0x00,0x02};
                           // String message = msg + "\r\n";
                            os.write(heartBeatData);
                            os.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                sendTime = System.currentTimeMillis();//每次发送成数据，就改一下最后成功发送的时间，节省心跳间隔时间
            } else {
                return false;
            }
        return true;
    }
    /*
    *初始化Socket
     */
    private void initSocket() {
        try {
            Socket so = new Socket(HOST, PORT);
            mSocket = new WeakReference<Socket>(so);
            mReadThread = new ReadThread(so);
            mReadThread.start();
            mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//初始化成功后，就准备发送心跳包
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 心跳机制判断出socket已经断开后，就销毁连接方便重新创建连接
     * @param mSocket
     */
    private void releaseLastSocket(WeakReference<Socket> mSocket) {
        try {
            if (null != mSocket) {
                Socket sk = mSocket.get();
                if (!sk.isClosed()) {
                    sk.close();
                }
                sk = null;
                mSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class InitSocketThread extends Thread {
        @Override
        public void run() {
            super.run();
            initSocket();
        }
    }

    // Thread to read content from Socket
    class ReadThread extends Thread {
        private WeakReference<Socket> mWeakSocket;
        private boolean isStart = true;

        public ReadThread(Socket socket) {
            mWeakSocket = new WeakReference<Socket>(socket);
        }

        public void release() {
            isStart = false;
            releaseLastSocket(mWeakSocket);
        }

        @Override
        public void run() {
            super.run();
            Socket socket = mWeakSocket.get();
            if (null != socket) {
                try {
                    InputStream is = socket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int length = 0;
                    while (!socket.isClosed() && !socket.isInputShutdown()
                            && isStart && ((length = is.read(buffer)) != -1)) {
                        if (length > 0) {
                            int datas = DataUtils.bytesToInt(buffer);
                            Log.e(TAG, "server data =" +datas);
                            //收到服务器过来的消息，就通过Broadcast发送出去
                            if(BEATOK == datas){//处理心跳回复
                                Log.i(TAG,"心跳 ok");
                            }else{
                                Log.i(TAG,"" +"断开");
                                //其他消息回复
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void StopConnect(){
        mHandler.removeCallbacks(heartBeatRunnable);
        mReadThread.release();
        releaseLastSocket(mSocket);
    }

}
