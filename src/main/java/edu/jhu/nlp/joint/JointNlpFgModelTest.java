package edu.jhu.nlp.joint;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.simple.AnnoSentenceCollection;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.feat.ObsFeatureConjoinerTest;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.joint.JointNlpEncoder.JointNlpFeatureExtractorPrm;

public class JointNlpFgModelTest {

    @Test
    public void testIsSerializable() throws IOException {
        try {
            InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
            CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
            CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
            AnnoSentenceCollection sents = CoNLL09Sentence.toAnno(cr.readSents(1), csPrm.useGoldSyntax);
            CorpusStatistics cs = new CorpusStatistics(csPrm);
            cs.init(sents);
            
            FactorTemplateList fts = ObsFeatureConjoinerTest.getFtl();
            ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);
            ofc.init();
            
            // Just test that no exception is thrown.
            JointNlpFgModel model = new JointNlpFgModel(cs, ofc, new JointNlpFeatureExtractorPrm());
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
