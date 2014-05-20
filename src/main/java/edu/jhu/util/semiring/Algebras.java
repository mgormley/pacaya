package edu.jhu.util.semiring;

public class Algebras {

    private Algebras() {
        // private constructor.
    }

    public static double[] getToReal(double[] compacted, Algebra s) {
        double[] reals = new double[compacted.length];
        for (int i=0; i<compacted.length; i++) {
            reals[i] = s.toReal(compacted[i]);   
        }
        return reals;
    }

    public static double[][] getToReal(double[][] compacted, Algebra s) {
        double[][] reals = new double[compacted.length][];
        for (int i=0; i<compacted.length; i++) {
            reals[i] = getToReal(compacted[i], s);   
        }
        return reals;
    }
    
    public static double[] getFromReal(double[] reals, Algebra s) {
        double[] compacted = new double[reals.length];
        for (int i=0; i<reals.length; i++) {
            compacted[i] = s.fromReal(reals[i]);   
        }
        return compacted;
    } 
    
    public static double[][] getFromReal(double[][] reals, Algebra s) {
        double[][] compacted = new double[reals.length][];
        for (int i=0; i<reals.length; i++) {
            compacted[i] = getFromReal(reals[i], s);   
        }
        return compacted;
    }
    
    public static double[] getFromLogProb(double[] reals, Algebra s) {
        double[] compacted = new double[reals.length];
        for (int i=0; i<reals.length; i++) {
            compacted[i] = s.fromLogProb(reals[i]);   
        }
        return compacted;
    } 
    
    public static double[][] getFromLogProb(double[][] reals, Algebra s) {
        double[][] compacted = new double[reals.length][];
        for (int i=0; i<reals.length; i++) {
            compacted[i] = getFromLogProb(reals[i], s);   
        }
        return compacted;
    }

    public static void toReal(double[] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            vals[i] = s.toReal(vals[i]);   
        }
    }
    
    public static void toReal(double[][] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            toReal(vals[i], s);   
        }
    }

    public static void toReal(double[][][] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            toReal(vals[i], s);   
        }
    }

    public static void toReal(double[][][][] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            toReal(vals[i], s);   
        }
    }
    
    public static void fromReal(double[] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            vals[i] = s.fromReal(vals[i]);   
        }
    }
    
    public static void fromReal(double[][] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            fromReal(vals[i], s);   
        }
    }

    public static void fromReal(double[][][] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            fromReal(vals[i], s);   
        }
    }

    public static void fromReal(double[][][][] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            fromReal(vals[i], s);   
        }
    }
    
    public static void fromLogProb(double[] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            vals[i] = s.fromLogProb(vals[i]);   
        }
    }
    
    public static void fromLogProb(double[][] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            fromLogProb(vals[i], s);   
        }
    }

    public static void fromLogProb(double[][][] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            fromLogProb(vals[i], s);   
        }
    }

    public static void fromLogProb(double[][][][] vals, Algebra s) {
        for (int i=0; i<vals.length; i++) {
            fromLogProb(vals[i], s);   
        }
    }
    
}
