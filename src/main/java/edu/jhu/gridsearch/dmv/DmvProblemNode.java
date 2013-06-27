package edu.jhu.gridsearch.dmv;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import edu.jhu.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.gridsearch.ProblemNode;
import edu.jhu.gridsearch.Relaxation;
import edu.jhu.gridsearch.RelaxedSolution;
import edu.jhu.gridsearch.cpt.CptBoundsDeltaFactory;
import edu.jhu.gridsearch.cpt.CptBoundsDeltaList;

public class DmvProblemNode implements ProblemNode {

    private static final Logger log = Logger.getLogger(DmvProblemNode.class);

    private static AtomicInteger atomicIntId = new AtomicInteger(0);

    private int id;
    private DmvProblemNode parent;
    protected int depth;
    private int side;
    private CptBoundsDeltaList deltas;
    protected double optimisticBound;
    
    // For active node only:
    private CptBoundsDeltaFactory deltasFactory;
    
    // For "fork" nodes only (i.e. nodes that store warm-start information)
    private SoftReference<WarmStart> warmStart;

    /**
     * Root node constructor
     */
    public DmvProblemNode(CptBoundsDeltaFactory brancher) {
        this.id = getNextId();
        this.parent = null;
        this.depth = 0;
        this.side = 0;
        this.deltasFactory = brancher;
        this.optimisticBound = LazyBranchAndBoundSolver.BEST_SCORE;
    }

    /**
     * Non-root node constructor
     */
    public DmvProblemNode(CptBoundsDeltaList deltas, CptBoundsDeltaFactory deltasFactory, DmvProblemNode parent, int side) {
        this.deltas = deltas;
        this.deltasFactory = deltasFactory;
        this.id = getNextId();
        this.parent = parent;
        this.depth = parent.depth + 1;
        this.side = side;
        // Take the optimistic bound from the parent.
        this.optimisticBound = parent.optimisticBound;
        
        // Check for null deltas (used for testing only)
        if (deltas != null) {
            // Conserve space on the internal list representation.
            deltas.trimToSize();
        }
    }
    

    /**
     * For testing only
     */
    protected DmvProblemNode() {
        
    }
    
    /**
     * @return negative infinity if the problem is infeasible, and an upper
     *         bound otherwise
     */
    @Override
    public double getLocalUb() {
        return optimisticBound;
    }

    @Override
    public List<ProblemNode> branch(Relaxation relaxation, RelaxedSolution relaxSol) {
        DmvRelaxation relax = (DmvRelaxation)relaxation;
        assert(this == relax.getActiveNode());        
        List<CptBoundsDeltaList> deltasForChildren = deltasFactory.getDeltas(this, relax, (DmvRelaxedSolution)relaxSol);
        return branch(deltasForChildren);
    }

    /** 
     * For use in strong branching
     */
    public List<ProblemNode> branch(List<CptBoundsDeltaList> deltasForChildren) {
        ArrayList<ProblemNode> children = new ArrayList<ProblemNode>(deltasForChildren.size());
        for (int i=0; i<deltasForChildren.size(); i++) {
            CptBoundsDeltaList deltasForChild = deltasForChildren.get(i);
            children.add(new DmvProblemNode(deltasForChild, deltasFactory, this, i));
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

    @Override
    public int getSide() {
        return side;
    }

    private static int getNextId() {
        return atomicIntId.getAndIncrement();
    }

    public static List<CptBoundsDeltaList> getDeltasBetween(DmvProblemNode prevNode, DmvProblemNode curNode) {

        // Get sequence of deltas to be forward applied to the current relaxation.
        List<CptBoundsDeltaList> deltas = new ArrayList<CptBoundsDeltaList>();

        // Find the least common ancestor
        DmvProblemNode lca = curNode.findLeastCommonAncestor(prevNode);
        
        // Reverse apply changes to the bounds moving from prevNode to the LCA
        for (DmvProblemNode node = prevNode; node != lca; node = node.parent) { 
            deltas.add(CptBoundsDeltaList.getReverse(node.deltas));
        }
        // Create a list of the ancestors of the current node up to, but not including the LCA
        List<DmvProblemNode> ancestors = new ArrayList<DmvProblemNode>(curNode.depth - lca.depth);
        for (DmvProblemNode node = curNode; node != lca; node = node.parent) { 
            ancestors.add(node);
        }
        // Forward apply the bounds moving from the LCA to this
        for (int i=ancestors.size()-1; i>=0; i--) {
            deltas.add(ancestors.get(i).deltas);
        }
        
        return deltas;
    }
    
    /**
     * @return The least common ancestor of this node with prevNode.
     * @throws IllegalStateException if no LCA exists.
     */
    public DmvProblemNode findLeastCommonAncestor(DmvProblemNode prevNode) {
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
        if (tmp1 != tmp2 || tmp1 == null || tmp2 == null) {
            // No LCA found.
            throw new IllegalStateException("No LCA found");
        }
        return tmp1;
    }

    @Override
    public String toString() {
        return String.format("DmvProblemNode[upperBound=%f]", optimisticBound);
    }

    public CptBoundsDeltaList getDeltas() {
        return deltas;
    }

    @Override
    public WarmStart getWarmStart() {
        // Find the closest non-null warm start from the ancestors.
        DmvProblemNode cur = this;
        return getWarmStart(cur);
    }

    private static WarmStart getWarmStart(DmvProblemNode cur) {
        while (cur != null) {
            if (cur.warmStart != null) {
                WarmStart ws = cur.warmStart.get();
                if (ws != null) {
                    return ws;
                }
            }
            cur = cur.parent;
        }
        return null;
    }

    @Override
    public void setWarmStart(WarmStart warmStart) {
        this.warmStart = new SoftReference<WarmStart>(warmStart);
    }

    @Override
    public void setOptimisticBound(double optimisticBound) {
        this.optimisticBound = optimisticBound;
    }

    public void clear() {
        this.optimisticBound = LazyBranchAndBoundSolver.BEST_SCORE;
        this.warmStart = null;
    }    
    
    // For testing only.
    public static void resetIdCounter() {
        atomicIntId = new AtomicInteger(0);
    }
    
}
