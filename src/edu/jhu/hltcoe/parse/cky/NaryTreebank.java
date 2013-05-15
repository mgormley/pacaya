package edu.jhu.hltcoe.parse.cky;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import edu.jhu.hltcoe.util.Alphabet;

public class NaryTreebank extends ArrayList<NaryTreeNode> {

    private static final long serialVersionUID = -8440401929408530783L;

    /**
     * Reads a list of trees in Penn Treebank format.
     */
    public static NaryTreebank readTreesInPtbFormat(Alphabet<String> lexAlphabet, Alphabet<String> ntAlphabet, Reader reader) throws IOException {
        NaryTreebank trees = new NaryTreebank();
        while (true) {
            NaryTreeNode tree = NaryTreeNode.readTreeInPtbFormat(lexAlphabet, ntAlphabet, reader);
            if (tree != null) {
                trees.add(tree);
            }
            if (tree == null) {
                break;
            }
        }
        return trees;
    } 

}
