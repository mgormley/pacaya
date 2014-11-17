package edu.jhu.parse.cky;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.nlp.data.Sentence;
import edu.jhu.parse.cky.CkyPcfgParser.CkyPcfgParserPrm;
import edu.jhu.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.parse.cky.chart.Chart;
import edu.jhu.parse.cky.chart.Chart.ChartCellType;
import edu.jhu.parse.cky.chart.Chart.ParseType;
import edu.jhu.parse.cky.data.BinaryTree;
import edu.jhu.parse.cky.data.BinaryTreebank;
import edu.jhu.parse.cky.data.NaryTree;
import edu.jhu.parse.cky.data.NaryTree.NaryTreeNodeFilter;
import edu.jhu.parse.cky.data.NaryTreebank;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.Lambda.FnO1ToVoid;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prng;
import edu.jhu.util.Timer;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;
import edu.jhu.util.files.Files;

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
    
    // Parsing.
    @Opt(hasArg = true, description = "The parsing loop order for processing binary rules.")
    public static LoopOrder loopOrder = LoopOrder.LEFT_CHILD;
    @Opt(hasArg = true, description = "The parse chart cell type.")
    public static ChartCellType cellType = ChartCellType.FULL;
    
    public void run() throws IOException {
        log.info("Reading grammar from file");
        CnfGrammarReader builder = new CnfGrammarReader();
        builder.loadFromFile(grammar);        
        CnfGrammar grammar = builder.getGrammar(loopOrder);

        log.info("# Nonterminals in grammar: " + grammar.getNumNonTerminals());
        log.info("# Lexical types in grammar: " + grammar.getNumLexicalTypes());
        
        log.info("Reading trees from file: " + train);
        NaryTreebank naryTrees = readPtbTrees();
        
        log.info("Removing null elements");
        removeNullElements(naryTrees);
        
        log.info("Removing function tags");
        removeFunctionTagsAndTraces(naryTrees);

        // We can't stop the alphabet growth for the Berkeley grammar because
        // we will end up removing the non-terminal refinements at the end.
        //        log.info("Stopping alphabet growth");
        //        lexAlphabet.stopGrowth();
        //        ntAlphabet.stopGrowth();

        log.info("Converting OOVs to Berkeley OOV-class signatures used in the grammar.");
        useSignaturesForUnknownWords(naryTrees, grammar);

        naryTrees.intern();
        
        if (treeFile != null) {
            log.info("Writing (munged) trees to file: " + treeFile);
            naryTrees.writeTreesInOneLineFormat(treeFile);
            naryTrees.writeSentencesInOneLineFormat(treeFile + ".sent");
        }

        log.info("Parsing " + naryTrees.size() + " trees");
        BinaryTreebank binaryParses = new BinaryTreebank();
        Timer timer = new Timer();
        CkyPcfgParserPrm prm = new CkyPcfgParserPrm();
        prm.loopOrder = loopOrder;
        prm.cellType = cellType;
        prm.cacheChart = true;
        prm.parseType = ParseType.VITERBI;
        CkyPcfgParser parser = new CkyPcfgParser(prm);
        for (NaryTree tree : naryTrees) {            
            Sentence sent = tree.getSentence(grammar.getLexAlphabet());
            timer.start();
            Chart chart = parser.parseSentence(sent, grammar);
            timer.stop();
            Pair<BinaryTree, Double> pair = chart.getViterbiParse();
            BinaryTree parse = pair.get1();
            if (parse == null) {
                log.warn("Unable to parse sentence: " + sent);
            } else {
                parse.intern();
            }
            binaryParses.add(parse);
            log.debug("Avg seconds per parse: " + timer.avgSec());
        }

        // Remove non-terminal refinements (e.g. NP_10 should be NP).
        log.info("Removing nonterminal refinements");        
        removeRefinements(binaryParses);
        
        log.info("Collapsing binary parses back into n-ary parses");
        NaryTreebank naryParses = binaryParses.collapseToNary();

        if (parseFile != null) {
            log.info("Writing parses to file: " + parseFile);
            naryParses.writeTreesInOneLineFormat(parseFile);
        }
        
        if (evalbDir != null) {
            Evalb evalb = new Evalb(evalbDir);
            evalb.runEvalb(treeFile, parseFile, new File("evalb.txt"));
        }
    }

    private void removeRefinements(BinaryTreebank binaryParses) {
        FnO1ToVoid<BinaryTree> refineRemover = new FnO1ToVoid<BinaryTree>() {
            @Override
            public void call(BinaryTree node) {
                if (!node.isLexical()) {
                    String tag = node.getSymbol();
                    // Remove the function tags.
                    tag = GrammarConstants.removeTagRefinements(tag);
                    node.setSymbol(tag);
                }
            }
        };
        for (int i=0; i<binaryParses.size(); i++) {
            BinaryTree tree = binaryParses.get(i);
            tree.postOrderTraversal(refineRemover);
            binaryParses.set(i, tree);
        }
    }

    private void useSignaturesForUnknownWords(NaryTreebank naryTrees,
            final CnfGrammar grammar) {
        FnO1ToVoid<NaryTree> ftRemover = new FnO1ToVoid<NaryTree>() {
            private final Alphabet<String> emptySet = Alphabet.getEmptyStoppedAlphabet();
            @Override
            public void call(NaryTree node) {
                if (node.isLexical()) {
                    String word = node.getSymbol();
                    if (grammar.isUnknownWord(word)) {
                        // Replace unknown words with their signature.
                        word = GrammarConstants.getSignature(word, node.getStart(), emptySet);
                    }
                    node.setSymbol(word);
                }
            }
        };
        for (int i=0; i<naryTrees.size(); i++) {
            NaryTree tree = naryTrees.get(i);
            tree.postOrderTraversal(ftRemover);
            naryTrees.set(i, tree);
        }
    }
    
    private void removeFunctionTagsAndTraces(NaryTreebank naryTrees) {
        FnO1ToVoid<NaryTree> ftRemover = new FnO1ToVoid<NaryTree>() {
            @Override
            public void call(NaryTree node) {
                if (!node.isLexical()) {
                    String tag = node.getSymbol();
                    // Remove the function tags.
                    tag = GrammarConstants.removeFunctionTag(tag);
                    // Remove the traces.
                    tag = GrammarConstants.removeTagTrace(tag);
                    node.setSymbol(tag);
                }
            }
        };
        for (int i=0; i<naryTrees.size(); i++) {
            NaryTree tree = naryTrees.get(i);
            tree.postOrderTraversal(ftRemover);
            naryTrees.set(i, tree);
        }
    }

    /**
     * Removes null elements and any empty parents.
     * @param ntAlphabet The alphabet to 
     * @param naryTrees
     */
    private void removeNullElements(NaryTreebank naryTrees) {
        NaryTreeNodeFilter nullElementFilter = new NaryTreeNodeFilter() {
            @Override
            public boolean accept(NaryTree node) {
                if (node.getSymbol().equals(GrammarConstants.getNullElementTag())) {
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
    }

    private NaryTreebank readPtbTrees() throws FileNotFoundException,
            IOException {
        NaryTreebank naryTrees = new NaryTreebank();
        List<File> mrgFiles = Files.getMatchingFiles(train, ".*\\.mrg");
        for (File mrgFile : mrgFiles) {
            BufferedReader reader = new BufferedReader(new FileReader(mrgFile));
            NaryTreebank tmpTrees = NaryTreebank.readTreesInPtbFormat(reader);
            for (NaryTree tree : tmpTrees) {
                if (tree.getWords().size() <= maxSentenceLength) {
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
        return naryTrees;
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
