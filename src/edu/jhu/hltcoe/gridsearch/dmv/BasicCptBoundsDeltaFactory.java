package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.List;

public class BasicCptBoundsDeltaFactory implements CptBoundsDeltaFactory {

    private VariableSelector varSelector;
    private VariableSplitter varSplitter;
    
    public BasicCptBoundsDeltaFactory(VariableSelector varSelector, VariableSplitter varSplitter) {
        this.varSelector = varSelector;
        this.varSplitter = varSplitter;
    }

    @Override
    public List<CptBoundsDelta> getDmvBounds(DmvProblemNode node) {
        VariableId varId = varSelector.select(node);
        return varSplitter.split(node.getBounds(), varId);
    }

}
