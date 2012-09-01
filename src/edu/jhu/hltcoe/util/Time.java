package edu.jhu.hltcoe.util;

import org.jboss.dna.common.statistic.Stopwatch;

public class Time {

    /**
     * Gets the average number of milliseconds.
     * Helper function b/c it's so long to write out each time.
     */    
    public static double avgMs(Stopwatch timer) {
        return timer.getAverageDuration().getDurationInMilliseconds().doubleValue();
    }
    
    /**
     * Gets the total number of milliseconds.
     * Helper function b/c it's so long to write out each time.
     */    
    public static double totMs(Stopwatch timer) {
        return timer.getTotalDuration().getDurationInMilliseconds().doubleValue();
    }
    
    /**
     * Gets the total number of seconds.
     * Helper function b/c it's so long to write out each time.
     */    
    public static double totSec(Stopwatch timer) {
        return timer.getTotalDuration().getDurationInSeconds().doubleValue();
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
