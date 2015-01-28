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
        public static final byte ARC = next();
        public static final byte SIBLING = next();
        public static final byte GRANDPARENT = next();
        public static boolean isValid(byte type) {
            return (0 <= type && type <= MAX_VAL);
        }
    }
    
    /** 
     * Template IDs for ARC features in {@link BitshiftDepParseFeatures}.
     * 
     * In the names below, we have the following mapping:
     * H = head
     * M = modifier
     * W = word
     * P = POS tag
     * Q = coarse POS tag
     * W5 = prefix of length 5
     * l = token to the left of following position
     * r = token to the right of following position
     * BTWN = each position betwen the head and modifier
     */
    private static class ArcTs {
        private static int templ = 0;
        protected static byte next() {
            return SafeCast.safeIntToUnsignedByte(templ++);
        }
        // McDonald et al. (2005) features templates.
        public static final byte HW = next();
        public static final byte MW = next();
        public static final byte BIAS = next();
        public static final byte DIR = next();
        public static final byte EXACT_DIST = next();
        public static final byte BIN_DIST = next();
        public static final byte HW_HP = next();
        public static final byte HP = next();
        public static final byte MW_MP = next();
        public static final byte MP = next();
        public static final byte HW_MW_HP_MP = next();
        public static final byte MW_HP_MP = next();
        public static final byte HW_MW_MP = next();
        public static final byte HW_HP_MP = next();
        public static final byte HW_MW_HP = next();
        public static final byte HW_MW = next();
        public static final byte HP_MP = next();
        public static final byte lHP_HP_lMP_MP = next();
        public static final byte lHP_HP_MP_rMP = next();
        public static final byte lHP_HP_MP = next();
        public static final byte HP_rHP_lMP_MP = next();
        public static final byte HP_rHP_MP_rMP = next();
        public static final byte HP_lMP_MP = next();
        public static final byte HP_MP_rMP = next();
        public static final byte HP_rHP_MP = next();
        public static final byte BTWNP_HP_MP = next();
        public static final byte HW5_HP_MP = next();
        public static final byte HW5_HP = next();
        public static final byte HW5 = next();
        public static final byte MW5_HP_MP = next();
        public static final byte MW5_MP = next();
        public static final byte MW5 = next();
        public static final byte HW5_MW5_HP_MP = next();
        public static final byte HW5_MW5_MP = next();
        public static final byte HW5_MW5_HP = next();
        public static final byte HW5_MW5 = next();
        // Coarse POS tag versions of McDonald et al. (2005) features templates.
        public static final byte HW_HQ = next();
        public static final byte HQ = next();
        public static final byte MW_MQ = next();
        public static final byte MQ = next();
        public static final byte HW_MW_HQ_MQ = next();
        public static final byte MW_HQ_MQ = next();
        public static final byte HW_MW_MQ = next();
        public static final byte HW_HQ_MQ = next();
        public static final byte HW_MW_HQ = next();
        public static final byte HQ_MQ = next();
        public static final byte HQ_rHQ_lMQ_MQ = next();
        public static final byte lHQ_HQ_lMQ_MQ = next();
        public static final byte HQ_rHQ_MQ_rMQ = next();
        public static final byte lHQ_HQ_MQ_rMQ = next();
        public static final byte lHQ_HQ_MQ = next();
        public static final byte HQ_lMQ_MQ = next();
        public static final byte HQ_MQ_rMQ = next();
        public static final byte HQ_rHQ_MQ = next();
        public static final byte BTWNP_HQ_MQ = next();
        public static final byte HW5_HQ_MQ = next();
        public static final byte HW5_HQ = next();
        public static final byte MW5_HQ_MQ = next();
        public static final byte MW5_MQ = next();
        public static final byte HW5_MW5_HQ_MQ = next();
        public static final byte HW5_MW5_MQ = next();
        public static final byte HW5_MW5_HQ = next();
        // Additional feature templates from TurboParser (Martins et al., 2013)
        public static final byte HL = next();
        public static final byte HF = next();
        public static final byte HW_HF = next();
        public static final byte ML = next();
        public static final byte MF = next();
        public static final byte MW_MF = next();
        public static final byte lHW = next();
        public static final byte lHP = next();
        public static final byte lHQ = next();
        public static final byte lHL = next();
        public static final byte lHW_lHP = next();
        public static final byte lHW_lHQ = next();
        public static final byte rHW = next();
        public static final byte rHP = next();
        public static final byte rHQ = next();
        public static final byte rHL = next();
        public static final byte rHW_rHP = next();
        public static final byte rHW_rHQ = next();
        public static final byte lMW = next();
        public static final byte lMP = next();
        public static final byte lMQ = next();
        public static final byte lML = next();
        public static final byte lMW_lMP = next();
        public static final byte lMW_lMQ = next();
        public static final byte rMW = next();
        public static final byte rMP = next();
        public static final byte rMQ = next();
        public static final byte rML = next();
        public static final byte rMW_rMP = next();
        public static final byte rMW_rMQ = next();
        public static final byte HP_lHP = next();
        public static final byte HP_lHP_llHP = next();
        public static final byte HP_rHP = next();
        public static final byte HP_rHP_rrHP = next();
        public static final byte HQ_lHQ = next();
        public static final byte llHW = next();
        public static final byte llHP = next();
        public static final byte llHQ = next();
        public static final byte llHL = next();
        public static final byte llHW_llHP = next();
        public static final byte llHW_llHQ = next();
        public static final byte rrHW = next();
        public static final byte rrHP = next();
        public static final byte rrHQ = next();
        public static final byte rrHL = next();
        public static final byte rrHW_rrHP = next();
        public static final byte rrHW_rrHQ = next();
        public static final byte llMW = next();
        public static final byte llMP = next();
        public static final byte llMQ = next();
        public static final byte llML = next();
        public static final byte llMW_llMQ = next();
        public static final byte rrMW = next();
        public static final byte rrMP = next();
        public static final byte rrMQ = next();
        public static final byte rrML = next();
        public static final byte rrMW_rrMP = next();
        public static final byte rrMW_rrMQ = next();
        public static final byte HQ_lHQ_llHQ = next();
        public static final byte llMW_llMP = next();
        public static final byte HQ_rHQ = next();
        public static final byte HQ_rHQ_rrHQ = next();
        public static final byte MP_lMP = next();
        public static final byte MP_lMP_llMP = next();
        public static final byte MP_rMP = next();
        public static final byte MP_rMP_rrMP = next();
        public static final byte MQ_lMQ = next();
        public static final byte MQ_lMQ_llMQ = next();
        public static final byte MQ_rMQ = next();
        public static final byte MQ_rMQ_rrMQ = next();
        public static final byte MW_HP = next();
        public static final byte HW_MP = next();
        public static final byte MW_HQ = next();
        public static final byte HW_MQ = next();
        public static final byte HF_MF = next();
        public static final byte HF_MP = next();
        public static final byte MF_HP = next();
        public static final byte HF_MF_MP = next();
        public static final byte HF_MF_HP = next();
        public static final byte HF_HP_MP = next();
        public static final byte MF_HP_MP = next();
        public static final byte HF_MF_HP_MP = next();
        public static final byte HF_MQ = next();
        public static final byte MF_HQ = next();
        public static final byte HF_MF_MQ = next();
        public static final byte HF_MF_HQ = next();
        public static final byte HF_HQ_MQ = next();
        public static final byte MF_HQ_MQ = next();
        public static final byte HF_MF_HQ_MQ = next();
        public static final byte lHP_HP_rHP_lMP_MP_rMP = next();
        public static final byte lHQ_HQ_rHQ_lMQ_MQ_rMQ = next();
        public static final byte BFLAG = next();
        public static final byte HP_MP_BFLAG = next();
        public static final byte HQ_MQ_BFLAG = next();
        public static final byte BTWNP_HW_MW = next();
        public static final byte BTWNP_HW_MP = next();
        public static final byte BTWNP_HP_MW = next();
        public static final byte BTWNQ_HQ_MQ = next();
        public static final byte BTWNQ_HW_MQ = next();
        public static final byte BTWNQ_HQ_MW = next();
        public static final byte BTWNQ_HW_MW = next();
    }
    
    /** 
     * Template IDs for triplet features (e.g. SIBLING or GRANDPARENT) in {@link BitshiftDepParseFeatures}. 
     */
    private static class TriTs {
        private static int templ = 0;
        protected static byte next() {
            return SafeCast.safeIntToUnsignedByte(templ++);
        }
        // Carreras et al. (2007) templates.
        public static final byte HQ_MQ_SQ = next();
        public static final byte HQ_SQ = next();
        public static final byte MQ_SQ = next();
        public static final byte HQ_MQ = next();
        public static final byte SW_HQ = next();
        public static final byte SW_MQ = next();
        public static final byte HW_SQ = next();
        public static final byte MW_SQ = next();
        public static final byte MW_HQ = next();
        public static final byte HW_MQ = next();
        public static final byte HW_SW = next();
        public static final byte MW_SW = next();
        public static final byte HW_MW = next();
        
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
    
    public static void addArcFeats(IntAnnoSentence sent, int p, int c, BitshiftDepParseFeatureExtractorPrm prm, FeatureVector feats) {
        if (prm.useMstFeats) {
            BitshiftDepParseFeatures.addArcFactoredMSTFeats(sent, p, c, FeatureCollection.ARC, feats, false, prm.useCoarseTags, prm.featureHashMod);
        } else {
            BitshiftDepParseFeatures.addTurboWordPairFeats(sent, p, c, FeatureCollection.ARC, feats, prm.featureHashMod, 
                    prm.maxTokenContext, prm.isLabeledParsing, prm.useNonTurboFeats, prm.useLemmaFeats, prm.useMorphologicalFeatures, 
                    prm.useCoarseTags);
        }
    }
    
    /**
     * Word pair features from Martins et al. (2013) "Turning on the Turbo...". This feature set
     * draws from EGSTRA
     * 
     *  Martins (personal correspondance) notes that TurboParser was found to perform best when the coarse
     * POS tags were replaced with just POS tags. Accordingly, in the feature set below, we define features in 
     * terms of POS tags, and offer an optional <code>if (useCoarsePosTags) { ... }</code> option for the cases 
     * where the original features were defined in TurboParser over coarse tags only.
     */
    public static void addTurboWordPairFeats(final IntAnnoSentence sent, final int p, final int c, final byte pairType, 
            final FeatureVector feats, final int mod,
            final int maxTokenContext,
            final boolean isLabeledParsing,
            final boolean useNonTurboFeats,
            final boolean useLemmaFeats,
            final boolean useMorphologicalFeatures,
            final boolean useCoarseTags) {
        int sentLen = sent.size();

        // Distance codes.
        int distance = (p < c) ? c - p : p - c;
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
        byte direction = (p < c) ? (byte) 0 : (byte) 1;        
        int leftTok = (p < c) ? p : c;
        int rightTok = (p < c) ? c : p;
        
        // Number of certain POS tags in between.
        int numVerbsBetween = sent.getNumVerbsInBetween(p, c);
        int numPuncsBetween = sent.getNumPuncsInBetween(p, c);
        int numConjsBetween = sent.getNumConjsInBetween(p, c);
        // Use at most 4-bits to denote the number in between. 
        int maxOccurrences = 0xf; // 15
        if (numVerbsBetween > maxOccurrences) { numVerbsBetween = maxOccurrences; }
        if (numPuncsBetween > maxOccurrences) { numPuncsBetween = maxOccurrences; }
        if (numConjsBetween > maxOccurrences) { numConjsBetween = maxOccurrences; }
        // Use 4-bits to denote the type of tag between (e.g. verb, punc, conj)
        byte verbsBetweenCode = SafeCast.safeIntToUnsignedByte(0x0 | (numVerbsBetween << 4));
        byte puncsBetweenCode = SafeCast.safeIntToUnsignedByte(0x1 | (numPuncsBetween << 4));
        byte conjsBetweenCode = SafeCast.safeIntToUnsignedByte(0x2 | (numConjsBetween << 4));
        
        // Head and modifier words / POS tags. We denote the head by p (for parent) and the modifier
        // by c (for child).
        short pWord = (p < 0) ? TOK_WALL_INT : sent.getWord(p);
        short cWord = (c < 0) ? TOK_WALL_INT : sent.getWord(c);
        byte pPos = (p < 0) ? TOK_WALL_INT : sent.getPosTag(p);
        byte cPos = (c < 0) ? TOK_WALL_INT : sent.getPosTag(c);
        short pLemma = (p < 0) ? TOK_WALL_INT : sent.getLemma(p);
        short cLemma = (c < 0) ? TOK_WALL_INT : sent.getLemma(c);
        byte pCpos = (p < 0) ? TOK_WALL_INT : sent.getCposTag(p);
        byte cCpos = (c < 0) ? TOK_WALL_INT : sent.getCposTag(c);

        // Surrounding words / POS tags. 
        // One token to the left (l) and right (r).
        short lpWord = (p-1 < 0) ? TOK_START_INT : sent.getWord(p-1);
        short lcWord = (c-1 < 0) ? TOK_START_INT : sent.getWord(c-1);
        short rpWord = (p+1 >= sentLen) ? TOK_END_INT : sent.getWord(p+1);
        short rcWord = (c+1 >= sentLen) ? TOK_END_INT : sent.getWord(c+1);
        //
        byte lpPos = (p-1 < 0) ? TOK_START_INT : sent.getPosTag(p-1);
        byte lcPos = (c-1 < 0) ? TOK_START_INT : sent.getPosTag(c-1);
        byte rpPos = (p+1 >= sentLen) ? TOK_END_INT : sent.getPosTag(p+1);
        byte rcPos = (c+1 >= sentLen) ? TOK_END_INT : sent.getPosTag(c+1);
        //
        short lpLemma = (p-1 < 0) ? TOK_START_INT : sent.getLemma(p-1);
        short lcLemma = (c-1 < 0) ? TOK_START_INT : sent.getLemma(c-1);
        short rpLemma = (p+1 >= sentLen) ? TOK_END_INT : sent.getLemma(p+1);
        short rcLemma = (c+1 >= sentLen) ? TOK_END_INT : sent.getLemma(c+1);
        //
        byte lpCpos = (p-1 < 0) ? TOK_START_INT : sent.getCposTag(p-1);
        byte lcCpos = (c-1 < 0) ? TOK_START_INT : sent.getCposTag(c-1);
        byte rpCpos = (p+1 >= sentLen) ? TOK_END_INT : sent.getCposTag(p+1);
        byte rcCpos = (c+1 >= sentLen) ? TOK_END_INT : sent.getCposTag(c+1);
        
        // Two tokens to the left (ll) and right (rr).
        short llpWord = (p-2 < 0) ? TOK_START_INT : sent.getWord(p-2);
        short llcWord = (c-2 < 0) ? TOK_START_INT : sent.getWord(c-2);
        short rrpWord = (p+2 >= sentLen) ? TOK_END_INT : sent.getWord(p+2);
        short rrcWord = (c+2 >= sentLen) ? TOK_END_INT : sent.getWord(c+2);
        //
        byte llpPos = (p-2 < 0) ? TOK_START_INT : sent.getPosTag(p-2);
        byte llcPos = (c-2 < 0) ? TOK_START_INT : sent.getPosTag(c-2);
        byte rrpPos = (p+2 >= sentLen) ? TOK_END_INT : sent.getPosTag(p+2);
        byte rrcPos = (c+2 >= sentLen) ? TOK_END_INT : sent.getPosTag(c+2);
        //
        short llpLemma = (p-2 < 0) ? TOK_START_INT : sent.getLemma(p-2);
        short llcLemma = (c-2 < 0) ? TOK_START_INT : sent.getLemma(c-2);
        short rrpLemma = (p+2 >= sentLen) ? TOK_END_INT : sent.getLemma(p+2);
        short rrcLemma = (c+2 >= sentLen) ? TOK_END_INT : sent.getLemma(c+2);
        //
        byte llpCpos = (p-2 < 0) ? TOK_START_INT : sent.getCposTag(p-2);
        byte llcCpos = (c-2 < 0) ? TOK_START_INT : sent.getCposTag(c-2);
        byte rrpCpos = (p+2 >= sentLen) ? TOK_END_INT : sent.getCposTag(p+2);
        byte rrcCpos = (c+2 >= sentLen) ? TOK_END_INT : sent.getCposTag(c+2); 
        
        // Flags for the type of feature.
        byte flags = pairType; // 4 bits.
        flags |= (direction << 4); // 1 more bit.

        addFeat(feats, mod, encodeFeatureB___(ArcTs.BIAS, flags, (byte)0));

        // --------------------------------------------------------------------
        // Unigram Features of the Parent and Child tokens.
        // --------------------------------------------------------------------
        
        // Head Only.
        addFeat(feats, mod, encodeFeatureS___(ArcTs.HW, flags, pWord));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.HP, flags, pPos));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.HQ, flags, pCpos));
        if (useLemmaFeats) {
            addFeat(feats, mod, encodeFeatureS___(ArcTs.HL, flags, pLemma));
        }
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.HW_HP, flags, pWord, pPos));
        if (useCoarseTags) {
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.HW_HQ, flags, pWord, pCpos));            
        }
        if (useMorphologicalFeatures) {
            ShortArrayList pMorphosList = safeGetFeats(sent, p);
            short[] pMorphos = pMorphosList.getInternalElements();
            for (int j=0; j < pMorphosList.size(); j++) {
                short pMorpho = pMorphos[j];
                if (pMorpho > 0xfff) {
                    throw new IllegalStateException("Too many morphological feature values.");
                }
                if (pMorphosList.size() >= 0xf) {
                    log.warn("Too many morphological fetures: " + pMorphosList.size());
                    pMorpho = SafeCast.safeIntToShort((pMorpho << 4) | 0xf);
                } else {
                    pMorpho = SafeCast.safeIntToShort((pMorpho << 4) | j);
                }
                addFeat(feats, mod, encodeFeatureS___(ArcTs.HF, flags, pMorpho));
                addFeat(feats, mod, encodeFeatureSS__(ArcTs.HW_HF, flags, pWord, pMorpho));
            }
        }

        if (isLabeledParsing) {
            // Modifier Only.
            addFeat(feats, mod, encodeFeatureS___(ArcTs.MW, flags, cWord));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.MP, flags, cPos));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.MQ, flags, cCpos));
            if (useLemmaFeats) {
                addFeat(feats, mod, encodeFeatureS___(ArcTs.ML, flags, cLemma));
            }            
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.MW_MP, flags, cWord, cPos));
            if (useCoarseTags) {
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.MW_MQ, flags, cWord, cCpos));
            }
            if (useMorphologicalFeatures) {
                ShortArrayList cMorphosList = safeGetFeats(sent, c);
                short[] cMorphos = cMorphosList.getInternalElements();
                for (int k=0; k < cMorphosList.size(); k++) {
                    short cMorpho = cMorphos[k];
                    if (cMorpho > 0xfff) {
                        throw new IllegalStateException("Too many morphological feature values.");
                    }
                    if (cMorphosList.size() >= 0xf) {
                        log.warn("Too many morphological fetures: " + cMorphosList.size());
                        cMorpho = SafeCast.safeIntToShort((cMorpho << 4) | 0xf);
                    } else {
                        cMorpho = SafeCast.safeIntToShort((cMorpho << 4) | k);
                    }
                    addFeat(feats, mod, encodeFeatureS___(ArcTs.MF, flags, cMorpho));
                    addFeat(feats, mod, encodeFeatureSS__(ArcTs.MW_MF, flags, pWord, cMorpho));
                }
            }
        }
        
        // --------------------------------------------------------------------
        // Unigram Features of the words Left and Right of the Parent and Child. 
        // --------------------------------------------------------------------
                
        if (maxTokenContext >= 1) {
            // -- Head Context --
            // Word to the left.
            addFeat(feats, mod, encodeFeatureS___(ArcTs.lHW, flags, lpWord));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.lHP, flags, lpPos));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.lHQ, flags, lpCpos));
            if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.lHL, flags, lpLemma)); }
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.lHW_lHP, flags, lpWord, lpPos));            
            if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.lHW_lHQ, flags, lpWord, lpCpos)); } 
            // Word to the right.
            addFeat(feats, mod, encodeFeatureS___(ArcTs.rHW, flags, rpWord));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.rHP, flags, rpPos));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.rHQ, flags, rpCpos));
            if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.rHL, flags, rpLemma)); }
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.rHW_rHP, flags, rpWord, rpPos));            
            if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.rHW_rHQ, flags, rpWord, rpCpos)); } 
            // -- Modifier Context --
            // TurboParser excludes these features that look only at the modifier unless they are
            // also conjoined with the label.
            if (isLabeledParsing) {
                // Word to the left.
                addFeat(feats, mod, encodeFeatureS___(ArcTs.lMW, flags, lcWord));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.lMP, flags, lcPos));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.lMQ, flags, lcCpos));
                if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.lML, flags, lcLemma)); }
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.lMW_lMP, flags, lcWord, lcPos));                
                if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.lMW_lMQ, flags, lcWord, lcCpos)); } 
                // Word to the right.
                addFeat(feats, mod, encodeFeatureS___(ArcTs.rMW, flags, rcWord));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.rMP, flags, rcPos));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.rMQ, flags, rcCpos));
                if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.rML, flags, rcLemma)); }
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.rMW_rMP, flags, rcWord, rcPos));                
                if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.rMW_rMQ, flags, rcWord, rcCpos)); }
            }
        }
        
        if (maxTokenContext >= 2) {
            // -- Head Context --
            // Two words to the left.
            addFeat(feats, mod, encodeFeatureS___(ArcTs.llHW, flags, llpWord));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.llHP, flags, llpPos));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.llHQ, flags, llpCpos));
            if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.llHL, flags, llpLemma)); }
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.llHW_llHP, flags, llpWord, llpPos));            
            if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.llHW_llHQ, flags, llpWord, llpCpos)); } 
            // Two words to the right.
            addFeat(feats, mod, encodeFeatureS___(ArcTs.rrHW, flags, rrpWord));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.rrHP, flags, rrpPos));
            addFeat(feats, mod, encodeFeatureB___(ArcTs.rrHQ, flags, rrpCpos));
            if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.rrHL, flags, rrpLemma)); }
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.rrHW_rrHP, flags, rrpWord, rrpPos));            
            if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.rrHW_rrHQ, flags, rrpWord, rrpCpos)); } 
            // -- Modifier Context --
            if (isLabeledParsing) {
                // Two words to the left.
                //
                // TurboParser excludes these features that look only at the modifier unless they are
                // also conjoined with the label.
                addFeat(feats, mod, encodeFeatureS___(ArcTs.llMW, flags, llcWord));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.llMP, flags, llcPos));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.llMQ, flags, llcCpos));
                if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.llML, flags, llcLemma)); }
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.llMW_llMP, flags, llcWord, llcPos));                 
                if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.llMW_llMQ, flags, llcWord, llcCpos)); } 
                // Two words to the right.
                addFeat(feats, mod, encodeFeatureS___(ArcTs.rrMW, flags, rrcWord));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.rrMP, flags, rrcPos));
                addFeat(feats, mod, encodeFeatureB___(ArcTs.rrMQ, flags, rrcCpos));
                if (useLemmaFeats) { addFeat(feats, mod, encodeFeatureS___(ArcTs.rrML, flags, rrcLemma)); }
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.rrMW_rrMP, flags, rrcWord, rrcPos));               
                if (useCoarseTags) { addFeat(feats, mod, encodeFeatureSB__(ArcTs.rrMW_rrMQ, flags, rrcWord, rrcCpos)); }
            }
        }

        // --------------------------------------------------------------------
        // Sequential Bigram and Trigram Features of the words the Parent plus context 
        // or Child plus context. 
        // --------------------------------------------------------------------

        addFeat(feats, mod, encodeFeatureBB__(ArcTs.HP_lHP, flags, pPos, lpPos));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_lHP_llHP, flags, pPos, lpPos, llpPos));
        addFeat(feats, mod, encodeFeatureBB__(ArcTs.HP_rHP, flags, pPos, rpPos));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_rHP_rrHP, flags, pPos, rpPos, rrpPos));   
        if (useCoarseTags) {         
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.HQ_lHQ, flags, pCpos, lpCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_lHQ_llHQ, flags, pCpos, lpCpos, llpCpos));
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.HQ_rHQ, flags, pCpos, rpCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_rHQ_rrHQ, flags, pCpos, rpCpos, rrpCpos));
        }
        if (isLabeledParsing) {
            // TurboParser excludes these features that look only at the modifier unless they are
            // also conjoined with the label.
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.MP_lMP, flags, cPos, lcPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.MP_lMP_llMP, flags, cPos, lcPos, llcPos));
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.MP_rMP, flags, cPos, rcPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.MP_rMP_rrMP, flags, cPos, rcPos, rrcPos));   
            if (useCoarseTags) {         
                addFeat(feats, mod, encodeFeatureBB__(ArcTs.MQ_lMQ, flags, cCpos, lcCpos));
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.MQ_lMQ_llMQ, flags, cCpos, lcCpos, llcCpos));
                addFeat(feats, mod, encodeFeatureBB__(ArcTs.MQ_rMQ, flags, cCpos, rcCpos));
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.MQ_rMQ_rrMQ, flags, cCpos, rcCpos, rrcCpos));
            }
        }
        
        // --------------------------------------------------------------------
        // Bigram and Trigram features of the Parent plus Child.
        // --------------------------------------------------------------------

        // Words only.
        addFeat(feats, mod, encodeFeatureSS__(ArcTs.HW_MW, flags, pWord, cWord));
        // POS tags and Words.
        addFeat(feats, mod, encodeFeatureBB__(ArcTs.HP_MP, flags, pPos, cPos));
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.MW_HP, flags, cWord, pPos));
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.HW_MP, flags, pWord, cPos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.MW_HP_MP, flags, cWord, pPos, cPos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.HW_HP_MP, flags, pWord, pPos, cPos));
        addFeat(feats, mod, encodeFeatureSSBB(ArcTs.HW_MW_HP_MP, flags, pWord, cWord, pPos, cPos));       
        if (useCoarseTags) {
            // Coarse POS tags and Words.
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.HQ_MQ, flags, pCpos, cCpos));     
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.MW_HQ, flags, cWord, pCpos));
            addFeat(feats, mod, encodeFeatureSB__(ArcTs.HW_MQ, flags, pWord, cCpos));
            addFeat(feats, mod, encodeFeatureSBB_(ArcTs.MW_HQ_MQ, flags, cWord, pCpos, cCpos));
            addFeat(feats, mod, encodeFeatureSBB_(ArcTs.HW_HQ_MQ, flags, pWord, pCpos, cCpos));        
            addFeat(feats, mod, encodeFeatureSSBB(ArcTs.HW_MW_HQ_MQ, flags, pWord, cWord, pCpos, cCpos));
        }
        if (useNonTurboFeats) {
            // Both Words plus a single Tag (from MST Parser).
            addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW_MW_MQ, flags, pWord, cWord, cCpos));
            addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW_MW_HQ, flags, pWord, cWord, pCpos));
            addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW_MW_MP, flags, pWord, cWord, cPos));
            addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW_MW_HP, flags, pWord, cWord, pPos));
        }
 
        if (useMorphologicalFeatures) {
            // For each morphological feature of the Head.
            ShortArrayList pMorphosList = safeGetFeats(sent, p);
            short[] pMorphos = pMorphosList.getInternalElements();
            for (int j=0; j < pMorphosList.size(); j++) {
                short pMorpho = pMorphos[j];
                if (pMorpho > 0xfff) {
                    throw new IllegalStateException("Too many morphological feature values.");
                }
                if (pMorphosList.size() >= 0xf) {
                    log.warn("Too many morphological fetures: " + pMorphosList.size());
                    pMorpho = SafeCast.safeIntToShort((pMorpho << 4) | 0xf);
                } else {
                    pMorpho = SafeCast.safeIntToShort((pMorpho << 4) | j);
                }
                // For each morphological feature of the Modifier.
                ShortArrayList cMorphosList = safeGetFeats(sent, c);
                short[] cMorphos = cMorphosList.getInternalElements();
                for (int k=0; k < cMorphosList.size(); k++) {
                    short cMorpho = cMorphos[k];
                    if (cMorpho > 0xfff) {
                        throw new IllegalStateException("Too many morphological feature values.");
                    }
                    if (cMorphosList.size() >= 0xf) {
                        log.warn("Too many morphological fetures: " + cMorphosList.size());
                        cMorpho = SafeCast.safeIntToShort((cMorpho << 4) | 0xf);
                    } else {
                        cMorpho = SafeCast.safeIntToShort((cMorpho << 4) | k);
                    }
                    addFeat(feats, mod, encodeFeatureSS__(ArcTs.HF_MF, flags, pMorpho, cMorpho));

                    addFeat(feats, mod, encodeFeatureSB__(ArcTs.HF_MP, flags, pMorpho, cPos));
                    addFeat(feats, mod, encodeFeatureSB__(ArcTs.MF_HP, flags, cMorpho, pPos));
                    addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HF_MF_MP, flags, pMorpho, cMorpho, cPos));
                    addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HF_MF_HP, flags, pMorpho, cMorpho, pPos));                    
                    addFeat(feats, mod, encodeFeatureSBB_(ArcTs.HF_HP_MP, flags, pMorpho, pPos, cPos));
                    addFeat(feats, mod, encodeFeatureSBB_(ArcTs.MF_HP_MP, flags, cMorpho, pPos, cPos));                    
                    addFeat(feats, mod, encodeFeatureSSBB(ArcTs.HF_MF_HP_MP, flags, pMorpho, cMorpho, pPos, cPos));
                    
                    if (useCoarseTags) {
                        addFeat(feats, mod, encodeFeatureSB__(ArcTs.HF_MQ, flags, pMorpho, cCpos));
                        addFeat(feats, mod, encodeFeatureSB__(ArcTs.MF_HQ, flags, cMorpho, pCpos));
                        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HF_MF_MQ, flags, pMorpho, cMorpho, cCpos));
                        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HF_MF_HQ, flags, pMorpho, cMorpho, pCpos));                    
                        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.HF_HQ_MQ, flags, pMorpho, pCpos, cCpos));
                        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.MF_HQ_MQ, flags, cMorpho, pCpos, cCpos));                    
                        addFeat(feats, mod, encodeFeatureSSBB(ArcTs.HF_MF_HQ_MQ, flags, pMorpho, cMorpho, pCpos, cCpos));
                    }
                }
            }
        }

        // Surrounding POS Features
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.lHP_HP_MP, flags, lpPos, pPos, cPos));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_lMP_MP, flags, pPos, lcPos, cPos));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_MP_rMP, flags, pPos, cPos, rcPos));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_rHP_MP, flags, pPos, rpPos, cPos));
        addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HP_rHP_lMP_MP, flags, pPos, rpPos, lcPos, cPos));
        addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lHP_HP_lMP_MP, flags, lpPos, pPos, lcPos, cPos));
        addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HP_rHP_MP_rMP, flags, pPos, rpPos, cPos, rcPos));
        addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lHP_HP_MP_rMP, flags, lpPos, pPos, cPos, rcPos));
        //    we don't backoff to the 5 tag case, i.e. BBBBB.
        addFeat(feats, mod, encodeFeatureBBBBBB(ArcTs.lHP_HP_rHP_lMP_MP_rMP, flags, lpPos, pPos, rpPos, lcPos, cPos, rcPos));       
        if (useCoarseTags) {
            // Surrounding Coarse POS Features
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.lHQ_HQ_MQ, flags, lpCpos, pCpos, cCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_lMQ_MQ, flags, pCpos, lcCpos, cCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_MQ_rMQ, flags, pCpos, cCpos, rcCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_rHQ_MQ, flags, pCpos, rpCpos, cCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HQ_rHQ_lMQ_MQ, flags, pCpos, rpCpos, lcCpos, cCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lHQ_HQ_lMQ_MQ, flags, lpCpos, pCpos, lcCpos, cCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HQ_rHQ_MQ_rMQ, flags, pCpos, rpCpos, cCpos, rcCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lHQ_HQ_MQ_rMQ, flags, lpCpos, pCpos, cCpos, rcCpos));
            //    we don't backoff to the 5 tag case, i.e. BBBBB.
            addFeat(feats, mod, encodeFeatureBBBBBB(ArcTs.lHQ_HQ_rHQ_lMQ_MQ_rMQ, flags, lpCpos, pCpos, rpCpos, lcCpos, cCpos, rcCpos));
        }
        
        // The two cases of Adjacent Dependencies.
        if (p != -1 && p == c - 1) {
            // Parent is the token to the Left of the Child.
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lHP_HP_MP, flags, lpPos, pPos, cPos, (byte)0x1));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HP_MP_rMP, flags, pPos, cPos, rcPos, (byte)0x1));
            addFeat(feats, mod, encodeFeatureBBBBB(ArcTs.lHP_HP_MP_rMP, flags, lpPos, pPos, cPos, rcPos, (byte)0x1)); 
            if (useCoarseTags) {                   
                addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lHQ_HQ_MQ, flags, lpCpos, pCpos, cCpos, (byte)0x1));
                addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HQ_MQ_rMQ, flags, pCpos, cCpos, rcCpos, (byte)0x1));
                addFeat(feats, mod, encodeFeatureBBBBB(ArcTs.lHQ_HQ_MQ_rMQ, flags, lpCpos, pCpos, cCpos, rcCpos, (byte)0x1));
            }
        } else if (p != -1 && p == c + 1) {
            // Parent is the token to the Right of the Child.
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_lMP_MP, flags, pPos, lcPos, cPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_rHP_MP, flags, pPos, rpPos, cPos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HP_rHP_lMP_MP, flags, pPos, rpPos, lcPos, cPos)); 
            if (useCoarseTags) {                  
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_lMQ_MQ, flags, pCpos, lcCpos, cCpos));
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_rHQ_MQ, flags, pCpos, rpCpos, cCpos));
                addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HQ_rHQ_lMQ_MQ, flags, pCpos, rpCpos, lcCpos, cCpos));
            }
        }
        
        // --------------------------------------------------------------------
        // Arc Length Features.
        // --------------------------------------------------------------------

        addFeat(feats, mod, encodeFeatureB___(ArcTs.EXACT_DIST, flags, exactDistCode));
        // Add features for each applicable bin.
        for (byte bin = 0; bin <= binDistCode; bin++) {
            addFeat(feats, mod, encodeFeatureB___(ArcTs.BIN_DIST, flags, bin));
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.HP, flags, pPos, bin));
            addFeat(feats, mod, encodeFeatureBB__(ArcTs.MP, flags, cPos, bin));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_MP, flags, pPos, cPos, bin));  
            if (useCoarseTags) {
                addFeat(feats, mod, encodeFeatureBB__(ArcTs.HQ, flags, pCpos, bin));
                addFeat(feats, mod, encodeFeatureBB__(ArcTs.MQ, flags, cCpos, bin));
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_MQ, flags, pCpos, cCpos, bin));                  
            }
        }

        // --------------------------------------------------------------------
        // Features on the tokens between Parent and Child.
        // --------------------------------------------------------------------
        
        addFeat(feats, mod, encodeFeatureB___(ArcTs.BFLAG, flags, verbsBetweenCode));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.BFLAG, flags, puncsBetweenCode));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.BFLAG, flags, conjsBetweenCode));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_MP_BFLAG, flags, pPos, cPos, verbsBetweenCode));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_MP_BFLAG, flags, pPos, cPos, puncsBetweenCode));
        addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_MP_BFLAG, flags, pPos, cPos, conjsBetweenCode));
        if (useCoarseTags) {
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_MQ_BFLAG, flags, pCpos, cCpos, verbsBetweenCode));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_MQ_BFLAG, flags, pCpos, cCpos, puncsBetweenCode));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_MQ_BFLAG, flags, pCpos, cCpos, conjsBetweenCode));
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
                addFeat(feats, mod, encodeFeatureSSB_(ArcTs.BTWNP_HW_MW, flags, pWord, cWord, btwnPos[i]));
                addFeat(feats, mod, encodeFeatureBBB_(ArcTs.BTWNP_HP_MP, flags, pPos, cPos, btwnPos[i]));
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.BTWNP_HW_MP, flags, pWord, cPos, btwnPos[i]));
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.BTWNP_HP_MW, flags, cWord, pPos, btwnPos[i]));
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
                    addFeat(feats, mod, encodeFeatureSSB_(ArcTs.BTWNQ_HW_MW, flags, pWord, cWord, btwnCpos[i]));
                    addFeat(feats, mod, encodeFeatureBBB_(ArcTs.BTWNQ_HQ_MQ, flags, pCpos, cCpos, btwnCpos[i]));
                    addFeat(feats, mod, encodeFeatureSBB_(ArcTs.BTWNQ_HW_MQ, flags, pWord, cCpos, btwnCpos[i]));
                    addFeat(feats, mod, encodeFeatureSBB_(ArcTs.BTWNQ_HQ_MW, flags, cWord, pCpos, btwnCpos[i]));   
                }
            }                          
        }
    }

    private static ShortArrayList safeGetFeats(IntAnnoSentence sent, int idx) {
        if (idx >= 0) { 
            return sent.getFeats(idx); 
        } else {
            return WALL_MORPHO ;
        }
    }
    
    /** Features from McDonald et al. (2005) "Online Large-Margin Training of Dependency Parsers." */
    public static void addArcFactoredMSTFeats(IntAnnoSentence sent, int p, int c, byte pairType,  
            FeatureVector feats, boolean basicOnly, boolean useCoarseFeats, int mod) {
        // Head and modifier words / POS tags. We denote the head by p (for parent) and the modifier
        // by c (for child).
        short pWord = (p < 0) ? TOK_WALL_INT : sent.getWord(p);
        short cWord = (c < 0) ? TOK_WALL_INT : sent.getWord(c);
        byte pPos = (p < 0) ? TOK_WALL_INT : sent.getPosTag(p);
        byte cPos = (c < 0) ? TOK_WALL_INT : sent.getPosTag(c);
        // 5-character prefixes.
        short pPrefix = (p < 0) ? TOK_WALL_INT : sent.getPrefix(p);
        short cPrefix = (c < 0) ? TOK_WALL_INT : sent.getPrefix(c);
        // Whether to include features for the 5-char prefixes.
        AnnoSentence aSent = sent.getAnnoSentence();
        boolean pPrefixFeats = (p < 0) ? false : aSent.getWord(p).length() > 5;
        boolean cPrefixFeats = (c < 0) ? false : aSent.getWord(c).length() > 5;
        
        // Surrounding words / POS tags. 
        int sentLen = sent.size();
        byte lpPos = (p-1 < 0) ? TOK_START_INT : sent.getPosTag(p-1);
        byte lcPos = (c-1 < 0) ? TOK_START_INT : sent.getPosTag(c-1);
        byte rpPos = (p+1 >= sentLen) ? TOK_END_INT : sent.getPosTag(p+1);
        byte rcPos = (c+1 >= sentLen) ? TOK_END_INT : sent.getPosTag(c+1);
        
        int distance = (p < c) ? c - p : p - c;
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

        byte direction = (p < c) ? (byte) 0 : (byte) 1;        
        
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
            
            extractMstFeaturesWithPos(sent, p, c, feats, basicOnly, pWord, cWord, pPos, cPos, pPrefix, cPrefix,
                    pPrefixFeats, cPrefixFeats, lpPos, lcPos, rpPos, rcPos, distance, binDistCode, direction, mode,
                    flags, mod);
            if (useCoarseFeats) {
                extractMstFeaturesWithCpos(sent, p, c, feats, basicOnly, pWord, cWord, pPos, cPos, pPrefix, cPrefix,
                        pPrefixFeats, cPrefixFeats, lpPos, lcPos, rpPos, rcPos, distance, binDistCode, direction, mode,
                        flags, mod);
            }
        }
    }

    /** Regular POS tag versions of the MST features. */
    private static void extractMstFeaturesWithPos(IntAnnoSentence sent, int p, int c, FeatureVector feats,
            boolean basicOnly, short pWord, short cWord, byte pPos, byte cPos, short pPrefix, short cPrefix,
            boolean pPrefixFeats, boolean cPrefixFeats, byte lpPos, byte lcPos, byte rpPos, byte rcPos, int distance,
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
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.HW_HP, flags, pWord, pPos));
        addFeat(feats, mod, encodeFeatureS___(ArcTs.HW, flags, pWord));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.HP, flags, pPos));
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.MW_MP, flags, cWord, cPos));
        addFeat(feats, mod, encodeFeatureS___(ArcTs.MW, flags, cWord));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.MP, flags, cPos));
        
        //    # Basic Bigram Features
        addFeat(feats, mod, encodeFeatureSSBB(ArcTs.HW_MW_HP_MP, flags, pWord, cWord, pPos, cPos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.MW_HP_MP, flags, cWord, pPos, cPos));
        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW_MW_MP, flags, pWord, cWord, cPos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.HW_HP_MP, flags, pWord, pPos, cPos));
        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW_MW_HP, flags, pWord, cWord, pPos));
        addFeat(feats, mod, encodeFeatureSS__(ArcTs.HW_MW, flags, pWord, cWord));
        addFeat(feats, mod, encodeFeatureBB__(ArcTs.HP_MP, flags, pPos, cPos));            
        
        if (!basicOnly) {
            //    # Surrounding Word POS Features
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HP_rHP_lMP_MP, flags, pPos, rpPos, lcPos, cPos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lHP_HP_lMP_MP, flags, lpPos, pPos, lcPos, cPos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HP_rHP_MP_rMP, flags, pPos, rpPos, cPos, rcPos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lHP_HP_MP_rMP, flags, lpPos, pPos, cPos, rcPos));
            
            //    # Backed-off versions of Surrounding Word POS Features
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.lHP_HP_MP, flags, lpPos, pPos, cPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_lMP_MP, flags, pPos, lcPos, cPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_MP_rMP, flags, pPos, cPos, rcPos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HP_rHP_MP, flags, pPos, rpPos, cPos));
            
            //    # In Between POS Features
            int leftTok  = (p < c) ? p : c;
            int rightTok = (p > c) ? p : c;
            byte[] btwnPos = new byte[distance+1];
            int j = 0;
            for (int i=leftTok; i<=rightTok; i++) {
                btwnPos[j++] = (i < 0) ? TOK_START_INT : sent.getPosTag(i);
            }
            ByteSort.sortAsc(btwnPos);
            for (int i=0; i<btwnPos.length; i++) {
                if (i == 0 || btwnPos[i] != btwnPos[i-1]) {
                    addFeat(feats, mod, encodeFeatureBBB_(ArcTs.BTWNP_HP_MP, flags, btwnPos[i], pPos, cPos));
                }
            }
            
            //    # These features are added for both the entire words as well as the
            //    # 5-gram prefix if the word is longer than 5 characters.
            if (pPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.HW5_HP_MP, flags, pPrefix, pPos, cPos));
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.HW5_HP, flags, pPrefix, pPos));
                addFeat(feats, mod, encodeFeatureS___(ArcTs.HW5, flags, pPrefix));
            }
            if (cPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.MW5_HP_MP, flags, cPrefix, pPos, cPos));
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.MW5_MP, flags, cPrefix, cPos));
                addFeat(feats, mod, encodeFeatureS___(ArcTs.MW5, flags, cPrefix));
            }
            if (pPrefixFeats || cPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSSBB(ArcTs.HW5_MW5_HP_MP, flags, pPrefix, cPrefix, pPos, cPos));
                addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW5_MW5_MP, flags, pPrefix, cPrefix, cPos));
                addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW5_MW5_HP, flags, pPrefix, cPrefix, pPos));
                addFeat(feats, mod, encodeFeatureSS__(ArcTs.HW5_MW5, flags, pPrefix, cPrefix));
            }
        }
    }
    
    /** Coarse POS tag versions of the MST features. */
    private static void extractMstFeaturesWithCpos(IntAnnoSentence sent, int p, int c, FeatureVector feats,
            boolean basicOnly, short pWord, short cWord, byte pPos_NOTUSED, byte cPos_NOTUSED, short pPrefix,
            short cPrefix, boolean pPrefixFeats, boolean cPrefixFeats, byte lpPos_NOTUSED, byte lcPos_NOTUSED, 
            byte rpPos_NOTUSED, byte rcPos_NOTUSED,
            int distance, byte binnedDist, byte direction, byte mode, byte flags, int mod) {
        byte pCpos = (p < 0) ? TOK_WALL_INT : sent.getCposTag(p);
        byte cCpos = (c < 0) ? TOK_WALL_INT : sent.getCposTag(c);

        // Surrounding words / POS tags. 
        int sentLen = sent.size();
        byte lpCpos = (p-1 < 0) ? TOK_START_INT : sent.getCposTag(p-1);
        byte lcCpos = (c-1 < 0) ? TOK_START_INT : sent.getCposTag(c-1);
        byte rpCpos = (p+1 >= sentLen) ? TOK_END_INT : sent.getCposTag(p+1);
        byte rcCpos = (c+1 >= sentLen) ? TOK_END_INT : sent.getCposTag(c+1);
        
        //    # Basic Unigram Features
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.HW_HQ, flags, pWord, pCpos));
        // word only: addFeat(feats, encodeFeatureS___(ArcTs.HW, flags, pWord));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.HQ, flags, pCpos));
        addFeat(feats, mod, encodeFeatureSB__(ArcTs.MW_MQ, flags, cWord, cCpos));
        // word only: addFeat(feats, encodeFeatureS___(ArcTs.MW, flags, cWord));
        addFeat(feats, mod, encodeFeatureB___(ArcTs.MQ, flags, cCpos));
        
        //    # Basic Bigram Features
        addFeat(feats, mod, encodeFeatureSSBB(ArcTs.HW_MW_HQ_MQ, flags, pWord, cWord, pCpos, cCpos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.MW_HQ_MQ, flags, cWord, pCpos, cCpos));
        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW_MW_MQ, flags, pWord, cWord, cCpos));
        addFeat(feats, mod, encodeFeatureSBB_(ArcTs.HW_HQ_MQ, flags, pWord, pCpos, cCpos));
        addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW_MW_HQ, flags, pWord, cWord, pCpos));
        // word only: addFeat(feats, encodeFeatureSS__(ArcTs.HW_MW, flags, pWord, cWord));
        addFeat(feats, mod, encodeFeatureBB__(ArcTs.HQ_MQ, flags, pCpos, cCpos));            
        
        if (!basicOnly) {            
            //    # Surrounding Word POS Features
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HQ_rHQ_lMQ_MQ, flags, pCpos, rpCpos, lcCpos, cCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lHQ_HQ_lMQ_MQ, flags, lpCpos, pCpos, lcCpos, cCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.HQ_rHQ_MQ_rMQ, flags, pCpos, rpCpos, cCpos, rcCpos));
            addFeat(feats, mod, encodeFeatureBBBB(ArcTs.lHQ_HQ_MQ_rMQ, flags, lpCpos, pCpos, cCpos, rcCpos));
            
            //    # Backed-off versions of Surrounding Word POS Features
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.lHQ_HQ_MQ, flags, lpCpos, pCpos, cCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_lMQ_MQ, flags, pCpos, lcCpos, cCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_MQ_rMQ, flags, pCpos, cCpos, rcCpos));
            addFeat(feats, mod, encodeFeatureBBB_(ArcTs.HQ_rHQ_MQ, flags, pCpos, rpCpos, cCpos));
            
            //    # In Between POS Features
            int leftTok  = (p < c) ? p : c;
            int rightTok = (p > c) ? p : c;
            // TODO: Switch to bytes.
            short[] btwnPos = new short[distance+1];
            int j = 0;
            for (int i=leftTok; i<=rightTok; i++) {
                btwnPos[j++] = (i < 0) ? TOK_START_INT : sent.getPosTag(i);
            }
            ShortSort.sortAsc(btwnPos);
            for (int i=0; i<btwnPos.length; i++) {
                if (i == 0 || btwnPos[i] != btwnPos[i-1]) {
                    addFeat(feats, mod, encodeFeatureSBB_(ArcTs.BTWNP_HQ_MQ, flags, btwnPos[i], pCpos, cCpos));
                }
            }
            
            //    # These features are added for both the entire words as well as the
            //    # 5-gram prefix if the word is longer than 5 characters.
            if (pPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.HW5_HQ_MQ, flags, pPrefix, pCpos, cCpos));
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.HW5_HQ, flags, pPrefix, pCpos));
                // word only: addFeat(feats, encodeFeatureS___(ArcTs.HW5, flags, pPrefix));
            }
            if (cPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSBB_(ArcTs.MW5_HQ_MQ, flags, cPrefix, pCpos, cCpos));
                addFeat(feats, mod, encodeFeatureSB__(ArcTs.MW5_MQ, flags, cPrefix, cCpos));
                // word only: addFeat(feats, encodeFeatureS___(ArcTs.MW5, flags, cPrefix));
            }
            if (pPrefixFeats || cPrefixFeats) {
                addFeat(feats, mod, encodeFeatureSSBB(ArcTs.HW5_MW5_HQ_MQ, flags, pPrefix, cPrefix, pCpos, cCpos));
                addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW5_MW5_MQ, flags, pPrefix, cPrefix, cCpos));
                addFeat(feats, mod, encodeFeatureSSB_(ArcTs.HW5_MW5_HQ, flags, pPrefix, cPrefix, pCpos));
                // word only: addFeat(feats, encodeFeatureSS__(ArcTs.HW5_MW5, flags, pPrefix, cPrefix));
            }
        }
    }
    
    /**
     * This is similar to the 2nd order features from Cararras et al. (2007), but incorporates some
     * features from Martins' TurboParser.
     */
    public static void add2ndOrderSiblingFeats(IntAnnoSentence sent, int p, int c, int s, int mod, FeatureVector feats) {
        // Direction flags.
        // Parent-child relationship.
        byte direction_pc = (p < c) ? (byte) 0 : (byte) 1;
        // Parent-sibling relationship.
        byte direction_ps = (p < s) ? (byte) 0 : (byte) 1;
        
        byte flags = FeatureCollection.SIBLING; // 4 bits.
        flags |= direction_pc << 4; // 1 bit.
        flags |= direction_ps << 5; // 1 bit.
        
        addTripletFeatures(sent, p, c, s, feats, flags, mod);
    }
    
    /**
     * This is similar to the 2nd order features from Cararras et al. (2007), but incorporates some
     * features from Martins' TurboParser.
     */
    public static void add2ndOrderGrandparentFeats(IntAnnoSentence sent, int g, int p, int c, FeatureVector feats, int mod) {
        // Direction flags.
        // Parent-grandparent relationship.
        byte direction_gp = (g < p) ? (byte) 0 : (byte) 1;
        // Parent-child relationship.
        byte direction_pc = (p < c) ? (byte) 0 : (byte) 1;
        // Grandparent-child relationship.
        byte direction_gc = (g < c) ? (byte) 0 : (byte) 1;
               
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
        
        byte flags = FeatureCollection.GRANDPARENT; // 4 bits.
        flags |= direction << 4; // 2 bits.
        
        addTripletFeatures(sent, g, p, c, feats, flags, mod);
    }

    // Extra triplets are from TurboParser and can be beneficial because of the flags with which they are conjoined.
    public static final boolean extraTriplets = false;
    
    /** Can be used for either sibling or grandparent features. */
    private static void addTripletFeatures(IntAnnoSentence sent, int p, int c, int s, FeatureVector feats, byte flags, int mod) {
        // Head, modifier, and sibling words / POS tags. We denote the head by p (for parent), the modifier
        // by c (for child), and the sibling by s.
        short pWord = (p < 0) ? TOK_WALL_INT : sent.getWord(p);
        short cWord = (c < 0) ? TOK_WALL_INT : sent.getWord(c);
        short sWord = (s < 0) ? TOK_WALL_INT : sent.getWord(s);
        // Use coarse POS tags.
        byte pCpos = (p < 0) ? TOK_WALL_INT : sent.getCposTag(p);
        byte cCpos = (c < 0) ? TOK_WALL_INT : sent.getCposTag(c);
        byte sCpos = (s < 0) ? TOK_WALL_INT : sent.getCposTag(s);
                
        // --- Triplet features. ----
        
        //    cpos(p) + cpos(c) + cpos(s)
        addFeat(feats, mod, encodeFeatureBBB_(TriTs.HQ_MQ_SQ, flags, pCpos, cCpos, sCpos));

        // --- Pairwise features. ----
        
        //    cpos(p) + cpos(s)
        //    cpos(c) + cpos(s)
        //    cpos(p) + cpos(c) << Not in Carreras. From TurboParser.
        addFeat(feats, mod, encodeFeatureBB__(TriTs.HQ_SQ, flags, pCpos, sCpos));
        addFeat(feats, mod, encodeFeatureBB__(TriTs.MQ_SQ, flags, cCpos, sCpos));
        if (extraTriplets) {
            addFeat(feats, mod, encodeFeatureBB__(TriTs.HQ_MQ, flags, pCpos, cCpos));
        }

        //    cpos(p) + word(s)
        //    cpos(c) + word(s)
        //    word(p) + cpos(s)
        //    word(c) + cpos(s)
        //    word(p) + cpos(c) << Not in Carreras. From TurboParser.
        //    word(c) + cpos(p) << Not in Carreras. From TurboParser.
        addFeat(feats, mod, encodeFeatureSB__(TriTs.SW_HQ, flags, sWord, pCpos));
        addFeat(feats, mod, encodeFeatureSB__(TriTs.SW_MQ, flags, sWord, cCpos));
        addFeat(feats, mod, encodeFeatureSB__(TriTs.HW_SQ, flags, pWord, sCpos));
        addFeat(feats, mod, encodeFeatureSB__(TriTs.MW_SQ, flags, cWord, sCpos));
        if (extraTriplets) {
            addFeat(feats, mod, encodeFeatureSB__(TriTs.MW_HQ, flags, cWord, pCpos));
            addFeat(feats, mod, encodeFeatureSB__(TriTs.HW_MQ, flags, pWord, cCpos));
        }

        //    word(p) + word(s)
        //    word(c) + word(s)
        //    word(p) + word(c) << Not in Carreras. From TurboParser.
        addFeat(feats, mod, encodeFeatureSS__(TriTs.HW_SW, flags, pWord, sWord));
        addFeat(feats, mod, encodeFeatureSS__(TriTs.MW_SW, flags, cWord, sWord));
        if (extraTriplets) {
            addFeat(feats, mod, encodeFeatureSS__(TriTs.HW_MW, flags, pWord, cWord));
        }
    }

    private static void addFeat(FeatureVector feats, int mod, long feat) {
        int hash = MurmurHash.hash32(feat);
        if (mod > 0) {
            hash = FastMath.mod(hash, mod);
        }
        feats.add(hash, 1.0);
    }

    private static final long BYTE_MAX =  0xff;
    private static final long SHORT_MAX = 0xffff;
    private static final long INT_MAX =   0xffffffff;

    private static long encodeFeatureS___(byte template, byte flags, short pWord) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pWord & SHORT_MAX) << 16);
    }
    
    private static long encodeFeatureB___(byte template, byte flags, byte pPos) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pPos & BYTE_MAX) << 16);
    }
    
    private static long encodeFeatureSB__(byte template, byte flags, short pWord, byte pPos) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pWord & SHORT_MAX) << 16) | ((pPos & BYTE_MAX) << 32);
    }

    private static long encodeFeatureSS__(byte template, byte flags, short pWord, short cWord) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pWord & SHORT_MAX) << 16) | ((cWord & SHORT_MAX) << 32);
    }

    private static long encodeFeatureBB__(byte template, byte flags, byte pPos, byte cPos) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pPos & SHORT_MAX) << 16) | ((cPos & SHORT_MAX) << 24);
    }

    private static long encodeFeatureSSB_(byte template, byte flags, short pWord, short cWord, byte pPos) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pWord & SHORT_MAX) << 16) | ((cWord & SHORT_MAX) << 32)
                | ((pPos & BYTE_MAX) << 48);
    }

    private static long encodeFeatureSBB_(byte template, byte flags, short pWord, byte pPos, byte cPos) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pWord & SHORT_MAX) << 16) 
                | ((pPos & BYTE_MAX) << 32) | ((cPos & BYTE_MAX) << 40);
    }
    
    private static long encodeFeatureSSBB(byte template, byte flags, short pWord, short cWord, byte pPos, byte cPos) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pWord & SHORT_MAX) << 16) | ((cWord & SHORT_MAX) << 32)
                | ((pPos & BYTE_MAX) << 48) | ((cPos & BYTE_MAX) << 56);
    }

    private static long encodeFeatureBBB_(byte template, byte flags, byte pPos, byte lpPos, byte rcPos) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pPos & BYTE_MAX) << 16) | ((lpPos & BYTE_MAX) << 24)
                | ((rcPos & BYTE_MAX) << 32);
    }
    
    private static long encodeFeatureBBBB(byte template, byte flags, byte pPos, byte lpPos, byte rcPos, byte cPos) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pPos & BYTE_MAX) << 16) | ((lpPos & BYTE_MAX) << 24)
                | ((rcPos & BYTE_MAX) << 32) | ((cPos & BYTE_MAX) << 40);
    }
    
    private static long encodeFeatureBBBBB(byte template, byte flags, byte pos1, byte pos2, byte pos3, byte pos4, byte pos5) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pos1 & BYTE_MAX) << 16) | ((pos2 & BYTE_MAX) << 24)
                | ((pos3 & BYTE_MAX) << 32) | ((pos4 & BYTE_MAX) << 40) | ((pos5 & BYTE_MAX) << 48);
    }
    
    private static long encodeFeatureBBBBBB(byte template, byte flags, byte pos1, byte pos2, byte pos3, byte pos4, byte pos5, byte pos6) {
        return (template & BYTE_MAX) | ((flags & BYTE_MAX) << 8) | ((pos1 & BYTE_MAX) << 16) | ((pos2 & BYTE_MAX) << 24)
                | ((pos3 & BYTE_MAX) << 32) | ((pos4 & BYTE_MAX) << 40) | ((pos5 & BYTE_MAX) << 48) | ((pos6 & BYTE_MAX) << 56);
    }
    
}
