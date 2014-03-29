package edu.jhu.util.semiring;


public interface SemiringExt extends Semiring {

    double minus(double x, double y);
    double divide(double x, double y);
    double exp(double x);
    double log(double x);
    double toReal(double nonReal);
    double fromReal(double real);
    double toLogProb(double nonReal);
    double fromLogProb(double logProb);
    
}
