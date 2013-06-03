package edu.jhu.hltcoe.util.math;

import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;

public class Multinomials {

    private Multinomials() {
        // private constructor
    }

    public static double[] randomMultinomial(int length) {
        double[] props = new double[length];
        for (int i=0; i<props.length; i++) {
            props[i] = Prng.nextDouble();
        }
        normalizeProps(props);
        return props;
    }
    
    public static void normalizeProps(double[] props) {
        double propSum = 0;
        for (int d = 0; d < props.length; d++) {
            propSum += props[d];
        }
        if (propSum != 0) {
            for (int d = 0; d < props.length; d++) {
                props[d] /= propSum;
                assert(!Double.isNaN(props[d]));
            }
        } else {
            for (int d = 0; d < props.length; d++) {
                props[d] = 1.0 / (double)props.length;
            }
        }
    }
    
    public static void normalizeLogProps(double[] logProps) {
        double logPropSum = Double.NEGATIVE_INFINITY;
        for (int d = 0; d < logProps.length; d++) {
            logPropSum = Utilities.logAdd(logPropSum, logProps[d]);
        }
        for (int d = 0; d < logProps.length; d++) {
            logProps[d] -= logPropSum;
            assert(!Double.isNaN(logProps[d]));
        }
    }
    
    /**
     * Asserts that the parameters are log-normalized within some delta.
     */
    public static void assertLogNormalized(double[] logProps, double delta) {
        double logPropSum = Double.NEGATIVE_INFINITY;
        for (int d = 0; d < logProps.length; d++) {
            logPropSum = Utilities.logAdd(logPropSum, logProps[d]);
        }
        assert(Utilities.equals(0.0, logPropSum, delta));
    }

    public static int sampleFromProportions(double[] dProp) {
        double dPropSum = 0;
        for (int d = 0; d < dProp.length; d++) {
            dPropSum += dProp[d];
        }
        int d;
        double rand = Prng.nextDouble() * dPropSum;
        double partialSum = 0;
        for (d = 0; d < dProp.length; d++) {
            partialSum += dProp[d];
            if (rand <= partialSum && dProp[d] != 0.0) {
                break;
            }
        }
    
        assert (d < dProp.length);
        return d;
    }
    
}
