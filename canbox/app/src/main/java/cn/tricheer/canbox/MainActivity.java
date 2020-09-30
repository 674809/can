package cn.tricheer.canbox;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;

import android_serialport_api.SerialHelper;
import cn.tricheer.canbox.R;


public class MainActivity extends Activity {
    private String TAG = "MainActivity";
    private Button button;
    private TextView tv;
    private SerialHelper mserialHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.btn);
        tv = (TextView) findViewById(R.id.tv);
        Intent intent = new Intent(MainActivity.this, CanService.class);
        startService(intent);
        int a =  (0x90+0x02+0x71+0x03)^0xFF;
        Log.i(TAG,"a = " +Integer.toHexString(a));
    }



    @Override
    public void finish() {
        super.finish();
    }
}
