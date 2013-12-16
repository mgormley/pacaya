package edu.jhu.featurize;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceTest;
import edu.jhu.featurize.TemplateLanguage.EdgeProperty;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate3;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate4;
import edu.jhu.featurize.TemplateLanguage.ListModifier;
import edu.jhu.featurize.TemplateLanguage.OtherFeat;
import edu.jhu.featurize.TemplateLanguage.PositionList;
import edu.jhu.featurize.TemplateLanguage.TokProperty;
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
        int cidx = 3;
        ArrayList<Object> feats = new ArrayList<Object>();
        extr.addFeatures(tpls, pidx, cidx, feats);
        
        for (Object feat : feats) {
            System.out.println(feat);
        }
    }

    @Test
    public void testDeprelPathFeature() {
        FeatTemplate tpl = new FeatTemplate3(PositionList.PATH_P_C, TokProperty.DEPREL, EdgeProperty.DIR, ListModifier.SEQ);
        String expectedFeat = "PATH_P_C.DEPREL.DIR.SEQ_det_UP_subj_UP_v_DOWN_obj";
        getFeatAndAssertEquality(tpl, expectedFeat);
    }
    
    @Test
    public void testEdgrelPathFeature() {
        FeatTemplate tpl = new FeatTemplate3(PositionList.PATH_P_C, null, EdgeProperty.EDGEREL, ListModifier.SEQ);
        String expectedFeat = "PATH_P_C..EDGEREL.SEQ_det_subj_obj";
        getFeatAndAssertEquality(tpl, expectedFeat);
    }

    private void getFeatAndAssertEquality(FeatTemplate tpl, String expectedFeat) {
        SimpleAnnoSentence sent = CoNLL09Sentence.toSimpleAnnoSentence(SimpleAnnoSentenceTest.getDogConll09Sentence(), true);
        CorpusStatistics cs = new CorpusStatistics(new CorpusStatisticsPrm());
        cs.init(Lists.getList(sent));
        TemplateLanguageExtractor extr = new TemplateLanguageExtractor(sent, cs);  
        
        int pidx = 0;
        int cidx = 3;
        ArrayList<Object> feats = new ArrayList<Object>();
        extr.addFeatures(tpl, pidx, cidx, feats);

        for (Object feat : feats) {
            System.out.println(feat);
        }
        assertEquals(feats.size(), 1);
        assertEquals(expectedFeat, feats.get(0));
    }
    
    @Test
    public void testPathGramsFeature() {
        SimpleAnnoSentence sent = CoNLL09Sentence.toSimpleAnnoSentence(SimpleAnnoSentenceTest.getDogConll09Sentence(), true);
        CorpusStatistics cs = new CorpusStatistics(new CorpusStatisticsPrm());
        cs.init(Lists.getList(sent));
        TemplateLanguageExtractor extr = new TemplateLanguageExtractor(sent, cs);  
        
        FeatTemplate tpl = new FeatTemplate4(OtherFeat.PATH_GRAMS);
        int pidx = 0;
        int cidx = 3;
        ArrayList<Object> feats = new ArrayList<Object>();
        extr.addFeatures(tpl, pidx, cidx, feats);

        String[] expectedPathGrams = new String[] { "PATH_GRAMS_the", "PATH_GRAMS_Det", "PATH_GRAMS_dog",
                "PATH_GRAMS_N", "PATH_GRAMS_ate", "PATH_GRAMS_V", "PATH_GRAMS_food", "PATH_GRAMS_N",
                "PATH_GRAMS_the_dog", "PATH_GRAMS_Det_dog", "PATH_GRAMS_the_N", "PATH_GRAMS_Det_N",
                "PATH_GRAMS_dog_ate", "PATH_GRAMS_N_ate", "PATH_GRAMS_dog_V", "PATH_GRAMS_N_V", "PATH_GRAMS_ate_food",
                "PATH_GRAMS_V_food", "PATH_GRAMS_ate_N", "PATH_GRAMS_V_N", "PATH_GRAMS_the_dog_ate",
                "PATH_GRAMS_Det_dog_ate", "PATH_GRAMS_the_N_ate", "PATH_GRAMS_Det_N_ate", "PATH_GRAMS_the_dog_V",
                "PATH_GRAMS_Det_dog_V", "PATH_GRAMS_the_N_V", "PATH_GRAMS_Det_N_V", "PATH_GRAMS_dog_ate_food",
                "PATH_GRAMS_N_ate_food", "PATH_GRAMS_dog_V_food", "PATH_GRAMS_N_V_food", "PATH_GRAMS_dog_ate_N",
                "PATH_GRAMS_N_ate_N", "PATH_GRAMS_dog_V_N", "PATH_GRAMS_N_V_N", };
        for (Object feat : feats) {
            System.out.println(feat);
        }
        assertEquals(new HashSet<String>(Arrays.asList(expectedPathGrams)), new HashSet<Object>(feats));
    }
    
    public static void addFakeBrownClusters(SimpleAnnoSentence sent) {
        ArrayList<String> clusters = new ArrayList<String>();
        for (int i=0; i<sent.size(); i++) {
            clusters.add(FastMath.mod(i*7, 2) + "10101" + FastMath.mod(i*39, 2));
        }
        sent.setClusters(clusters);
    }

}
