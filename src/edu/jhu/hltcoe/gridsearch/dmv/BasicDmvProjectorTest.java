package edu.jhu.hltcoe.gridsearch.dmv;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.dmv.BasicDmvProjector.DmvProjectorPrm;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.RandomDmvModelFactory;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.relax.LpDmvRelaxedParser;
import edu.jhu.hltcoe.parse.relax.LpDmvRelaxedParser.LpDmvRelaxedParserPrm;
import edu.jhu.hltcoe.parse.relax.LpDmvRelaxedParserTest;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Prng;

public class BasicDmvProjectorTest {

    private final static double lambda = 0.1;

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }

    @Test
    public void testProjection() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("cat ate hat");
        sentences.addSentenceFromString("mouse cat ate");        
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());

        LpDmvRelaxedParserPrm prm = new LpDmvRelaxedParserPrm();
        prm.parsePrm.formulation = IlpFormulation.FLOW_PROJ_LPRELAX;
        prm.tempDir = new File(".");

        LpDmvRelaxedParser parser = new LpDmvRelaxedParser(prm);
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);
        RelaxedDepTreebank trees = parser.getRelaxedParse(corpus, model);
        LpDmvRelaxedParserTest.checkFractionalTrees(trees);
        
        BasicDmvProjector projector = new BasicDmvProjector(new DmvProjectorPrm(), corpus);
        projector.getProjectedParses(trees);
    }
    
}
