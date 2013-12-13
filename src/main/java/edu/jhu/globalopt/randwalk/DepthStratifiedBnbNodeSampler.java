package edu.jhu.globalopt.randwalk;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.globalopt.LazyBranchAndBoundSolver;
import edu.jhu.globalopt.ProblemNode;
import edu.jhu.globalopt.Projector;
import edu.jhu.globalopt.Relaxation;
import edu.jhu.globalopt.RelaxedSolution;
import edu.jhu.globalopt.Solution;
import edu.jhu.globalopt.dmv.DmvProblemNode;
import edu.jhu.util.Prng;
import edu.jhu.util.Timer;

public class DepthStratifiedBnbNodeSampler extends LazyBranchAndBoundSolver {
    private static final Logger log = Logger.getLogger(DepthStratifiedBnbNodeSampler.class);
    
    public static class DepthStratifiedBnbSamplerPrm implements LazyBnbSolverFactory {
        public int maxDepth = 60;
        public int maxSamples = 10000;
        public LazyBnbSolverPrm bnbPrm = new LazyBnbSolverPrm();
        public LazyBnbSolverPrm safeGetBnbPrm() {
            bnbPrm.epsilon = 0;
            bnbPrm.disableFathoming = true;
            return bnbPrm;
        }
        @Override
        public LazyBranchAndBoundSolver getInstance(Relaxation relaxation, Projector projector) {
            this.bnbPrm.relaxation = relaxation;
            this.bnbPrm.projector = projector;
            return new DepthStratifiedBnbNodeSampler(this);
        }
    }
    
    private DepthStratifiedBnbSamplerPrm prm;

    public DepthStratifiedBnbNodeSampler(DepthStratifiedBnbSamplerPrm prm) {
        super(prm.safeGetBnbPrm());
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
            while (curNode.getDepth() < curDiveDepth && totalTimer.totSec() < prm.bnbPrm.timeoutSeconds) {
                RelaxedSolution relaxSol = prm.bnbPrm.relaxation.getRelaxedSolution(curNode, incumbentScore);
                // Branch.
                List<ProblemNode> children = curNode.branch(prm.bnbPrm.relaxation, relaxSol);
                // Get a random child as the new current node.
                curNode = children.get(Prng.nextInt(children.size()));
            }            

            if (totalTimer.totSec() > prm.bnbPrm.timeoutSeconds) {
                // Done: Timeout reached.
                break;
            } else if (numProcessed >= prm.maxSamples) {
                break;
            }
            
            nodeTimer.start();
            
            // Logging
            if (log.isDebugEnabled() && numProcessed % 100 == 0) {
                printTimers(numProcessed);
            }
            
            // Process node.
            NodeResult result = processNode(curNode);

            // Check if this node offers a better feasible solution
            updateIncumbent(result.feasSol);
            
            numProcessed++;

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
