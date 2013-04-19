package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.util.Alphabet;

/**
 * Binary tree from a context free grammar.
 * 
 * @author mgormley
 *
 */
public class CfgTree {
	
	private CfgTreeNode root;

	public CfgTree(CfgTreeNode root) {
		this.root = root;
	}

	@Override
	public String toString() {
		return "CfgTree [root=" + root + "]";
	}
	

	public String getAsPennTreebankString() {
		return root.getAsPennTreebankString();
	}

}
