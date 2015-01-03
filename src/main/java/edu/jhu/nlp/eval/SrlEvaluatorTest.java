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
        predSrl.set(-1, 2, "eat.01");  // Pred
        // Arg (incorrect: missing 2, 0, agent.)
        predSrl.set(2, 3, "theme");    // Arg (incorrect label)
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
    public void testLabeledF1() {        
        SrlEvaluatorPrm prm = new SrlEvaluatorPrm();
        prm.labeled = true;
        prm.predictSense = false;
        SrlEvaluator eval = new SrlEvaluator(prm);
        eval.evaluate(predSents, goldSents, "dataset name");
        double ep = 2./4.;
        double er = 2./5.;
        assertEquals(ep, eval.getPrecision(), 1e-13);
        assertEquals(er, eval.getRecall(), 1e-13);
        assertEquals(2 * ep * er / (ep + er), eval.getF1(), 1e-13);
    }
    
    @Test
    public void testUnlabeledF1() {        
        SrlEvaluatorPrm prm = new SrlEvaluatorPrm();
        prm.labeled = false;
        prm.predictSense = false;
        SrlEvaluator eval = new SrlEvaluator(prm);
        eval.evaluate(predSents, goldSents, "dataset name");
        double ep = 3./4.;
        double er = 3./5.;
        assertEquals(ep, eval.getPrecision(), 1e-13);
        assertEquals(er, eval.getRecall(), 1e-13);
        assertEquals(2 * ep * er / (ep + er), eval.getF1(), 1e-13);
    }

}
