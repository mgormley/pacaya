package edu.jhu.parse.relax;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.data.DepTreebank;
import edu.jhu.data.SentenceCollection;
import edu.jhu.model.Model;
import edu.jhu.model.dmv.DmvModelFactory;
import edu.jhu.model.dmv.RandomDmvModelFactory;
import edu.jhu.parse.relax.RelaxedParserWrapper.RelaxedDepParserWrapperPrm;
import edu.jhu.train.dmv.DmvTrainCorpus;
import edu.jhu.util.Prng;


public class RelaxedParserWrapperTest {

    @Before
    public void setUp() {
        Prng.seed(Prng.DEFAULT_SEED);
    }
    
    @Test
    public void testProjection() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("cat ate hat");
        sentences.addSentenceFromString("mouse cat ate");        
        DmvModelFactory modelFactory = new RandomDmvModelFactory(0.1);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());

        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);
        
        RelaxedParserWrapper parser = new RelaxedParserWrapper(new RelaxedDepParserWrapperPrm());
        DepTreebank trees = parser.getViterbiParse(corpus, model);
        System.out.println(trees);
        double score = parser.getLastParseWeight();
        System.out.println(score);
        Assert.assertEquals(-27, score, 1);
        // TODO: add assertions.
    }
    
}
