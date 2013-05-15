package edu.jhu.hltcoe.parse.cky;

import java.util.ArrayList;

import edu.jhu.hltcoe.parse.cky.Lambda.LambdaOne;
import edu.jhu.hltcoe.util.Alphabet;

/**
 * Binary tree for a context free grammar.
 * 
 * @author mgormley
 *
 */
public class BinaryTree {

    private int parent;
    private int start;
    private int end;
    private BinaryTree leftChildNode;
    private BinaryTree rightChildNode;
    private boolean isLexical;
    
    private Alphabet<String> alphabet;
    
    public BinaryTree(int parent, int start, int end, BinaryTree leftChildNode,
            BinaryTree rightChildNode, boolean isLexical, Alphabet<String> alphabet) {
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
        sb.append("(");
        getAsPennTreebankString(0, 0, sb);
        sb.append(")");
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

    public void preOrderTraversal(LambdaOne<BinaryTree> function) {
        // Visit this node.
        function.call(this);
        // Pre-order traversal of each child.
        leftChildNode.preOrderTraversal(function);
        rightChildNode.preOrderTraversal(function);
    }

    public void inOrderTraversal(LambdaOne<BinaryTree> function) {
        // In-order traversal of left child.
        leftChildNode.inOrderTraversal(function);
        // Visit this node.
        function.call(this);
        // In-order traversal of right child.
        rightChildNode.inOrderTraversal(function);
    }
    
    public void postOrderTraversal(LambdaOne<BinaryTree> function) {
        // Post-order traversal of each child.
        leftChildNode.postOrderTraversal(function);
        rightChildNode.postOrderTraversal(function);
        // Visit this node.
        function.call(this);
    }

    public int getStart() {
        return start;
    }
    
    public int getEnd() {
        return end;
    }

    public boolean isLeaf() {
        return leftChildNode == null && rightChildNode == null;
    }

    public Alphabet<String> getAlphabet() {
        return alphabet;
    }
    
    /**
     * Updates all the start end fields, treating the current node as the root.
     */
    public void updateStartEnd() {
        ArrayList<BinaryTree> leaves = getLeaves();
        for (int i=0; i<leaves.size(); i++) {
            BinaryTree leaf = leaves.get(i);
            leaf.start = i;
            leaf.end = i+1;
        }
        postOrderTraversal(new UpdateStartEnd());
    }

    /**
     * Gets the leaves of this tree.
     */
    public ArrayList<BinaryTree> getLeaves() {
        LeafCollector leafCollector = new LeafCollector();
        postOrderTraversal(leafCollector);
        return leafCollector.leaves;
    }
    
    /**
     * Gets the lexical item ids comprising the sentence.
     */
    public int[] getSentence() {
        ArrayList<BinaryTree> leaves = getLeaves();
        int[] sent = new int[leaves.size()];
        for (int i=0; i<sent.length; i++) {
            sent[i] = leaves.get(i).parent;
        }
        return sent;
    }

    @Override
    public String toString() {
        return "BinaryTreeNode [parent=" + getParentStr() + "_{" + start + ", "
                + end + "}, leftChildNode=" + leftChildNode
                + ", rightChildNode=" + rightChildNode + "]";
    }

    private class LeafCollector implements LambdaOne<BinaryTree> {

        public ArrayList<BinaryTree> leaves = new ArrayList<BinaryTree>();
        
        @Override
        public void call(BinaryTree node) {
            if (node.isLeaf()) {
                leaves.add(node);
            }
        }
        
    }
    
    private class UpdateStartEnd implements LambdaOne<BinaryTree> {

        @Override
        public void call(BinaryTree node) {
            if (!node.isLeaf()) {
                node.start = node.leftChildNode.start;
                if (node.rightChildNode == null) {
                    node.end = node.leftChildNode.end;
                } else {
                    node.end = node.rightChildNode.end;
                }
            }
        }
        
    }
}
