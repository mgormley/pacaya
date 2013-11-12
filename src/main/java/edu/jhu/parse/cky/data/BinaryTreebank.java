package edu.jhu.parse.cky.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import edu.jhu.data.Label;
import edu.jhu.util.Alphabet;

public class BinaryTreebank extends ArrayList<BinaryTree> {

    private static final long serialVersionUID = -8440401929408530783L;

    public NaryTreebank collapseToNary(Alphabet<Label> ntAlphabet) {
        NaryTreebank naryTrees = new NaryTreebank();
        for (BinaryTree tree : this) {
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
        for (BinaryTree tree : this) {
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
        for (BinaryTree tree : this) {
            writer.write(tree.getAsOneLineString());
            writer.write("\n");
        }
        writer.close();        
    }

    public void resetAlphabets(Alphabet<Label> lexAlphabet,
            Alphabet<Label> ntAlphabet) {
        for (BinaryTree tree : this) {
            tree.resetAlphabets(lexAlphabet, ntAlphabet);
        }
    } 

}
