package edu.jhu.hltcoe.gridsearch.randwalk;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.gridsearch.DmvLazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Timer;

public class DepthStratifiedBnbNodeSampler extends DmvLazyBranchAndBoundSolver {
    private static final Logger log = Logger.getLogger(DepthStratifiedBnbNodeSampler.class);
    
    public static class DepthStratifiedBnbSamplerPrm {
        public int maxDepth = 60;
    }
    
    private DepthStratifiedBnbSamplerPrm prm;

    public DepthStratifiedBnbNodeSampler(DepthStratifiedBnbSamplerPrm prm, double timeoutSeconds, DependencyParserEvaluator evaluator) {
        super(0, null, timeoutSeconds, evaluator);
        this.prm = prm;
    }

    @Override
    public SearchStatus runBranchAndBound(ProblemNode rootNode, Solution initialSolution, double initialScore) {
        // Initialize
        this.incumbentSolution = initialSolution;
        this.incumbentScore = initialScore;
        int numProcessed = 0;
        
        evalIncumbent(initialSolution);
        int curDiveDepth = 0;
        Timer totalTimer = new Timer();
        totalTimer.start();
        while (true) {
            if (nodeTimer.isRunning()) { nodeTimer.stop(); }
            // Off the clock...
            // Starting at the root, randomly dive to curDiveDepth.
            ProblemNode curNode = rootNode;
            ((DmvProblemNode)curNode).clear();
            while (curNode.getDepth() < curDiveDepth && totalTimer.totSec() < timeoutSeconds) {
                curNode.setAsActiveNode();
                curNode.getOptimisticBound();
                // Branch.
                List<ProblemNode> children = curNode.branch();
                // Get a random child as the new current node.
                curNode = children.get(Prng.nextInt(children.size()));
            }            

            if (totalTimer.totSec() > timeoutSeconds) {
                // Done: Timeout reached.
                break;
            }
            
            nodeTimer.start();
            
            // Logging
            if (log.isDebugEnabled() && numProcessed % 100 == 0) {
                printTimers(numProcessed);
            }
            
            // Process and discard the result.
            processNode(curNode);

            // Update the dive depth for the next dive.
            curDiveDepth = (curDiveDepth + 1) % prm.maxDepth;
        }
        
        // Print summary
        evalIncumbent(incumbentSolution);
        printTimers(numProcessed);

        SearchStatus status = SearchStatus.NON_OPTIMAL_SOLUTION_FOUND;
        log.info("B&B search status: " + status);
        return status;
    }

}
