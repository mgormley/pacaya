package edu.jhu.util.math;

import java.util.Random;

import org.junit.Test;

import edu.jhu.util.Utilities;

/**
 * A port of Jason Smith's C++ LogTable code, used for POS tagging.
 * @author mgormley
 */
public class LogAddTable {

    // Be careful changing this, logAdd depends on both this and logAddMin being
    // powers of 2
    // (any change in this requires a change in logAdd)
    private static final int LOG_ADD_TABLE_SIZE = 65536;
    static final double LOG_ADD_MIN = -128;
    private static final double logAddInc = -LOG_ADD_MIN / LOG_ADD_TABLE_SIZE;
    private static final double invLogAddInc = LOG_ADD_TABLE_SIZE / -LOG_ADD_MIN;
    private static final double[] logAddTable = new double[LOG_ADD_TABLE_SIZE + 1];
    private static final double[] logSubtractTable = new double[LOG_ADD_TABLE_SIZE + 1];

    static {
        for (int i = 0; i <= LOG_ADD_TABLE_SIZE; i++) {
            logAddTable[i] = Math.log1p(Math.exp((i * logAddInc) + LOG_ADD_MIN));
        }

        for (int i = 0; i <= LOG_ADD_TABLE_SIZE; i++) {
            logSubtractTable[i] = Math.log1p( - Math.exp((i * logAddInc) + LOG_ADD_MIN));
        }
    }

    public static double logAdd(double a, double b) {
        if (b > a) {
            double temp = a;
            a = b;
            b = temp;
        }
        double negDiff = b - a - LOG_ADD_MIN;
        if (negDiff < 0.0)
            return a;
        // this assumes invLogAddInc will be 2^9
        // return a + logAddTable[((int) negDiff) << 9];
        return a + logAddTable[(int) (negDiff * invLogAddInc)];
        // if (b - a < logAddMin)
        // return a;
        // return a + log1p(exp(b-a)); // switch to this line to make it exact
    }
    
    public static double logSubtract(double a, double b) {
        if (b > a) {            
            throw new IllegalStateException("a must be >= b. a=" + a + " b=" + b);
        }
        double negDiff = b - a - LOG_ADD_MIN;
        if (negDiff < 0.0)
            return a;
        return a + logSubtractTable[(int) (negDiff * invLogAddInc)];
    }

}
