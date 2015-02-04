package edu.jhu.nlp.depparse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.nlp.data.simple.AlphabetStore;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.IntAnnoSentence;
import edu.jhu.nlp.depparse.BitshiftDepParseFeatureExtractor.BitshiftDepParseFeatureExtractorPrm;
import edu.jhu.prim.list.ShortArrayList;
import edu.jhu.prim.sort.ByteSort;
import edu.jhu.prim.sort.ShortSort;
import edu.jhu.prim.util.SafeCast;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.hash.MurmurHash;

// TODO: we should have a special token representing the wall. Instead we're using the int
// for the start of the sentence.
public class BitshiftDepParseFeatures {
    
    private static final Logger log = LoggerFactory.getLogger(BitshiftDepParseFeatures.class);     

    private static final int TOK_START_INT = AlphabetStore.TOK_START_INT;
    private static final int TOK_END_INT = AlphabetStore.TOK_END_INT;
    private static final int TOK_WALL_INT = AlphabetStore.TOK_WALL_INT;
    
    /**
     * IDs for feature collections.
     * @author mgormley
     */
    public static class FeatureCollection {
        
        public static int MAX_VAL = 0xf; // 4 bits
        private static int templ = 0;

        protected static byte next() {
            byte b = SafeCast.safeIntToUnsignedByte(templ++);
            if (b > MAX_VAL) { 
                throw new IllegalStateException("Too many feature collections.");
            }
            return b;
        }
        
        public static boolean isValid(byte type) {
            return (0 <= type && type <= MAX_VAL);
        }
        
        public static final byte ARC = next();
        public static final byte SIBLING = next();
        public static final byte GRANDPARENT = next();
        public static final byte CONS_SIBL = next();
        public static final byte ARBI_SIBL = next();
        public static final byte CONS_SIBL_M_S = next();
        public static final byte ARBI_SIBL_M_S = next();
        public static final byte GRANDPARENT_G_H = next();
        public static final byte GRANDPARENT_G_M = next();
        public static final byte GRANDPARENT_NONPROJ_H_M = next();
                
    }
    
    /** 
     * Template IDs for ARC features in {@link BitshiftDepParseFeatures}.
     * 
     * In the names below, we have the following mapping:
     * h := parent (head) 
     * m := child (modifier)
     * s := sibling
     * g := grandparent
     * l := token to the left of following position
     * r := token to the right of following position
     * btwn := each position between the head and modifier
     * 
     * W := word
     * P := POS tag
     * C := coarse POS tag
     * L := lemma
     * F := morphological feature
     * W5 := prefix of length 5
     */
    static class ArcTs {
        
        private static int templ = 0;

        protected static byte next() {
            return SafeCast.safeIntToUnsignedByte(templ++);
        }
        
        // McDonald et al. (2005) features templates.
        public static final byte hW = next();
        public static final byte mW = next();
        public static final byte BIAS = next();
        public static final byte DIR = next();
        public static final byte EXACT_DIST = next();
        public static final byte BIN_DIST = next();
        public static final byte hW_hP = next();
        public static final byte hP = next();
        public static final byte mW_mP = next();
        public static final byte mP = next();
        public static final byte hW_mW_hP_mP = next();
        public static final byte mW_hP_mP = next();
        public static final byte hW_mW_mP = next();
        public static final byte hW_hP_mP = next();
        public static final byte hW_mW_hP = next();
        public static final byte hW_mW = next();
        public static final byte hP_mP = next();
        public static final byte lhP_hP_lmP_mP = next();
        public static final byte lhP_hP_mP_rmP = next();
        public static final byte lhP_hP_mP = next();
        public static final byte hP_rhP_lmP_mP = next();
        public static final byte hP_rhP_mP_rmP = next();
        public static final byte hP_lmP_mP = next();
        public static final byte hP_mP_rmP = next();
        public static final byte hP_rhP_mP = next();
        public static final byte btwnP_hP_mP = next();
        public static final byte hW5_hP_mP = next();
        public static final byte hW5_hP = next();
        public static final byte hW5 = next();
        public static final byte mW5_hP_mP = next();
        public static final byte mW5_mP = next();
        public static final byte mW5 = next();
        public static final byte hW5_mW5_hP_mP = next();
        public static final byte hW5_mW5_mP = next();
        public static final byte hW5_mW5_hP = next();
        public static final byte hW5_mW5 = next();
        // Coarse POS tag versions of McDonald et al. (2005) features templates.
        public static final byte hW_hC = next();
        public static final byte hC = next();
        public static final byte mW_mC = next();
        public static final byte mC = next();
        public static final byte hW_mW_hC_mC = next();
        public static final byte mW_hC_mC = next();
        public static final byte hW_mW_mC = next();
        public static final byte hW_hC_mC = next();
        public static final byte hW_mW_hC = next();
        public static final byte hC_mC = next();
        public static final byte hC_rhC_lmC_mC = next();
        public static final byte lhC_hC_lmC_mC = next();
        public static final byte hC_rhC_mC_rmC = next();
        public static final byte lhC_hC_mC_rmC = next();
        public static final byte lhC_hC_mC = next();
        public static final byte hC_lmC_mC = next();
        public static final byte hC_mC_rmC = next();
        public static final byte hC_rhC_mC = next();
        public static final byte btwnP_hC_mC = next();
        public static final byte hW5_hC_mC = next();
        public static final byte hW5_hC = next();
        public static final byte mW5_hC_mC = next();
        public static final byte mW5_mC = next();
        public static final byte hW5_mW5_hC_mC = next();
        public static final byte hW5_mW5_mC = next();
        public static final byte hW5_mW5_hC = next();
        // Additional feature templates from TurboParser (Martins et al., 2013)
        public static final byte hL = next();
        public static final byte hF = next();
        public static final byte hW_hF = next();
        public static final byte mL = next();
        public static final byte mF = next();
        public static final byte mW_mF = next();
        public static final byte lhW = next();
        public static final byte lhP = next();
        public static final byte lhC = next();
        public static final byte lhL = next();
        public static final byte lhW_lhP = next();
        public static final byte lhW_lhC = next();
        public static final byte rhW = next();
        public static final byte rhP = next();
        public static final byte rhC = next();
        public static final byte rhL = next();
        public static final byte rhW_rhP = next();
        public static final byte rhW_rhC = next();
        public static final byte lmW = next();
        public static final byte lmP = next();
        public static final byte lmC = next();
        public static final byte lmL = next();
        public static final byte lmW_lmP = next();
        public static final byte lmW_lmC = next();
        public static final byte rmW = next();
        public static final byte rmP = next();
        public static final byte rmC = next();
        public static final byte rmL = next();
        public static final byte rmW_rmP = next();
        public static final byte rmW_rmC = next();
        public static final byte hP_lhP = next();
        public static final byte hP_lhP_llhP = next();
        public static final byte hP_rhP = next();
        public static final byte hP_rhP_rrhP = next();
        public static final byte hC_lhC = next();
        public static final byte llhW = next();
        public static final byte llhP = next();
        public static final byte llhC = next();
        public static final byte llhL = next();
        public static final byte llhW_llhP = next();
        public static final byte llhW_llhC = next();
        public static final byte rrhW = next();
        public static final byte rrhP = next();
        public static final byte rrhC = next();
        public static final byte rrhL = next();
        public static final byte rrhW_rrhP = next();
        public static final byte rrhW_rrhC = next();
        public static final byte llmW = next();
        public static final byte llmP = next();
        public static final byte llmC = next();
        public static final byte llmL = next();
        public static final byte llmW_llmC = next();
        public static final byte rrmW = next();
        public static final byte rrmP = next();
        public static final byte rrmC = next();
        public static final byte rrmL = next();
        public static final byte rrmW_rrmP = next();
        public static final byte rrmW_rrmC = next();
        public static final byte hC_lhC_llhC = next();
        public static final byte llmW_llmP = next();
        public static final byte hC_rhC = next();
        public static final byte hC_rhC_rrhC = next();
        public static final byte mP_lmP = next();
        public static final byte mP_lmP_llmP = next();
        public static final byte mP_rmP = next();
        public static final byte mP_rmP_rrmP = next();
        public static final byte mC_lmC = next();
        public static final byte mC_lmC_llmC = next();
        public static final byte mC_rmC = next();
        public static final byte mC_rmC_rrmC = next();
        public static final byte mW_hP = next();
        public static final byte hW_mP = next();
        public static final byte mW_hC = next();
        public static final byte hW_mC = next();
        public static final byte hF_mF = next();
        public static final byte hF_mP = next();
        public static final byte mF_hP = next();
        public static final byte hF_mF_mP = next();
        public static final byte hF_mF_hP = next();
        public static final byte hF_hP_mP = next();
        public static final byte mF_hP_mP = next();
        public static final byte hF_mF_hP_mP = next();
        public static final byte hF_mC = next();
        public static final byte mF_hC = next();
        public static final byte hF_mF_mC = next();
        public static final byte hF_mF_hC = next();
        public static final byte hF_hC_mC = next();
        public static final byte mF_hC_mC = next();
        public static final byte hF_mF_hC_mC = next();
        public static final byte lhP_hP_rhP_lmP_mP_rmP = next();
        public static final byte lhC_hC_rhC_lmC_mC_rmC = next();
        public static final byte BFLAG = next();
        public static final byte hP_mP_BFLAG = next();
        public static final byte hC_mC_BFLAG = next();
        public static final byte btwnP_hW_mW = next();
        public static final byte btwnP_hW_mP = next();
        public static final byte btwnP_hP_mW = next();
        public static final byte btwnC_hC_mC = next();
        public static final byte btwnC_hW_mC = next();
        public static final byte btwnC_hC_mW = next();
        public static final byte btwnC_hW_mW = next();
    }
    
