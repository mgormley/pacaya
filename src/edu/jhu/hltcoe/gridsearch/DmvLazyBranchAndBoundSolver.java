/**
 * 
 */
package edu.jhu.hltcoe.gridsearch;

import java.util.Comparator;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.gridsearch.dmv.DmvSolution;

public class DmvLazyBranchAndBoundSolver extends LazyBranchAndBoundSolver {

    private static Logger log = Logger.getLogger(DmvLazyBranchAndBoundSolver.class);
    private DependencyParserEvaluator evaluator;
    
    public DmvLazyBranchAndBoundSolver(double epsilon, Comparator<ProblemNode> leafComparator, double timeoutSeconds, DependencyParserEvaluator evaluator) {
        super(epsilon, leafComparator, timeoutSeconds);
        this.evaluator = evaluator;
    }
    
    @Override
    protected void evalIncumbent(Solution incumbentSolution, double incumbentScore) {
        if (evaluator != null && incumbentSolution != null) {
            log.info("Incumbent logLikelihood: " + incumbentScore);
            DmvSolution sol = (DmvSolution) incumbentSolution;
            log.info("Incumbent accuracy: " + evaluator.evaluate(sol.getTreebank()));
        }
    }
}