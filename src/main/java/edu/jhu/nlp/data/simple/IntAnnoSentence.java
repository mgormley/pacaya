package edu.jhu.nlp.data.simple;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.nlp.tag.StrictPosTagAnnotator.StrictPosTag;
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
    final static int MAX_PREFIX = SHORT_MAX;
    final static int MAX_LEMMA = SHORT_MAX;
    final static int MAX_POS = BYTE_MAX;
    final static int MAX_CPOS = BYTE_MAX;
    final static int MAX_CLUSTER = SHORT_MAX;
    final static int MAX_FEAT = SHORT_MAX;
    final static int MAX_DEPREL = BYTE_MAX;
    
    private ShortArrayList words;
    private ShortArrayList prefixes;
    private ShortArrayList lemmas;
    private ByteArrayList posTags;
    private ByteArrayList cposTags;
    private ShortArrayList clusters;
    private ArrayList<ShortArrayList> feats;
    private ByteArrayList deprels;
    // TODO: private IntNaryTree naryTree;
    
    private ByteArrayList coarserPosTags;
    private ShortArrayList numVerbsToLeft;
    private ShortArrayList numNounsToLeft;
    private ShortArrayList numPuncsToLeft;
    private ShortArrayList numConjsToLeft;
    
    private AnnoSentence sent;
    private AlphabetStore store;
    
    public IntAnnoSentence(AnnoSentence sent, AlphabetStore store) {
        this.sent = sent;
        this.store = store;
        this.words = getShorts(sent.getWords(), store.words);
        this.prefixes = getShorts(sent.getPrefixes(), store.prefixes);
        this.lemmas = getShorts(sent.getLemmas(), store.lemmas);
        this.posTags = getBytes(sent.getPosTags(), store.posTags);
        this.cposTags = getBytes(sent.getCposTags(), store.cposTags);
        this.clusters = getShorts(sent.getClusters(), store.clusters);
        if (sent.getFeats() != null) {
            feats = new ArrayList<>(sent.getFeats().size());
            for (List<String> featList : sent.getFeats()) {
                feats.add(getShorts(featList, store.feats));
            }
        }
        this.deprels = getBytes(sent.getDeprels(), store.deprels);
        if (StrictPosTag.values().length > BYTE_MAX) {
            throw new IllegalStateException("Too many strict POS tags.");
        }
        this.coarserPosTags = getBytesFromEnums(sent.getStrictPosTags());
        this.numVerbsToLeft = getNumToLeft(sent.getStrictPosTags(), StrictPosTag.VERB);
        this.numNounsToLeft = getNumToLeft(sent.getStrictPosTags(), StrictPosTag.NOUN);
        this.numPuncsToLeft = getNumToLeft(sent.getStrictPosTags(), StrictPosTag.PUNC);
        this.numConjsToLeft = getNumToLeft(sent.getStrictPosTags(), StrictPosTag.CONJ);
    }
    
    private static IntArrayList getInts(List<String> tokens, Alphabet<String> alphabet) {
        if (tokens == null) { return null; }
        IntArrayList arr = new IntArrayList(tokens.size());
        for (int i=0; i<tokens.size(); i++) {
            int idx = AlphabetStore.safeLookup(alphabet, tokens.get(i));
            arr.add(idx);
        }
        return arr;
    }
    
    private static ShortArrayList getShorts(List<String> tokens, Alphabet<String> alphabet) {
        if (tokens == null) { return null; }
        ShortArrayList arr = new ShortArrayList(tokens.size());
        for (int i=0; i<tokens.size(); i++) {
            int idx = AlphabetStore.safeLookup(alphabet, tokens.get(i));
            arr.add(SafeCast.safeIntToUnsignedShort(idx));
        }
        return arr;
    }
    
    private static ByteArrayList getBytes(List<String> tokens, Alphabet<String> alphabet) {
        if (tokens == null) { return null; }
        ByteArrayList arr = new ByteArrayList(tokens.size());
        for (int i=0; i<tokens.size(); i++) {
            int idx = AlphabetStore.safeLookup(alphabet, tokens.get(i));
            arr.add(SafeCast.safeIntToUnsignedByte(idx));
        }
        return arr;
    }
    
    private static ByteArrayList getBytesFromEnums(List<? extends Enum<?>> tokens) {
        if (tokens == null) { return null; }
        ByteArrayList arr = new ByteArrayList(tokens.size());
        for (int i=0; i<tokens.size(); i++) {
            int idx = tokens.get(i).ordinal();
            arr.add(SafeCast.safeIntToUnsignedByte(idx));
        }
        return arr;
    }

    private static <X> ShortArrayList getNumToLeft(List<X> tokens, X type) {
        if (tokens == null) { return null; }
        ShortArrayList arr = new ShortArrayList(tokens.size());
        int numSeen = 0;
        for (int i=0; i<=tokens.size(); i++) {
            arr.add(SafeCast.safeIntToShort(numSeen));
            if (i < tokens.size() && (type == tokens.get(i) || type.equals(tokens.get(i)))) {
                numSeen++;
            }
        }
        return arr;
    }

    
    /** Gets the i'th word as a String. */
    public short getWord(int i) {
        return words.get(i);
    }
    
    /** Gets the i'th word as a String. */
    public short getPrefix(int i) {
        return prefixes.get(i);
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
    
    /** Gets the number of verbs in between tokens a and b. */
    public short getNumVerbsInBetween(int a, int b) {
        return getNumInBetween(numVerbsToLeft, a, b);
    }

    /** Gets the number of nouns in between tokens a and b. */
    public short getNumNounsInBetween(int a, int b) {
        return getNumInBetween(numNounsToLeft, a, b);
    }

    /** Gets the number of punctuations in between tokens a and b. */
    public short getNumPuncsInBetween(int a, int b) {
        return getNumInBetween(numPuncsToLeft, a, b);
    }

    /** Gets the number of conjunctions in between tokens a and b. */
    public short getNumConjsInBetween(int a, int b) {
        return getNumInBetween(numConjsToLeft, a, b);
    }
    
    private static short getNumInBetween(ShortArrayList numToLeft, int a, int b) {
        if (b > a) {
            return SafeCast.safeIntToShort(numToLeft.get(b) - numToLeft.get(a+1));
        } else if (a > b) {
            return SafeCast.safeIntToShort(numToLeft.get(a) - numToLeft.get(b+1));
        } else {
            // a == b
            return 0;
        }
    }

    public int size() {
        return words.size();
    }
    
    public AnnoSentence getAnnoSentence() {
        return sent;
    }

}
