package edu.jhu.hltcoe.util;

import org.jboss.dna.common.statistic.Stopwatch;

public class Time {

    /**
     * Helper function b/c it's so long to write out each time.
     */    
    public static double avgMs(Stopwatch timer) {
        return timer.getAverageDuration().getDurationInMilliseconds().doubleValue();
    }
    
    /**
     * Helper function b/c it's so long to write out each time.
     */    
    public static double totMs(Stopwatch timer) {
        return timer.getTotalDuration().getDurationInMilliseconds().doubleValue();
    }
    
    public static String durAsStr(double milliseconds) {
        long timeInSeconds = (long)(milliseconds / 1000.0);
        long hours, minutes, seconds;
        hours = timeInSeconds / 3600;
        timeInSeconds = timeInSeconds - (hours * 3600);
        minutes = timeInSeconds / 60;
        timeInSeconds = timeInSeconds - (minutes * 60);
        seconds = timeInSeconds;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

}
