package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.parse.cky.Lambda.LambdaOne;
import edu.jhu.hltcoe.util.Alphabet;

/**
 * Node in a binary tree for a context free grammar.
 * 
 * @author mgormley
 *
 */
public class BinaryTreeNode {

    private static final int INDENT_CHARS = 4;
    private int parent;
    private int start;
    private int end;
    private BinaryTreeNode leftChildNode;
    private BinaryTreeNode rightChildNode;
    private boolean isLexical;
    
    private Alphabet<String> alphabet;
    
    public BinaryTreeNode(int parent, int start, int end, BinaryTreeNode leftChildNode,
            BinaryTreeNode rightChildNode, boolean isLexical, Alphabet<String> alphabet) {
        this.parent = parent;
        this.start = start;
        this.end = end;
        this.leftChildNode = leftChildNode;
        this.rightChildNode = rightChildNode;
        this.isLexical = isLexical;
        this.alphabet = alphabet;
    }

//    public Span getSpan() {
//        return new Span(start, end);
//    }
    
    private String getParentStr() {
        return alphabet.lookupObject(parent);
    }
    
    @Override
    public String toString() {
        return "CfgTreeNode [parent=" + getParentStr() + "_{" + start + ", "
                + end + "}, leftChildNode=" + leftChildNode
                + ", rightChildNode=" + rightChildNode + "]";
    }
    
    /**
     * Gets a string representation of this parse that looks like the typical 
     * Penn Treebank style parse.
     * 
     * Example:
     *  (ROOT (S (NP (NN time))
     *           (VP (VBZ flies)
     *               (PP (IN like)
     *                   (NP (DT an)
     *                       (NN arrow))))))
     *                       
     * @return A string representing this parse.
     */
    public String getAsPennTreebankString() {
        StringBuilder sb = new StringBuilder();
        getAsPennTreebankString(0, 0, sb);
        return sb.toString();
    }

    private void getAsPennTreebankString(int indent, int numOnLine, StringBuilder sb) {
        int numSpaces = indent - numOnLine;
        if (numSpaces <= 0 && indent != 0) {
            numSpaces = 1;
        }
        for (int i=0; i<numSpaces; i++) {
            sb.append(" ");
        }
        if (isLexical) {
            sb.append(getParentStr());
        } else {
            sb.append("(");
            sb.append(getParentStr());
            
            // If this is a constant instead, then we have each depth in one column.
            int numNewChars = 1 + getParentStr().length();

            if (leftChildNode != null) {
                //sb.append("\n");
                leftChildNode.getAsPennTreebankString(indent+numNewChars+1, indent + numNewChars, sb);
            }
            if (rightChildNode != null) {
                sb.append("\n");
                rightChildNode.getAsPennTreebankString(indent+numNewChars+1, 0, sb);
            }
            sb.append(")");
        }
    }

    public void preOrderTraversal(LambdaOne<BinaryTreeNode> function) {
        // Visit this node.
        function.apply(this);
        // Pre-order traversal of each child.
        leftChildNode.preOrderTraversal(function);
        rightChildNode.preOrderTraversal(function);
    }

    public void inOrderTraversal(LambdaOne<BinaryTreeNode> function) {
        // In-order traversal of left child.
        leftChildNode.inOrderTraversal(function);
        // Visit this node.
        function.apply(this);
        // In-order traversal of right child.
        rightChildNode.inOrderTraversal(function);
    }
    
    public void postOrderTraversal(LambdaOne<BinaryTreeNode> function) {
        // Post-order traversal of each child.
        leftChildNode.postOrderTraversal(function);
        rightChildNode.postOrderTraversal(function);
        // Visit this node.
        function.apply(this);
    }
}
