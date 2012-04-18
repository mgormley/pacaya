package edu.jhu.hltcoe.gridsearch.dmv;

import java.io.File;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.BfsComparator;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.train.Trainer;

public class BnBDmvTrainer implements Trainer {

    private LazyBranchAndBoundSolver bnbSolver;
    private double epsilon;
    private File tempDir;
    
    public BnBDmvTrainer(double epsilon) {
        this(epsilon, null);
    }
    
    public BnBDmvTrainer(double epsilon, File tempDir) {
        this.epsilon = epsilon; 
        bnbSolver = new LazyBranchAndBoundSolver();
        this.tempDir = tempDir;
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        DmvProblemNode rootNode = new DmvProblemNode(sentences, tempDir);
        bnbSolver.runBranchAndBound(rootNode, epsilon, new BfsComparator());
    }
    
    @Override
    public Model getModel() {
        DmvSolution solution = (DmvSolution) bnbSolver.getIncumbentSolution();
        // Create a new DmvModel from these model parameters
        return solution.getIdm().getDmvModel(solution.getLogProbs());
    }

}
