package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.ProblemNode;

public class FullStrongBranchingDeltaFactory implements DmvBoundsDeltaFactory {

    private static Logger log = Logger.getLogger(FullStrongBranchingDeltaFactory.class);

    // This is the epsilon specified in Tobias Achterberg's thesis for the product score
    private static final double EPSILON = 1e-6;

    private RegretDmvBoundsDeltaFactory regretFactory; 
    
    public FullStrongBranchingDeltaFactory() {
        regretFactory = new RegretDmvBoundsDeltaFactory();
    }

    @Override
    public List<DmvBoundsDelta> getDmvBounds(DmvProblemNode node) {
        // Cache the regret based deltas in case we need them as a fallback
        List<DmvBoundsDelta> regretDeltas = regretFactory.getDmvBounds(node);
        
        IndexedDmvModel idm = node.getIdm();
        DmvBounds origBounds = node.getBounds();
        //RelaxedDmvSolution relaxSol = node.getRelaxedSolution();

        // TODO: consider using regret to filter this down
        double parentBound = node.getOptimisticBound();
        
        double maxScore = EPSILON * EPSILON;
        int maxC = -1;
        int maxM = -1;
        for (int c=0; c<idm.getNumConds(); c++) {
            for (int m=0; m<idm.getNumParams(c); m++) {
                if (!origBounds.canBranch(c, m)) {
                    continue;
                }
                
                node.setAsActiveNode();
                List<DmvBoundsDelta> deltas = split(origBounds, c, m);
                List<ProblemNode> children = node.branch(deltas);
                assert(children.size() == 2);
                
                // Left child
                DmvProblemNode child1 = (DmvProblemNode)children.get(0);
                child1.setAsActiveNode();
                double c1Bound = child1.getOptimisticBound();

                // Right child
                DmvProblemNode child2 = (DmvProblemNode)children.get(0);
                child2.setAsActiveNode();
                double c2Bound = child2.getOptimisticBound();
                
                // Since we're doing maximization...
                double c1Delta = parentBound - c1Bound;
                double c2Delta = parentBound - c2Bound;
                // The product score used in SCIP. See Eq (5.2) in Tobias Achterberg's thesis.
                double score = Math.max(c1Delta, EPSILON) * Math.max(c2Delta, EPSILON);
                
                if (score > maxScore) {
                    maxScore = score;
                    maxC = c;
                    maxM = m;
                }

                String name = idm.getName(c, m);
                log.trace(String.format("Probing: c=%d m=%d name=%s score=%f", c, m, name, score));
            }
        }
        node.setAsActiveNode();
        
        if (maxC == -1 || maxM == -1) {
            return regretDeltas;
        }
        
        String name = idm.getName(maxC, maxM);
        log.info(String.format("Branching: c=%d m=%d name=%s score=%f", maxC, maxM, name, maxScore));

        return split(origBounds, maxC, maxM);
    }
    
    private List<DmvBoundsDelta> split(DmvBounds bounds, int c, int m) {
        return RegretDmvBoundsDeltaFactory.splitHalfProbSpace(bounds, c, m);
    }


}
