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
import edu.jhu.featurize.TemplateLanguage.BigramTemplate;
import edu.jhu.featurize.TemplateLanguage.EdgeProperty;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate1;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate2;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate3;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate4;
import edu.jhu.featurize.TemplateLanguage.ListModifier;
import edu.jhu.featurize.TemplateLanguage.OtherFeat;
import edu.jhu.featurize.TemplateLanguage.Position;
import edu.jhu.featurize.TemplateLanguage.PositionList;
import edu.jhu.featurize.TemplateLanguage.PositionModifier;
import edu.jhu.featurize.TemplateLanguage.TokPropList;
import edu.jhu.featurize.TemplateLanguage.TokProperty;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.util.collections.Lists;

/**
 * Tests for the template feature extractor.
 * @author mgormley
 */
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
    public void testParentPosition() {
        FeatTemplate tpl = new FeatTemplate1(Position.PARENT, PositionModifier.IDENTITY, TokProperty.WORD);
        String expectedFeat = "PARENT.IDENTITY.WORD_the";
        getFeatAndAssertEquality(tpl, expectedFeat);
    }

    @Test
    public void testChildPosition() {
        FeatTemplate tpl = new FeatTemplate1(Position.CHILD, PositionModifier.IDENTITY, TokProperty.WORD);
        String expectedFeat = "CHILD.IDENTITY.WORD_food";
        getFeatAndAssertEquality(tpl, expectedFeat);
    }
    
    @Test
    public void testPositionModifiers() {
        testPositionModifiersHelper(1, PositionModifier.IDENTITY, "Resultaban");
        testPositionModifiersHelper(1, PositionModifier.LMC, "_");
        testPositionModifiersHelper(1, PositionModifier.LNC, "_");
        testPositionModifiersHelper(1, PositionModifier.RMC, ".");
        testPositionModifiersHelper(1, PositionModifier.RNC, "baratos");
        //
        testPositionModifiersHelper(3, PositionModifier.LNS, "_");
        testPositionModifiersHelper(3, PositionModifier.RNS, "para");
        testPositionModifiersHelper(4, PositionModifier.LNS, "baratos");
        testPositionModifiersHelper(4, PositionModifier.RNS, ".");
        //
        testPositionModifiersHelper(5, PositionModifier.HEAD, "para");
        testPositionModifiersHelper(4, PositionModifier.BEFORE1, "baratos");
        testPositionModifiersHelper(4, PositionModifier.AFTER1, "ser");
        // 
        testPositionModifiersHelper(6, PositionModifier.LOW_SV, "ser");
        testPositionModifiersHelper(6, PositionModifier.HIGH_SV, "Resultaban");
        testPositionModifiersHelper(6, PositionModifier.LOW_SN, "BEGIN_NO_FORM");
        testPositionModifiersHelper(6, PositionModifier.HIGH_SN, "BEGIN_NO_FORM");
        testPositionModifiersHelper(1, PositionModifier.LOW_SV, "Resultaban");
        testPositionModifiersHelper(1, PositionModifier.HIGH_SV, "Resultaban");        
    }

    private void testPositionModifiersHelper(int pidx, PositionModifier mod, String expectedWord) {
        FeatTemplate tpl = new FeatTemplate1(Position.PARENT, mod, TokProperty.WORD);
        TemplateLanguageExtractor extr = getCoNLLSentenceExtractor1();        
        int cidx = -1;
        ArrayList<Object> feats = new ArrayList<Object>();
        extr.addFeatures(tpl, pidx, cidx, feats);
        for (Object feat : feats) {
            System.out.println(feat);
        }
        assertEquals(feats.size(), 1);
        assertEquals("PARENT."+mod.name()+".WORD_"+expectedWord, feats.get(0));
    }
    
    @Test
    public void testTokPropLists() {
        TemplateLanguageExtractor extr = getCoNLLSentenceExtractor1();
        List<String> feats = extr.getTokPropList(TokPropList.EACH_MORPHO, 3);
        String[] expected = new String[]{"postype=qualificative","gen=m","num=p"};
        assertEquals(new HashSet<String>(Arrays.asList(expected)), new HashSet<String>(feats));
    }

    @Test
    public void testTokPropListFeature() {
        FeatTemplate tpl = new FeatTemplate2(Position.PARENT, PositionModifier.IDENTITY, TokPropList.EACH_MORPHO);
        String[] expected = new String[] { "PARENT.IDENTITY.EACH_MORPHO_feat1", "PARENT.IDENTITY.EACH_MORPHO_feat2" };        
        getFeatsAndAssertEquality(tpl, expected);
    }
    
    @Test
    public void testTokProperties() {
        TemplateLanguageExtractor extr = getCoNLLSentenceExtractor1();
        assertEquals("baratos", extr.getTokProp(TokProperty.WORD, 3));
        assertEquals("barato", extr.getTokProp(TokProperty.LEMMA, 3));
        assertEquals("a", extr.getTokProp(TokProperty.POS, 3));
        assertEquals("postype=qualificative_gen=m_num=p", extr.getTokProp(TokProperty.MORPHO, 3));
        assertEquals("11010", extr.getTokProp(TokProperty.BC0, 3));
        assertEquals("1101011", extr.getTokProp(TokProperty.BC1, 3));
        assertEquals("cpred", extr.getTokProp(TokProperty.DEPREL, 3));
        assertEquals("UNK-LC-s", extr.getTokProp(TokProperty.UNK, 3));
        // 
        assertEquals("Resultaban", extr.getTokProp(TokProperty.WORD, 1));
        assertEquals("resultaban", extr.getTokProp(TokProperty.LC, 1));
        assertEquals("Resul", extr.getTokProp(TokProperty.CHPRE5, 1));
    }
    
    @Test
    public void testBosProperties() {
        TemplateLanguageExtractor extr = getCoNLLSentenceExtractor1();
        assertEquals("BEGIN_NO_FORM", extr.getTokProp(TokProperty.WORD, -1));
        assertEquals("BEGIN_NO_LEMMA", extr.getTokProp(TokProperty.LEMMA, -1));
        assertEquals("BEGIN_NO_POS", extr.getTokProp(TokProperty.POS, -1));
        assertEquals("NO_MORPH", extr.getTokProp(TokProperty.MORPHO, -1));
        assertEquals("BEGIN", extr.getTokProp(TokProperty.BC0, -1));
        assertEquals("BEGIN_NO_CLUSTER", extr.getTokProp(TokProperty.BC1, -1));
        assertEquals("BEGIN_NO_DEPREL", extr.getTokProp(TokProperty.DEPREL, -1));
        assertEquals("UNK-CAPS", extr.getTokProp(TokProperty.UNK, -1));
        assertEquals("begin_no_form", extr.getTokProp(TokProperty.LC, -1));
        assertEquals("BEGIN", extr.getTokProp(TokProperty.CHPRE5, -1));
    }
    
    @Test    
    public void testEosProperties() {
        TemplateLanguageExtractor extr = getCoNLLSentenceExtractor1();
        int n = SentFeatureExtractorTest.getSpanishConll09Sentence1().size();
        assertEquals("END_NO_FORM", extr.getTokProp(TokProperty.WORD, n));
        assertEquals("END_NO_LEMMA", extr.getTokProp(TokProperty.LEMMA, n));
        assertEquals("END_NO_POS", extr.getTokProp(TokProperty.POS, n));
        assertEquals("NO_MORPH", extr.getTokProp(TokProperty.MORPHO, n));
        assertEquals("END_N", extr.getTokProp(TokProperty.BC0, n));
        assertEquals("END_NO_CLUSTER", extr.getTokProp(TokProperty.BC1, n));
        assertEquals("END_NO_DEPREL", extr.getTokProp(TokProperty.DEPREL, n));
        assertEquals("UNK-CAPS", extr.getTokProp(TokProperty.UNK, n));
        assertEquals("end_no_form", extr.getTokProp(TokProperty.LC, n));
        assertEquals("END_N", extr.getTokProp(TokProperty.CHPRE5, n));
    }
    
    @Test
    public void testTokPropertyAndEdgePropertyNulls1() {        
        FeatTemplate tpl = new FeatTemplate3(PositionList.PATH_P_C, TokProperty.DEPREL, EdgeProperty.DIR, ListModifier.SEQ);
        String expectedFeat = "PATH_P_C.DEPREL.DIR.SEQ_det_UP_subj_UP_v_DOWN_obj";
        getFeatAndAssertEquality(tpl, expectedFeat);
    }
    
    @Test
    public void testTokPropertyAndEdgePropertyNulls2() {
        FeatTemplate tpl = new FeatTemplate3(PositionList.PATH_P_C, TokProperty.DEPREL, null, ListModifier.SEQ);
        String expectedFeat = "PATH_P_C.DEPREL..SEQ_det_subj_v_obj";
        getFeatAndAssertEquality(tpl, expectedFeat);
    }
    
    @Test
    public void testTokPropertyAndEdgePropertyNulls3() {
        FeatTemplate tpl = new FeatTemplate3(PositionList.PATH_P_C, null, EdgeProperty.EDGEREL, ListModifier.SEQ);
        String expectedFeat = "PATH_P_C..EDGEREL.SEQ_det_subj_obj";
        getFeatAndAssertEquality(tpl, expectedFeat);
    }

    @Test
    public void testListModifiers() {
        {
            FeatTemplate tpl = new FeatTemplate3(PositionList.LINE_P_C, TokProperty.POS, null, ListModifier.SEQ);
            String expectedFeat = "LINE_P_C.POS..SEQ_Det_N_V_N";
            getFeatAndAssertEquality(tpl, expectedFeat);
        }
        {
            FeatTemplate tpl = new FeatTemplate3(PositionList.LINE_P_C, TokProperty.POS, null, ListModifier.BAG);
            String expectedFeat = "LINE_P_C.POS..BAG_Det_N_V";
            getFeatAndAssertEquality(tpl, expectedFeat);
        }
        {
            FeatTemplate tpl = new FeatTemplate3(PositionList.LINE_P_C, TokProperty.MORPHO, null, ListModifier.NO_DUP);
            String expectedFeat = "LINE_P_C.MORPHO..NO_DUP_feat1_feat2_feat";
            getFeatAndAssertEquality(tpl, expectedFeat);
        }
    }
    
    @Test
    public void testPositionLists() {
        testPositionListsHelper(2, 3, PositionList.PATH_P_C, "lo_UP_hicieron_DOWN_que", true);
        testPositionListsHelper(2, 4, PositionList.PATH_P_C, "lo_UP_hicieron_DOWN__", true);
        testPositionListsHelper(2, 3, PositionList.PATH_C_LCA, "que_UP_hicieron", true);
        testPositionListsHelper(2, 3, PositionList.PATH_P_LCA, "lo_UP_hicieron", true);
        testPositionListsHelper(2, 3, PositionList.PATH_LCA_ROOT, "hicieron_UP_es_UP_BEGIN_NO_FORM", true);
        
        testPositionListsHelper(2, 3, PositionList.LINE_P_C, "lo_que", false);
        testPositionListsHelper(2, 2, PositionList.LINE_P_C, "lo", false);
        testPositionListsHelper(0, 2, PositionList.LINE_P_C, "Eso_es_lo", false);
        testPositionListsHelper(0, 6, PositionList.LINE_P_C, "Eso_es_lo_que___hicieron_.", false);
        
        // 3 children on the left
        testPositionListsHelper(5, -1, PositionList.CHILDREN_P, "lo_que__", false);
        testPositionListsHelper(5, -1, PositionList.NO_FAR_CHILDREN_P, "que__", false);
        // 1 left and 2 right
        testPositionListsHelper(1, -1, PositionList.CHILDREN_P, "Eso_hicieron_.", false);
        testPositionListsHelper(1, -1, PositionList.NO_FAR_CHILDREN_P, "hicieron", false);
        // No children
        testPositionListsHelper(6, -1, PositionList.CHILDREN_P, "", false);
        testPositionListsHelper(6, -1, PositionList.NO_FAR_CHILDREN_P, "", false);
    }

    private void testPositionListsHelper(int pidx, int cidx, PositionList pl, String expectedVal, boolean includeDir) {
        FeatTemplate tpl = new FeatTemplate3(pl, TokProperty.WORD, includeDir ? EdgeProperty.DIR : null, ListModifier.SEQ);
        TemplateLanguageExtractor extr = getCoNLLSentenceExtractor2();        
        ArrayList<Object> feats = new ArrayList<Object>();
        extr.addFeatures(tpl, pidx, cidx, feats);
        for (Object feat : feats) {
            System.out.println(feat);
        }
        assertEquals(feats.size(), 1);
        assertEquals(tpl.getName() + "_" + expectedVal, feats.get(0));
    }
    

    @Test
    public void testOtherFeatures() {
        testOtherFeaturesHelper(2, 4, OtherFeat.CONTINUITY, "1");
        //
        testOtherFeaturesHelper(2, 4, OtherFeat.DISTANCE, "2");
        testOtherFeaturesHelper(4, 2, OtherFeat.DISTANCE, "2");
        //
        testOtherFeaturesHelper(5, 3, OtherFeat.GENEOLOGY, "parent");
        testOtherFeaturesHelper(3, 5, OtherFeat.GENEOLOGY, "child");
        testOtherFeaturesHelper(1, 2, OtherFeat.GENEOLOGY, "ancestor");
        testOtherFeaturesHelper(2, 1, OtherFeat.GENEOLOGY, "descendent");
        testOtherFeaturesHelper(0, 2, OtherFeat.GENEOLOGY, "cousin");
        testOtherFeaturesHelper(2, 0, OtherFeat.GENEOLOGY, "cousin");
        testOtherFeaturesHelper(4, 2, OtherFeat.GENEOLOGY, "sibling");
        //
        testOtherFeaturesHelper(4, 2, OtherFeat.PATH_LEN, "3");
        testOtherFeaturesHelper(2, 4, OtherFeat.PATH_LEN, "3");
        testOtherFeaturesHelper(0, 3, OtherFeat.PATH_LEN, "4");
        testOtherFeaturesHelper(0, 0, OtherFeat.PATH_LEN, "1");
        //
        testOtherFeaturesHelper(4, 2, OtherFeat.RELATIVE, "after");
        testOtherFeaturesHelper(2, 2, OtherFeat.RELATIVE, "on");
        testOtherFeaturesHelper(2, 4, OtherFeat.RELATIVE, "before");
    }

    private void testOtherFeaturesHelper(int pidx, int cidx, OtherFeat f, String expectedVal) {
        FeatTemplate tpl = new FeatTemplate4(f);
        TemplateLanguageExtractor extr = getCoNLLSentenceExtractor2();        
        ArrayList<Object> feats = new ArrayList<Object>();
        extr.addFeatures(tpl, pidx, cidx, feats);
        for (Object feat : feats) {
            System.out.println(feat);
        }
        assertEquals(feats.size(), 1);
        assertEquals(tpl.getName() + "_" + expectedVal, feats.get(0));
    }
    
    @Test
    public void testPathGramsFeature() {      
        FeatTemplate tpl = new FeatTemplate4(OtherFeat.PATH_GRAMS);

        String[] expectedPathGrams = new String[] { "PATH_GRAMS_the", "PATH_GRAMS_Det", "PATH_GRAMS_dog",
                "PATH_GRAMS_N", "PATH_GRAMS_ate", "PATH_GRAMS_V", "PATH_GRAMS_food", "PATH_GRAMS_N",
                "PATH_GRAMS_the_dog", "PATH_GRAMS_Det_dog", "PATH_GRAMS_the_N", "PATH_GRAMS_Det_N",
                "PATH_GRAMS_dog_ate", "PATH_GRAMS_N_ate", "PATH_GRAMS_dog_V", "PATH_GRAMS_N_V", "PATH_GRAMS_ate_food",
                "PATH_GRAMS_V_food", "PATH_GRAMS_ate_N", "PATH_GRAMS_V_N", "PATH_GRAMS_the_dog_ate",
                "PATH_GRAMS_Det_dog_ate", "PATH_GRAMS_the_N_ate", "PATH_GRAMS_Det_N_ate", "PATH_GRAMS_the_dog_V",
                "PATH_GRAMS_Det_dog_V", "PATH_GRAMS_the_N_V", "PATH_GRAMS_Det_N_V", "PATH_GRAMS_dog_ate_food",
                "PATH_GRAMS_N_ate_food", "PATH_GRAMS_dog_V_food", "PATH_GRAMS_N_V_food", "PATH_GRAMS_dog_ate_N",
                "PATH_GRAMS_N_ate_N", "PATH_GRAMS_dog_V_N", "PATH_GRAMS_N_V_N", };
        
        getFeatsAndAssertEquality(tpl, expectedPathGrams);
    }
    
    @Test
    public void testBigramFeature() {   
        {
            // Single feature.
            FeatTemplate tpl1 = new FeatTemplate1(Position.PARENT, PositionModifier.IDENTITY, TokProperty.MORPHO);
            String expected1 = "PARENT.IDENTITY.MORPHO_feat1_feat2";
            FeatTemplate tpl2 = new FeatTemplate3(PositionList.LINE_P_C, TokProperty.POS, null, ListModifier.SEQ);
            String expected2 = "LINE_P_C.POS..SEQ_Det_N_V_N";
            FeatTemplate tpl = new BigramTemplate(tpl1, tpl2);
            getFeatAndAssertEquality(tpl, expected1 + "_" + expected2);
        }
        {
            // Multiple features.
            FeatTemplate tpl1 = new FeatTemplate2(Position.PARENT, PositionModifier.IDENTITY, TokPropList.EACH_MORPHO);
            String[] expected1 = new String[] { "PARENT.IDENTITY.EACH_MORPHO_feat1",
                    "PARENT.IDENTITY.EACH_MORPHO_feat2" };
            FeatTemplate tpl2 = new FeatTemplate3(PositionList.LINE_P_C, TokProperty.POS, null, ListModifier.SEQ);
            String expected2 = "LINE_P_C.POS..SEQ_Det_N_V_N";
            FeatTemplate tpl = new BigramTemplate(tpl1, tpl2);
            String[] expected = new String[] { expected1[0] + "_" + expected2, expected1[1] + "_" + expected2 };
            getFeatsAndAssertEquality(tpl, expected);
        }
    }
    
    // Single feature.
    private void getFeatAndAssertEquality(FeatTemplate tpl, String expectedFeat) {
        TemplateLanguageExtractor extr = getDogSentenceExtractor();  
        
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

    // Multiple features
    private void getFeatsAndAssertEquality(FeatTemplate tpl, String[] expectedPathGrams) {
        TemplateLanguageExtractor extr = getDogSentenceExtractor();  
        int pidx = 0;
        int cidx = 3;
        ArrayList<Object> feats = new ArrayList<Object>();
        extr.addFeatures(tpl, pidx, cidx, feats);

        for (Object feat : feats) {
            System.out.println(feat);
        }
        assertEquals(new HashSet<String>(Arrays.asList(expectedPathGrams)), new HashSet<Object>(feats));
    }

    private static TemplateLanguageExtractor getDogSentenceExtractor() {
        SimpleAnnoSentence sent = CoNLL09Sentence.toSimpleAnnoSentence(SimpleAnnoSentenceTest.getDogConll09Sentence(), true);
        CorpusStatistics cs = new CorpusStatistics(new CorpusStatisticsPrm());
        cs.init(Lists.getList(sent));
        TemplateLanguageExtractor extr = new TemplateLanguageExtractor(sent, cs);
        return extr;
    }

    private static TemplateLanguageExtractor getCoNLLSentenceExtractor1() {
        SimpleAnnoSentence sent = CoNLL09Sentence.toSimpleAnnoSentence(SentFeatureExtractorTest.getSpanishConll09Sentence1(), true);
        addFakeBrownClusters(sent);
        CorpusStatistics cs = new CorpusStatistics(new CorpusStatisticsPrm());
        cs.init(Lists.getList(sent));
        TemplateLanguageExtractor extr = new TemplateLanguageExtractor(sent, cs);
        return extr;
    }

    private static TemplateLanguageExtractor getCoNLLSentenceExtractor2() {
        SimpleAnnoSentence sent = CoNLL09Sentence.toSimpleAnnoSentence(SentFeatureExtractorTest.getSpanishConll09Sentence2(), true);
        addFakeBrownClusters(sent);
        CorpusStatistics cs = new CorpusStatistics(new CorpusStatisticsPrm());
        cs.init(Lists.getList(sent));
        TemplateLanguageExtractor extr = new TemplateLanguageExtractor(sent, cs);
        return extr;
    }
    
    public static void addFakeBrownClusters(SimpleAnnoSentence sent) {
        ArrayList<String> clusters = new ArrayList<String>();
        for (int i=0; i<sent.size(); i++) {
            clusters.add(FastMath.mod(i*7, 2) + "10101" + FastMath.mod(i*39, 2));
        }
        sent.setClusters(clusters);
    }

}
