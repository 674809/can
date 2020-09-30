package cn.tricheer.canbox;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;


import java.io.IOException;

import java.security.InvalidParameterException;

import java.util.ArrayList;

import android_serialport_api.SerialHelper;
import cn.tricheer.canbox.client.SocketClient;


public class CanService extends Service implements SerialHelper.ISeriaData {
    private static String TAG = "CanService";
    private SerialHelper mserialHelper;
    private SocketClient msocketClient;
    private String VINHEAD = "2e70";
    private String VIN = "-1";
    private ArrayList<String> arrayList = new ArrayList();
    private static RemoteCallbackList<IChangeCallBack> mRemoteCallbackList = new RemoteCallbackList<>();

    @Override
    public IBinder onBind(Intent intent) {
        return new BackServer();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "CanService onCreate");
        initMessage();
        initSeria();
        initSocket();


    }

    private void initMessage() {
        arrayList.add(Cmd.REQUEST_0);
        arrayList.add(Cmd.REQUEST_1);
        arrayList.add(Cmd.REQUEST_2);
        arrayList.add(Cmd.REQUEST_3);
    }


    private void initSeria() {
        mserialHelper = new SerialHelper();
        mserialHelper.SetISeriaDataListener(this);
        try {
            mserialHelper.open();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (InvalidParameterException e) {
            e.printStackTrace();
        }
    }

    private void initSocket() {
        msocketClient = new SocketClient(this);
    }

    @Override
    public void onDataReceived(byte[] data) {
        String datas = DataUtils.bytesToHex(data);
        String vin = datas.substring(0, 4);
        Log.i(TAG, "onDataReceived datas = " + datas + " \n vin substring = " + vin);
        if (VINHEAD.equals(vin)) {
            VIN = datas;
            Log.i(TAG, "vin send client ");
            callBack();
        }
        if (msocketClient != null && msocketClient.isConnected()) {
            msocketClient.sendData(data);
            Log.i(TAG, "sendData");
        }
    }

    @Override
    public void onOpenSend() {
        if (mserialHelper != null && mserialHelper.isOpen()) {
            Log.i(TAG, "SEND MESSAGE TO CAN  size = " + arrayList.size());
            for (int i = 0; i < arrayList.size(); i++) {
                mserialHelper.send(DataUtils.HexToByteArr(arrayList.get(i)));
                Log.i(TAG, "send = " + arrayList.get(i));
            }
            //
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mserialHelper != null) {
            mserialHelper.close();
        }
        if (arrayList.size() > 0) {
            arrayList.clear();
        }
    }

    /**
     * 回调给客户端
     *
     * @param
     */
   public void callBack() {
        int count = mRemoteCallbackList.beginBroadcast();
        Log.i(TAG, "count = " + count);
        try {
            for (int i = 0; i < count; i++) {
                mRemoteCallbackList.getBroadcastItem(i).callBack(VIN);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            mRemoteCallbackList.finishBroadcast();
        }
    }

    /**
     * aidl 服务端
     */

    public  class BackServer extends IBackService.Stub{

        @Override
        public String onVinChange() throws RemoteException {
            return null;
        }

        @Override
        public void registerListener(IChangeCallBack callBack) throws RemoteException {
            Log.i(TAG,"registerListener callBack = "+callBack);
          if(mRemoteCallbackList != null){
               Log.i(TAG,"registerListener  ok");
                mRemoteCallbackList.register(callBack);

            }
        }

        @Override
        public void unRegisterListener(IChangeCallBack callBack) throws RemoteException {
            Log.i(TAG,"unRegisterListener");
            if(mRemoteCallbackList != null){
                Log.i(TAG,"unRegisterListener ok");
               mRemoteCallbackList.unregister(callBack);

            }
        }
    }



}
