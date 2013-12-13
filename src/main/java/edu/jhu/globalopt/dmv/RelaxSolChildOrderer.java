package edu.jhu.globalopt.dmv;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.globalopt.ChildOrderer;
import edu.jhu.globalopt.ProblemNode;
import edu.jhu.globalopt.RelaxedSolution;
import edu.jhu.globalopt.cpt.CptBoundsDelta;
import edu.jhu.globalopt.cpt.CptBoundsDelta.Lu;
import edu.jhu.globalopt.cpt.CptBoundsDelta.Type;
import edu.jhu.prim.Primitives;
import edu.jhu.util.Prng;

/**
 * The LP solution guided rule of Martin (1998) to order the children
 * during plunging or depth first search.
 * 
 * @author mgormley
 * 
 */
public class RelaxSolChildOrderer implements ChildOrderer {

    private double equalityTolerance = 1e-10;

    @Override
    public List<ProblemNode> orderChildren(RelaxedSolution relaxSol, RelaxedSolution rootSol, List<ProblemNode> children) {
        if (children.size() > 2) {
            throw new IllegalStateException("The LP guided rule we use expects a binary branching tree.");
        }
        if (!(children.get(0) instanceof DmvProblemNode) || !(children.get(1) instanceof DmvProblemNode)) {
            throw new IllegalStateException("Children must be DmvProblemNodes");
        }
        DmvProblemNode c1 = (DmvProblemNode) children.get(0);
        DmvProblemNode c2 = (DmvProblemNode) children.get(1);

        CptBoundsDelta d1 = c1.getDeltas().getPrimary();
        CptBoundsDelta d2 = c2.getDeltas().getPrimary();
        
        if (!(d1.getLu() == Lu.UPPER && d2.getLu() == Lu.LOWER)) {
            throw new IllegalStateException(
                    "Expecting the first child to be (var <= const) and the second child to be (var >= const).");
        }
        
        DmvRelaxedSolution root = (DmvRelaxedSolution) rootSol;
        DmvRelaxedSolution parent = (DmvRelaxedSolution) relaxSol;
                
        int c = d1.getC();
        int m = d1.getM();
        Type type = d1.getType();
                
        List<ProblemNode> order = new ArrayList<ProblemNode>(children.size());
        assert type == Type.PARAM || type == Type.COUNT;
        
        if (!parent.getStatus().hasSolution() ||
                (type == Type.PARAM && Primitives.equals(parent.getLogProbs()[c][m], root.getLogProbs()[c][m], equalityTolerance)) ||
                (type == Type.COUNT && Primitives.equals(parent.getFeatCounts()[c][m], root.getFeatCounts()[c][m], equalityTolerance))) {
            // Break ties randomly.
            if (Prng.nextBoolean()) {
                order.add(c1);
                order.add(c2);
            } else {
                order.add(c2);
                order.add(c1);
            }
        } else if ((type == Type.PARAM && parent.getLogProbs()[c][m] < root.getLogProbs()[c][m]) ||
                (type == Type.COUNT && parent.getFeatCounts()[c][m] < root.getFeatCounts()[c][m])) { 
            order.add(c2); // push onto stack first (low priority)
            order.add(c1); // push onto stack second (high priority)
        } else {
            order.add(c1); // push onto stack first (low priority)
            order.add(c2); // push onto stack second (high priority)
        }
        return order;
    }

}
