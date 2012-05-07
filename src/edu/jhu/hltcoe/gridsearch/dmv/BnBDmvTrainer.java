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
    private DmvBoundsDeltaFactory brancher;
    private DmvProblemNode rootNode;
    
    public BnBDmvTrainer(double epsilon, DmvBoundsDeltaFactory brancher) {
        this(epsilon, brancher, null);
    }
    
    public BnBDmvTrainer(double epsilon, DmvBoundsDeltaFactory brancher, File tempDir) {
        this.epsilon = epsilon; 
        this.bnbSolver = new LazyBranchAndBoundSolver();
        this.tempDir = tempDir;
        this.brancher = brancher;
    }

    @Override
    public void train(SentenceCollection sentences) {
        init(sentences);
        train();
    }
    
    public void init(SentenceCollection sentences) {
        rootNode = new DmvProblemNode(sentences, brancher, tempDir);
    }
    
    public void train() {
        bnbSolver.runBranchAndBound(rootNode, epsilon, new BfsComparator());
    }
    
    @Override
    public Model getModel() {
        DmvSolution solution = (DmvSolution) bnbSolver.getIncumbentSolution();
        // Create a new DmvModel from these model parameters
        return solution.getIdm().getDmvModel(solution.getLogProbs());
    }

    public DmvDantzigWolfeRelaxation getRootRelaxation() {
        return rootNode.getRelaxation();
    }

}
