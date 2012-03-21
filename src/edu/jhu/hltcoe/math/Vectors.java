package edu.jhu.hltcoe.math;

import edu.jhu.hltcoe.util.Utilities;

public class Vectors {

    private Vectors() {
        // private constructor
    }
    

    public static double sum(double[] vector) {
        double sum = 0.0;
        for(int i=0; i<vector.length; i++) {
            sum += vector[i];
        }
        return sum;
    }


    public static void assertNoZeroes(double[] draw, double[] logDraw) {
        assertNoZeros(draw);
        assertNoNegInfs(logDraw);
    }

    public static void assertNoNegInfs(double[] logDraw) {
        for (int i=0; i<logDraw.length; i++) {
            assert(!Double.isNaN(logDraw[i]));
            assert(!Double.isInfinite(logDraw[i]));
        }
    }

    public static void assertNoZeros(double[] draw) {
        for (int i=0; i<draw.length; i++) {
            assert(!Double.isNaN(draw[i]));
            assert(draw[i] != 0.0);
        }
    }
    
    public static double[] getExp(double[] logPhi) {
        double[] phi = new double[logPhi.length];
        for (int i=0; i<phi.length; i++) {
            phi[i] = Utilities.exp(logPhi[i]);
        }
        return phi;
    }
    
    public static void exp(double[] phi) {
        for (int i=0; i<phi.length; i++) {
            phi[i] = Utilities.exp(phi[i]);
        }
    }
    
    public static void log(double[] phi) {
        for (int i=0; i<phi.length; i++) {
            phi[i] = Utilities.log(phi[i]);
        }
    }
    
    public static void logForIlp(double[] phi) {
        for (int i=0; i<phi.length; i++) {
            phi[i] = Utilities.logForIlp(phi[i]);
        }
    }

    public static double[] getLog(double[] phi) {
        double[] logPhi = new double[phi.length];
        Vectors.updateLogPhi(phi, logPhi);
        return logPhi;
    }
    
    public static double[] getLogForIlp(double[] phi) {
        double[] logPhi = new double[phi.length];
        for (int t=0; t<logPhi.length; t++) {
            logPhi[t] = Utilities.logForIlp(phi[t]);
        }
        return logPhi;
    }

    public static void updateLogPhi(double[] phi, double[] logPhi) {
    	for (int t=0; t<logPhi.length; t++) {
    		logPhi[t] = Utilities.log(phi[t]);
    	}
    }

    /**
     * TODO: This should live in a matrix class
     */
    public static double sum(double[][] matrix) {
        double sum = 0.0; 
        for (int i=0; i<matrix.length; i++) {
            sum += Vectors.sum(matrix[i]);
        }
        return sum;
    }

}
