package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.DfsBfcComparator;
import edu.jhu.hltcoe.gridsearch.EagerBranchAndBoundSolver;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.train.Trainer;
import edu.jhu.hltcoe.train.ViterbiTrainer;

public class BnBDmvTrainer implements Trainer {

    private EagerBranchAndBoundSolver bnbSolver;
    private double epsilon;
    
    public BnBDmvTrainer(double epsilon) {
        this.epsilon = epsilon; 
        bnbSolver = new EagerBranchAndBoundSolver();
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        DepTreebank treebank = getInitFeasSol(sentences);
        DmvProblemNode rootNode = new DmvProblemNode(sentences, treebank);
        bnbSolver.runBranchAndBound(rootNode, epsilon, new DfsBfcComparator());
    }
    
    /**
     * package private for testing
     */
    DepTreebank getInitFeasSol(SentenceCollection sentences) {
        // Run Viterbi EM to get a reasonable starting incumbent solution
        double lambda = 0.1;
        int iterations = 25;
        ViterbiParser parser = new DmvCkyParser();
        DmvMStep mStep = new DmvMStep(lambda);
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        ViterbiTrainer trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, 0.99999);
        // TODO: use random restarts
        trainer.train(sentences);
        return trainer.getCounts();
    }

    @Override
    public Model getModel() {
        DmvSolution solution = (DmvSolution) bnbSolver.getIncumbentSolution();
        // Create a new DmvModel from these model parameters
        return solution.getIdm().getDmvModel(solution.getLogProbs());
    }

}
