package edu.jhu.util.semiring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Algebras {
    
    private static final Logger log = LoggerFactory.getLogger(Algebras.class);

    private Algebras() {
        // private constructor.
    }
    
    // Algebras.
    public static final RealAlgebra REAL_ALGEBRA = new RealAlgebra(); 
    public static final LogSignAlgebra LOG_SIGN_ALGEBRA = new LogSignAlgebra(); 
    public static final Algebra SPLIT_ALGEBRA = new SplitAlgebra(); 
    public static final Algebra SHIFTED_REAL_ALGEBRA = new ShiftedRealAlgebra(); 
    
    // Semirings.
    public static final LogSemiring LOG_SEMIRING = new LogSemiring(); 
    public static final ViterbiSemiring VITERBI_SEMIRING = new ViterbiSemiring(); 
    public static final TropicalSemiring TROPICAL_SEMIRING = new TropicalSemiring();
    
    // TODO: unit test.
    public static double convertAlgebra(double value, Algebra src, Algebra dst) {
        if (dst.equals(src)) {
            return value;
        } else if (src.equals(Algebras.REAL_ALGEBRA)) {
            return dst.fromReal(value);
        } else if (src.equals(Algebras.LOG_SEMIRING)) {
            return dst.fromLogProb(value);
        } else if (dst.equals(Algebras.REAL_ALGEBRA)) {
            return src.toReal(value);
        } else if (dst.equals(Algebras.LOG_SEMIRING)) {
            return src.toLogProb(value);
        } else {
            // We pivot through the real numbers, but this could cause a loss of
            // floating point precision.
            log.warn("FOR TESTING ONLY: unsafe conversion from " + src + " to " + dst);
            return dst.fromReal(src.toReal(value));
        }
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
    
    public static void convertAlgebra(double[] vals, Algebra src, Algebra dst) {
        for (int i=0; i<vals.length; i++) {
            vals[i] = convertAlgebra(vals[i], src, dst);   
        }
    }
    
    public static void convertAlgebra(double[][] vals, Algebra src, Algebra dst) {
        for (int i=0; i<vals.length; i++) {
            convertAlgebra(vals[i], src, dst); 
        }
    }

    public static void convertAlgebra(double[][][] vals, Algebra src, Algebra dst) {
        for (int i=0; i<vals.length; i++) {
            convertAlgebra(vals[i], src, dst);
        }
    }

    public static void convertAlgebra(double[][][][] vals, Algebra src, Algebra dst) {
        for (int i=0; i<vals.length; i++) {
            convertAlgebra(vals[i], src, dst);
        }
    }
    
}
