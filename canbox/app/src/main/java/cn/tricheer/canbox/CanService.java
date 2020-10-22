package cn.tricheer.canbox;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;


import java.io.IOException;

import java.security.InvalidParameterException;

import java.util.ArrayList;
import java.util.Arrays;

import android_serialport_api.SerialHelper;
import cn.tricheer.canbox.client.SocketClient;
import cn.tricheer.canbox.SubUtil;

//cn.tricheer.canbox.CanService
public class CanService extends Service implements SerialHelper.ISeriaData {
    private static String TAG = "CanService";
    private SerialHelper mserialHelper;
    private SocketClient msocketClient;
    private String VINHEAD = "2e70";
    private String CANID = "2e71";
    private String GEAR = "2e710f02"; //档位
    private String MILE = "2e710f03"; //里程
    private String VIN = "null";
    private boolean isConnect = false;
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
        arrayList.add(Cmd.REQUEST_4);
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
        String datas = DataUtils.bytesToHex(data);//"c5050000000844006759000c007f1bff2e70114c47424835324530304b59313039343231802e710f0221040000000308000000000000004d0000000000000000";
       //datas = "c5050000000844006759000c007f1bff2e70114c47424835324530304b59313039343231802e710f0221040000000308000000000000004d0000000000000000";
        Log.i(TAG, "onDataReceived datas = " +datas );
        if(datas.contains(VINHEAD)){//vin
             getCanVin(datas);                   //2e701100000000000000000000000000000000007e
        }
        if(datas.contains(CANID)){ //can过滤  2e710f0221040000000308000000000000004dff
            getCanID(datas);
        }
        if(datas.contains("a511223344556677")){
            canBusTestCallBack(true);
            isConnect = true;
        }else {
            canBusTestCallBack(false);
            isConnect = false;
        }
    }

    /**
     * 车身 Can过滤 ID数据帧
     */
    private void getCanID(String data) {
        int start = data.indexOf(GEAR);//23
        Log.i(TAG,"start = "+start);
        if(start == -1){
            start= 0;
        }
        String  canid = data.substring(start,start+40);//2e710f0221040000000308000000000000004dff
            Log.i(TAG, "can 过滤 +id ="+canid);
            if(canid.length() >8){
                String sw = canid.substring(0, 8);
                Log.i(TAG,"sw = "+sw);
                if (GEAR.equals(sw)) { //当位
                    String gear = SubUtil.formatCanGear(data);
                    Log.i(TAG,"gear = "+gear);
                    sendData(DataUtils.HexToByteArr(gear));
                } else if (MILE.equals(sw)) {// 里程
                    String mile = SubUtil.formatCanMile(data);
                    Log.i(TAG,"mile = "+mile);
                    sendData(DataUtils.HexToByteArr(mile));
                }
            }

    }

    /**
     * vin 逻辑处理
     */
    private void getCanVin(String data) {
        int start = data.indexOf(VINHEAD);//23
        int dataLength = start+42;
        if(data.length()>= dataLength){
            VIN = data.substring(start,start+42);//2e70114c47424835324530304b5931303934323180
        }
        Log.i(TAG, "vinCode = " + VIN);
        String  callbackdata = SubUtil.formatVin(VIN);//3114c47424835324530344a59363933363133
        Log.i(TAG,"callbackdata length = "+callbackdata.length() +"\n"+callbackdata);
        if (VIN.length() > 40) {
            String vin = VIN.substring(6, VIN.length()-2);
            VIN = DataUtils.convertHexToString(vin);
            Log.i(TAG,"callback vin code = "+VIN);
            callBack(VIN);
        }
        sendData(DataUtils.HexToByteArr(callbackdata)); //发给联友
    }

    /**
     * 发送数据给联友
     * @param data
     */
    public void sendData(byte[] data) {
        if (msocketClient != null && msocketClient.isConnected()) {
            msocketClient.sendData(data);
            Log.i(TAG, "sendData = "+ Arrays.toString(data));
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
     * @param
     */
    public void callBack(String data) {
        Log.i(TAG, "callBack = " + data);
        int count = mRemoteCallbackList.beginBroadcast();
        Log.i(TAG, "count = " + count);
        try {
            for (int i = 0; i < count; i++) {
                mRemoteCallbackList.getBroadcastItem(i).callBack(data);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            mRemoteCallbackList.finishBroadcast();
        }
    }

    public void canBusTestCallBack(boolean isConnect){
        int count = mRemoteCallbackList.beginBroadcast();
        Log.i(TAG, "count = " + count);
        try {
            for (int i = 0; i < count; i++) {
                mRemoteCallbackList.getBroadcastItem(i).canBusTest(isConnect);
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

    public class BackServer extends IBackService.Stub {

        @Override
        public String onVinChange() throws RemoteException {
            sendData(DataUtils.HexToByteArr(Cmd.REQUEST_4));
            return VIN;
        }

        @Override
        public boolean canbusTest() throws RemoteException {
            sendData(DataUtils.HexToByteArr(Cmd.TEST_FACTORY));
            return isConnect;
        }

        @Override
        public void registerListener(IChangeCallBack callBack) throws RemoteException {
            Log.i(TAG, "registerListener callBack = " + callBack);
            if (mRemoteCallbackList != null) {
                Log.i(TAG, "registerListener  ok");
                mRemoteCallbackList.register(callBack);

            }
        }

        @Override
        public void unRegisterListener(IChangeCallBack callBack) throws RemoteException {
            Log.i(TAG, "unRegisterListener");
            if (mRemoteCallbackList != null) {
                Log.i(TAG, "unRegisterListener ok");
                mRemoteCallbackList.unregister(callBack);

            }
        }
    }


}
