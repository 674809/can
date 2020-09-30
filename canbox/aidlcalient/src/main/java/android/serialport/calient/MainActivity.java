package android.serialport.calient;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import cn.tricheer.canbox.IBackService;
import cn.tricheer.canbox.IChangeCallBack;

public class MainActivity extends Activity {
    private String TAG ="MainActivityt";
    private IBackService iBackService;
    private ServiceCar serviceCar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
    }

    private void initData() {
        Intent in = new Intent();
         serviceCar = new ServiceCar();
        in.setComponent(new ComponentName("cn.tricheer.canbox","cn.tricheer.canbox.CanService"));
        in.setAction("android.aidl.serviece");
        bindService(in, serviceCar, Service.BIND_AUTO_CREATE);

    }

    IChangeCallBack iChangeCallBack = new IChangeCallBack.Stub() {
        @Override
        public void callBack(String vin) throws RemoteException {
            Log.e(TAG, "callBack = "+vin);
        };
    };


   public class ServiceCar  implements ServiceConnection{

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            iBackService = IBackService.Stub.asInterface(iBinder);
            Log.e(TAG, "onServiceConnected = "+iBackService.toString());
            if(iBackService == null){
                Log.e(TAG, "iService is null");
            }else {
               try {
                    iBackService.registerListener(iChangeCallBack);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
           try {
                iBackService.unRegisterListener(iChangeCallBack);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            iBackService = null;
            Log.e(TAG, "iService is null" );
        }

    }




}
