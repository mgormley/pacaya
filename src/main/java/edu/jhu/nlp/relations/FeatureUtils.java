package edu.jhu.nlp.relations;

import java.util.Collection;
import java.util.List;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.hash.MurmurHash3;

public class FeatureUtils {

    /**
     * Adds each feature to fv using the given alphabet.
     */
    public static void addFeatures(Collection<String> obsFeats, FeatureNames alphabet, FeatureVector fv, boolean isBiasFeat, int featureHashMod) {
        if (featureHashMod <= 0) {
            // Just use the features as-is.
            for (String fname : obsFeats) {
                int fidx = alphabet.lookupIndex(fname);
                if (isBiasFeat) { alphabet.setIsBias(fidx); }
                if (fidx != -1) {
                    fv.add(fidx, 1.0);
                }
            }
        } else {
            // Apply the feature-hashing trick.
            for (String fname : obsFeats) {
                int hash = MurmurHash3.murmurhash3_x86_32(fname);
                hash = FastMath.mod(hash, featureHashMod);
                int fidx = alphabet.lookupIndex(hash);
                if (isBiasFeat) { alphabet.setIsBias(fidx); }
                if (fidx != -1) {
                    int revHash = FeatureUtils.reverseHashCode(fname);
                    if (revHash < 0) {
                        fv.add(fidx, -1.0);
                    } else {
                        fv.add(fidx, 1.0);
                    }
                }
            }
        }
    }

    /**
     * Returns the hash code of the reverse of this string.
     */
    public static int reverseHashCode(String fname) {
        int hash = 0;
        int n = fname.length();
        for (int i=n-1; i>=0; i--) {
            hash += 31 * hash + fname.charAt(i);
        }
        return hash;
    }
    
    /** Prepends a prefix to each string. */
    public static void addPrefix(List<String> strs, String prefix) {
        for (int i=0; i<strs.size(); i++) {
            strs.set(i, prefix + strs.get(i));
        }
    }

}
