package edu.jhu.srl;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FgExamples;
import edu.jhu.srl.SrlFgExampleBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.Alphabet;

public class SrlFgExamplesBuilderTest {

    @Test
    public void testGetData() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(7);
        
        System.out.println("Done reading.");
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(new SrlFgExampleBuilderPrm(), new Alphabet<Feature>());
        FgExamples data = builder.getData(sents);
    }

}
