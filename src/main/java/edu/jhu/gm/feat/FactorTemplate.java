package edu.jhu.gm.feat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.util.FeatureNames;

public class FactorTemplate implements Serializable {

    private static final long serialVersionUID = -6605264098531200020L;
    
    // The number of possible assignments for the variables in this feature template.
    private int numConfigs;
    // The alphabet of observation function features.
    private FeatureNames alphabet;
    // The unique identifier for this template.
    private Object key;
    // The first VarSet used to construct this template. These vars are NOT
    // representative of the template, but the state names of the variables are.
    // Accordingly, we use this to construct the state names for a given configuration.
    private VarSet vars;
    
    public FactorTemplate(VarSet vars, FeatureNames alphabet, Object key) {
        super();
        if (VarSet.getVarsOfType(vars, VarType.OBSERVED).size() != 0) {
            throw new IllegalStateException("Only predicted and latent variables may participate in a feature template.");
        }
        this.numConfigs = vars.calcNumConfigs();
        this.alphabet = alphabet;
        this.key = key;

        // Copy the input vars since we'll want to serialize these.
        Var[] tmpVars = new Var[vars.size()];
        int i=0; 
        for (Var v : vars) {
            tmpVars[i++] = new Var(v.getType(), v.getNumStates(), v.getName(), v.getStateNames());
        }
        this.vars = new VarSet(tmpVars);
    }
    
    public int getNumConfigs() {
        return numConfigs;
    }
    
    public FeatureNames getAlphabet() {
        return alphabet;
    }

    public Object getKey() {
        return key;
    }
    
    public List<String> getStateNames(int configId) {
        List<String> states = new ArrayList<String>(vars.size());
        VarConfig vc = vars.getVarConfig(configId);
        for (Var v : vars) {
            states.add(vc.getStateName(v));
        }
        return states;
    }

    public String getStateNamesStr(int configId) {
        return StringUtils.join(getStateNames(configId), "_");
    }
    
    @Override
    public String toString() {
        return "FeatureTemplate [key=" + key + ", numConfigs=" + numConfigs + ", alphabet=" + alphabet + "]";
    }
    
    // Only for debugging.
    VarSet getVars() {
        return vars;
    }

}
