package edu.jhu.hltcoe.gridsearch;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.gridsearch.FathomStats.FathomStatus;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.Time;
import edu.jhu.hltcoe.util.Utilities;

/**
 * For a maximization problem, this performs eager (as opposed to lazy) branch
 * and bound.
 * 
 * The SCIP thesis section 6.3 notes that
 * "Usually, the child nodes inherit the dual bound of their parent node", so
 * maybe we should switch to lazy branch and bound.
 */
public class LazyBranchAndBoundSolver {

    private static final Logger log = Logger.getLogger(LazyBranchAndBoundSolver.class);

    public enum SearchStatus {
        OPTIMAL_SOLUTION_FOUND, NON_OPTIMAL_SOLUTION_FOUND
    }
    
    public static final double WORST_SCORE = Double.NEGATIVE_INFINITY;
    public static final double BEST_SCORE = Double.POSITIVE_INFINITY;
    protected double incumbentScore;
    protected Solution incumbentSolution;

    protected SearchStatus status;

    // Storage of active nodes
    protected final NodeOrderer leafNodePQ;
    protected final PriorityQueue<ProblemNode> upperBoundPQ;
    
    protected final double epsilon;
    protected final double timeoutSeconds;
    protected Stopwatch nodeTimer;
    protected Stopwatch switchTimer;
    protected Stopwatch relaxTimer;
    protected Stopwatch feasTimer;
    protected Stopwatch branchTimer;

    // If true, fathoming is disabled. This enables random sampling of the
    // branch and bound tree.
    protected boolean disableFathoming;
    
    public LazyBranchAndBoundSolver(double epsilon, NodeOrderer leafNodeOrderer, double timeoutSeconds) {
        this.epsilon = epsilon;
        this.leafNodePQ = leafNodeOrderer;
        this.upperBoundPQ = new PriorityQueue<ProblemNode>(11, new BfsComparator());
        this.timeoutSeconds = timeoutSeconds;
        this.disableFathoming = false;
        
        // Timers
        nodeTimer = new Stopwatch();
        switchTimer = new Stopwatch();
        relaxTimer = new Stopwatch();
        feasTimer = new Stopwatch();
        branchTimer = new Stopwatch();
    }

    public SearchStatus runBranchAndBound(ProblemNode rootNode) {
        return runBranchAndBound(rootNode, null, WORST_SCORE);
    }    

