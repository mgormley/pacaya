package edu.jhu.gridsearch.cpt;

import java.util.List;

import edu.jhu.gridsearch.dmv.DmvProblemNode;
import edu.jhu.gridsearch.dmv.DmvRelaxation;
import edu.jhu.gridsearch.dmv.DmvRelaxedSolution;

public interface CptBoundsDeltaFactory {

    List<CptBoundsDeltaList> getDeltas(DmvProblemNode dmvProblemNode, DmvRelaxation relax, DmvRelaxedSolution relaxSol);

}
