package cn.tricheer.canbox.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

public class SocketClient implements SocketClientResponseInterface {

    private static final String TAG = "SocketClient";
    private final Context mContext;
    private SocketClientThread socketClientThread;
    private boolean isConnected = false;

    public SocketClient(Context context) {
        mContext = context;
        socketClientThread = new SocketClientThread("socketClientThread", this);
        new Thread(socketClientThread).start();
        //regitsterNetworkReceiver();
    }

    private void regitsterNetworkReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(new NetworkReceiver(),intentFilter);
    }


    private class NetworkReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,"onReceive()   action:  " + action);
            if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)){

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (socketClientThread){
                            socketClientThread.onSocketDisconnection();
                        }
                    }
                }).start();

            }
        }
    }

    @Override
    public void onSocketConnect() {
        Log.i(TAG, "onSocketConnect: 连接成功");
        isConnected = true;
    }

    @Override
    public void onSocketReceive(Object socketResult, int code) {
        Log.i(TAG, "onSocketReceive: 收到消息 ,  data: " + socketResult + " , code: " + code);
        if(null!=socketClientThread){
            if(null!=socketResult){
                socketClientThread.onSocketReceive(String.valueOf(socketResult));
            }
        }
    }

    @Override
    public void onSocketDisable(String msg, int code) {
        Log.i(TAG, "onSocketDisable: 连接断开 , msg: " + msg + " , code: " + code);
        isConnected = false;
    }

    public boolean isConnected() {
        return isConnected;
    }

    /*void sendmsg(){
        Parcel p;
        int length = p.readByte();
        byte[] CANSignalData = new byte[length];
        for(int i =0; i <length; i++){
            CANSignalData[i] = p.readByte();
        }
        sendData(CANSignalData);
    }*/

    public void sendData(byte[] canSignalData) {
        if (socketClientThread != null) {
            socketClientThread.sendMsg(canSignalData);
        }
    }

    /*public <T> void sendData(T data) {
        //convert to string or serialize object
        String s = (String) data;
        if (TextUtils.isEmpty(s)) {
            Log.i(TAG, "sendData: 消息不能为空");
            return;
        }
        if (socketClientThread != null) {
            socketClientThread.sendMsg(s);
        }
    }*/

    public void stopSocket() {
        //一定要在子线程内执行关闭socket等IO操作
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (socketClientThread){
                    socketClientThread.setReConnect(false);
                    socketClientThread.stopThread();
                }
            }
        }).start();
    }
}
