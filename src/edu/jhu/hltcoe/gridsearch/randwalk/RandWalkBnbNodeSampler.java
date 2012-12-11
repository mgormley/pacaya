package edu.jhu.hltcoe.gridsearch.randwalk;

import java.util.List;

import org.apache.log4j.Logger;
import edu.jhu.hltcoe.util.Timer;

import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.gridsearch.DmvLazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.FathomStats;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.RelaxStatus;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.gridsearch.FathomStats.FathomStatus;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver.NodeResult;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver.SearchStatus;
import edu.jhu.hltcoe.gridsearch.cpt.RandomVariableSelector;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;

/**
 * Implementation of the random walk algorithm (Knuth, 1975) for estimating tree
 * size and solution time.
 * 
 * @author mgormley
 */
public class RandWalkBnbNodeSampler extends DmvLazyBranchAndBoundSolver {
    private static final Logger log = Logger.getLogger(RandWalkBnbNodeSampler.class);

    public static class CostEstimator {

        
        public void add(ProblemNode curNode, double cost) {
            if (curNode.getDepth() == 0) {
                // Reset for new sample.
            }
            
        }
        
    }
    
    public static class RandWalkBnbSamplerPrm {
        public int maxSamples = 10000;
    }

    private RandWalkBnbSamplerPrm prm;

    public RandWalkBnbNodeSampler(RandWalkBnbSamplerPrm prm, double timeoutSeconds, DependencyParserEvaluator evaluator) {
        super(0, null, timeoutSeconds, evaluator);
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

        int numSamples = 0;
        evalIncumbent(initialSolution);
        while (true) {
            if (nodeTimer.isRunning()) { nodeTimer.stop(); }
            nodeTimer.start();
            
            numProcessed++;
            
            if (nodeTimer.totSec() > timeoutSeconds) {
                // Timeout reached.
                break;
            } else if (numSamples >= prm.maxSamples) {
                // Collected all the samples.
                break;
            }
            
            // Logging.
            if (log.isDebugEnabled() && numProcessed % 100 == 0) {
                printTimers(numProcessed);
            }
            
            // Process the next node.
            Timer timer = new Timer();
            timer.start();
            NodeResult result = processNode(curNode, numProcessed);
            timer.stop();
            
            // Update cost estimators.
            nodeCountEst.add(curNode, 1);
            solTimeEst.add(curNode, timer.totMs());
            
            // Get the next node.
            if (result.status != FathomStatus.NotFathomed) {
                // Get a random child node.
                curNode = result.children.get(Prng.nextInt(result.children.size()));
            } else {
                // Start the next sample at the root.
                curNode = rootNode;
                ((DmvProblemNode)curNode).clear();
                numSamples++;
            }
        }
        if (nodeTimer.isRunning()) { nodeTimer.stop(); }

        // Print summary
        evalIncumbent(incumbentSolution);
        printTimers(numProcessed);

        status = SearchStatus.NON_OPTIMAL_SOLUTION_FOUND;
        log.info("B&B search status: " + status);
        return status;
    }

}
