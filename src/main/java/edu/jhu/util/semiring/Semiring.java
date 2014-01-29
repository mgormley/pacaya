package edu.jhu.util.semiring;

public interface Semiring {

    double plus(double x, double y);
    double times(double x, double y);    
    double zero();    
    double one();
    
}
