package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.gridsearch.Projector;

public interface DmvProjector extends Projector {

    DmvSolution getProjectedDmvSolution(RelaxedDmvSolution relaxSol);
    
}
