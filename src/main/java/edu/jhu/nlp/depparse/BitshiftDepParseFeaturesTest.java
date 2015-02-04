package edu.jhu.nlp.depparse;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.nlp.data.simple.AlphabetStore;
import edu.jhu.nlp.data.simple.AlphabetStoreTest;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.IntAnnoSentence;
import edu.jhu.nlp.depparse.BitshiftDepParseFeatureExtractor.BitshiftDepParseFeatureExtractorPrm;
import edu.jhu.nlp.depparse.BitshiftDepParseFeatures.ArcTs;
import edu.jhu.prim.set.LongHashSet;
import edu.jhu.prim.util.SafeCast;
import edu.jhu.prim.vector.LongDoubleUnsortedVector;
import edu.jhu.util.collections.Lists;


public class BitshiftDepParseFeaturesTest {

    private static class LongFeatureVector extends FeatureVector {
        
        LongDoubleUnsortedVector longs = new LongDoubleUnsortedVector();
        
        public void addLong(long index, double value) {
            longs.add(index, value);
        }
        
    }
    
    @Test
    public void testAddArcFeats() throws Exception {
        IntAnnoSentence isent = getIntAnnoSentence();
        BitshiftDepParseFeatureExtractorPrm prm = getDefaultBitshiftDepParseFeatureExtractorPrm();
        {
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addArcFeats(isent, 0, 3, prm, feats);
            checkNumFeatsBeforeAndAfterCompact(feats, 183, 183);
        }{
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addArcFeats(isent, -1, 2, prm, feats);
            checkNumFeatsBeforeAndAfterCompact(feats, 183, 183);
        }
        prm.useNonTurboFeats = true;
        {
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addArcFeats(isent, 0, 3, prm, feats);
            checkNumFeatsBeforeAndAfterCompact(feats, 187, 187);
        }
        prm.useMstFeats = true;
        {
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addArcFeats(isent, 0, 3, prm, feats);
            checkNumFeatsBeforeAndAfterCompact(feats, 98, 98);
        }{
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addArcFeats(isent, -1, 2, prm, feats);
            checkNumFeatsBeforeAndAfterCompact(feats, 98, 98);
        }
    }

    protected BitshiftDepParseFeatureExtractorPrm getDefaultBitshiftDepParseFeatureExtractorPrm() {
        BitshiftDepParseFeatureExtractorPrm prm = new BitshiftDepParseFeatureExtractorPrm();
        
        prm.useMstFeats = false;
        prm.isLabeledParsing = true;
        prm.maxTokenContext = 2;
        prm.useCoarseTags = true;
        prm.useLemmaFeats = true;
        prm.useMorphologicalFeatures = true;
        prm.featureHashMod = -1; // Only uses MurmurHash from long to int.
        
        // For 2nd-order only.
        prm.useNonprojGrandDepFeats = true;
        prm.useNonTurboFeats = false;
        prm.usePairFor2ndOrder = true;
        prm.usePairFor2ndOrderArbiSibl = true;
        prm.useTrilexicalFeats = true;
        prm.useUpperGrandDepFeats = true;
        
        return prm;
    }

    @Test
    public void testAddCarerrasSiblingFeats() throws Exception {
        IntAnnoSentence isent = getIntAnnoSentence();
        {
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addCarerrasSiblingFeats(isent, 0, 2, 3, feats, -1, false);
            checkNumFeatsBeforeAndAfterCompact(feats, 10, 10);
        }
    }
    
    @Test
    public void testAddCarerrasGrandparentFeats() throws Exception {
        IntAnnoSentence isent = getIntAnnoSentence();
        {
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addCarerrasGrandparentFeats(isent, 0, 2, 3, feats, -1);
            checkNumFeatsBeforeAndAfterCompact(feats, 10, 10);
        }
    }

