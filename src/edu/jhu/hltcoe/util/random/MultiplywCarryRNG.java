package edu.jhu.hltcoe.util.random;

/**
 * Marsaglia's multiply-with-carry random number generator. Robert Dodier,
 * dod...@colorado.edu.
 * 
 * Copied from http://groups.google.com/group/sci.math/browse_thread/thread/
 * b2cdf0a0a96c7375/fa034083b193adf0
 * 
 * This is a much faster version than CMWC4096, but has a shorter perior.
 * 
 * @author mgormley
 */
public class MultiplywCarryRNG {
    private static long a = 2083801278, x = 362436069, c = 1234567;

    public static int next32() {
        long axc = a * x + c;
        x = axc & 0xFFFFFFFFL;
        c = (axc >>> 32) & 0xFFFFFFFFL;
        return (int) x;
    }

    public static int next(int bits) {
        return next32() >> (32 - bits);
    }

    public static double nextDouble() {
        return (((long) next(26) << 27) + next(27)) / (double) (1L << 53);
    }

}