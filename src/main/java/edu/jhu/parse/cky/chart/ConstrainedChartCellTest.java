package edu.jhu.parse.cky.chart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.data.DepTree;
import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.Word;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.data.conll.ValidParentsSentence;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.model.dmv.DmvModelFactory;
import edu.jhu.model.dmv.RandomDmvModelFactory;
import edu.jhu.parse.cky.chart.Chart.ChartCellType;
import edu.jhu.parse.dmv.DmvCkyParser;
import edu.jhu.parse.dmv.DmvCkyParser.DmvCkyParserPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.JUnitUtils;
import edu.jhu.util.Prng;
import edu.jhu.util.tuple.Pair;

public class ConstrainedChartCellTest {
    
    @Before
    public void setUp() {
        Prng.seed(12345);
    }
    
    @Test
    public void testUnconstrainedSentence() throws IOException {
        Alphabet<Label> alphabet = new Alphabet<Label>();        
        ValidParentsSentence sentence = getSimpleUnconstrainedSentence(alphabet);
        System.out.println(sentence);

        DmvModelFactory modelFactory = new RandomDmvModelFactory(0.1);
        DmvModel dmv = modelFactory.getInstance(alphabet);
        
        Pair<DepTree, Double> pair = parseSentence(sentence, dmv);
        DepTree tree = pair.get1();
        double logProb = pair.get2();
        
        System.out.println(logProb);
        System.out.println(tree);

        JUnitUtils.assertArrayEquals(new int[]{ 1,  2, -1, 2, 3 }, tree.getParents());
        Assert.assertEquals(-19.7, logProb, 1e-1);
    }

    public static ValidParentsSentence getSimpleUnconstrainedSentence(Alphabet<Label> alphabet) {
        ArrayList<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();
        tokens.add(getTok("time",  "_ vice"));
        tokens.add(getTok("flies", "_ pres"));
        tokens.add(getTok("like",  "_ says"));
        tokens.add(getTok("an",    "_ says"));
        tokens.add(getTok("arrow", "_ jump"));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
        ValidParentsSentence sentence = new ValidParentsSentence(sent, alphabet);
        
        // TODO: This is a hack to convert the TaggedWords (which is what Sentence creates by default) to Words which is what the Grammar expects.
        boolean[] validRoot = sentence.getValidRoot();
        boolean[][] validParents = sentence.getValidParents();
        ArrayList<Label> labels = new ArrayList<Label>();
        for (Label l : sentence) {
            labels.add(new Word(l.getLabel()));
        }
        sentence = new ValidParentsSentence(alphabet, labels, validRoot, validParents);
                
        return sentence;
    }

    @Test
    public void testConstrainedSentence() throws IOException {
        Alphabet<Label> alphabet = new Alphabet<Label>();        
        ValidParentsSentence sentence = getSimpleConstrainedSentence(alphabet);
        System.out.println(sentence);

        DmvModelFactory modelFactory = new RandomDmvModelFactory(0.1);
        DmvModel dmv = modelFactory.getInstance(alphabet);
        
        Pair<DepTree, Double> pair = parseSentence(sentence, dmv);
        DepTree tree = pair.get1();
        double logProb = pair.get2();
        
        System.out.println(logProb);
        System.out.println(tree);

        //JUnitUtils.assertArrayEquals(new int[]{ 2,  0, -1, 2, 2 }, tree.getParents());
        JUnitUtils.assertArrayEquals(new int[]{ -1,  0, 0, 2, 2 }, tree.getParents());
        Assert.assertEquals(-21.1, logProb, 1e-1);
    }

    public static ValidParentsSentence getSimpleConstrainedSentence(Alphabet<Label> alphabet) {
        ArrayList<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();
        tokens.add(getTok("time",  "Y vice _ _"));
        tokens.add(getTok("flies", "_ pres A1 _"));
        tokens.add(getTok("like",  "Y says A2 _"));
        tokens.add(getTok("an",    "_ says _ A2"));
        tokens.add(getTok("arrow", "_ jump _ A1"));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
        ValidParentsSentence sentence = new ValidParentsSentence(sent, alphabet);
        
        // TODO: This is a hack to convert the TaggedWords (which is what Sentence creates by default) to Words which is what the Grammar expects.
        boolean[] validRoot = sentence.getValidRoot();
        boolean[][] validParents = sentence.getValidParents();
        System.out.println(Arrays.deepToString(validParents));
        ArrayList<Label> labels = new ArrayList<Label>();
        for (Label l : sentence) {
            labels.add(new Word(l.getLabel()));
        }
        sentence = new ValidParentsSentence(alphabet, labels, validRoot, validParents);
                
        return sentence;
    }


    @Test
    public void testCachedChart() throws IOException {
        Alphabet<Label> alphabet = new Alphabet<Label>();        
        ValidParentsSentence sentence = getSimpleConstrainedSentence(alphabet);
        System.out.println(sentence);

        DmvModelFactory modelFactory = new RandomDmvModelFactory(0.1);
        DmvModel dmv = modelFactory.getInstance(alphabet);
        

        DmvCkyParserPrm prm = new DmvCkyParserPrm();
        prm.ckyPrm.cellType = ChartCellType.CONSTRAINED_FULL;
        prm.ckyPrm.cacheChart = true;
        DmvCkyParser parser = new DmvCkyParser(prm);

        DepTree tree;

        tree = parser.parse(sentence, dmv).get1();
        JUnitUtils.assertArrayEquals(new int[]{ -1,  0, 0, 2, 2 }, tree.getParents());
        
        tree = parser.parse(sentence, dmv).get1();
        JUnitUtils.assertArrayEquals(new int[]{ -1,  0, 0, 2, 2 }, tree.getParents());
    }

    public static CoNLL09Token getTok(String form, String fillpredPredApreds) {
        // Columns: ID FORM LEMMA PLEMMA POS PPOS FEAT PFEAT HEAD PHEAD DEPREL PDEPREL
        // FILLPRED PRED APREDs
        return new CoNLL09Token("0 " + form + " lemma plemma " + form + " ppos feat pfeat 0 0 deprel pdeprel " + fillpredPredApreds);
    }
    
    // TODO: we should parse with each method and check that we get the same solution.
    public static Pair<DepTree, Double> parseSentence(Sentence sentence, DmvModel dmv) {
        DmvCkyParserPrm prm = new DmvCkyParserPrm();
        prm.ckyPrm.cellType = ChartCellType.CONSTRAINED_FULL;
        prm.ckyPrm.cacheChart = false;
        return new DmvCkyParser(prm).parse(sentence, dmv);
    }
}
