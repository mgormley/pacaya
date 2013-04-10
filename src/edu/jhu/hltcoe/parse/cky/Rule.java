package edu.jhu.hltcoe.parse.cky;

import util.Alphabet;

/**
 * A unary or binary rule (or production) in a CNF grammar.
 * 
 * @author mgormley
 *
 */
public class Rule {

	/** If this is not a unary rule, then it is a binary rule. */
	private boolean isUnary;
	
	private int parent;
	private int leftChild;
	private int rightChild;
	// Subclass: LogProb rule should have this: private double logProb;
	// Separate subclass should compute based on features of the sentence.
	
	private Alphabet<String> symbolMap;

	public double getScore() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getParent() {
		return parent;
	}

	public boolean isUnary() {
		return isUnary;
	}

	public int getLeftChild() {
		return leftChild;
	}

	public int getRightChild() {
		return rightChild;
	}
		
}
