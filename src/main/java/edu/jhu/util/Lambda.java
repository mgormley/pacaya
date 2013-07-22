package edu.jhu.util;


/**
 * Container for lambda expression interfaces.
 * 
 * @author mgormley
 *
 */
public class Lambda {

    private Lambda() {
        // private constructor.
    }

    // TODO: Generalize this.
    public interface LambdaOne<T> {
        public void call(T obj);
    }
    
    public interface LambdaOneToOne<T,S> {
        public S call(T obj);
    }

    public interface LambdaTwo<T,S> {
        public void call(T obj1, S obj2);
    }

    public interface LambdaTwoToOne<T,S,V> {
        public V call(T obj1, S obj2);
    }
        
    /* -------------------- Doubles ---------------------- */
    
    /** A binary operator on doubles. */
    public interface LambdaBinOpDouble {
        public double call(double v1, double v2);
    }
    
    /** Addition operator on doubles. */
    public static final class DoubleAdd implements LambdaBinOpDouble {
        public double call(double v1, double v2) {
            return v1 + v2;
        }
    }
    
    /** Subtraction operator on doubles. */
    public static final class DoubleSubtract implements LambdaBinOpDouble {
        public double call(double v1, double v2) {
            return v1 - v2;
        }
    }
    
    /** Multiplication operator on doubles. */
    public static final class DoubleProd implements LambdaBinOpDouble {
        public double call(double v1, double v2) {
            return v1 * v2;
        }
    }
    
    /** Division operator on doubles. */
    public static final class DoubleDiv implements LambdaBinOpDouble {
        public double call(double v1, double v2) {
            return v1 / v2;
        }
    }
    
    /** Log-add operator on doubles. */
    public static final class DoubleLogAdd implements LambdaBinOpDouble {
        public double call(double v1, double v2) {
            return Utilities.logAdd(v1, v2);
        }
    }
    

    /* -------------------- Longs ---------------------- */
    
    /** A binary operator on longs. */
    public interface LambdaBinOpLong {
        public long call(long v1, long v2);
    }
    
    /** Addition operator on longs. */
    public static final class LongAdd implements LambdaBinOpLong {
        public long call(long v1, long v2) {
            return v1 + v2;
        }
    }
    
    /** Subtraction operator on longs. */
    public static final class LongSubtract implements LambdaBinOpLong {
        public long call(long v1, long v2) {
            return v1 - v2;
        }
    }
    
    /** Multiplication operator on longs. */
    public static final class LongProd implements LambdaBinOpLong {
        public long call(long v1, long v2) {
            return v1 * v2;
        }
    }
    
    /** Division operator on longs. */
    public static final class LongDiv implements LambdaBinOpLong {
        public long call(long v1, long v2) {
            return v1 / v2;
        }
    }
    

    /* -------------------- Ints ---------------------- */
    
    /** A binary operator on ints. */
    public interface LambdaBinOpInt {
        public int call(int v1, int v2);
    }
    
    /** Addition operator on ints. */
    public static final class IntAdd implements LambdaBinOpInt {
        public int call(int v1, int v2) {
            return v1 + v2;
        }
    }
    
    /** Subtraction operator on ints. */
    public static final class IntSubtract implements LambdaBinOpInt {
        public int call(int v1, int v2) {
            return v1 - v2;
        }
    }
    
    /** Multiplication operator on ints. */
    public static final class IntProd implements LambdaBinOpInt {
        public int call(int v1, int v2) {
            return v1 * v2;
        }
    }
    
    /** Division operator on ints. */
    public static final class IntDiv implements LambdaBinOpInt {
        public int call(int v1, int v2) {
            return v1 / v2;
        }
    }
    
}
