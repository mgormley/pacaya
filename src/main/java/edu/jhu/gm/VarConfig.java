package edu.jhu.gm;

import java.util.HashMap;

/**
 * A configuration of a set of variables. 
 * 
 * @author mgormley
 *
 */
public class VarConfig {

    private HashMap<Var,Integer> config;
    private VarSet vars;
    
    public VarConfig() {
        config = new HashMap<Var,Integer>();
        vars = new VarSet();
    }

    /**
     * Gets the index of this configuration for the variable set it represents.
     * 
     * This is used to provide a unique index for each setting of the the
     * variables in a VarSet.
     */
    public int getConfigIndex() {
        int configIndex = 0;
        int numStatesProd = 1;
        for (Var var : vars) {
            int state = config.get(var);
            configIndex += state * numStatesProd;
            numStatesProd *= var.getNumStates();
        }
        return configIndex;
    }
    
    /** Sets the state value to stateName for the given variable, adding it if necessary. */
    public void put(Var var, String stateName) {
        put(var, var.getState(stateName));
    }
    
    /** Sets the state value to state for the given variable, adding it if necessary. */
    public void put(Var var, int state) {
        assert (state >= 0 && state < var.getNumStates());
        config.put(var, state);
        vars.add(var);
    }
    
    /** Gets the state (in this config) for a given variable. */
    public int getState(Var var) {
        return config.get(var);
    }

    public VarSet getVars() {
        return vars;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        result = prime * result + ((vars == null) ? 0 : vars.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VarConfig other = (VarConfig) obj;
        if (config == null) {
            if (other.config != null)
                return false;
        } else if (!config.equals(other.config))
            return false;
        if (vars == null) {
            if (other.vars != null)
                return false;
        } else if (!vars.equals(other.vars))
            return false;
        return true;
    }
        
}
