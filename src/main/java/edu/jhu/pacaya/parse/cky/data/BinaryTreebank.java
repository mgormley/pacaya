package edu.jhu.pacaya.parse.cky.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class BinaryTreebank extends ArrayList<BinaryTree> {

    private static final long serialVersionUID = -8440401929408530783L;

    public NaryTreebank collapseToNary() {
        NaryTreebank naryTrees = new NaryTreebank();
        for (BinaryTree tree : this) {
            naryTrees.add(tree.collapseToNary());
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

    /** Intern all the strings. */
    public void intern() {
        for (BinaryTree tree : this) {
            tree.intern();
        }
    }
    
}
