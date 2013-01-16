package edu.jhu.hltcoe.gridsearch.cpt;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDmvSolution;
import edu.jhu.hltcoe.util.IntTuple;
import edu.jhu.hltcoe.util.Utilities;

public class RegretVariableSelector implements VariableSelector {

    private static final Logger log = Logger.getLogger(RegretVariableSelector.class);
    private RandomVariableSelector randBrancher;

    public RegretVariableSelector() {
        this.randBrancher = new RandomVariableSelector(true);
    }

    @Override
    public VariableId select(DmvProblemNode node, DmvRelaxation relax, RelaxedDmvSolution relaxSol) {
        CptBounds origBounds = relax.getBounds();
        double[][] regret = getRegretCm(relaxSol);

        if (regret == null) {
            // Back off to random branching.
            log.warn("Regret not available. Backing off to random.");
            return randBrancher.select(node, relax, relaxSol);
        }
        
        // Don't branch on variables that have bottomed out
        for (int c=0; c<regret.length; c++) {
            for (int m=0; m<regret[c].length; m++) {
                if (!origBounds.canBranch(Type.PARAM,c, m)) {
                    regret[c][m] = Double.NEGATIVE_INFINITY;
                }
            }
        }
        
        IntTuple max = Utilities.getArgmax(regret);
        int c = max.get(0);
        int m = max.get(1);

        assert(!Double.isNaN(regret[c][m]));
        
        if (c == -1 || m == -1) {
            log.warn("Branching bottomed-out at node " + node.getId());
            return new VariableId();
        }
        
        String name = relax.getIdm().getName(c, m);
        log.info(String.format("Branching: c=%d m=%d name=%s regret=%f", c, m, name, regret[c][m]));
        assert(!Double.isInfinite(regret[c][m]));
        
        // TODO: make this an option: split at current value
        // TODO: as is, this is buggy: it will sometimes set the ub to -inf which is lower than the lb
        //RelaxedDmvSolution relaxSol = node.getRelaxedSolution();
        //return splitAtMidPoint(origBounds, c, m, relaxSol.getLogProbs()[c][m]);
        
        return new VariableId(c, m);
    }

    public static double[][] getRegretCm(RelaxedDmvSolution relaxSol) {
        return getRegretCm(relaxSol.getLogProbs(), relaxSol.getFeatCounts(), relaxSol.getObjVals());
    }

    /**
     * @return A CxM array of doubles containing the regret of each model
     *         parameter, or null if the regret is unavailable.
     */
    public static double[][] getRegretCm(double[][] logProbs, double[][] featCounts, double[][] objVals) {
        // If optimal model parameters \theta_{c,m} are not present, return null.
        if (logProbs == null) {
            return null;
        }

        // Compute the regret as the difference between the
        // objective value and true objective value
        double[][] regret = new double[logProbs.length][];
        for (int c = 0; c < regret.length; c++) {
            regret[c] = new double[logProbs[c].length];
            for (int m = 0; m < regret[c].length; m++) {
                regret[c][m] = objVals[c][m] - (logProbs[c][m] * featCounts[c][m]);
                //TODO: this seems to be too strong:
                //assert Utilities.gte(regret[c][m], 0.0, 1e-7) : String.format("regret[%d][%d] = %f", c, m, regret[c][m]);
                if (!Utilities.gte(regret[c][m], 0.0, 1e-7)) {
                    log.warn(String.format("Invalid negative regret: regret[%d][%d] = %f", c, m, regret[c][m]));
                }
            }
        }

        return regret;
    }
    
}
