package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.util.Alphabet;

/**
 * Binary tree from a context free grammar.
 * 
 * @author mgormley
 *
 */
public class BinaryTree {
    
    private BinaryTreeNode root;

    public BinaryTree(BinaryTreeNode root) {
        this.root = root;
    }

    public String getAsPennTreebankString() {
        return root.getAsPennTreebankString();
    }

    @Override
    public String toString() {
        return "BinaryTree [root=" + root + "]";
    }
    
}
