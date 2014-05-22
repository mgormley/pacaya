package edu.jhu.gm.model;

import java.io.Serializable;
import java.util.Arrays;

import edu.jhu.prim.Primitives;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.arrays.Multinomials;
import edu.jhu.prim.iter.IntIter;
import edu.jhu.prim.util.Lambda;
import edu.jhu.prim.util.Lambda.LambdaBinOpDouble;
import edu.jhu.prim.util.math.FastMath;

/**
 * A multivariate Multinomial distribution. 
 * 
 * @author mgormley
 *
 */
public class VarTensor implements Serializable {

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
    public VarTensor(VarSet vars) {
        this(vars, 0.0);
    }
    
    /**
     * Constructs a factor where each value is set to some initial value.
     * @param vars The variable set.
     * @param initialValue The initial value.
     */
    public VarTensor(VarSet vars, double initialValue) {
        this.vars = vars;
        int numConfigs = vars.calcNumConfigs();
        this.values = new double[numConfigs];
        Arrays.fill(values, initialValue);
    }
    
    /** Copy constructor. */
    public VarTensor(VarTensor f) {
        this.vars = f.vars;
        this.values = DoubleArrays.copyOf(f.values);
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
    public VarTensor getMarginal(VarSet vars, boolean normalize) {
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
    public VarTensor getLogMarginal(VarSet vars, boolean normalize) {
        return getMarginal(vars, normalize, true);
    }
    
    private VarTensor getMarginal(VarSet vars, boolean normalize, boolean logDomain) {
        VarSet margVars = new VarSet(this.vars);
        margVars.retainAll(vars);
        
        VarTensor marg = new VarTensor(margVars, logDomain ? Double.NEGATIVE_INFINITY : 0.0);
        if (margVars.size() == 0) {
            return marg;
        }
        
        IntIter iter = margVars.getConfigIter(this.vars);
        for (int i=0; i<this.values.length; i++) {
            int j = iter.next();
            if (logDomain) {
                marg.values[j] = FastMath.logAdd(marg.values[j], this.values[i]);
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
    
    public VarTensor getClamped(VarConfig clmpVarConfig) {
        if (clmpVarConfig.size() == 0) {
            return new VarTensor(this);
        }
        VarSet clmpVars = clmpVarConfig.getVars();
        VarSet unclmpVars = new VarSet(this.vars);
        unclmpVars.removeAll(clmpVars); 

        VarTensor clmp = new VarTensor(unclmpVars);
        IntIter iter = IndexForVc.getConfigIter(this.vars, clmpVarConfig);
        
        if (clmp.values.length > 0) {
            for (int c=0; c<clmp.values.length; c++) {
                int config = iter.next();
                clmp.values[c] = this.values[config];
            }
        }
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
        DoubleArrays.add(values, addend);
    }
    
    /** Scale each value by lambda. */
    public void scale(double lambda) {
        DoubleArrays.scale(values, lambda);
    }
    
    /** Normalizes the values. */
    public double normalize() {
        return Multinomials.normalizeProps(values);
    }

    /** Normalizes the values. */
    public double logNormalize() {
        return Multinomials.normalizeLogProps(values);
    }
    
    /** Takes the log of each value. */
    public void convertRealToLog() {
        DoubleArrays.log(values);
    }
    
    /** Takes the exp of each value. */
    public void convertLogToReal() {
        DoubleArrays.exp(values);
    }

    /** Gets the sum of the values for this factor. */
    public double getSum() {
        return DoubleArrays.sum(values);
    }
    
    /** Gets the log of the sum of the values for this factor. */
    public double getLogSum() {
        return DoubleArrays.logSum(values);
    }
    
    /**
     * Adds a factor to this one.
     * 
     * From libDAI: 
     *  The sum of two factors is defined as follows: if
     *  \f$f : \prod_{l\in L} X_l \to [0,\infty)\f$ and \f$g : \prod_{m\in M} X_m \to [0,\infty)\f$, then
     *  \f[f+g : \prod_{l\in L\cup M} X_l \to [0,\infty) : x \mapsto f(x_L) + g(x_M).\f]
     */
    public void add(VarTensor f) {
        VarTensor newFactor = applyBinOp(this, f, new Lambda.DoubleAdd());
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
    public void prod(VarTensor f) {
        VarTensor newFactor = applyBinOp(this, f, new Lambda.DoubleProd());
        this.vars = newFactor.vars;
        this.values = newFactor.values;  
    }
    
    /**
     * this /= f
     * indices matching 0 /= 0 are set to 0.
     */
    public void divBP(VarTensor f) {
    	VarTensor newFactor = applyBinOp(this, f, new Lambda.DoubleDivBP());
        this.vars = newFactor.vars;
        this.values = newFactor.values;
    }
    
    /**
     * this -= f
     * indices matching (-Infinity) -= (-Infinity) are set to 0.
     */
    public void subBP(VarTensor f) {
    	VarTensor newFactor = applyBinOp(this, f, new Lambda.DoubleSubtractBP());
        this.vars = newFactor.vars;
        this.values = newFactor.values;
    }
    
    /**
     * Log-adds a factor to this one.
     * 
     * This is analogous to factor addition, except that the logAdd operator
     * is used instead.
     */
    public void logAdd(VarTensor f) {
        VarTensor newFactor = applyBinOp(this, f, new Lambda.DoubleLogAdd());
        this.vars = newFactor.vars;
        this.values = newFactor.values;  
    }
    
    /**
     * Applies the binary operator to factors f1 and f2.
     * 
     * This method will opt to be destructive on f1 (returning it instead of a
     * new factor) if time/space can be saved by doing so.
     * 
     * Note: destructive if necessary.
     * 
     * @param f1 The first factor. (returned if it will save time/space)
     * @param f2 The second factor.
     * @param op The binary operator.
     * @return The new factor.
     */
    private static VarTensor applyBinOp(final VarTensor f1, final VarTensor f2, final LambdaBinOpDouble op) {
        if (f1.vars.size() == 0) {
            // Return a copy of f2.
            return new VarTensor(f2);
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
            int n = f1.vars.calcNumConfigs();
            for (int c = 0; c < n; c++) {
                f1.values[c] = op.call(f1.values[c], f2.values[iter2.next()]);
            }
            assert(!iter2.hasNext());
            return f1;
        } else {
            // The union of the two variable sets must be created.
            VarSet union = new VarSet(f1.vars, f2.vars);
            VarTensor out = new VarTensor(union);
            IntIter iter1 = f1.vars.getConfigIter(union);
            IntIter iter2 = f2.vars.getConfigIter(union);
            int n = out.vars.calcNumConfigs();
            for (int c = 0; c < n; c++) {
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
    public void set(VarTensor f) {
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
    
    public boolean containsBadValues(boolean logDomain) {
    	for(int i=0; i<values.length; i++) {
    		if(Double.isNaN(values[i]))
    			return true;
    		if(!logDomain && (values[i] < 0d || Double.isInfinite(values[i])))
    			return true;
    	}
    	return false;
    }
    
    /* Note that Factors do not implement the standard hashCode() or equals() methods. */
    
    /** Special equals with a tolerance. */
    public boolean equals(VarTensor other, double delta) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (this.values.length != other.values.length)
            return false;
        for (int i=0; i<values.length; i++) {
            if (!Primitives.equals(values[i], other.values[i], delta))
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
        return DoubleArrays.argmax(values);
    }    
    
    public int size() {
        return values.length;
    }

    /**
     * Gets the infinity norm of this tensor. Defined as the maximum absolute
     * value of the entries.
     */
    public double getInfNorm() {
        double maxAbs = Double.NEGATIVE_INFINITY;
        for (int c=0; c<values.length; c++) {
            double abs = Math.abs(values[c]);
            if (abs > maxAbs) {
                maxAbs = abs;
            }
        }
        return maxAbs;
    }

    /** Computes the sum of the entries of the pointwise product of two tensors with identical domains. */
    public double dotProduct(VarTensor other) {
        if (!this.vars.equals(other.vars)) {
            throw new IllegalArgumentException("Tensors must have identical domains");
        }
        double sum = 0;
        for (int c=0; c<this.values.length; c++) {
            sum += this.values[c] * other.values[c];
        }
        return sum;
    }
    
}