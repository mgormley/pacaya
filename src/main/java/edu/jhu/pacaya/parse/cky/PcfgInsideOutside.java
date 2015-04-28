package edu.jhu.pacaya.parse.cky;

import edu.jhu.pacaya.nlp.data.Sentence;
import edu.jhu.pacaya.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.pacaya.parse.cky.chart.Chart;
import edu.jhu.pacaya.parse.cky.chart.ChartCell;
import edu.jhu.pacaya.parse.cky.chart.ScoresSnapshot;
import edu.jhu.pacaya.parse.cky.chart.Chart.ChartCellType;
import edu.jhu.pacaya.parse.cky.chart.Chart.ParseType;
import edu.jhu.pacaya.parse.cky.chart.ConstrainedChartCell.ChartCellConstraint;

public class PcfgInsideOutside {
    
    public static class PcfgInsideOutsidePrm {
        // Whether or not to cache the chart cells between calls to the parser.
        public boolean cacheChart = true;
        public LoopOrder loopOrder = LoopOrder.LEFT_CHILD;
        public Scorer scorer = new RuleScorer();
    }
    
    /**
     * An inside/outside chart for a PCFG parse.
     * 
     * @author mgormley
     * 
     */
    public static class PcfgIoChart {
        /** Length of the sentence. */
        private final int n;
        private CnfGrammar grammar;
        private Chart insideChart;
        private Chart outsideChart;

        public PcfgIoChart(Sentence sentence, CnfGrammar grammar, Chart insideChart, Chart outsideChart) {
            this.n = sentence.size();
            this.grammar = grammar;
            this.insideChart = insideChart;
            this.outsideChart = outsideChart;
        }

        public double getLogInsideScore(int symbol, int start, int end) {
            checkCell(start, end);
            return insideChart.getCell(start, end).getScore(symbol);
        }

        public double getLogOutsideScore(int symbol, int start, int end) {
            checkCell(start, end);
            return outsideChart.getCell(start, end).getScore(symbol);
        }

        public double getLogPartitionFunction() {
            return getLogInsideScore(grammar.getRootSymbol(), 0, n);
        }

        public double getLogSumOfPotentials(int nt, int start, int end) {
            return getLogInsideScore(nt, start, end) + getLogOutsideScore(nt, start, end);
        }

        public double getLogExpectedCount(int nt, int start, int end) {
            return getLogSumOfPotentials(nt, start, end) - getLogPartitionFunction();
        }

        /**
         * Checks that start \in [0, n-1] and end \in [1, n], where n is the
         * length of the sentence.
         */
        private void checkCell(int start, int end) {
            if (start > n - 1 || end > n || start < 0 || end < 1) {
                throw new IllegalStateException(String.format("Invalid cell: start=%d end=%d", start, end));
            }
        }
    }
    
    /** The inside chart. */
    private Chart inChart;
    /** The outside chart. */
    private Chart outChart;

    // These are private final just to ensure that they are never slow accesses.
    private final LoopOrder loopOrder;
    private final ChartCellType cellType;
    private final ParseType parseType;
    private final boolean cacheChart;
    private final ChartCellConstraint constraint;
    private final Scorer scorer;
    
    public PcfgInsideOutside(PcfgInsideOutsidePrm prm) {
        // Fixed Parameters.
        this.cellType = ChartCellType.FULL;
        this.parseType = ParseType.INSIDE;
        this.constraint = null; // prm.constraint;
        // Others.
        this.loopOrder = prm.loopOrder;
        this.cacheChart = prm.cacheChart;
        this.scorer = prm.scorer;
    }
    
    public final PcfgIoChart runInsideOutside(final Sentence sentence, final CnfGrammar grammar) {
        if (sentence.getAlphabet() != grammar.getLexAlphabet()) {
            throw new IllegalArgumentException("Alphabets for sentence and grammar must be the same.");
        }

        if (!cacheChart || inChart == null || inChart.getGrammar() != grammar) {
            // Construct a new chart.
            inChart = new Chart(sentence, grammar, cellType, parseType, constraint);
            outChart = new Chart(sentence, grammar, cellType, parseType, constraint);
        } else {
            // If it already exists, just reset it for efficiency.
            inChart.reset(sentence);
            outChart.reset(sentence);
        }
        int[] sent = sentence.getLabelIds();
        CkyPcfgParser.parseSentence(sent, grammar, loopOrder, inChart, scorer);
        runOutsideAlgorithm(sent, grammar, loopOrder, inChart, outChart, scorer);
        return new PcfgIoChart(sentence, grammar, inChart, outChart);    
     }
    
