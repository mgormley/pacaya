package edu.jhu.nlp.tag;

import static org.junit.Assert.*;
import static edu.jhu.nlp.tag.StrictPosTagAnnotator.StrictPosTag.*;

import org.junit.Test;

import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.util.collections.Lists;

public class StrictPosTagAnnotatorTest {

    @Test
    public void testCposMapping() {
        // Make sentence.
        AnnoSentence sent = new AnnoSentence();
        sent.setWords(Lists.getList("OTHER", "VERB", "NOUN", "PUNC", "CONJ", "OTHER2"));
        sent.setCposTags(Lists.getList("VV-other", "VERB", "NOUN", ".", "CONJ", "NN-OTHER2"));
        // Annotate.
        StrictPosTagAnnotator anno = new StrictPosTagAnnotator();
        anno.annotate(sent);
        // Check.
        assertEquals(Lists.getList(OTHER, VERB, NOUN, PUNC, CONJ, OTHER), sent.getStrictPosTags());
    }

    @Test
    public void testPosMapping() {
        // Make sentence.
        AnnoSentence sent = new AnnoSentence();
        sent.setWords(Lists.getList("OTHER", "VERB", "NOUN", "PUNC", "CONJ", "OTHER2"));
        sent.setPosTags(Lists.getList("OTHER-VV", "VV", "NN", "PUNC", "Conj", "OTHER2-NN"));
        // Annotate.
        StrictPosTagAnnotator anno = new StrictPosTagAnnotator();
        anno.annotate(sent);
        // Check.
        assertEquals(Lists.getList(OTHER, VERB, NOUN, PUNC, CONJ, OTHER), sent.getStrictPosTags());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullBehavior() {
        // Make sentence.
        AnnoSentence sent = new AnnoSentence();
        sent.setWords(Lists.getList("OTHER", "VERB", "NOUN", "PUNC", "CONJ", "OTHER2"));
        // Annotate.
        StrictPosTagAnnotator anno = new StrictPosTagAnnotator();
        anno.annotate(sent);
    }

}
