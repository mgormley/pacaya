package edu.jhu.gridsearch;

import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;

import org.apache.log4j.Logger;

import edu.jhu.gridsearch.LazyBranchAndBoundSolver.NodeResult;
import edu.jhu.gridsearch.dmv.DmvProblemNode;
import edu.jhu.gridsearch.dmv.RelaxSolChildOrderer;
import edu.jhu.util.IterIterator;

/**
 * This class implements Best First Search with Plunging for branch-and-bound.
 *  
 * @author mgormley
 * 
 */
public class PlungingBfsNodeOrderer implements NodeOrderer {

    public static class PlungingBfsNodeOrdererPrm {
        /**
         * The default threshold for the local relative gap
         */
        public double localRelativeGapThreshold = 0.25;
        public double minPlungeDepthProp = 0.1;
        public double maxPlungeDepthProp = 0.5;
        public ChildOrderer childOrderer = new RelaxSolChildOrderer();
    }

    private static final Logger log = Logger.getLogger(PlungingBfsNodeOrderer.class);

    private PlungingBfsNodeOrdererPrm prm;
    private PriorityQueue<ProblemNode> pq;
    private int maxDepthProcessed;
    private int plungeSteps;
    private Stack<ProblemNode> stack;
    private RelaxedSolution rootSol;

    public PlungingBfsNodeOrderer(PlungingBfsNodeOrdererPrm prm) {
        this.prm = prm;
        this.pq = new PriorityQueue<ProblemNode>(11, new BfsComparator());
        stack = new Stack<ProblemNode>();
        maxDepthProcessed = 0;
        plungeSteps = 0;
    }

    public void addRoot(ProblemNode root) {
        pq.add(root);
    }

    public void addChildrenOfResult(NodeResult result, double globalUb, double globalLb, boolean isRoot) {
        if (isRoot) {
            this.rootSol = result.relaxSol;
        }
        double localUb = result.relaxSol.getScore();
        double localRelativeGap = (globalUb - localUb) / (globalUb - globalLb);
        log.debug("Local relative gap: " + localRelativeGap);
        // Check whether we've exceed the number of plunging steps or if we're
        // over the minimum number of steps whether the local relative gap
        // exceeds the threshold.
        if ((plungeSteps > prm.maxPlungeDepthProp * maxDepthProcessed)
                || (plungeSteps > prm.minPlungeDepthProp * maxDepthProcessed 
                        && localRelativeGap > prm.localRelativeGapThreshold)) {
            log.debug(String.format("Resetting plunge after %d steps", plungeSteps));
            // Clean up / Reset plunge.
            plungeSteps = 0;

            // Move all nodes remaining on the stack to the BFS priority queue.
            pq.addAll(stack);
            stack.clear();

            // Add the new children to the priority queue.
            pq.addAll(result.children);
        } else {
            List<ProblemNode> children = prm.childOrderer.orderChildren(result.relaxSol, rootSol, result.children);
            
            // Add the new children to the stack for plunging.
            for (ProblemNode node : children) {
                stack.push(node);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return pq.isEmpty() && stack.isEmpty();
    }

    @Override
    public ProblemNode remove() {
        ProblemNode node;
        // Check whether the local relative gap exceeds the threshold.
        if (stack.size() == 0) {
            // Get the next best node.
            node = pq.remove();
        } else {
            // Get the next plunge node from the stack.
            node = stack.pop();
            plungeSteps++;
        }
        maxDepthProcessed = Math.max(maxDepthProcessed, node.getDepth());
        return node;
    }

    @Override
    public int size() {
        return pq.size() + stack.size();
    }

    @Override
    public void clear() {
        pq.clear();
        stack.clear();
    }

    @Override
    public Iterator<ProblemNode> iterator() {
        return new IterIterator<ProblemNode>(stack.iterator(), pq.iterator());
    }

}
