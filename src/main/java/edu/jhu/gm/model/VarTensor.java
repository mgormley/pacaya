package edu.jhu.gm.model;

import java.io.Serializable;

import edu.jhu.autodiff.Tensor;
import edu.jhu.prim.iter.IntIter;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.AlgebraLambda;

/**
 * A multivariate Multinomial distribution.
 * 
 * @author mgormley
 *
 */
public class VarTensor extends Tensor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** All variables without an id are given this value. */
    public static final int UNINITIALIZED_NODE_ID = -1;

    /** The set of variables in this factor. */
    private VarSet vars;

    /** Constructs a factor initializing the values to 0.0. 
     * @param s TODO*/
    public VarTensor(Algebra s, VarSet vars) {
        this(s, vars, s.zero());
    }

    /**
     * Constructs a factor where each value is set to some initial value.
     * @param s TODO
     * @param vars The variable set.
     * @param initialValue The initial value.
     */
    public VarTensor(Algebra s, VarSet vars, double initialValue) {
        super(s, getDims(vars));
        this.vars = vars;
        this.fill(initialValue);
    }

    /** Copy constructor. */
    public VarTensor(VarTensor other) {
        super(other);
        this.vars = other.vars;
    }

    /**
     * Gets the tensor dimensions: dimension i corresponds to the i'th variable, and the size of
     * that dimension is the number of states for the variable.
     */
    private static int[] getDims(VarSet vars) {
        int[] dims = new int[vars.size()];
        for (int i=0; i<vars.size(); i++) {
            dims[i] = vars.get(i).getNumStates();
        }
        return dims;
    }

    /**
     * Gets the marginal distribution over a subset of the variables in this
     * factor, optionally normalized.
     * 
     * @param vars The subset of variables for the marginal distribution. This will sum over all variables not in this set.
     * @param normalize Whether to normalize the resulting distribution.
     * @return The marginal distribution.
     */
    public VarTensor getMarginal(VarSet vars, boolean normalize) {
        VarSet margVars = new VarSet(this.vars);
        margVars.retainAll(vars);
        
        VarTensor marg = new VarTensor(s, margVars, s.zero());
        if (margVars.size() == 0) {
            return marg;
        }
        
        IntIter iter = margVars.getConfigIter(this.vars);
        for (int i=0; i<this.values.length; i++) {
            int j = iter.next();
            marg.values[j] = s.plus(marg.values[j], this.values[i]);
        }
        
        if (normalize) {
            marg.normalize();
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

        VarTensor clmp = new VarTensor(s, unclmpVars);
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
     * Adds a factor to this one.
     * 
     * From libDAI: 
     *  The sum of two factors is defined as follows: if
     *  \f$f : \prod_{l\in L} X_l \to [0,\infty)\f$ and \f$g : \prod_{m\in M} X_m \to [0,\infty)\f$, then
     *  \f[f+g : \prod_{l\in L\cup M} X_l \to [0,\infty) : x \mapsto f(x_L) + g(x_M).\f]
     */
    public void add(VarTensor f) {
        VarTensor newFactor = applyBinOp(this, f, new AlgebraLambda.Add());
        internalSet(newFactor);      
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
        VarTensor newFactor = applyBinOp(this, f, new AlgebraLambda.Prod());
        internalSet(newFactor);  
    }
    
    /**
     * this /= f
     * indices matching 0 /= 0 are set to 0.
     */
    public void divBP(VarTensor f) {
    	VarTensor newFactor = applyBinOp(this, f, new AlgebraLambda.DivBP());
        internalSet(newFactor);
    }

    /** This set method is used to internally update ALL the fields. */
    private void internalSet(VarTensor newFactor) {
        this.vars = newFactor.vars;
        this.dims = newFactor.dims;
        this.strides = newFactor.strides;
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
    private static VarTensor applyBinOp(final VarTensor f1, final VarTensor f2, final AlgebraLambda.LambdaBinOp op) {
        checkSameAlgebra(f1, f2);
        Algebra s = f1.s;
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
                f1.values[c] = op.call(s, f1.values[c], f2.values[c]);
            }
            return f1;
        } else if (f1.vars.isSuperset(f2.vars)) {
            // Special case where f1 is a superset of f2.
            IntIter iter2 = f2.vars.getConfigIter(f1.vars);
            int n = f1.vars.calcNumConfigs();
            for (int c = 0; c < n; c++) {
                f1.values[c] = op.call(s, f1.values[c], f2.values[iter2.next()]);
            }
            assert(!iter2.hasNext());
            return f1;
        } else {
            // The union of the two variable sets must be created.
            VarSet union = new VarSet(f1.vars, f2.vars);
            VarTensor out = new VarTensor(s, union);
            IntIter iter1 = f1.vars.getConfigIter(union);
            IntIter iter2 = f2.vars.getConfigIter(union);
            int n = out.vars.calcNumConfigs();
            for (int c = 0; c < n; c++) {
                out.values[c] = op.call(s, f1.values[iter1.next()], f2.values[iter2.next()]);
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
    // TODO: Is this called, if so, it should become setValuesOnly.
    public void set(VarTensor f) {
        if (!this.vars.equals(f.vars)) {
            throw new IllegalStateException("The varsets must be equal.");
        }
        super.setValuesOnly(f);
    }

    public VarTensor copyAndConvertAlgebra(Algebra newS) {
        VarTensor t = new VarTensor(newS, this.vars);
        t.setFromDiffAlgebra(this);
        return t;
    }

    @Override
    public String toString() {
        return toString(false);
    }
    
    /** Returns a string representation of the VarTensor, which (optionally) excludes zero-valued rows. */
    public String toString(boolean sparse) {
        StringBuilder sb = new StringBuilder();
        sb.append("VarTensor [\n");
        for (Var var : vars) {
            String name = var.getName();
            // Take the 5-char suffix if the variable name is too long.
            name = name.substring(Math.max(0, name.length()-5));
            sb.append(String.format("%6s", name));
        }
        sb.append(String.format("  |  %s\n", "value"));
        for (int c=0; c<vars.calcNumConfigs(); c++) {
            if (!sparse || values[c] != 0.0) {
                int[] states = vars.getVarConfigAsArray(c);
                for (int state : states) {
                    // TODO: use string names for states if available.
                    sb.append(String.format("%6d", state));
                }
                sb.append(String.format("  |  %g\n", values[c]));
            }
        }
        sb.append("]");
        return sb.toString();
    }
        
    // TODO: Move this to BeliefPropagation.java.
    public boolean containsBadValues() {
    	for(int i=0; i<values.length; i++) {
    		if(s.isNaN(values[i])) {
    			return true;
    		}
    		if (s.lt(values[i], s.zero()) || values[i] == s.posInf()) {
    			return true;
    		}
    	}
    	return false;
    }
    
    /* Note that VarTensors do not implement the standard hashCode() or equals() methods. */
    
    /** Special equals with a tolerance. */
    public boolean equals(VarTensor other, double delta) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (!super.equals(other, delta)) 
            return false;
        if (vars == null) {
            if (other.vars != null)
                return false;
        } else if (!vars.equals(other.vars))
            return false;
        return true;
    }

    /** Takes the log of each value. */
    public void convertRealToLog() {
        this.log();
    }
    
}