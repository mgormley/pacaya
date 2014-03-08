package edu.jhu.util.semiring;


public interface SemiringExt extends Semiring {

    double minus(double x, double y);
    double divide(double x, double y);
    double toReal(double nonReal);
    double fromReal(double real);
    
}
