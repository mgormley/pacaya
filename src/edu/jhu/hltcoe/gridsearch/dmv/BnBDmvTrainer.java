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
    private DmvBoundsDeltaFactory brancher;
    private DmvProblemNode rootNode;
    private DmvRelaxation relax;
    
    public BnBDmvTrainer(double epsilon, DmvBoundsDeltaFactory brancher, double timeoutSeconds) {
        this(epsilon, brancher, new DmvDantzigWolfeRelaxation(null, 100, new CutCountComputer()), timeoutSeconds);
    }
    
    public BnBDmvTrainer(double epsilon, DmvBoundsDeltaFactory brancher, DmvRelaxation relax, double timeoutSeconds) {
        this.bnbSolver = new LazyBranchAndBoundSolver(epsilon, new BfsComparator(), timeoutSeconds);
        this.brancher = brancher;
        this.relax = relax;
    }

    @Override
    public void train(SentenceCollection sentences) {
        init(sentences);
        train();
    }
    
    public void init(SentenceCollection sentences) {
        rootNode = new DmvProblemNode(sentences, brancher, relax);
    }
    
    public void train() {
        bnbSolver.runBranchAndBound(rootNode);
        rootNode.end();
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
