package edu.jhu.pacaya.autodiff;

import edu.jhu.pacaya.util.semiring.Algebra;

/**
 * A simple vector interface. This is specifically designed to be the output of a module.
 * <p>
 * This interface is generic in order to accomodate a typed copy method. By convention, the type T
 * should always be the class which is implementing this interface.
 * <p>
 * Important Note for implementers of the copy methods: it is recommended that the return type in
 * the declaration of the copy methods be the actual type instead of MVec. See {@link Tensor} for an
 * example.
 * 
 * @author mgormley
 */
public interface MVec {
        
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
    MVec copy();
    
    /** Creates a copy of this vector and converts it to a new abstract algebra. */
    MVec copyAndConvertAlgebra(Algebra newS);

    /** Creates a copy of this vector and fills it with a value. */
    MVec copyAndFill(double val);
    
    /** Adds (elementwise) a vector whose type is identical to this one. */
    void elemAdd(MVec addend);
    
}
