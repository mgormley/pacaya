package edu.jhu.nlp.data.simple;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.primitives.UnsignedBytes;

import edu.jhu.prim.list.ByteArrayList;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.list.ShortArrayList;
import edu.jhu.prim.util.SafeCast;
import edu.jhu.util.Alphabet;
import edu.jhu.util.CountingAlphabet;
import edu.jhu.util.collections.Lists;

public class IntAnnoSentence {

    public static class AlphabetStore {

        private static final Logger log = Logger.getLogger(AlphabetStore.class);

        public static String UNKNOWN_STR = "UNKNOWN_LABEL";
        public static int UNKNOWN_INT = 0; 
        
        private Alphabet<String> words;
        private Alphabet<String> lemmas;
        private Alphabet<String> posTags;
        private Alphabet<String> cposTags;
        private Alphabet<String> clusters;
        private Alphabet<String> feats;
        private Alphabet<String> deprels;
        // TODO: 
        //Alphabet<String> lexAlphabet;
        //Alphabet<String> ntAlphabet;    
        private List<Alphabet<String>> as;
        
        private interface StrGetter {
            List<String> getStrs(AnnoSentence sent);
        }
        private StrGetter wordGetter = new StrGetter() {
            public List<String> getStrs(AnnoSentence sent) { return sent.getWords(); }
        };
        private StrGetter lemmaGetter = new StrGetter() {
            public List<String> getStrs(AnnoSentence sent) { return sent.getLemmas(); }
        };
        private StrGetter posTagGetter = new StrGetter() {
            public List<String> getStrs(AnnoSentence sent) { return sent.getPosTags(); }
        };
        private StrGetter cposTagGetter = new StrGetter() {
            public List<String> getStrs(AnnoSentence sent) { return sent.getCposTags(); }
        };
        private StrGetter clusterGetter = new StrGetter() {
            public List<String> getStrs(AnnoSentence sent) { return sent.getClusters(); }
        };
        private StrGetter featGetter = new StrGetter() {
            public List<String> getStrs(AnnoSentence sent) {
                if (sent.getFeats() == null) { return null; }
                ArrayList<String> strs = new ArrayList<>();
                for (List<String> featList : sent.getFeats()) {
                    if (featList != null) {
                        strs.addAll(featList);
                    }
                }
                return strs;
            }
        };
        private StrGetter deprelGetter = new StrGetter() {
            public List<String> getStrs(AnnoSentence sent) { return sent.getDeprels(); }
        };
        
        public AlphabetStore(AnnoSentenceCollection sents) {
            words = getInitAlphabet("word", wordGetter, IntAnnoSentence.MAX_WORD, sents);
            lemmas = getInitAlphabet("lemma", lemmaGetter, IntAnnoSentence.MAX_LEMMA, sents);
            posTags = getInitAlphabet("pos", posTagGetter, IntAnnoSentence.MAX_POS, sents);
            cposTags = getInitAlphabet("cpos", cposTagGetter, IntAnnoSentence.MAX_CPOS, sents);
            clusters = getInitAlphabet("cluster", clusterGetter, IntAnnoSentence.MAX_CLUSTER, sents);
            feats = getInitAlphabet("feat", featGetter, IntAnnoSentence.MAX_FEAT, sents);
            deprels= getInitAlphabet("deprel", deprelGetter, IntAnnoSentence.MAX_DEPREL, sents);
            
            as = Lists.getList(words, lemmas, posTags, cposTags, clusters, feats, deprels);
        }

        private static Alphabet<String> getInitAlphabet(String name, StrGetter sg, int maxIdx, AnnoSentenceCollection sents) {
            CountingAlphabet<String> counter = new CountingAlphabet<>();
            for (AnnoSentence sent : sents) {
                List<String> strs = sg.getStrs(sent);
                if (strs != null) {
                    for (String str : strs) {
                        counter.lookupIndex(str);
                    }
                }
            }
            Alphabet<String> alphabet;
            for (int cutoff = 1; ; cutoff++) {
                alphabet = getInitAlphabet();
                for (int idx=0; idx<counter.size(); idx++) {
                    String str = counter.lookupObject(idx);
                    int count = counter.lookupObjectCount(idx);
                    if (count >= cutoff) {
                        alphabet.lookupIndex(str);
                    }
                }
                if (alphabet.size()-1 <= maxIdx) {
                    log.info(String.format("For %s: Actual count = %d Reduced count = %d Cutoff = %d", 
                            name, counter.size(), alphabet.size(), cutoff));
                    break;
                }
            }
            return alphabet;
        }
        
