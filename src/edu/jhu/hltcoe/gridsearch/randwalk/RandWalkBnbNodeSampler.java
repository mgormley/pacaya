package edu.jhu.hltcoe.gridsearch.randwalk;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.Projector;
import edu.jhu.hltcoe.gridsearch.Relaxation;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.gridsearch.FathomStats.FathomStatus;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Timer;
import gnu.trove.TDoubleArrayList;

/**
 * Implementation of the random walk algorithm (Knuth, 1975) for estimating tree
 * size and solution time.
 * 
 * @author mgormley
 */
public class RandWalkBnbNodeSampler extends LazyBranchAndBoundSolver {
    private static final Logger log = Logger.getLogger(RandWalkBnbNodeSampler.class);

    public static class CostEstimator {

        private TDoubleArrayList estimates = new TDoubleArrayList();
        private double weight = 1;
        private double sum = 0;
        
        public void add(ProblemNode node, NodeResult result, double cost) {
            int numSuccessors = result.children == null ? 0 : result.children.size();
            add(node.getDepth(), numSuccessors, cost);
        }
        
        public void add(int depth, int numSuccesors, double cost) {
            if (depth == 0 && sum > 0) {
                log.warn("sum = " + sum + ". Maybe CostEstimator::doneWithSample() should have been called.");
            }
            sum += cost * weight;
            weight *= numSuccesors;
        }

        /**
         * Cache the current, completed sample.
         */
        public void doneWithSample() {
            estimates.add(sum);
            weight = 1;
            sum = 0;
        }

        public double getMean() {
            return Vectors.mean(estimates.toNativeArray());
        }
        
        public double getVariance() {
            return Vectors.variance(estimates.toNativeArray());
        }
        
        public double getStdDev() {
            return Vectors.stdDev(estimates.toNativeArray());
        }
        
        public int getNumSamples() {
            return estimates.size();
        }
    }
    
    public static class RandWalkBnbSamplerPrm implements LazyBnbSolverFactory {
        public int maxSamples = 10000;
        public LazyBnbSolverPrm bnbPrm = new LazyBnbSolverPrm();
        @Override
        public LazyBranchAndBoundSolver getInstance(Relaxation relaxation, Projector projector) {
            this.bnbPrm.relaxation = relaxation;
            this.bnbPrm.projector = projector;
            return new RandWalkBnbNodeSampler(this);
        }
    }

    private RandWalkBnbSamplerPrm prm;

    public RandWalkBnbNodeSampler(RandWalkBnbSamplerPrm prm) {
        super(prm.bnbPrm);
        this.prm = prm;
    }

    public SearchStatus runBranchAndBound(ProblemNode rootNode, Solution initialSolution, double initialScore) {
        // Initialize
        this.incumbentSolution = initialSolution;
        this.incumbentScore = initialScore;
        int numProcessed = 0;
        CostEstimator nodeCountEst = new CostEstimator();
        CostEstimator solTimeEst = new CostEstimator();
        
        ProblemNode curNode = rootNode;

        evalIncumbent(initialSolution);
        while (true) {
            if (nodeTimer.isRunning()) { nodeTimer.stop(); }
            nodeTimer.start();
            
            numProcessed++;
            
            if (nodeTimer.totSec() > prm.bnbPrm.timeoutSeconds) {
                // Done: Timeout reached.
                break;
            } else if (nodeCountEst.getNumSamples() >= prm.maxSamples) {
                // Done: Collected all the samples.
                break;
            }
            
            // Logging.
            if (log.isDebugEnabled() && numProcessed % 100 == 0) {
                printTimers(numProcessed);
            }
            
            // Process the next node.
            Timer timer = new Timer();
            timer.start();
            NodeResult result = processNode(curNode);

            // Check if this node offers a better feasible solution
            updateIncumbent(result.feasSol);
            timer.stop();
            
            // Update cost estimators.
            nodeCountEst.add(curNode, result, 1);
            solTimeEst.add(curNode, result, timer.totMs());
            
            // Get the next node.
            if (result.status == FathomStatus.NotFathomed) {
                // Get a random child node.
                curNode = result.children.get(Prng.nextInt(result.children.size()));
            } else {
                // Start the next sample at the root.
                curNode = rootNode;
                ((DmvProblemNode)curNode).clear();
                nodeCountEst.doneWithSample();
                solTimeEst.doneWithSample();

                // Print cost estimator summaries. 
                printEstimatorSummaries(nodeCountEst, solTimeEst);
            }
        }
        if (nodeTimer.isRunning()) { nodeTimer.stop(); }

        // Print cost estimator summaries. 
        printEstimatorSummaries(nodeCountEst, solTimeEst);
        
        // Print summary
        evalIncumbent(incumbentSolution);
        printTimers(numProcessed);

        status = SearchStatus.NON_OPTIMAL_SOLUTION_FOUND;
        log.info("B&B search status: " + status);
        return status;
    }

    private void printEstimatorSummaries(CostEstimator nodeCountEst, CostEstimator solTimeEst) {
        log.info("Num samples for estimates: " + nodeCountEst.getNumSamples());
        log.info("Node count estimate mean: " + nodeCountEst.getMean());
        log.info("Node count estimate stddev: " + nodeCountEst.getStdDev());
        log.info("Solution time (ms) estimate mean: " + solTimeEst.getMean());
        log.info("Solution time (ms) estimate stddev: " + solTimeEst.getStdDev());
    }

}
