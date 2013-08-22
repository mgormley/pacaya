package edu.jhu.srl;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FgModelTest;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.util.Utilities;

public class SrlFgModelTest {

    @Test
    public void testIsSerializable() throws IOException {
        try {
            InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
            CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
            List<CoNLL09Sentence> sents = cr.readSents(1);
            
            CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
            CorpusStatistics cs = new CorpusStatistics(csPrm);
            cs.init(sents);
            
            // Just test that no exception is thrown.
            SrlFgModel model = new SrlFgModel(FgModelTest.getFtl(), cs);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(model);
            out.close();
        } catch(java.io.NotSerializableException e) {
            e.printStackTrace();
            fail("FgModel is not serializable: " + e.getMessage());
        }
    }

}
