package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.parse.cky.chart.Chart;
import edu.jhu.hltcoe.parse.cky.chart.ChartCell;
import edu.jhu.hltcoe.parse.cky.chart.Chart.ChartCellType;

/**
 * CKY Parsing algorithm for a CNF PCFG grammar.
 * 
 * Currently running at 0.075 seconds per sentence for the first 200 sentences of
 * WSJ section 24 with LoopOrder.LEFT_CHILD and CellType.FULL. This is about
 * twice as fast as the exhaustive bubs-parser "ecpccl", though slightly slower
 * than the reported times in (Dunlop et al. 2010).
 * 
 * With caching we run slightly faster at: 0.067 seconds per sentence.
 * 
 * @author mgormley
 * 
 */
public class CkyPcfgParser {

    public static class CkyPcfgParserPrm {
        // Whether or not to cache the chart cells between calls to the parser.
        public boolean cacheChart = true;  
        public LoopOrder loopOrder = LoopOrder.LEFT_CHILD;
        public ChartCellType cellType = ChartCellType.FULL;
    }
    
    public enum LoopOrder { LEFT_CHILD, RIGHT_CHILD, CARTESIAN_PRODUCT }

    private Chart chart;

    // These are private final just to ensure that they are never slow accesses.
    private final LoopOrder loopOrder;
    private final ChartCellType cellType;
    private final boolean cacheChart;
    
    public CkyPcfgParser(CkyPcfgParserPrm prm) {
        this.loopOrder = prm.loopOrder;
        this.cellType = prm.cellType;
        this.cacheChart = prm.cacheChart;
    }
    
    @Deprecated
    public final Chart parseSentence(final Sentence sentence, final CnfGrammar grammar) {
        // TODO: assert sentence.getAlphabet() == grammar.getLexAlphabet();
        int[] sent = sentence.getLabelIds();
        return parseSentence(sent, grammar);
    }
    
    public final Chart parseSentence(final int[] sent, final CnfGrammar grammar) {
        if (!cacheChart || chart == null || chart.getGrammar() != grammar) {
            // Construct a new chart.
            chart = new Chart(sent, grammar, cellType);
        } else {
            // If it already exists, just reset it for efficiency.
            chart.reset(sent);
        }
        return parseSentence(sent, grammar, chart, loopOrder);
    }
    
    public static final Chart parseSentence(final int[] sent, final CnfGrammar grammar, final Chart chart, final LoopOrder loopOrder) {
        // Apply lexical rules to each word.
        for (int i = 0; i <= sent.length - 1; i++) {
            ChartCell cell = chart.getCell(i, i+1);
            for (final Rule r : grammar.getLexicalRulesWithChild(sent[i])) {
                double score = r.getScore();
                cell.updateCell(i+1, r, score);
            }
        }
        
        // For each cell in the chart.
        for (int width = 1; width <= sent.length; width++) {
            for (int start = 0; start <= sent.length - width; start++) {
                int end = start + width;
                ChartCell cell = chart.getCell(start, end);
                
                // Apply binary rules.
                if (loopOrder == LoopOrder.CARTESIAN_PRODUCT) {
                    processCellCartesianProduct(grammar, chart, start, end, cell);
                } else if (loopOrder == LoopOrder.LEFT_CHILD) {
                    processCellLeftChild(grammar, chart, start, end, cell);
                } else if (loopOrder == LoopOrder.RIGHT_CHILD) {
                    processCellRightChild(grammar, chart, start, end, cell);
                } else {
                    throw new RuntimeException("Not implemented: " + loopOrder);
                }

                
                // Apply unary rules.
                MaxScoresSnapshot maxScores = cell.getMaxScoresSnapshot();
                int[] nts = cell.getNts();
                for(final int parentNt : nts) {
                    for (final Rule r : grammar.getUnaryRulesWithChild(parentNt)) {
                        double score = r.getScore() + maxScores.getMaxScore(r.getLeftChild());
                        cell.updateCell(end, r, score);
                    }
                }
                
                cell.close();
            }
        }
    
        return chart;
    }

    /**
     * Process a cell (binary rules only) using the Cartesian product of the children's rules.
     * 
     * This follows the description in (Dunlop et al., 2010).
     */
    private static final void processCellCartesianProduct(CnfGrammar grammar, Chart chart, int start,
            int end, ChartCell cell) {
        // Apply binary rules.
        for (int mid = start + 1; mid <= end - 1; mid++) {
            ChartCell leftCell = chart.getCell(start, mid);
            ChartCell rightCell = chart.getCell(mid, end);
            
            // Loop through all possible pairs of left/right non-terminals.
            for (final int leftChildNt : leftCell.getNts()) {
                double leftScoreForNt = leftCell.getMaxScore(leftChildNt);
                for (final int rightChildNt : rightCell.getNts()) {
                    double rightScoreForNt = rightCell.getMaxScore(rightChildNt);
                    // Lookup the rules with those left/right children.
                    for (final Rule r : grammar.getBinaryRulesWithChildren(leftChildNt, rightChildNt)) {
                        double score = r.getScore()
                                + leftScoreForNt
                                + rightScoreForNt;
                        cell.updateCell(mid, r, score);
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
    private static final void processCellLeftChild(CnfGrammar grammar, Chart chart, int start,
            int end, ChartCell cell) {
        // Apply binary rules.
        for (int mid = start + 1; mid <= end - 1; mid++) {
            ChartCell leftCell = chart.getCell(start, mid);
            ChartCell rightCell = chart.getCell(mid, end);
            
            // Loop through each left child non-terminal.
            for (final int leftChildNt : leftCell.getNts()) {
                double leftScoreForNt = leftCell.getMaxScore(leftChildNt);
                // Lookup all rules with that left child.
                for (final Rule r : grammar.getBinaryRulesWithLeftChild(leftChildNt)) {
                    // Check whether the right child of that rule is in the right child cell.
                    double rightScoreForNt = rightCell.getMaxScore(r.getRightChild());
                    if (rightScoreForNt > Double.NEGATIVE_INFINITY) {
                        double score = r.getScore() 
                                + leftScoreForNt
                                + rightScoreForNt;
                        cell.updateCell(mid, r, score);
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
    private static final void processCellRightChild(CnfGrammar grammar, Chart chart, int start,
            int end, ChartCell cell) {
        // Apply binary rules.
        for (int mid = start + 1; mid <= end - 1; mid++) {
            ChartCell leftCell = chart.getCell(start, mid);
            ChartCell rightCell = chart.getCell(mid, end);
            
            // Loop through each right child non-terminal.
            for (final int rightChildNt : rightCell.getNts()) {
                double rightScoreForNt = rightCell.getMaxScore(rightChildNt);
                // Lookup all rules with that right child.
                for (final Rule r : grammar.getBinaryRulesWithRightChild(rightChildNt)) {
                    // Check whether the left child of that rule is in the left child cell.
                    double leftScoreForNt = leftCell.getMaxScore(r.getLeftChild());
                    if (leftScoreForNt > Double.NEGATIVE_INFINITY) {
                        double score = r.getScore() 
                                + leftScoreForNt
                                + rightScoreForNt;
                        cell.updateCell(mid, r, score);
                    }
                }
            }
        }
    }
    
}
