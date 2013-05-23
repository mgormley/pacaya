package edu.jhu.hltcoe.parse.cky;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import edu.jhu.hltcoe.util.Alphabet;

public class NaryTreebank extends ArrayList<NaryTree> {

    private static final long serialVersionUID = -8440401929408530783L;

    /**
     * Reads a list of trees in Penn Treebank format.
     */
    public static NaryTreebank readTreesInPtbFormat(Alphabet<String> lexAlphabet, Alphabet<String> ntAlphabet, Reader reader) throws IOException {
        NaryTreebank trees = new NaryTreebank();
        while (true) {
            NaryTree tree = NaryTree.readTreeInPtbFormat(lexAlphabet, ntAlphabet, reader);
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
     * Writes the trees to a file.
     * @param outFile The output file.
     * @throws IOException 
     */
    public void writeTreesInPtbFormat(File outFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        for (NaryTree tree : this) {
            writer.write(tree.getAsPennTreebankString());
            writer.write("\n\n");
        }
        writer.close();        
    } 

    /**
     * Writes the trees to a file.
     * @param outFile The output file.
     * @throws IOException 
     */
    public void writeTreesInOneLineFormat(File outFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        for (NaryTree tree : this) {
            writer.write(tree.getAsOneLineString());
            writer.write("\n");
        }
        writer.close();        
    }

    public void writeSentencesInOneLineFormat(String outFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        for (NaryTree tree : this) {
            List<String> sent = tree.getSentenceStrs();
            writer.write(StringUtils.join(sent.toArray(), " "));
            writer.write("\n");
        }
        writer.close(); 
    } 

    public BinaryTreebank leftBinarize(Alphabet<String> ntAlphabet) {
        BinaryTreebank binaryTrees = new BinaryTreebank();
        for (NaryTree tree : this) {
            binaryTrees.add(tree.leftBinarize(ntAlphabet));
        }
        return binaryTrees;
    }

    public void resetAlphabets(Alphabet<String> lexAlphabet,
            Alphabet<String> ntAlphabet) {
        for (NaryTree tree : this) {
            tree.resetAlphabets(lexAlphabet, ntAlphabet);
        }
    }

}