        private static Alphabet<String> getInitAlphabet() {
            Alphabet<String> alphabet = new Alphabet<String>();
            int idx = alphabet.lookupIndex(UNKNOWN_STR);
            if (idx != UNKNOWN_INT) {
                throw new RuntimeException("Expecting first index from alphabet to be 0");
            }
            return alphabet;
        }

        public void startGrowth() {
            for (Alphabet<String> a : as) {
                a.startGrowth();
            }
        }
        
        public void stopGrowth() {
            for (Alphabet<String> a : as) {
                a.stopGrowth();
            }
        }
        
        private static int safeLookup(Alphabet<String> alphabet, String word) {
            int idx = alphabet.lookupIndex(word);
            if (idx == -1) {
                idx = UNKNOWN_INT;
            }
            return idx;
        }
        
        public int getWordIdx(String word) {
            return safeLookup(words, word);
        }

        public int getPosTagIdx(String pos) {
            return safeLookup(posTags, pos);
        }

        public int getCposTagIdx(String cpos) {
            return safeLookup(cposTags, cpos);
        }

        public int getClusterIdx(String cluster) {
            return safeLookup(clusters, cluster);
        }

        public int getFeatIdx(String feat) {
            return safeLookup(feats, feat);
        }

        public int getDeprelIdx(String deprel) {
            return safeLookup(deprels, deprel);
        }
        
    }

    private static final int BYTE_MAX = 0xff;
    private static final int SHORT_MAX = 0xffff;
    // TODO: We're missing a bit because our Alphabets always return signed values. 
    private static final int INT_MAX = Integer.MAX_VALUE; //0xffffffff;

    private final static int MAX_WORD = SHORT_MAX;
    private final static int MAX_LEMMA = SHORT_MAX;
    private final static int MAX_POS = BYTE_MAX;
    private final static int MAX_CPOS = BYTE_MAX;
    private final static int MAX_CLUSTER = SHORT_MAX;
    private final static int MAX_FEAT = SHORT_MAX;
    private final static int MAX_DEPREL = BYTE_MAX;
    
    private ShortArrayList words;
    private ShortArrayList lemmas;
    private ByteArrayList posTags;
    private ByteArrayList cposTags;
    private ShortArrayList clusters;
    private ArrayList<ShortArrayList> feats;
    private ByteArrayList deprels;
    // TODO: private IntNaryTree naryTree;
    
    private AlphabetStore store;
    
    public IntAnnoSentence(AnnoSentence sent, AlphabetStore store) {
        this.store = store;
        this.words = getShorts(sent.getWords(), store.words);
        this.lemmas = getShorts(sent.getLemmas(), store.lemmas);
        this.posTags = getBytes(sent.getPosTags(), store.posTags);
        this.cposTags = getBytes(sent.getCposTags(), store.cposTags);
        this.clusters = getShorts(sent.getClusters(), store.clusters);
        feats = new ArrayList<>(sent.getFeats().size());
        for (List<String> featList : sent.getFeats()) {
            feats.add(getShorts(featList, store.feats));
        }
        this.deprels = getBytes(sent.getDeprels(), store.deprels);
    }

    private static IntArrayList getInts(List<String> tokens, Alphabet<String> alphabet) {
        if (tokens == null) { return null; }
        IntArrayList arr = new IntArrayList();
        for (int i=0; i<tokens.size(); i++) {
            int idx = AlphabetStore.safeLookup(alphabet, tokens.get(i));
            arr.add(idx);
        }
        return arr;
    }
    
    private static ShortArrayList getShorts(List<String> tokens, Alphabet<String> alphabet) {
        if (tokens == null) { return null; }
        ShortArrayList arr = new ShortArrayList();
        for (int i=0; i<tokens.size(); i++) {
            int idx = AlphabetStore.safeLookup(alphabet, tokens.get(i));
            arr.add(SafeCast.safeIntToShort(idx));
        }
        return arr;
    }
    
    private static ByteArrayList getBytes(List<String> tokens, Alphabet<String> alphabet) {
        if (tokens == null) { return null; }
        ByteArrayList arr = new ByteArrayList();
        for (int i=0; i<tokens.size(); i++) {
            int idx = AlphabetStore.safeLookup(alphabet, tokens.get(i));
            arr.add(SafeCast.safeIntToByte(idx));
        }
        return arr;
    }

}
