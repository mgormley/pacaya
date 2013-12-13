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
        System.out.println("Number of unigram templates: " + tpls.size());
        assertEquals(706, tpls.size());
        
        tpls = TemplateSets.getAllBigramFeatureTemplates();
        System.out.println("Number of bigram templates: " + tpls.size());
        assertEquals(248865, tpls.size());
    }

}
