package edu.jhu.autodiff;

import edu.jhu.util.semiring.Algebra;

/**
 * A simple vector interface. This is specifically designed to be the output of a module.
 * 
 * This interface is generic in order to accomodate a typed copy method. By convention, the type T
 * should always be the class which is implementing this interface.
 * 
 * @author mgormley
 */
public interface MVec<T> {
        
    /** Gets the abstract algebra in which values are represented. */
    Algebra getAlgebra();
    
    /** Gets the number of entries. */
    int size();
    
    /** Gets the idx'th value. */
    double getValue(int idx);

    /** Sets the idx'th value. */
    double setValue(int idx, double val);

    /** Fills the entire vector with a value. */
    void fill(double val);

    /** Creates a copy of this vector. */
    T copy();
    
    /** Creates a copy of this vector and converts it to a new abstract algebra. */
    T copyAndConvertAlgebra(Algebra newS);

    /** Creates a copy of this vector and fills it with a value. */
    T copyAndFill(double val);
    
    /** Adds (elementwise) a vector whose type is identical to this one. */
    void elemAdd(T addend);
    
}
