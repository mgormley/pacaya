package edu.jhu.parse.cky.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.Word;
import edu.jhu.parse.cky.GrammarConstants;
import edu.jhu.prim.util.Lambda.LambdaOne;
import edu.jhu.util.Alphabet;

/**
 * Binary tree for a context free grammar.
 * 
 * @author mgormley
 *
 */
public class BinaryTree {

    private String symbol;
    private int start;
    private int end;
    private BinaryTree leftChild;
    private BinaryTree rightChild;
    private boolean isLexical;
    
    public BinaryTree(String symbol, int start, int end, BinaryTree leftChildNode,
            BinaryTree rightChildNode, boolean isLexical) {
        this.symbol = symbol;
        this.start = start;
        this.end = end;
        this.leftChild = leftChildNode;
        this.rightChild = rightChildNode;
        this.isLexical = isLexical;
    }

//    public Span getSpan() {
//        return new Span(start, end);
//    }
       
    private static String canonicalizeTreeString(String newTreeStr) {
        return newTreeStr.trim().replaceAll("\\s+\\)", ")").replaceAll("\\s+", " ");
    }
    
    public String getAsOneLineString() {
        // TODO: speedup.
        return canonicalizeTreeString(getAsPennTreebankString());
    }
    
    /**
     * Gets a string representation of this parse that looks like the typical 
     * Penn Treebank style parse.
     * 
     * Example:
     *  ((ROOT (S (NP (NN time))
     *           (VP (VBZ flies)
     *               (PP (IN like)
     *                   (NP (DT an)
     *                       (NN arrow)))))))
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
        for (int i=0; i<numSpaces; i++) {
            sb.append(" ");
        }
        if (isLexical) {
            sb.append(getSymbol());
        } else {
            sb.append("(");
            sb.append(getSymbol());
            
            // If this is a constant instead, then we have each depth in one column.
            int numNewChars = 1 + getSymbol().length();

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
    
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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
     * Gets the leaves of this tree in left-to-right order.
     */
    public ArrayList<BinaryTree> getLeaves() {
        LeafCollector leafCollector = new LeafCollector();
        postOrderTraversal(leafCollector);
        return leafCollector.leaves;
    }
    
    /**
     * Gets the lexical leaves of this tree in left-to-right order.
     */
    public ArrayList<BinaryTree> getLexicalLeaves() {
        LexicalLeafCollector leafCollector = new LexicalLeafCollector();
        postOrderTraversal(leafCollector);
        return leafCollector.leaves;
    }
    
    /**
     * Gets the lexical item ids comprising the sentence.
     */
    public int[] getSentenceIds(Alphabet<String> lexAlphabet) {
        ArrayList<BinaryTree> leaves = getLexicalLeaves();
        int[] sent = new int[leaves.size()];
        for (int i=0; i<sent.length; i++) {
            sent[i] = lexAlphabet.lookupIndex(leaves.get(i).symbol);
        }
        return sent;
    }

    public Sentence getSentence(Alphabet<Label> lexAlphabet) {
        ArrayList<BinaryTree> leaves = getLexicalLeaves();
        ArrayList<Label> labels = new ArrayList<Label>(leaves.size());
        for (int i = 0; i < leaves.size(); i++) {
            labels.add(new Word(leaves.get(i).symbol));
        }
        return new Sentence(lexAlphabet, labels);
    }

    public List<String> getWords() {
        ArrayList<BinaryTree> leaves = getLexicalLeaves();
        ArrayList<String> words = new ArrayList<String>(leaves.size());
        for (int i = 0; i < leaves.size(); i++) {            
            words.add(leaves.get(i).symbol);
        }
        return words;
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
    
    private class LexicalLeafCollector implements LambdaOne<BinaryTree> {

        public ArrayList<BinaryTree> leaves = new ArrayList<BinaryTree>();
        
        @Override
        public void call(BinaryTree node) {
            if (node.isLeaf() && node.isLexical()) {
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
    
    /** Intern all the strings. */
    public void intern() {
        symbol = symbol.intern();
        if (leftChild != null) {
            leftChild.intern();
        }
        if (rightChild != null) {
            rightChild.intern();
        }
    }

    public NaryTree collapseToNary() {        
        ArrayList<NaryTree> children = null;
        if (!isLeaf()) {
            assert (leftChild != null);
            LinkedList<NaryTree> queue = new LinkedList<NaryTree>();
            addToQueue(queue, leftChild);
            addToQueue(queue, rightChild);
            children = new ArrayList<NaryTree>(queue);
        }
        
        return new NaryTree(symbol, start, end, children, isLexical);         
    }

    private static void addToQueue(LinkedList<NaryTree> queue, BinaryTree child) {
        if (child == null) {
            return;
        }
        String symbolStr = child.getSymbol();
        if (GrammarConstants.isBinarized(symbolStr)) {
            addToQueue(queue, child.leftChild);
            addToQueue(queue, child.rightChild);
        } else {
            queue.add(child.collapseToNary());
        }
    }

    @Override
    public String toString() {
        return "BinaryTreeNode [symbol=" + getSymbol() + "_{" + start + ", "
                + end + "}, leftChildNode=" + leftChild
                + ", rightChildNode=" + rightChild + "]";
    }
    
}
