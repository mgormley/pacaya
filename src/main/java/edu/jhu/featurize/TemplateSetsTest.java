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
        
        assertEquals(728, numUnigrams);
        assertEquals(264628, numBigrams);
    }

}