    public SearchStatus runBranchAndBound(ProblemNode rootNode, Solution initialSolution, double initialScore) {
        // Initialize
        this.incumbentSolution = initialSolution;
        this.incumbentScore = initialScore;
        double upperBound = BEST_SCORE;
        status = SearchStatus.NON_OPTIMAL_SOLUTION_FOUND;
        leafNodePQ.clear();
        upperBoundPQ.clear();
        int numProcessed = 0;
        FathomStats fathom = new FathomStats();
        
        addToLeafNodes(rootNode);

        double rootLogSpace = rootNode.getLogSpace();
        double logSpaceRemain = rootLogSpace;
        ProblemNode curNode = null;

        evalIncumbent(initialSolution);
        while (hasNextLeafNode()) {
            if (nodeTimer.isRunning()) { nodeTimer.stop(); }
            nodeTimer.start();
            
            switchTimer.start();
            // The upper bound can only decrease
            if (upperBoundPQ.peek().getOptimisticBound() > upperBound + 1e-8) {
                log.warn(String.format("Upper bound should be strictly decreasing: peekUb = %e\tprevUb = %e", upperBoundPQ.peek().getOptimisticBound(), upperBound));
            }
            upperBound = upperBoundPQ.peek().getOptimisticBound();
            assert (!Double.isNaN(upperBound));
            
            curNode = getNextLeafNode();
            numProcessed++;
            double relativeDiff = computeRelativeDiff(upperBound, incumbentScore);
             
            // Logging
            printSummary(upperBound, relativeDiff, numProcessed, fathom);
            if (log.isDebugEnabled() && numProcessed % 100 == 0) {
                printLeafNodeBoundHistogram();
                printTimers(numProcessed);
                printSpaceRemaining(numProcessed, rootLogSpace, logSpaceRemain);
            }
            
            curNode.setAsActiveNode();
            switchTimer.stop();
            if (relativeDiff <= epsilon) {
                // Optimal solution found.
                break;
            } else if (Time.totSec(nodeTimer) > timeoutSeconds) {
                // Timeout reached.
                break;
            }
            curNode.updateTimeRemaining(timeoutSeconds - Time.totSec(nodeTimer));
            // TODO: else if, ran out of memory or disk space, break

            // The active node can compute a tighter upper bound instead of
            // using its parent's bound
            relaxTimer.start();
            double curNodeLowerBound;
            if (disableFathoming) {
                // If not fathoming, don't stop the relaxation early.
                curNodeLowerBound = curNode.getOptimisticBound();
            } else {
                curNodeLowerBound = curNode.getOptimisticBound(incumbentScore);
            }
            RelaxedSolution relax = curNode.getRelaxedSolution();
            relaxTimer.stop();
            log.info(String.format("CurrentNode: id=%d depth=%d side=%d relaxScore=%f relaxStatus=%s incumbScore=%f avgNodeTime=%f", curNode.getId(),
                    curNode.getDepth(), curNode.getSide(), relax.getScore(), relax.getStatus().toString(), incumbentScore, Time.totMs(nodeTimer) / numProcessed));
            if (curNodeLowerBound <= incumbentScore && !disableFathoming) {
                // Fathom this node: it is either infeasible or was pruned.
                if (relax.getStatus() == RelaxStatus.Infeasible) {
                    fathom.fathom(curNode, FathomStatus.Infeasible);
                } else if (relax.getStatus() == RelaxStatus.Pruned) {
                    fathom.fathom(curNode, FathomStatus.Pruned);
                } else {
                    log.warn("Unhandled status for relaxed solution: " + relax.getStatus());
                }
                logSpaceRemain = Utilities.logSubtractExact(logSpaceRemain, curNode.getLogSpace());
                continue;
            }

            // Check if the child node offers a better feasible solution
            feasTimer.start();
            Solution sol = curNode.getFeasibleSolution();
            assert (sol == null || !Double.isNaN(sol.getScore()));
            if (sol != null && sol.getScore() > incumbentScore) {
                incumbentScore = sol.getScore();
                incumbentSolution = sol;
                evalIncumbent(incumbentSolution);
                // TODO: pruneActiveNodes();
                // We could store a priority queue in the opposite order (or
                // just a sorted list)
                // and remove nodes from it while their optimisticBound is
                // worse than the
                // new incumbentScore.
            }
            feasTimer.stop();
            
            if (sol != null && Utilities.equals(sol.getScore(), relax.getScore(), 1e-13)  && !disableFathoming) {
                // Fathom this node: the optimal solution for this subproblem was found.
                fathom.fathom(curNode, FathomStatus.CompletelySolved);
                logSpaceRemain = Utilities.logSubtractExact(logSpaceRemain, curNode.getLogSpace());
                continue;
            }
            
            branchTimer.start();
            List<ProblemNode> children = curNode.branch();
            if (children.size() == 0) {
                // Fathom this node: no more branches can be made.
                fathom.fathom(curNode, FathomStatus.BottomedOut);
                logSpaceRemain = Utilities.logSubtractExact(logSpaceRemain, curNode.getLogSpace());
            }
            for (ProblemNode childNode : children) {
                addToLeafNodes(childNode);
            }
            branchTimer.stop();
        }
        
        // Print summary
        evalIncumbent(incumbentSolution);
        double relativeDiff = computeRelativeDiff(upperBound, incumbentScore);
        if (relativeDiff <= epsilon) {
            status = SearchStatus.OPTIMAL_SOLUTION_FOUND;
        }
        printSummary(upperBound, relativeDiff, numProcessed, fathom);
        printTimers(numProcessed);
        leafNodePQ.clear();
        upperBoundPQ.clear();

        log.info("B&B search status: " + status);
        
        // Return epsilon optimal solution
        return status;
    }

    private static double computeRelativeDiff(double upperBound, double lowerBound) {
        // TODO: This is incorrect if the bounds are positive.
        return Math.abs(upperBound - lowerBound) / Math.abs(lowerBound);
    }

