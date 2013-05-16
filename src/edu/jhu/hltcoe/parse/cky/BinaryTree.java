package edu.jhu.hltcoe.parse.cky;

import java.util.ArrayList;
import java.util.LinkedList;

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
    private BinaryTree leftChild;
    private BinaryTree rightChild;
    private boolean isLexical;
    
    private Alphabet<String> alphabet;
    
    public BinaryTree(int parent, int start, int end, BinaryTree leftChildNode,
            BinaryTree rightChildNode, boolean isLexical, Alphabet<String> alphabet) {
        this.parent = parent;
        this.start = start;
        this.end = end;
        this.leftChild = leftChildNode;
        this.rightChild = rightChildNode;
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
        getAsPennTreebankString(1, 1, sb);
        sb.append(")");
        return sb.toString();
    }

    private void getAsPennTreebankString(int indent, int numOnLine, StringBuilder sb) {
        int numSpaces = indent - numOnLine;
        if (numSpaces <= 0 && indent != 0) {
            //numSpaces = 1;
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

            if (leftChild != null) {
                //sb.append("\n");
                leftChild.getAsPennTreebankString(indent+numNewChars+1, indent + numNewChars, sb);
            }
            if (rightChild != null) {
                sb.append("\n");
                rightChild.getAsPennTreebankString(indent+numNewChars+1, 0, sb);
            }
            sb.append(")");
        }
    }

    public void preOrderTraversal(LambdaOne<BinaryTree> function) {
        // Visit this node.
        function.call(this);
        // Pre-order traversal of each child.
        if (leftChild != null) {
            leftChild.preOrderTraversal(function);
        }
        if (rightChild != null) {
            rightChild.preOrderTraversal(function);
        }
    }

    public void inOrderTraversal(LambdaOne<BinaryTree> function) {
        // In-order traversal of left child.
        if (leftChild != null) {
            leftChild.inOrderTraversal(function);
        }
        // Visit this node.
        function.call(this);
        // In-order traversal of right child.
        if (rightChild != null) {
            rightChild.inOrderTraversal(function);
        }
    }
    
    public void postOrderTraversal(LambdaOne<BinaryTree> function) {
        // Post-order traversal of each child.
        if (leftChild != null) {
            leftChild.postOrderTraversal(function);
        }
        if (rightChild != null) {
            rightChild.postOrderTraversal(function);
        }
        // Visit this node.
        function.call(this);
    }

    public int getParent() {
        return parent;
    }
    
    public int getStart() {
        return start;
    }
    
    public int getEnd() {
        return end;
    }

    public boolean isLeaf() {
        return leftChild == null && rightChild == null;
    }

    public boolean isLexical() {
        return isLexical;
    }
    
    public BinaryTree getLeftChild() {
        return leftChild;
    }

    public BinaryTree getRightChild() {
        return rightChild;
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
                + end + "}, leftChildNode=" + leftChild
                + ", rightChildNode=" + rightChild + "]";
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
                node.start = node.leftChild.start;
                if (node.rightChild == null) {
                    node.end = node.leftChild.end;
                } else {
                    node.end = node.rightChild.end;
                }
            }
        }
        
    }

    public NaryTree collapseToNary(Alphabet<String> ntAlphabet) {
        Alphabet<String> alphabet = isLexical ? this.alphabet : ntAlphabet;

        ArrayList<NaryTree> children = null;
        if (!isLeaf()) {
            assert (leftChild != null);
            LinkedList<NaryTree> queue = new LinkedList<NaryTree>();
            addToQueue(queue, leftChild, ntAlphabet);
            addToQueue(queue, rightChild, ntAlphabet);
            children = new ArrayList<NaryTree>(queue);
        }        
        return new NaryTree(parent, start, end, children, isLexical, alphabet);         
    }

    private void addToQueue(LinkedList<NaryTree> queue, BinaryTree child,
            Alphabet<String> ntAlphabet) {
        if (child == null) {
            return;
        }
        String parentStr = alphabet.lookupObject(child.getParent());
        if (GrammarConstants.isBinarized(parentStr)) {
            addToQueue(queue, child.leftChild, ntAlphabet);
            addToQueue(queue, child.rightChild, ntAlphabet);
        } else {
            queue.add(child.collapseToNary(ntAlphabet));
        }
    }

    public void setParent(String parentStr) {
        this.parent = alphabet.lookupIndex(parentStr);
        if (this.parent == -1) {
            throw new IllegalArgumentException("Invalid parent string: " + parentStr + " " + parent);
        }
    }
    
    public void setParent(int parent) {
        if (parent >= alphabet.size() || parent < 0) {
            throw new IllegalArgumentException("Invalid parent: " + parent);
        }
        this.parent = parent;
    }
}
