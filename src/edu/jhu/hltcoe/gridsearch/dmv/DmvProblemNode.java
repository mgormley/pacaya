package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.EagerBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.model.dmv.DmvModel;

public class DmvProblemNode implements ProblemNode {

    private static final int NO_PARENT = -1;
        
    private static AtomicInteger atomicIntId = new AtomicInteger(0);
    
    private int id;
    private int parentId;
    private int depth;
    private DmvBounds bounds;
    private DmvBoundsFactory boundsFactory;
    private DmvDantzigWolfeRelaxation dwRelax;

    private boolean isOptimisticBoundCached;
    private double optimisticBound;
    private SentenceCollection sentences;
    
    /**
     * Root node constructor
     * @param sentences 
     */
    public DmvProblemNode(SentenceCollection sentences, DmvBounds bounds, DmvBoundsFactory boundsFactory) {
        this.sentences = sentences;
        this.bounds = bounds;
        this.boundsFactory = boundsFactory;
        id = 0;
        parentId = NO_PARENT;
        depth = 0;
        dwRelax = new DmvDantzigWolfeRelaxation(bounds);
        isOptimisticBoundCached = false;
    }

    /**
     * Non-root node constructor
     */
    public DmvProblemNode(SentenceCollection sentences, DmvBounds bounds, DmvBoundsFactory boundsFactory, int id, DmvProblemNode parent) {
        this.sentences = sentences;
        this.bounds = bounds;
        this.boundsFactory = boundsFactory;
        this.id = id;
        this.parentId = parent.id;
        this.depth = parent.depth + 1;
        // TODO: This seems sensible for a DFS, but we might want to do
        // something different if we're not doing DFS
        this.dwRelax = parent.dwRelax;
        isOptimisticBoundCached = false;
    }

    /**
     * @return negative infinity if the problem is infeasible, and an upper bound otherwise
     */
    @Override
    public double getOptimisticBound() {
        if (!isOptimisticBoundCached) {
            // Run the Dantzig-Wolfe algorithm on the relaxation of the main problem
            dwRelax.updateBounds(bounds);
            optimisticBound = dwRelax.solve();
            isOptimisticBoundCached = true;
        }
        return optimisticBound;
    }

    @Override
    public Solution getFeasibleSolution() {
        // Project the Dantzig-Wolfe model parameters back into the bounded
        // sum-to-exactly-one space
        // TODO: must use bounds here
        DmvModel model = dwRelax.getProjectedModel();
        
        // Project the fractional parse back to the feasible region
        // where the weight of each edge is given by the indicator variable
        // TODO: How would we do randomized rounding on the Dantzig-Wolfe parse solution?
        DepTreebank treebank = dwRelax.getProjectedParses();
        
        // TODO: write a new DmvMStep that stays in the bounded parameter space
        double score = dwRelax.computeTrueObjective(model, treebank);
        
        // Note: these approaches might be wrong if our objective function includes posterior constraints
        //        PrCkyParser parser = new PrCkyParser();
        //        DepTreebank viterbiTreebank = parser.getViterbiParse(sentences, model);
        //        double score = parser.getLastParseWeight();
        // 
        //   Then run Viterbi EM starting from the randomly rounded solution
        //   and respecting the bounds.
        
        return new DmvSolution(model, treebank, score);
    }

    @Override
    public List<ProblemNode> branch() {
        List<DmvBounds> boundsForChildren = boundsFactory.getDmvBounds(this);
        ArrayList<ProblemNode> children = new ArrayList<ProblemNode>(boundsForChildren.size());
        for (DmvBounds boundsForChild : boundsForChildren) {
            children.add(new DmvProblemNode(sentences, boundsForChild, boundsFactory, getNextId(), this));
        }
        return children;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getParentId() {
        return parentId;
    }

    @Override
    public int getDepth() {
        return depth;
    }
    
    private static int getNextId() {
        return atomicIntId.getAndIncrement();
    }

}
