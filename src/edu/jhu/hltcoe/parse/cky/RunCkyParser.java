package edu.jhu.hltcoe.parse.cky;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.cli.ArgParser;
import edu.jhu.hltcoe.util.cli.Opt;

public class RunCkyParser {
    
    private static final Logger log = Logger.getLogger(RunCkyParser.class);

    @Opt(hasArg = true, required = true, description = "Penn Treebank training data directory")
    public static File train;
    @Opt(hasArg = true, required = true, description = "CNF grammar")
    public static File grammar;
    @Opt(hasArg = true, description = "File to which the trees should be written")
    public static File treeFile = null;
    @Opt(hasArg = true, description = "Pseudo random number generator seed")
    public static long seed = Prng.DEFAULT_SEED;

    public void run() throws IOException {
        Alphabet<String> lexAlphabet = new Alphabet<String>();
        Alphabet<String> ntAlphabet = new Alphabet<String>();

        log.info("Reading grammar from file");
        CnfGrammarBuilder builder = new CnfGrammarBuilder(lexAlphabet, ntAlphabet);
        builder.loadFromFile(grammar);        
        CnfGrammar grammar = builder.getGrammar();
        
        log.info("Stopping alphabet growth");
        lexAlphabet.stopGrowth();
        ntAlphabet.stopGrowth();
        
        log.info("Reading trees from file: " + train);
        ArrayList<NaryTree> naryTrees = new ArrayList<NaryTree>();
        List<File> mrgFiles = Utilities.getMatchingFiles(train, ".*\\.mrg");
        for (File mrgFile : mrgFiles) {
            BufferedReader reader = new BufferedReader(new FileReader(mrgFile));
            naryTrees.addAll(NaryTree.readTreesInPtbFormat(lexAlphabet, ntAlphabet, reader));
            reader.close();
        }
        
        // TODO: remove function tags and null elements.
        
        log.info("Binarizing " + naryTrees.size() + " trees");
        ArrayList<BinaryTree> binaryTrees = new ArrayList<BinaryTree>();
        for (NaryTree tree : naryTrees) {
            // TODO: use a separate alphabet?
            binaryTrees.add(tree.binarize());
        }
        naryTrees = null;
        
        log.info("Parsing " + binaryTrees.size() + " trees");
        ArrayList<BinaryTree> parseTrees = new ArrayList<BinaryTree>();
        for (BinaryTree tree : binaryTrees) {
            int[] sent = tree.getSentence();
            Chart chart = CkyPcfgParser.parseSentence(sent, grammar);
            Pair<BinaryTree, Double> pair = chart.getViterbiParse();
            parseTrees.add(pair.get1());
        }
        
        if (treeFile != null) {
            log.info("Writing trees to file: " + treeFile);
            BufferedWriter writer = new BufferedWriter(new FileWriter(treeFile));
            for (BinaryTree tree : parseTrees) {
                writer.write(tree.getAsPennTreebankString());
                writer.write("\n\n");
            }
            writer.close();
        }
    }
    
    public static void main(String[] args) throws IOException {
        ArgParser parser = new ArgParser(RunCkyParser.class);
        parser.addClass(RunCkyParser.class);
        CommandLine cmd = null;
        try {
            cmd = parser.parseArgs(args);
        } catch (ParseException e) {
            log.error(e.getMessage());
            parser.printUsage();
            System.exit(1);
        }
        
        Prng.seed(seed);
        
        RunCkyParser pipeline = new RunCkyParser();
        pipeline.run();
    }
}
