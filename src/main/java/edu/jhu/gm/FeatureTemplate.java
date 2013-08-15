package edu.jhu.gm;

import java.io.Serializable;

import edu.jhu.gm.Var.VarType;
import edu.jhu.util.Alphabet;

public class FeatureTemplate implements Serializable {

    private static final long serialVersionUID = -6605264098531200020L;
    
    // The predicted and latent variables in this feature template.
    private VarSet vars;
    // The alphabet of observation function features.
    private Alphabet<Feature> alphabet;
    // The unique name for this template.
    private String name;
    
    public FeatureTemplate(VarSet vars, Alphabet<Feature> alphabet, String name) {
        super();
        if (VarSet.getVarsOfType(vars, VarType.OBSERVED).size() != 0) {
            throw new IllegalStateException("Only predicted and latent variables may participate in a feature template.");
        }
        this.vars = vars;
        this.alphabet = alphabet;
        this.name = name;
    }
    
    public VarSet getVars() {
        return vars;
    }
    
    public Alphabet<Feature> getAlphabet() {
        return alphabet;
    }

    public String getName() {
        return name;
    }    
    
}
