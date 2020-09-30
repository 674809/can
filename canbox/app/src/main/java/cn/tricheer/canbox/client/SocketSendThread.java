package cn.tricheer.canbox.client;

import android.util.Log;

import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketSendThread extends  Thread {
    private static final String TAG = "SocketSendThread";

    private volatile String name;

    private volatile boolean isCancel = false;
    private boolean closeSendTask;
    private final OutputStream outputStream;

    protected volatile ConcurrentLinkedQueue<byte[]> dataQueue = new ConcurrentLinkedQueue<>();
    //protected volatile ConcurrentLinkedQueue<String> dataQueue = new ConcurrentLinkedQueue<>();

    public SocketSendThread(String name, OutputStream outputStream) {
        this.name = name;
        this.outputStream = outputStream;
    }

    @Override
    public void run() {
        final Thread currentThread = Thread.currentThread();
        final String oldName = currentThread.getName();
        currentThread.setName("Processing-" + name);
        try {
            while (!isCancel) {
                //String dataContent = dataQueue.poll();
                byte[] dataContent = dataQueue.poll();
                if (dataContent == null) {
                    //没有发送数据则等待
                    SocketUtil.toWait(dataQueue);
                    if (closeSendTask) {
                        //notify()调用后，并不是马上就释放对象锁的，所以在此处中断发送线程
                        close();
                    }
                } else if (outputStream != null) {
                    synchronized (outputStream) {
                        SocketUtil.write2Stream(dataContent, outputStream);
                    }
                }
            }
        } finally {
            //循环结束则退出输出流
            if (outputStream != null) {
                synchronized (outputStream) {
                    SocketUtil.closeOutStream(outputStream);
                }
            }
            currentThread.setName(oldName);
            Log.i(TAG, "SocketSendThread finish");
        }
    }

    /**
     * 发送消息
     */
    public void sendMsg(byte[] canSignalData) {
        dataQueue.add(canSignalData);
        //有新增待发送数据，则唤醒发送线程
        SocketUtil.toNotifyAll(dataQueue);
    }

    /**
     * 清除数据
     */
    public void clearData() {
        dataQueue.clear();
    }

    public void close() {
        isCancel = true;
        this.interrupt();
        if (outputStream != null) {
            //防止写数据时停止，写完再停
            synchronized (outputStream) {
                SocketUtil.closeOutStream(outputStream);
            }
        }
    }

    public void wakeSendTask() {
        closeSendTask = true;
        SocketUtil.toNotifyAll(dataQueue);
    }

    public void setCloseSendTask(boolean closeSendTask) {
        this.closeSendTask = closeSendTask;
    }


}
