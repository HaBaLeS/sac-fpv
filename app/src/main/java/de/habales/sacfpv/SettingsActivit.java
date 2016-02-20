package de.habales.sacfpv;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class SettingsActivit extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(sharedPreferences.getString("fileNameVideoSource","rpi960mal810.h264").
                equals(sharedPreferences.getString("fileName", "mGroundRecording.h264"))){
            Toast.makeText(this,"You mustn't select same file for video source and ground recording",Toast.LENGTH_LONG).show();
            SharedPreferences.Editor editor=sharedPreferences.edit();
            editor.putString("fileNameVideoSource","rpi960mal810.h264");
            editor.commit();
            editor.putString("fileName", "mGroundRecording.h264");
            editor.commit();
        }else{}
    }
}