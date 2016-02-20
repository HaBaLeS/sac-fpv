package de.habales.sacfpv;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.jcodec.common.DictionaryCompressor;


public class MainActivity extends AppCompatActivity {
    Intent mSurfaceViewI;
    Intent mOpenGLI;
    Intent mTextureViewI;
    Intent mSettingsI;
    Intent mTestActivityIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        Button startFileSystemPlayer =  (Button)findViewById(R.id.startReplay);
        startFileSystemPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startReplayIntent = new Intent();
                startReplayIntent.setClass(MainActivity.this, RecordingPlaybackActivity.class);
                startActivity(startReplayIntent);
            }
        });

        Button mSurfaceTextureB=(Button)findViewById(R.id.button2);
        mTextureViewI=new Intent();
        mTextureViewI.setClass(this, TextureViewActivity.class);
        mSurfaceTextureB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mTextureViewI);
            }
        });
        Button mOpenGLB=(Button)findViewById(R.id.button3);
        mOpenGLI=new Intent();
        mOpenGLI.setClass(this, OpenGLActivity.class);
        mOpenGLB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mOpenGLI);
            }
        });

        Button mSettingsB=(Button)findViewById(R.id.button4);
        mSettingsI=new Intent();
        mSettingsI.setClass(this, SettingsActivit.class);
        mSettingsB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mSettingsI);
            }
        });

        Button mTestActivityB=(Button)findViewById(R.id.button5);
        mTestActivityIntent=new Intent();
        mTestActivityIntent.setClass(this, TestActivity.class);
        mTestActivityB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(mTestActivityIntent);
            }
        });

    }


}