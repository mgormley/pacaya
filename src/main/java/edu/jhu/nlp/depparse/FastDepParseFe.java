package edu.jhu.nlp.depparse;

import edu.jhu.nlp.data.simple.AlphabetStore;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.IntAnnoSentence;
import edu.jhu.prim.list.LongArrayList;
import edu.jhu.prim.sort.ShortSort;

public class FastDepParseFe {

    private static final int TOK_START_INT = AlphabetStore.TOK_START_INT;
    private static final int TOK_END_INT = AlphabetStore.TOK_END_INT;
    
    /** Returns the bin into which the given size falls. */
    public static int binInt(int size, int...bins) {
        for (int i=bins.length-1; i >= 0; i--) {
            if (size >= bins[i]) {
                return i;
            }
        }
        return bins.length;
    }
    
    public static void addArcFactoredMSTFeats(IntAnnoSentence sent, int p, int c, LongArrayList feats) {
        // Head and modifier words / POS tags. We denote the head by p (for parent) and the modifier
        // by c (for child).
        // TODO: we should have a special token representing the wall. Instead we're using the int for the start of the sentence.
        short pWord = (p < 0) ? TOK_START_INT : sent.getWord(p);
        short cWord = (c < 0) ? TOK_START_INT : sent.getWord(c);
        byte pPos = (p < 0) ? TOK_START_INT : sent.getPosTag(p);
        byte cPos = (c < 0) ? TOK_START_INT : sent.getPosTag(c);
        // 5-character prefixes.
        short pPrefix = (p < 0) ? TOK_START_INT : sent.getPrefix(p);
        short cPrefix = (c < 0) ? TOK_START_INT : sent.getPrefix(c);
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
            byte flags = mode;
            if (mode == 1) {
                //    # All features in Table 1 were conjoined with *direction* of attachment and *distance*.
                flags |= direction << 1;
                flags |= binnedDist << 2;
            }
            byte templ = 0;
            
            // Bias features.
            //    # TODO: It's not clear whether these were included in McDonald et al. (2005), 
            //    # but Koo et al. (2008) had them.
            //    relative(p,c)
            //    distance(p,c)
            //    relative(p,c) + distance(p,c)
            feats.add(encodeFeatureB___(templ++, flags, (byte)0));
            if (mode == 0) {
                feats.add(encodeFeatureB___(templ++, flags, direction));
                feats.add(encodeFeatureB___(templ++, flags, binnedDist));
            } else {
                templ++;
                templ++;
            }
            
            //    # Basic Unigram Features
            //    word(p) + pos(p)
            //    word(p)
            //    pos(p)
            //    word(c) + pos(c)
            //    word(c)
            //    pos(c)
            feats.add(encodeFeatureSB__(templ++, flags, pWord, pPos));
            feats.add(encodeFeatureS___(templ++, flags, pWord));
            feats.add(encodeFeatureB___(templ++, flags, pPos));
            feats.add(encodeFeatureSB__(templ++, flags, cWord, cPos));
            feats.add(encodeFeatureS___(templ++, flags, cWord));
            feats.add(encodeFeatureB___(templ++, flags, cPos));
            
            //    # Basic Bigram Features
            //    word(p) + pos(p) + word(c) + pos(c)
            //    pos(p) + word(c) + pos(c)
            //    word(p) + word(c) + pos(c)            
            //    word(p) + pos(p) + pos(c)            
            //    word(p) + pos(p) + word(c)
            //    word(p) + word(c)
            //    pos(p) + pos(c)
            feats.add(encodeFeatureSSBB(templ++, flags, pWord, cWord, pPos, cPos));
            feats.add(encodeFeatureSBB_(templ++, flags, cWord, pPos, cPos));
            feats.add(encodeFeatureSSB_(templ++, flags, pWord, cWord, cPos));
            feats.add(encodeFeatureSBB_(templ++, flags, pWord, pPos, cPos));
            feats.add(encodeFeatureSSB_(templ++, flags, pWord, cWord, pPos));
            feats.add(encodeFeatureSS__(templ++, flags, pWord, cWord));
            feats.add(encodeFeatureBB__(templ++, flags, pPos, cPos));            
            
            //    # Surrounding Word POS Features
            //    pos(p) + pos(1(p)) + pos(-1(c)) + pos(c)
            //    pos(-1(p)) + pos(p) + pos(-1(c)) + pos(c)
            //    pos(p) + pos(1(p)) + pos(c) + pos(1(c))
            //    pos(-1(p)) + pos(p) + pos(c) + pos(1(c))
            feats.add(encodeFeatureBBBB(templ++, flags, pPos, rpPos, lcPos, cPos));
            feats.add(encodeFeatureBBBB(templ++, flags, lpPos, pPos, lcPos, cPos));
            feats.add(encodeFeatureBBBB(templ++, flags, pPos, rpPos, cPos, rcPos));
            feats.add(encodeFeatureBBBB(templ++, flags, lpPos, pPos, cPos, rcPos));
            
            //    # Backed-off versions of Surrounding Word POS Features
            //    pos(-1(p)) + pos(p) + pos(c)
            //    pos(p) + pos(-1(c)) + pos(c)
            //    pos(p) + pos(c) + pos(1(c))
            //    pos(p) + pos(1(p)) + pos(c)
            feats.add(encodeFeatureBBB_(templ++, flags, lpPos, pPos, cPos));
            feats.add(encodeFeatureBBB_(templ++, flags, pPos, lcPos, cPos));
            feats.add(encodeFeatureBBB_(templ++, flags, pPos, cPos, rcPos));
            feats.add(encodeFeatureBBB_(templ++, flags, pPos, rpPos, cPos));
            
            //    # In Between POS Features
            //    pos(p) + pos(1gram(btwn(p,c))) + pos(c)
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
                    feats.add(encodeFeatureSBB_(templ, flags, btwnPos[i], pPos, cPos));
                }
            }
            templ++;
            
            //    # These features are added for both the entire words as well as the
            //    # 5-gram prefix if the word is longer than 5 characters.
            //    chpre5(p) + pos(p)
            //    chpre5(p)
            //    chpre5(c) + pos(c)
            //    chpre5(c)
            //    chpre5(p) + pos(p) + chpre5(c) + pos(c)
            //    pos(p) + chpre5(c) + pos(c)
            //    chpre5(p) + chpre5(c) + pos(c)
            //    chpre5(p) + pos(p) + pos(c)
            //    chpre5(p) + pos(p) + chpre5(c)
            //    chpre5(p) + chpre5(c)
            if (pPrefixFeats) {
                feats.add(encodeFeatureSB__(templ++, flags, pPrefix, pPos));
                feats.add(encodeFeatureS___(templ++, flags, pPrefix));
                feats.add(encodeFeatureSBB_(templ++, flags, pPrefix, pPos, cPos));
            }
            if (cPrefixFeats) {
                feats.add(encodeFeatureSB__(templ++, flags, cPrefix, cPos));
                feats.add(encodeFeatureS___(templ++, flags, cPrefix));
                feats.add(encodeFeatureSBB_(templ++, flags, cPrefix, pPos, cPos));
            }
            if (pPrefixFeats || cPrefixFeats) {
                feats.add(encodeFeatureSSBB(templ++, flags, pPrefix, cPrefix, pPos, cPos));
                feats.add(encodeFeatureSSB_(templ++, flags, pPrefix, cPrefix, cPos));
                feats.add(encodeFeatureSSB_(templ++, flags, pPrefix, cPrefix, pPos));
                feats.add(encodeFeatureSS__(templ++, flags, pPrefix, cPrefix));
            }
        }
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
