package de.habales.sacfpv;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import de.habales.metrics.Metrics;
import de.habales.metrics.MxMeter;

public class BroadcastTestActivity extends Activity {

    private TextView logView;
    private BroadcastTestThread thread;
    public static int VIDEO_MULTICAST_PORT = 50666;
    int outSize = 1024 * 1024;
    byte[] message = new byte[outSize];
    boolean threadRunning = true;

    private MxMeter bandwidthMeter;
    private MxMeter ppsMeter;
    private Timer rateDisplay;
    private WifiManager.MulticastLock wifilock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        bandwidthMeter = Metrics.getMeter("Bandwidth");
        ppsMeter = Metrics.getMeter("PPS");

        //Does this help ?
        WifiManager wifi;
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifilock = wifi.createMulticastLock("just some tag text");
        wifilock.acquire();

        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_broadcast_test);
        logView = (TextView)findViewById(R.id.logview);
        thread = new BroadcastTestThread();
        thread.start();

        rateDisplay = new Timer("Rates Logger",true);
        rateDisplay.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                log("PPS:" + ppsMeter.lastSec());
                log("Bandwidth:" + bandwidthMeter.lastSec() + " bytes/s");

            }
        },1000,1000);
    }

    protected void log(final String msg){
        logView.post(new Runnable() {
            @Override
            public void run() {
                Log.d("TL", "Log: " + msg );
                logView.append("[X]\t" + msg + "\n");
            }
        });
    }


    private class BroadcastTestThread extends Thread{
        @Override
        public void run() {
            log("Starting UDP Broadcast Listener");

            try {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (networkInterfaces.hasMoreElements()){
                    NetworkInterface networkInterface = networkInterfaces.nextElement();
                    if(networkInterface.getDisplayName().contains("wlan")){
                        log("Interface " + networkInterface.getDisplayName());
                        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                        while(inetAddresses.hasMoreElements()){
                            log("\t" + inetAddresses.nextElement().getHostAddress());
                        }
                    }
                }

            } catch (SocketException e) {
                throw new RuntimeException("Wups", e);
            }

            try {
                DatagramSocket dataSocket = new DatagramSocket(VIDEO_MULTICAST_PORT);
                DatagramPacket packet = new DatagramPacket(message, message.length);
                dataSocket.setSoTimeout(200);
                while (threadRunning) {
                    try {
                        dataSocket.receive(packet);
                        //log(System.currentTimeMillis() + "\t" + packet.getLength());
                        ppsMeter.mark();
                        bandwidthMeter.mark(packet.getLength());
                    } catch (InterruptedIOException irx) {
                        //nix
                    }catch (Exception e){
                        throw new RuntimeException("IO Exception in UDP revice",e);
                    }
                }
                Log.d("TL", "run: Stopping thread");
                dataSocket.close();
            } catch (Exception e) {
                throw new RuntimeException("Error processing file!",e);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        threadRunning = false;
        wifilock.release();
        rateDisplay.cancel();
        rateDisplay.purge();
    }
}
