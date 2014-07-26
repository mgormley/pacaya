package edu.jhu.autodiff;

import edu.jhu.util.semiring.Algebra;

/**
 * The simplest possible interface for the output of a module.
 * @author mgormley
 */
public interface ModuleTensor<T> {
        
    Algebra getAlgebra();
    
    int size();
    
    double getValue(int idx);

    double setValue(int idx, double val);

    void fill(double val);
    
    T copyAndFill(double val);
    
    T copyAndConvertAlgebra(Algebra newS);

    void elemAdd(T addend);
    
    T copy();
        
}