    /** 
     * Template IDs for triplet features (e.g. SIBLING or GRANDPARENT) in {@link BitshiftDepParseFeatures}. 
     * 
     * See {@link ArcTs} for a description of the encoding of these feature template names.
     */
    private static class TriTs {
        private static int templ = 0;
        protected static byte next() {
            return SafeCast.safeIntToUnsignedByte(templ++);
        }
        // Carreras et al. (2007) templates.
        public static final byte BIAS = next();
        public static final byte hC_mC_sC = next();
        public static final byte hC_sC = next();
        public static final byte mC_sC = next();
        public static final byte hC_mC = next();
        public static final byte sW_hC = next();
        public static final byte sW_mC = next();
        public static final byte hW_sC = next();
        public static final byte mW_sC = next();
        public static final byte mW_hC = next();
        public static final byte hW_mC = next();
        public static final byte hW_sW = next();
        public static final byte mW_sW = next();
        public static final byte hW_mW = next();
        // Extra templates for Martins et al. (2013) templates.
        public static final byte hP_mP_sP = next();
        public static final byte hW_mP_sP = next();
        public static final byte hP_mW_sP = next();
        public static final byte hP_mP_sW = next();
        public static final byte hW_mW_sP = next();
        public static final byte hW_mP_sW= next();
        public static final byte hP_mW_sW= next();
        public static final byte hW_mW_sW= next();
        public static final byte hP_mP= next();
        public static final byte hP_sP= next();
        public static final byte mP_sP= next();
        public static final byte hW_sP= next();
        public static final byte hW_mP= next();
        public static final byte mW_sP= next();
        public static final byte mW_hP= next();
        public static final byte sW_hP= next();
        public static final byte sW_mP= next();
        public static final byte hW_mC_sC= next();
        public static final byte hC_mW_sC= next();
        public static final byte hC_mC_sW= next();
        public static final byte hW_mW_sC= next();
        public static final byte hW_mC_sW= next();
        public static final byte hC_mW_sW= next();
    }

    /** Returns the bin into which the given size falls. */
    public static int binInt(int size, int...bins) {
        for (int i=bins.length-1; i >= 0; i--) {
            if (size >= bins[i]) {
                return i;
            }
        }
        return bins.length;
    }
    
    private static final ShortArrayList WALL_MORPHO = new ShortArrayList();
    static {
        WALL_MORPHO.add((short)TOK_WALL_INT);
    }
    
    public static void addArcFeats(IntAnnoSentence sent, int head, int modifier, BitshiftDepParseFeatureExtractorPrm prm, FeatureVector feats) {
        if (prm.useMstFeats) {
            BitshiftDepParseFeatures.addArcFactoredMSTFeats(sent, head, modifier, FeatureCollection.ARC, feats, false, prm.useCoarseTags, prm.featureHashMod);
        } else {
            BitshiftDepParseFeatures.addTurboWordPairFeats(sent, head, modifier, FeatureCollection.ARC, feats, prm);
        }
    }
    

    /** Wrapper of {@link #addTurboWordPairFeats(IntAnnoSentence, int, int, byte, FeatureVector, int, int, boolean, boolean, boolean, boolean, boolean)}. */
    public static void addTurboWordPairFeats(final IntAnnoSentence sent, final int head, final int modifier, final byte pairType, 
            final FeatureVector feats, BitshiftDepParseFeatureExtractorPrm prm) {
        addTurboWordPairFeats(sent, head, modifier, pairType, feats, prm.featureHashMod, 
                prm.maxTokenContext, prm.isLabeledParsing, prm.useNonTurboFeats, prm.useLemmaFeats, prm.useMorphologicalFeatures, 
                prm.useCoarseTags);
    }
    
