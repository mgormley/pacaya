package edu.jhu.featurize;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceTest;
import edu.jhu.featurize.TemplateLanguage.AT;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;

public class TemplateLanguageTest {

    @Test
    public void testCheckRequiredAnnotations() {
        SimpleAnnoSentence sent = CoNLL09Sentence.toSimpleAnnoSentence(SimpleAnnoSentenceTest.getDogConll09Sentence(), true);
        List<FeatTemplate> tpls = TemplateSets.getAllUnigramFeatureTemplates();
        Set<AT> types = TemplateLanguage.getRequiredAnnotationTypes(tpls);
        
        assertFalse(TemplateLanguage.hasRequiredAnnotationTypes(sent, types));
        assertTrue(TemplateLanguage.hasRequiredAnnotationType(sent, AT.DEPREL));
        assertFalse(TemplateLanguage.hasRequiredAnnotationType(sent, AT.BROWN));
        
        TemplateFeatureExtractorTest.addFakeBrownClusters(sent);        
        assertTrue(TemplateLanguage.hasRequiredAnnotationTypes(sent, types));
        assertTrue(TemplateLanguage.hasRequiredAnnotationType(sent, AT.DEPREL));
        assertTrue(TemplateLanguage.hasRequiredAnnotationType(sent, AT.BROWN));
    }

}