    /**
     * Runs the outside algorithm, given an inside chart.
     * 
     * @param sent The input sentence.
     * @param grammar The input grammar.
     * @param loopOrder The loop order to use when parsing.
     * @param inChart The inside chart, already populated.
     * @param outChart The outside chart (the output of this function).
     */
    public static void runOutsideAlgorithm(final int[] sent, final CnfGrammar grammar, final LoopOrder loopOrder, final Chart inChart, final Chart outChart, final Scorer scorer) {
        // Base case.
        // -- part I: outsideScore(S, 0, n) = 0 = log(1).
        ChartCell outRootCell = outChart.getCell(0, sent.length);
        outRootCell.updateCell(grammar.getRootSymbol(), 0, -1, null);
        // -- part II: outsideScore(A, 0, n) = -inf = log(0) for all A \neq S.
        // This second part of the base case is the default initialization for each cell. 

        // We still start at width = n so that we can apply unary rules to our
        // base case root terminal S.
        // 
        // For each cell in the chart. (width decreasing)
        for (int width = sent.length; width >= 1; width--) {
            for (int start = 0; start <= sent.length - width; start++) {
                int end = start + width;
                ChartCell outCell = outChart.getCell(start, end);
                
                // Apply unary rules.
                ScoresSnapshot scoresSnapshot = outCell.getScoresSnapshot();
                int[] nts = outCell.getNts();
                for(final int parentNt : nts) {
                    for (final Rule r : grammar.getUnaryRulesWithParent(parentNt)) {
                        // TODO: Check whether this outside rule even matters.
                        // Arguably this would create an outside chart that is
                        // incomplete.
                        //ChartCell inCell = inChart.getCell(start, end); // TODO: move this out of the loop.
                        //if (inCell.getScore(r.getLeftChild()) > Double.NEGATIVE_INFINITY) {                            
                            double score = scorer.score(r, start, end, end)
                                    + scoresSnapshot.getScore(parentNt);
                            outCell.updateCell(r.getLeftChild(), score, end, r);
                        //}
                    }
                }
                
                // Apply binary rules.
                if (loopOrder == LoopOrder.CARTESIAN_PRODUCT) {
                    processCellCartesianProduct(grammar, inChart, outChart, start, end, outCell, scorer);
                } else if (loopOrder == LoopOrder.LEFT_CHILD) {
                    processCellLeftChild(grammar, inChart, outChart, start, end, outCell, scorer);
                } else if (loopOrder == LoopOrder.RIGHT_CHILD) {
                    processCellRightChild(grammar, inChart, outChart, start, end, outCell, scorer);
                } else {
                    throw new RuntimeException("Not implemented: " + loopOrder);
                }
            }
        }
        
        // Apply lexical rules to each word.
        for (int i = 0; i <= sent.length - 1; i++) {
            ChartCell outCell = outChart.getCell(i, i+1);
            ScoresSnapshot scoresSnapshot = outCell.getScoresSnapshot();
            for (final Rule r : grammar.getLexicalRulesWithChild(sent[i])) {
                double score = scorer.score(r, i, i+1, i+1)
                        + scoresSnapshot.getScore(r.getParent());
                outCell.updateCell(r.getLeftChild(), score, i+1, r);
            }
        }
    }
    
    /**
     * Process a cell (binary rules only) using the Cartesian product of the children's rules.
     * 
     * This follows the description in (Dunlop et al., 2010).
     */
    private static void processCellCartesianProduct(final CnfGrammar grammar, final Chart inChart,
            final Chart outChart, final int start, final int end, final ChartCell outCell, final Scorer scorer) {
        // Apply binary rules.
        for (int mid = start + 1; mid <= end - 1; mid++) {
            ChartCell leftInCell = inChart.getCell(start, mid);
            ChartCell rightInCell = inChart.getCell(mid, end);
            ChartCell leftOutCell = outChart.getCell(start, mid);
            ChartCell rightOutCell = outChart.getCell(mid, end);
            
            // Loop through all possible pairs of left/right non-terminals.
            for (final int leftChildNt : leftInCell.getNts()) {
                double leftScoreForNt = leftInCell.getScore(leftChildNt);
                for (final int rightChildNt : rightInCell.getNts()) {
                    double rightScoreForNt = rightInCell.getScore(rightChildNt);
                    // Lookup the rules with those left/right children.
                    for (final Rule r : grammar.getBinaryRulesWithChildren(leftChildNt, rightChildNt)) {
                        double cellScoreForNt = outCell.getScore(r.getParent());
                        double score = scorer.score(r, start, mid, end);
                        // Update left cell.
                        double addendLeft = score
                                + cellScoreForNt + rightScoreForNt;
                        leftOutCell.updateCell(leftChildNt, addendLeft, mid, r);
                        // Update right cell.
                        double addendRight = score
                                + leftScoreForNt + cellScoreForNt;
                        rightOutCell.updateCell(rightChildNt, addendRight, mid, r);
                    }
                }
            }
        }
    }
    
