package edu.jhu.model.dmv;

import edu.jhu.data.Label;
import edu.jhu.data.TaggedWord;
import edu.jhu.prim.map.IntIntHashMap;
import edu.jhu.util.Alphabet;
import edu.jhu.util.dist.Beta;
import edu.jhu.util.dist.Gaussian;

public class DmvModelGenerator {

    public static DmvModel getTieredModel(int numTiers, int numTags, RealParamGen gen) {
        // Create the alphabet and tiers of tags.
        Alphabet<Label> alphabet = new Alphabet<Label>();
        IntIntHashMap idxTierMap = new IntIntHashMap(numTags, -1);
        Label[][] labels = new Label[numTiers][];
        int remain = numTags;
        int count = 0;
        for (int tier=0; tier<numTiers; tier++) {
            int numTagsInTier;
            if (tier == numTiers - 1) {
                // Give the remainder to the last tier.
                numTagsInTier = remain;
            } else {
                // Divide and round down.
                numTagsInTier = numTags / numTiers;
            }
            remain -= numTagsInTier;
            labels[tier] = new Label[numTagsInTier];
            for (int i=0; i<labels[tier].length; i++) {
                // Create one letter tag/word labels. Tags are capital, words are lowercase.
                // 65 --> A 
                // 97 --> a
                labels[tier][i] = new TaggedWord(Character.toString((char)(97 + i)) + tier, Character.toString((char)(65 + i)) + tier);
                count++;
                int idx = alphabet.lookupIndex(labels[tier][i]);
                idxTierMap.put(idx, tier);
            }
        }
        
        DmvModelFactory modelFactory = new RandomDmvModelFactory(1.0);
        DmvModel dmv = (DmvModel) modelFactory.getInstance(alphabet);
        dmv.fill(0.0);
        dmv.convertLogToReal();
        
        for (int c=0; c<numTags; c++) {
            // Get the tier of the child.
            int ctier = idxTierMap.get(c);
            
            dmv.root[c] = gen.getRoot(ctier, numTiers);

            for (int p=0; p<numTags; p++) {
                // Get the tier of the parent.
                int ptier = idxTierMap.get(p);
                
                for (int dir=0; dir<2; dir++) {
                    dmv.child[c][p][dir] = gen.getChild(ptier, ctier, numTiers);
                }
            }

            for (int dir=0; dir<2; dir++) {
                for (int val=0; val<2; val++) {
                    dmv.decision[c][dir][val][DmvModel.CONT] = gen.getCont(ctier, numTiers, val, dir);
                    dmv.decision[c][dir][val][DmvModel.END] = 1.0 - dmv.decision[c][dir][val][DmvModel.CONT];
                }
            }
        }

        dmv.normalize();
        dmv.convertRealToLog();
        dmv.assertLogNormalized(1e-13);
        return dmv;
    }
    
    public interface RealParamGen {

        double getRoot(int ctier, int numTiers);

        double getCont(int ctier, int numTiers, int val, int dir);

        double getChild(int ptier, int ctier, int numTiers);

    }
    
    /** This will give a fixed cascade, which always stops on the lowest tier. */
    public static class FixedRealParamGenerator implements RealParamGen {
    
        public double getRoot(int ctier, int numTiers) {
            if (ctier == 0) { 
                return 1.0;
            } else {
                return 0.0;
            }
        }
        
        public double getCont(int ctier, int numTiers, int val, int dir) {
            numTiers--;
            double prob;           
            if (val == 0) {
                prob = 1.0 * (numTiers - ctier) / (double) (numTiers);
            } else {
                //prob = 0.1 * (numTiers - ctier + 1) / (double) (numTiers + 1);
                prob = 0;
            }
                
            // Make these more right-branching.
            if (dir == DmvModel.LEFT) {            
                prob = 0;
            }
            return prob;
        }
    
        public double getChild(int ptier, int ctier, int numTiers) {
            double prop;
            if (ptier == ctier - 1) {
                // If parent is directly above the child.
                prop = 1.0;
            } else {
                prop = 0.0;
            }
            return prop;
        }
    
    }
    
    public static class StochasticRealParamGenerator implements RealParamGen {
    
        private double standardDeviation = 0.1;
            
        
        public double getRoot(int ctier, int numTiers) {
            // Do an exponential falloff, so that we prefer to generate the top tier from the root.
            double mean = 100 * Math.pow(0.1, ctier);
            return Gaussian.nextDouble(mean, standardDeviation);
        }
        
        public double getCont(int ctier, int numTiers, int val, int dir) {
            double prob;
            if (ctier == numTiers - 1) {
                // Never generate from the bottom tier.
                prob = 0.0;
            } else if (val == 0) {
                if (val == 0 && ctier == 0) {
                    // If top tier.
                    prob = 0.75;
                } else {
                    // Falloff with number of tiers so that we prefer to generate more children the closer we are to the top tier.
                    prob = Beta.nextDouble(10, 100 * Math.pow(0.5, ctier)); // (numTiers - ctier + 1) / (double) (numTiers + 1));
                }
            } else {
                prob = 0.1;
            }
                
            // Make these more right-branching.
            if (dir == DmvModel.LEFT) {            
                prob /= 2;
            }
            return prob;
        }
    
        public double getChild(int ptier, int ctier, int numTiers) {
            double mean;
            if (ptier == ctier - 1) {
                // If parent is directly above the child.
                mean = 100;
            } else if (ptier < ctier) {
                // If parent tier is above child tier.
                // Falloff exponentially with their difference.
                mean = 100 * Math.pow(0.5, Math.abs(ptier - ctier));
            } else if (ptier == ctier) {
                // If the same tier.
                mean = 25;
            } else {
                // If parent tier is below child tier.
                // 
                // Falloff exponentially with their difference (but very fast!)
                mean = 25 * Math.pow(0.1, Math.abs(ptier - ctier));
            }
            return Gaussian.nextDouble(mean, standardDeviation);
        }
    
    }
    
}
