package cn.tricheer.canbox.client;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class SocketUtil {
    private static final String TAG = "SocketUtil";
    public static int PORT = 10001;
    public static int FAILED = -101;
    public static int SUCCESS = 1;

    /**
     * 读数据
     *
     * @param bufferedReader
     */
    public static String readFromStream(BufferedReader bufferedReader) {
        try {
            String s;
            if ((s = bufferedReader.readLine()) != null) {
                return s;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }


    /**
     * 写数据
     *  @param data
     * @param outputStream
     */
    public static void write2Stream(byte[] data, OutputStream outputStream) {
        if (data == null) {
            return;
        }
        if (outputStream != null) {
            try {
                outputStream.write(data);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }





    protected static void toWait(Object o) {
        synchronized (o) {
            try {
                o.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    protected static void toWait(Object o,long time) {
        synchronized (o) {
            try {
                o.wait(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * notify()调用后，并不是马上就释放对象锁的，而是在相应的synchronized(){}语句块执行结束，自动释放锁后
     *
     * @param o
     */
    protected static void toNotifyAll(Object o) {
        synchronized (o) {
            o.notifyAll();
        }
    }



    /**
     * 关闭输入流
     *
     * @param socket
     */
    public static void inputStreamShutdown(Socket socket) {
        try {
            if (!socket.isClosed() && !socket.isInputShutdown()) {
                socket.shutdownInput();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭BufferedReader
     *
     * @param br
     */
    public static void closeBufferedReader(BufferedReader br) {
        try {
            if (br != null) {
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭输出流
     *
     * @param socket
     */
    public static void outputStreamShutdown(Socket socket) {
        try {
            if (!socket.isClosed() && !socket.isOutputShutdown()) {
                socket.shutdownOutput();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭PrintWriter
     *
     * @param pw
     */
    public static void closePrintWriter(PrintWriter pw) {
        if (pw != null) {
            pw.close();
        }
    }

    /**
     * 关闭OutStream
     *
     * @param os
     */
    public static void closeOutStream(OutputStream os) {
        if (os != null) {
            try {
                os.flush();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 获取本机IP地址
     */
    public static String getIP() {
        String hostIP = "127.0.0.1";
    /*    try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIP = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }*/
        Log.d(TAG,"getIP:  " + hostIP);
        return hostIP;
    }
}
