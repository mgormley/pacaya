package edu.jhu.pacaya.parse.cky.intdata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.jhu.prim.bimap.IntObjectBimap;

public class IntNaryTreebank extends ArrayList<IntNaryTree> {

    private static final long serialVersionUID = -8440401929408530783L;

    /**
     * Reads a list of trees in Penn Treebank format.
     */
    public static IntNaryTreebank readTreesInPtbFormat(IntObjectBimap<String> lexAlphabet, IntObjectBimap<String> ntAlphabet, Reader reader) throws IOException {
        IntNaryTreebank trees = new IntNaryTreebank();
        while (true) {
            IntNaryTree tree = IntNaryTree.readTreeInPtbFormat(lexAlphabet, ntAlphabet, reader);
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
        for (IntNaryTree tree : this) {
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
        for (IntNaryTree tree : this) {
            writer.write(tree.getAsOneLineString());
            writer.write("\n");
        }
        writer.close();        
    }

    public void writeSentencesInOneLineFormat(String outFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        for (IntNaryTree tree : this) {
            List<String> sent = tree.getSentence().getLabels();
            writer.write(StringUtils.join(sent.toArray(), " "));
            writer.write("\n");
        }
        writer.close(); 
    } 

    public IntBinaryTreebank leftBinarize(IntObjectBimap<String> ntAlphabet) {
        IntBinaryTreebank binaryTrees = new IntBinaryTreebank();
        for (IntNaryTree tree : this) {
            binaryTrees.add(tree.leftBinarize(ntAlphabet));
        }
        return binaryTrees;
    }

    public void resetAlphabets(IntObjectBimap<String> lexAlphabet,
            IntObjectBimap<String> ntAlphabet) {
        for (IntNaryTree tree : this) {
            tree.resetAlphabets(lexAlphabet, ntAlphabet);
        }
    }

}
