package edu.jhu.hltcoe.parse.cky;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Stack;

import edu.jhu.hltcoe.parse.cky.Lambda.LambdaOne;
import edu.jhu.hltcoe.util.Alphabet;

/**
 * Node in an n-ary tree for a context free grammar.
 * 
 * @author mgormley
 *
 */
public class NaryTreeNode {

    private static final int NOT_INITIALIZED = -1;
    private int parent;
    private int start;
    private int end;
    private ArrayList<NaryTreeNode> children;
    private boolean isLexical;
    
    private Alphabet<String> alphabet;
    
    public NaryTreeNode(int parent, int start, int end, ArrayList<NaryTreeNode> children,
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
    
    @Override
    public String toString() {
        return "CfgTreeNode [parent=" + getParentStr() + "_{" + start + ", "
                + end + "}, children=" + children + "]";
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

            for (int i=0; i<children.size(); i++) {
                NaryTreeNode child = children.get(i);
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

    public static ArrayList<NaryTreeNode> readTreesInPtbFormat(
            Alphabet<String> lexAlphabet, Alphabet<String> ntAlphabet, Reader reader) throws IOException {
        ArrayList<NaryTreeNode> trees = new ArrayList<NaryTreeNode>();
        while (true) {
            NaryTreeNode tree = readTreeInPtbFormat(lexAlphabet, ntAlphabet, reader);
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
    public static NaryTreeNode readTreeInPtbFormat(Alphabet<String> lexAlphabet, Alphabet<String> ntAlphabet, Reader reader) throws IOException {
        ReaderState state = ReaderState.START;
        StringBuilder parentSb = new StringBuilder();
        ArrayList<NaryTreeNode> children = null;
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
        
        //String treeStr = origTreeStr.replaceAll("\\s+", " ");
        //String parentStr = treeStr.substring(ntStart, ntEnd);
        // TODO: resolve start and end.
        int start = NOT_INITIALIZED;
        int end = NOT_INITIALIZED;
        NaryTreeNode root = new NaryTreeNode(ntAlphabet.lookupIndex(parentSb.toString()), start, end, children, isLexical, ntAlphabet);
        return root;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\t';
    }
    
    public void preOrderTraversal(LambdaOne<NaryTreeNode> function) {
        // Visit this node.
        function.apply(this);
        // Pre-order traversal of each child.
        if (children != null) {
            for (NaryTreeNode child : children) {
                child.postOrderTraversal(function);
            }
        }
    }
    
    public void postOrderTraversal(LambdaOne<NaryTreeNode> function) {
        // Post-order traversal of each child.
        if (children != null) {
            for (NaryTreeNode child : children) {
                child.postOrderTraversal(function);
            }
        }
        // Visit this node.
        function.apply(this);
    }

    public boolean isLeaf() {
        return children == null || children.size() == 0;
    }
    
    /**
     * Updates all the start end fields, treating the current node as the root.
     */
    public void updateStartEnd() {
        ArrayList<NaryTreeNode> leaves = getLeaves();
        for (int i=0; i<leaves.size(); i++) {
            NaryTreeNode leaf = leaves.get(i);
            leaf.start = i;
            leaf.end = i+1;
        }
        postOrderTraversal(new UpdateStartEnd());
    }

    private ArrayList<NaryTreeNode> getLeaves() {
        LeafCollector leafCollector = new LeafCollector();
        postOrderTraversal(leafCollector);
        return leafCollector.leaves;
    }

    private class LeafCollector implements LambdaOne<NaryTreeNode> {

        public ArrayList<NaryTreeNode> leaves = new ArrayList<NaryTreeNode>();
        
        @Override
        public void apply(NaryTreeNode node) {
            if (node.isLeaf()) {
                leaves.add(node);
            }
        }
        
    }
    
    private class UpdateStartEnd implements LambdaOne<NaryTreeNode> {

        @Override
        public void apply(NaryTreeNode node) {
            if (!node.isLeaf()) {
                node.start = node.children.get(0).start;
                node.end = node.children.get(node.children.size()-1).end;
            }
        }
        
    }

    public int getStart() {
        return start;
    }
    
    public int getEnd() {
        return end;
    }

    public ArrayList<NaryTreeNode> getChildren() {
        return children;
    }
}
