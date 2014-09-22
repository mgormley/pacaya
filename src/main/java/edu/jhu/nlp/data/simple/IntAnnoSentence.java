package edu.jhu.nlp.data.simple;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.prim.list.ByteArrayList;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.list.ShortArrayList;
import edu.jhu.prim.util.SafeCast;
import edu.jhu.util.Alphabet;

public class IntAnnoSentence {

    private static final int BYTE_MAX = 0xff;
    private static final int SHORT_MAX = 0xffff;
    // TODO: We're missing a bit because our Alphabets always return signed values. 
    private static final int INT_MAX = Integer.MAX_VALUE; //0xffffffff;

    final static int MAX_WORD = SHORT_MAX;
    final static int MAX_LEMMA = SHORT_MAX;
    final static int MAX_POS = BYTE_MAX;
    final static int MAX_CPOS = BYTE_MAX;
    final static int MAX_CLUSTER = SHORT_MAX;
    final static int MAX_FEAT = SHORT_MAX;
    final static int MAX_DEPREL = BYTE_MAX;
    
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
            arr.add(SafeCast.safeIntToUnsignedShort(idx));
        }
        return arr;
    }
    
    private static ByteArrayList getBytes(List<String> tokens, Alphabet<String> alphabet) {
        if (tokens == null) { return null; }
        ByteArrayList arr = new ByteArrayList();
        for (int i=0; i<tokens.size(); i++) {
            int idx = AlphabetStore.safeLookup(alphabet, tokens.get(i));
            arr.add(SafeCast.safeIntToUnsignedByte(idx));
        }
        return arr;
    }
    
    /** Gets the i'th word as a String. */
    public short getWord(int i) {
        return words.get(i);
    }
    
    /** Gets the i'th lemma as a String. */
    public short getLemma(int i) {
        return lemmas.get(i);
    }

    /** Gets the i'th POS tag as a String. */
    public byte getPosTag(int i) {
        return posTags.get(i);
    }
    
    /** Gets the i'th Coarse POS tag as a String. */
    public byte getCposTag(int i) {
        return cposTags.get(i);
    }

    /** Gets the i'th Distributional Similarity Cluster ID as a String. */
    public short getCluster(int i) {
        return clusters.get(i);
    }
    
    /** Gets the features (e.g. morphological features) of the i'th word. */
    public ShortArrayList getFeats(int i) {
        return feats.get(i);
    }

    /** Gets the dependency relation label for the arc from the i'th word to its parent. */
    public byte getDeprel(int i) {
        return deprels.get(i);
    }

}
