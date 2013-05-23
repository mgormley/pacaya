package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.data.Sentence;
import gnu.trove.TIntDoubleHashMap;

/**
 * CKY Parsing algorithm for a CNF PCFG grammar.
 * 
 * @author mgormley
 * 
 */
public class CkyPcfgParser {

    public enum LoopOrder { LEFT_CHILD, RIGHT_CHILD, CARTESIAN_PRODUCT };
    
    private LoopOrder loopOrder = LoopOrder.CARTESIAN_PRODUCT;
    
    @Deprecated
    public static Chart parseSentence(Sentence sentence, CnfGrammar grammar) {
        // TODO: assert sentence.getAlphabet() == grammar.getLexAlphabet();
        int[] sent = sentence.getLabelIds();
        return parseSentence(sent, grammar);
    }
    
    public static Chart parseSentence(int[] sent, CnfGrammar grammar) {
        Chart chart = new Chart(sent, grammar);

        // Apply lexical rules to each word.
        for (int i = 0; i <= sent.length - 1; i++) {
            ChartCell cell = chart.getCell(i, i+1);
            for (Rule r : grammar.getLexicalRulesWithChild(sent[i])) {
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
                for (int mid = start + 1; mid <= end - 1; mid++) {
                    ChartCell leftCell = chart.getCell(start, mid);
                    ChartCell rightCell = chart.getCell(mid, end);
                    
                    // Loop through all possible pairs of left/right non-terminals.
                    for (final int leftChildNt : leftCell.getNts()) {
                        for (final int rightChildNt : rightCell.getNts()) {
                            // Lookup the rules with those left/right children.
                            for (final Rule r : grammar.getBinaryRulesWithChildren(leftChildNt, rightChildNt)) {
                                double score = r.getScore() 
                                        + leftCell.getMaxScore(r.getLeftChild()) 
                                        + rightCell.getMaxScore(r.getRightChild());
                                cell.updateCell(mid, r, score);
                            }
                        }
                    }
                }
                
                // Apply unary rules.
                MaxScoresSnapshot maxScores = cell.getMaxScoresSnapshot();
                int[] nts = cell.getNts();
                for(final int parentNt : nts) {
                    for (Rule r : grammar.getUnaryRulesWithChild(parentNt)) {
                        double score = r.getScore() + maxScores.getMaxScore(r.getLeftChild());
                        cell.updateCell(end, r, score);
                    }
                }
                
                cell.close();
            }
        }
    
        return chart;
    }

}
