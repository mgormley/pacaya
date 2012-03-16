package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.EagerBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.DfsBfcComparator;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.train.Trainer;

public class BnBDmvTrainer implements Trainer {

    private EagerBranchAndBoundSolver bnbSolver;
    private double epsilon;
    
    public BnBDmvTrainer(double epsilon) {
        this.epsilon = epsilon; 
        bnbSolver = new EagerBranchAndBoundSolver();
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        DmvBounds rootBounds = new DmvBounds();
        DmvBoundsFactory boundsFactory = new FixedDmvBoundsFactory(sentences);
        DmvProblemNode rootNode = new DmvProblemNode(sentences, rootBounds, boundsFactory);
        bnbSolver.runBranchAndBound(rootNode, epsilon, new DfsBfcComparator());
    }
    
    @Override
    public Model getModel() {
        DmvSolution solution = (DmvSolution) bnbSolver.getIncumbentSolution();
        return solution.getDmvModel();
    }

}
