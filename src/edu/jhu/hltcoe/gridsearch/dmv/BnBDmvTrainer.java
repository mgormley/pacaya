package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.Comparator;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.gridsearch.DmvLazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.train.TrainCorpus;
import edu.jhu.hltcoe.train.Trainer;

public class BnBDmvTrainer implements Trainer<DepTreebank> {

    private LazyBranchAndBoundSolver bnbSolver;
    private DmvBoundsDeltaFactory brancher;
    private DmvProblemNode rootNode;
    private DmvRelaxation relax;

    public BnBDmvTrainer(double epsilon, DmvBoundsDeltaFactory brancher, DmvRelaxation relax, double timeoutSeconds,
            DependencyParserEvaluator evaluator, Comparator<ProblemNode> leafComparator) {
        this.bnbSolver = new DmvLazyBranchAndBoundSolver(epsilon, leafComparator, timeoutSeconds, evaluator);
        this.brancher = brancher;
        this.relax = relax;
    }

    @Override
    public void train(TrainCorpus corpus) {
        init((DmvTrainCorpus)corpus);
        train();
    }
    
    public void init(DmvTrainCorpus corpus) {
        rootNode = new DmvProblemNode(corpus, brancher, relax);
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