    /**
     * Word pair features from Martins et al. (2013) "Turning on the Turbo...". This feature set
     * draws from EGSTRA
     * 
     * Martins (personal correspondance) notes that TurboParser was found to perform best when the coarse
     * POS tags were replaced with just POS tags. Accordingly, in the feature set below, we define features in 
     * terms of POS tags, and offer an optional <code>if (useCoarsePosTags) { ... }</code> option for the cases 
     * where the original features were defined in TurboParser over coarse tags only.
     */
    public static void addTurboWordPairFeats(final IntAnnoSentence sent, final int head, final int modifier, final byte pairType, 
            final FeatureVector feats, final int mod,
            final int maxTokenContext,
            final boolean isLabeledParsing,
            final boolean useNonTurboFeats,
            final boolean useLemmaFeats,
            final boolean useMorphologicalFeatures,
            final boolean useCoarseTags) {
        int sentLen = sent.size();

        // Distance codes.
        int distance = (head < modifier) ? modifier - head : head - modifier;
        byte exactDistCode = SafeCast.safeIntToUnsignedByte((distance > 0xff) ? 0xff : distance);
        byte binDistCode; // = SafeCast.safeIntToUnsignedByte(binInt(sentLen, 0, 2, 5, 10, 20, 30, 40));
        // The order here matters since we'll add features for each applicable bin.
        if (distance > 40) {
            binDistCode = 6;
        } else if (distance > 30) {
            binDistCode = 5;
        } else if (distance > 20) {
            binDistCode = 4;
        } else if (distance > 10) {
            binDistCode = 3;
        } else if (distance > 5) {
            binDistCode = 2;
        } else if (distance > 2) {
            binDistCode = 1;
        } else {
            binDistCode = 0;
        }

        // Direction code and indices.
        byte direction = (head < modifier) ? (byte) 0 : (byte) 1;        
        int leftTok = (head < modifier) ? head : modifier;
        int rightTok = (head < modifier) ? modifier : head;
        
        // Number of certain POS tags in between.
        int numVerbsBetween = sent.getNumVerbsInBetween(head, modifier);
        int numPuncsBetween = sent.getNumPuncsInBetween(head, modifier);
        int numConjsBetween = sent.getNumConjsInBetween(head, modifier);
        // Use at most 4-bits to denote the number in between. 
        int maxOccurrences = 0xf; // 15
        if (numVerbsBetween > maxOccurrences) { numVerbsBetween = maxOccurrences; }
        if (numPuncsBetween > maxOccurrences) { numPuncsBetween = maxOccurrences; }
        if (numConjsBetween > maxOccurrences) { numConjsBetween = maxOccurrences; }
        // Use 4-bits to denote the type of tag between (e.g. verb, punc, conj)
        byte verbsBetweenCode = SafeCast.safeIntToUnsignedByte(0x0 | (numVerbsBetween << 4));
        byte puncsBetweenCode = SafeCast.safeIntToUnsignedByte(0x1 | (numPuncsBetween << 4));
        byte conjsBetweenCode = SafeCast.safeIntToUnsignedByte(0x2 | (numConjsBetween << 4));
        
        // Head (h) and modifier (m) words/tags/lemmas.
        short hWord = (head < 0) ? TOK_WALL_INT : sent.getWord(head);
        short mWord = (modifier < 0) ? TOK_WALL_INT : sent.getWord(modifier);
        byte hPos = (head < 0) ? TOK_WALL_INT : sent.getPosTag(head);
        byte mPos = (modifier < 0) ? TOK_WALL_INT : sent.getPosTag(modifier);
        short hLemma = (head < 0) ? TOK_WALL_INT : sent.getLemma(head);
        short mLemma = (modifier < 0) ? TOK_WALL_INT : sent.getLemma(modifier);
        byte hCpos = (head < 0) ? TOK_WALL_INT : sent.getCposTag(head);
        byte mCpos = (modifier < 0) ? TOK_WALL_INT : sent.getCposTag(modifier);

        // Surrounding words / POS tags. 
        // One token to the left (l) and right (r).
        short lhWord = (head-1 < 0) ? TOK_START_INT : sent.getWord(head-1);
        short lmWord = (modifier-1 < 0) ? TOK_START_INT : sent.getWord(modifier-1);
        short rhWord = (head+1 >= sentLen) ? TOK_END_INT : sent.getWord(head+1);
        short rmWord = (modifier+1 >= sentLen) ? TOK_END_INT : sent.getWord(modifier+1);
        //
        byte lhPos = (head-1 < 0) ? TOK_START_INT : sent.getPosTag(head-1);
        byte lmPos = (modifier-1 < 0) ? TOK_START_INT : sent.getPosTag(modifier-1);
        byte rhPos = (head+1 >= sentLen) ? TOK_END_INT : sent.getPosTag(head+1);
        byte rmPos = (modifier+1 >= sentLen) ? TOK_END_INT : sent.getPosTag(modifier+1);
        //
        short lhLemma = (head-1 < 0) ? TOK_START_INT : sent.getLemma(head-1);
        short lmLemma = (modifier-1 < 0) ? TOK_START_INT : sent.getLemma(modifier-1);
        short rhLemma = (head+1 >= sentLen) ? TOK_END_INT : sent.getLemma(head+1);
        short rmLemma = (modifier+1 >= sentLen) ? TOK_END_INT : sent.getLemma(modifier+1);
        //
        byte lhCpos = (head-1 < 0) ? TOK_START_INT : sent.getCposTag(head-1);
        byte lmCpos = (modifier-1 < 0) ? TOK_START_INT : sent.getCposTag(modifier-1);
        byte rhCpos = (head+1 >= sentLen) ? TOK_END_INT : sent.getCposTag(head+1);
        byte rmCpos = (modifier+1 >= sentLen) ? TOK_END_INT : sent.getCposTag(modifier+1);
        
        // Two tokens to the left (ll) and right (rr).
        short llhWord = (head-2 < 0) ? TOK_START_INT : sent.getWord(head-2);
        short llmWord = (modifier-2 < 0) ? TOK_START_INT : sent.getWord(modifier-2);
        short rrhWord = (head+2 >= sentLen) ? TOK_END_INT : sent.getWord(head+2);
        short rrmWord = (modifier+2 >= sentLen) ? TOK_END_INT : sent.getWord(modifier+2);
        //
        byte llhPos = (head-2 < 0) ? TOK_START_INT : sent.getPosTag(head-2);
        byte llmPos = (modifier-2 < 0) ? TOK_START_INT : sent.getPosTag(modifier-2);
        byte rrhPos = (head+2 >= sentLen) ? TOK_END_INT : sent.getPosTag(head+2);
        byte rrmPos = (modifier+2 >= sentLen) ? TOK_END_INT : sent.getPosTag(modifier+2);
        //
        short llhLemma = (head-2 < 0) ? TOK_START_INT : sent.getLemma(head-2);
        short llmLemma = (modifier-2 < 0) ? TOK_START_INT : sent.getLemma(modifier-2);
        short rrhLemma = (head+2 >= sentLen) ? TOK_END_INT : sent.getLemma(head+2);
        short rrmLemma = (modifier+2 >= sentLen) ? TOK_END_INT : sent.getLemma(modifier+2);
        //
        byte llhCpos = (head-2 < 0) ? TOK_START_INT : sent.getCposTag(head-2);
        byte llmCpos = (modifier-2 < 0) ? TOK_START_INT : sent.getCposTag(modifier-2);
        byte rrhCpos = (head+2 >= sentLen) ? TOK_END_INT : sent.getCposTag(head+2);
        byte rrmCpos = (modifier+2 >= sentLen) ? TOK_END_INT : sent.getCposTag(modifier+2); 
        
        // Flags for the type of feature.
        byte flags = pairType; // 4 bits.
        flags |= (direction << 4); // 1 more bit.

        // --------------------------------------------------------------------
        // Bias Feature.
        // --------------------------------------------------------------------        
        addFeat(feats, mod, encodeFeatureB___(ArcTs.BIAS, flags, (byte)0));

        // --------------------------------------------------------------------
        // Unigram Features of the Parent and Child tokens.
        // --------------------------------------------------------------------
        
        // Head Only.
        addFeat(feats, mod, encodeFeatureS___(ArcTs.hW, flags, hWord));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.hP, flags, hPos));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.hC, flags, hCpos));
        if (useLemmaFeats) {
            addFeat(feats, mod, encodeFeatureS___(ArcTs.hL, flags, hLemma));
        }
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.hW_hP, flags, hWord, hPos));
        if (useCoarseTags) {
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.hW_hC, flags, hWord, hCpos));            
        }
        if (useMorphologicalFeatures) {
            ShortArrayList hMorphosList = safeGetFeats(sent, head);
            short[] hMorphos = hMorphosList.getInternalElements();
            for (int j=0; j < hMorphosList.size(); j++) {
                short hMorpho = hMorphos[j];
                if (hMorpho > 0xfff) {
                    throw new IllegalStateException("Too many morphological feature values.");
                }
                if (hMorphosList.size() >= 0xf) {
                    log.warn("Too many morphological fetures: " + hMorphosList.size());
                    hMorpho = SafeCast.safeIntToShort((hMorpho << 4) | 0xf);
                } else {
                    hMorpho = SafeCast.safeIntToShort((hMorpho << 4) | j);
                }
                addFeat(feats, mod, encodeFeatureS___(ArcTs.hF, flags, hMorpho));
                addFeat(feats, mod, encodeFeatureSS__(ArcTs.hW_hF, flags, hWord, hMorpho));
            }
        }

        if (isLabeledParsing) {
            // Modifier Only.
            addFeat(feats, mod, encodeFeatureS___(ArcTs.mW, flags, mWord));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.mP, flags, mPos));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.mC, flags, mCpos));
            if (useLemmaFeats) {
                addFeat(feats, mod, encodeFeatureS___(ArcTs.mL, flags, mLemma));
            }            
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.mW_mP, flags, mWord, mPos));
            if (useCoarseTags) {
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.mW_mC, flags, mWord, mCpos));
            }
            if (useMorphologicalFeatures) {
                ShortArrayList mMorphosList = safeGetFeats(sent, modifier);
                short[] mMorphos = mMorphosList.getInternalElements();
                for (int k=0; k < mMorphosList.size(); k++) {
                    short mMorpho = mMorphos[k];
                    if (mMorpho > 0xfff) {
                        throw new IllegalStateException("Too many morphological feature values.");
                    }
                    if (mMorphosList.size() >= 0xf) {
                        log.warn("Too many morphological fetures: " + mMorphosList.size());
                        mMorpho = SafeCast.safeIntToShort((mMorpho << 4) | 0xf);
                    } else {
                        mMorpho = SafeCast.safeIntToShort((mMorpho << 4) | k);
                    }
                    addFeat(feats, mod, encodeFeatureS___(ArcTs.mF, flags, mMorpho));
                    addFeat(feats, mod, encodeFeatureSS__(ArcTs.mW_mF, flags, hWord, mMorpho));
                }
            }
        }
        
        // --------------------------------------------------------------------
        // Unigram Features of the words Left and Right of the Parent and Child. 
        // --------------------------------------------------------------------
                
        if (maxTokenContext >= 1) {
            // -- Head Context --
            // Word to the left.
            addFeat(feats, mod, encodeFeatureS___(ArcTs.lhW, flags, lhWord));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.lhP, flags, lhPos));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.lhC, flags, lhCpos));
            if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.lhL, flags, lhLemma)); }
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.lhW_lhP, flags, lhWord, lhPos));            
            if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.lhW_lhC, flags, lhWord, lhCpos)); } 
            // Word to the right.
            addFeat(feats, mod, encodeFeatureS___(ArcTs.rhW, flags, rhWord));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.rhP, flags, rhPos));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.rhC, flags, rhCpos));
            if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.rhL, flags, rhLemma)); }
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.rhW_rhP, flags, rhWord, rhPos));            
            if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.rhW_rhC, flags, rhWord, rhCpos)); } 
            // -- Modifier Context --
            // TurboParser excludes these features that look only at the modifier unless they are
            // also conjoined with the label.
            if (isLabeledParsing) {
                // Word to the left.
                addFeat(feats, mod, encodeFeatureS___(ArcTs.lmW, flags, lmWord));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.lmP, flags, lmPos));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.lmC, flags, lmCpos));
                if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.lmL, flags, lmLemma)); }
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.lmW_lmP, flags, lmWord, lmPos));                
                if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.lmW_lmC, flags, lmWord, lmCpos)); } 
                // Word to the right.
                addFeat(feats, mod, encodeFeatureS___(ArcTs.rmW, flags, rmWord));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.rmP, flags, rmPos));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.rmC, flags, rmCpos));
                if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.rmL, flags, rmLemma)); }
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.rmW_rmP, flags, rmWord, rmPos));                
                if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.rmW_rmC, flags, rmWord, rmCpos)); }
            }
        }
        
        if (maxTokenContext >= 2) {
            // -- Head Context --
            // Two words to the left.
            addFeat(feats, mod, encodeFeatureS___(ArcTs.llhW, flags, llhWord));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.llhP, flags, llhPos));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.llhC, flags, llhCpos));
            if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.llhL, flags, llhLemma)); }
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.llhW_llhP, flags, llhWord, llhPos));            
            if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.llhW_llhC, flags, llhWord, llhCpos)); } 
            // Two words to the right.
            addFeat(feats, mod, encodeFeatureS___(ArcTs.rrhW, flags, rrhWord));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.rrhP, flags, rrhPos));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.rrhC, flags, rrhCpos));
            if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.rrhL, flags, rrhLemma)); }
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.rrhW_rrhP, flags, rrhWord, rrhPos));            
            if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.rrhW_rrhC, flags, rrhWord, rrhCpos)); } 
            // -- Modifier Context --
            if (isLabeledParsing) {
                // Two words to the left.
                //
                // TurboParser excludes these features that look only at the modifier unless they are
                // also conjoined with the label.
                addFeat(feats, mod, encodeFeatureS___(ArcTs.llmW, flags, llmWord));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.llmP, flags, llmPos));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.llmC, flags, llmCpos));
                if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.llmL, flags, llmLemma)); }
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.llmW_llmP, flags, llmWord, llmPos));                 
                if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.llmW_llmC, flags, llmWord, llmCpos)); } 
                // Two words to the right.
                addFeat(feats, mod, encodeFeatureS___(ArcTs.rrmW, flags, rrmWord));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.rrmP, flags, rrmPos));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.rrmC, flags, rrmCpos));
                if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.rrmL, flags, rrmLemma)); }
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.rrmW_rrmP, flags, rrmWord, rrmPos));               
                if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.rrmW_rrmC, flags, rrmWord, rrmCpos)); }
            }
        }

        // --------------------------------------------------------------------
        // Sequential Bigram and Trigram Features of the words the Parent plus context 
        // or Child plus context. 
        // --------------------------------------------------------------------

        addFeat(feats, mod, encodeFeatureBB__(ArcTs.hP_lhP, flags, hPos, lhPos));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_lhP_llhP, flags, hPos, lhPos, llhPos));
        addFeat(feats, mod, encodeFeatureBB__(ArcTs.hP_rhP, flags, hPos, rhPos));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_rhP_rrhP, flags, hPos, rhPos, rrhPos));   
        if (useCoarseTags) {         
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.hC_lhC, flags, hCpos, lhCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_lhC_llhC, flags, hCpos, lhCpos, llhCpos));
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.hC_rhC, flags, hCpos, rhCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_rhC_rrhC, flags, hCpos, rhCpos, rrhCpos));
        }
        if (isLabeledParsing) {
            // TurboParser excludes these features that look only at the modifier unless they are
            // also conjoined with the label.
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.mP_lmP, flags, mPos, lmPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.mP_lmP_llmP, flags, mPos, lmPos, llmPos));
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.mP_rmP, flags, mPos, rmPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.mP_rmP_rrmP, flags, mPos, rmPos, rrmPos));   
            if (useCoarseTags) {         
                addFeat(feats, mod, encodeFeatureBB__(ArcTs.mC_lmC, flags, mCpos, lmCpos));
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.mC_lmC_llmC, flags, mCpos, lmCpos, llmCpos));
                addFeat(feats, mod, encodeFeatureBB__(ArcTs.mC_rmC, flags, mCpos, rmCpos));
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.mC_rmC_rrmC, flags, mCpos, rmCpos, rrmCpos));
            }
        }
        
        // --------------------------------------------------------------------
        // Bigram and Trigram features of the Parent plus Child.
        // --------------------------------------------------------------------

        // Words only.
        addFeat(feats, mod, encodeFeatureSS__(ArcTs.hW_mW, flags, hWord, mWord));
        // POS tags and Words.
        addFeat(feats, mod, encodeFeatureBB__(ArcTs.hP_mP, flags, hPos, mPos));
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.mW_hP, flags, mWord, hPos));
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.hW_mP, flags, hWord, mPos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.mW_hP_mP, flags, mWord, hPos, mPos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.hW_hP_mP, flags, hWord, hPos, mPos));
        addFeat(feats, mod, encodeFeatureSSBB(ArcTs.hW_mW_hP_mP, flags, hWord, mWord, hPos, mPos));       
        if (useCoarseTags) {
            // Coarse POS tags and Words.
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.hC_mC, flags, hCpos, mCpos));     
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.mW_hC, flags, mWord, hCpos));
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.hW_mC, flags, hWord, mCpos));
            addFeat(feats, mod, encodeFeatureSBB_(ArcTs.mW_hC_mC, flags, mWord, hCpos, mCpos));
            addFeat(feats, mod, encodeFeatureSBB_(ArcTs.hW_hC_mC, flags, hWord, hCpos, mCpos));        
            addFeat(feats, mod, encodeFeatureSSBB(ArcTs.hW_mW_hC_mC, flags, hWord, mWord, hCpos, mCpos));
        }
        if (useNonTurboFeats) {
            // Both Words plus a single Tag (from MST Parser).
            addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW_mW_mC, flags, hWord, mWord, mCpos));
            addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW_mW_hC, flags, hWord, mWord, hCpos));
            addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW_mW_mP, flags, hWord, mWord, mPos));
            addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW_mW_hP, flags, hWord, mWord, hPos));
        }
 
        if (useMorphologicalFeatures) {
            // For each morphological feature of the Head.
            ShortArrayList hMorphosList = safeGetFeats(sent, head);
            short[] hMorphos = hMorphosList.getInternalElements();
            for (int j=0; j < hMorphosList.size(); j++) {
                short hMorpho = hMorphos[j];
                if (hMorpho > 0xfff) {
                    throw new IllegalStateException("Too many morphological feature values.");
                }
                if (hMorphosList.size() >= 0xf) {
                    log.warn("Too many morphological fetures: " + hMorphosList.size());
                    hMorpho = SafeCast.safeIntToShort((hMorpho << 4) | 0xf);
                } else {
                    hMorpho = SafeCast.safeIntToShort((hMorpho << 4) | j);
                }
                // For each morphological feature of the Modifier.
                ShortArrayList mMorphosList = safeGetFeats(sent, modifier);
                short[] mMorphos = mMorphosList.getInternalElements();
                for (int k=0; k < mMorphosList.size(); k++) {
                    short mMorpho = mMorphos[k];
                    if (mMorpho > 0xfff) {
                        throw new IllegalStateException("Too many morphological feature values.");
                    }
                    if (mMorphosList.size() >= 0xf) {
                        log.warn("Too many morphological fetures: " + mMorphosList.size());
                        mMorpho = SafeCast.safeIntToShort((mMorpho << 4) | 0xf);
                    } else {
                        mMorpho = SafeCast.safeIntToShort((mMorpho << 4) | k);
                    }
                    addFeat(feats, mod, encodeFeatureSS__(ArcTs.hF_mF, flags, hMorpho, mMorpho));

                    addFeat(feats, mod, encodeFeatureSB__(ArcTs.hF_mP, flags, hMorpho, mPos));
                    addFeat(feats, mod, encodeFeatureSB__(ArcTs.mF_hP, flags, mMorpho, hPos));
                    addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hF_mF_mP, flags, hMorpho, mMorpho, mPos));
                    addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hF_mF_hP, flags, hMorpho, mMorpho, hPos));                    
                    addFeat(feats, mod, encodeFeatureSBB_(ArcTs.hF_hP_mP, flags, hMorpho, hPos, mPos));
                    addFeat(feats, mod, encodeFeatureSBB_(ArcTs.mF_hP_mP, flags, mMorpho, hPos, mPos));                    
                    addFeat(feats, mod, encodeFeatureSSBB(ArcTs.hF_mF_hP_mP, flags, hMorpho, mMorpho, hPos, mPos));
                    
                    if (useCoarseTags) {
                        addFeat(feats, mod, encodeFeatureSB__(ArcTs.hF_mC, flags, hMorpho, mCpos));
                        addFeat(feats, mod, encodeFeatureSB__(ArcTs.mF_hC, flags, mMorpho, hCpos));
                        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hF_mF_mC, flags, hMorpho, mMorpho, mCpos));
                        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hF_mF_hC, flags, hMorpho, mMorpho, hCpos));                    
                        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.hF_hC_mC, flags, hMorpho, hCpos, mCpos));
                        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.mF_hC_mC, flags, mMorpho, hCpos, mCpos));                    
                        addFeat(feats, mod, encodeFeatureSSBB(ArcTs.hF_mF_hC_mC, flags, hMorpho, mMorpho, hCpos, mCpos));
                    }
                }
            }
        }

        // Surrounding POS Features
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.lhP_hP_mP, flags, lhPos, hPos, mPos));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_lmP_mP, flags, hPos, lmPos, mPos));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_mP_rmP, flags, hPos, mPos, rmPos));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_rhP_mP, flags, hPos, rhPos, mPos));
        addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hP_rhP_lmP_mP, flags, hPos, rhPos, lmPos, mPos));
        addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lhP_hP_lmP_mP, flags, lhPos, hPos, lmPos, mPos));
        addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hP_rhP_mP_rmP, flags, hPos, rhPos, mPos, rmPos));
        addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lhP_hP_mP_rmP, flags, lhPos, hPos, mPos, rmPos));
        //    we don't backoff to the 5 tag case, i.e. BBBBB.
        addFeat(feats, mod, encodeFeatureBBBBBB(ArcTs.lhP_hP_rhP_lmP_mP_rmP, flags, lhPos, hPos, rhPos, lmPos, mPos, rmPos));       
        if (useCoarseTags) {
            // Surrounding Coarse POS Features
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.lhC_hC_mC, flags, lhCpos, hCpos, mCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_lmC_mC, flags, hCpos, lmCpos, mCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_mC_rmC, flags, hCpos, mCpos, rmCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_rhC_mC, flags, hCpos, rhCpos, mCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hC_rhC_lmC_mC, flags, hCpos, rhCpos, lmCpos, mCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lhC_hC_lmC_mC, flags, lhCpos, hCpos, lmCpos, mCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hC_rhC_mC_rmC, flags, hCpos, rhCpos, mCpos, rmCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lhC_hC_mC_rmC, flags, lhCpos, hCpos, mCpos, rmCpos));
            //    we don't backoff to the 5 tag case, i.e. BBBBB.
            addFeat(feats, mod, encodeFeatureBBBBBB(ArcTs.lhC_hC_rhC_lmC_mC_rmC, flags, lhCpos, hCpos, rhCpos, lmCpos, mCpos, rmCpos));
        }
        
        // The two cases of Adjacent Dependencies.
        if (head != -1 && head == modifier - 1) {
            // Parent is the token to the Left of the Child.
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lhP_hP_mP, flags, lhPos, hPos, mPos, (byte)0x1));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hP_mP_rmP, flags, hPos, mPos, rmPos, (byte)0x1));
            addFeat(feats, mod, encodeFeatureBBBBB(ArcTs.lhP_hP_mP_rmP, flags, lhPos, hPos, mPos, rmPos, (byte)0x1)); 
            if (useCoarseTags) {                   
                addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lhC_hC_mC, flags, lhCpos, hCpos, mCpos, (byte)0x1));
                addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hC_mC_rmC, flags, hCpos, mCpos, rmCpos, (byte)0x1));
                addFeat(feats, mod, encodeFeatureBBBBB(ArcTs.lhC_hC_mC_rmC, flags, lhCpos, hCpos, mCpos, rmCpos, (byte)0x1));
            }
        } else if (head != -1 && head == modifier + 1) {
            // Parent is the token to the Right of the Child.
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_lmP_mP, flags, hPos, lmPos, mPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_rhP_mP, flags, hPos, rhPos, mPos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hP_rhP_lmP_mP, flags, hPos, rhPos, lmPos, mPos)); 
            if (useCoarseTags) {                  
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_lmC_mC, flags, hCpos, lmCpos, mCpos));
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_rhC_mC, flags, hCpos, rhCpos, mCpos));
                addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hC_rhC_lmC_mC, flags, hCpos, rhCpos, lmCpos, mCpos));
            }
        }
        
        // --------------------------------------------------------------------
        // Arc Length Features.
        // --------------------------------------------------------------------

        addFeat(feats, mod, encodeFeatureB___(ArcTs.EXACT_DIST, flags, exactDistCode));
        // Add features for each applicable bin.
        for (byte bin = 0; bin <= binDistCode; bin++) {
            addFeat(feats, mod, encodeFeatureB___(ArcTs.BIN_DIST, flags, bin));
            // NOTE: It looks like TurboParser's approach to these flags actually leads to
            // collisions with the non-binned-distance uses of these templates. The extra 0x1 flag
            // ensures we avoid them.
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP, flags, hPos, bin, (byte) 0x1));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.mP, flags, mPos, bin, (byte)0x1));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hP_mP, flags, hPos, mPos, bin, (byte)0x1));  
            if (useCoarseTags) {
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC, flags, hCpos, bin, (byte)0x1));
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.mC, flags, mCpos, bin, (byte)0x1));
                addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hC_mC, flags, hCpos, mCpos, bin, (byte)0x1));                  
            }
        }

        // --------------------------------------------------------------------
        // Features on the tokens between Parent and Child.
        // --------------------------------------------------------------------
        
        addFeat(feats, mod, encodeFeatureB___(ArcTs.BFLAG, flags, verbsBetweenCode));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.BFLAG, flags, puncsBetweenCode));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.BFLAG, flags, conjsBetweenCode));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_mP_BFLAG, flags, hPos, mPos, verbsBetweenCode));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_mP_BFLAG, flags, hPos, mPos, puncsBetweenCode));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_mP_BFLAG, flags, hPos, mPos, conjsBetweenCode));
        if (useCoarseTags) {
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_mC_BFLAG, flags, hCpos, mCpos, verbsBetweenCode));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_mC_BFLAG, flags, hCpos, mCpos, puncsBetweenCode));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_mC_BFLAG, flags, hCpos, mCpos, conjsBetweenCode));
        }
           
        // In Between POS Features.
        byte[] btwnPos = new byte[distance+1];
        int j = 0;
        for (int i=leftTok; i<=rightTok; i++) {
            btwnPos[j++] = (i < 0) ? TOK_START_INT : sent.getPosTag(i);
        }
        ByteSort.sortAsc(btwnPos);
        for (int i=0; i<btwnPos.length; i++) {
            if (i == 0 || btwnPos[i] != btwnPos[i-1]) {
                addFeat(feats, mod, encodeFeatureSSB_(ArcTs.btwnP_hW_mW, flags, hWord, mWord, btwnPos[i]));
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.btwnP_hP_mP, flags, hPos, mPos, btwnPos[i]));
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.btwnP_hW_mP, flags, hWord, mPos, btwnPos[i]));
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.btwnP_hP_mW, flags, mWord, hPos, btwnPos[i]));
            }
        }
        if (useCoarseTags) {
            // In Between Coarse POS Features.
            byte[] btwnCpos = new byte[distance+1];
            j = 0;
            for (int i=leftTok; i<=rightTok; i++) {
                btwnCpos[j++] = (i < 0) ? TOK_START_INT : sent.getCposTag(i);
            }
            ByteSort.sortAsc(btwnCpos);
            for (int i=0; i<btwnCpos.length; i++) {
                if (i == 0 || btwnCpos[i] != btwnCpos[i-1]) {
                    addFeat(feats, mod, encodeFeatureSSB_(ArcTs.btwnC_hW_mW, flags, hWord, mWord, btwnCpos[i]));
                    addFeat(feats, mod, encodeFeatureBBB_(ArcTs.btwnC_hC_mC, flags, hCpos, mCpos, btwnCpos[i]));
                    addFeat(feats, mod, encodeFeatureSBB_(ArcTs.btwnC_hW_mC, flags, hWord, mCpos, btwnCpos[i]));
                    addFeat(feats, mod, encodeFeatureSBB_(ArcTs.btwnC_hC_mW, flags, mWord, hCpos, btwnCpos[i]));   
                }
            }                          
        }
    }

    private static ShortArrayList safeGetFeats(IntAnnoSentence sent, int idx) {
        if (idx >= 0) { 
            return sent.getFeats(idx); 
        } else {
            return WALL_MORPHO;
        }
    }
    
    /** Features from McDonald et al. (2005) "Online Large-Margin Training of Dependency Parsers." */
    public static void addArcFactoredMSTFeats(IntAnnoSentence sent, int head, int modifier, byte pairType,  
            FeatureVector feats, boolean basicOnly, boolean useCoarseFeats, int mod) {
        // Head and modifier words / POS tags. We denote the head by p (for parent) and the modifier
        // by c (for child).
        short hWord = (head < 0) ? TOK_WALL_INT : sent.getWord(head);
        short mWord = (modifier < 0) ? TOK_WALL_INT : sent.getWord(modifier);
        byte hPos = (head < 0) ? TOK_WALL_INT : sent.getPosTag(head);
        byte mPos = (modifier < 0) ? TOK_WALL_INT : sent.getPosTag(modifier);
        // 5-character prefixes.
        short hPrefix = (head < 0) ? TOK_WALL_INT : sent.getPrefix(head);
        short mPrefix = (modifier < 0) ? TOK_WALL_INT : sent.getPrefix(modifier);
        // Whether to include features for the 5-char prefixes.
        AnnoSentence aSent = sent.getAnnoSentence();
        boolean hPrefixFeats = (head < 0) ? false : aSent.getWord(head).length() > 5;
        boolean mPrefixFeats = (modifier < 0) ? false : aSent.getWord(modifier).length() > 5;
        
        // Surrounding words / POS tags. 
        int sentLen = sent.size();
        byte lhPos = (head-1 < 0) ? TOK_START_INT : sent.getPosTag(head-1);
        byte lmPos = (modifier-1 < 0) ? TOK_START_INT : sent.getPosTag(modifier-1);
        byte rhPos = (head+1 >= sentLen) ? TOK_END_INT : sent.getPosTag(head+1);
        byte rmPos = (modifier+1 >= sentLen) ? TOK_END_INT : sent.getPosTag(modifier+1);
        
        int distance = (head < modifier) ? modifier - head : head - modifier;
        // The bins are: 0, 2, 5, 10, 20, 30, 40. 
        // We reserve the 7th bin to indicate mode == 0.
        byte binDistCode; 
        if (distance >= 40) {
            binDistCode = 6;
        } else if (distance >= 30) {
            binDistCode = 5;
        } else if (distance >= 20) {
            binDistCode = 4;
        } else if (distance >= 10) {
            binDistCode = 3;
        } else if (distance >= 5) {
            binDistCode = 2;
        } else if (distance >= 2) {
            binDistCode = 1;
        } else {
            binDistCode = 0;
        }

        byte direction = (head < modifier) ? (byte) 0 : (byte) 1;        
        
        for (byte mode = 0; mode < 2; mode++) {
            assert FeatureCollection.isValid(pairType);
            byte flags = pairType; // 4 bits.
            // We used to do this: "flags |= mode << 4; // 1 bit." but instead are packing the mode
            // into the binDistCode as the last value.
            if (mode == 1) {
                //    # All features in Table 1 were conjoined with *direction* of attachment and *distance*.
                flags |= direction << 4; // 1 bit.
                flags |= binDistCode << 5; // 3 bits. (8 total)
                assert binDistCode < 7;
            } else {
                flags |= 0 << 4; // 1 bit. 
                flags |= 7 << 5; // 3 bits (8 total).
            }
            
            extractMstFeaturesWithPos(sent, head, modifier, feats, basicOnly, hWord, mWord, hPos, mPos, hPrefix, mPrefix,
                    hPrefixFeats, mPrefixFeats, lhPos, lmPos, rhPos, rmPos, distance, binDistCode, direction, mode,
                    flags, mod);
            if (useCoarseFeats) {
                extractMstFeaturesWithCpos(sent, head, modifier, feats, basicOnly, hWord, mWord, hPos, mPos, hPrefix, mPrefix,
                        hPrefixFeats, mPrefixFeats, lhPos, lmPos, rhPos, rmPos, distance, binDistCode, direction, mode,
                        flags, mod);
            }
        }
    }

    /** Regular POS tag versions of the MST features. */
    private static void extractMstFeaturesWithPos(IntAnnoSentence sent, int head, int modifier, FeatureVector feats,
            boolean basicOnly, short hWord, short mWord, byte hPos, byte mPos, short hPrefix, short mPrefix,
            boolean hPrefixFeats, boolean mPrefixFeats, byte lhPos, byte lmPos, byte rhPos, byte rmPos, int distance,
            byte binnedDist, byte direction, byte mode, byte flags, int mod) {
        // Bias features.
        //    # TODO: It's not clear whether these were included in McDonald et al. (2005), 
        //    # but Koo et al. (2008) had them.
        addFeat(feats, mod, encodeFeatureB___(ArcTs.BIAS, flags, (byte)0));
        if (mode == 0) {
            addFeat(feats, mod, encodeFeatureB___(ArcTs.DIR, flags, direction));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.BIN_DIST, flags, binnedDist));
        }
        
        //    # Basic Unigram Features
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.hW_hP, flags, hWord, hPos));
        addFeat(feats, mod, encodeFeatureS___(ArcTs.hW, flags, hWord));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.hP, flags, hPos));
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.mW_mP, flags, mWord, mPos));
        addFeat(feats, mod, encodeFeatureS___(ArcTs.mW, flags, mWord));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.mP, flags, mPos));
        
        //    # Basic Bigram Features
        addFeat(feats, mod, encodeFeatureSSBB(ArcTs.hW_mW_hP_mP, flags, hWord, mWord, hPos, mPos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.mW_hP_mP, flags, mWord, hPos, mPos));
        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW_mW_mP, flags, hWord, mWord, mPos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.hW_hP_mP, flags, hWord, hPos, mPos));
        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW_mW_hP, flags, hWord, mWord, hPos));
        addFeat(feats, mod, encodeFeatureSS__(ArcTs.hW_mW, flags, hWord, mWord));
        addFeat(feats, mod, encodeFeatureBB__(ArcTs.hP_mP, flags, hPos, mPos));            
        
        if (!basicOnly) {
            //    # Surrounding Word POS Features
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hP_rhP_lmP_mP, flags, hPos, rhPos, lmPos, mPos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lhP_hP_lmP_mP, flags, lhPos, hPos, lmPos, mPos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hP_rhP_mP_rmP, flags, hPos, rhPos, mPos, rmPos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lhP_hP_mP_rmP, flags, lhPos, hPos, mPos, rmPos));
            
            //    # Backed-off versions of Surrounding Word POS Features
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.lhP_hP_mP, flags, lhPos, hPos, mPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_lmP_mP, flags, hPos, lmPos, mPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_mP_rmP, flags, hPos, mPos, rmPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hP_rhP_mP, flags, hPos, rhPos, mPos));
            
            //    # In Between POS Features
            int leftTok  = (head < modifier) ? head : modifier;
            int rightTok = (head > modifier) ? head : modifier;
            byte[] btwnPos = new byte[distance+1];
            int j = 0;
            for (int i=leftTok; i<=rightTok; i++) {
                btwnPos[j++] = (i < 0) ? TOK_START_INT : sent.getPosTag(i);
            }
            ByteSort.sortAsc(btwnPos);
            for (int i=0; i<btwnPos.length; i++) {
                if (i == 0 || btwnPos[i] != btwnPos[i-1]) {
                    addFeat(feats, mod, encodeFeatureBBB_(ArcTs.btwnP_hP_mP, flags, btwnPos[i], hPos, mPos));
                }
            }
            
            //    # These features are added for both the entire words as well as the
            //    # 5-gram prefix if the word is longer than 5 characters.
            if (hPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.hW5_hP_mP, flags, hPrefix, hPos, mPos));
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.hW5_hP, flags, hPrefix, hPos));
                addFeat(feats, mod, encodeFeatureS___(ArcTs.hW5, flags, hPrefix));
            }
            if (mPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.mW5_hP_mP, flags, mPrefix, hPos, mPos));
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.mW5_mP, flags, mPrefix, mPos));
                addFeat(feats, mod, encodeFeatureS___(ArcTs.mW5, flags, mPrefix));
            }
            if (hPrefixFeats || mPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSSBB(ArcTs.hW5_mW5_hP_mP, flags, hPrefix, mPrefix, hPos, mPos));
                addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW5_mW5_mP, flags, hPrefix, mPrefix, mPos));
                addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW5_mW5_hP, flags, hPrefix, mPrefix, hPos));
                addFeat(feats, mod, encodeFeatureSS__(ArcTs.hW5_mW5, flags, hPrefix, mPrefix));
            }
        }
    }
    
    /** Coarse POS tag versions of the MST features. */
    private static void extractMstFeaturesWithCpos(IntAnnoSentence sent, int head, int modifier, FeatureVector feats,
            boolean basicOnly, short hWord, short mWord, byte hPos_NOTUSED, byte mPos_NOTUSED, short hPrefix,
            short mPrefix, boolean hPrefixFeats, boolean mPrefixFeats, byte lhPos_NOTUSED, byte lmPos_NOTUSED, 
            byte rhPos_NOTUSED, byte rmPos_NOTUSED,
            int distance, byte binnedDist, byte direction, byte mode, byte flags, int mod) {
        byte hCpos = (head < 0) ? TOK_WALL_INT : sent.getCposTag(head);
        byte mCpos = (modifier < 0) ? TOK_WALL_INT : sent.getCposTag(modifier);

        // Surrounding words / POS tags. 
        int sentLen = sent.size();
        byte lhCpos = (head-1 < 0) ? TOK_START_INT : sent.getCposTag(head-1);
        byte lmCpos = (modifier-1 < 0) ? TOK_START_INT : sent.getCposTag(modifier-1);
        byte rhCpos = (head+1 >= sentLen) ? TOK_END_INT : sent.getCposTag(head+1);
        byte rmCpos = (modifier+1 >= sentLen) ? TOK_END_INT : sent.getCposTag(modifier+1);
        
        //    # Basic Unigram Features
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.hW_hC, flags, hWord, hCpos));
        // word only: addFeat(feats, encodeFeatureS___(ArcTs.hW, flags, hWord));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.hC, flags, hCpos));
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.mW_mC, flags, mWord, mCpos));
        // word only: addFeat(feats, encodeFeatureS___(ArcTs.mW, flags, mWord));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.mC, flags, mCpos));
        
        //    # Basic Bigram Features
        addFeat(feats, mod, encodeFeatureSSBB(ArcTs.hW_mW_hC_mC, flags, hWord, mWord, hCpos, mCpos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.mW_hC_mC, flags, mWord, hCpos, mCpos));
        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW_mW_mC, flags, hWord, mWord, mCpos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.hW_hC_mC, flags, hWord, hCpos, mCpos));
        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW_mW_hC, flags, hWord, mWord, hCpos));
        // word only: addFeat(feats, encodeFeatureSS__(ArcTs.hW_mW, flags, hWord, mWord));
        addFeat(feats, mod, encodeFeatureBB__(ArcTs.hC_mC, flags, hCpos, mCpos));            
        
        if (!basicOnly) {            
            //    # Surrounding Word POS Features
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hC_rhC_lmC_mC, flags, hCpos, rhCpos, lmCpos, mCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lhC_hC_lmC_mC, flags, lhCpos, hCpos, lmCpos, mCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.hC_rhC_mC_rmC, flags, hCpos, rhCpos, mCpos, rmCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lhC_hC_mC_rmC, flags, lhCpos, hCpos, mCpos, rmCpos));
            
            //    # Backed-off versions of Surrounding Word POS Features
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.lhC_hC_mC, flags, lhCpos, hCpos, mCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_lmC_mC, flags, hCpos, lmCpos, mCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_mC_rmC, flags, hCpos, mCpos, rmCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.hC_rhC_mC, flags, hCpos, rhCpos, mCpos));
            
            //    # In Between POS Features
            int leftTok  = (head < modifier) ? head : modifier;
            int rightTok = (head > modifier) ? head : modifier;
            // TODO: Switch to bytes.
            short[] btwnPos = new short[distance+1];
            int j = 0;
            for (int i=leftTok; i<=rightTok; i++) {
                btwnPos[j++] = (i < 0) ? TOK_START_INT : sent.getPosTag(i);
            }
            ShortSort.sortAsc(btwnPos);
            for (int i=0; i<btwnPos.length; i++) {
                if (i == 0 || btwnPos[i] != btwnPos[i-1]) {
                    addFeat(feats, mod, encodeFeatureSBB_(ArcTs.btwnP_hC_mC, flags, btwnPos[i], hCpos, mCpos));
                }
            }
            
            //    # These features are added for both the entire words as well as the
            //    # 5-gram prefix if the word is longer than 5 characters.
            if (hPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.hW5_hC_mC, flags, hPrefix, hCpos, mCpos));
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.hW5_hC, flags, hPrefix, hCpos));
                // word only: addFeat(feats, encodeFeatureS___(ArcTs.hW5, flags, hPrefix));
            }
            if (mPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.mW5_hC_mC, flags, mPrefix, hCpos, mCpos));
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.mW5_mC, flags, mPrefix, mCpos));
                // word only: addFeat(feats, encodeFeatureS___(ArcTs.mW5, flags, mPrefix));
            }
            if (hPrefixFeats || mPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSSBB(ArcTs.hW5_mW5_hC_mC, flags, hPrefix, mPrefix, hCpos, mCpos));
                addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW5_mW5_mC, flags, hPrefix, mPrefix, mCpos));
                addFeat(feats, mod, encodeFeatureSSB_(ArcTs.hW5_mW5_hC, flags, hPrefix, mPrefix, hCpos));
                // word only: addFeat(feats, encodeFeatureSS__(ArcTs.hW5_mW5, flags, hPrefix, mPrefix));
            }
        }
    }

    public static void addArbitrarySiblingFeats(IntAnnoSentence sent, int head, int modifier, int sibling,
            FeatureVector feats, BitshiftDepParseFeatureExtractorPrm prm) {
        if (prm.useCarerrasFeats) {
            addCarerrasSiblingFeats(sent, head, modifier, sibling, feats, prm.featureHashMod, false);
        } else {
            addTurboArbitrarySiblingFeats(sent, head, modifier, sibling, feats, prm);
        }
    }

    public static void addConsecutiveSiblingFeats(IntAnnoSentence sent, int head, int modifier, int sibling,
            FeatureVector feats, BitshiftDepParseFeatureExtractorPrm prm) {
        if (prm.useCarerrasFeats) {
            addCarerrasSiblingFeats(sent, head, modifier, sibling, feats, prm.featureHashMod, true);
        } else {
            addTurboConsecutiveSiblingFeats(sent, head, modifier, sibling, feats, prm);
        }
    }
    
    public static void addGrandparentFeats(IntAnnoSentence sent, int grandparent, int head, int modifier,
            FeatureVector feats, BitshiftDepParseFeatureExtractorPrm prm) {
        if (prm.useCarerrasFeats) {
            addCarerrasGrandparentFeats(sent, grandparent, head, modifier, feats, prm.featureHashMod);
        } else {
            addTurboGrandparentFeats(sent, grandparent, head, modifier, feats, prm);
        }
    }
    
    /**
     * This is similar to the 2nd order features from Carerras et al. (2007), but incorporates some
     * features from Martins' TurboParser.
     */
    public static void addCarerrasSiblingFeats(IntAnnoSentence sent, int head, int modifier, int sibling, FeatureVector feats, int mod, boolean consecutive) {
        // Direction flags.
        // Parent-child relationship.
        byte direction_pc = (head < modifier) ? (byte) 0 : (byte) 1;
        // Parent-sibling relationship.
        byte direction_ps = (head < sibling) ? (byte) 0 : (byte) 1;
                
        byte flags = consecutive ? FeatureCollection.CONS_SIBL : FeatureCollection.ARBI_SIBL; // 4 bits.
        flags |= direction_pc << 4; // 1 bit.
        flags |= direction_ps << 5; // 1 bit.
        
        addCarerrasTripletFeatures(sent, head, modifier, sibling, feats, flags, mod);
    }
    
    /**
     * This is similar to the 2nd order features from Carerras et al. (2007), but incorporates some
     * features from Martins' TurboParser.
     */
    public static void addCarerrasGrandparentFeats(IntAnnoSentence sent, int grandparent, int head, int modifier, FeatureVector feats, int mod) {
        byte direction = getGrandparentDirectionCode(grandparent, head, modifier);        
        byte flags = FeatureCollection.GRANDPARENT; // 4 bits.
        flags |= direction << 4; // 2 bits.        
        addCarerrasTripletFeatures(sent, grandparent, head, modifier, feats, flags, mod);
    }

    /** Gets a 2-bit grandparent direction code. */
    protected static byte getGrandparentDirectionCode(int grandparent, int head, int modifier) {
        // Direction flags.
        // Parent-grandparent relationship.
        byte direction_gp = (grandparent < head) ? (byte) 0 : (byte) 1;
        // Parent-child relationship.
        byte direction_pc = (head < modifier) ? (byte) 0 : (byte) 1;
        // Grandparent-child relationship.
        byte direction_gc = (grandparent < modifier) ? (byte) 0 : (byte) 1;
               
        // Use the direction code from Martins' TurboParser.
        byte direction;
        if (direction_gp == direction_pc) {
            // Pointing in the same direction.
            direction = 0;
        } else if (direction_pc != direction_gc) {
            // Projective with c inside range [g, p].
            direction = 1;
        } else {
            // Non-projective with c outside range [g, p].
            direction = 2;
        }
        return direction;
    }

    // Extra triplets are from TurboParser and can be beneficial because of the flags with which they are conjoined.
    public static final boolean extraTriplets = false;
    
    /** Can be used for either sibling or grandparent features. */
    private static void addCarerrasTripletFeatures(IntAnnoSentence sent, int head, int modifier, int sibling, FeatureVector feats, byte flags, int mod) {
        // Head, modifier, and sibling words / POS tags. We denote the head by p (for parent), the modifier
        // by c (for child), and the sibling by s.
        short hWord = (head < 0) ? TOK_WALL_INT : sent.getWord(head);
        short mWord = (modifier < 0) ? TOK_WALL_INT : sent.getWord(modifier);
        short sWord = (sibling < 0) ? TOK_WALL_INT : sent.getWord(sibling);
        // Use coarse POS tags.
        byte hCpos = (head < 0) ? TOK_WALL_INT : sent.getCposTag(head);
        byte mCpos = (modifier < 0) ? TOK_WALL_INT : sent.getCposTag(modifier);
        byte sCpos = (sibling < 0) ? TOK_WALL_INT : sent.getCposTag(sibling);
                
        // --- Triplet features. ----
        addFeat(feats, mod, encodeFeatureB___(TriTs.BIAS, flags, (byte)0));

        //    cpos(p) + cpos(c) + cpos(s)
        addFeat(feats, mod, encodeFeatureBBB_(TriTs.hC_mC_sC, flags, hCpos, mCpos, sCpos));

        // --- Pairwise features. ----
        
        //    cpos(p) + cpos(s)
        //    cpos(c) + cpos(s)
        //    cpos(p) + cpos(c) << Not in Carreras. From TurboParser.
        addFeat(feats, mod, encodeFeatureBB__(TriTs.hC_sC, flags, hCpos, sCpos));
        addFeat(feats, mod, encodeFeatureBB__(TriTs.mC_sC, flags, mCpos, sCpos));
        if (extraTriplets) {
            addFeat(feats, mod, encodeFeatureBB__(TriTs.hC_mC, flags, hCpos, mCpos));
        }

        //    cpos(p) + word(s)
        //    cpos(c) + word(s)
        //    word(p) + cpos(s)
        //    word(c) + cpos(s)
        //    word(p) + cpos(c) << Not in Carreras. From TurboParser.
        //    word(c) + cpos(p) << Not in Carreras. From TurboParser.
        addFeat(feats, mod, encodeFeatureSB__(TriTs.sW_hC, flags, sWord, hCpos));
        addFeat(feats, mod, encodeFeatureSB__(TriTs.sW_mC, flags, sWord, mCpos));
        addFeat(feats, mod, encodeFeatureSB__(TriTs.hW_sC, flags, hWord, sCpos));
        addFeat(feats, mod, encodeFeatureSB__(TriTs.mW_sC, flags, mWord, sCpos));
        if (extraTriplets) {
            addFeat(feats, mod, encodeFeatureSB__(TriTs.mW_hC, flags, mWord, hCpos));
            addFeat(feats, mod, encodeFeatureSB__(TriTs.hW_mC, flags, hWord, mCpos));
        }

        //    word(p) + word(s)
        //    word(c) + word(s)
        //    word(p) + word(c) << Not in Carreras. From TurboParser.
        addFeat(feats, mod, encodeFeatureSS__(TriTs.hW_sW, flags, hWord, sWord));
        addFeat(feats, mod, encodeFeatureSS__(TriTs.mW_sW, flags, mWord, sWord));
        if (extraTriplets) {
            addFeat(feats, mod, encodeFeatureSS__(TriTs.hW_mW, flags, hWord, mWord));
        }
    }

    /** Adds the features for consecutive siblings from TurboParser (Martins et al., 2013). */
    public static void addTurboConsecutiveSiblingFeats(final IntAnnoSentence sent, final int head, final int modifier, final int sibling, 
            final FeatureVector feats, BitshiftDepParseFeatureExtractorPrm prm) {
        addTurboSiblingFeats(sent, head, modifier, sibling, feats, prm, true);
    }
    
    /** Adds the features for arbitrary siblings from TurboParser (Martins et al., 2013). */
    public static void addTurboArbitrarySiblingFeats(final IntAnnoSentence sent, final int head, final int modifier, final int sibling, 
            final FeatureVector feats, BitshiftDepParseFeatureExtractorPrm prm) {
        addTurboSiblingFeats(sent, head, modifier, sibling, feats, prm, false);
    }
    
    /** Can be used for any type of sibling features. */
    private static void addTurboSiblingFeats(final IntAnnoSentence sent, final int head, final int modifier, final int sibling, 
            final FeatureVector feats, BitshiftDepParseFeatureExtractorPrm prm, final boolean consecutive) {
        final int mod = prm.featureHashMod;
        final boolean useCoarseTags = prm.useCoarseTags;
        final boolean useTrilexicalFeats = prm.useTrilexicalFeats;
        final boolean usePairFor2ndOrder = prm.usePairFor2ndOrder;
        final boolean usePairFor2ndOrderArbiSibl = prm.usePairFor2ndOrderArbiSibl;

        // The sibling is the first child of the head. Encoded by setting the modifier to -1.
        // UNUSED: boolean isFirstChild = consecutive && modifier < 0;
        // The modifier is the last child of the head. Encoded by setting the sibling to -1.
        // UNUSED: boolean isLastChild = consecutive && sibling < 0;
        
        if (usePairFor2ndOrder) {
            byte pairType = consecutive ? FeatureCollection.CONS_SIBL_M_S : FeatureCollection.ARBI_SIBL_M_S;
            if (consecutive || usePairFor2ndOrderArbiSibl) {
                addTurboWordPairFeats(sent, modifier, sibling, pairType, feats, prm);
            }
        }

        // Direction code and indices.
        byte directionFirst = (head < modifier) ? (byte) 0 : (byte) 1;
        byte directionSecond = (head < sibling) ? (byte) 0 : (byte) 1;
        
        // Create flags.
        byte flags = consecutive ? FeatureCollection.CONS_SIBL : FeatureCollection.ARBI_SIBL; // 4 bits.
        if (!consecutive) {
            flags |= (directionFirst << 4); // 1 more bit.
        }
        flags |= (directionSecond << 5); // 1 more bit.

        addTurboTripletFeats(sent, head, modifier, sibling, flags, feats, mod, useCoarseTags, useTrilexicalFeats);
    }
    
    /** Adds the features for grandparents siblings from TurboParser (Martins et al., 2013). */
    public static void addTurboGrandparentFeats(final IntAnnoSentence sent, final int grandparent, final int head, 
            final int modifier,             
            final FeatureVector feats, BitshiftDepParseFeatureExtractorPrm prm) {
        final int mod = prm.featureHashMod;
        final boolean useCoarseTags = prm.useCoarseTags;
        final boolean useTrilexicalFeats = prm.useTrilexicalFeats;
        final boolean usePairFor2ndOrder = prm.usePairFor2ndOrder;
        final boolean useUpperGrandDepFeats = prm.useUpperGrandDepFeats;
        final boolean useNonprojGrandDepFeats = prm.useNonprojGrandDepFeats;

        if (usePairFor2ndOrder) {
            if (useUpperGrandDepFeats) {
                addTurboWordPairFeats(sent, grandparent, head, FeatureCollection.GRANDPARENT_G_H, feats, prm);
            }
            addTurboWordPairFeats(sent, grandparent, modifier, FeatureCollection.GRANDPARENT_G_M, feats, prm);
        }
        
        // Create flags.
        byte direction = getGrandparentDirectionCode(grandparent, head, modifier);        
        byte flags = FeatureCollection.GRANDPARENT; // 4 bits.
        flags |= direction << 4; // 2 bits.  

        if (useNonprojGrandDepFeats && direction == 0x2) {
            addTurboWordPairFeats(sent, grandparent, modifier, FeatureCollection.GRANDPARENT_NONPROJ_H_M, feats, prm);
        }

        addTurboTripletFeats(sent, head, modifier, grandparent, flags, feats, mod, useCoarseTags, useTrilexicalFeats);
    }

    /** Features for siblings or grandparents. */
    private static void addTurboTripletFeats(final IntAnnoSentence sent, final int head, final int modifier,
            final int sibling, byte flags, final FeatureVector feats, final int mod, final boolean useCoarseTags,
            final boolean useTrilexicalFeats) {
        // Head, modifier, and sibling words/tags.
        short hWord = (head < 0) ? TOK_WALL_INT : sent.getWord(head);
        byte hPos = (head < 0) ? TOK_WALL_INT : sent.getPosTag(head);
        byte hCpos = (head < 0) ? TOK_WALL_INT : sent.getCposTag(head);
        //
        short mWord = (modifier < 0) ? TOK_START_INT : sent.getWord(modifier);
        byte mPos = (modifier < 0) ? TOK_START_INT : sent.getPosTag(modifier);
        byte mCpos = (modifier < 0) ? TOK_START_INT : sent.getCposTag(modifier);
        //
        short sWord = (sibling < 0) ? TOK_END_INT : sent.getWord(sibling);
        byte sPos = (sibling < 0) ? TOK_END_INT : sent.getPosTag(sibling);
        byte sCpos = (sibling < 0) ? TOK_END_INT : sent.getCposTag(sibling);

        // Bias feature.
        addFeat(feats, mod, encodeFeatureB___(TriTs.BIAS, flags, (byte)0));
        
        // --- Triplet features. ----

        // Three POS tags.
        addFeat(feats, mod, encodeFeatureBBB_(TriTs.hP_mP_sP, flags, hPos, mPos, sPos));
        // One word; Two POS tags.
        addFeat(feats, mod, encodeFeatureSBB_(TriTs.hW_mP_sP, flags, hWord, mPos, sPos));
        addFeat(feats, mod, encodeFeatureSBB_(TriTs.hP_mW_sP, flags, mWord, hPos, sPos));
        addFeat(feats, mod, encodeFeatureSBB_(TriTs.hP_mP_sW, flags, sWord, hPos, mPos));
        // Two words; One POS tags.
        addFeat(feats, mod, encodeFeatureSSB_(TriTs.hW_mW_sP, flags, hWord, mWord, sPos));
        addFeat(feats, mod, encodeFeatureSSB_(TriTs.hW_mP_sW, flags, hWord, sWord, mPos));
        addFeat(feats, mod, encodeFeatureSSB_(TriTs.hP_mW_sW, flags, mWord, sWord, hPos));
        // Three words.
        if (useTrilexicalFeats) {
            addFeat(feats, mod, encodeFeatureSSS_(TriTs.hW_mW_sW, flags, hWord, mWord, sWord));
        }

        // --- Pairwise features. ----
        // Martins notes that these are not redundant with the word-pair features added above, since the 
        // flags used here may differ.
        
        // Two POS tags.
        addFeat(feats, mod, encodeFeatureBB__(TriTs.hP_mP, flags, hPos, mPos));
        addFeat(feats, mod, encodeFeatureBB__(TriTs.hP_sP, flags, hPos, sPos));
        addFeat(feats, mod, encodeFeatureBB__(TriTs.mP_sP, flags, mPos, sPos));
        // One word; One POS tag.
        addFeat(feats, mod, encodeFeatureSB__(TriTs.hW_sP, flags, hWord, sPos));
        addFeat(feats, mod, encodeFeatureSB__(TriTs.hW_mP, flags, hWord, mPos));
        addFeat(feats, mod, encodeFeatureSB__(TriTs.mW_sP, flags, mWord, sPos));
        addFeat(feats, mod, encodeFeatureSB__(TriTs.mW_hP, flags, mWord, hPos));
        addFeat(feats, mod, encodeFeatureSB__(TriTs.sW_hP, flags, sWord, hPos));
        addFeat(feats, mod, encodeFeatureSB__(TriTs.sW_mP, flags, sWord, mPos));
        // Two words.
        addFeat(feats, mod, encodeFeatureSS__(TriTs.hW_sW, flags, hWord, sWord));
        addFeat(feats, mod, encodeFeatureSS__(TriTs.mW_sW, flags, mWord, sWord));
        addFeat(feats, mod, encodeFeatureSS__(TriTs.hW_mW, flags, hWord, mWord));
        
        if (useCoarseTags) {
            // --- Triplet features. ----
            // Three POS tags.
            addFeat(feats, mod, encodeFeatureBBB_(TriTs.hC_mC_sC, flags, hCpos, mCpos, sCpos));
            // One word; Two POS tags.
            addFeat(feats, mod, encodeFeatureSBB_(TriTs.hW_mC_sC, flags, hWord, mCpos, sCpos));
            addFeat(feats, mod, encodeFeatureSBB_(TriTs.hC_mW_sC, flags, mWord, hCpos, sCpos));
            addFeat(feats, mod, encodeFeatureSBB_(TriTs.hC_mC_sW, flags, sWord, hCpos, mCpos));
            // Two words; One POS tags.
            addFeat(feats, mod, encodeFeatureSSB_(TriTs.hW_mW_sC, flags, hWord, mWord, sCpos));
            addFeat(feats, mod, encodeFeatureSSB_(TriTs.hW_mC_sW, flags, hWord, sWord, mCpos));
            addFeat(feats, mod, encodeFeatureSSB_(TriTs.hC_mW_sW, flags, mWord, sWord, hCpos));
            // --- Pairwise features. ----
            // Martins notes that these are not redundant with the word-pair features added above, since the 
            // flags used here may differ.
            //
            // Two POS tags.
            addFeat(feats, mod, encodeFeatureBB__(TriTs.hC_mC, flags, hCpos, mCpos));
            addFeat(feats, mod, encodeFeatureBB__(TriTs.hC_sC, flags, hCpos, sCpos));
            addFeat(feats, mod, encodeFeatureBB__(TriTs.mC_sC, flags, mCpos, sCpos));
            // One word; One POS tag.
            addFeat(feats, mod, encodeFeatureSB__(TriTs.hW_sC, flags, hWord, sCpos));
            addFeat(feats, mod, encodeFeatureSB__(TriTs.hW_mC, flags, hWord, mCpos));
            addFeat(feats, mod, encodeFeatureSB__(TriTs.mW_sC, flags, mWord, sCpos));
            addFeat(feats, mod, encodeFeatureSB__(TriTs.mW_hC, flags, mWord, hCpos));
            addFeat(feats, mod, encodeFeatureSB__(TriTs.sW_hC, flags, sWord, hCpos));
            addFeat(feats, mod, encodeFeatureSB__(TriTs.sW_mC, flags, sWord, mCpos));           
        }
    }
    
    private static void addFeat(FeatureVector feats, int mod, long feat) {
        int hash = MurmurHash.hash32(feat);
        if (mod > 0) {
            hash = FastMath.mod(hash, mod);
        }
        feats.add(hash, 1.0);
        // Enable this for debugging of feature creation.
        //        if (feats instanceof LongFeatureVector) {
        //            ((LongFeatureVector)feats).addLong(feat, 1.0);
        //        }
    }

    private static final long BYTE_MAX =  0xff;
    private static final long SHORT_MAX = 0xffff;
    private static final long INT_MAX =   0xffffffff;

    private static long encodeFeatureS___(byte template, byte flags, short s1) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((s1 & SHORT_MAX) << 16);
    }
    
    private static long encodeFeatureB___(byte template, byte flags, byte b1) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((b1 & BYTE_MAX) << 16);
    }
    
    private static long encodeFeatureSB__(byte template, byte flags, short s1, byte b2) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((s1 & SHORT_MAX) << 16) | ((b2 & BYTE_MAX) << 32);
    }

    private static long encodeFeatureSS__(byte template, byte flags, short s1, short s2) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((s1 & SHORT_MAX) << 16) | ((s2 & SHORT_MAX) << 32);
    }

    private static long encodeFeatureBB__(byte template, byte flags, byte b1, byte b2) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((b1 & SHORT_MAX) << 16) | ((b2 & SHORT_MAX) << 24);
    }

    private static long encodeFeatureSSB_(byte template, byte flags, short s1, short s2, byte b3) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((s1 & SHORT_MAX) << 16) | ((s2 & SHORT_MAX) << 32)
                | ((b3 & BYTE_MAX) << 48);
    }

    private static long encodeFeatureSSS_(byte template, byte flags, short s1, short s2, short s3) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((s1 & SHORT_MAX) << 16) | ((s2 & SHORT_MAX) << 32)
                | ((s3 & SHORT_MAX) << 48);
    }
    
    private static long encodeFeatureSBB_(byte template, byte flags, short s1, byte b2, byte b3) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((s1 & SHORT_MAX) << 16) 
                | ((b2 & BYTE_MAX) << 32) | ((b3 & BYTE_MAX) << 40);
    }
    
    private static long encodeFeatureSSBB(byte template, byte flags, short s1, short s2, byte b3, byte b4) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((s1 & SHORT_MAX) << 16) | ((s2 & SHORT_MAX) << 32)
                | ((b3 & BYTE_MAX) << 48) | ((b4 & BYTE_MAX) << 56); // Full.
    }

    private static long encodeFeatureBBB_(byte template, byte flags, byte b1, byte b2, byte b3) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((b1 & BYTE_MAX) << 16) | ((b2 & BYTE_MAX) << 24)
                | ((b3 & BYTE_MAX) << 32);
    }
    
    private static long encodeFeatureBBBB(byte template, byte flags, byte b1, byte b2, byte b3, byte b4) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((b1 & BYTE_MAX) << 16) | ((b2 & BYTE_MAX) << 24)
                | ((b3 & BYTE_MAX) << 32) | ((b4 & BYTE_MAX) << 40);
    }
    
    private static long encodeFeatureBBBBB(byte template, byte flags, byte b1, byte b2, byte b3, byte b4, byte b5) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((b1 & BYTE_MAX) << 16) | ((b2 & BYTE_MAX) << 24)
                | ((b3 & BYTE_MAX) << 32) | ((b4 & BYTE_MAX) << 40) | ((b5 & BYTE_MAX) << 48);
    }
    
    private static long encodeFeatureBBBBBB(byte template, byte flags, byte b1, byte b2, byte b3, byte b4, byte b5, byte b6) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((b1 & BYTE_MAX) << 16) | ((b2 & BYTE_MAX) << 24)
                | ((b3 & BYTE_MAX) << 32) | ((b4 & BYTE_MAX) << 40) | ((b5 & BYTE_MAX) << 48) | ((b6 & BYTE_MAX) << 56); // Full.
    }

    
}
