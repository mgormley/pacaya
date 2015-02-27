package edu.jhu.autodiff;

import java.util.Arrays;

import edu.jhu.util.semiring.Algebra;

/**
 * An array of vectors implementing {@link MVec}s. This presents a vector view of the concatenation
 * of the vectors.
 * 
 * @author mgormley
 * @param <Y> The type of vector (i.e. type of {@link MVec})
 */
public class MVecArray<Y extends MVec> implements MVec {
    
    public Y[] f;
    public Algebra s;
    
    public MVecArray(Algebra s) {
        this.s = s;
    }

    public MVecArray(Y[] facBeliefs) {
        this.f = facBeliefs;
    }

    public void fill(double val) {
        fillArray(f, val);
    }
    
    public int size() {
        return count(f);
    }

    public double getValue(int idx) {
        return getValue(idx, f);
    }
    
    public double setValue(int idx, double val) {
        return setValue(idx, val, f);
    }
    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void elemAdd(MVec addend) {
        if (addend instanceof MVecArray) {
            elemAdd((MVecArray)addend);
        } else {
            throw new IllegalArgumentException("Addend must be of type " + this.getClass());
        }
    }
    
    public void elemAdd(MVecArray<Y> addend) {
        addArray(this.f, addend.f);
    }

    public MVecArray<Y> copy() {
        MVecArray<Y> clone = new MVecArray<Y>(s);
        clone.f = copyOfArray(this.f);
        return clone;
    }

    public MVecArray<Y> copyAndConvertAlgebra(Algebra newS) {
        MVecArray<Y> clone = new MVecArray<Y>(newS);
        clone.f = copyAndConvertAlgebraOfArray(this.f, newS);
        return clone;
    }

    public MVecArray<Y> copyAndFill(double val) {
        MVecArray<Y> clone = copy();
        clone.fill(val);
        return clone;
    }

    public Algebra getAlgebra() {
        return s;
    }
    
    public Y[] getArray() {
        return f;
    }

    /* --------------------------------------------------------- */
    
    // For use by this class only, these methods can be rewritten as having type <T extends
    // MVec>. However, for the general case we would want <T extends MVec> which
    // Java seems confused by. This is similar to the way the JDK handles Arrays.copyOf().
    
    public static <T extends MVec> int count(T[] beliefs) {
        int count = 0;
        if (beliefs != null) {
            for (int i = 0; i < beliefs.length; i++) {
                if (beliefs[i] != null) {
                    count += beliefs[i].size();
                }
            }
        }
        return count;
    }
        
    public static <T extends MVec> void fillArray(T[] beliefs, double val) {
        if (beliefs != null) {
            for (int i = 0; i < beliefs.length; i++) {
                if (beliefs[i] != null) {
                    beliefs[i].fill(val);
                }
            }
        }
    }

    public static <T extends MVec> double setValue(int idx, double val, T[] beliefs) {
        int seen = 0;
        for (int i = 0; i < beliefs.length; i++) {
            if (beliefs[i] != null) {
                if (beliefs[i].size() + seen > idx) {
                    return beliefs[i].setValue(idx - seen, val);
                }
                seen += beliefs[i].size();
            }
        }
        throw new RuntimeException("Index out of bounds: " + idx);
    }
    
    public static <T extends MVec> double getValue(int idx, T[] beliefs) {
        int seen = 0;
        for (int i = 0; i < beliefs.length; i++) {
            if (beliefs[i] != null) {
                if (beliefs[i].size() + seen > idx) {
                    return beliefs[i].getValue(idx - seen);
                }
                seen += beliefs[i].size();
            }
        }
        throw new RuntimeException("Index out of bounds: " + idx);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends MVec> T[] copyOfArray(T[] orig) {
        if (orig == null) {
            return null;
        }
        T[] clone = Arrays.copyOf(orig, orig.length);
        for (int v = 0; v < clone.length; v++) {
            if (orig[v] != null) {
                MVec copy = orig[v].copy();
                if (!copy.getClass().equals(orig[v].getClass())) {
                    throw new RuntimeException(orig[v].getClass() + "does not correctly implement MVec."
                            + " Its copy method must return its own type instead of " + copy.getClass());
                }
                clone[v] = (T) copy;
            }
        }
        return clone;
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends MVec> T[] copyAndConvertAlgebraOfArray(T[] orig, Algebra newS) {
        if (orig == null) {
            return null;
        }
        T[] clone = Arrays.copyOf(orig, orig.length);
        for (int v = 0; v < clone.length; v++) {
            if (orig[v] != null) {
                MVec copy = orig[v].copyAndConvertAlgebra(newS);
                if (!copy.getClass().equals(orig[v].getClass())) {
                    throw new RuntimeException(orig[v].getClass() + " does not correctly implement MVec."
                            + " Its copy method must return its own type instead of " + copy.getClass());
                }
                clone[v] = (T) copy;
            }
        }
        return clone;
    }
    
    public static <T extends MVec> void addArray(T[] b1, T[] addend) {
        assert b1.length == addend.length;
        for (int i = 0; i < b1.length; i++) {            
            if (b1[i] != null) {
                ((MVec)b1[i]).elemAdd(addend[i]);
            }
        }
    }
    
    /* --------------------------------------------------------- */

    
}