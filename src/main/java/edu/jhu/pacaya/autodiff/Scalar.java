package edu.jhu.pacaya.autodiff;

import edu.jhu.pacaya.util.semiring.Algebra;

public class Scalar {

    private Scalar() { 
        // Private constructor. 
    }
    
    public static Tensor getInstance(Algebra s, double value) {
        Tensor t = new Tensor(s, 1);
        t.set(value, 0);
        return t;
    }
    
}
