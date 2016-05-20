package edu.jhu.pacaya.util.semiring;

/**
 * Simple algebra which works on the real numbers shifted by some addend. For example, if the addend
 * is 3.0, then the real numbers [-1, 0, 1] map to the shifted set [2, 3, 4] respectively.
 * 
 * @author mgormley
 */
public final class ShiftedRealAlgebra extends AbstractToFromRealAlgebra {
    
    private static final long serialVersionUID = 1L;
    private static final ShiftedRealAlgebra SINGLETON = new ShiftedRealAlgebra(3.0);    
    private double addend;
    
    public ShiftedRealAlgebra(double addend) {
        this.addend = addend;
    }
    
    public static ShiftedRealAlgebra getInstance() {
        return SINGLETON;
    }
    
    @Override
    public double toReal(double nonReal) {
        return nonReal - addend;
    }

    @Override
    public double fromReal(double real) {
        return real + addend;
    }

}
