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

    public static double sum(int[] vector) {
        int sum = 0;
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


    public static double max(double[] array) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i=0; i<array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }
    
    public static double min(double[] array) {
        double min = Double.POSITIVE_INFINITY;
        for (int i=0; i<array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }


    public static double dotProduct(double[] array1, double[] array2) {
        if (array1.length != array2.length) {
            throw new IllegalStateException("array1.length != array2.length");
        }
        double dotProduct = 0.0;
        for (int i=0; i<array1.length; i++) {
            dotProduct += array1[i] * array2[i];
        }
        return dotProduct;
    }


    public static void scale(double[] array, double alpha) {
        for (int i=0; i<array.length; i++) {
            array[i] *= alpha;
        }
    }


    public static void scale(int[] array, int alpha) {
        for (int i=0; i<array.length; i++) {
            array[i] *= alpha;
        }
    }

    public static void add(double[] params, double lambda) {
        for (int i=0; i<params.length; i++) {
            params[i] += lambda;
        }
    }

}
