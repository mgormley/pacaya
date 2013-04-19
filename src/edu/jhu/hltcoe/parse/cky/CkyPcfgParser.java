package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.data.Sentence;

/**
 * CKY Parsing algorithm for a CNF PCFG grammar.
 * 
 * @author mgormley
 * 
 */
public class CkyPcfgParser {

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
            for (Rule r : grammar.getLexicalRulesWithChild(sent[i])) {
                double score = r.getScore();
                chart.updateCell(i, i+1, r, score);
            }
        }
        
        // For each cell in the chart.
        for (int width = 1; width <= sent.length; width++) {
            for (int start = 0; start <= sent.length - width; start++) {
                int end = start + width;
                
                // Apply binary rules.
                for (int mid = start + 1; mid <= end - 1; mid++) {
                    // Loop through all possible pairs of left/right non-terminals.
                    for (final int leftChildNt : chart.getNonTerminals(start, mid)) {
                        for (final int rightChildNt : chart.getNonTerminals(mid, end)) {
                            // Lookup the rules with those left/right children.
                            for (final Rule r : grammar.getBinaryRulesWithChildren(leftChildNt, rightChildNt)) {
                                double score = r.getScore() 
                                        + chart.getMaxScore(start, mid, r.getLeftChild()) 
                                        + chart.getMaxScore(mid, end, r.getRightChild());
                                chart.updateCell(start, mid, end, r, score);
                            }
                        }
                    }
                }
                
                // Apply unary rules.
                for(final int parentNt : chart.getNonTerminals(start, end)) {
                    for (Rule r : grammar.getUnaryRulesWithChild(parentNt)) {
                        double score = r.getScore() + chart.getMaxScore(start, end, r.getLeftChild());
                        chart.updateCell(start, end, r, score);
                    }
                }
                
            }
        }
    
        return chart;
    }

}
