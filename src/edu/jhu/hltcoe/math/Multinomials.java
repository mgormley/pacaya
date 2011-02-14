package edu.jhu.hltcoe.math;

import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;

public class Multinomials {

    private Multinomials() {
        // private constructor
    }

    public static double[] randomMultinomial(int length) {
        double[] props = new double[length];
        for (int i=0; i<props.length; i++) {
            props[i] = Prng.random.nextDouble();
        }
        normalizeProps(props);
        return props;
    }
    
    public static void normalizeProps(double[] props) {
        double propSum = 0;
        for (int d = 0; d < props.length; d++) {
            propSum += props[d];
        }
        for (int d = 0; d < props.length; d++) {
            props[d] /= propSum;
            assert(!Double.isNaN(props[d]));
        }
        //TODO: remove 
//        dPropSum = 0;
//        for (int d = 0; d < dProp.length; d++) {
//            dPropSum += dProp[d];
//        }
//        assert(dPropSum - 1.0 < 0.000000000001);
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

    public static int sampleFromProportions(double[] dProp) {
        double dPropSum = 0;
        for (int d = 0; d < dProp.length; d++) {
            dPropSum += dProp[d];
        }
        int d;
        double rand = Prng.random.nextDouble() * dPropSum;
        double partialSum = 0;
        for (d = 0; d < dProp.length; d++) {
            partialSum += dProp[d];
            if (rand <= partialSum) {
                break;
            }
        }
    
        assert (d < dProp.length);
        return d;
    }
    
}
