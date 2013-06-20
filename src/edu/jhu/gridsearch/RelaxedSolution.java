package edu.jhu.gridsearch;

public interface RelaxedSolution {
    
    RelaxStatus getStatus();
    double getScore();
    double getTrueObjectiveForRelaxedSolution();
    void setScore(double score);

}
