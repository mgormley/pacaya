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
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.cli.ArgParser;
import edu.jhu.hltcoe.util.cli.Opt;

public class RunCkyParser {
    
    private static final Logger log = Logger.getLogger(RunCkyParser.class);

    @Opt(hasArg = true, required = true, description = "Penn Treebank training data directory")
    public static File train;
    @Opt(hasArg = true, description = "File to which the trees should be written")
    public static File treeFile = null;
    @Opt(hasArg = true, description = "Pseudo random number generator seed")
    public static long seed = Prng.DEFAULT_SEED;

    public void run() throws IOException {
        Alphabet<String> lexAlphabet = new Alphabet<String>();
        Alphabet<String> ntAlphabet = new Alphabet<String>();

        ArrayList<NaryTree> trees = new ArrayList<NaryTree>();
        List<File> mrgFiles = Utilities.getMatchingFiles(train, ".*\\.mrg");
        for (File mrgFile : mrgFiles) {
            BufferedReader reader = new BufferedReader(new FileReader(mrgFile));
            trees.addAll(NaryTree.readTreesInPtbFormat(lexAlphabet, ntAlphabet, reader));
            reader.close();
        }
        
        if (treeFile != null) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(treeFile));
            for (NaryTree tree : trees) {
                BinaryTree binaryTree = tree.binarize();
                writer.write(binaryTree.getAsPennTreebankString());
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
