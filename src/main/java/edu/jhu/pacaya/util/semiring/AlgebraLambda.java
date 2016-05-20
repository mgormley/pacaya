package edu.jhu.pacaya.util.semiring;


public class AlgebraLambda {

    private AlgebraLambda() { 
        // private constructor
    }
    
    /** A binary operator. */
    public interface LambdaBinOp {
        public double call(Algebra s, double v1, double v2);
    }
    
    /** Addition operator. */
    public static final class Add implements LambdaBinOp {
        public double call(Algebra s, double v1, double v2) {
            return s.plus(v1, v2);
        }
    }
    
    /** Subtraction operator. */
    public static final class Subtract implements LambdaBinOp {
        public double call(Algebra s, double v1, double v2) {
            return s.minus(v1, v2);
        }
    }
    
    /** Multiplication operator. */
    public static final class Prod implements LambdaBinOp {
        public double call(Algebra s, double v1, double v2) {
            return s.times(v1, v2);
        }
    }
    
    /** Division operator. */
    public static final class Div implements LambdaBinOp {
        public double call(Algebra s, double v1, double v2) {
            return s.divide(v1, v2);
        }
    }
    
    /**
     * Like DoubleDiv, but handles edge cases slightly differently than Java:
     * 0 / 0 == 0  (java would have this be NaN)
     * This is useful in Belief Propagation.
     */
    public static final class DivBP implements LambdaBinOp {
        public final double call(Algebra s, double v1, double v2) {
            if(v1 == s.zero() && v2 == s.zero()) {
                return s.one();
            }
            return s.divide(v1, v2);
        }
    }


}
