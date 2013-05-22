package edu.jhu.hltcoe.parse.cky;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.hltcoe.parse.cky.Lambda.LambdaOne;
import edu.jhu.hltcoe.parse.cky.NaryTree.NaryTreeNodeFilter;
import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Timer;
import edu.jhu.hltcoe.util.Utilities;
import edu.jhu.hltcoe.util.cli.ArgParser;
import edu.jhu.hltcoe.util.cli.Opt;

public class RunCkyParser {
    
    private static final Logger log = Logger.getLogger(RunCkyParser.class);

    // Input data.
    @Opt(hasArg = true, required = true, description = "Penn Treebank training data directory")
    public static File train;
    @Opt(hasArg = true, description = "Maximum sentence length for train data.")
    public static int maxSentenceLength = Integer.MAX_VALUE;
    @Opt(hasArg = true, description = "Maximum number of sentences/trees to include in training data.")
    public static int maxNumSentences = Integer.MAX_VALUE; 
    
    // Grammar.
    @Opt(hasArg = true, required = true, description = "CNF grammar")
    public static File grammar;
    
    // Output files.
    @Opt(hasArg = true, description = "File to which the trees should be written")
    public static File treeFile = null;
    @Opt(hasArg = true, description = "File to which the parses should be written")
    public static File parseFile = null;

    // Evaluation.
    @Opt(hasArg = true, description = "Directory containing evalb")
    public static File evalbDir = null;

    @Opt(hasArg = true, description = "Pseudo random number generator seed")
    public static long seed = Prng.DEFAULT_SEED;
    
    public void run() throws IOException {
        Alphabet<String> lexAlphabet = new Alphabet<String>();
        Alphabet<String> ntAlphabet = new Alphabet<String>();

        log.info("Reading grammar from file");
        CnfGrammarBuilder builder = new CnfGrammarBuilder(lexAlphabet, ntAlphabet);
        builder.loadFromFile(grammar);        
        CnfGrammar grammar = builder.getGrammar();

        log.info("Nonterminal alphabet size: " + ntAlphabet.size());
        log.info("Lexical alphabet size: " + lexAlphabet.size());
        
        log.info("Restarting alphabet growth");
        lexAlphabet.startGrowth();
        ntAlphabet.startGrowth();
        
        log.info("Reading trees from file: " + train);
        NaryTreebank naryTrees = new NaryTreebank();
        List<File> mrgFiles = Utilities.getMatchingFiles(train, ".*\\.mrg");
        for (File mrgFile : mrgFiles) {
            BufferedReader reader = new BufferedReader(new FileReader(mrgFile));
            NaryTreebank tmpTrees = NaryTreebank.readTreesInPtbFormat(lexAlphabet, ntAlphabet, reader);
            for (NaryTree tree : tmpTrees) {
                if (tree.getSentence().length <= maxSentenceLength) {
                    naryTrees.add(tree);
                }
                if (naryTrees.size() >= maxNumSentences) {
                    break;
                }
            }
            if (naryTrees.size() >= maxNumSentences) {
                break;
            }
            reader.close();
        }

        log.info("Nonterminal alphabet size: " + ntAlphabet.size());
        log.info("Lexical alphabet size: " + lexAlphabet.size());
                
        log.info("Removing null elements");
        final int nullElement = ntAlphabet.lookupIndex("-NONE-");
        NaryTreeNodeFilter nullElementFilter = new NaryTreeNodeFilter() {
            @Override
            public boolean accept(NaryTree node) {
                if (node.getSymbol() == nullElement) {
                    return false;
                } else if (!node.isLexical() && node.isLeaf()) {
                    return false;
                }
                return true;
            }
        };
        for (int i=0; i<naryTrees.size(); i++) {
            NaryTree tree = naryTrees.get(i);
            tree.postOrderFilterNodes(nullElementFilter);
            naryTrees.set(i, tree);
        }
        
        log.info("Removing function tags");
        LambdaOne<NaryTree> ftRemover = new LambdaOne<NaryTree>() {
            private final Pattern functionTag = Pattern.compile("-[A-Z]+$");
            @Override
            public void call(NaryTree node) {
                if (!node.isLexical()) {
                    Alphabet<String> alphabet = node.getAlphabet();
                    int p = node.getSymbol();
                    String pStr = alphabet.lookupObject(p);
                    // Remove the function tags.
                    pStr = functionTag.matcher(pStr).replaceAll("");
                    node.setSymbol(pStr);
                }
            }
        };
        for (int i=0; i<naryTrees.size(); i++) {
            NaryTree tree = naryTrees.get(i);
            tree.postOrderTraversal(ftRemover);
            naryTrees.set(i, tree);
        }

        // We can't stop the alphabet growth for the Berkeley grammar because
        // we will end up removing the non-terminal refinements at the end.
        //        log.info("Stopping alphabet growth");
        //        lexAlphabet.stopGrowth();
        //        ntAlphabet.stopGrowth();

        if (treeFile != null) {
            log.info("Writing (munged) trees to file: " + treeFile);
            naryTrees.writeTreesInOneLineFormat(treeFile);
        }
        
        // TODO: Convert OOVs to OOV terminals in the grammar.
        
        // TODO: why binarize at all? We would only do this if we wanted to learn a grammar.
        log.info("Binarizing " + naryTrees.size() + " trees");
        BinaryTreebank binaryTrees = new BinaryTreebank();
        for (NaryTree tree : naryTrees) {
            binaryTrees.add(tree.leftBinarize(ntAlphabet));
        }
        naryTrees = null;
        
        log.info("Parsing " + binaryTrees.size() + " trees");
        BinaryTreebank binaryParses = new BinaryTreebank();
        Timer timer = new Timer();
        timer.start();
        for (BinaryTree tree : binaryTrees) {            
            int[] sent = tree.getSentence();
            Chart chart = CkyPcfgParser.parseSentence(sent, grammar);
            Pair<BinaryTree, Double> pair = chart.getViterbiParse();
            binaryParses.add(pair.get1());
            timer.split();
            log.debug("Avg seconds per parse: " + timer.avgSec());
        }
        timer.stop();
        
        // Remove non-terminal refinements (e.g. NP_10 should be NP).
        log.info("Removing nonterminal refinements");        
        LambdaOne<BinaryTree> refineRemover = new LambdaOne<BinaryTree>() {
            private final Pattern refine = Pattern.compile("_\\d+$");
            @Override
            public void call(BinaryTree node) {
                if (!node.isLexical()) {
                    Alphabet<String> alphabet = node.getAlphabet();
                    int p = node.getSymbol();
                    String pStr = alphabet.lookupObject(p);
                    // Remove the function tags.
                    pStr = refine.matcher(pStr).replaceAll("");
                    node.setSymbol(pStr);
                }
            }
        };
        for (int i=0; i<binaryParses.size(); i++) {
            BinaryTree tree = binaryParses.get(i);
            tree.postOrderTraversal(refineRemover);
            binaryParses.set(i, tree);
        }
        
        log.info("Collapsing binary trees back into n-ary trees");
        NaryTreebank naryParses = new NaryTreebank();
        for (BinaryTree tree : binaryParses) {
            naryParses.add(tree.collapseToNary(ntAlphabet));
        }
        binaryTrees = null;

        if (parseFile != null) {
            log.info("Writing parses to file: " + parseFile);
            naryParses.writeTreesInOneLineFormat(parseFile);
        }
        
        if (evalbDir != null) {
            Evalb evalb = new Evalb(evalbDir);
            evalb.runEvalb(treeFile, parseFile, new File("evalb.txt"));
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
