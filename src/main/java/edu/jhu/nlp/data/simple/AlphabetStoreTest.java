package edu.jhu.nlp.data.simple;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.util.collections.Lists;

public class AlphabetStoreTest {

    public static final int NUM_TOKENS = 100 + AlphabetStore.NUM_SPECIAL_TOKS; 
    public static final String TOK_UNK_STR = AlphabetStore.TOK_UNK_STR;
    public static final int FIRST_TOK_ID = AlphabetStore.NUM_SPECIAL_TOKS;
    
    @Test
    public void testAlphabetStoreNoCutoff() {
        AnnoSentenceCollection sents = getSents(false);
        AlphabetStore store = new AlphabetStore(sents);
        
        // Check alphabet sizes.
        assertEquals(NUM_TOKENS, store.words.size());
        assertEquals(NUM_TOKENS, store.prefixes.size());
        assertEquals(NUM_TOKENS, store.lemmas.size());
        assertEquals(NUM_TOKENS, store.posTags.size());
        assertEquals(NUM_TOKENS, store.cposTags.size());
        assertEquals(NUM_TOKENS, store.clusters.size());
        assertEquals(NUM_TOKENS, store.feats.size());
        assertEquals(NUM_TOKENS, store.deprels.size());
        
        
        // Check alphabet contents.
        assertEquals(TOK_UNK_STR, store.words.lookupObject(0));
        assertEquals("word"+0, store.words.lookupObject(FIRST_TOK_ID));
        assertEquals(FIRST_TOK_ID, store.getWordIdx("word"+0));

        assertEquals(TOK_UNK_STR, store.prefixes.lookupObject(0));
        assertEquals("prefix"+0, store.prefixes.lookupObject(FIRST_TOK_ID));
        assertEquals(FIRST_TOK_ID, store.getPrefixIdx("prefix"+0));
        
        assertEquals(TOK_UNK_STR, store.lemmas.lookupObject(0));
        assertEquals("lemma"+0, store.lemmas.lookupObject(FIRST_TOK_ID));
        assertEquals(FIRST_TOK_ID, store.getLemmaIdx("lemma"+0));

        assertEquals(TOK_UNK_STR, store.posTags.lookupObject(0));
        assertEquals("pos"+0, store.posTags.lookupObject(FIRST_TOK_ID));
        assertEquals(FIRST_TOK_ID, store.getPosTagIdx("pos"+0));

        assertEquals(TOK_UNK_STR, store.cposTags.lookupObject(0));
        assertEquals("cpos"+0, store.cposTags.lookupObject(FIRST_TOK_ID));
        assertEquals(FIRST_TOK_ID, store.getCposTagIdx("cpos"+0));

        assertEquals(TOK_UNK_STR, store.clusters.lookupObject(0));
        assertEquals("cluster"+0, store.clusters.lookupObject(FIRST_TOK_ID));
        assertEquals(FIRST_TOK_ID, store.getClusterIdx("cluster"+0));

        assertEquals(TOK_UNK_STR, store.feats.lookupObject(0));
        assertEquals("feat"+0, store.feats.lookupObject(FIRST_TOK_ID));
        assertEquals(FIRST_TOK_ID, store.getFeatIdx("feat"+0));

        assertEquals(TOK_UNK_STR, store.deprels.lookupObject(0));
        assertEquals("deprel"+0, store.deprels.lookupObject(FIRST_TOK_ID));
        assertEquals(FIRST_TOK_ID, store.getDeprelIdx("deprel"+0));
    }

