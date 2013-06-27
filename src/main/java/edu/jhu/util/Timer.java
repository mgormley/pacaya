package edu.jhu.util;

public class Timer {

    private int numStarts;
    private long totMs;
    private long startTime;
    private boolean isRunning;
    
    public Timer() {
        numStarts = 0;
        totMs = 0;
        startTime = 0;
        isRunning = false;
    }

    public void start() {
        if (isRunning == true) {
            throw new IllegalStateException("Timer is already running");
        }
        startTime = System.currentTimeMillis();
        isRunning = true;
        numStarts++;
    }

    public void stop() {
        if (isRunning != true) {
            throw new IllegalStateException("Timer is not running");
        }
        totMs += elapsedSinceLastStart();
        isRunning = false;
    }

    public void split() {
        stop();
        start();
    }

    private long elapsedSinceLastStart() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Gets the number of times that timer has been started or split.
     */
    public Object getCount() {
        return numStarts;
    }

    /**
     * Queries whether or not the timer is running.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Gets the total number of milliseconds.
     */
    public double totMs() {
        if (isRunning) {
            return totMs + elapsedSinceLastStart();
        } else {
            return totMs;
        }
    }

    /**
     * Gets the average number of milliseconds.
     */
    public double avgMs() {
        return totMs() / numStarts;
    }

    /**
     * Gets the total number of seconds.
     */
    public double totSec() {
        return totMs() / 1000.0;
    }

    /**
     * Gets the average number of seconds.
     */
    public double avgSec() {
        return totMs() / numStarts / 1000.0;
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
