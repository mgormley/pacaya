package edu.jhu.nlp.depparse;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.nlp.data.simple.AlphabetStore;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.IntAnnoSentence;
import edu.jhu.prim.sort.ShortSort;
import edu.jhu.prim.util.SafeCast;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.hash.MurmurHash;

// TODO: we should have a special token representing the wall. Instead we're using the int
// for the start of the sentence.
public class BitshiftDepParseFeatures {

    private static final int TOK_START_INT = AlphabetStore.TOK_START_INT;
    private static final int TOK_END_INT = AlphabetStore.TOK_END_INT;
    private static final int TOK_WALL_INT = AlphabetStore.TOK_WALL_INT;
    
    private enum FeatureCollection {
        ARC,
        SIBLING,
        GRANDPARENT,
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
    
    /** Features from McDonald et al. (2005) "Online Large-Margin Training of Dependency Parsers." 
     * @param featureHashMod TODO*/
    public static void addArcFactoredMSTFeats(IntAnnoSentence sent, int p, int c, FeatureVector feats, 
            boolean basicOnly, boolean coarseTagFeats, int featureHashMod) {
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
        byte binnedDist; // = SafeCast.safeIntToUnsignedByte(binInt(sentLen, 0, 2, 5, 10, 20, 30, 40));
        if (distance >= 40) {
            binnedDist = 0;
        } else if (distance >= 30) {
            binnedDist = 1;
        } else if (distance >= 20) {
            binnedDist = 2;
        } else if (distance >= 10) {
            binnedDist = 3;
        } else if (distance >= 5) {
            binnedDist = 4;
        } else if (distance >= 2) {
            binnedDist = 5;
        } else {
            binnedDist = 6;
        }

        byte direction = (p < c) ? (byte) 0 : (byte) 1;        
        
        for (byte mode = 0; mode < 2; mode++) {
            assert FeatureCollection.values().length <= 8;
            byte flags = (byte) (FeatureCollection.ARC.ordinal() << 5); // 3 bits.
            flags |= mode << 3; // 1 bit.
            if (mode == 1) {
                //    # All features in Table 1 were conjoined with *direction* of attachment and *distance*.
                flags |= direction << 4; // 1 bit.
                flags |= binnedDist << 5; // 3 bits. (8 total)
            }
            
            extractMstFeaturesWithPos(sent, p, c, feats, basicOnly, pWord, cWord, pPos, cPos, pPrefix, cPrefix,
                    pPrefixFeats, cPrefixFeats, lpPos, lcPos, rpPos, rcPos, distance, binnedDist, direction, mode,
                    flags, featureHashMod);
            if (coarseTagFeats) {
                extractMstFeaturesWithCpos(sent, p, c, feats, basicOnly, pWord, cWord, pPos, cPos, pPrefix, cPrefix,
                        pPrefixFeats, cPrefixFeats, lpPos, lcPos, rpPos, rcPos, distance, binnedDist, direction, mode,
                        flags, featureHashMod);
            }
        }
    }

