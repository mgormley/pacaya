package edu.jhu.hltcoe.parse.cky;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import edu.jhu.hltcoe.parse.cky.Lambda.LambdaOne;
import edu.jhu.hltcoe.util.Alphabet;

/**
 * N-ary tree from a context free grammar.
 * 
 * @author mgormley
 *
 */
public class NaryTree {
    
    private NaryTreeNode root;

    public NaryTree(NaryTreeNode root) {
        this.root = root;
    }

    @Override
    public String toString() {
        return "CfgTree [root=" + root + "]";
    }
    

    public String getAsPennTreebankString() {
        return "( " + root.getAsPennTreebankString() + " )";
    }

    public static ArrayList<NaryTree> readTreesInPtbFormat(Alphabet<String> lexAlphabet, Alphabet<String> ntAlphabet, Reader reader) throws IOException {
        ArrayList<NaryTree> trees = new ArrayList<NaryTree>();
        while (true) {
            NaryTree tree = readTreeInPtbFormat(lexAlphabet, ntAlphabet, reader);
            if (tree != null) {
                trees.add(tree);
            }
            if (tree == null) {
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
        readUntilCharacter(reader, '(');
        NaryTreeNode root = NaryTreeNode.readTreeInPtbFormat(lexAlphabet, ntAlphabet, reader);
        readUntilCharacter(reader, ')');
        root.updateStartEnd();
        NaryTree tree = new NaryTree(root);        
        return tree;
    }

    private static void readUntilCharacter(Reader reader, char stopChar) throws IOException {
        char[] cbuf = new char[1];
        while (reader.read(cbuf) != -1) {
            char c = cbuf[0];
            if (c == stopChar) {
                break;
            }
        }
    }
        
}
