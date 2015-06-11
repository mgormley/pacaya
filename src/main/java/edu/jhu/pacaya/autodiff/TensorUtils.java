package edu.jhu.pacaya.autodiff;

import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class TensorUtils {

    private TensorUtils() {
        // Private constructor.
    }

    public static Tensor get1DTensor(Algebra s, int s1) {
        Tensor t1 = new Tensor(s, s1);
        double val;
        val = 0;
        for (int i=0; i<s1; i++) {
            t1.set(val++, i);
        }
        return t1;
    }

    public static Tensor get2DTensor(Algebra s, int s1, int s2) {
        Tensor t1 = new Tensor(s, s1, s2);
        double val;
        val = 0;
        for (int i=0; i<s1; i++) {
            for (int j=0; j<s2; j++) {
                t1.set(val++, i,j);
            }
        }
        return t1;
    }

    public static Tensor get3DTensor(Algebra s, int s1, int s2, int s3) {
        Tensor t1 = new Tensor(s, s1, s2, s3);
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

    public static Tensor getVectorFromValues(Algebra s, double... values) {
        Tensor t1 = new Tensor(s, values.length);
        for (int c=0; c<values.length; c++) {
            t1.setValue(c, values[c]);
        }
        return t1;
    }

    /** Gets a tensor in the s semiring, where the input values are assumed to be in the reals. */
    public static Tensor getVectorFromReals(Algebra s, double... values) {
        Tensor t0 = getVectorFromValues(RealAlgebra.SINGLETON, values);
        return t0.copyAndConvertAlgebra(s);
    }

}
