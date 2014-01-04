package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.prim.tuple.Pair;

/**
 * Cache of features for a pair of words in a sentence (parent/predicate and
 * child/argument).
 * 
 * @author mmitchell
 * @author mgormley
 */
public class FeaturizedTokenPair {
    
    /* Feature constructor based on CoNLL 2009:
     * "Multilingual Dependency Learning:
     * A Huge Feature Engineering Method to Semantic Dependency Parsing"
     * Hai Zhao, Wenliang Chen, Chunyu Kit, Guodong Zhou 
     * and
     * "Multilingual Semantic Role Labeling"
     * Anders Bjo ̈rkelund, Love Hafdell, Pierre Nugues
     * Treats features as combinations of feature templates, for:
     * 1. word form (formFeats)
     * 2. lemma (lemmaFeats)
     * 3. part-of-speech (tagFeats)
     * 4. morphological features (morphFeats)
     * 5. syntactic dependency label (deprelFeats)
     * 6. children (childrenFeats)
     * 7. dependency paths (pathFeats)
     * 8. 'high' and 'low' support, siblings, parents (syntacticConnectionFeats).
     */    

    private int pidx;
    private int aidx;
    private FeaturizedToken pTok;
    private FeaturizedToken aTok;    
    private int[] parents;
    
    private ArrayList<Integer> linePath;
    private List<Pair<Integer, Dir>> dependencyPath;
    private ArrayList<Pair<Integer, Dir>> dpPathShare;
    private List<Pair<Integer, Dir>> dpPathPred;
    private List<Pair<Integer, Dir>> dpPathArg;
    
    public FeaturizedTokenPair(int pidx, int aidx, FeaturizedToken pTok, FeaturizedToken aTok, SimpleAnnoSentence sent) {
        assert pTok.getSent() == aTok.getSent();
        assert pTok.getSent() == sent;
        this.parents = sent.getParents();
        this.pTok = pTok;
        this.aTok = aTok;
        this.pidx = pidx;
        this.aidx = aidx;
        /* ZHAO:  Path. There are two basic types of path between the predicate and the argument candidates. 
         * One is the linear path (linePath) in the sequence, the other is the path in the syntactic 
         * parsing tree (dpPath). For the latter, we further divide it into four sub-types by 
         * considering the syntactic root, dpPath is the full path in the syntactic tree. */
    }
    
    // ------------------------ Getters and Caching Methods ------------------------ //
        
    public List<Pair<Integer,Dir>> getDependencyPath() {
        if (dependencyPath == null) {
            this.dependencyPath = DepTree.getDependencyPath(pidx, aidx, parents);
        }
        return dependencyPath;
    }
    
    public List<Pair<Integer, Dir>> getDpPathPred() {
        ensureDpPathShare();
        return dpPathPred;
    }
    
    public List<Pair<Integer, Dir>> getDpPathArg() {
        ensureDpPathShare();
        return this.dpPathArg;
    }
    
    public List<Pair<Integer, Dir>> getDpPathShare() {
        ensureDpPathShare();
        return dpPathShare;
    }
    
    private void ensureDpPathShare() {
        if (dpPathShare != null) {
            return;
        }        
        this.dpPathShare = new ArrayList<Pair<Integer,DepTree.Dir>>();
        /* ZHAO:  Leading two paths to the root from the predicate and the argument, respectively, 
         * the common part of these two paths will be dpPathShare. */
        List<Pair<Integer, Dir>> argRootPath = aTok.getRootPath();
        List<Pair<Integer, Dir>> predRootPath = pTok.getRootPath();
        int i = argRootPath.size() - 1;
        int j = predRootPath.size() - 1;
        Pair<Integer,DepTree.Dir> argP = argRootPath.get(i);
        Pair<Integer,DepTree.Dir> predP = predRootPath.get(j);
        while (argP.equals(predP)) {
            this.dpPathShare.add(argP);
            if (i == 0 || j == 0) {
                break;
            }
            i--;
            j--;
            argP = argRootPath.get(i);
            predP = predRootPath.get(j);
        }
        /* ZHAO:  Assume that dpPathShare starts from a node r', 
         * then dpPathPred is from the predicate to r', and dpPathArg is from the argument to r'. */
        // Reverse, so path goes towards the root.
        Collections.reverse(this.dpPathShare);
        int r;
        if (this.dpPathShare.isEmpty()) {
            r = -1;
            this.dpPathPred = new ArrayList<Pair<Integer, Dir>>();
            this.dpPathArg = new ArrayList<Pair<Integer, Dir>>();
        } else {
            r = this.dpPathShare.get(0).get1();
            this.dpPathPred = DepTree.getDependencyPath(pidx, r, parents);
            this.dpPathArg = DepTree.getDependencyPath(aidx, r, parents);
        }
    }
    
    public ArrayList<Integer> getLinePath() {
        if (linePath == null) {
            cacheLinePath();
        }
        return linePath;
    }
    
    private void cacheLinePath() {
        this.linePath = new ArrayList<Integer>();        
        if (pidx < aidx) {
            for (int i=pidx; i<=aidx; i++) {
                this.linePath.add(i);
            }
        } else {
            for (int i=pidx; i>=aidx; i--) {
                this.linePath.add(i);
            }
        }        
    }
    
    public String getGeneologicalRelation() {
        if (pidx == aidx) {
            return "self";
        } else if (hasParent(aidx) && pidx == parents[aidx]) {
            return "parent";
        } else if (hasParent(pidx) && parents[pidx] == aidx) {
            return "child";
        } else if (hasParent(aidx) && hasParent(pidx) && parents[pidx] == parents[aidx]) {
            return "sibling";
        } else if (pidx == -1) {
            return "ancestor";     // Short circuit to avoid ArrayIndexOutOfBounds.
        } else if (aidx == -1) {
            return "descendent";   // Short circuit to avoid ArrayIndexOutOfBounds.
        } else if (DepTree.isAncestor(pidx, aidx, parents)) {
            return "ancestor";
        } else if (DepTree.isAncestor(aidx, pidx, parents)) {
            return "descendent";
        } else {
            return "cousin";
        }
    }
    
    private boolean hasParent(int aidx) {
        return 0 <= aidx && aidx < parents.length;
    }

    public String getRelativePosition() {
        if (pidx == aidx) {
            return "on";
        } else if (pidx < aidx) {
            return "before";
        } else {
            return "after";
        }
    }

    public int getCountOfNonConsecutivesInPath() {
        List<Pair<Integer,Dir>> path = getDependencyPath();
        int count = 0;
        if (path.size() > 0) {
            for (int i=1; i<path.size(); i++) {
                int current = path.get(i).get1();
                int previous = path.get(i-1).get1();
                if (Math.abs(current - previous) != 1) {
                    count++;
                }
            }
        }
        return count;
    }
    
}
