package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.util.Utilities;

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
    
    /** A binary operator on doubles. */
    public interface LambdaBinOpD {
        public double call(double v1, double v2);
    }
    
    /** Addition operator on doubles. */
    public static final class DoubleAdd implements LambdaBinOpD {
        public double call(double v1, double v2) {
            return v1 + v2;
        }
    }
    
    /** Multiplication operator on doubles. */
    public static final class DoubleProd implements LambdaBinOpD {
        public double call(double v1, double v2) {
            return v1 * v2;
        }
    }
    
    /** Log-add operator on doubles. */
    public static final class DoubleLogAdd implements LambdaBinOpD {
        public double call(double v1, double v2) {
            return Utilities.logAdd(v1, v2);
        }
    }
    
}
