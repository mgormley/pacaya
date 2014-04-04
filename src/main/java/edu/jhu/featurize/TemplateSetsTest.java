package edu.jhu.featurize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import edu.jhu.featurize.TemplateLanguage.AT;
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
        
        assertTrue(1567 <= numUnigrams);
        assertTrue(1226961 <= numBigrams);
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

        assertEquals(19, numArg);
        assertEquals(5, numSense);
    }    
    

    @Test
    public void testGetZhao() {
        List<FeatTemplate> tpls;
        
        tpls = TemplateSets.getZhaoCaArgUnigramFeatureTemplates();
        int numArg = tpls.size();
        System.out.println("Number of arg templates: " + numArg);        
        
        tpls = TemplateSets.getZhaoEnSenseUnigramFeatureTemplates();
        int numSense = tpls.size();
        System.out.println("Number of sense templates: " + numSense);

        assertEquals(48, numArg);
        assertEquals(8, numSense);
    }

    @Test
    public void testGetCoarseUnigramSet1() {
        List<FeatTemplate> tpls;        
        {
            tpls = TemplateSets.getCoarseUnigramSet1();
            int numArg = tpls.size();
            System.out.println("Number of templates: " + numArg);
            assertEquals(127, numArg);
        }

        // This tests that names are created correctly.
        for (FeatTemplate tpl : tpls) {
            System.out.println(tpl);
        }
        // Remove each level of supervision.
        for (AT at : new AT[] { AT.DEPREL, AT.DEP_TREE, AT.MORPHO, AT.POS, AT.LEMMA }) {
            tpls = TemplateLanguage.filterOutRequiring(tpls, at);
            System.out.println(String.format("Number of templates after filtering %s: %d", at.name(), tpls.size()));
        }
    }
    
    @Test
    public void testGetCoarseUnigramSet2() {    
        List<FeatTemplate> tpls;        
        {
            tpls = TemplateSets.getCoarseUnigramSet2();
            int numArg = tpls.size();
            System.out.println("Number of templates: " + numArg);
            //assertEquals(213, numArg);
        }

        // Remove each level of supervision.
        for (AT at : new AT[] { AT.DEPREL, AT.DEP_TREE, AT.MORPHO, AT.POS, AT.LEMMA }) {
            tpls = TemplateLanguage.filterOutRequiring(tpls, at);
            System.out.println(String.format("Number of templates after filtering %s: %d", at.name(), tpls.size()));
        }

        // This tests that names are created correctly.
        for (FeatTemplate tpl : tpls) {
            System.out.println(tpl);
        }
        
    }
    
    @Test
    public void testGetCustomArgSet1() {
        Collection<FeatTemplate> tpls = new HashSet<FeatTemplate>();        
        {
            tpls.addAll(TemplateSets.getCustomArgSet1());
            int numArg = tpls.size();
            System.out.println("Number of templates: " + numArg);
        }
        // This tests that names are created correctly.
        for (FeatTemplate tpl : tpls) {
            System.out.println(tpl);
        }
        System.out.println();
        // Remove each level of supervision.
        for (AT at : new AT[] { AT.DEPREL, AT.DEP_TREE, AT.MORPHO, AT.POS, AT.LEMMA }) {
            tpls = TemplateLanguage.filterOutRequiring(new ArrayList<FeatTemplate>(tpls), at);
            System.out.println(String.format("Number of templates after filtering %s: %d", at.name(), tpls.size()));
        }
    }
    
    @Test
    public void testGetCustomSenseSet1() {
        Collection<FeatTemplate> tpls = new HashSet<FeatTemplate>();        
        {
            tpls.addAll(TemplateSets.getCustomSenseSet1());
            int numArg = tpls.size();
            System.out.println("Number of templates: " + numArg);
        }

        // This tests that names are created correctly.
        for (FeatTemplate tpl : tpls) {
            System.out.println(tpl);
        }
        System.out.println();
        // Remove each level of supervision.
        for (AT at : new AT[] { AT.DEPREL, AT.DEP_TREE, AT.MORPHO, AT.POS, AT.LEMMA }) {
            tpls = TemplateLanguage.filterOutRequiring(new ArrayList<FeatTemplate>(tpls), at);
            System.out.println(String.format("Number of templates after filtering %s: %d", at.name(), tpls.size()));
        }
    }

}
