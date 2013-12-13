package edu.jhu.globalopt.dmv;

import edu.jhu.globalopt.Projector;

public interface DmvProjector extends Projector {

    DmvSolution getProjectedDmvSolution(DmvRelaxedSolution relaxSol);
    
}
