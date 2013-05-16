package edu.jhu.hltcoe.parse.cky;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;

import edu.jhu.hltcoe.parse.cky.Lambda.LambdaOne;
import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.util.Files;

/**
 * N-ary tree for a context free grammar.
 * 
 * @author mgormley
 *
 */
public class NaryTree {

    private static final int NOT_INITIALIZED = -1;
    private int parent;
    private int start;
    private int end;
    /** Children of this node, ordered left-to-right */
    private ArrayList<NaryTree> children;
    private boolean isLexical;
    
    private Alphabet<String> alphabet;
    
    public NaryTree(int parent, int start, int end, ArrayList<NaryTree> children,
            boolean isLexical, Alphabet<String> alphabet) {
        this.parent = parent;
        this.start = start;
        this.end = end;
        this.children = children;
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

    public static ArrayList<NaryTree> readTreesInPtbFormat(
            Alphabet<String> lexAlphabet, Alphabet<String> ntAlphabet, Reader reader) throws IOException {
        ArrayList<NaryTree> trees = new ArrayList<NaryTree>();
        while (true) {
            NaryTree tree = readSubtreeInPtbFormat(lexAlphabet, ntAlphabet, reader);
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
    public static NaryTree readTreeInPtbFormat(Alphabet<String> lexAlphabet, Alphabet<String> ntAlphabet, Reader reader) throws IOException {
        Files.readUntilCharacter(reader, '(');
        NaryTree root = NaryTree.readSubtreeInPtbFormat(lexAlphabet, ntAlphabet, reader);
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
    private static NaryTree readSubtreeInPtbFormat(Alphabet<String> lexAlphabet, Alphabet<String> ntAlphabet, Reader reader) throws IOException {
        ReaderState state = ReaderState.START;
        StringBuilder parentSb = new StringBuilder();
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
                    parentSb.append(c);
                    state = ReaderState.LEXICAL;
                    isLexical = true;
                }
            } else if (state == ReaderState.LEXICAL) {
                if (isWhitespace(c) || c == ')') {
                    state = ReaderState.DONE;
                    break;
                } else {
                    parentSb.append(c);
                }
            } else if (state == ReaderState.NONTERMINAL) {
                if (isWhitespace(c)) {
                    state = ReaderState.CHILDREN;
                    children = readTreesInPtbFormat(lexAlphabet, ntAlphabet, reader);
                    state = ReaderState.DONE;
                    break;
                } else {
                    parentSb.append(c);
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
        Alphabet<String> alphabet = (isLexical ? lexAlphabet : ntAlphabet);
        int parent = alphabet.lookupIndex(parentSb.toString());
        if (parent == -1) {
            throw new IllegalStateException("Unknown "
                    + (isLexical ? "word" : "nonterminal") + ": "
                    + parentSb.toString());
        }
        NaryTree root = new NaryTree(parent, start, end, children, isLexical, alphabet);
        return root;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\t';
    }
    
    public void preOrderTraversal(LambdaOne<NaryTree> function) {
        // Visit this node.
        function.call(this);
        // Pre-order traversal of each child.
        if (children != null) {
            for (NaryTree child : children) {
                child.postOrderTraversal(function);
            }
        }
    }
    
    public void postOrderTraversal(LambdaOne<NaryTree> function) {
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

    /**
     * Gets the leaves of this tree in left-to-right order.
     */
    public ArrayList<NaryTree> getLeaves() {
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
//                if (node.parent == nullElement) {
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
        postOrderTraversal(new LambdaOne<NaryTree>() {
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
     * Gets the lexical item ids comprising the sentence.
     */
    public int[] getSentence() {
        ArrayList<NaryTree> leaves = getLeaves();
        int[] sent = new int[leaves.size()];
        for (int i=0; i<sent.length; i++) {
            sent[i] = leaves.get(i).parent;
        }
        return sent;
    }

    /**
     * Get a left-binarized form of this tree.
     * 
     * This returns a binarized form that relabels the nodes just as in the
     * Berkeley parser.
     * 
     * @param ntAlphabet The alphabet to use for the non-lexical nodes. 
     */
    public BinaryTree leftBinarize(Alphabet<String> ntAlphabet) {
        Alphabet<String> alphabet = isLexical ? this.alphabet : ntAlphabet;

        BinaryTree leftChild;
        BinaryTree rightChild;
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
            int xbarParent = ntAlphabet.lookupIndex("@" + getParentStr());
            
            LinkedList<NaryTree> queue = new LinkedList<NaryTree>(children);
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
                // That is, a node (@parentStr --> (L) (R)).
                // Store this new node as L and repeat.
                leftChild = new BinaryTree(xbarParent, leftChild.getStart(),
                        rightChild.getEnd(), leftChild, rightChild, isLexical,
                        alphabet);
            }
        }
        return new BinaryTree(parent, start, end, leftChild, rightChild , isLexical, alphabet);                
    }
    
    @Override
    public String toString() {
        return "NaryTreeNode [parent=" + getParentStr() + "_{" + start + ", "
                + end + "}, children=" + children + "]";
    }

    private class LeafCollector implements LambdaOne<NaryTree> {

        public ArrayList<NaryTree> leaves = new ArrayList<NaryTree>();
        
        @Override
        public void call(NaryTree node) {
            if (node.isLeaf()) {
                leaves.add(node);
            }
        }
        
    }
    
    private class UpdateStartEnd implements LambdaOne<NaryTree> {

        @Override
        public void call(NaryTree node) {
            if (!node.isLeaf()) {
                node.start = node.children.get(0).start;
                node.end = node.children.get(node.children.size()-1).end;
            }
        }
        
    }

    public int getParent() {
        return parent;
    }

    public boolean isLexical() {
        return isLexical;
    }

    public Alphabet<String> getAlphabet() {
        return alphabet;
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
