package edu.jhu.parse.cky.intdata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import edu.jhu.data.Label;
import edu.jhu.util.Alphabet;

public class IntBinaryTreebank extends ArrayList<IntBinaryTree> {

    private static final long serialVersionUID = -8440401929408530783L;

    public IntNaryTreebank collapseToNary(Alphabet<Label> ntAlphabet) {
        IntNaryTreebank naryTrees = new IntNaryTreebank();
        for (IntBinaryTree tree : this) {
            naryTrees.add(tree.collapseToNary(ntAlphabet));
        }
        return naryTrees;
    } 

    /**
     * Writes the trees to a file.
     * @param outFile The output file.
     * @throws IOException 
     */
    public void writeTreesInPtbFormat(File outFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        for (IntBinaryTree tree : this) {
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
        for (IntBinaryTree tree : this) {
            writer.write(tree.getAsOneLineString());
            writer.write("\n");
        }
        writer.close();        
    }

    public void resetAlphabets(Alphabet<Label> lexAlphabet,
            Alphabet<Label> ntAlphabet) {
        for (IntBinaryTree tree : this) {
            tree.resetAlphabets(lexAlphabet, ntAlphabet);
        }
    } 

}
