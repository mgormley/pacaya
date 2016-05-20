package edu.jhu.pacaya.autodiff;

import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class VTensorUtils {

    private VTensorUtils() {
        // Private constructor.
    }

    public static VTensor get1DTensor(Algebra s, int s1) {
        VTensor t1 = new VTensor(s, s1);
        double val;
        val = 0;
        for (int i=0; i<s1; i++) {
            t1.set(val++, i);
        }
        return t1;
    }

    public static VTensor get2DTensor(Algebra s, int s1, int s2) {
        VTensor t1 = new VTensor(s, s1, s2);
        double val;
        val = 0;
        for (int i=0; i<s1; i++) {
            for (int j=0; j<s2; j++) {
                t1.set(val++, i,j);
            }
        }
        return t1;
    }

    public static VTensor get3DTensor(Algebra s, int s1, int s2, int s3) {
        VTensor t1 = new VTensor(s, s1, s2, s3);
        double val;
        val = 0;
        for (int i=0; i<s1; i++) {
            for (int j=0; j<s2; j++) {
                for (int k=0; k<s3; k++) {
                    t1.set(val++, i,j,k);            
                }
            }
        }
        return t1;
    }

    public static VTensor getVectorFromValues(Algebra s, double... values) {
        VTensor t1 = new VTensor(s, values.length);
        for (int c=0; c<values.length; c++) {
            t1.setValue(c, values[c]);
        }
        return t1;
    }

    /** Gets a tensor in the s semiring, where the input values are assumed to be in the reals. */
    public static VTensor getVectorFromReals(Algebra s, double... values) {
        VTensor t0 = getVectorFromValues(RealAlgebra.getInstance(), values);
        return t0.copyAndConvertAlgebra(s);
    }

}
