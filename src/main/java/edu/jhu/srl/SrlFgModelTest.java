package edu.jhu.srl;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.junit.Test;

import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.Feature;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.util.Utilities;

public class SrlFgModelTest {

    @Test
    public void testIsSerializable() throws IOException {
        try {
            CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
            CorpusStatistics cs = new CorpusStatistics(csPrm);
            // TODO: cs.init(sents);
            
            // Just test that no exception is thrown.
            SrlFgModel model = new SrlFgModel(Utilities.getList(new Feature("asdf")), cs);
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
