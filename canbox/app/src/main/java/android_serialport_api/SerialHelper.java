package android_serialport_api;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Arrays;

import cn.tricheer.canbox.DataUtils;


public class SerialHelper {
    private String TAG = "SerialHelper";
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private SendThread mSendThread;
    private String sPort = "/dev/ttyHSL2";
    private int iBaudRate = 38400;
    private boolean _isOpen = false;
    private byte[] _bLoopData = new byte[]{0x30};
    private int iDelay = 500;

    public ISeriaData miSeriaData;
   public  interface ISeriaData {
        void onDataReceived(byte[] data);
        void onOpenSend();
    }

    public void SetISeriaDataListener(ISeriaData iSeriaData){
        this.miSeriaData = iSeriaData;
    }
    //----------------------------------------------------
    public SerialHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    public SerialHelper() {
        this("/dev/ttyHSL2", 38400);
    }


    //----------------------------------------------------
    public void open() throws SecurityException, IOException, InvalidParameterException {
        mSerialPort = new SerialPort(new File(sPort), iBaudRate, 0);
        Log.i(TAG, "打开串口");
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();
        mReadThread = new ReadThread();
        mReadThread.start();
        mSendThread = new SendThread();
        mSendThread.setSuspendFlag();
        mSendThread.start();
        _isOpen = true;
        if(miSeriaData != null){
            miSeriaData.onOpenSend();
        }
    }

    //----------------------------------------------------
    public void close() {
        if (mReadThread != null)
            mReadThread.interrupt();
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
            Log.i(TAG, "关闭串口");
        }
        _isOpen = false;
    }

    //----------------------------------------------------
    public void send(byte[] bOutArray) {
        try {
            mOutputStream.write(bOutArray);
            Log.d(TAG, " send :" + bOutArray.toString());
            mOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //----------------------------------------------------
    public void sendHex(String sHex) {
        byte[] bOutArray = DataUtils.HexToByteArr(sHex);
        send(bOutArray);
    }

    //----------------------------------------------------
    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                Log.d(TAG, "thread read!");
                try {
                    if (mInputStream == null) {
                        Log.d(TAG, "mInputStream is null");
                        return;
                    }
                    byte[] buffer = new byte[64];
                    int size = mInputStream.read(buffer);
                    Log.d(TAG, "ReadThread.run() size = "+size);
                   if (size > 0) {
                       Log.d(TAG, "ReadThread.run() buffer=" + Arrays.toString(buffer) );
                       if(miSeriaData !=null ){
                           miSeriaData.onDataReceived(buffer);
                       }
                        //onDataReceived(ComRecData);
                    } else {
                        Log.d(TAG, "Size is < 0");
                    }
                    try {
                        Thread.sleep(100);//延时50ms
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Exception = " + e.toString());
                    e.printStackTrace();
                    return;
                }
            }
        }
    }



    //----------------------------------------------------
    private class SendThread extends Thread {
        public boolean suspendFlag = true;// 控制线程的执行

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                synchronized (this) {
                    while (suspendFlag) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                Log.d(TAG, "thread send!");
                send(getbLoopData());
                try {
                    Thread.sleep(iDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //线程暂停
        public void setSuspendFlag() {
            this.suspendFlag = true;
        }

        //唤醒线程
        public synchronized void setResume() {
            this.suspendFlag = false;
            notify();
        }
    }

    //----------------------------------------------------
    public int getBaudRate() {
        return iBaudRate;
    }

    public boolean setBaudRate(int iBaud) {
        if (_isOpen) {
            return false;
        } else {
            iBaudRate = iBaud;
            return true;
        }
    }

    public boolean setBaudRate(String sBaud) {
        int iBaud = Integer.parseInt(sBaud);
        return setBaudRate(iBaud);
    }

    //----------------------------------------------------
    public String getPort() {
        return sPort;
    }

    public boolean setPort(String sPort) {
        if (_isOpen) {
            return false;
        } else {
            this.sPort = sPort;
            return true;
        }
    }

    //----------------------------------------------------
    public boolean isOpen() {
        return _isOpen;
    }

    //----------------------------------------------------
    public byte[] getbLoopData() {
        return _bLoopData;
    }

    //----------------------------------------------------
    public void setbLoopData(byte[] bLoopData) {
        this._bLoopData = bLoopData;
    }

    //----------------------------------------------------
    public void setTxtLoopData(String sTxt) {
        this._bLoopData = sTxt.getBytes();
    }

    //----------------------------------------------------
    public void setHexLoopData(String sHex) {
        this._bLoopData = DataUtils.HexToByteArr(sHex);
    }

    //----------------------------------------------------
    public int getiDelay() {
        return iDelay;
    }

    //----------------------------------------------------
    public void setiDelay(int iDelay) {
        this.iDelay = iDelay;
    }

    //----------------------------------------------------
    public void startSend() {
        if (mSendThread != null) {
            mSendThread.setResume();
        }
    }

    //----------------------------------------------------
    public void stopSend() {
        if (mSendThread != null) {
            mSendThread.setSuspendFlag();
        }
    }


}
