package edu.jhu.hltcoe.math;

import java.util.Random;

import org.junit.Test;

import edu.jhu.hltcoe.util.Utilities;

/**
 * A port of Jason Smith's C++ LogTable code, used for POS tagging.
 * @author mgormley
 */
public class LogAddTable {

    // Be careful changing this, logAdd depends on both this and logAddMin being
    // powers of 2
    // (any change in this requires a change in logAdd)
    private static final int LOG_ADD_TABLE_SIZE = 65536;
    private static final double LOG_ADD_MIN = -128;
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
    
    @Test
    public void testLogAddTable() {
        Random random = new Random();

        System.out.println("Log add test: ");
        double logAddDiff = 0;
        for (int i = 0; i < 100; i++) {
            double a = LOG_ADD_MIN * random.nextDouble();
            double b = a * random.nextDouble();
            System.out.println("a="+a + " b=" + b);
            double diff = logAdd(a, b) - Utilities.logAddExact(a, b);
            if (diff < 0)
                logAddDiff -= diff;
            else
                logAddDiff += diff;
        }
        System.out.println("Total log add difference: " + logAddDiff);
        System.out.println(Math.exp(-128));
    }
    
    @Test
    public void testLogSubtractTable() {
        Random random = new Random();

        System.out.println("Log subtract test: ");
        double logSubtractDiff = 0;
        for (int i = 0; i < 100; i++) {
            double b = LOG_ADD_MIN * random.nextDouble();
            double a = b * random.nextDouble();
            System.out.println("a="+a + " b=" + b);
            double diff = logSubtract(a, b) - Utilities.logSubtractExact(a, b);
            if (diff < 0)
                logSubtractDiff -= diff;
            else
                logSubtractDiff += diff;
        }
        System.out.println("Total log subtract difference: " + logSubtractDiff);
        System.out.println(Math.exp(-128));
    }
    
    @Test
    public void testAddSubtract() {
        double sum;
        System.out.println("sum: " + Math.log(0.2 + 0.3));
        sum = Double.NEGATIVE_INFINITY;
        sum = logAdd(sum, Math.log(0.2));
        sum = logAdd(sum, Math.log(0.3));
        System.out.println("sum: " + sum);
        sum = Double.NEGATIVE_INFINITY;
        sum = Utilities.logAddExact(sum, Math.log(0.2));
        sum = Utilities.logAddExact(sum, Math.log(0.3));
        System.out.println("sum: " + sum);
        sum = Utilities.logAddExact(sum, Math.log(0.1));
        sum = Utilities.logSubtractExact(sum, Math.log(0.1));
        System.out.println("sum: " + sum);
    }
    
    @Test
    public void test3() {
        double largestNegDouble = -Double.MAX_VALUE;
        System.out.println("Largest negative double: " + largestNegDouble);
        System.out.println("Log add of largest neg double with itself: " + logAdd(largestNegDouble, largestNegDouble));
        System.out.println("Min double: " + Double.MIN_VALUE);
        System.out.println("Log of min double: " + Math.log(Double.MIN_VALUE));
        System.out.println("Min 32-bit float: " + -Float.MAX_VALUE); //-3.4E38);
        //System.out.println("Min 32-bit float: " + -3.4E38);
    }
    
    @Test
    public void test4() {
        System.out.println(Utilities.logAddExact(Double.NEGATIVE_INFINITY, 0));
        System.out.println(Utilities.logAddExact(0, 0));
    }

}
