package edu.jhu.hltcoe.gridsearch.cpt;

import java.util.List;

import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDmvSolution;

public class BasicCptBoundsDeltaFactory implements CptBoundsDeltaFactory {

    private VariableSelector varSelector;
    private VariableSplitter varSplitter;
    
    public BasicCptBoundsDeltaFactory(VariableSelector varSelector, VariableSplitter varSplitter) {
        this.varSelector = varSelector;
        this.varSplitter = varSplitter;
    }

    @Override
    public List<CptBoundsDeltaList> getDeltas(DmvProblemNode node, DmvRelaxation relax, RelaxedDmvSolution relaxSol) {
        VariableId varId = varSelector.select(node, relax, relaxSol);
        return varSplitter.split(relax.getBounds(), varId);
    }

}
