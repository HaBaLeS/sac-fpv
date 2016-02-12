package de.habales.sacfpv;


import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class SurfaceViewActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private UdpReceiverDecoderThread mDecoder = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        setContentView(sv);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mDecoder == null) {
            mDecoder = new UdpReceiverDecoderThread(holder.getSurface(), 5000,this);
            mDecoder.start();
            Toast.makeText(this, "has been started", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(mDecoder != null) {
            mDecoder.interrupt();
            mDecoder=null;
        }
    }

}