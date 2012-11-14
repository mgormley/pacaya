/**
 * 
 */
package edu.jhu.hltcoe.gridsearch;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.gridsearch.dmv.DmvSolution;

public class DmvLazyBranchAndBoundSolver extends LazyBranchAndBoundSolver {

    private static final Logger log = Logger.getLogger(DmvLazyBranchAndBoundSolver.class);
    private DependencyParserEvaluator evaluator;
    
    public DmvLazyBranchAndBoundSolver(double epsilon, NodeOrderer nodeOrderer, double timeoutSeconds, DependencyParserEvaluator evaluator) {
        super(epsilon, nodeOrderer, timeoutSeconds);
        this.evaluator = evaluator;
    }
    
    @Override
    protected void evalIncumbent(Solution incumbentSolution) {
        if (evaluator != null && incumbentSolution != null) {
            DmvSolution sol = (DmvSolution) incumbentSolution;
            log.info("Incumbent logLikelihood: " + sol.getScore());
            log.info("Incumbent accuracy: " + evaluator.evaluate(sol.getTreebank()));
        }
    }
}