package de.habales.sacfpv;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class RecordingPlaybackActivity extends Activity implements SurfaceHolder.Callback{

    Thread decoderThread;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //window settings
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        SurfaceView sv = new SurfaceView(this);
        sv.getHolder().addCallback(this);
        setContentView(sv);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        //decoderThread = new H264DecodeThreadBlafasel(holder.getSurface());
        decoderThread = new H264DecodeThreadUDP(holder.getSurface());
        //decoderThread = new H264DecodeThreadFileBasedLogging(holder.getSurface());
        //decoderThread = new H264DecodeThread(holder.getSurface());
        //decoderThread = new H264DecodeThreadFileBased(holder.getSurface());




        decoderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        decoderThread.interrupt();;
    }
}
