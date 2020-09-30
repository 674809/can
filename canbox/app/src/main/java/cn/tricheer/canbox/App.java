package cn.tricheer.canbox;

import android.app.Application;
import android.content.Intent;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent();
        startService(intent);
    }
}
