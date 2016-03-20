package de.habales.metrics;

import java.util.HashMap;

/**
 * Created by falko on 20.03.2016.
 * Inspired by https://dropwizard.github.io/metrics/3.1.0/getting-started/
 */
public class Metrics {

    static HashMap<String,MxCounter> counterMap = new HashMap();
    static HashMap<String,MxGauge> gaugesMap = new HashMap();
    static HashMap<String,MxMeter> meterMap = new HashMap();
    static HashMap<String,MxTimer> timerMap = new HashMap();


    public static MxCounter getCounter(String name){
        MxCounter mx = counterMap.get(name);
        if(mx == null){
            mx = new MxCounter(name);
            counterMap.put(name,mx);
        }
        return mx;
    }

    /**
     * A gauge is an instantaneous measurement of a value. For example, we may want to measure the number of pending jobs in a queue:
     */
    public static MxGauge getGauge(String name){
        return null;
    }

    /**
     * A meter measures the rate of events over time (e.g., “requests per second”).
     */
    public static MxMeter getMeter(String name){
        MxMeter mx = meterMap.get(name);
        if(mx == null){
            mx = new MxMeter(name);
            meterMap.put(name,mx);
        }
        return mx;
    }

    /**
     *    A timer measures both the rate that a particular piece of code is called and the distribution of its duration.
     */
    public static MxTimer getTimer(String name) {
        return null;
    }
}