    @Test
    public void testRemovedAt() {
        AnnoSentenceCollection orig = getSents(false);
        for (AT at : AT.values()) {
            AnnoSentenceCollection sents = orig.getWithAtsRemoved(Lists.getList(at));
            AlphabetStore store = new AlphabetStore(sents);
            
            // Check alphabet sizes.
            assertEquals(at == AT.WORD ? FIRST_TOK_ID : NUM_TOKENS, store.words.size());
            assertEquals(at == AT.PREFIX ? FIRST_TOK_ID : NUM_TOKENS, store.prefixes.size());
            assertEquals(at == AT.LEMMA ? FIRST_TOK_ID : NUM_TOKENS, store.lemmas.size());
            assertEquals(at == AT.POS ? FIRST_TOK_ID : NUM_TOKENS, store.posTags.size());
            assertEquals(at == AT.CPOS ? FIRST_TOK_ID : NUM_TOKENS, store.cposTags.size());
            assertEquals(at == AT.BROWN ? FIRST_TOK_ID : NUM_TOKENS, store.clusters.size());
            assertEquals(at == AT.MORPHO ? FIRST_TOK_ID : NUM_TOKENS, store.feats.size());
            assertEquals(at == AT.DEPREL ? FIRST_TOK_ID : NUM_TOKENS, store.deprels.size());
        }
    }
    
    @Test
    public void testAlphabetStoreWithCutoff() {
        AnnoSentenceCollection sents = getSents(true);
        AlphabetStore store = new AlphabetStore(sents);
        assertEquals(NUM_TOKENS, store.words.size());
        assertEquals(NUM_TOKENS, store.prefixes.size());
        assertEquals(NUM_TOKENS, store.lemmas.size());
        assertEquals(NUM_TOKENS, store.posTags.size());
        assertEquals(NUM_TOKENS, store.cposTags.size());
        assertEquals(NUM_TOKENS, store.clusters.size());
        assertEquals(NUM_TOKENS, store.feats.size());
        assertEquals(NUM_TOKENS, store.deprels.size());
    }

    public static AnnoSentenceCollection getSents(boolean includeExtras) {
        AnnoSentenceCollection sents = new AnnoSentenceCollection();
        // Add three tokens for word<i> for i in [0,..,99].
        for (int j=0; j<3; j++) {
            for (int i=0; i<100; i++) {
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
            }
        }
        
        if (includeExtras) {
            // Add one token for word<i> for i in [100,..., 65545].
            int i=100;
            AnnoSentence s = new AnnoSentence();
            s.setWords(getList("word"+i));
            s.setPrefixes(getList("prefix"+i));
            s.setLemmas(getList("lemma"+i));
            s.setPosTags(getList("pos"+i));
            s.setCposTags(getList("cpos"+i));
            s.setClusters(getList("cluster"+i));
            s.setFeats(getList(getList("feat"+i)));
            s.setDeprels(getList("deprel"+i));
            sents.add(s);
            
            for (i=101; i<0xffff+10; i++) {
                s.getWords().add("word"+i);
                s.getPrefixes().add("prefix"+i);
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
    
    @Test
    public void testStopGrowth() {
        AlphabetStore store = new AlphabetStore(new AnnoSentenceCollection());
        store.startGrowth();
        assertEquals(true, store.words.isGrowing());
        assertEquals(true, store.prefixes.isGrowing());
        assertEquals(true, store.lemmas.isGrowing());
        assertEquals(true, store.posTags.isGrowing());
        assertEquals(true, store.cposTags.isGrowing());
        assertEquals(true, store.clusters.isGrowing());
        assertEquals(true, store.feats.isGrowing());
        assertEquals(true, store.deprels.isGrowing());
        store.stopGrowth();
        assertEquals(false, store.words.isGrowing());
        assertEquals(false, store.prefixes.isGrowing());
        assertEquals(false, store.lemmas.isGrowing());
        assertEquals(false, store.posTags.isGrowing());
        assertEquals(false, store.cposTags.isGrowing());
        assertEquals(false, store.clusters.isGrowing());
        assertEquals(false, store.feats.isGrowing());
        assertEquals(false, store.deprels.isGrowing());        
    }
    

    @SafeVarargs
    public static <T> List<T> getList(T... args) {
        ArrayList<T> a = new ArrayList<>();
        a.addAll(Arrays.asList(args));
        return a;
    }    

}
