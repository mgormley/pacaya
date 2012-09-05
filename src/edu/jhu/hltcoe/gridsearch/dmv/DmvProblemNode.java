package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelConverter;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.model.dmv.DmvWeightCopier;
import edu.jhu.hltcoe.model.dmv.SmoothedDmvWeightCopier;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.train.ViterbiTrainer;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;

public class DmvProblemNode implements ProblemNode {

    private static Logger log = Logger.getLogger(DmvProblemNode.class);

    private static AtomicInteger atomicIntId = new AtomicInteger(0);

    private int id;
    private DmvProblemNode parent;
    private int depth;
    private int side;
    private DmvBoundsDelta deltas;
    
    // For active node only:
    private DmvBoundsDeltaFactory deltasFactory;
    private DmvRelaxation dwRelax;
    protected boolean isOptimisticBoundCached;
    protected double optimisticBound;
    private SentenceCollection sentences;
    private RelaxedDmvSolution relaxSol;
    
    // For "fork" nodes only (i.e. nodes that store warm-start information)
    private WarmStart warmStart;
    
    // For root node only
    private DmvSolution initFeasSol;

    private static DmvProblemNode activeNode;

    /**
     * Root node constructor
     */
    public DmvProblemNode(SentenceCollection sentences, DmvBoundsDeltaFactory brancher, DmvRelaxation dwRelax) {
        this.sentences = sentences;
        this.dwRelax = dwRelax;
        this.dwRelax.setSentences(sentences);
        // Save and use this solution as the first incumbent
        this.initFeasSol = getInitFeasSol(sentences);
        log.info("Initial solution score: " + initFeasSol.getScore());
        this.id = getNextId();
        this.parent = null;
        this.depth = 0;
        this.side = 0;
        this.deltasFactory = brancher;
        this.isOptimisticBoundCached = false;
        
        this.dwRelax.init(initFeasSol);
        
        if (activeNode != null) {
            throw new IllegalStateException("Multiple trees not allowed");
        }
        setAsActiveNode();
    }

    /**
     * Non-root node constructor
     */
    public DmvProblemNode(DmvBoundsDelta deltas, DmvBoundsDeltaFactory deltasFactory, DmvProblemNode parent, int side) {
        this.deltas = deltas;
        this.deltasFactory = deltasFactory;
        this.id = getNextId();
        this.parent = parent;
        this.depth = parent.depth + 1;
        this.side = side;
        isOptimisticBoundCached = false;
        // Take the warm start information from the parent
        this.warmStart = parent.warmStart;
        // The relaxation is set only when this node is set to be the active one
        this.dwRelax = null;
        this.sentences = parent.sentences;
    }
    

    /**
     * For testing only
     */
    protected DmvProblemNode() {
        
    }

    public RelaxedDmvSolution getRelaxedSolution() {
        return relaxSol;
    }
    
    /**
     * @return negative infinity if the problem is infeasible, and an upper
     *         bound otherwise
     */
    @Override
    public double getOptimisticBound() {
        return getOptimisticBound(LazyBranchAndBoundSolver.WORST_SCORE);
    }

    /**
     * @return negative infinity if the problem is infeasible, and an upper
     *         bound otherwise
     */
    @Override
    public double getOptimisticBound(double incumbentScore) {
        if (!isOptimisticBoundCached) {
            if (dwRelax != null) {
                // Run the Dantzig-Wolfe algorithm on the relaxation of the main
                // problem
                if (warmStart != null) {
                    dwRelax.setWarmStart(warmStart);
                }
                relaxSol = dwRelax.solveRelaxation(incumbentScore);
                optimisticBound = relaxSol.getScore();
                isOptimisticBoundCached = true;
                warmStart = dwRelax.getWarmStart();
            } else if (parent != null){
                return parent.getOptimisticBound();
            } else {
                return LazyBranchAndBoundSolver.BEST_SCORE;
            }
        }
        return optimisticBound;
    }

    @Override
    public Solution getFeasibleSolution() {
        if (relaxSol == null) {
            throw new IllegalStateException("No relaxed solution cached.");
        }
        List<DmvSolution> solutions = new ArrayList<DmvSolution>();
        if (initFeasSol != null) {
            solutions.add(initFeasSol);
        }
        
        DmvSolution projectedSol = getProjectedSolution();
        solutions.add(projectedSol);
        // TODO: These solutions might not be feasible according to the bounds.
        // TODO: Decide on a better heuristic for when to do this (e.g. depth >
        // dwRelax.getIdm().getNumTotalParams())
        double random = Prng.nextDouble();
        double proportionViterbiImprove = 0.1;
        if (random < proportionViterbiImprove) {
            // Run Viterbi EM starting from the randomly rounded solution.
            if (random < proportionViterbiImprove / 2.0) {
                solutions.add(getImprovedSol(sentences, projectedSol.getTreebank()));
            } else {
                solutions.add(getImprovedSol(sentences, projectedSol.getLogProbs(), projectedSol.getIdm()));
            }
        }

        return Collections.max(solutions, new Comparator<DmvSolution>() {

            /**
             * This will only return nulls if there are no non-null entries
             */
            @Override
            public int compare(DmvSolution sol1, DmvSolution sol2) {
                if (sol1 == null && sol2 == null) {
                    return 0;
                } else if (sol1 == null) {
                    return 1;
                } else if (sol2 == null) {
                    return -1;
                } else {
                    return Double.compare(sol1.getScore(), sol2.getScore());
                }
            }
            
        });
    }

