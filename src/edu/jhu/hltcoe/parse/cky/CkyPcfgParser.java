package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.data.Sentence;

/**
 * CKY Parsing algorithm for a CNF PCFG grammar.
 * 
 * @author mgormley
 *
 */
public class CkyPcfgParser {

	public static void parseSentence(Sentence sent, CnfGrammar grammar) {
		Chart chart = new Chart(sent, grammar);
		for (int i=0; i<sent.size(); i++) {
			for (Rule r : grammar.getUnaryRulesWithChild(sent.get(i))) {
				double score = r.getScore();
				if (chart.updateCell(i-1, i, r.getParent(), score)) {
					
				}
			}
		}
		
	}
	
}
