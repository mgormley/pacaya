package edu.jhu.nlp.data.simple;

import org.junit.Test;

import static org.junit.Assert.*;
import edu.jhu.nlp.data.simple.AnnoSentenceReader.DatasetType;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.nlp.tag.StrictPosTagAnnotator;
import edu.jhu.util.collections.Lists;

public class IntAnnoSentenceTest {
    
    @Test    
    public void testKnownTypes() {
        AnnoSentenceCollection sents = AlphabetStoreTest.getSents(false);
        AlphabetStore store = new AlphabetStore(sents);
               
        // Test known words.
        int i = 0;
        AnnoSentence s = new AnnoSentence();
        s.setWords(Lists.getList("word"+i));
        s.setPrefixes(Lists.getList("prefix"+i));
        s.setLemmas(Lists.getList("lemma"+i));
        s.setPosTags(Lists.getList("pos"+i));
        s.setCposTags(Lists.getList("cpos"+i));
        s.setClusters(Lists.getList("cluster"+i));
        s.setFeats(Lists.getList(Lists.getList("feat"+i)));
        s.setDeprels(Lists.getList("deprel"+i));
        sents.add(s);
        
        IntAnnoSentence isent = new IntAnnoSentence(s, store);
        assertEquals((short) AlphabetStoreTest.FIRST_TOK_ID, isent.getWord(0));
        assertEquals((short) AlphabetStoreTest.FIRST_TOK_ID, isent.getPrefix(0));
        assertEquals((short) AlphabetStoreTest.FIRST_TOK_ID, isent.getLemma(0));
        assertEquals((byte) AlphabetStoreTest.FIRST_TOK_ID, isent.getPosTag(0));
        assertEquals((byte) AlphabetStoreTest.FIRST_TOK_ID, isent.getCposTag(0));
        assertEquals((short) AlphabetStoreTest.FIRST_TOK_ID, isent.getCluster(0));
        assertEquals((short) AlphabetStoreTest.FIRST_TOK_ID, isent.getFeats(0).get(0));
        assertEquals((byte) AlphabetStoreTest.FIRST_TOK_ID, isent.getDeprel(0));
    }
    
    @Test    
    public void testUnknownTypes() {
        AnnoSentenceCollection sents = AlphabetStoreTest.getSents(false);
        AlphabetStore store = new AlphabetStore(sents);
        
        // Test unknown words.
        String i = "-unseen-suffix";
        AnnoSentence s = new AnnoSentence();
        s.setWords(Lists.getList("word"+i));
        s.setPrefixes(Lists.getList("prefix"+i));
        s.setLemmas(Lists.getList("lemma"+i));
        s.setPosTags(Lists.getList("pos"+i));
        s.setCposTags(Lists.getList("cpos"+i));
        s.setClusters(Lists.getList("cluster"+i));
        s.setFeats(Lists.getList(Lists.getList("feat"+i)));
        s.setDeprels(Lists.getList("deprel"+i));
        sents.add(s);
        
        IntAnnoSentence isent = new IntAnnoSentence(s, store);
        assertEquals((short) 0, isent.getWord(0));
        assertEquals((short) 0, isent.getPrefix(0));
        assertEquals((short) 0, isent.getLemma(0));
        assertEquals((byte) 0, isent.getPosTag(0));
        assertEquals((byte) 0, isent.getCposTag(0));
        assertEquals((short) 0, isent.getCluster(0));
        assertEquals((short) 0, isent.getFeats(0).get(0));
        assertEquals((byte) 0, isent.getDeprel(0));
    }
    
    @Test
    public void testNumInBetween() {
        AnnoSentence sent = new AnnoSentence();
        sent.setWords(Lists.getList(   "0",     "1",    "2",    "3", "4",    "5",    "6",    "7"));
        sent.setCposTags(Lists.getList("VERB", "VERB", "NOUN", ".", "CONJ", "NOUN", "VERB", "VERB"));
        AnnoSentenceCollection sents = new AnnoSentenceCollection();
        sents.add(sent);
        AlphabetStore store = new AlphabetStore(sents);
        StrictPosTagAnnotator anno = new StrictPosTagAnnotator();
        anno.annotate(sents);
        
        IntAnnoSentence isent = new IntAnnoSentence(sent, store);
        assertEquals(0, isent.getNumVerbsInBetween(1, 1));
        assertEquals(1, isent.getNumVerbsInBetween(0, 2));
        assertEquals(2, isent.getNumVerbsInBetween(0, 7));
        assertEquals(3, isent.getNumVerbsInBetween(0, 8));
        assertEquals(4, isent.getNumVerbsInBetween(-1, 8));
    }
}
