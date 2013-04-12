package edu.jhu.hltcoe.parse.cky;

import java.util.ArrayList;

import util.Alphabet;

import com.sun.tools.javac.util.List;

/**
 * Grammar in Chomsky normal form.
 * 
 * @author mgormley
 *
 */
public class CnfGrammar {

	private ArrayList<Rule> allRules;

	private ArrayList<Rule> unaryRules;
	private ArrayList<Rule> binaryRules;
	
	private ArrayList<Rule>[] unaryRulesForChild;
	private ArrayList<Rule>[][] binaryRulesForChildren;
	
	public CnfGrammar(ArrayList<Rule> allRules, Alphabet<String> lexAlphabet, Alphabet<String> ntAlphabet) {
		this.allRules = allRules;
		unaryRulesForChild = new ArrayList[lexAlphabet.size()];
		binaryRulesForChildren = new ArrayList[ntAlphabet.size()][ntAlphabet.size()];
		for (Rule r : allRules) {
			if (r.isUnary()) {
				if (unaryRulesForChild[r.getLeftChild()] == null) {
					unaryRulesForChild[r.getLeftChild()] = new ArrayList<Rule>();
				}
				unaryRulesForChild[r.getLeftChild()].add(r);
			} else {
				if (binaryRulesForChildren[r.getLeftChild()][r.getRightChild()] == null) {
					binaryRulesForChildren[r.getLeftChild()][r.getRightChild()] = new ArrayList<Rule>();
				}
				binaryRulesForChildren[r.getLeftChild()][r.getRightChild()].add(r);
			}
		}
	}
	
	public ArrayList<Rule> getUnaryRulesWithChild(int child) {
		return unaryRulesForChild[child];
	}

	public ArrayList<Rule> getBinaryRulesWithChildren(int leftChildNt, int rightChildNt) {
		return binaryRulesForChildren[leftChildNt][rightChildNt];
	}
	
}
