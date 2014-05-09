package edu.jhu.gm.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.jhu.gm.inf.BeliefPropagation.Messages;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.parse.cky.CnfGrammar;
import edu.jhu.parse.cky.Rule;
import edu.jhu.parse.cky.chart.Chart;

/**
 * only does bracketing
 */
public class ConstituencyTreeFactor extends AbstractGlobalFactor implements GlobalFactor {

    private static final long serialVersionUID = 1L;
    
    public static class SpanVar extends Var {

        private static final long serialVersionUID = 1L;
        private static final List<String> stateNames = Arrays.asList("false", "true");
        public static final int FALSE = 0;
        public static final int TRUE = 1;
        
        private int i, j;

        public SpanVar(int i, int j) {
            super(VarType.LATENT, 2, String.format("Span(%d,%d)", i, j), stateNames);
        }
    }
    
    public static final CnfGrammar bracketingGrammar;
    static {
		Rule xGoesToXx = new Rule(1, 1, 1, Double.NaN, null, null);
		Rule xGoesToY = new Rule(1, 2, Rule.UNARY_RULE, Double.NaN, null, null);
		ArrayList<Rule> rules = new ArrayList<Rule>();
		rules.add(xGoesToXx);
		rules.add(xGoesToY);
		LoopOrder loopOrder = LoopOrder.LEFT_CHILD;
		bracketingGrammar = new CnfGrammar(rules, 1, null, null, loopOrder);
    }
    
    private Chart chart;
    private SpanVar[][] spanVars;
    private VarSet varSet;
    
    public ConstituencyTreeFactor(int n) {
        varSet = new VarSet();
        spanVars = new SpanVar[n][n];
        for(int i=0; i<n-1; i++) {
            for(int j=i+1; j<n; j++) {
                spanVars[i][j] = new SpanVar(i, j);
                varSet.add(spanVars[i][j]);
            }
        }
    }

    @Override
    public Factor getClamped(VarConfig clmpVarConfig) {
        if (clmpVarConfig.size() == 0) {
            // None clamped.
            return this;
        } else if (clmpVarConfig.size() == varSet.size()) {
            // All clamped.
            return new ProjDepTreeFactor(0, VarType.OBSERVED);
        } else {
            // Some clamped.
            throw new IllegalStateException("Unable to clamp these variables.");
        }
    }

    @Override
    public VarSet getVars() {
        return varSet;
    }

    @Override
    public double getUnormalizedScore(int config) {
        VarConfig vc = varSet.getVarConfig(config);
        return getUnormalizedScore(vc);
    }

    @Override
    public double getUnormalizedScore(VarConfig config) {
        throw new RuntimeException("implement me");
    }

    @Override
    public double getExpectedLogBelief(FgNode parent, Messages[] msgs, boolean logDomain) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected void createMessages(FgNode parent, Messages[] msgs, boolean logDomain, boolean normalizeMessages) {
        // TODO Auto-generated method stub
        
    }

}
