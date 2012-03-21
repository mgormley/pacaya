package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;

public class DmvProblemNode implements ProblemNode {

    private static AtomicInteger atomicIntId = new AtomicInteger(0);

    private int id;
    private DmvProblemNode parent;
    private int depth;
    private DmvBoundsDelta deltas;
    
    // For active node only:
    private DmvBoundsDeltaFactory deltasFactory;
    private DmvDantzigWolfeRelaxation dwRelax;
    private boolean isOptimisticBoundCached;
    private double optimisticBound;
    private SentenceCollection sentences;
    private RelaxedDmvSolution relaxSol;

    /**
     * Root node constructor
     */
    public DmvProblemNode(SentenceCollection sentences, DmvBoundsDeltaFactory deltasFactory,
            DmvModelFactory modelFactory) {
        this.deltasFactory = deltasFactory;
        this.sentences = sentences;
        id = 0;
        parent = null;
        depth = 0;
        dwRelax = new DmvDantzigWolfeRelaxation(modelFactory, sentences);
        isOptimisticBoundCached = false;
    }

    /**
     * Non-root node constructor
     */
    public DmvProblemNode(DmvBoundsDelta deltas, DmvBoundsDeltaFactory deltasFactory, int id, DmvProblemNode parent) {
        this.deltas = deltas;
        this.deltasFactory = deltasFactory;
        this.id = id;
        this.parent = parent;
        this.depth = parent.depth + 1;
        isOptimisticBoundCached = false;
        // The relaxation is set only when this node is set to be the active one
        this.dwRelax = null;
        this.sentences = parent.sentences;
    }

    /**
     * @return negative infinity if the problem is infeasible, and an upper
     *         bound otherwise
     */
    @Override
    public double getOptimisticBound() {
        if (!isOptimisticBoundCached) {
            // Run the Dantzig-Wolfe algorithm on the relaxation of the main
            // problem
            relaxSol = dwRelax.solveRelaxation();
            optimisticBound = relaxSol.getScore();
            isOptimisticBoundCached = true;
        }
        return optimisticBound;
    }

    @Override
    public Solution getFeasibleSolution() {
        // Project the Dantzig-Wolfe model parameters back into the bounded
        // sum-to-exactly-one space
        // TODO: must use bounds here
        DmvModel model = getProjectedModel(relaxSol.getDmvModel());

        // Project the fractional parse back to the feasible region
        // where the weight of each edge is given by the indicator variable
        // TODO: How would we do randomized rounding on the Dantzig-Wolfe parse
        // solution?
        DepTreebank treebank = getProjectedParses(relaxSol.getFracRoots(), relaxSol.getFracChildren());

        // TODO: write a new DmvMStep that stays in the bounded parameter space
        double score = dwRelax.computeTrueObjective(model, treebank);

        // Note: these approaches might be wrong if our objective function
        // includes posterior constraints
        // PrCkyParser parser = new PrCkyParser();
        // DepTreebank viterbiTreebank = parser.getViterbiParse(sentences,
        // model);
        // double score = parser.getLastParseWeight();
        // 
        // Then run Viterbi EM starting from the randomly rounded solution
        // and respecting the bounds.

        // Throw away the relaxed solution
        relaxSol = null;

        return new DmvSolution(model, treebank, score);
    }

    public DmvModel getProjectedModel(double[][] modelParams) {
        // Project the model parameters back onto the feasible (sum-to-one)
        // region
        // ignoring the model parameter bounds (we just want a good solution)
        // there's
        // no reason to constrain it.
        for (int c = 0; c < modelParams.length; c++) {
            modelParams[c] = Projections.getProjectedParams(modelParams[c]);
        }

        // Create a new DmvModel from these model parameters
        IndexedDmvModel idm = dwRelax.getIdm();
        return idm.getDmvModel(modelParams);
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
        ArrayList<ProblemNode> children = new ArrayList<ProblemNode>(deltasForChildren.size());
        for (DmvBoundsDelta deltasForChild : deltasForChildren) {
            children.add(new DmvProblemNode(deltasForChild, deltasFactory, getNextId(), this));
        }
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

    private static int getNextId() {
        return atomicIntId.getAndIncrement();
    }

    @Override
    public void setAsActiveNode(ProblemNode prevNode0) {
        if (prevNode0 == null) {
            return;
        }
        DmvProblemNode prevNode = (DmvProblemNode) prevNode0;
        
        // Switch the relaxation over to the new node
        this.dwRelax = prevNode.dwRelax;
        // Deactivate the previous node
        prevNode.dwRelax = null;
        prevNode.relaxSol = null;
        
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

}
