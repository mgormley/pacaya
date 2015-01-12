package edu.jhu.util.semiring;

import java.io.Serializable;

/**
 * A semiring. (See also Algebra.)
 * 
 * @author mgormley
 */
public interface Semiring extends Serializable {

    double plus(double x, double y);
    double times(double x, double y);
    double zero();
    double one();
    
}
