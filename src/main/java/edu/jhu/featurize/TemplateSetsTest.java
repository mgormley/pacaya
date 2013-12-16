package edu.jhu.featurize;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import edu.jhu.featurize.TemplateLanguage.FeatTemplate;

public class TemplateSetsTest {

    @Test
    public void testGetAll() {
        List<FeatTemplate> tpls;
        
        tpls = TemplateSets.getAllUnigramFeatureTemplates();
        int numUnigrams = tpls.size();
        System.out.println("Number of unigram templates: " + numUnigrams);        
        
        tpls = TemplateSets.getAllBigramFeatureTemplates();
        int numBigrams = tpls.size();
        System.out.println("Number of bigram templates: " + numBigrams);
        
        assertEquals(862, numUnigrams);
        assertEquals(371091, numBigrams);
    }
    
    @Test
    public void testGetBjorkelund() {
        List<FeatTemplate> tpls;
        
        tpls = TemplateSets.getBjorkelundArgUnigramFeatureTemplates();
        int numArg = tpls.size();
        System.out.println("Number of arg templates: " + numArg);        
        
        tpls = TemplateSets.getBjorkelundSenseUnigramFeatureTemplates();
        int numSense = tpls.size();
        System.out.println("Number of sense templates: " + numSense);

        // Bjorkelund lists 32 features, one of which is Sense which we don't
        // treat as observed.
        assertEquals(31, numArg);
        assertEquals(11, numSense);
    } 
    
    @Test
    public void testGetNaradowsky() {
        List<FeatTemplate> tpls;
        
        tpls = TemplateSets.getNaradowskyArgUnigramFeatureTemplates();
        int numArg = tpls.size();
        System.out.println("Number of arg templates: " + numArg);        
        
        tpls = TemplateSets.getNaradowskySenseUnigramFeatureTemplates();
        int numSense = tpls.size();
        System.out.println("Number of sense templates: " + numSense);

        assertEquals(35, numArg);
        assertEquals(6, numSense);
    }    

    @Test
    public void testGetCoarseUnigramSet1() {
        List<FeatTemplate> tpls;        
        tpls = TemplateSets.getCoarseUnigramSet1();
        int numArg = tpls.size();
        System.out.println("Number of templates: " + numArg);
        assertEquals(127, numArg);
    }

}
