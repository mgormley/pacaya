package edu.jhu.nlp.eval;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.nlp.data.DepGraph;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.eval.SrlEvaluator.SrlEvaluatorPrm;
import edu.jhu.util.collections.Lists;

public class SrlEvaluatorTest {

    AnnoSentenceCollection predSents;
    AnnoSentenceCollection goldSents;
    
    /**
     * Creates two SRL graphs (predicted and gold) and stores them in AnnoSentenceCollections.
     */
    @Before
    public void setUp() {
        predSents = new AnnoSentenceCollection();
        goldSents = new AnnoSentenceCollection();
        AnnoSentence pred = new AnnoSentence();
        AnnoSentence gold = new AnnoSentence();
        int n = 5;
        List<String> words = Lists.getList("cats", "like", "eating", "food");
        pred.setWords(words);
        gold.setWords(words);
        
        DepGraph predSrl = new DepGraph(n);
        DepGraph goldSrl = new DepGraph(n);
        
        predSrl.set(-1, 1, "like.01"); // Pred
        predSrl.set(1, 0, "agent");    // Arg
        predSrl.set(1, 1, "patient");  // Arg (incorrect position)
        predSrl.set(1, 3, "nonarg");   // Arg
        predSrl.set(-1, 2, "drink.01");// Pred (incorrect label)
                                       // Arg (incorrect: missing 2, 0, agent.)
        predSrl.set(2, 3, "theme");    // Arg (incorrect label)
        predSrl.set(-1, 0, "run.02");  // Pred (extra)
        pred.setSrlGraph(predSrl.toSrlGraph());
        
        goldSrl.set(-1, 1, "like.01"); // Pred
        goldSrl.set(1, 0, "agent");    // Arg
        goldSrl.set(1, 2, "patient");  // Arg
        goldSrl.set(1, 3, "nonarg");   // Arg
        goldSrl.set(-1, 2, "eat.01");  // Pred
        goldSrl.set(2, 0, "agent");    // Arg
        goldSrl.set(2, 3, "patient");  // Arg
        gold.setSrlGraph(goldSrl.toSrlGraph());
        
        System.out.println(pred.getSrlGraph());
        System.out.println(gold.getSrlGraph());
        
        predSents.add(pred);
        goldSents.add(gold);
    }

    @Test
    public void testZeros() {
        SrlEvaluatorPrm prm = new SrlEvaluatorPrm();
        prm.labeled = true;
        prm.evalSense = true;
        prm.evalPredicatePosition = true;
        SrlEvaluator eval = new SrlEvaluator(prm);
        eval.evaluate(new AnnoSentenceCollection(), new AnnoSentenceCollection(), "empty dataset");
        assertEquals(0.0, eval.getPrecision(), 1e-13);
        assertEquals(0.0, eval.getRecall(), 1e-13);
        assertEquals(0.0, eval.getF1(), 1e-13);
    }
    
    @Test
    public void testUnlabeled() {
        checkSrlPrecRecallF1(3, 4, 5, false, false, false, true);
    }
    
    @Test
    public void testLabeled() {     
        checkSrlPrecRecallF1(2, 4, 5, true, false, false, true);
    }
    
    @Test
    public void testUnlabeledSense() {
        checkSrlPrecRecallF1(5, 6, 7, false, true, false, true);
    }
    
    @Test
    public void testLabeledSense() {     
        checkSrlPrecRecallF1(3, 6, 7, true, true, false, true);
    }

    @Test
    public void testUnlabeledPosition() {
        checkSrlPrecRecallF1(5, 7, 7, false, false, true, true);
    }
    
    @Test
    public void testLabeledPosition() {     
        checkSrlPrecRecallF1(4, 7, 7, true, false, true, true);
    }

    @Test
    public void testUnlabeledSensePosition() {
        checkSrlPrecRecallF1(5, 7, 7, false, true, true, true);
    }
    
    @Test
    public void testLabeledSensePosition() {     
        checkSrlPrecRecallF1(3, 7, 7, true, true, true, true);
    }

    @Test
    public void testPositionNoRoles() {     
        checkSrlPrecRecallF1(2, 3, 2, false, false, true, false);
    }
    
    @Test
    public void testSensePositionNoRoles() {     
        checkSrlPrecRecallF1(1, 3, 2, true, true, true, false);
    }

    protected void checkSrlPrecRecallF1(int numCorrectPositives, int numPredictedPositives, int numTruePositives, 
            boolean labeled, boolean evalSense, boolean evalPredicatePosition, boolean evalRoles) {
        double ep = (double) numCorrectPositives / numPredictedPositives;
        double er = (double) numCorrectPositives / numTruePositives;
        SrlEvaluatorPrm prm = new SrlEvaluatorPrm();
        prm.labeled = labeled;
        prm.evalSense = evalSense;
        prm.evalPredicatePosition = evalPredicatePosition;
        prm.evalRoles = evalRoles;
        SrlEvaluator eval = new SrlEvaluator(prm);
        eval.evaluate(predSents, goldSents, "dataset name");
        assertEquals(ep, eval.getPrecision(), 1e-13);
        assertEquals(er, eval.getRecall(), 1e-13);
        assertEquals(2 * ep * er / (ep + er), eval.getF1(), 1e-13);
    }

}
