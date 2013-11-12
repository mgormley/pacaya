package edu.jhu.data.convert;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.DepTreebankReader;
import edu.jhu.data.DepTreebankReader.DatasetType;
import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.conll.CoNLLXSentence;
import edu.jhu.data.conll.CoNLLXWriter;
import edu.jhu.prim.arrays.IntArrays;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prng;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;

/**
 * Converts Penn Treebank data to a CoNLL-X file.
 *  
 * @author mgormley
 *
 */
public class Ptb2ConllX {

    private static final Logger log = Logger.getLogger(Ptb2ConllX.class);

    @Opt(hasArg = true, required = true, description = "Penn Treebank training data directory")
    public static File ptbIn;
    @Opt(hasArg = true, required = true, description = "CoNLL-X output file")
    public static File conllxOut;
    @Opt(hasArg = true, description = "Pseudo random number generator seed")
    public static long seed = Prng.DEFAULT_SEED;

    public void run() throws IOException {
        Alphabet<Label> alphabet = new Alphabet<Label>();
        DepTreebank trees = DepTreebankReader.getTreebank(ptbIn, DatasetType.PTB, alphabet);
        CoNLLXWriter conllxWriter = new CoNLLXWriter(conllxOut);
        for (int i=0; i<trees.size(); i++) {
            DepTree tree = trees.get(i);
            Sentence sent = trees.getSentences().get(i);
            int[] heads = tree.getParents();
            IntArrays.add(heads, 1);
            conllxWriter.write(new CoNLLXSentence(sent, heads));
        }
        conllxWriter.close();
    }
    
    public static void main(String[] args) throws IOException {
        ArgParser parser = new ArgParser(Ptb2ConllX.class);
        parser.addClass(Ptb2ConllX.class);
        parser.addClass(DepTreebankReader.class);
        try {
            parser.parseArgs(args);
        } catch (ParseException e) {
            log.error(e.getMessage());
            parser.printUsage();
            System.exit(1);
        }
        
        Prng.seed(seed);
        
        Ptb2ConllX pipeline = new Ptb2ConllX();
        pipeline.run();
    }

}
