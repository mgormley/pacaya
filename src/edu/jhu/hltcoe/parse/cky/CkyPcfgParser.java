package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.data.Sentence;

/**
 * CKY Parsing algorithm for a CNF PCFG grammar.
 * 
 * @author mgormley
 * 
 */
public class CkyPcfgParser {

	public static Chart parseSentence(Sentence sentence, CnfGrammar grammar) {
		int[] sent = sentence.getLabelIds();

		Chart chart = new Chart(sentence, grammar);
		for (int i = 1; i <= sent.length; i++) {
			for (Rule r : grammar.getUnaryRulesWithChild(sent[i])) {
				double score = r.getScore();
				chart.updateCell(i - 1, i, r, score);
			}
		}

		for (int width = 2; width <= sent.length; width++) {
			for (int start = 0; start <= sent.length - width; start++) {
				int end = start + width;
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
			}
		}
	
		return chart;
	}

}
