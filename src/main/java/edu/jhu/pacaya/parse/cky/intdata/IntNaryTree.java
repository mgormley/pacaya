package edu.jhu.pacaya.parse.cky.intdata;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;

import edu.jhu.pacaya.nlp.data.Sentence;
import edu.jhu.pacaya.parse.cky.GrammarConstants;
import edu.jhu.pacaya.util.files.Files;
import edu.jhu.prim.bimap.IntObjectBimap;
import edu.jhu.prim.util.Lambda.FnO1ToVoid;

/**
 * N-ary tree for a context free grammar.
 * 
 * @author mgormley
 *
 */
public class IntNaryTree {

    private static final int NOT_INITIALIZED = -1;
    private int symbol;
    private int start;
    private int end;
    /** Children of this node, ordered left-to-right */
    private ArrayList<IntNaryTree> children;
    private boolean isLexical;
    
    private IntObjectBimap<String> alphabet;
    
    public IntNaryTree(int symbol, int start, int end, ArrayList<IntNaryTree> children,
            boolean isLexical, IntObjectBimap<String> alphabet) {
        this.symbol = symbol;
        this.start = start;
        this.end = end;
        this.children = children;
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
//        StringBuilder sb = new StringBuilder();
//        getAsPennTreebankString(1, 1, sb);
//        return canonicalizeTreeString(sb.toString());
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

            for (int i=0; i<children.size(); i++) {
                IntNaryTree child = children.get(i);
                if (i==0) {
                    // First child.
                    child.getAsPennTreebankString(indent+numNewChars+1, indent + numNewChars, sb);
                } else {
                    // Other children.
                    sb.append("\n");
                    child.getAsPennTreebankString(indent+numNewChars+1, 0, sb);
                }
            }
            sb.append(")");
        }
    }
        
    public enum ReaderState {
        START, LEXICAL, NONTERMINAL, CHILDREN, DONE,
    }

    public static ArrayList<IntNaryTree> readTreesInPtbFormat(
            IntObjectBimap<String> lexAlphabet, IntObjectBimap<String> ntAlphabet, Reader reader) throws IOException {
        ArrayList<IntNaryTree> trees = new ArrayList<IntNaryTree>();
        while (true) {
            IntNaryTree tree = readSubtreeInPtbFormat(lexAlphabet, ntAlphabet, reader);
            if (tree != null) {
                trees.add(tree);
            }            
            if (tree == null || tree.isLexical) {
                break;
            }            
        }
        return trees;
    }


    /**
     * Reads a full tree in Penn Treebank format. Such a tree should include an
     * outer set of parentheses. The returned tree will have initialized the
     * start/end fields.
     */
    public static IntNaryTree readTreeInPtbFormat(IntObjectBimap<String> lexAlphabet, IntObjectBimap<String> ntAlphabet, Reader reader) throws IOException {
        Files.readUntilCharacter(reader, '(');
        IntNaryTree root = IntNaryTree.readSubtreeInPtbFormat(lexAlphabet, ntAlphabet, reader);
        Files.readUntilCharacter(reader, ')');
        if (root == null) {
            return null;
        }
        root.updateStartEnd();
        return root;
    }
    
    /**
     * Reads an NaryTreeNode from a string.
     * 
     * Example:
     * (NP (DT the) (NN board) )
     * 
     * Note that the resulting tree will NOT have the start/end fields initialized.
     * @param lexAlphabet TODO
     * @param ntAlphabet 
     * @param reader
     * 
     * @return
     * @throws IOException
     */
    private static IntNaryTree readSubtreeInPtbFormat(IntObjectBimap<String> lexAlphabet, IntObjectBimap<String> ntAlphabet, Reader reader) throws IOException {
        ReaderState state = ReaderState.START;
        StringBuilder symbolSb = new StringBuilder();
        ArrayList<IntNaryTree> children = null;
        boolean isLexical = false;

        char[] cbuf = new char[1];
        while (reader.read(cbuf) != -1) {
        //for (int i=0; i<treeStr.length(); i++) {
            //char c = treeStr.charAt(i);
            char c = cbuf[0];
            if (state == ReaderState.START) {
                if (c == '(') {
                    state = ReaderState.NONTERMINAL;
                } else if (c == ')') {
                    // This was the tail end of a tree.
                    break;
                } else if (!isWhitespace(c)) {
                    symbolSb.append(c);
                    state = ReaderState.LEXICAL;
                    isLexical = true;
                }
            } else if (state == ReaderState.LEXICAL) {
                if (isWhitespace(c) || c == ')') {
                    state = ReaderState.DONE;
                    break;
                } else {
                    symbolSb.append(c);
                }
            } else if (state == ReaderState.NONTERMINAL) {
                if (isWhitespace(c)) {
                    state = ReaderState.CHILDREN;
                    children = readTreesInPtbFormat(lexAlphabet, ntAlphabet, reader);
                    state = ReaderState.DONE;
                    break;
                } else {
                    symbolSb.append(c);
                }
            } else {
                throw new IllegalStateException("Invalid state: " + state);
            }
        }
        if (state != ReaderState.DONE) {
            // This reader did not start with a valid PTB style tree.
            return null;
        }
        
        int start = NOT_INITIALIZED;
        int end = NOT_INITIALIZED;
        IntObjectBimap<String> alphabet = (isLexical ? lexAlphabet : ntAlphabet);
        String symbolStr = symbolSb.toString();
        String l = isLexical ? symbolStr : symbolStr;
        int symbol = alphabet.lookupIndex(l);
        if (symbol == -1) {
            throw new IllegalStateException("Unknown "
                    + (isLexical ? "word" : "nonterminal") + ": "
                    + symbolSb.toString());
        }
        IntNaryTree root = new IntNaryTree(symbol, start, end, children, isLexical, alphabet);
        return root;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\t';
    }
    
    public void preOrderTraversal(FnO1ToVoid<IntNaryTree> function) {
        // Visit this node.
        function.call(this);
        // Pre-order traversal of each child.
        if (children != null) {
            for (IntNaryTree child : children) {
                child.postOrderTraversal(function);
            }
        }
    }
    
    public void postOrderTraversal(FnO1ToVoid<IntNaryTree> function) {
        // Post-order traversal of each child.
        if (children != null) {
            for (IntNaryTree child : children) {
                child.postOrderTraversal(function);
            }
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

    public ArrayList<IntNaryTree> getChildren() {
        return children;
    }

    public boolean isLeaf() {
        return children == null || children.size() == 0;
    }
    
    /**
     * Updates all the start end fields, treating the current node as the root.
     */
    public void updateStartEnd() {
        ArrayList<IntNaryTree> leaves = getLeaves();
        for (int i=0; i<leaves.size(); i++) {
            IntNaryTree leaf = leaves.get(i);
            leaf.start = i;
            leaf.end = i+1;
        }
        postOrderTraversal(new UpdateStartEnd());
    }

    /**
     * Gets the leaves of this tree in left-to-right order.
     */
    public ArrayList<IntNaryTree> getLeaves() {
        LeafCollector leafCollector = new LeafCollector();
        postOrderTraversal(leafCollector);
        return leafCollector.leaves;
    }
    
    // TODO: remove.          
//    public void removeNullElements() {
//        final int nullElement = ntAlphabet.lookupIndex("-NONE-");
//        postOrderFilterNodes(new NaryTreeNodeFilter() {
//            @Override
//            public boolean accept(NaryTreeNode node) {
//                if (node.symbol == nullElement) {
//                    return false;
//                } else if (!node.isLexical && node.isLeaf()) {
//                    return false;
//                }
//            } 
//        });
//    }
    
    public interface NaryTreeNodeFilter {
        public boolean accept(IntNaryTree node);
    }

    /**
     * Keep only those nodes which the filter accepts.
     */
    public void postOrderFilterNodes(final NaryTreeNodeFilter filter) {
        postOrderTraversal(new FnO1ToVoid<IntNaryTree>() {
            @Override
            public void call(IntNaryTree node) {
                if (!node.isLeaf()) {
                    ArrayList<IntNaryTree> filtChildren = new ArrayList<IntNaryTree>();
                    for (IntNaryTree child : node.children) {
                        if (filter.accept(child)) {
                            filtChildren.add(child);
                        }
                    }
                    node.children = filtChildren;
                }
            }
        });
        updateStartEnd();
    }
    
    /**
     * Gets the lexical item ids comprising the sentence.
     */
    public int[] getSentenceIds() {
        ArrayList<IntNaryTree> leaves = getLeaves();
        int[] sent = new int[leaves.size()];
        for (int i=0; i<sent.length; i++) {
            sent[i] = leaves.get(i).symbol;
        }
        return sent;
    }

    public Sentence getSentence() {
        ArrayList<IntNaryTree> leaves = getLeaves();
        ArrayList<String> labels = new ArrayList<String>(leaves.size());
        for (int i = 0; i < leaves.size(); i++) {
            labels.add(leaves.get(i).getSymbolLabel());
        }
        return new Sentence(leaves.get(0).alphabet, labels);
    }

    /**
     * Get a left-binarized form of this tree.
     * 
     * This returns a binarized form that relabels the nodes just as in the
     * Berkeley parser.
     * 
     * @param ntAlphabet The alphabet to use for the non-lexical nodes. 
     */
    public IntBinaryTree leftBinarize(IntObjectBimap<String> ntAlphabet) {
        IntObjectBimap<String> alphabet = isLexical ? this.alphabet : ntAlphabet;
        // Reset the symbol id according to the new alphabet.
        int symbol = alphabet.lookupIndex(getSymbolLabel());

        IntBinaryTree leftChild;
        IntBinaryTree rightChild;
        if (isLeaf()) {
            leftChild = null;
            rightChild = null;
        } else if (children.size() == 1) {
            leftChild = children.get(0).leftBinarize(ntAlphabet);
            rightChild = null;
        } else if (children.size() == 2) {
            leftChild = children.get(0).leftBinarize(ntAlphabet);
            rightChild = children.get(1).leftBinarize(ntAlphabet);
        } else {
            // Define the label of the new parent node as in the Berkeley grammar.
            int xbarParent = alphabet.lookupIndex(GrammarConstants
                    .getBinarizedTag(getSymbolStr()));
            
            LinkedList<IntNaryTree> queue = new LinkedList<IntNaryTree>(children);
            // Start by binarizing the left-most child, and store as L.
            leftChild = queue.removeFirst().leftBinarize(ntAlphabet);
            while (true) {
                // Working left-to-right, remove and binarize the next-left-most child, and store as R.
                rightChild = queue.removeFirst().leftBinarize(ntAlphabet);
                // Break once we've acquired the right-most child.
                if (queue.isEmpty()) {
                    break;
                }
                // Then form a new binary node that has left/right children: L and R.
                // That is, a node (@symbolStr --> (L) (R)).
                // Store this new node as L and repeat.
                leftChild = new IntBinaryTree(xbarParent, leftChild.getStart(),
                        rightChild.getEnd(), leftChild, rightChild, isLexical,
                        alphabet);
            }
        }
        return new IntBinaryTree(symbol, start, end, leftChild, rightChild , isLexical, alphabet);                
    }
    
    @Override
    public String toString() {
        return "NaryTreeNode [symbol=" + getSymbolStr() + "_{" + start + ", "
                + end + "}, children=" + children + "]";
    }

    private class LeafCollector implements FnO1ToVoid<IntNaryTree> {

        public ArrayList<IntNaryTree> leaves = new ArrayList<IntNaryTree>();
        
        @Override
        public void call(IntNaryTree node) {
            if (node.isLeaf()) {
                leaves.add(node);
            }
        }
        
    }
    
    private class UpdateStartEnd implements FnO1ToVoid<IntNaryTree> {

        @Override
        public void call(IntNaryTree node) {
            if (!node.isLeaf()) {
                node.start = node.children.get(0).start;
                node.end = node.children.get(node.children.size()-1).end;
            }
        }
        
    }
    public boolean isLexical() {
        return isLexical;
    }

    public IntObjectBimap<String> getAlphabet() {
        return alphabet;
    }
    
    /** This method does an alphabet lookup and is slow. */
    private String getSymbolStr() {
        return alphabet.lookupObject(symbol);
    }
    
    /** This method does an alphabet lookup and is slow. */
    public String getSymbolLabel() {
        return alphabet.lookupObject(symbol);
    }

    private void setSymbolStr(String symbolStr) {
        String l = isLexical ? symbolStr : symbolStr;
        this.symbol = alphabet.lookupIndex(l);
        if (this.symbol < 0) {
            throw new IllegalArgumentException("Symbol is not in alphabet. symbolStr=" + symbolStr + " id=" + symbol);
        }
    }
    
    public void setSymbolLabel(String label) {
        this.symbol = alphabet.lookupIndex(label);
        if (this.symbol < 0) {
            throw new IllegalArgumentException("Symbol is not in alphabet. label=" + label + " id=" + symbol);
        }
    }
    
    public void setSymbol(int symbol) {
        if (symbol >= alphabet.size() || symbol < 0) {
            throw new IllegalArgumentException("Invalid symbol id: " + symbol);
        }
        this.symbol = symbol;
    }

    public void resetAlphabets(final IntObjectBimap<String> lexAlphabet,
            final IntObjectBimap<String> ntAlphabet) {
        preOrderTraversal(new FnO1ToVoid<IntNaryTree>() {
            public void call(IntNaryTree node) {
                String label = node.getSymbolLabel();
                node.alphabet = node.isLexical ? lexAlphabet : ntAlphabet;
                node.setSymbolLabel(label);
            }
        });
    }
}
