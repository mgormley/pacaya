package edu.jhu.autodiff.erma;

import edu.jhu.gm.model.VarTensor;

/** Struct for beliefs (i.e. approximate marginals) of a factor graph. */
public class Beliefs {
    
    public VarTensor[] varBeliefs;
    public VarTensor[] facBeliefs;

    public Beliefs() {
    }

    public Beliefs(VarTensor[] varBeliefs, VarTensor[] facBeliefs) {
        this.varBeliefs = varBeliefs;
        this.facBeliefs = facBeliefs;
    }

    public Beliefs copy() {
        Beliefs clone = new Beliefs();
        clone.varBeliefs = new VarTensor[this.varBeliefs.length];
        clone.facBeliefs = new VarTensor[this.facBeliefs.length];
        for (int v = 0; v < varBeliefs.length; v++) {
            clone.varBeliefs[v] = new VarTensor(this.varBeliefs[v]);
        }
        for (int a = 0; a < facBeliefs.length; a++) {
            clone.facBeliefs[a] = new VarTensor(this.facBeliefs[a]);
        }
        return clone;
    }

    public void fill(double val) {
        for (int v = 0; v < varBeliefs.length; v++) {
            varBeliefs[v].fill(val);
        }
        for (int a = 0; a < facBeliefs.length; a++) {
            facBeliefs[a].fill(val);
        }
    }

    public Beliefs copyAndFill(double val) {
        Beliefs clone = copy();
        clone.fill(val);
        return clone;
    }
    
}