    private DmvSolution getProjectedSolution() {
        if (!relaxSol.getStatus().hasSolution()) {
            return null;
        }
        // Project the Dantzig-Wolfe model parameters back into the bounded
        // sum-to-exactly-one space
        // TODO: must use bounds here?
   
        double[][] logProbs = relaxSol.getLogProbs();
        // Project the model parameters back onto the feasible (sum-to-one)
        // region ignoring the model parameter bounds (we just want a good solution)
        // there's no reason to constrain it.
        for (int c = 0; c < logProbs.length; c++) {
            double[] probs = Vectors.getExp(logProbs[c]);
            probs = Projections.getProjectedParams(probs);
            logProbs[c] = Vectors.getLog(probs); 
        }
        // Create a new DmvModel from these model parameters
        IndexedDmvModel idm = dwRelax.getIdm();
   
        // Project the fractional parse back to the feasible region
        // where the weight of each edge is given by the indicator variable
        // TODO: How would we do randomized rounding on the Dantzig-Wolfe parse
        // solution?
        DepTreebank treebank = getProjectedParses(relaxSol.getFracRoots(), relaxSol.getFracChildren());
   
        // TODO: write a new DmvMStep that stays in the bounded parameter space
        double score = dwRelax.computeTrueObjective(logProbs, treebank);
        
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, score);
        return sol;
    }

    public DepTreebank getProjectedParses(double[][] fracRoots, double[][][] fracChildren) {
        DepTreebank treebank = new DepTreebank();
        for (int s = 0; s < fracChildren.length; s++) {
            Sentence sentence = sentences.get(s);
            double[] fracRoot = fracRoots[s];
            double[][] fracChild = fracChildren[s];

            // For projective case we use a DP parser
            DepTree tree = Projections.getProjectiveParse(sentence, fracRoot, fracChild);
            treebank.add(tree);
            
            // For non-projective case we'd do something like this.
            // int[] parents = new int[weights.length];
            // Edmonds eds = new Edmonds();
            // CompleteGraph graph = new CompleteGraph(weights);
            // eds.getMaxBranching(graph, 0, parents);
        }
        return treebank;
    }

    @Override
    public List<ProblemNode> branch() {
        List<DmvBoundsDelta> deltasForChildren = deltasFactory.getDmvBounds(this);
        return branch(deltasForChildren);
    }

    /** 
     * For use in strong branching
     */
    List<ProblemNode> branch(List<DmvBoundsDelta> deltasForChildren) {
        ArrayList<ProblemNode> children = new ArrayList<ProblemNode>(deltasForChildren.size());
        for (int i=0; i<deltasForChildren.size(); i++) {
            DmvBoundsDelta deltasForChild = deltasForChildren.get(i);
            children.add(new DmvProblemNode(deltasForChild, deltasFactory, this, i));
        }
        warmStart = null;
        return children;
    }

    @Override
    public int getId() {
        return id;
    }
    
    public DmvProblemNode getParent() {
        return parent;
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public int getSide() {
        return side;
    }

    private static int getNextId() {
        return atomicIntId.getAndIncrement();
    }

    @Override
    public void setAsActiveNode() {
        if (activeNode == null || activeNode == this) {
            activeNode = this;
            return;
        } 
        DmvProblemNode prevNode = activeNode;
        activeNode = this;
        
        if (prevNode.dwRelax == null) {
            throw new IllegalStateException("prevNode is not active");
        }
        if (this.dwRelax != null) {
            throw new IllegalStateException("this node is already active");
        }
        
        // Switch the relaxation over to the new node
        this.dwRelax = prevNode.dwRelax;
        this.deltasFactory = prevNode.deltasFactory;
        // Deactivate the previous node
        prevNode.dwRelax = null;
        prevNode.relaxSol = null;
        prevNode.initFeasSol = null;
        prevNode.deltasFactory = null;
        
        // Find the least common ancestor
        DmvProblemNode lca = findLeastCommonAncestor(prevNode);
        // Reverse apply changes to the bounds moving from prevNode to the LCA
        for (DmvProblemNode node = prevNode; node != lca; node = node.parent) { 
            dwRelax.reverseApply(node.deltas);
        }
        // Create a list of the ancestors of the current node up to, but not including the LCA
        List<DmvProblemNode> ancestors = new ArrayList<DmvProblemNode>(depth - lca.depth);
        for (DmvProblemNode node = this; node != lca; node = node.parent) { 
            ancestors.add(node);
        }
        // Forward apply the bounds moving from the LCA to this
        for (int i=ancestors.size()-1; i>=0; i--) {
            dwRelax.forwardApply(ancestors.get(i).deltas);
        }
    }

    public void clear() {
        this.isOptimisticBoundCached = false;
        this.relaxSol = null;
    }
    
    private DmvProblemNode findLeastCommonAncestor(DmvProblemNode prevNode) {
        DmvProblemNode tmp1 = this;
        DmvProblemNode tmp2 = prevNode;
        // Move tmp nodes to same depth
        while (tmp1.depth > tmp2.depth) {
            tmp1 = tmp1.parent;
        }
        while (tmp2.depth > tmp1.depth) {
            tmp2 = tmp2.parent;
        }
        // Move up by one node until least common ancestor is reached
        while (tmp1 != tmp2) {
            tmp2 = tmp2.parent;
            tmp1 = tmp1.parent;
        }
        return tmp1;
    }

    public DmvBounds getBounds() {
        return dwRelax.getBounds();
    }

    public void end() {
        setAsActiveNode();
        dwRelax.end();
    }
    
    @Override
    public String toString() {
        if (isOptimisticBoundCached) {
            return String.format("DmvProblemNode[upperBound=%f]", optimisticBound);
        } else {
            return String.format("DmvProblemNode[upperBound=?]");
        }
    }

    private DmvSolution getImprovedSol(SentenceCollection sentences, double[][] logProbs, IndexedDmvModel idm) {
        double lambda = 1e-6;
        // TODO: this is a slow conversion
        DmvModel model = idm.getDmvModel(logProbs);
        // We must smooth the weights so that there exists some valid parse
        DmvModelFactory modelFactory = new DmvModelFactory(new SmoothedDmvWeightCopier(model, lambda));
        return runViterbiEmHelper(sentences, modelFactory, 0);
    }
    
    private DmvSolution getImprovedSol(SentenceCollection sentences, DepTreebank treebank) {  
        double lambda = 0.1;
        // Do one M-step to create a model
        DmvMStep mStep = new DmvMStep(lambda);
        DmvModel model = (DmvModel) mStep.getModel(treebank);
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvWeightCopier(model));
        // Then run Viterbi EM
        return runViterbiEmHelper(sentences, modelFactory, 0);
    }
    
    private DmvSolution getInitFeasSol(SentenceCollection sentences) {        
        double lambda = 0.1;
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        return runViterbiEmHelper(sentences, modelFactory, 9);
    }

    private DmvSolution runViterbiEmHelper(SentenceCollection sentences, 
            DmvModelFactory modelFactory, int numRestarts) {
        // Run Viterbi EM to get a reasonable starting incumbent solution
        int iterations = 25;        
        double lambda = 0.1;
        double convergenceRatio = 0.99999;

        ViterbiParser parser = new DmvCkyParser();
        DmvMStep mStep = new DmvMStep(lambda);
        ViterbiTrainer trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, convergenceRatio, numRestarts, Double.POSITIVE_INFINITY, null);
        trainer.train(sentences);
        
        DepTreebank treebank = trainer.getCounts();
        IndexedDmvModel idm = dwRelax.getIdm();
        DepProbMatrix dpm = DmvModelConverter.getDepProbMatrix((DmvModel)trainer.getModel(), sentences.getLabelAlphabet());
        double[][] logProbs = idm.getCmLogProbs(dpm);
        
        // Compute the score for the solution
        double score = dwRelax.computeTrueObjective(logProbs, treebank);
        log.debug("Computed true objective: " + score);
        assert Utilities.equals(score, trainer.getLogLikelihood(), 1e-5) : "difference = " + (score - trainer.getLogLikelihood());
                
        // We let the DmvProblemNode compute the score
        DmvSolution sol = new DmvSolution(logProbs, idm, treebank, score);
        return sol;
    }

    @Override
    public double getLogSpace() {
        if (dwRelax == null) {
            throw new IllegalStateException("This is not the active node");
        }
        return dwRelax.getBounds().getLogSpace();
    }
    
    public double[][] getRegretCm() {
        // TODO: we could store this in the relaxed solution if 
        // we start using it regularly.
        return dwRelax.getRegretCm();
    }

    public IndexedDmvModel getIdm() {
        return dwRelax.getIdm();
    }
    
    public DmvRelaxation getRelaxation() {
        return dwRelax;
    }

    /**
     * For testing only.
     */
    public static void clearActiveNode() {
        activeNode = null;
    }
    
}