    private void printSummary(double upperBound, double relativeDiff, int numProcessed, FathomStats fathom) {
        int numFathomed = fathom.getNumFathomed();
        log.info(String.format("Summary: upBound=%f lowBound=%f relativeDiff=%f #leaves=%d #fathom=%d #prune=%d #infeasible=%d, avgFathomDepth=%.0f #seen=%d", 
                upperBound, incumbentScore, relativeDiff, leafNodePQ.size(), numFathomed, fathom.numPruned, fathom.numInfeasible, fathom.getAverageDepth(), numProcessed));
    }

    /**
     * Override this method.
     */
    protected void evalIncumbent(Solution incumbentSolution) {
        return;
    }

    private boolean hasNextLeafNode() {
        return !leafNodePQ.isEmpty();
    }

    private ProblemNode getNextLeafNode() {
        ProblemNode node = leafNodePQ.remove();
        upperBoundPQ.remove(node);
        return node;
    }

    private void addToLeafNodes(ProblemNode node) {
        leafNodePQ.add(node);
        if (disableFathoming && upperBoundPQ.size() > 0) {
            // This is a hack to ensure that we don't populate the upperBoundPQ.
            return;
        } else {
            upperBoundPQ.add(node);
        }
    }

    public Solution getIncumbentSolution() {
        return incumbentSolution;
    }
    
    public double getIncumbentScore() {
        return incumbentScore;
    }
    
    public void setDisableFathoming(boolean disableFathoming) {
        this.disableFathoming = disableFathoming;
    }
    
    private void printSpaceRemaining(int numProcessed, double rootLogSpace, double logSpaceRemain) {
        // Print stats about the space remaining.
        log.info("Log space remaining (sub): " + logSpaceRemain);
        if (numProcessed % 10000 == 0) {
            double logSpaceRemainAdd = computeLogSpaceRemain();
            log.info("Log space remaining (add): " + logSpaceRemainAdd);
            if (!Utilities.equals(logSpaceRemain, logSpaceRemainAdd, 1e-4)) {
                log.warn("Log space remaining differs between subtraction and addition versions.");
            }
        }
        log.info("Space remaining: " + Utilities.exp(logSpaceRemain));
        log.info("Proportion of root space remaining: " + Utilities.exp(logSpaceRemain - rootLogSpace));
    }

    protected void printTimers(int numProcessed) {
        // Print timers.
        log.debug("Avg time(ms) per node: " + Time.totMs(nodeTimer) / numProcessed);
        log.debug("Avg switch time(ms) per node: " + Time.totMs(switchTimer) / numProcessed);
        log.debug("Avg relax time(ms) per node: " + Time.totMs(relaxTimer) / numProcessed);
        log.debug("Avg project time(ms) per node: " + Time.totMs(feasTimer) / numProcessed);
        log.debug("Avg branch time(ms) per node: " + Time.totMs(branchTimer) / numProcessed);
    }

    private void printLeafNodeBoundHistogram() {
        // Print Histogram
        double[] bounds = new double[leafNodePQ.size()];
        int i = 0;
        for (ProblemNode node : leafNodePQ) {
            bounds[i] = node.getOptimisticBound();
            i++;
        }
        log.debug(getHistogram(bounds));
    }
    
    /** 
     * This VERY SLOWLY computes the log space remaining by 
     * adding up all the bounds of the leaf nodes.
     */
    private double computeLogSpaceRemain() {
        double logSpaceRemain = Double.NEGATIVE_INFINITY;
        for (ProblemNode node : leafNodePQ) {
            node.setAsActiveNode();
            logSpaceRemain = Utilities.logAdd(logSpaceRemain, node.getLogSpace());
        }
        return logSpaceRemain;
    }

    private String getHistogram(double[] bounds) {
        int numBins = 10;

        double max = Vectors.max(bounds);
        double min = Vectors.min(bounds);
        double binWidth = (max - min) / numBins;
        
        int[] hist = new int[numBins];
        for (int i = 0; i < bounds.length; i++) {
            int idx = (int) ((bounds[i] - min) / binWidth);
            if (idx == hist.length) {
                idx--;
            }
            hist[idx]++;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("histogram: min=%f max=%f\n", min, max));
        for (int i=0; i<hist.length; i++) {
            sb.append(String.format("\t[%.3f, %.3f) : %d\n", binWidth*i + min, binWidth*(i+1) + min, hist[i]));
        }
        return sb.toString();
    }
    
}