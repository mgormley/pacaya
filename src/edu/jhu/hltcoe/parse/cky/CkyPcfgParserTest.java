package edu.jhu.hltcoe.parse.cky;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.hltcoe.util.Pair;

public class CkyPcfgParserTest {

    public static final String r0GrammarFile = "/Users/mgormley/research/parsing/data/grammars/eng.R0.gr.gz";
    
    @Test
    public void testSimpleSentence1() throws IOException {
        // time flies like an arrow.
        CnfGrammarBuilder builder = new CnfGrammarBuilder();
        builder.loadFromResource(CnfGrammarBuilderTest.timeFliesGrammarResource);
        
        CnfGrammar grammar = builder.getGrammar();
                
        Pair<BinaryTree, Double> pair = parseSentence("time flies like an arrow", grammar);
        BinaryTree tree = pair.get1();
        double logProb = pair.get2();
        
        System.out.println(logProb);
        System.out.println(tree);
        System.out.println(tree.getAsPennTreebankString());
        String goldTree = "((S (NP (N time)) (VP (V flies) (PP (IN like) (NP (DT an) (N arrow))))))";
        String treeStr = tree.getAsPennTreebankString().replaceAll("\\s+", " ");
        Assert.assertEquals(goldTree, treeStr);
        Assert.assertEquals(-11.0, logProb, 1e-13);
    }
    
    @Test
    public void testSimpleSentence2() throws IOException {
        // an arrow flies like time
        CnfGrammarBuilder builder = new CnfGrammarBuilder();
        builder.loadFromResource(CnfGrammarBuilderTest.timeFliesGrammarResource);
        
        CnfGrammar grammar = builder.getGrammar();
                
        Pair<BinaryTree, Double> pair = parseSentence("an arrow flies like time", grammar);
        BinaryTree tree = pair.get1();
        double logProb = pair.get2();
        
        System.out.println(logProb);
        System.out.println(tree);
        System.out.println(tree.getAsPennTreebankString());
        String treeStr = tree.getAsPennTreebankString().replaceAll("\\s+", " ");
        System.out.println(treeStr);
        String goldTree = "((S (NP (DT an) (N arrow)) (VP (V flies) (PP (IN like) (NP (N time))))))";
        Assert.assertEquals(goldTree, treeStr);
        Assert.assertEquals(-11.0, logProb, 1e-13);
    }
    
    @Test
    public void testLargeGrammar() throws IOException {
        // time flies like an arrow.
        CnfGrammarBuilder builder = new CnfGrammarBuilder();
        builder.loadFromFile(r0GrammarFile);
        
        CnfGrammar grammar = builder.getGrammar();
                
        Pair<BinaryTree, Double> pair = parseSentence("time flies like an arrow", grammar);
        BinaryTree tree = pair.get1();
        double logProb = pair.get2();
        
        System.out.println(logProb);
        System.out.println(tree);
        System.out.println(tree.getAsPennTreebankString());
        String goldTree = "((ROOT (S (NP (NN time)) (VP (VBZ flies) (PP (IN like) (NP (DT an) (NN arrow)))))))";
        String treeStr = tree.getAsPennTreebankString().replaceAll("\\s+", " ");
        System.out.println(treeStr);
        Assert.assertEquals(goldTree, treeStr);
        Assert.assertEquals(-46.24, logProb, 1e-2);
    }
    
    public static Pair<BinaryTree, Double> parseSentence(String sentence, CnfGrammar grammar) {
        String[] tokens = sentence.split(" ");
        int[] sent = grammar.getLexAlphabet().lookupIndices(tokens);
        System.out.println(Arrays.toString(sent));
        Chart chart = CkyPcfgParser.parseSentence(sent, grammar);
        return chart.getViterbiParse();
    }
}
