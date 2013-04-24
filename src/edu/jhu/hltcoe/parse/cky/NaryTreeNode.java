package edu.jhu.hltcoe.parse.cky;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import edu.jhu.hltcoe.util.Alphabet;

/**
 * Node in a binary tree for a context free grammar.
 * 
 * @author mgormley
 *
 */
public class NaryTreeNode {

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
    
    public static NaryTreeNode readTreeInPtbFormat(Alphabet<String> alphabet, Reader reader) throws IOException {
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
                    children = readTreesInPtbFormat(alphabet, reader);
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
        
        NaryTreeNode root = new NaryTreeNode(alphabet.lookupIndex(parentSb.toString()), -1, -1, children, isLexical, alphabet);
        return root;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\t';
    }

    public static ArrayList<NaryTreeNode> readTreesInPtbFormat(
            Alphabet<String> alphabet, Reader reader) throws IOException {
        ArrayList<NaryTreeNode> trees = new ArrayList<NaryTreeNode>();
        while (true) {
            NaryTreeNode tree = readTreeInPtbFormat(alphabet, reader);
            if (tree != null) {
                trees.add(tree);
            }            
            if (tree == null || tree.isLexical) {
                break;
            }            
        }
        return trees;
    }
}
