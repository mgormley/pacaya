package edu.jhu.hltcoe.math;

import java.util.Random;

import org.junit.Test;

/**
 * A port of Jason Smith's C++ LogTable code, used for POS tagging.
 * @author mgormley
 */
public class LogAddTable {

    // Be careful changing this, logAdd depends on both this and logAddMin being
    // powers of 2
    // (any change in this requires a change in logAdd)
    private static final int LOG_ADD_TABLE_SIZE = 65536;
    private static final double logAddMin = -128;
    private static final double logAddInc = -logAddMin / LOG_ADD_TABLE_SIZE;
    private static final double invLogAddInc = LOG_ADD_TABLE_SIZE / -logAddMin;
    private static double[] logAddTable = new double[LOG_ADD_TABLE_SIZE + 1];

    static {
        for (int i = 0; i <= LOG_ADD_TABLE_SIZE; i++) {
            logAddTable[i] = Math.log1p(Math.exp((i * logAddInc) + logAddMin));
        }
    }

    public static double logAdd(double a, double b) {
        if (b > a) {
            double temp = a;
            a = b;
            b = temp;
        }
        double negDiff = b - a - logAddMin;
        if (negDiff < 0.0)
            return a;
        // this assumes invLogAddInc will be 2^9
        // return a + logAddTable[((int) negDiff) << 9];
        return a + logAddTable[(int) (negDiff * invLogAddInc)];
        // if (b - a < logAddMin)
        // return a;
        // return a + log1p(exp(b-a)); // switch to this line to make it exact
    }

    @Test
    public void test() {
        Random random = new Random();

        System.out.println("Log add test: ");
        double logAddDiff = 0;
        for (int i = 0; i < 100; i++) {
            double a = logAddMin * random.nextDouble();
            double b = a * random.nextDouble();
            System.out.println("a="+a + " b=" + b);
            double diff = logAdd(a, b) - (b + Math.log(1.0 + Math.exp(a)));
            if (diff < 0)
                logAddDiff -= diff;
            else
                logAddDiff += diff;
        }
        System.out.println("Total log add difference: " + logAddDiff);
        System.out.println(Math.exp(-128));
    }

}
