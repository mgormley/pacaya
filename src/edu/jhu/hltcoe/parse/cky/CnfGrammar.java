package edu.jhu.hltcoe.parse.cky;

import java.util.ArrayList;

import edu.jhu.hltcoe.data.Label;

/**
 * Grammar in Chomsky normal form.
 * 
 * @author mgormley
 *
 */
public class CnfGrammar {

	private ArrayList<Rule> allRules;
	
	public CnfGrammar() {
		allRules = new ArrayList<Rule>();
	}
	
	public void addRule(Rule rule) {
		allRules.add(rule);
	}

	public ArrayList<Rule> getUnaryRulesWithChild(Label label) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
