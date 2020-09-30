package cn.tricheer.canbox.client;

import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;

public class SocketReceiveThread extends Thread{
    private static final String TAG = "SocketReceiveThread";
    private  DataInputStream dataInputStream;

    private volatile String name;

    private volatile boolean isCancel = false;

    private SocketCloseInterface socketCloseInterface;

    private SocketClientResponseInterface socketClientResponseInterface;


    public SocketReceiveThread(String name, DataInputStream dataInputStream,
                               SocketClientResponseInterface socketClientResponseInterface,
                               SocketClientThread socketCloseInterface) {
        this.name = name;
        this.dataInputStream = dataInputStream;
        this.socketClientResponseInterface = socketClientResponseInterface;
        this.socketCloseInterface = socketCloseInterface;

    }

    @Override
    public void run() {
        final Thread currentThread = Thread.currentThread();
        final String oldName = currentThread.getName();
        currentThread.setName("Processing-" + name);
        try {
            while (true) {
                if(isCancel){
                    break;
                }
                if (dataInputStream != null) {
                    //String receiverData = SocketUtil.readFromStream(dataInputStream);
                    byte[] bytes = new byte[1]; // 一次读取一个byte
                    String receiverData = "";
                    while ((dataInputStream.available() != 0)&&(dataInputStream.read(bytes) != -1)) {
                        receiverData += SocketUtil.bytesToHexString(bytes);
                        if (dataInputStream.available() == 0) { //一个请求
                            if (receiverData != null) {
                                successMessage(receiverData);
                            }
                        }
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            currentThread.setName(oldName);
            Log.i(TAG, "SocketReceiveThread finish");
        }
    }

    /**
     * 接收消息回调
     */
    private void successMessage(String data) {
        Log.i(TAG, "receiver data from server! successMessage()  data: " + data);
        if (socketClientResponseInterface != null) {
            socketClientResponseInterface.onSocketReceive(data, SocketUtil.SUCCESS);
        }
    }

    public void close() {
        isCancel = true;
        this.interrupt();
        if (dataInputStream != null) {
            if (socketCloseInterface != null) {
                socketCloseInterface.onSocketShutdownInput();
            }
            try {
                dataInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dataInputStream = null;
        }
    }
}
