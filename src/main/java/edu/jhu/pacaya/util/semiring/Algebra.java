package edu.jhu.pacaya.util.semiring;


/** 
 * An abstract algebra.
 * 
 * @author mgormley
 */
public interface Algebra extends Semiring {

    /* ----- Basic Operations ----- */    
    double minus(double x, double y);
    double divide(double x, double y);
    double exp(double x);
    double log(double x);
    double abs(double x);
    double negate(double x);
    
    /* ----- Equality / Inequality Testing ----- */    
    boolean gt(double x, double y);
    boolean lt(double x, double y);
    boolean gte(double x, double y);
    boolean lte(double x, double y);
    boolean eq(double x, double y, double delta);
    
    /* ----- Conversion ----- */    
    double toReal(double nonReal);
    double fromReal(double real);
    double toLogProb(double nonReal);
    double fromLogProb(double logProb);
        
    /* ----- Introspection ----- */    
    boolean isNaN(double x);
    
    /* ----- Constants ----- */    
    double posInf();
    double negInf();
    double minValue();
    
}
