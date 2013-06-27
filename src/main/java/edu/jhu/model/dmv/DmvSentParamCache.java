package edu.jhu.model.dmv;

import edu.jhu.data.Sentence;

public class DmvSentParamCache {

    // Probabilities or log-probabilities.
    public final double[] root; // Indexed by child position.
    public final double[][][] child; // Indexed by child position, parent position, and direction.
    public final double[][][][] decision; // Indexed by parent position, direction, valence (0 or 1), and STOP/CONT.    
        
    public DmvSentParamCache(DmvModel dmv, Sentence sent) {
        int[] tags = sent.getLabelIds();
        int sentLen = tags.length;
        
        this.root = new double[sentLen];
        this.child = new double[sentLen][sentLen][2];
        this.decision = new double[sentLen][2][2][2];
        
        // Cache the model parameters for this sentence.
        for (int c=0; c<sentLen; c++) {
            this.root[c] = dmv.root[tags[c]];
            for (int dir=0; dir<2; dir++) {
                for (int p=0; p<sentLen; p++) {
                    this.child[c][p][dir] = dmv.child[tags[c]][tags[p]][dir];
                }
                for (int val=0; val<2; val++) {
                    for (int sc=0; sc<2; sc++) {
                        this.decision[c][dir][val][sc] = dmv.decision[tags[c]][dir][val][sc];
                    }
                }
            }
        }
    }
    
}
