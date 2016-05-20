package edu.jhu.pacaya.util.semiring;

public final class LogViterbiSemiring implements Semiring {

    private static final long serialVersionUID = 1L;
    private static final LogViterbiSemiring SINGLETON = new LogViterbiSemiring();
    
    private LogViterbiSemiring() {
        // Private constructor.
    }
    
    public static LogViterbiSemiring getInstance() {
        return SINGLETON;
    }
    
    @Override
    public double plus(double x, double y) {
        return Math.max(x, y);
    }

    @Override
    public double times(double x, double y) {
        return x + y;
    }

    @Override
    public double zero() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public double one() {
        return 0;
    }

    // Two Algebras / Semirings are equal if they are of the same class.
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null) { return false; }
        if (this.getClass() == other.getClass()) { return true; }
        return false;
    }
    
}