    @Test
    public void testAddTurboConsecutiveSiblingFeats() throws Exception {
        IntAnnoSentence isent = getIntAnnoSentence();
        BitshiftDepParseFeatureExtractorPrm prm = getDefaultBitshiftDepParseFeatureExtractorPrm();
        prm.usePairFor2ndOrder = false;
        {
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addTurboConsecutiveSiblingFeats(isent, 0, 2, 3, feats, prm);
            checkNumFeatsBeforeAndAfterCompact(feats, 37, 37);
        }
        prm.usePairFor2ndOrder = true;
        {
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addTurboConsecutiveSiblingFeats(isent, 0, 2, 3, feats, prm);
            checkNumFeatsBeforeAndAfterCompact(feats, 203, 203);
        }
    }

    @Test
    public void testAddTurboArbitrarySiblingFeats() throws Exception {
        IntAnnoSentence isent = getIntAnnoSentence();
        BitshiftDepParseFeatureExtractorPrm prm = getDefaultBitshiftDepParseFeatureExtractorPrm();
        prm.usePairFor2ndOrder = false;
        {
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addTurboArbitrarySiblingFeats(isent, 0, 2, 3, feats, prm);
            checkNumFeatsBeforeAndAfterCompact(feats, 37, 37);
        }
        prm.usePairFor2ndOrder = true;
        {
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addTurboArbitrarySiblingFeats(isent, 0, 2, 3, feats, prm);
            checkNumFeatsBeforeAndAfterCompact(feats, 203, 203);
        }
    }

    @Test
    public void testAddTurboGrandparentFeats() throws Exception {
        IntAnnoSentence isent = getIntAnnoSentence();
        BitshiftDepParseFeatureExtractorPrm prm = getDefaultBitshiftDepParseFeatureExtractorPrm();
        prm.usePairFor2ndOrder = false;
        {
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addTurboGrandparentFeats(isent, 0, 2, 3, feats, prm);
            checkNumFeatsBeforeAndAfterCompact(feats, 37, 37);
        }
        prm.usePairFor2ndOrder = true;
        {
            FeatureVector feats = getFv();
            BitshiftDepParseFeatures.addTurboGrandparentFeats(isent, 0, 2, 3, feats, prm);
            checkNumFeatsBeforeAndAfterCompact(feats, 388, 388);
        }
    }

    protected FeatureVector getFv() {
        return new FeatureVector();
    }

    protected IntAnnoSentence getIntAnnoSentence() {
        AnnoSentence sent = AlphabetStoreTest.getAnnoSentenceForRange(0, 4);
        AlphabetStore store = new AlphabetStore(Lists.getList(sent));
        IntAnnoSentence isent = new IntAnnoSentence(sent, store);
        return isent;
    }

    private static void checkNumFeatsBeforeAndAfterCompact(FeatureVector feats, int expectedBefore, int expectedAfter) {
        if (feats instanceof LongFeatureVector) {
            LongDoubleUnsortedVector lfs = ((LongFeatureVector)feats).longs;
            int before = lfs.getUsed();
            {
                LongHashSet ls = new LongHashSet();
                long[] lfeats = lfs.getInternalIndices();
                for (int i=0; i<lfs.getUsed(); i++) {
                    if (ls.contains(lfeats[i])) {
                        System.out.printf("Known feature template: %d\n", (lfeats[i] & (long)0xff));
                        System.out.printf("hP: %d\n", SafeCast.safeUnsignedByteToInt(ArcTs.hP));
                    }
                    ls.add(lfeats[i]);
                }
            }
            lfs.compact();
            int after = lfs.getUsed();
            System.out.println(String.format("Num feats before %d and after %d compacting. Difference is %d.", before, after, before - after));
            assertEquals(expectedBefore, before);
            assertEquals(expectedAfter, after);
        } else {
            int before = feats.getUsed();
            feats.compact();
            int after = feats.getUsed();
            System.out.println(String.format("Num feats before %d and after %d compacting. Difference is %d.", before, after, before - after));
            assertEquals(expectedBefore, before);
            assertEquals(expectedAfter, after);            
        }
    }

}
