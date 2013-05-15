package edu.jhu.hltcoe.parse.cky;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.hltcoe.parse.cky.Lambda.LambdaOne;
import edu.jhu.hltcoe.parse.cky.NaryTreeNode.NaryTreeNodeFilter;
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
    @Opt(hasArg = true, description = "Directory containing evalb")
    public static File evalbDir = null;

    public void run() throws IOException {
        Alphabet<String> lexAlphabet = new Alphabet<String>();
        Alphabet<String> ntAlphabet = new Alphabet<String>();

        log.info("Reading grammar from file");
        CnfGrammarBuilder builder = new CnfGrammarBuilder(lexAlphabet, ntAlphabet);
        builder.loadFromFile(grammar);        
        CnfGrammar grammar = builder.getGrammar();
        
        log.info("Reading trees from file: " + train);
        NaryTreebank naryTrees = new NaryTreebank();
        List<File> mrgFiles = Utilities.getMatchingFiles(train, ".*\\.mrg");
        for (File mrgFile : mrgFiles) {
            BufferedReader reader = new BufferedReader(new FileReader(mrgFile));
            naryTrees.addAll(NaryTreebank.readTreesInPtbFormat(lexAlphabet, ntAlphabet, reader));
            reader.close();
        }
        
        log.info("Stopping alphabet growth");
        lexAlphabet.stopGrowth();
        ntAlphabet.stopGrowth();
        
        log.info("Removing null elements");
        final int nullElement = ntAlphabet.lookupIndex("-NONE-");
        NaryTreeNodeFilter nullElementFilter = new NaryTreeNodeFilter() {
            @Override
            public boolean accept(NaryTreeNode node) {
                if (node.getParent() == nullElement) {
                    return false;
                } else if (!node.isLexical() && node.isLeaf()) {
                    return false;
                }
                return true;
            }
        };
        for (int i=0; i<naryTrees.size(); i++) {
            NaryTreeNode tree = naryTrees.get(i);
            tree.postOrderFilterNodes(nullElementFilter);
            naryTrees.set(i, tree);
        }
        
        log.info("Removing function tags");
        LambdaOne<NaryTreeNode> ftRemover = new LambdaOne<NaryTreeNode>() {
            private final Pattern functionTag = Pattern.compile("-[A-Z]+$");
            @Override
            public void call(NaryTreeNode node) {
                if (!node.isLexical()) {
                    Alphabet<String> alphabet = node.getAlphabet();
                    int p = node.getParent();
                    String pStr = alphabet.lookupObject(p);
                    // Remove the function tags.
                    pStr = functionTag.matcher(pStr).replaceAll("");
                    node.setParent(alphabet.lookupIndex(pStr));
                }
            }
        };
        for (int i=0; i<naryTrees.size(); i++) {
            NaryTreeNode tree = naryTrees.get(i);
            tree.postOrderTraversal(ftRemover);
            naryTrees.set(i, tree);
        }
        
        // TODO: Convert OOVs to OOV terminals in the grammar.
        
        // TODO: why binarize at all? We would only do this if we wanted to learn a grammar.
        log.info("Binarizing " + naryTrees.size() + " trees");
        BinaryTreebank binaryTrees = new BinaryTreebank();
        for (NaryTreeNode tree : naryTrees) {
            binaryTrees.add(tree.binarize(ntAlphabet));
        }
        naryTrees = null;
        
        log.info("Parsing " + binaryTrees.size() + " trees");
        BinaryTreebank parseTrees = new BinaryTreebank();
        for (BinaryTreeNode tree : binaryTrees) {
            int[] sent = tree.getSentence();
            Chart chart = CkyPcfgParser.parseSentence(sent, grammar);
            Pair<BinaryTreeNode, Double> pair = chart.getViterbiParse();
            parseTrees.add(pair.get1());
        }
        
        // Remove non-terminal refinements (e.g. NP_10 should be NP).
        log.info("Removing nonterminal refinements");        
        LambdaOne<NaryTreeNode> refineRemover = new LambdaOne<NaryTreeNode>() {
            private final Pattern refine = Pattern.compile("_\\d+$");
            @Override
            public void call(NaryTreeNode node) {
                if (!node.isLexical()) {
                    Alphabet<String> alphabet = node.getAlphabet();
                    int p = node.getParent();
                    String pStr = alphabet.lookupObject(p);
                    // Remove the function tags.
                    pStr = refine.matcher(pStr).replaceAll("");
                    node.setParent(alphabet.lookupIndex(pStr));
                }
            }
        };
        for (int i=0; i<naryTrees.size(); i++) {
            NaryTreeNode tree = naryTrees.get(i);
            tree.postOrderTraversal(refineRemover);
            naryTrees.set(i, tree);
        }
        
        if (treeFile != null) {
            log.info("Writing trees to file: " + treeFile);
            BufferedWriter writer = new BufferedWriter(new FileWriter(treeFile));
            for (BinaryTreeNode tree : parseTrees) {
                // TODO: Collapse binary trees back into n-ary trees.
                writer.write(tree.getAsPennTreebankString());
                writer.write("\n\n");
            }
            writer.close();
        }
        
        if (evalbDir != null) {
            Evalb evalb = new Evalb(evalbDir);
            //evalb.runEvalb(goldTrees, treeFile, "evalb.txt");
            // TODO: run evalb
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
