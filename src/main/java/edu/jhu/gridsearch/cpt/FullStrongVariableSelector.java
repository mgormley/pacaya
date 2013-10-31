package edu.jhu.gridsearch.cpt;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.gridsearch.ProblemNode;
import edu.jhu.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.gridsearch.dmv.DmvProblemNode;
import edu.jhu.gridsearch.dmv.DmvRelaxation;
import edu.jhu.gridsearch.dmv.DmvRelaxedSolution;
import edu.jhu.gridsearch.dmv.IndexedDmvModel;

public class FullStrongVariableSelector implements VariableSelector {

    private static final Logger log = Logger.getLogger(FullStrongVariableSelector.class);

    // This is the epsilon specified in Tobias Achterberg's thesis for the product score
    public static final double EPSILON = 1e-6;

    private RegretVariableSelector regretFactory;
    private VariableSplitter varSplitter; 
    
    public FullStrongVariableSelector(VariableSplitter varSplitter) {
        regretFactory = new RegretVariableSelector();
        this.varSplitter = varSplitter; 
    }

    @Override
    public VariableId select(DmvProblemNode node, DmvRelaxation relax, DmvRelaxedSolution relaxSol) {
        // Cache the regret based deltas in case we need them as a fallback
        VariableId regretVarId = regretFactory.select(node, relax, relaxSol);
        
        IndexedDmvModel idm = relax.getIdm();
        CptBounds origBounds = relax.getBounds();
        //RelaxedDmvSolution relaxSol = node.getRelaxedSolution();

        // TODO: consider using regret to filter this down
        
        double maxScore = EPSILON * EPSILON;
        int maxC = -1;
        int maxM = -1;
        for (int c=0; c<idm.getNumConds(); c++) {
            for (int m=0; m<idm.getNumParams(c); m++) {
                if (!origBounds.canBranch(Type.PARAM, c, m)) {
                    continue;
                }
                
                double score = getStrongScore(node, c, m, relax);
                
                if (score > maxScore) {
                    maxScore = score;
                    maxC = c;
                    maxM = m;
                }
            }
        }
        
        if (maxC == -1 || maxM == -1) {
            return regretVarId;
        }
        
        String name = idm.getName(maxC, maxM);
        log.info(String.format("Branching: c=%d m=%d name=%s score=%f", maxC, maxM, name, maxScore));

        return new VariableId(maxC, maxM);
    }

    public double getStrongScore(DmvProblemNode node, int c, int m, DmvRelaxation relax) {
        CptBounds origBounds = relax.getBounds();
        double parentBound = node.getLocalUb();

        List<CptBoundsDeltaList> deltas = varSplitter.split(origBounds, new VariableId(c, m));
        List<ProblemNode> children = node.branch(deltas);
        assert(children.size() == 2);
        
        // Left child
        DmvProblemNode child1 = (DmvProblemNode)children.get(0);
        relax.getRelaxedSolution(child1);
        double c1Bound = child1.getLocalUb();

        // Right child
        DmvProblemNode child2 = (DmvProblemNode)children.get(1);
        relax.getRelaxedSolution(child2);
        double c2Bound = child2.getLocalUb();
        
        // Since we're doing maximization...
        double c1Delta = parentBound - c1Bound;
        double c2Delta = parentBound - c2Bound;
        // The product score used in SCIP. See Eq (5.2) in Tobias Achterberg's thesis.
        double score = computeScore(c1Delta, c2Delta);

        String name = relax.getIdm().getName(c, m);
        log.trace(String.format("Probing: c=%d m=%d name=%s score=%f", c, m, name, score));

        return score;
    }
    
    /**
     * The product score used in SCIP. See Eq (5.2) in Tobias Achterberg's thesis.
     */
    public static double computeScore(double c1Delta, double c2Delta) {
        return Math.max(c1Delta, EPSILON) * Math.max(c2Delta, EPSILON);
    }

}
