package edu.jhu.hltcoe.gridsearch.cpt;

import java.util.List;

import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDmvSolution;

public interface CptBoundsDeltaFactory {

    List<CptBoundsDeltaList> getDeltas(DmvProblemNode dmvProblemNode, DmvRelaxation relax, RelaxedDmvSolution relaxSol);

}
