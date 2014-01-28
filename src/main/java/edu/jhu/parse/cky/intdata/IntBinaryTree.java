package edu.jhu.parse.cky.intdata;

import java.util.ArrayList;
import java.util.LinkedList;

import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.Tag;
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
public class IntBinaryTree {

    private int symbol;
    private int start;
    private int end;
    private IntBinaryTree leftChild;
    private IntBinaryTree rightChild;
    private boolean isLexical;
    
    private Alphabet<Label> alphabet;
    
    public IntBinaryTree(int symbol, int start, int end, IntBinaryTree leftChildNode,
            IntBinaryTree rightChildNode, boolean isLexical, Alphabet<Label> alphabet) {
        this.symbol = symbol;
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
            sb.append(getSymbolStr());
        } else {
            sb.append("(");
            sb.append(getSymbolStr());
            
            // If this is a constant instead, then we have each depth in one column.
            int numNewChars = 1 + getSymbolStr().length();

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

    public void preOrderTraversal(LambdaOne<IntBinaryTree> function) {
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

    public void inOrderTraversal(LambdaOne<IntBinaryTree> function) {
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
    
    public void postOrderTraversal(LambdaOne<IntBinaryTree> function) {
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
    
    public IntBinaryTree getLeftChild() {
        return leftChild;
    }

    public IntBinaryTree getRightChild() {
        return rightChild;
    }

    public Alphabet<Label> getAlphabet() {
        return alphabet;
    }
    
    /** This method does an alphabet lookup and is slow. */
    private String getSymbolStr() {
        return alphabet.lookupObject(symbol).getLabel();
    }
    
    /** This method does an alphabet lookup and is slow. */
    public Label getSymbolLabel() {
        return alphabet.lookupObject(symbol);
    }

    private void setSymbolStr(String symbolStr) {
        Label l = isLexical ? new Word(symbolStr) : new Tag(symbolStr);
        this.symbol = alphabet.lookupIndex(l);
        if (this.symbol == -1) {
            throw new IllegalArgumentException("Invalid symbol string: " + symbolStr + " " + symbol);
        }
    }

    public void setSymbolLabel(Label label) {
        this.symbol = alphabet.lookupIndex(label);
        if (this.symbol < 0) {
            throw new IllegalArgumentException(
                    "Symbol is not in alphabet. label=" + label + " id=" + symbol);
        }
    }

    public void setSymbol(int symbol) {
        if (symbol >= alphabet.size() || symbol < 0) {
            throw new IllegalArgumentException("Invalid symbol: " + symbol);
        }
        this.symbol = symbol;
    }
    
    /**
     * Updates all the start end fields, treating the current node as the root.
     */
    public void updateStartEnd() {
        ArrayList<IntBinaryTree> leaves = getLeaves();
        for (int i=0; i<leaves.size(); i++) {
            IntBinaryTree leaf = leaves.get(i);
            leaf.start = i;
            leaf.end = i+1;
        }
        postOrderTraversal(new UpdateStartEnd());
    }

    /**
     * Gets the leaves of this tree.
     */
    public ArrayList<IntBinaryTree> getLeaves() {
        LeafCollector leafCollector = new LeafCollector();
        postOrderTraversal(leafCollector);
        return leafCollector.leaves;
    }
    
    /**
     * Gets the lexical item ids comprising the sentence.
     */
    public int[] getSentenceIds() {
        ArrayList<IntBinaryTree> leaves = getLeaves();
        int[] sent = new int[leaves.size()];
        for (int i=0; i<sent.length; i++) {
            sent[i] = leaves.get(i).symbol;
        }
        return sent;
    }

    public Sentence getSentence() {
        ArrayList<IntBinaryTree> leaves = getLeaves();
        ArrayList<Label> labels = new ArrayList<Label>(leaves.size());
        for (int i = 0; i < leaves.size(); i++) {
            labels.add(leaves.get(i).getSymbolLabel());
        }
        return new Sentence(leaves.get(0).alphabet, labels);
    }

    @Override
    public String toString() {
        return "BinaryTreeNode [symbol=" + getSymbolStr() + "_{" + start + ", "
                + end + "}, leftChildNode=" + leftChild
                + ", rightChildNode=" + rightChild + "]";
    }

    private class LeafCollector implements LambdaOne<IntBinaryTree> {

        public ArrayList<IntBinaryTree> leaves = new ArrayList<IntBinaryTree>();
        
        @Override
        public void call(IntBinaryTree node) {
            if (node.isLeaf()) {
                leaves.add(node);
            }
        }
        
    }
    
    private class UpdateStartEnd implements LambdaOne<IntBinaryTree> {

        @Override
        public void call(IntBinaryTree node) {
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

    public IntNaryTree collapseToNary(Alphabet<Label> ntAlphabet) {
        Alphabet<Label> alphabet = isLexical ? this.alphabet : ntAlphabet;
        // Reset the symbol id according to the new alphabet.
        int symbol = alphabet.lookupIndex(getSymbolLabel());
        
        ArrayList<IntNaryTree> children = null;
        if (!isLeaf()) {
            assert (leftChild != null);
            LinkedList<IntNaryTree> queue = new LinkedList<IntNaryTree>();
            addToQueue(queue, leftChild, ntAlphabet);
            addToQueue(queue, rightChild, ntAlphabet);
            children = new ArrayList<IntNaryTree>(queue);
        }
        
        return new IntNaryTree(symbol, start, end, children, isLexical, alphabet);         
    }

    private static void addToQueue(LinkedList<IntNaryTree> queue, IntBinaryTree child,
            Alphabet<Label> ntAlphabet) {
        if (child == null) {
            return;
        }
        String symbolStr = child.getSymbolStr();
        if (GrammarConstants.isBinarized(symbolStr)) {
            addToQueue(queue, child.leftChild, ntAlphabet);
            addToQueue(queue, child.rightChild, ntAlphabet);
        } else {
            queue.add(child.collapseToNary(ntAlphabet));
        }
    }

    public void resetAlphabets(final Alphabet<Label> lexAlphabet,
            final Alphabet<Label> ntAlphabet) {
        preOrderTraversal(new LambdaOne<IntBinaryTree>() {
            public void call(IntBinaryTree node) {
                Label label = node.getSymbolLabel();
                node.alphabet = node.isLexical ? lexAlphabet : ntAlphabet;
                node.setSymbolLabel(label);
            }
        });
    }
}
