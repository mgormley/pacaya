package edu.jhu.globalopt.cpt;

import java.util.List;

import edu.jhu.globalopt.dmv.DmvProblemNode;
import edu.jhu.globalopt.dmv.DmvRelaxation;
import edu.jhu.globalopt.dmv.DmvRelaxedSolution;

public class BasicCptBoundsDeltaFactory implements CptBoundsDeltaFactory {

    private VariableSelector varSelector;
    private VariableSplitter varSplitter;
    
    public BasicCptBoundsDeltaFactory(VariableSelector varSelector, VariableSplitter varSplitter) {
        this.varSelector = varSelector;
        this.varSplitter = varSplitter;
    }

    @Override
    public List<CptBoundsDeltaList> getDeltas(DmvProblemNode node, DmvRelaxation relax, DmvRelaxedSolution relaxSol) {
        VariableId varId = varSelector.select(node, relax, relaxSol);
        return varSplitter.split(relax.getBounds(), varId);
    }

}
