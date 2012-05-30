package edu.jhu.hltcoe.gridsearch.dmv;

import java.io.File;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.BfsComparator;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation.CutCountComputer;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.train.Trainer;

public class BnBDmvTrainer implements Trainer {

    private LazyBranchAndBoundSolver bnbSolver;
    private double epsilon;
    private File tempDir;
    private DmvBoundsDeltaFactory brancher;
    private DmvProblemNode rootNode;
    private DmvRelaxation relax;
    
    public BnBDmvTrainer(double epsilon, DmvBoundsDeltaFactory brancher) {
        this(epsilon, brancher, null, null);
    }
    
    public BnBDmvTrainer(double epsilon, DmvBoundsDeltaFactory brancher, File tempDir) {
        this(epsilon, brancher, null, tempDir);
    }
    
    public BnBDmvTrainer(double epsilon, DmvBoundsDeltaFactory brancher, DmvRelaxation relax) {
        this(epsilon, brancher, relax, null);
    }
    
    public BnBDmvTrainer(double epsilon, DmvBoundsDeltaFactory brancher, DmvRelaxation relax, File tempDir) {
        this.epsilon = epsilon; 
        this.bnbSolver = new LazyBranchAndBoundSolver();
        this.brancher = brancher;
        if (relax == null) {
            relax = new DmvDantzigWolfeRelaxation(null, 100, new CutCountComputer());
        }
        this.relax = relax;
        this.tempDir = tempDir;
    }

    @Override
    public void train(SentenceCollection sentences) {
        init(sentences);
        train();
    }
    
    public void init(SentenceCollection sentences) {
        rootNode = new DmvProblemNode(sentences, brancher, relax, tempDir);
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

    public DmvRelaxation getRootRelaxation() {
        return rootNode.getRelaxation();
    }

}
