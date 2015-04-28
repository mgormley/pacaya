package edu.jhu.pacaya.parse.cky;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.pacaya.nlp.data.Sentence;
import edu.jhu.pacaya.nlp.data.SentenceCollection;
import edu.jhu.pacaya.parse.cky.CkyPcfgParser.CkyPcfgParserPrm;
import edu.jhu.pacaya.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.pacaya.parse.cky.chart.Chart;
import edu.jhu.pacaya.parse.cky.chart.Chart.ChartCellType;
import edu.jhu.pacaya.parse.cky.data.BinaryTree;
import edu.jhu.prim.tuple.Pair;

public class CkyPcfgParserTest {

    public static final String r0GrammarFile = "/Users/mgormley/research/parsing/data/grammars/eng.R0.gr.gz";
    
    @Test
    public void testSimpleSentence1() throws IOException {        
        testSimpleSentence1Helper(LoopOrder.LEFT_CHILD, ChartCellType.FULL);
        testSimpleSentence1Helper(LoopOrder.RIGHT_CHILD, ChartCellType.FULL);
        testSimpleSentence1Helper(LoopOrder.CARTESIAN_PRODUCT, ChartCellType.FULL);
        testSimpleSentence1Helper(LoopOrder.LEFT_CHILD, ChartCellType.FULL_BREAK_TIES);
        testSimpleSentence1Helper(LoopOrder.RIGHT_CHILD, ChartCellType.FULL_BREAK_TIES);
        testSimpleSentence1Helper(LoopOrder.CARTESIAN_PRODUCT, ChartCellType.FULL_BREAK_TIES);
        testSimpleSentence1Helper(LoopOrder.LEFT_CHILD, ChartCellType.SINGLE_HASH);
        testSimpleSentence1Helper(LoopOrder.RIGHT_CHILD, ChartCellType.SINGLE_HASH);
        testSimpleSentence1Helper(LoopOrder.CARTESIAN_PRODUCT, ChartCellType.SINGLE_HASH);
        testSimpleSentence1Helper(LoopOrder.LEFT_CHILD, ChartCellType.SINGLE_HASH_BREAK_TIES);
        testSimpleSentence1Helper(LoopOrder.RIGHT_CHILD, ChartCellType.SINGLE_HASH_BREAK_TIES);
        testSimpleSentence1Helper(LoopOrder.CARTESIAN_PRODUCT, ChartCellType.SINGLE_HASH_BREAK_TIES);
        testSimpleSentence1Helper(LoopOrder.LEFT_CHILD, ChartCellType.DOUBLE_HASH);
        testSimpleSentence1Helper(LoopOrder.RIGHT_CHILD, ChartCellType.DOUBLE_HASH);
        testSimpleSentence1Helper(LoopOrder.CARTESIAN_PRODUCT, ChartCellType.DOUBLE_HASH);
    }

    private void testSimpleSentence1Helper(LoopOrder loopOrder, ChartCellType cellType) throws IOException {
        // time flies like an arrow.
        CnfGrammarReader builder = new CnfGrammarReader();
        builder.loadFromResource(CnfGrammarReaderTest.timeFliesGrammarResource);
        
        CnfGrammar grammar = builder.getGrammar(loopOrder);
                
        Pair<BinaryTree, Double> pair = parseSentence("time flies like an arrow", grammar, loopOrder, cellType);
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
        testSimpleSentence2Helper(LoopOrder.LEFT_CHILD, ChartCellType.FULL);
        testSimpleSentence2Helper(LoopOrder.RIGHT_CHILD, ChartCellType.FULL);
        testSimpleSentence2Helper(LoopOrder.CARTESIAN_PRODUCT, ChartCellType.FULL);
        testSimpleSentence2Helper(LoopOrder.LEFT_CHILD, ChartCellType.FULL_BREAK_TIES);
        testSimpleSentence2Helper(LoopOrder.RIGHT_CHILD, ChartCellType.FULL_BREAK_TIES);
        testSimpleSentence2Helper(LoopOrder.CARTESIAN_PRODUCT, ChartCellType.FULL_BREAK_TIES);
        testSimpleSentence2Helper(LoopOrder.LEFT_CHILD, ChartCellType.SINGLE_HASH);
        testSimpleSentence2Helper(LoopOrder.RIGHT_CHILD, ChartCellType.SINGLE_HASH);
        testSimpleSentence2Helper(LoopOrder.CARTESIAN_PRODUCT, ChartCellType.SINGLE_HASH);
        testSimpleSentence2Helper(LoopOrder.LEFT_CHILD, ChartCellType.SINGLE_HASH_BREAK_TIES);
        testSimpleSentence2Helper(LoopOrder.RIGHT_CHILD, ChartCellType.SINGLE_HASH_BREAK_TIES);
        testSimpleSentence2Helper(LoopOrder.CARTESIAN_PRODUCT, ChartCellType.SINGLE_HASH_BREAK_TIES);
        testSimpleSentence2Helper(LoopOrder.LEFT_CHILD, ChartCellType.DOUBLE_HASH);
        testSimpleSentence2Helper(LoopOrder.RIGHT_CHILD, ChartCellType.DOUBLE_HASH);
        testSimpleSentence2Helper(LoopOrder.CARTESIAN_PRODUCT, ChartCellType.DOUBLE_HASH);
    }

