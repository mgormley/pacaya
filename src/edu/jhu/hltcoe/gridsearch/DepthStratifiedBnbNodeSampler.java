package edu.jhu.hltcoe.gridsearch;

import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.gridsearch.cpt.RandomVariableSelector;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Time;

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
        Stopwatch totalTimer = new Stopwatch();
        totalTimer.start();
        while (true) {
            if (nodeTimer.isRunning()) { nodeTimer.stop(); }
            // Off the clock...
            // Starting at the root, randomly dive to curDiveDepth.
            ProblemNode curNode = rootNode;
            ((DmvProblemNode)curNode).clear();
            while (curNode.getDepth() < curDiveDepth) {
                curNode.setAsActiveNode();
                curNode.getOptimisticBound();
                // Branch.
                List<ProblemNode> children = curNode.branch();
                // Get a random child as the new current node.
                curNode = children.get(Prng.nextInt(children.size()));
            }            
            
            nodeTimer.start();

            switchTimer.start();
            numProcessed++;
            // Logging
            if (log.isDebugEnabled() && numProcessed % 100 == 0) {
                printTimers(numProcessed);
            }
            curNode.setAsActiveNode();
            switchTimer.stop();
            
            totalTimer.stop();
            totalTimer.start();
            if (Time.totSec(totalTimer) > timeoutSeconds) {
                // Timeout reached.
                break;
            }
            
            // The active node can compute a tighter upper bound instead of
            // using its parent's bound
            relaxTimer.start();
            // Get but discard the lower bound. If not fathoming, don't use the
            // incumbent score to stop the relaxation early.
            curNode.getOptimisticBound();
            RelaxedSolution relax = curNode.getRelaxedSolution();
            relaxTimer.stop();
            
            log.info(String.format("CurrentNode: id=%d depth=%d side=%d relaxScore=%f relaxStatus=%s incumbScore=%f avgNodeTime=%f", curNode.getId(),
                    curNode.getDepth(), curNode.getSide(), relax.getScore(), relax.getStatus().toString(), incumbentScore, Time.totMs(nodeTimer) / numProcessed));

            // Check if the child node offers a better feasible solution
            feasTimer.start();
            Solution sol = curNode.getFeasibleSolution();
            assert (sol == null || !Double.isNaN(sol.getScore()));
            if (sol != null && sol.getScore() > incumbentScore) {
                incumbentScore = sol.getScore();
                incumbentSolution = sol;
                evalIncumbent(incumbentSolution);
            }
            feasTimer.stop();
            
            branchTimer.start();
            // Branch so that we expend the time, but just discard the children.
            curNode.branch();
            branchTimer.stop();
            
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
