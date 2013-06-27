package edu.jhu.gridsearch.dmv;

import edu.jhu.gridsearch.Projector;

public interface DmvProjector extends Projector {

    DmvSolution getProjectedDmvSolution(DmvRelaxedSolution relaxSol);
    
}
