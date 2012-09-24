package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelConverter;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.parse.DmvCkyParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.train.TrainCorpus;
import edu.jhu.hltcoe.train.ViterbiTrainer;
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
    private DmvTrainCorpus corpus;
    private RelaxedDmvSolution relaxSol;
    
    // For "fork" nodes only (i.e. nodes that store warm-start information)
    private WarmStart warmStart;
    
    // For root node only
    private DmvSolution initFeasSol;

    private ViterbiEmDmvProjector dmvProjector;

    private static DmvProblemNode activeNode;

    /**
     * Root node constructor
     */
    public DmvProblemNode(DmvTrainCorpus corpus, DmvBoundsDeltaFactory brancher, DmvRelaxation dwRelax) {
        this.corpus = corpus;
        this.dwRelax = dwRelax;
        this.dwRelax.init1(corpus);
        // Save and use this solution as the first incumbent
        this.initFeasSol = getInitFeasSol(corpus);
        log.info("Initial solution score: " + initFeasSol.getScore());
        this.id = getNextId();
        this.parent = null;
        this.depth = 0;
        this.side = 0;
        this.deltasFactory = brancher;
        this.isOptimisticBoundCached = false;
        
        this.dwRelax.init2(initFeasSol);
        this.dmvProjector = new ViterbiEmDmvProjector(this.corpus, this.dwRelax, this.initFeasSol);

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
        this.corpus = parent.corpus;
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
        return dmvProjector.getProjectedDmvSolution(relaxSol);
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
        this.dmvProjector = prevNode.dmvProjector;
        
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
    
    private DmvSolution getInitFeasSol(TrainCorpus corpus) {        
        double lambda = 0.1;
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        return runViterbiEmHelper(corpus, modelFactory, 9);
    }

    private DmvSolution runViterbiEmHelper(TrainCorpus corpus, 
            DmvModelFactory modelFactory, int numRestarts) {
        // Run Viterbi EM to get a reasonable starting incumbent solution
        int iterations = 25;        
        double lambda = 0.1;
        double convergenceRatio = 0.99999;

        ViterbiParser parser = new DmvCkyParser();
        DmvMStep mStep = new DmvMStep(lambda);
        ViterbiTrainer trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, convergenceRatio, numRestarts, Double.POSITIVE_INFINITY, null);
        trainer.train(corpus);
        
        DepTreebank treebank = trainer.getCounts();
        IndexedDmvModel idm = dwRelax.getIdm();
        DepProbMatrix dpm = DmvModelConverter.getDepProbMatrix((DmvModel)trainer.getModel(), corpus.getLabelAlphabet());
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
        atomicIntId = new AtomicInteger(0);
    }
    
}
