package edu.jhu.parse.cky.data;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.jhu.nlp.data.Sentence;
import edu.jhu.parse.cky.GrammarConstants;
import edu.jhu.prim.util.Lambda.FnO1ToVoid;
import edu.jhu.util.Alphabet;
import edu.jhu.util.files.Files;

/**
 * N-ary tree for a context free grammar.
 * 
 * @author mgormley
 *
 */
public class NaryTree {

    public static final int NOT_INITIALIZED = -1;
    private String symbol;
    // Start token of this span (inclusive)
    private int start;
    // End token of this span (exclusive)
    private int end;
    // Children of this node, ordered left-to-right
    private ArrayList<NaryTree> children;
    private boolean isLexical;
    // Parent of this node.
    private NaryTree parent;
    
    public NaryTree(String symbol, int start, int end, ArrayList<NaryTree> children,
            boolean isLexical) {
        this.symbol = symbol;
        this.start = start;
        this.end = end;
        this.children = children;
        this.isLexical = isLexical;
        this.parent = null;
        for (NaryTree child : children) {
            child.parent = this;
        }
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
            sb.append(getSymbol());
        } else {
            sb.append("(");
            sb.append(getSymbol());
            
            // If this is a constant instead, then we have each depth in one column.
            int numNewChars = 1 + getSymbol().length();

            for (int i=0; i<children.size(); i++) {
                NaryTree child = children.get(i);
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

    /**
     * Reads a full tree in Penn Treebank format. Such a tree should include an
     * outer set of parentheses. The returned tree will have initialized the
     * start/end fields.
     */
    public static NaryTree readTreeInPtbFormat(Reader reader) throws IOException {
        Files.readUntilCharacter(reader, '(');
        NaryTree root = NaryTree.readSubtreeInPtbFormat(reader);
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
    private static NaryTree readSubtreeInPtbFormat(Reader reader) throws IOException {
        ReaderState state = ReaderState.START;
        StringBuilder symbolSb = new StringBuilder();
        ArrayList<NaryTree> children = null;
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
                    children = readTreesInPtbFormat(reader);
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
        String symbol = symbolSb.toString();
        NaryTree root = new NaryTree(symbol, start, end, children, isLexical);
        return root;
    }

    private static ArrayList<NaryTree> readTreesInPtbFormat(Reader reader) throws IOException {
        ArrayList<NaryTree> trees = new ArrayList<NaryTree>();
        while (true) {
            NaryTree tree = readSubtreeInPtbFormat(reader);
            if (tree != null) {
                trees.add(tree);
            }            
            if (tree == null || tree.isLexical) {
                break;
            }            
        }
        return trees;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\t';
    }
    
    public void preOrderTraversal(FnO1ToVoid<NaryTree> function) {
        // Visit this node.
        function.call(this);
        // Pre-order traversal of each child.
        if (children != null) {
            for (NaryTree child : children) {
                child.postOrderTraversal(function);
            }
        }
    }
    
    public void postOrderTraversal(FnO1ToVoid<NaryTree> function) {
        // Post-order traversal of each child.
        if (children != null) {
            for (NaryTree child : children) {
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

    public ArrayList<NaryTree> getChildren() {
        return children;
    }

    public boolean isLeaf() {
        return children == null || children.size() == 0;
    }

    public NaryTree getParent() {
        return parent;
    }
        
    /**
     * Updates all the start end fields, treating the current node as the root.
     */
    public void updateStartEnd() {
        ArrayList<NaryTree> leaves = getLeaves();
        for (int i=0; i<leaves.size(); i++) {
            NaryTree leaf = leaves.get(i);
            leaf.start = i;
            leaf.end = i+1;
        }
        postOrderTraversal(new UpdateStartEnd());
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
        public boolean accept(NaryTree node);
    }

    /**
     * Keep only those nodes which the filter accepts.
     */
    public void postOrderFilterNodes(final NaryTreeNodeFilter filter) {
        postOrderTraversal(new FnO1ToVoid<NaryTree>() {
            @Override
            public void call(NaryTree node) {
                if (!node.isLeaf()) {
                    ArrayList<NaryTree> filtChildren = new ArrayList<NaryTree>();
                    for (NaryTree child : node.children) {
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
     * Gets the leaves of this tree in left-to-right order.
     */
    public ArrayList<NaryTree> getLeaves() {
        LeafCollector leafCollector = new LeafCollector();
        postOrderTraversal(leafCollector);
        return leafCollector.leaves;
    }
    
    /**
     * Gets the lexical leaves of this tree in left-to-right order.
     */
    public ArrayList<NaryTree> getLexicalLeaves() {
        LexicalLeafCollector leafCollector = new LexicalLeafCollector();
        postOrderTraversal(leafCollector);
        return leafCollector.leaves;
    }

    /** Gets the leaf containing the specified token index. */
    public NaryTree getLeafAt(int idx) {
        NaryTree leaf = null;
        for (NaryTree l : this.getLeaves()) {
            if (l.start <= idx && idx < l.end) {
                leaf = l;
            }
        }
        return leaf;
    }
    
    /**
     * Gets the lexical item ids comprising the sentence.
     */
    public int[] getSentenceIds(Alphabet<String> lexAlphabet) {
        ArrayList<NaryTree> leaves = getLexicalLeaves();
        int[] sent = new int[leaves.size()];
        for (int i=0; i<sent.length; i++) {
            sent[i] = lexAlphabet.lookupIndex(leaves.get(i).symbol);
        }
        return sent;
    }

    public Sentence getSentence(Alphabet<String> lexAlphabet) {
        ArrayList<NaryTree> leaves = getLexicalLeaves();
        ArrayList<String> labels = new ArrayList<String>(leaves.size());
        for (int i = 0; i < leaves.size(); i++) {
            labels.add(leaves.get(i).symbol);
        }
        return new Sentence(lexAlphabet, labels);
    }

    public List<String> getWords() {
        ArrayList<NaryTree> leaves = getLexicalLeaves();
        ArrayList<String> words = new ArrayList<String>(leaves.size());
        for (int i = 0; i < leaves.size(); i++) {            
            words.add(leaves.get(i).symbol);
        }
        return words;
    }

    private class LeafCollector implements FnO1ToVoid<NaryTree> {

        public ArrayList<NaryTree> leaves = new ArrayList<NaryTree>();
        
        @Override
        public void call(NaryTree node) {
            if (node.isLeaf()) {
                leaves.add(node);
            }
        }
        
    }
    
    private class LexicalLeafCollector implements FnO1ToVoid<NaryTree> {

        public ArrayList<NaryTree> leaves = new ArrayList<NaryTree>();
        
        @Override
        public void call(NaryTree node) {
            if (node.isLeaf() && node.isLexical()) {
                leaves.add(node);
            }
        }
        
    }

    /**
     * Get a left-binarized form of this tree.
     * 
     * This returns a binarized form that relabels the nodes just as in the
     * Berkeley parser.
     */
    public BinaryTree leftBinarize() {
        BinaryTree leftChild;
        BinaryTree rightChild;
        if (isLeaf()) {
            leftChild = null;
            rightChild = null;
        } else if (children.size() == 1) {
            leftChild = children.get(0).leftBinarize();
            rightChild = null;
        } else if (children.size() == 2) {
            leftChild = children.get(0).leftBinarize();
            rightChild = children.get(1).leftBinarize();
        } else {
            // Define the label of the new parent node as in the Berkeley grammar.
            String xbarParent = GrammarConstants.getBinarizedTag(symbol);
            
            LinkedList<NaryTree> queue = new LinkedList<NaryTree>(children);
            // Start by binarizing the left-most child, and store as L.
            leftChild = queue.removeFirst().leftBinarize();
            while (true) {
                // Working left-to-right, remove and binarize the next-left-most child, and store as R.
                rightChild = queue.removeFirst().leftBinarize();
                // Break once we've acquired the right-most child.
                if (queue.isEmpty()) {
                    break;
                }
                // Then form a new binary node that has left/right children: L and R.
                // That is, a node (@symbolStr --> (L) (R)).
                // Store this new node as L and repeat.
                leftChild = new BinaryTree(xbarParent, leftChild.getStart(),
                        rightChild.getEnd(), leftChild, rightChild, isLexical);
            }
        }
        return new BinaryTree(symbol, start, end, leftChild, rightChild , isLexical);                
    }
    
    private class UpdateStartEnd implements FnO1ToVoid<NaryTree> {

        @Override
        public void call(NaryTree node) {
            if (!node.isLeaf()) {
                node.start = node.children.get(0).start;
                node.end = node.children.get(node.children.size()-1).end;
            }
        }
        
    }

    /** Intern all the strings. */
    public void intern() {
        symbol = symbol.intern();
        if (children != null) {
            for (NaryTree node : children) {
                node.intern();
            }
        }
    }

    public boolean isLexical() {
        return isLexical;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    @Override
    public String toString() {
        return "NaryTree [symbol=" + symbol + ", start=" + start + ", end=" + end + ", isLexical=" + isLexical + "]";
    }

    public void addChild(NaryTree child) {
        children.add(child);
        child.parent = this;
    }

}
