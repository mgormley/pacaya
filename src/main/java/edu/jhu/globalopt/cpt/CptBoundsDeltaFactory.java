package edu.jhu.globalopt.cpt;

import java.util.List;

import edu.jhu.globalopt.dmv.DmvProblemNode;
import edu.jhu.globalopt.dmv.DmvRelaxation;
import edu.jhu.globalopt.dmv.DmvRelaxedSolution;

public interface CptBoundsDeltaFactory {

    List<CptBoundsDeltaList> getDeltas(DmvProblemNode dmvProblemNode, DmvRelaxation relax, DmvRelaxedSolution relaxSol);

}
