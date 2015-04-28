package edu.jhu.pacaya.util.semiring;

/**
 * Simple algebra which works on the real numbers shifted by some addend. For example, if the addend
 * is 3.0, then the real numbers [-1, 0, 1] map to the shifted set [2, 3, 4] respectively.
 * 
 * @author mgormley
 */
public class ShiftedRealAlgebra extends AbstractToFromRealAlgebra {

    private double addend;
    
    public ShiftedRealAlgebra() {
        this(3.0);
    }
    
    public ShiftedRealAlgebra(double addend) {
        this.addend = addend;
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