    /**
     * Process a cell (binary rules only) using the left-child to constrain the set of rules we consider.
     * 
     * This follows the description in (Dunlop et al., 2010).
     */
    private static void processCellLeftChild(final CnfGrammar grammar, final Chart inChart,
            final Chart outChart, final int start, final int end, final ChartCell outCell, final Scorer scorer) {
        // Apply binary rules.
        for (int mid = start + 1; mid <= end - 1; mid++) {
            ChartCell leftInCell = inChart.getCell(start, mid);
            ChartCell rightInCell = inChart.getCell(mid, end);
            ChartCell leftOutCell = outChart.getCell(start, mid);
            ChartCell rightOutCell = outChart.getCell(mid, end);
            
            // Loop through each left child non-terminal.
            for (final int leftChildNt : leftInCell.getNts()) {
                double leftScoreForNt = leftInCell.getScore(leftChildNt);
                // Lookup all rules with that left child.
                for (final Rule r : grammar.getBinaryRulesWithLeftChild(leftChildNt)) {
                    // Check whether the right child of that rule is in the right child cell.
                    int rightChildNt = r.getRightChild();
                    double rightScoreForNt = rightInCell.getScore(rightChildNt);
                    if (rightScoreForNt > Double.NEGATIVE_INFINITY) {
                        double cellScoreForNt = outCell.getScore(r.getParent());
                        double score = scorer.score(r, start, mid, end);
                        // Update left cell.
                        double addendLeft = score
                                + cellScoreForNt + rightScoreForNt;
                        leftOutCell.updateCell(leftChildNt, addendLeft, mid, r);
                        // Update right cell.
                        double addendRight = score
                                + leftScoreForNt + cellScoreForNt;
                        rightOutCell.updateCell(rightChildNt, addendRight, mid, r);
                    }
                }
            }
        }
    }

    /**
     * Process a cell (binary rules only) using the left-child to constrain the set of rules we consider.
     * 
     * This follows the description in (Dunlop et al., 2010).
     */
    private static void processCellRightChild(final CnfGrammar grammar, final Chart inChart,
            final Chart outChart, final int start, final int end, final ChartCell outCell, final Scorer scorer) {
        // Apply binary rules.
        for (int mid = start + 1; mid <= end - 1; mid++) {
            ChartCell leftInCell = inChart.getCell(start, mid);
            ChartCell rightInCell = inChart.getCell(mid, end);
            ChartCell leftOutCell = outChart.getCell(start, mid);
            ChartCell rightOutCell = outChart.getCell(mid, end);

            // Loop through each right child non-terminal.
            for (final int rightChildNt : rightInCell.getNts()) {
                double rightScoreForNt = rightInCell.getScore(rightChildNt);
                // Lookup all rules with that right child.
                for (final Rule r : grammar.getBinaryRulesWithRightChild(rightChildNt)) {
                    // Check whether the left child of that rule is in the left child cell.
                    int leftChildNt = r.getLeftChild();
                    double leftScoreForNt = leftInCell.getScore(leftChildNt);
                    if (leftScoreForNt > Double.NEGATIVE_INFINITY) {
                        double cellScoreForNt = outCell.getScore(r.getParent());
                        double score = scorer.score(r, start, mid, end);
                        // Update left cell.
                        double addendLeft = score
                                + cellScoreForNt + rightScoreForNt;
                        leftOutCell.updateCell(leftChildNt, addendLeft, mid, r);
                        // Update right cell.
                        double addendRight = score
                                + leftScoreForNt + cellScoreForNt;
                        rightOutCell.updateCell(rightChildNt, addendRight, mid, r);
                    }
                }
            }
        }            
    }

    
}
