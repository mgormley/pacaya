package edu.jhu.pacaya.util.semiring;

public class ViterbiSemiring implements Semiring {

    private static final long serialVersionUID = 1L;
    public static final ViterbiSemiring SINGLETON = new ViterbiSemiring();

    private ViterbiSemiring() {
        // Private constructor.
    }
    
    @Override
    public double plus(double x, double y) {
        return Math.max(x, y);
    }

    @Override
    public double times(double x, double y) {
        return x * y;
    }

    @Override
    public double zero() {
        return 0;
    }

    @Override
    public double one() {
        return 1;
    }

    // Two Algebras / Semirings are equal if they are of the same class.
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null) { return false; }
        if (this.getClass() == other.getClass()) { return true; }
        return false;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
    
}
