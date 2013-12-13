package edu.jhu.globalopt.dmv;

import org.apache.log4j.Logger;

import edu.jhu.eval.DependencyParserEvaluator;
import edu.jhu.globalopt.Solution;
import edu.jhu.globalopt.SolutionEvaluator;

public class DmvSolutionEvaluator implements SolutionEvaluator {

    private static final Logger log = Logger.getLogger(DmvSolutionEvaluator.class);

    private DependencyParserEvaluator evaluator;

    public DmvSolutionEvaluator(DependencyParserEvaluator evaluator) {
        this.evaluator = evaluator;
    }
    
    @Override
    public void evalIncumbent(Solution incumbentSolution) {
        if (evaluator != null && incumbentSolution != null) {
            DmvSolution sol = (DmvSolution) incumbentSolution;
            log.info("Incumbent logLikelihood: " + sol.getScore());
            log.info("Incumbent accuracy: " + evaluator.evaluate(sol.getTreebank()));
        }
    }
    
}
