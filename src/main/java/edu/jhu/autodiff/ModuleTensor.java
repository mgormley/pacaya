package edu.jhu.autodiff;

import edu.jhu.util.semiring.Algebra;

/**
 * The simplest possible interface for the output of a module.
 * @author mgormley
 */
public interface ModuleTensor<T> {
        
    int size();

    void fill(double val);
    
    T copyAndFill(double val);
    
    T copyAndConvertAlgebra(Algebra newS);

    void elemAdd(T addend);
    
    // Not currently needed:
    //double setValue(int idx, double val);
    //ModuleTensor copy();
        
}
