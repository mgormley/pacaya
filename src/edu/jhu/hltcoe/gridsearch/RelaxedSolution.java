package edu.jhu.hltcoe.gridsearch;

public interface RelaxedSolution {
    
    RelaxStatus getStatus();
    double getScore();
    double getTrueObjectiveForRelaxedSolution();

}
