package edu.jhu.nlp.features;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.data.simple.AnnoSentenceTest;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;

public class TemplateLanguageTest {

    @Test
    public void testCheckRequiredAnnotations() {
        AnnoSentence sent = CoNLL09Sentence.toAnnoSentence(AnnoSentenceTest.getDogConll09Sentence(), true);
        List<FeatTemplate> tpls = TemplateSets.getAllUnigramFeatureTemplates();
        Set<AT> types = TemplateLanguage.getRequiredAnnotationTypes(tpls);
        
        assertFalse(TemplateLanguage.hasRequiredAnnotationTypes(sent, types));
        assertTrue(TemplateLanguage.hasRequiredAnnotationType(sent, AT.DEPREL));
        assertFalse(TemplateLanguage.hasRequiredAnnotationType(sent, AT.BROWN));
        
        TemplateFeatureExtractorTest.addFakeBrownClusters(sent);
        sent.setCposTags(sent.getPosTags());
        assertTrue(TemplateLanguage.hasRequiredAnnotationTypes(sent, types));
        assertTrue(TemplateLanguage.hasRequiredAnnotationType(sent, AT.DEPREL));
        assertTrue(TemplateLanguage.hasRequiredAnnotationType(sent, AT.BROWN));
    }

}
