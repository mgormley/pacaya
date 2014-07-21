package edu.jhu.autodiff;

/**
 * The simplest possible interface for the output of a module.
 * @author mgormley
 */
public interface ModuleTensor {
    
    double setValue(int idx, double val);
    
    int size();
    
}
