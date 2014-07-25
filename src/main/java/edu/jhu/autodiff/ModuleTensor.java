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
    
    double getValue(int idx);

    double setValue(int idx, double val);
    
    T copy();
        
}