    /** Regular POS tag versions of the MST features. 
     * @param featureHashMod TODO*/
    private static void extractMstFeaturesWithPos(IntAnnoSentence sent, int p, int c, FeatureVector feats,
            boolean basicOnly, short pWord, short cWord, byte pPos, byte cPos, short pPrefix, short cPrefix,
            boolean pPrefixFeats, boolean cPrefixFeats, byte lpPos, byte lcPos, byte rpPos, byte rcPos, int distance,
            byte binnedDist, byte direction, byte mode, byte flags, int featureHashMod) {
        // Bias features.
        //    # TODO: It's not clear whether these were included in McDonald et al. (2005), 
        //    # but Koo et al. (2008) had them.
        addFeat(feats, encodeFeatureB___(ArcTs.BIAS, flags, (byte)0), featureHashMod);
        if (mode == 0) {
            addFeat(feats, encodeFeatureB___(ArcTs.DIR, flags, direction), featureHashMod);
            addFeat(feats, encodeFeatureB___(ArcTs.BIN_DIST, flags, binnedDist), featureHashMod);
        }
        
        //    # Basic Unigram Features
        addFeat(feats, encodeFeatureSB__(ArcTs.HW_HP, flags, pWord, pPos), featureHashMod);
        addFeat(feats, encodeFeatureS___(ArcTs.HW, flags, pWord), featureHashMod);
        addFeat(feats, encodeFeatureB___(ArcTs.HP, flags, pPos), featureHashMod);
        addFeat(feats, encodeFeatureSB__(ArcTs.MW_MP, flags, cWord, cPos), featureHashMod);
        addFeat(feats, encodeFeatureS___(ArcTs.MW, flags, cWord), featureHashMod);
        addFeat(feats, encodeFeatureB___(ArcTs.MP, flags, cPos), featureHashMod);
        
        //    # Basic Bigram Features
        addFeat(feats, encodeFeatureSSBB(ArcTs.HW_MW_HP_MP, flags, pWord, cWord, pPos, cPos), featureHashMod);
        addFeat(feats, encodeFeatureSBB_(ArcTs.MW_HP_MP, flags, cWord, pPos, cPos), featureHashMod);
        addFeat(feats, encodeFeatureSSB_(ArcTs.HW_MW_MP, flags, pWord, cWord, cPos), featureHashMod);
        addFeat(feats, encodeFeatureSBB_(ArcTs.HW_HP_MP, flags, pWord, pPos, cPos), featureHashMod);
        addFeat(feats, encodeFeatureSSB_(ArcTs.HW_MW_HP, flags, pWord, cWord, pPos), featureHashMod);
        addFeat(feats, encodeFeatureSS__(ArcTs.HW_MW, flags, pWord, cWord), featureHashMod);
        addFeat(feats, encodeFeatureBB__(ArcTs.HP_MP, flags, pPos, cPos), featureHashMod);            
        
        if (!basicOnly) {            
            //    # Surrounding Word POS Features
            addFeat(feats, encodeFeatureBBBB(ArcTs.HP_rHP_lMP_MP, flags, pPos, rpPos, lcPos, cPos), featureHashMod);
            addFeat(feats, encodeFeatureBBBB(ArcTs.lHP_HP_lMP_MP, flags, lpPos, pPos, lcPos, cPos), featureHashMod);
            addFeat(feats, encodeFeatureBBBB(ArcTs.HP_rHP_MP_rMP, flags, pPos, rpPos, cPos, rcPos), featureHashMod);
            addFeat(feats, encodeFeatureBBBB(ArcTs.lHP_HP_MP_rMP, flags, lpPos, pPos, cPos, rcPos), featureHashMod);
            
            //    # Backed-off versions of Surrounding Word POS Features
            addFeat(feats, encodeFeatureBBB_(ArcTs.lHP_HP_MP, flags, lpPos, pPos, cPos), featureHashMod);
            addFeat(feats, encodeFeatureBBB_(ArcTs.HP_lMP_MP, flags, pPos, lcPos, cPos), featureHashMod);
            addFeat(feats, encodeFeatureBBB_(ArcTs.HP_MP_rMP, flags, pPos, cPos, rcPos), featureHashMod);
            addFeat(feats, encodeFeatureBBB_(ArcTs.HP_rHP_MP, flags, pPos, rpPos, cPos), featureHashMod);
            
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
                    addFeat(feats, encodeFeatureSBB_(ArcTs.BTWNP_HP_MP, flags, btwnPos[i], pPos, cPos), featureHashMod);
                }
            }
            
