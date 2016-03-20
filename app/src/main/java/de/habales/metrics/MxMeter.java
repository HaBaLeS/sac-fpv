package de.habales.metrics;

import android.support.v4.util.CircularIntArray;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.List;
import java.util.Random;

/**
 * Created by falko on 20.03.2016.
 */
public class MxMeter {

    private String name;
    CircularFifoQueue circularBuffer = new CircularFifoQueue(60);
    long lastTick;
    int currentTickCount = 0;

    protected MxMeter(String name) {
        this.name = name;
        lastTick = System.currentTimeMillis();

        //Init the Ring
        for(int i=0; i<circularBuffer.maxSize(); i++){
           circularBuffer.add(0);
        }
    }

    public void mark(){
        tickIfneccessary();
        currentTickCount += 1;
    }

    public void mark(int amount){
        tickIfneccessary();
        currentTickCount += amount;
    }

    private void tickIfneccessary() {
        long refTime =  System.currentTimeMillis() - lastTick;
        if(refTime > 1000){
            System.out.println("Tick + " + refTime);
            for (int i = 1; i < (int)refTime/1000; i++) {
                System.out.println("xtra Skip");
                circularBuffer.add(0);
            }

            circularBuffer.add(currentTickCount);
            lastTick = System.currentTimeMillis();
            currentTickCount = 0;
        }
    }

    public String last60Sec(){
        int sum = 0;
        for(int i=0; i<circularBuffer.size(); i++){
            sum +=(Integer)circularBuffer.get(i);
        }
        return String.format("%1$.2f", (double)sum/60d);
    }

    public String last5Sec(){
        int sum = 0;
        for(int i=1; i<6; i++){
            sum +=(Integer)circularBuffer.get(circularBuffer.maxSize()-i);
        }
        return String.format("%1$.2f", (double)sum/5d);
    }

    public String lastSec(){
        int sum = (Integer) circularBuffer.get(circularBuffer.maxSize()-1);
        return String.format("%1$.2f", (double)sum);
    }


    /*
    public static void main(String arg[]) throws Exception{
        MxMeter mxm = new MxMeter("Blafasel");
        long st = System.currentTimeMillis();
        int cnt = 0;
        while(true){
            Thread.sleep(new Random().nextInt(50));
            mxm.mark();
            if(st+1000 < System.currentTimeMillis()) {

                System.out.println(cnt + "\t1 Sec " + mxm.lastSec() + "\t 5 Sec " + mxm.last5Sec() + "\t60 Sec " + mxm.last60Sec());
                st = System.currentTimeMillis();
                cnt++;
            }
        }
    }*/
}
