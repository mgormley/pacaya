package edu.jhu.gm;

import java.io.Serializable;

import edu.jhu.gm.Var.VarType;
import edu.jhu.util.Alphabet;

public class FeatureTemplate implements Serializable {

    private static final long serialVersionUID = -6605264098531200020L;
    
    // The number of possible assignments for the variables in this feature template.
    private int numConfigs;
    // The alphabet of observation function features.
    private Alphabet<Feature> alphabet;
    // The unique identifier for this template.
    private Object key;
    
    public FeatureTemplate(VarSet vars, Alphabet<Feature> alphabet, Object key) {
        super();
        if (VarSet.getVarsOfType(vars, VarType.OBSERVED).size() != 0) {
            throw new IllegalStateException("Only predicted and latent variables may participate in a feature template.");
        }
        this.numConfigs = vars.calcNumConfigs();
        this.alphabet = alphabet;
        this.key = key;
    }
    
    public int getNumConfigs() {
        return numConfigs;
    }
    
    public Alphabet<Feature> getAlphabet() {
        return alphabet;
    }

    public Object getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "FeatureTemplate [key=" + key + ", numConfigs=" + numConfigs + ", alphabet=" + alphabet + "]";
    }    

}
