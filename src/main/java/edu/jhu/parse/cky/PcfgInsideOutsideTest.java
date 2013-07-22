package edu.jhu.parse.cky;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.SentenceCollection;
import edu.jhu.data.Tag;
import edu.jhu.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.parse.cky.PcfgInsideOutside.PcfgIoChart;
import edu.jhu.parse.cky.PcfgInsideOutside.PcfgInsideOutsidePrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Utilities;

public class PcfgInsideOutsideTest {
    
    public static final String pizzaAnochoviesGrammarResource = "/edu/jhu/parse/cky/pizza-anchovies.gr";

    @Test
    public void testSimpleSentence1() throws IOException {
        testSimpleSentence1Helper(LoopOrder.LEFT_CHILD);
        testSimpleSentence1Helper(LoopOrder.RIGHT_CHILD);
        testSimpleSentence1Helper(LoopOrder.CARTESIAN_PRODUCT);
    }

    private void testSimpleSentence1Helper(LoopOrder loopOrder) throws IOException {
        // time flies like an arrow.
        CnfGrammarReader builder = new CnfGrammarReader();
        builder.loadFromResource(CnfGrammarReaderTest.timeFliesGrammarResource);
        CnfGrammar grammar = builder.getGrammar(loopOrder);
                
        Sentence sent = getSentenceFromString("time flies like an arrow", grammar.getLexAlphabet());
        PcfgIoChart chart = parseSentence(sent , grammar, loopOrder);
        
        // There are three valid trees with weights -11, -12, -15.
        double sum = Utilities.logAdd(-11, -12);
        sum = Utilities.logAdd(sum, -15);
        assertEquals(-10.673, sum, 1e-3);
        assertEquals(-10.673, chart.getLogInsideScore(grammar.getRootSymbol(), 0, sent.size()), 1e-3);
        assertEquals(0, chart.getLogOutsideScore(grammar.getRootSymbol(), 0, sent.size()), 1e-3);

        // Each of the three trees uses this NP from 3 to 5. So the outside
        // score should be the sums of those trees minus 3 from each of their
        // weights, since 3 is the weight of this NP.
        sum = Utilities.logAdd(-8, -9);
        sum = Utilities.logAdd(sum, -12);
        assertEquals(sum, chart.getLogOutsideScore(grammar.getNtAlphabet().lookupIndex(new Tag("NP")), 3, 5), 1e-13);
        
        System.out.println("");
        int numNonInfs = 0;
        for (int width = 1; width <= sent.size(); width++) {
            for (int start = 0; start <= sent.size() - width; start++) {                                
                int end = start + width;
                
                for (int nt = 0; nt < grammar.getNumNonTerminals(); nt++) {
                    String msg = String.format("start=%d end=%d nt=%s", start, end, grammar.getNtAlphabet().lookupObject(nt));
                    msg += " is: " + chart.getLogInsideScore(nt, start, end) + " os: " + chart.getLogOutsideScore(nt, start, end);
                    double product = chart.getLogInsideScore(nt, start, end) + chart.getLogOutsideScore(nt, start, end);
                                        
                    if (product != Double.NEGATIVE_INFINITY) {
                        System.out.println(msg);
                        numNonInfs++;
                    }
                }
            }
        }
        assertEquals(18, numNonInfs);
    }

    @Test
    public void testSimpleSentence2() throws IOException {
        testSimpleSentence2Helper(LoopOrder.LEFT_CHILD);
        testSimpleSentence2Helper(LoopOrder.RIGHT_CHILD);
        testSimpleSentence2Helper(LoopOrder.CARTESIAN_PRODUCT);
    }

    private void testSimpleSentence2Helper(LoopOrder loopOrder) throws IOException {
        CnfGrammarReader builder = new CnfGrammarReader();
        builder.loadFromResource(pizzaAnochoviesGrammarResource);
        CnfGrammar grammar = builder.getGrammar(loopOrder);
                
        Sentence sent = getSentenceFromString("she eats pizza without anchovies", grammar.getLexAlphabet());
        PcfgIoChart chart = parseSentence(sent , grammar, loopOrder);

        // There is only one parse of this sentence, so all the expected
        // inside(A, i, j) * outside(A, i, j) should equal -9.
        assertEquals(-9.0, chart.getLogInsideScore(grammar.getRootSymbol(), 0, sent.size()), 1e-1);
        assertEquals(0, chart.getLogOutsideScore(grammar.getRootSymbol(), 0, sent.size()), 1e-1);
        
        System.out.println("");
        int numNonInfs = 0;
        for (int width = 1; width <= sent.size(); width++) {
            for (int start = 0; start <= sent.size() - width; start++) {                                
                int end = start + width;
                
                for (int nt = 0; nt < grammar.getNumNonTerminals(); nt++) {
                    String msg = String.format("start=%d end=%d nt=%s", start, end, grammar.getNtAlphabet().lookupObject(nt));
                    msg += " is: " + chart.getLogInsideScore(nt, start, end) + " os: " + chart.getLogOutsideScore(nt, start, end);
                    double product = chart.getLogInsideScore(nt, start, end) + chart.getLogOutsideScore(nt, start, end);
               
                    if (product != Double.NEGATIVE_INFINITY) {
                        System.out.println(msg);
                        assertEquals(msg, -9.0, product, 1e-13);
                        assertEquals(msg, -9.0, chart.getLogSumOfPotentials(nt, start, end), 1e-13);
                        numNonInfs++;
                    }
                }
            }
        }
        assertEquals(9, numNonInfs);
    }

    // TODO: we should parse with each method and check that we get the same solution.
    public static PcfgIoChart parseSentence(Sentence sentence, CnfGrammar grammar, LoopOrder loopOrder) {
        PcfgInsideOutsidePrm prm = new PcfgInsideOutsidePrm();
        prm.loopOrder = loopOrder;
        PcfgIoChart charts = new PcfgInsideOutside(prm).runInsideOutside(sentence, grammar);
        return charts;
    }

    private static Sentence getSentenceFromString(String sentStr,
            Alphabet<Label> alphabet) {
        SentenceCollection sentences = new SentenceCollection(alphabet);
        sentences.addSentenceFromString(sentStr);
        Sentence sentence = sentences.get(0);
        return sentence;
    }
    
}
