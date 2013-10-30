package edu.jhu.gm.model;

import java.io.Serializable;
import java.util.Arrays;

import edu.jhu.gm.util.IntIter;
import edu.jhu.util.Lambda;
import edu.jhu.util.Lambda.LambdaBinOpDouble;
import edu.jhu.util.Utilities;
import edu.jhu.util.math.Multinomials;
import edu.jhu.util.math.Vectors;

/**
 * A multivariate Multinomial distribution. 
 * 
 * @author mgormley
 *
 */
public class DenseFactor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** All variables without an id are given this value. */
    public static final int UNINITIALIZED_NODE_ID = -1;
  
    /** The set of variables in this factor. */
    private VarSet vars;
    /**
     * The values of each entry in this factor. These could be probabilities
     * (normalized or unormalized), in the real or log domain.
     */
    private double[] values;
    
    /** Constructs a factor initializing the values to 0.0. */
    public DenseFactor(VarSet vars) {
        this(vars, 0.0);
    }
    
    /**
     * Constructs a factor where each value is set to some initial value.
     * @param vars The variable set.
     * @param initialValue The initial value.
     */
    public DenseFactor(VarSet vars, double initialValue) {
        this.vars = vars;
        int numConfigs = vars.calcNumConfigs();
        this.values = new double[numConfigs];
        Arrays.fill(values, initialValue);
    }
    
    /** Copy constructor. */
    public DenseFactor(DenseFactor f) {
        this.vars = f.vars;
        this.values = Utilities.copyOf(f.values);
        // We don't want to copy the node id since it uniquely refers to the
        // node of the input factor.
    }

    /**
     * Gets the marginal distribution over a subset of the variables in this
     * factor, optionally normalized. This method assumes the values are reals
     * (i.e. not in the log domain).
     * 
     * @param vars The subset of variables for the marginal distribution. This will sum over all variables not in this set.
     * @param normalize Whether to normalize the resulting distribution.
     * @return The marginal distribution represented as log-probabilities.
     */
    public DenseFactor getMarginal(VarSet vars, boolean normalize) {
        return getMarginal(vars, normalize, false);
    }

    /**
     * Gets the marginal distribution over a subset of the variables in this
     * factor, optionally normalized. This method assumes the values are 
     * in the log domain.
     * 
     * @param vars The subset of variables for the marginal distribution. This will sum over all variables not in this set.
     * @param normalize Whether to normalize the resulting distribution.
     * @return The marginal distribution represented as log-probabilities.
     */
    public DenseFactor getLogMarginal(VarSet vars, boolean normalize) {
        return getMarginal(vars, normalize, true);
    }
    
    private DenseFactor getMarginal(VarSet vars, boolean normalize, boolean logDomain) {
        VarSet margVars = new VarSet(this.vars);
        margVars.retainAll(vars);
        
        DenseFactor marg = new DenseFactor(margVars, logDomain ? Double.NEGATIVE_INFINITY : 0.0);
        
        IntIter iter = margVars.getConfigIter(this.vars);
        for (int i=0; i<this.values.length; i++) {
            int j = iter.next();
            if (logDomain) {
                marg.values[j] = Utilities.logAdd(marg.values[j], this.values[i]);
            } else {
                marg.values[j] += this.values[i];
            }
        }
        
        if (normalize) {
            if (logDomain) {
                marg.logNormalize();
            } else {
                marg.normalize();
            }
        }
        
        return marg;
    }
    
    public DenseFactor getClamped(VarConfig clmpVarConfig) {
        if (clmpVarConfig.size() == 0) {
            return new DenseFactor(this);
        }
        VarSet clmpVars = clmpVarConfig.getVars();
        VarSet unclmpVars = new VarSet(this.vars);
        unclmpVars.removeAll(clmpVars); 

        DenseFactor clmp = new DenseFactor(unclmpVars);
        IntIter iter = unclmpVars.getConfigIter(this.vars);
        
        int numEqual = 0;
        if (clmp.values.length > 0) {
            int numConfigs = vars.calcNumConfigs();
            for (int c=0; c<numConfigs; c++) {
                VarConfig curClmpSubset = this.vars.getVarConfig(c).getSubset(clmpVars);
                assert curClmpSubset.size() == clmpVarConfig.size();
                int uc = iter.next();
                if (clmpVarConfig.equals(curClmpSubset)) {
                    clmp.values[uc] = this.values[c];
                    numEqual++;
                }
            }
        }
        assert numEqual == unclmpVars.calcNumConfigs() : "numEqual=" + numEqual;
        return clmp;
    }
    
    /** Gets the variables associated with this factor. */
    public VarSet getVars() {
        return vars;
    }

    /**
     * Gets the value of the c'th configuration of the variables.
     */
    public double getValue(int c) {
        return values[c];
    }

    /** Sets the value of the c'th configuration of the variables. */
    public void setValue(int c, double value) {
        values[c] = value;
    }
    
    /** Set all the values to the given value. */
    public void set(double value) {
        Arrays.fill(values, value);
    }
    
    /** Add the addend to each value. */
    public void add(double addend) {
        Vectors.add(values, addend);
    }
    
    /** Scale each value by lambda. */
    public void scale(double lambda) {
        Vectors.scale(values, lambda);
    }
    
    /** Normalizes the values. */
    public void normalize() {
        Multinomials.normalizeProps(values);
    }

    /** Normalizes the values. */
    public void logNormalize() {
        Multinomials.normalizeLogProps(values);
    }
    
    /** Takes the log of each value. */
    public void convertRealToLog() {
        Vectors.log(values);
    }
    
    /** Takes the exp of each value. */
    public void convertLogToReal() {
        Vectors.exp(values);
    }

    /** Gets the sum of the values for this factor. */
    public double getSum() {
        return Vectors.sum(values);
    }
    
    /** Gets the log of the sum of the values for this factor. */
    public double getLogSum() {
        return Vectors.logSum(values);
    }
    
    /**
     * Adds a factor to this one.
     * 
     * From libDAI: 
     *  The sum of two factors is defined as follows: if
     *  \f$f : \prod_{l\in L} X_l \to [0,\infty)\f$ and \f$g : \prod_{m\in M} X_m \to [0,\infty)\f$, then
     *  \f[f+g : \prod_{l\in L\cup M} X_l \to [0,\infty) : x \mapsto f(x_L) + g(x_M).\f]
     */
    public void add(DenseFactor f) {
        DenseFactor newFactor = applyBinOp(this, f, new Lambda.DoubleAdd());
        this.vars = newFactor.vars;
        this.values = newFactor.values;      
    }

    /**
     * Multiplies a factor to this one.
     * 
     * From libDAI:
     *  The product of two factors is defined as follows: if
     *  \f$f : \prod_{l\in L} X_l \to [0,\infty)\f$ and \f$g : \prod_{m\in M} X_m \to [0,\infty)\f$, then
     *  \f[fg : \prod_{l\in L\cup M} X_l \to [0,\infty) : x \mapsto f(x_L) g(x_M).\f]
     */
    public void prod(DenseFactor f) {
        DenseFactor newFactor = applyBinOp(this, f, new Lambda.DoubleProd());
        this.vars = newFactor.vars;
        this.values = newFactor.values;  
    }
    
    /**
     * Log-adds a factor to this one.
     * 
     * This is analogous to factor addition, except that the logAdd operator
     * is used instead.
     */
    public void logAdd(DenseFactor f) {
        DenseFactor newFactor = applyBinOp(this, f, new Lambda.DoubleLogAdd());
        this.vars = newFactor.vars;
        this.values = newFactor.values;  
    }
    
    /**
     * Applies the binary operator to factors f1 and f2.
     * 
     * This method will opt to be destructive on f1 (returing it instead of a
     * new factor) if time/space can be saved by doing so.
     * 
     * Note: destructive if necessary.
     * 
     * @param f1 The first factor. (returned if it will save time/space)
     * @param f2 The second factor.
     * @param op The binary operator.
     * @return The new factor.
     */
    private static DenseFactor applyBinOp(final DenseFactor f1, final DenseFactor f2, final LambdaBinOpDouble op) {
        if (f1.vars.size() == 0) {
            // Return a copy of f2.
            return new DenseFactor(f2);
        } else if (f2.vars.size() == 0) {
            // Don't use the copy constructor, just return f1.
            return f1;
        } else if (f1.vars == f2.vars || f1.vars.equals(f2.vars)) {
            // Special case where the factors have identical variable sets.
            assert (f1.values.length == f2.values.length);
            for (int c = 0; c < f1.values.length; c++) {
                f1.values[c] = op.call(f1.values[c], f2.values[c]);
            }
            return f1;
        } else if (f1.vars.isSuperset(f2.vars)) {
            // Special case where f1 is a superset of f2.
            IntIter iter2 = f2.vars.getConfigIter(f1.vars);
            for (int c = 0; c < f1.vars.calcNumConfigs(); c++) {
                f1.values[c] = op.call(f1.values[c], f2.values[iter2.next()]);
            }
            assert(!iter2.hasNext());
            return f1;
        } else {
            // The union of the two variable sets must be created.
            VarSet union = new VarSet(f1.vars, f2.vars);
            DenseFactor out = new DenseFactor(union);
            IntIter iter1 = f1.vars.getConfigIter(union);
            IntIter iter2 = f2.vars.getConfigIter(union);
            for (int c = 0; c < out.vars.calcNumConfigs(); c++) {
                out.values[c] = op.call(f1.values[iter1.next()], f2.values[iter2.next()]);
            }
            assert(!iter1.hasNext());
            assert(!iter2.hasNext());
            return out;
        }
    }
    
    /**
     * Sets each entry in this factor to that of the given factor.
     * @param factor
     */
    public void set(DenseFactor f) {
        if (!this.vars.equals(f.vars)) {
            throw new IllegalStateException("The varsets must be equal.");
        }
        
        for (int i=0; i<values.length; i++) {
            values[i] = f.values[i];
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Factor [\n");
        for (Var var : vars) {
            sb.append(String.format("%5s", var.getName()));
        }
        sb.append(String.format("  |  %s\n", "value"));
        for (int c=0; c<vars.calcNumConfigs(); c++) {
            int[] states = vars.getVarConfigAsArray(c);
            for (int state : states) {
                // TODO: use string names for states if available.
                sb.append(String.format("%5d", state));
            }
            sb.append(String.format("  |  %f\n", values[c]));
        }
        sb.append("]");
        return sb.toString();
    }

    /** For testing only. */
    public double[] getValues() {
        return values;
    }
    
    /* Note that Factors do not implement the standard hashCode() or equals() methods. */
    
    /** Special equals with a tolerance. */
    public boolean equals(DenseFactor other, double delta) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (this.values.length != other.values.length)
            return false;
        for (int i=0; i<values.length; i++) {
            if (!Utilities.equals(values[i], other.values[i], delta))
                return false;
        }
        if (vars == null) {
            if (other.vars != null)
                return false;
        } else if (!vars.equals(other.vars))
            return false;
        return true;
    }

    /** Gets the ID of the configuration with the maximum value. */
    public int getArgmaxConfigId() {
        return Vectors.argmax(values);
    }    
    
}