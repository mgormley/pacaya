package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.util.IntTuple;

public class VariableId extends IntTuple {

    private boolean hasVar;
    
    /**
     * Constructor for a single variable.
     */
    public VariableId(int... args) {
        super(args);
        hasVar = true;
    }
    
    /**
     * Constructor for no variable.
     */
    public VariableId() {
        hasVar = false;
    }

    public boolean hasVar() {
        return hasVar;
    }

}
