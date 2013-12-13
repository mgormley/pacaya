package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceTest;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.util.collections.Lists;

public class TemplateLanguageExtractorTest {

    @Test
    public void testGetAllUnigrams() {
        SimpleAnnoSentence sent = CoNLL09Sentence.toSimpleAnnoSentence(SimpleAnnoSentenceTest.getDogConll09Sentence(), true);
        addFakeBrownClusters(sent);
        
        CorpusStatistics cs = new CorpusStatistics(new CorpusStatisticsPrm());
        cs.init(Lists.getList(sent));
        TemplateLanguageExtractor extr = new TemplateLanguageExtractor(sent, cs);  
        
        List<FeatTemplate> tpls = TemplateSets.getAllUnigramFeatureTemplates();
        int pidx = 0;
        int cidx = 2;
        ArrayList<Object> feats = new ArrayList<Object>();
        extr.addFeatures(tpls, pidx, cidx, feats);
        
        for (Object feat : feats) {
            System.out.println(feat);
        }
    }

    public static void addFakeBrownClusters(SimpleAnnoSentence sent) {
        ArrayList<String> clusters = new ArrayList<String>();
        for (int i=0; i<sent.size(); i++) {
            clusters.add(FastMath.mod(i*7, 2) + "10101" + FastMath.mod(i*39, 2));
        }
        sent.setClusters(clusters);
    }

}
