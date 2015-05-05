package edu.jhu.pacaya.gm.model.globalfac;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.nlp.data.Sentence;
import edu.jhu.pacaya.parse.cky.CnfGrammar;
import edu.jhu.pacaya.parse.cky.CnfGrammarReader;
import edu.jhu.pacaya.parse.cky.PcfgInsideOutside;
import edu.jhu.pacaya.parse.cky.Rule;
import edu.jhu.pacaya.parse.cky.Scorer;
import edu.jhu.pacaya.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.pacaya.parse.cky.PcfgInsideOutside.PcfgInsideOutsidePrm;
import edu.jhu.pacaya.parse.cky.PcfgInsideOutside.PcfgIoChart;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.util.math.FastMath;

/**
 * Global factor which constrains O(n^2) variables to form a constituency tree,
 * following Naradowsky, Vieira, & Smith (2012)'s CKYTree factor.
 * 
 * @author mgormley
 */
public class ConstituencyTreeFactor extends AbstractConstraintFactor implements GlobalFactor {

    private static final long serialVersionUID = 1L;

    public static final CnfGrammar grammar;
    public static final int nonTerminalSymbol, terminalSymbol;
    static {
        try {
            StringReader reader = new StringReader(
                    "X\n" +             // Root symbol.
                    "X --> X X 0\n" +   // Binary rule.
                    "===\n" +           // Separator.
                    "X --> a 0\n"       // Unary rule.
                    );
            CnfGrammarReader builder = new CnfGrammarReader();
            builder.loadFromReader(reader);
            grammar = builder.getGrammar(LoopOrder.LEFT_CHILD);
            nonTerminalSymbol = grammar.getNtAlphabet().lookupIndex("X");
            terminalSymbol = grammar.getLexAlphabet().lookupIndex("a");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Span variable. When true it indicates that there is a span from start to end.
     * 
     * @author mgormley
     */
    public static class SpanVar extends Var {

        private static final long serialVersionUID = 1L;

        // The variable states.
        public static final int TRUE = 1;
        public static final int FALSE = 0;

        private static final List<String> BOOLEANS = Lists.getList("FALSE", "TRUE");
        private int start;
        private int end;

        public SpanVar(VarType type, String name, int start, int end) {
            super(type, BOOLEANS.size(), name, BOOLEANS);
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public static String getDefaultName(int i, int j) {
            return String.format("Span_%d_%d", i, j);
        }
    }
    
    private static final Logger log = LoggerFactory.getLogger(ConstituencyTreeFactor.class);
    
    private final VarSet vars;
    /** The sentence length. */
    private final int n;
    private SpanVar[][] spanVars;
    // Internal sentence used by parser.
    private Sentence sentence;

    /**
     * Constructor.
     * @param n The length of the sentence.
     */
    public ConstituencyTreeFactor(int n, VarType type) {
        super();
        this.vars = createVarSet(n, type);
        this.n = n;

        // TODO: We created the VarSet statically and then find extract the vars
        // again from the VarSet only because we're subclassing Factor. In the
        // future, we should drop this.
        spanVars = new SpanVar[n][n+1];
        VarSet vars = this.getVars();
        for (Var var : vars) {
            SpanVar span = (SpanVar) var;
            spanVars[span.getStart()][span.getEnd()] = span;
        }

        // Construct sentence.
        int[] sent = new int[n];
        Arrays.fill(sent, grammar.getLexAlphabet().lookupIndex("a"));
        sentence = new Sentence(grammar.getLexAlphabet(), sent);
    }

    /**
     * Get the span var corresponding to the specified start and end positions.
     * 
     * @param start The start of the span.
     * @param end The end of the span.
     * @return The span variable.
     */
    public SpanVar getSpanVar(int start, int end) {
        return spanVars[start][end];
    }

    private static VarSet createVarSet(int n, VarType type) {
        // Add a variable for each span.
        VarSet vars = new VarSet();
        // For each cell in the chart. (width increasing)
        for (int width = 1; width <= n; width++) {
            for (int start = 0; start <= n - width; start++) {
                int end = start + width;
                String name = SpanVar.getDefaultName(start, end);
                vars.add(new SpanVar(type, name, start, end));
            }
        }
        return vars;
    }

    @Override
    public void createMessages(VarTensor[] inMsgs, VarTensor[] outMsgs) {
        Algebra s = inMsgs[0].getAlgebra();
        if (!s.equals(RealAlgebra.REAL_ALGEBRA) && !s.equals(LogSemiring.LOG_SEMIRING)) {
            throw new IllegalStateException("ConstituencyTreeFactor only supports log and real semirings as input.");
        }
        
        // All internal computation is done in the logDomain
        // since (for example) pi the product of all incoming false messages
        // would overflow.
        final double[][] spanWeights = new double[n][n+1];
        getLogOddsRatios(inMsgs, spanWeights);
        double pi = getProductOfAllFalseMessages(inMsgs);

        // Compute the constituency tree marginals, summing over all 
        // constiuency trees via the inside-outside algorithm.
        PcfgInsideOutsidePrm prm = new PcfgInsideOutsidePrm();
        prm.scorer = new Scorer() {            
            @Override
            public double score(Rule r, int start, int mid, int end) {
                return spanWeights[start][end];
            }
        };
        PcfgInsideOutside io = new PcfgInsideOutside(prm);
        PcfgIoChart chart = io.runInsideOutside(sentence, grammar);

        // partition = pi * \sum_{y \in Trees} \prod_{edge \in y} weight(edge) 
        // Here we store the log partition.
        double partition = pi + chart.getLogPartitionFunction();

        if (log.isTraceEnabled()) {
            log.trace(String.format("partition: %.2f", partition));
        }

        // Create the messages and stage them in the Messages containers.
        for (int i=0; i<inMsgs.length; i++) {
            VarTensor inMsg = inMsgs[i];
            VarTensor outMsg = outMsgs[i];
            SpanVar span = (SpanVar) inMsg.getVars().get(0);
            
            // The beliefs are computed as follows.
            // beliefTrue = pi * FastMath.exp(chart.getLogSumOfPotentials(link.getParent(), link.getChild()));
            // beliefFalse = partition - beliefTrue;
            // 
            // Then the outgoing messages are computed as:
            // outMsgTrue = beliefTrue / inMsgTrue
            // outMsgFalse = beliefFalse / inMsgFalse
            // 
            // Here we compute the logs of these quantities.

            double beliefTrue = pi + chart.getLogSumOfPotentials(nonTerminalSymbol, span.getStart(), span.getEnd());
            double beliefFalse = safeLogSubtract(partition, beliefTrue);

            // TODO: Detect possible numerical precision error.

            // Get the incoming messages.
            double inMsgTrue = s.toLogProb(inMsg.getValue(SpanVar.TRUE));
            double inMsgFalse = s.toLogProb(inMsg.getValue(SpanVar.FALSE));
            
            double outMsgTrue = beliefTrue - inMsgTrue;
            double outMsgFalse = beliefFalse - inMsgFalse;

            outMsgTrue = (inMsgTrue == Double.NEGATIVE_INFINITY) ? Double.NEGATIVE_INFINITY : outMsgTrue;
            outMsgFalse = (inMsgFalse == Double.NEGATIVE_INFINITY) ? Double.NEGATIVE_INFINITY : outMsgFalse;
            
            setOutMsgs(outMsg, span, outMsgTrue, outMsgFalse);
        }
    }

    private double safeLogSubtract(double partition, double beliefTrue) {
        double outMsgFalse;
        if (partition < beliefTrue) {
            // This will happen very frequently if the log-add table is used
            // instead of "exact" log-add.
            if (log.isTraceEnabled()) {
                log.trace(String.format("Partition function less than belief: partition=%.20f belief=%.20f", partition, beliefTrue));
            }
            // To get around the floating point error, we truncate the
            // subtraction to log(0).
            outMsgFalse = Double.NEGATIVE_INFINITY;
            unsafeLogSubtracts++;
        } else {
            outMsgFalse = FastMath.logSubtractExact(partition, beliefTrue);
        }
        logSubtractCount++;
        return outMsgFalse;
    }
    private static int unsafeLogSubtracts = 0;
    private static int logSubtractCount = 0;
    private static int extremeOddsRatios = 0;
    private static int oddsRatioCount = 0;

    /** Computes the log odds ratio for each edge. w_{ij} = \mu_{ij}(1) / \mu_{ij}(0) */
    private void getLogOddsRatios(VarTensor[] inMsgs, double[][] spanWeights) {
        Algebra s = inMsgs[0].getAlgebra();
        
        // Compute the odds ratios of the messages for each edge in the tree.
        DoubleArrays.fill(spanWeights, Double.NEGATIVE_INFINITY);
        for (VarTensor inMsg : inMsgs) {
            SpanVar span = (SpanVar) inMsg.getVars().get(0);
            double oddsRatio = s.toLogProb(inMsg.getValue(SpanVar.TRUE)) - s.toLogProb(inMsg.getValue(SpanVar.FALSE));
            spanWeights[span.getStart()][span.getEnd()] = oddsRatio;                
        }

        checkSpanWeights(spanWeights);
    }

    private void checkSpanWeights(double[][] spanWeights) {
        // Keep track of the minimum and maximum odds ratios, in order to detect
        // possible numerical precision issues.
        double minOddsRatio = Double.POSITIVE_INFINITY;
        double maxOddsRatio = Double.NEGATIVE_INFINITY;
        
        for (int i=0; i<spanWeights.length; i++) {
            for (int j=0; j<spanWeights[i].length; j++) {
                double oddsRatio = spanWeights[i][j];
                // Check min/max.
                if (oddsRatio < minOddsRatio && oddsRatio != Double.NEGATIVE_INFINITY) {
                    // Don't count *negative* infinities when logging extreme odds ratios.
                    minOddsRatio = oddsRatio;
                }
                if (oddsRatio > maxOddsRatio) {
                    maxOddsRatio = oddsRatio;
                }
            }
        }

        // Check whether the max/min odds ratios (if added) would result in a
        // floating point error.
        oddsRatioCount++;
        if (FastMath.logSubtractExact(FastMath.logAdd(maxOddsRatio, minOddsRatio), maxOddsRatio) == Double.NEGATIVE_INFINITY) {
            extremeOddsRatios++;
            log.debug(String.format("maxOddsRatio=%.20g minOddsRatio=%.20g", maxOddsRatio, minOddsRatio));
            log.debug(String.format("Proportion extreme odds ratios:  %f (%d / %d)", (double) extremeOddsRatios/ oddsRatioCount, extremeOddsRatios, oddsRatioCount));
            // We log the proportion of unsafe log-subtracts here only as a convenient way of highlighting the two floating point errors together.
            log.debug(String.format("Proportion unsafe log subtracts:  %f (%d / %d)", (double) unsafeLogSubtracts / logSubtractCount, unsafeLogSubtracts, logSubtractCount));
        }
    }

    /** Computes pi = \prod_i \mu_i(0). */
    private double getProductOfAllFalseMessages(VarTensor[] inMsgs) {
        // Precompute the product of all the "false" messages.
        // pi = \prod_i \mu_i(0)
        // Here we store log pi.
        Algebra s = inMsgs[0].getAlgebra();
        double logPi = 0.0;
        for (VarTensor inMsg : inMsgs) {
            logPi += s.toLogProb(inMsg.getValue(SpanVar.FALSE));
        }
        return logPi;
    }

    /** Sets the outgoing messages. */
    private void setOutMsgs(VarTensor outMsg, SpanVar span, double outMsgTrue, double outMsgFalse) {
        
        // Set the outgoing messages.
        Algebra s = outMsg.getAlgebra();
        outMsg.setValue(SpanVar.FALSE, s.fromLogProb(outMsgFalse));
        outMsg.setValue(SpanVar.TRUE, s.fromLogProb(outMsgTrue));
                
        if (log.isTraceEnabled()) {
            log.trace(String.format("outMsgTrue: %s = %.2f", span.getName(), outMsg.getValue(LinkVar.TRUE)));
            log.trace(String.format("outMsgFalse: %s = %.2f", span.getName(), outMsg.getValue(LinkVar.FALSE)));
        }
        
        assert !outMsg.containsBadValues() : "message = " + outMsg;
    }

    @Override
    public double getExpectedLogBelief(VarTensor[] inMsgs) {
        if (n == 0) {
            return 0.0;
        }
        log.warn("Expected log belief not implemented for constituency tree factor"
                + " (this requires a first-order expectation semiring computation)." 
                + " Returning zero instead.");
        return 0.0;
    }

    public SpanVar[][] getSpanVars() {
        return spanVars;
    }

    @Override
    public VarSet getVars() {
        return vars;
    }

    @Override
    public Factor getClamped(VarConfig clmpVarConfig) {
        if (clmpVarConfig.size() == 0) {
            // None clamped.
            return this;
        } else if (clmpVarConfig.size() == vars.size()) {
            // All clamped.
            return new ConstituencyTreeFactor(0, VarType.OBSERVED);
        } else {
            // Some clamped.
            throw new IllegalStateException("Unable to clamp these variables.");
        }
    }

    @Override
    public double getLogUnormalizedScore(int configId) {
        VarConfig vc = vars.getVarConfig(configId);
        // TODO: This would be faster: int[] cfg = vars.getVarConfigAsArray(configId);
        return getLogUnormalizedScore(vc);
    }

    @Override
    public double getLogUnormalizedScore(VarConfig vc) {
        LogSemiring s = LogSemiring.LOG_SEMIRING;
        boolean[][] chart = getChart(n, vc);
        if (chart == null || !isTree(n, chart)) {
            log.warn("Tree is not a valid constituency tree.");
            return s.zero();
        }
        return s.one();
    }

    /**
     * Creates a boolean chart representing the set of spans which are "on", or
     * null if the number of span variables in the assignment is invalid.
     */
    private static boolean[][] getChart(int n, VarConfig vc) {
        boolean[][] chart = new boolean[n][n+1];
        int count = 0;
        for (Var v : vc.getVars()) {
            SpanVar span = (SpanVar) v;
            chart[span.getStart()][span.getEnd()] = (vc.getState(span) == SpanVar.TRUE);
            count++;
        }
        if (count != (n*(n+1)/2)) {
            // We didn't see a span variable for every chart cell.
            return null;
        }
        return chart;
    }

    /**
     * Returns true if the boolean chart represents a set of spans that form a
     * valid constituency tree, false otherwise.
     */
    private static boolean isTree(int n, boolean[][] chart) {
        if (!chart[0][n]) {
            // Root must be a span.
            return false;
        }
        for (int width = 1; width <= n; width++) {
            for (int start = 0; start <= n - width; start++) {
                int end = start + width;
                if (width == 1) {
                    if (!chart[start][end]) {
                        // All width 1 spans must be set.
                        return false;
                    }
                } else {
                    if (chart[start][end]) {
                        // Count the number of pairs of child spans which could have been combined to form this span.
                        int childPairCount = 0;
                        for (int mid = start + 1; mid <= end - 1; mid++) {
                            if (chart[start][mid] && chart[mid][end]) {
                                childPairCount++;
                            }
                        }
                        if (childPairCount != 1) {
                            // This span should have been built from exactly one pair of child spans.
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

}
