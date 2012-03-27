package edu.jhu.hltcoe.gridsearch.dmv;

import java.io.File;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.BfsComparator;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelConverter;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.train.Trainer;
import edu.jhu.hltcoe.train.ViterbiTrainer;

public class BnBDmvTrainer implements Trainer {

    private LazyBranchAndBoundSolver bnbSolver;
    private double epsilon;
    private File tempDir;
    
    public BnBDmvTrainer(double epsilon) {
        this.epsilon = epsilon; 
        bnbSolver = new LazyBranchAndBoundSolver();
    }
    
    @Override
    public void train(SentenceCollection sentences) {
        DmvSolution initSol = getInitFeasSol(sentences);
        DmvProblemNode rootNode = new DmvProblemNode(sentences, initSol, tempDir);
        bnbSolver.runBranchAndBound(rootNode, epsilon, new BfsComparator());
    }
    
    /**
     * package private for testing
     */
    DmvSolution getInitFeasSol(SentenceCollection sentences) {
        // Run Viterbi EM to get a reasonable starting incumbent solution
        double lambda = 0.1;
        int iterations = 25;
        ViterbiParser parser = new DmvCkyParser();
        DmvMStep mStep = new DmvMStep(lambda);
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        ViterbiTrainer trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, 0.99999, 10);
        // TODO: use random restarts
        trainer.train(sentences);
        
        DepTreebank treebank = trainer.getCounts();
        IndexedDmvModel idm = new IndexedDmvModel(sentences);
        DepProbMatrix dpm = DmvModelConverter.getDepProbMatrix((DmvModel)trainer.getModel(), sentences.getLabelAlphabet());
        double[][] logProbs = idm.getCmLogProbs(dpm);
        
        // We let the DmvProblemNode compute the score
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, Double.NaN);
        return sol;
    }

    @Override
    public Model getModel() {
        DmvSolution solution = (DmvSolution) bnbSolver.getIncumbentSolution();
        // Create a new DmvModel from these model parameters
        return solution.getIdm().getDmvModel(solution.getLogProbs());
    }

    public void setTempDir(File tempDir) {
        this.tempDir = tempDir;
    }

}
