package edu.jhu.nlp.embed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.util.Alphabet;

/**
 * Storage for a set of word embeddings. Also contains a method to load embeddings from a text file. 
 * @author mgormley
 */
public class Embeddings implements Serializable {
    
    public enum Scaling { NONE, L1_NORM, L2_NORM, STD_NORMAL }
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(Embeddings.class);
    private static final Pattern DIGITS = Pattern.compile("[0-9]");
    private Map<String,double[]> word2embed;
    private Alphabet<String> alphabet;
    
    public Embeddings() {
        word2embed = new HashMap<String,double[]>();
        alphabet = new Alphabet<String>();
    }
    
    /**
     * Loads the embeddings from a tab-separated text file in UTF-8. Each line consists of n+1 columns for an
     * n-dimensional embedding. The first column is the word. The i+1st column is the ith dimension
     * of the embedding for that word.
     * 
     * @param txtFile The UTF-8 encoded tsv file.
     * @throws IOException
     */
    public void loadFromFile(File txtFile) throws IOException {
        log.info("Reading word embeddings from file: " + txtFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(txtFile), "UTF-8"));
        String line;
        Pattern tab = Pattern.compile("\t");
        while ((line = reader.readLine()) != null) {
            String[] splits = tab.split(line);
            if (splits.length > 0) {
                String word = splits[0];
                double[] embed = new double[splits.length - 1];
                for (int i=1; i<splits.length; i++) {
                    embed[i-1] = Double.parseDouble(splits[i]);
                }
                word2embed.put(word, embed);
                alphabet.lookupIndex(word);
            }
        }
        reader.close();
    }
        
    // TODO: Could consider frequency of word when selecting embedding.
    public double[] getEmbedding(String word) {
        if (!word2embed.containsKey(word)) {
            word = DIGITS.matcher(word).replaceAll("#");
            if (!word2embed.containsKey(word)) {
                word = word.toLowerCase();
                if (!word2embed.containsKey(word)) {
                    word = word.toUpperCase();
                    if (!word2embed.containsKey(word)) {
                        word = "<UNK>";
                    }
                }
            }
        }
        return word2embed.get(word);
    }
    
    public Alphabet<String> getAlphabet() {
        return alphabet;
    }

    private void writeEmbeddings(PrintStream out) {
        for (String word : this.getAlphabet().getObjects()) {
            out.print(word);
            for (double e_i : this.getEmbedding(word)) {
                out.print("\t");
                out.print(e_i);
            }
            out.println();
        }
    }
    
    public void normPerWord(Scaling sc) {
        if (sc == Scaling.NONE || sc == null){
            return;
        }
        for (double[] emb : word2embed.values()) {
            if (sc == Scaling.L1_NORM) {
                DoubleArrays.scale(emb, 1.0 / DoubleArrays.l1norm(emb));
            } else if (sc == Scaling.L2_NORM) {
                DoubleArrays.scale(emb, 1.0 / DoubleArrays.l2norm(emb));
            } else if (sc == Scaling.STD_NORMAL) {
                DoubleArrays.add(emb, -DoubleArrays.mean(emb));
                DoubleArrays.scale(emb, 1.0 / DoubleArrays.stdDev(emb));
            } else {
                throw new RuntimeException("Unsupported method rescaling: " + sc);
            }
        }
    }

    public void scalePerWord(double alpha) {
        for (double[] emb : word2embed.values()) {
            DoubleArrays.scale(emb, alpha);
        }
    }
    
    public static void main(String[] args) throws IOException {
        Embeddings embed = new Embeddings();
        embed.loadFromFile(new File(args[0]));
        embed.writeEmbeddings(System.out);
    }
    
}
