package edu.jhu.hltcoe.gridsearch;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.FathomStats.FathomStatus;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRelaxation;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.Timer;
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

    public static interface LazyBnbSolverFactory {
        public LazyBranchAndBoundSolver getInstance(Relaxation relaxation, Projector projector);
    }
    
    public static class LazyBnbSolverPrm implements LazyBnbSolverFactory {
        // Required:
        public Relaxation relaxation = null;
        public Projector projector = null;
        public NodeOrderer leafNodeOrderer = new PqNodeOrderer(new BfsComparator());
        // Optional:
        public double epsilon = 0.1;
        public double timeoutSeconds = 1e+75;
        // If true, fathoming is disabled. This enables random sampling of the
        // branch and bound tree.
        public boolean disableFathoming = false;
        public SolutionEvaluator evaluator = null;
        @Override
        public LazyBranchAndBoundSolver getInstance(Relaxation relaxation, Projector projector) {
            this.relaxation = relaxation;
            this.projector = projector;
            return new LazyBranchAndBoundSolver(this);
        }
    }
    
    public enum SearchStatus {
        OPTIMAL_SOLUTION_FOUND, NON_OPTIMAL_SOLUTION_FOUND
    }
    
    private static final Logger log = Logger.getLogger(LazyBranchAndBoundSolver.class);
    public static final double WORST_SCORE = Double.NEGATIVE_INFINITY;
    public static final double BEST_SCORE = Double.POSITIVE_INFINITY;

    private LazyBnbSolverPrm prm;
    protected Solution incumbentSolution;
    /**
     * The global lower bound.
     */
    protected double incumbentScore;
    /**
     * The global upper bound.
     */
    private double globalUb;

    protected SearchStatus status;

    // Storage of active nodes
    protected final PriorityQueue<ProblemNode> upperBoundPQ;
    
    // Timers
    protected Timer nodeTimer;
    protected Timer relaxTimer;
    protected Timer feasTimer;
    protected Timer branchTimer;
    
    public LazyBranchAndBoundSolver(LazyBnbSolverPrm prm) {
        if (prm.relaxation == null || prm.projector == null || prm.leafNodeOrderer == null) {
            throw new IllegalStateException("Required parameters are not set.");
        }
        this.prm = prm;
        this.upperBoundPQ = new PriorityQueue<ProblemNode>(11, new BfsComparator());
        
        // Timers
        nodeTimer = new Timer();
        relaxTimer = new Timer();
        feasTimer = new Timer();
        branchTimer = new Timer();
    }

    public SearchStatus runBranchAndBound(ProblemNode rootNode) {
        return runBranchAndBound(rootNode, null, WORST_SCORE);
    }

    public SearchStatus runBranchAndBound(ProblemNode rootNode, Solution initialSolution, double initialScore) {
        // Initialize
        this.incumbentSolution = initialSolution;
        this.incumbentScore = initialScore;
        globalUb = BEST_SCORE;
        status = SearchStatus.NON_OPTIMAL_SOLUTION_FOUND;
        clearLeafNodes();
        int numProcessed = 0;
        FathomStats fathom = new FathomStats();
        
        addRootToLeafNodes(rootNode);

        double rootLogSpace = Double.NaN;
        double logSpaceRemain = Double.NaN;
        ProblemNode curNode = null;

        evalIncumbent(initialSolution);
        while (hasNextLeafNode()) {
            if (nodeTimer.isRunning()) { nodeTimer.stop(); }
            nodeTimer.start();
            
            // The upper bound can only decrease
            ProblemNode worstLeaf = getWorstLeaf();
            if (worstLeaf.getOptimisticBound() > globalUb + 1e-8) {
                log.warn(String.format("Upper bound should be strictly decreasing: peekUb = %e\tprevUb = %e", worstLeaf.getOptimisticBound(), globalUb));
            }
            globalUb = worstLeaf.getOptimisticBound();
            assert (!Double.isNaN(globalUb));
            
            numProcessed++;
            double relativeDiff = computeRelativeDiff(globalUb, incumbentScore);
            
            if (relativeDiff <= prm.epsilon) {
                // Optimal solution found.
                break;
            } else if (nodeTimer.totSec() > prm.timeoutSeconds) {
                // Timeout reached.
                break;
            }
            
            // Logging.
            printSummary(globalUb, relativeDiff, numProcessed, fathom);
            if (log.isDebugEnabled() && numProcessed % 100 == 0) {
                printLeafNodeBoundHistogram();
                printTimers(numProcessed);
                printSpaceRemaining(numProcessed, rootLogSpace, logSpaceRemain);
            }
            
            // Process the next node.
            curNode = getNextLeafNode();

            NodeResult result = processNode(curNode);
            fathom.add(curNode, result.status);
            if (result.status != FathomStatus.NotFathomed && prm.relaxation instanceof DmvRelaxation) {
                // TODO: Remove this after we've implemented structured logging that can produce the log-space statistics post-hoc.
                DmvRelaxation relax = (DmvRelaxation) prm.relaxation;
                if (numProcessed == 1) {
                    rootLogSpace = relax.getBounds().getLogSpace();
                    logSpaceRemain = rootLogSpace;
                }
                logSpaceRemain = Utilities.logSubtractExact(logSpaceRemain, relax.getBounds().getLogSpace());
            }

            if (result.status == FathomStatus.NotFathomed) {
                addToLeafNodes(result, curNode == rootNode);
            }
        }
        if (nodeTimer.isRunning()) { nodeTimer.stop(); }

        // If we have fathomed all the nodes, then the global solution is within
        // epsilon of the current incumbent.
        if (!hasNextLeafNode()) {
            globalUb = incumbentScore + prm.epsilon*Math.abs(incumbentScore);
        }
        
        // Print summary
        evalIncumbent(incumbentSolution);
        double relativeDiff = computeRelativeDiff(globalUb, incumbentScore);
        if (Utilities.lte(relativeDiff, prm.epsilon, 1e-13)) {
            status = SearchStatus.OPTIMAL_SOLUTION_FOUND;
        }
        printSummary(globalUb, relativeDiff, numProcessed, fathom);
        printTimers(numProcessed);
        clearLeafNodes();

        log.info("B&B search status: " + status);
        
        // Return epsilon optimal solution
        return status;
    }

    protected void evalIncumbent(Solution sol) {
        if (prm.evaluator != null && sol != null) {
            prm.evaluator.evalIncumbent(sol);
        }
    }

    public static class NodeResult {
        public FathomStatus status;
        public List<ProblemNode> children;
        public RelaxedSolution relaxSol;
        public Solution feasSol; 
        public NodeResult() {
            
        }
    }
    
    protected NodeResult processNode(ProblemNode curNode) {
        NodeResult r = new NodeResult();
        prm.relaxation.updateTimeRemaining(prm.timeoutSeconds - nodeTimer.totSec());
        // TODO: else if, ran out of memory or disk space, break

        // The active node can compute a tighter upper bound instead of
        // using its parent's bound
        relaxTimer.start();
        if (prm.disableFathoming) {
            // If not fathoming, don't stop the relaxation early.
            r.relaxSol = prm.relaxation.getRelaxedSolution(curNode);
        } else {
            r.relaxSol = prm.relaxation.getRelaxedSolution(curNode, incumbentScore + prm.epsilon*Math.abs(incumbentScore));
        }
        relaxTimer.stop();
        log.info(String.format("CurrentNode: id=%d depth=%d side=%d relaxScore=%f relaxStatus=%s incumbScore=%f avgNodeTime=%f", curNode.getId(),
                curNode.getDepth(), curNode.getSide(), r.relaxSol.getScore(), r.relaxSol.getStatus().toString(), incumbentScore, nodeTimer.avgMs()));
        if (r.relaxSol.getScore() <= incumbentScore + prm.epsilon*Math.abs(incumbentScore) && !prm.disableFathoming) {
            // Fathom this node: it is either infeasible or was pruned.
            if (r.relaxSol.getStatus() == RelaxStatus.Infeasible) {
                r.status = FathomStatus.Infeasible;
                return r;
            } else if (r.relaxSol.getStatus() == RelaxStatus.Pruned) {
                r.status = FathomStatus.Pruned;
                return r;
            } else {
                log.warn("Unhandled status for relaxed solution: " + r.relaxSol.getStatus() + " Treating as pruned.");
                r.status = FathomStatus.Pruned;
                return r;
            }
        }

        // Check if the child node offers a better feasible solution
        feasTimer.start();
        r.feasSol = prm.projector.getProjectedSolution(r.relaxSol);
        assert (r.feasSol == null || !Double.isNaN(r.feasSol.getScore()));
        if (r.feasSol != null && r.feasSol.getScore() > incumbentScore) {
            incumbentScore = r.feasSol.getScore();
            incumbentSolution = r.feasSol;
            evalIncumbent(incumbentSolution);
            // TODO: pruneActiveNodes();
            // We could store a priority queue in the opposite order (or
            // just a sorted list)
            // and remove nodes from it while their optimisticBound is
            // worse than the
            // new incumbentScore.
        }
        feasTimer.stop();
        
        if (r.feasSol != null && Utilities.equals(r.feasSol.getScore(), r.relaxSol.getScore(), 1e-13)  && !prm.disableFathoming) {
            // Fathom this node: the optimal solution for this subproblem was found.
            r.status = FathomStatus.CompletelySolved;
            return r;
        }
        
        branchTimer.start();
        r.children = curNode.branch(prm.relaxation, r.relaxSol);
        if (r.children.size() == 0) {
            // Fathom this node: no more branches can be made.
            r.status = FathomStatus.BottomedOut;
            return r;
        }
        branchTimer.stop();
        r.status = FathomStatus.NotFathomed;
        return r;
    }

    private static double computeRelativeDiff(double upperBound, double lowerBound) {
        // $(UB - LB) / |LB| <= \epsilon$ implies that $UB <= LB + \epsilon|LB|$, which is our fathoming criteria. 
        // TODO: This is incorrect if the bounds are positive.
        return Math.abs(upperBound - lowerBound) / Math.abs(lowerBound);
    }

    private void printSummary(double upperBound, double relativeDiff, int numProcessed, FathomStats fathom) {
        int numFathomed = fathom.getNumFathomed();
        log.info(String.format("Summary: upBound=%f lowBound=%f relativeDiff=%f #leaves=%d #fathom=%d #prune=%d #infeasible=%d avgFathomDepth=%.0f #seen=%d", 
                upperBound, incumbentScore, relativeDiff, prm.leafNodeOrderer.size(), numFathomed, fathom.numPruned, fathom.numInfeasible, fathom.getAverageDepth(), numProcessed));
    }

    /* ------------ Start Leaf Methods ------------- */
    
    private boolean hasNextLeafNode() {
        return !prm.leafNodeOrderer.isEmpty();
    }

    private ProblemNode getNextLeafNode() {
        ProblemNode node = prm.leafNodeOrderer.remove();
        upperBoundPQ.remove(node);
        return node;
    }

    private void addToLeafNodes(NodeResult result, boolean isRoot) {
        prm.leafNodeOrderer.addChildrenOfResult(result, globalUb, incumbentScore, isRoot);
        if (prm.disableFathoming && upperBoundPQ.size() > 0) {
            // This is a hack to ensure that we don't populate the upperBoundPQ.
            return;
        } else {
            for (ProblemNode child : result.children) {
                upperBoundPQ.add(child);
            }
        }
    }

    private void addRootToLeafNodes(ProblemNode root) {
        prm.leafNodeOrderer.addRoot(root);
        upperBoundPQ.add(root);
    }

    private ProblemNode getWorstLeaf() {
        return upperBoundPQ.peek();
    }

    private void clearLeafNodes() {
        prm.leafNodeOrderer.clear();
        upperBoundPQ.clear();
    }
    
    /* ------------ End Leaf Methods ------------- */

    public Solution getIncumbentSolution() {
        return incumbentSolution;
    }
    
    public double getIncumbentScore() {
        return incumbentScore;
    }
    
    private void printSpaceRemaining(int numProcessed, double rootLogSpace, double logSpaceRemain) {
        // Print stats about the space remaining.
        log.info("Log space remaining (sub): " + logSpaceRemain);
        // TODO: Maybe remove. This is slow and causes a NullPointerException.
        //        if (numProcessed % 2 == 0) {
        //            double logSpaceRemainAdd = computeLogSpaceRemain();
        //            log.info("Log space remaining (add): " + logSpaceRemainAdd);
        //            if (!Utilities.equals(logSpaceRemain, logSpaceRemainAdd, 1e-4)) {
        //                log.warn("Log space remaining differs between subtraction and addition versions.");
        //            }
        //        }
        log.info("Space remaining: " + Utilities.exp(logSpaceRemain));
        log.info("Proportion of root space remaining: " + Utilities.exp(logSpaceRemain - rootLogSpace));
    }

    protected void printTimers(int numProcessed) {
        // Print timers.
        log.debug("Avg time(ms) per node: " + nodeTimer.totMs() / numProcessed);
        log.debug("Avg relax time(ms) per node: " + relaxTimer.totMs() / numProcessed);
        log.debug("Avg project time(ms) per node: " + feasTimer.totMs() / numProcessed);
        log.debug("Avg branch time(ms) per node: " + branchTimer.totMs() / numProcessed);
    }

    private void printLeafNodeBoundHistogram() {
        // Print Histogram
        double[] bounds = new double[prm.leafNodeOrderer.size()];
        int i = 0;
        for (ProblemNode node : prm.leafNodeOrderer) {
            bounds[i] = node.getOptimisticBound();
            i++;
        }
        log.debug(getHistogram(bounds));
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

    public Relaxation getRelaxation() {
        return prm.relaxation;
    }
    
}