    private void testSimpleSentence2Helper(LoopOrder loopOrder, ChartCellType cellType) throws IOException {
        // an arrow flies like time
        CnfGrammarReader builder = new CnfGrammarReader();
        builder.loadFromResource(CnfGrammarReaderTest.timeFliesGrammarResource);
        
        CnfGrammar grammar = builder.getGrammar(loopOrder);
                
        Pair<BinaryTree, Double> pair = parseSentence("an arrow flies like time", grammar, loopOrder, cellType);
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
    public void testMockScorer() throws IOException {
        LoopOrder loopOrder = LoopOrder.LEFT_CHILD;
        ChartCellType cellType =  ChartCellType.FULL;
        // time flies like an arrow.
        CnfGrammarReader builder = new CnfGrammarReader();
        builder.loadFromResource(CnfGrammarReaderTest.timeFliesGrammarResource);
        
        CnfGrammar grammar = builder.getGrammar(loopOrder);                

        String sentStr = "time flies like an arrow";
        SentenceCollection sentences = new SentenceCollection(grammar.getLexAlphabet());
        sentences.addSentenceFromString(sentStr);
        Sentence sentence = sentences.get(0);
        CkyPcfgParserPrm prm = new CkyPcfgParserPrm();
        prm.loopOrder = loopOrder;
        prm.cellType = cellType;
        prm.cacheChart = true;
        prm.scorer = new Scorer() {
            @Override
            public double score(Rule r, int start, int mid, int end) {
                // Return the score as the rule score minus 1.
                return r.getScore() - 1;
            }
        };
        Chart chart = new CkyPcfgParser(prm).parseSentence(sentence, grammar);
        Pair<BinaryTree, Double> pair = chart.getViterbiParse();
        BinaryTree tree = pair.get1();
        double logProb = pair.get2();
        
        System.out.println(logProb);
        System.out.println(tree);
        System.out.println(tree.getAsPennTreebankString());
        String goldTree = "((S (NP (N time)) (VP (V flies) (PP (IN like) (NP (DT an) (N arrow))))))";
        String treeStr = tree.getAsPennTreebankString().replaceAll("\\s+", " ");
        Assert.assertEquals(goldTree, treeStr);
        // -11.0 comes from the rules, and -10.0 from subtracting off 1.0 per rule.
        Assert.assertEquals(-11.0 - 10.0, logProb, 1e-13);
    }
    
    //TODO: Remove reliance on hard-coded paths: @Test
    public void testLargeGrammar() throws IOException {
        LoopOrder loopOrder = LoopOrder.LEFT_CHILD;
        ChartCellType cellType = ChartCellType.FULL;
        
        CnfGrammarReader builder = new CnfGrammarReader();
        builder.loadFromFile(r0GrammarFile);
        
        CnfGrammar grammar = builder.getGrammar(loopOrder);
                
        Pair<BinaryTree, Double> pair = parseSentence("time flies like an arrow", grammar, loopOrder, cellType);
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
    
    //TODO: Remove reliance on hard-coded paths: @Test
    public void testConrailOnR0() throws IOException {
        LoopOrder loopOrder = LoopOrder.LEFT_CHILD;
        ChartCellType cellType = ChartCellType.FULL;
        
        CnfGrammarReader builder = new CnfGrammarReader();
        builder.loadFromFile(r0GrammarFile);
        
        CnfGrammar grammar = builder.getGrammar(loopOrder);
                
        Pair<BinaryTree, Double> pair = parseSentence("Conrail", grammar, loopOrder, cellType);
        BinaryTree tree = pair.get1();
        double logProb = pair.get2();
        
        System.out.println(logProb);
        System.out.println(tree);
        System.out.println(tree.getAsPennTreebankString());
        String goldTree = "((ROOT (NP (NNP Conrail))))";
        String treeStr = tree.getAsPennTreebankString().replaceAll("\\s+", " ");
        System.out.println(treeStr);
        Assert.assertEquals(goldTree, treeStr);
        Assert.assertEquals(-16.8401, logProb, 1e-2);
    }

    public void testPetrovGrammar() throws IOException {
        LoopOrder loopOrder = LoopOrder.LEFT_CHILD;
        ChartCellType cellType = ChartCellType.FULL;
        
        // time flies like an arrow.
        CnfGrammarReader builder = new CnfGrammarReader();
        builder.loadFromFile("/Users/mgormley/research/parsing/data/grammars/eng.sm6.gr.gz");
        
        CnfGrammar grammar = builder.getGrammar(loopOrder);
                
        Pair<BinaryTree, Double> pair = parseSentence("Papa ate the beans with a knife .", grammar, loopOrder, cellType);
        BinaryTree tree = pair.get1();
        double logProb = pair.get2();
        
        System.out.println(logProb);
        System.out.println(tree);
        System.out.println(tree.getAsPennTreebankString());
        String treeStr = tree.getAsPennTreebankString().replaceAll("\\s+", " ");
        System.out.println(treeStr);
    }
    
    // TODO: we should parse with each method and check that we get the same solution.
    public static Pair<BinaryTree, Double> parseSentence(String sentStr, CnfGrammar grammar, LoopOrder loopOrder, ChartCellType cellType) {
        SentenceCollection sentences = new SentenceCollection(grammar.getLexAlphabet());
        sentences.addSentenceFromString(sentStr);
        Sentence sentence = sentences.get(0);
        CkyPcfgParserPrm prm = new CkyPcfgParserPrm();
        prm.loopOrder = loopOrder;
        prm.cellType = cellType;
        prm.cacheChart = true;
        Chart chart = new CkyPcfgParser(prm).parseSentence(sentence, grammar);
        return chart.getViterbiParse();
    }
}
