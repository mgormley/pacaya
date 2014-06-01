package edu.jhu.autodiff.erma;

import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.semiring.Algebra;

/** Struct for beliefs (i.e. approximate marginals) of a factor graph. */
public class Beliefs {
    
    public VarTensor[] varBeliefs;
    public VarTensor[] facBeliefs;
    public Algebra s;
    
    public Beliefs(Algebra s) {
        this.s = s;
    }

    public Beliefs(VarTensor[] varBeliefs, VarTensor[] facBeliefs) {
        this.varBeliefs = varBeliefs;
        this.facBeliefs = facBeliefs;
    }

    public Beliefs copy() {
        Beliefs clone = new Beliefs(s);
        clone.varBeliefs = copyOfVarTensorArray(this.varBeliefs);
        clone.facBeliefs = copyOfVarTensorArray(this.facBeliefs);
        return clone;
    }

    private static VarTensor[] copyOfVarTensorArray(VarTensor[] orig) {
        if (orig == null) {
            return null;
        }
        VarTensor[] clone = new VarTensor[orig.length];
        for (int v = 0; v < clone.length; v++) {
            if (orig[v] != null) {
                clone[v] = new VarTensor(orig[v]);
            }
        }
        return clone;
    }

    public void fill(double val) {
        fillVarTensorArray(varBeliefs, val);
        fillVarTensorArray(facBeliefs, val);
    }

    private static void fillVarTensorArray(VarTensor[] beliefs, double val) {
        if (beliefs != null) {
            for (int i = 0; i < beliefs.length; i++) {
                if (beliefs[i] != null) {
                    beliefs[i].fill(val);
                }
            }
        }
    }

    public Beliefs copyAndFill(double val) {
        Beliefs clone = copy();
        clone.fill(val);
        return clone;
    }

    public int size() {
        return count(varBeliefs) + count(facBeliefs);
    }

    private static int count(VarTensor[] beliefs) {
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
    
}