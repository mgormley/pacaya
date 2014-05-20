package edu.jhu.util.semiring;

/**
 * A semiring. (See also Algebra.)
 * 
 * @author mgormley
 */
public interface Semiring {

    double plus(double x, double y);
    double times(double x, double y);
    double zero();
    double one();
    
}
