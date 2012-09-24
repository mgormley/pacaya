package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.List;

public class BasicDmvBoundsDeltaFactory implements DmvBoundsDeltaFactory {

    private VariableSelector varSelector;
    private VariableSplitter varSplitter;
    
    public BasicDmvBoundsDeltaFactory(VariableSelector varSelector, VariableSplitter varSplitter) {
        this.varSelector = varSelector;
        this.varSplitter = varSplitter;
    }

    @Override
    public List<DmvBoundsDelta> getDmvBounds(DmvProblemNode node) {
        VariableId varId = varSelector.select(node);
        return varSplitter.split(node.getBounds(), varId);
    }

}
