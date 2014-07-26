package edu.jhu.autodiff;

import edu.jhu.util.semiring.Algebra;

/**
 * A simple vector interface. This is specifically designed to be the output of a module.
 * 
 * @author mgormley
 */
public interface MVec<T> {
        
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
