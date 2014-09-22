package edu.jhu.nlp.data.simple;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.util.collections.Lists;

public class AlphabetStoreTest {

    @Test
    public void testAlphabetStoreNoCutoff() {
        AnnoSentenceCollection sents = getSents(false);
        AlphabetStore store = new AlphabetStore(sents);
        
        // Check alphabet sizes.
        assertEquals(101, store.words.size());
        assertEquals(101, store.lemmas.size());
        assertEquals(101, store.posTags.size());
        assertEquals(101, store.cposTags.size());
        assertEquals(101, store.clusters.size());
        assertEquals(101, store.feats.size());
        assertEquals(101, store.deprels.size());
        
        // Check alphabet contents.
        assertEquals(AlphabetStore.UNKNOWN_STR, store.words.lookupObject(0));
        assertEquals("word"+0, store.words.lookupObject(1));
        assertEquals(1, store.getWordIdx("word"+0));
        
        assertEquals(AlphabetStore.UNKNOWN_STR, store.lemmas.lookupObject(0));
        assertEquals("lemma"+0, store.lemmas.lookupObject(1));
        assertEquals(1, store.getLemmaIdx("lemma"+0));

        assertEquals(AlphabetStore.UNKNOWN_STR, store.posTags.lookupObject(0));
        assertEquals("pos"+0, store.posTags.lookupObject(1));
        assertEquals(1, store.getPosTagIdx("pos"+0));

        assertEquals(AlphabetStore.UNKNOWN_STR, store.cposTags.lookupObject(0));
        assertEquals("cpos"+0, store.cposTags.lookupObject(1));
        assertEquals(1, store.getCposTagIdx("cpos"+0));

        assertEquals(AlphabetStore.UNKNOWN_STR, store.clusters.lookupObject(0));
        assertEquals("cluster"+0, store.clusters.lookupObject(1));
        assertEquals(1, store.getClusterIdx("cluster"+0));

        assertEquals(AlphabetStore.UNKNOWN_STR, store.feats.lookupObject(0));
        assertEquals("feat"+0, store.feats.lookupObject(1));
        assertEquals(1, store.getFeatIdx("feat"+0));

        assertEquals(AlphabetStore.UNKNOWN_STR, store.deprels.lookupObject(0));
        assertEquals("deprel"+0, store.deprels.lookupObject(1));
        assertEquals(1, store.getDeprelIdx("deprel"+0));
    }

    @Test
    public void testRemovedAt() {
        AnnoSentenceCollection orig = getSents(false);
        for (AT at : AT.values()) {
            AnnoSentenceCollection sents = orig.getWithAtsRemoved(Lists.getList(at));
            AlphabetStore store = new AlphabetStore(sents);
            
            // Check alphabet sizes.
            assertEquals(at == AT.WORD ? 1 : 101, store.words.size());
            assertEquals(at == AT.LEMMA ? 1 : 101, store.lemmas.size());
            assertEquals(at == AT.POS ? 1 : 101, store.posTags.size());
            assertEquals(at == AT.CPOS ? 1 : 101, store.cposTags.size());
            assertEquals(at == AT.BROWN ? 1 : 101, store.clusters.size());
            assertEquals(at == AT.MORPHO ? 1 : 101, store.feats.size());
            assertEquals(at == AT.DEPREL ? 1 : 101, store.deprels.size());
        }
    }
    
    @Test
    public void testAlphabetStoreWithCutoff() {
        AnnoSentenceCollection sents = getSents(true);
        AlphabetStore store = new AlphabetStore(sents);
        assertEquals(101, store.words.size());
        assertEquals(101, store.lemmas.size());
        assertEquals(101, store.posTags.size());
        assertEquals(101, store.cposTags.size());
        assertEquals(101, store.clusters.size());
        assertEquals(101, store.feats.size());
        assertEquals(101, store.deprels.size());
    }

    public static AnnoSentenceCollection getSents(boolean includeExtras) {
        AnnoSentenceCollection sents = new AnnoSentenceCollection();
        // Add three tokens for word<i> for i in [0,..,99].
        for (int j=0; j<3; j++) {
            for (int i=0; i<100; i++) {
                AnnoSentence s = new AnnoSentence();
                s.setWords(Lists.getList("word"+i));
                s.setLemmas(Lists.getList("lemma"+i));
                s.setPosTags(Lists.getList("pos"+i));
                s.setCposTags(Lists.getList("cpos"+i));
                s.setClusters(Lists.getList("cluster"+i));
                s.setFeats(Lists.getList(Lists.getList("feat"+i)));
                s.setDeprels(Lists.getList("deprel"+i));
                sents.add(s);
            }
        }
        
        if (includeExtras) {
            // Add one token for word<i> for i in [100,..., 65545].
            int i=100;
            AnnoSentence s = new AnnoSentence();
            s.setWords(getList("word"+i));
            s.setLemmas(getList("lemma"+i));
            s.setPosTags(getList("pos"+i));
            s.setCposTags(getList("cpos"+i));
            s.setClusters(getList("cluster"+i));
            s.setFeats(getList(getList("feat"+i)));
            s.setDeprels(getList("deprel"+i));
            sents.add(s);
            
            for (i=101; i<0xffff+10; i++) {
                s.getWords().add("word"+i);
                s.getLemmas().add("lemma"+i);
                s.getPosTags().add("pos"+i);
                s.getCposTags().add("cpos"+i);
                s.getClusters().add("cluster"+i);
                s.getFeats().get(0).add("feat"+i);
                s.getDeprels().add("deprel"+i);
            }
        }
        
        return sents;
    }
    

    @SafeVarargs
    public static <T> List<T> getList(T... args) {
        ArrayList<T> a = new ArrayList<>();
        a.addAll(Arrays.asList(args));
        return a;
    }    

}
