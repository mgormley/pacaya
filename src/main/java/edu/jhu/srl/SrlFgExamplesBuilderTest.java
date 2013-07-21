package edu.jhu.srl;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.gm.CrfTrainer;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FgExamples;
import edu.jhu.gm.FgModel;
import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.CrfTrainer.CrfTrainerPrm;
import edu.jhu.srl.SrlFgExampleBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.Alphabet;

/**
 * Unit tests for {@link SrlFgExamplesBuilderTest}.
 * @author mgormley
 */
public class SrlFgExamplesBuilderTest {

    @Test
    public void testGetData() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        
        System.out.println("Done reading.");
        Alphabet<Feature> alphabet = new Alphabet<Feature>();
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        
        prm.fgPrm.useProjDepTreeFactor = true;
        prm.fePrm.biasOnly = true;
        
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, alphabet);
        FgExamples data = builder.getData(sents);
        
        System.out.println("Num features: " + alphabet.size());
        FgModel model = new FgModel(alphabet);
        model = train(model, data);
    }
    
    private static FgModel train(FgModel model, FgExamples data) {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.logDomain = true;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        
        CrfTrainerPrm prm = new CrfTrainerPrm();
        prm.infFactory = bpPrm;
        
        // To run with SGD, uncomment these lines.
        //        SGDPrm optPrm = new SGDPrm();
        //        optPrm.iterations = 100;
        //        optPrm.lrAtMidpoint = 0.1;
        //        prm.maximizer = new SGD(optPrm);
        prm.regularizer = null;
        
        CrfTrainer trainer = new CrfTrainer(prm);
        return trainer.train(model, data);
    }
}
