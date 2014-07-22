package edu.jhu.featurize;

import java.util.ArrayList;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.featurize.SentFeatureExtractorWithTemplates.SentFeatureExtractorWithTemplatesPrm;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.util.collections.Lists;

public class SentFeatureExtractorWithTemplatesTest {

    //@Test
    public void testTemplates() {
        CoNLL09Sentence sent = SentFeatureExtractorTest.getSpanishConll09Sentence2();
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        csPrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        AnnoSentence simpleSent = sent.toAnnoSentence(csPrm.useGoldSyntax);
        cs.init(Lists.getList(simpleSent));
        ArrayList<String> allFeats = new ArrayList<String>();
        SentFeatureExtractorWithTemplatesPrm fePrm = new SentFeatureExtractorWithTemplatesPrm();
        fePrm.withSupervision = false;

        fePrm.formFeats = true;
        fePrm.lemmaFeats = true;
        fePrm.tagFeats = true;
        fePrm.morphFeats = true;
        fePrm.deprelFeats = true;
        fePrm.childrenFeats = true;
        fePrm.pathFeats = true;
        fePrm.syntacticConnectionFeats = true;

        SentFeatureExtractorWithTemplates fe = new SentFeatureExtractorWithTemplates(fePrm, simpleSent, cs);
        //allFeats = new ArrayList<String>();
        // using "es" and "hicieron"...
        fe.addTemplatePairFeatures(1, 5, allFeats);
        /*for (int i = 0; i < sent.size(); i++) {
            for (int j = 0; j < sent.size(); j++) {
                ArrayList<String> pairFeatures = fe.addTemplatePairFeatures(i, j);
                allFeats.addAll(pairFeatures);
            }
        }*/
        // 
        //        for (String f : allFeats) {
        //            System.out.println(f);
        //        }
    }    

}