            //    # These features are added for both the entire words as well as the
            //    # 5-gram prefix if the word is longer than 5 characters.
            if (pPrefixFeats) {
                addFeat(feats, encodeFeatureSBB_(ArcTs.HW5_HP_MP, flags, pPrefix, pPos, cPos), featureHashMod);
                addFeat(feats, encodeFeatureSB__(ArcTs.HW5_HP, flags, pPrefix, pPos), featureHashMod);
                addFeat(feats, encodeFeatureS___(ArcTs.HW5, flags, pPrefix), featureHashMod);
            }
            if (cPrefixFeats) {
                addFeat(feats, encodeFeatureSBB_(ArcTs.MW5_HP_MP, flags, cPrefix, pPos, cPos), featureHashMod);
                addFeat(feats, encodeFeatureSB__(ArcTs.MW5_MP, flags, cPrefix, cPos), featureHashMod);
                addFeat(feats, encodeFeatureS___(ArcTs.MW5, flags, cPrefix), featureHashMod);
            }
            if (pPrefixFeats || cPrefixFeats) {
                addFeat(feats, encodeFeatureSSBB(ArcTs.HW5_MW5_HP_MP, flags, pPrefix, cPrefix, pPos, cPos), featureHashMod);
                addFeat(feats, encodeFeatureSSB_(ArcTs.HW5_MW5_MP, flags, pPrefix, cPrefix, cPos), featureHashMod);
                addFeat(feats, encodeFeatureSSB_(ArcTs.HW5_MW5_HP, flags, pPrefix, cPrefix, pPos), featureHashMod);
                addFeat(feats, encodeFeatureSS__(ArcTs.HW5_MW5, flags, pPrefix, cPrefix), featureHashMod);
            }
        }
    }
    
    /** Coarse POS tag versions of the MST features. 
     * @param featureHashMod TODO*/
    private static void extractMstFeaturesWithCpos(IntAnnoSentence sent, int p, int c, FeatureVector feats,
            boolean basicOnly, short pWord, short cWord, byte pPos_NOTUSED, byte cPos_NOTUSED, short pPrefix,
            short cPrefix, boolean pPrefixFeats, boolean cPrefixFeats, byte lpPos_NOTUSED, byte lcPos_NOTUSED, 
            byte rpPos_NOTUSED, byte rcPos_NOTUSED,
            int distance, byte binnedDist, byte direction, byte mode, byte flags, int featureHashMod) {
        byte pCpos = (p < 0) ? TOK_WALL_INT : sent.getCposTag(p);
        byte cCpos = (c < 0) ? TOK_WALL_INT : sent.getCposTag(c);

        // Surrounding words / POS tags. 
        int sentLen = sent.size();
        byte lpCpos = (p-1 < 0) ? TOK_START_INT : sent.getCposTag(p-1);
        byte lcCpos = (c-1 < 0) ? TOK_START_INT : sent.getCposTag(c-1);
        byte rpCpos = (p+1 >= sentLen) ? TOK_END_INT : sent.getCposTag(p+1);
        byte rcCpos = (c+1 >= sentLen) ? TOK_END_INT : sent.getCposTag(c+1);
        
        //    # Basic Unigram Features
        addFeat(feats, encodeFeatureSB__(ArcTs.HW_HQ, flags, pWord, pCpos), featureHashMod);
        // word only: addFeat(feats, encodeFeatureS___(ArcTs.HW, flags, pWord));
        addFeat(feats, encodeFeatureB___(ArcTs.HQ, flags, pCpos), featureHashMod);
        addFeat(feats, encodeFeatureSB__(ArcTs.MW_MQ, flags, cWord, cCpos), featureHashMod);
        // word only: addFeat(feats, encodeFeatureS___(ArcTs.MW, flags, cWord));
        addFeat(feats, encodeFeatureB___(ArcTs.MQ, flags, cCpos), featureHashMod);
        
        //    # Basic Bigram Features
        addFeat(feats, encodeFeatureSSBB(ArcTs.HW_MW_HQ_MQ, flags, pWord, cWord, pCpos, cCpos), featureHashMod);
        addFeat(feats, encodeFeatureSBB_(ArcTs.MW_HQ_MQ, flags, cWord, pCpos, cCpos), featureHashMod);
        addFeat(feats, encodeFeatureSSB_(ArcTs.HW_MW_MQ, flags, pWord, cWord, cCpos), featureHashMod);
        addFeat(feats, encodeFeatureSBB_(ArcTs.HW_HQ_MQ, flags, pWord, pCpos, cCpos), featureHashMod);
        addFeat(feats, encodeFeatureSSB_(ArcTs.HW_MW_HQ, flags, pWord, cWord, pCpos), featureHashMod);
        // word only: addFeat(feats, encodeFeatureSS__(ArcTs.HW_MW, flags, pWord, cWord));
        addFeat(feats, encodeFeatureBB__(ArcTs.HQ_MQ, flags, pCpos, cCpos), featureHashMod);            
        
        if (!basicOnly) {            
            //    # Surrounding Word POS Features
            addFeat(feats, encodeFeatureBBBB(ArcTs.HQ_rHQ_lMQ_MQ, flags, pCpos, rpCpos, lcCpos, cCpos), featureHashMod);
            addFeat(feats, encodeFeatureBBBB(ArcTs.lHQ_HQ_lMQ_MQ, flags, lpCpos, pCpos, lcCpos, cCpos), featureHashMod);
            addFeat(feats, encodeFeatureBBBB(ArcTs.HQ_rHQ_MQ_rMQ, flags, pCpos, rpCpos, cCpos, rcCpos), featureHashMod);
            addFeat(feats, encodeFeatureBBBB(ArcTs.lHQ_HQ_MQ_rMQ, flags, lpCpos, pCpos, cCpos, rcCpos), featureHashMod);
            
            //    # Backed-off versions of Surrounding Word POS Features
            addFeat(feats, encodeFeatureBBB_(ArcTs.lHQ_HQ_MQ, flags, lpCpos, pCpos, cCpos), featureHashMod);
            addFeat(feats, encodeFeatureBBB_(ArcTs.HQ_lMQ_MQ, flags, pCpos, lcCpos, cCpos), featureHashMod);
            addFeat(feats, encodeFeatureBBB_(ArcTs.HQ_MQ_rMQ, flags, pCpos, cCpos, rcCpos), featureHashMod);
            addFeat(feats, encodeFeatureBBB_(ArcTs.HQ_rHQ_MQ, flags, pCpos, rpCpos, cCpos), featureHashMod);
            
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
                    addFeat(feats, encodeFeatureSBB_(ArcTs.BTWNP_HQ_MQ, flags, btwnPos[i], pCpos, cCpos), featureHashMod);
                }
            }
            
            //    # These features are added for both the entire words as well as the
            //    # 5-gram prefix if the word is longer than 5 characters.
            if (pPrefixFeats) {
                addFeat(feats, encodeFeatureSBB_(ArcTs.HW5_HQ_MQ, flags, pPrefix, pCpos, cCpos), featureHashMod);
                addFeat(feats, encodeFeatureSB__(ArcTs.HW5_HQ, flags, pPrefix, pCpos), featureHashMod);
                // word only: addFeat(feats, encodeFeatureS___(ArcTs.HW5, flags, pPrefix));
            }
            if (cPrefixFeats) {
                addFeat(feats, encodeFeatureSBB_(ArcTs.MW5_HQ_MQ, flags, cPrefix, pCpos, cCpos), featureHashMod);
                addFeat(feats, encodeFeatureSB__(ArcTs.MW5_MQ, flags, cPrefix, cCpos), featureHashMod);
                // word only: addFeat(feats, encodeFeatureS___(ArcTs.MW5, flags, cPrefix));
            }
            if (pPrefixFeats || cPrefixFeats) {
                addFeat(feats, encodeFeatureSSBB(ArcTs.HW5_MW5_HQ_MQ, flags, pPrefix, cPrefix, pCpos, cCpos), featureHashMod);
                addFeat(feats, encodeFeatureSSB_(ArcTs.HW5_MW5_MQ, flags, pPrefix, cPrefix, cCpos), featureHashMod);
                addFeat(feats, encodeFeatureSSB_(ArcTs.HW5_MW5_HQ, flags, pPrefix, cPrefix, pCpos), featureHashMod);
                // word only: addFeat(feats, encodeFeatureSS__(ArcTs.HW5_MW5, flags, pPrefix, cPrefix));
            }
        }
    }
    
    /**
     * This is similar to the 2nd order features from Cararras et al. (2007), but incorporates some
     * features from Martins' TurboParser.
     * @param featureHashMod TODO
     */
    public static void add2ndOrderSiblingFeats(IntAnnoSentence sent, int p, int c, int s, int featureHashMod, FeatureVector feats) {
        // Direction flags.
        // Parent-child relationship.
        byte direction_pc = (p < c) ? (byte) 0 : (byte) 1;
        // Parent-sibling relationship.
        byte direction_ps = (p < s) ? (byte) 0 : (byte) 1;
        
        assert FeatureCollection.values().length <= 8;
        byte flags = (byte) (FeatureCollection.SIBLING.ordinal() << 5); // 3 bits.
        flags |= direction_pc << 3; // 1 bit.
        flags |= direction_ps << 4; // 1 bit.
                
        addTripletFeatures(sent, p, c, s, feats, flags, featureHashMod);
    }
    
    /**
     * This is similar to the 2nd order features from Cararras et al. (2007), but incorporates some
     * features from Martins' TurboParser.
     * @param featureHashMod TODO
     */
    public static void add2ndOrderGrandparentFeats(IntAnnoSentence sent, int g, int p, int c, FeatureVector feats, int featureHashMod) {
        // Direction flags.
        // Parent-grandparent relationship.
        byte direction_gp = (g < p) ? (byte) 0 : (byte) 1;
        // Parent-child relationship.
        byte direction_pc = (p < c) ? (byte) 0 : (byte) 1;
        // Grandparent-child relationship.
        byte direction_gc = (g < c) ? (byte) 0 : (byte) 1;
        
        assert FeatureCollection.values().length <= 8;
        byte flags = (byte) (FeatureCollection.GRANDPARENT.ordinal() << 5); // 3 bits.
        
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
        flags |= direction << 3; // 2 bits.
        
        addTripletFeatures(sent, g, p, c, feats, flags, featureHashMod);
    }

    // Extra triplets are from TurboParser and can be beneficial because of the flags with which they are conjoined.
    public static final boolean extraTriplets = false;
    
    /** Can be used for either sibling or grandparent features. 
     * @param featureHashMod TODO*/
    private static void addTripletFeatures(IntAnnoSentence sent, int p, int c, int s, FeatureVector feats, byte flags, int featureHashMod) {
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
        addFeat(feats, encodeFeatureBBB_(TriTs.HQ_MQ_SQ, flags, pCpos, cCpos, sCpos), featureHashMod);

        // --- Pairwise features. ----
        
        //    cpos(p) + cpos(s)
        //    cpos(c) + cpos(s)
        //    cpos(p) + cpos(c) << Not in Carreras. From TurboParser.
        addFeat(feats, encodeFeatureBB__(TriTs.HQ_SQ, flags, pCpos, sCpos), featureHashMod);
        addFeat(feats, encodeFeatureBB__(TriTs.MQ_SQ, flags, cCpos, sCpos), featureHashMod);
        if (extraTriplets) {
            addFeat(feats, encodeFeatureBB__(TriTs.HQ_MQ, flags, pCpos, cCpos), featureHashMod);
        }

        //    cpos(p) + word(s)
        //    cpos(c) + word(s)
        //    word(p) + cpos(s)
        //    word(c) + cpos(s)
        //    word(p) + cpos(c) << Not in Carreras. From TurboParser.
        //    word(c) + cpos(p) << Not in Carreras. From TurboParser.
        addFeat(feats, encodeFeatureSB__(TriTs.SW_HQ, flags, sWord, pCpos), featureHashMod);
        addFeat(feats, encodeFeatureSB__(TriTs.SW_MQ, flags, sWord, cCpos), featureHashMod);
        addFeat(feats, encodeFeatureSB__(TriTs.HW_SQ, flags, pWord, sCpos), featureHashMod);
        addFeat(feats, encodeFeatureSB__(TriTs.MW_SQ, flags, cWord, sCpos), featureHashMod);
        if (extraTriplets) {
            addFeat(feats, encodeFeatureSB__(TriTs.MW_HQ, flags, cWord, pCpos), featureHashMod);
            addFeat(feats, encodeFeatureSB__(TriTs.HW_MQ, flags, pWord, cCpos), featureHashMod);
        }

        //    word(p) + word(s)
        //    word(c) + word(s)
        //    word(p) + word(c) << Not in Carreras. From TurboParser.
        addFeat(feats, encodeFeatureSS__(TriTs.HW_SW, flags, pWord, sWord), featureHashMod);
        addFeat(feats, encodeFeatureSS__(TriTs.MW_SW, flags, cWord, sWord), featureHashMod);
        if (extraTriplets) {
            addFeat(feats, encodeFeatureSS__(TriTs.HW_MW, flags, pWord, cWord), featureHashMod);
        }
    }

    private static void addFeat(FeatureVector feats, long feat, int featureHashMod) {
        int hash = MurmurHash.hash32(feat);
        if (featureHashMod > 0) {
            hash = FastMath.mod(hash, featureHashMod);
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
    
}
