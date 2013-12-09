package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.prim.tuple.Pair;

public class FeatureObject {
    
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

    private static final String NO_MORPH = "NO_MORPH"; 

    private SimpleAnnoSentence sent;
    private int idx = -1;
    private ArrayList<String> feat;
    private ArrayList<Integer> children;
    private Integer farRightChild;
    private Integer farLeftChild;
    private Integer nearLeftChild;
    private Integer nearRightChild;
    private List<Pair<Integer, Dir>> rootPath;
    private int argLowSupport;
    private int argHighSupport;
    private int predLowSupport;
    private int predHighSupport;
    private List<Pair<Integer, Dir>> dependencyPath;
    private int[] parents;
    private ArrayList<Integer> linePath;
    private ArrayList<Pair<Integer, Dir>> dpPathShare;
    private List<Pair<Integer, Dir>> dpPathPred;
    private List<Pair<Integer, Dir>> dpPathArg;
    private ArrayList<Integer> noFarChildren;
    /* Additional features owing to Bjorkelund al. 2009 */
    private int leftSibling;
    private int rightSibling;
    private Dir direction = null; // null indicates no direction
    
    
    public FeatureObject(int idx, int[] parents, SimpleAnnoSentence sent) {
        /* Need following ZHAO we can get Word Property features.
         * Includes:
         * 1. word form, 
         * 2. lemma, 
         * 3. part-of-speech tag (PoS), 
         * 4. FEAT (additional morphological features), 
         * 5. syntactic dependency label (dprel), 
         * 6. semantic dependency label (semdprel) 
         * 7. and characters (char) in the word form (only suitable for Chinese and Japanese). */
        /* Following BJORKELUND, we additionally add sibling features */
        this.idx = idx;
        this.sent = sent;
        this.parents = parents;
        this.leftSibling = -1;
        this.rightSibling = sent.size();
        setSiblings();   
        // Basic strings available from input.
        // These are concatenated in different ways to create features.
        setFeat();
        setRootPath();
        setChildren();
        /* ZHAO:  Syntactic Connection. This includes syntactic head (h), left(right) farthest(nearest) child (lm,ln,rm,rn), 
         * and high(low) support verb or noun.
         * From the predicate or the argument to the syntactic root along with the syntactic tree, 
         * the first verb(noun) that is met is called as the low support verb(noun), 
         * and the nearest one to the root is called as the high support verb(noun). */
        setFarthestNearestChildren();
        setHighLowSupport();
        /* ZHAO: Family. Two types of children sets for the predicate or argument candidate are considered, 
         * the first includes all syntactic children (children), the second also includes all but 
         * excludes the left most and the right most children (noFarChildren). */
        setNoFarChildren();
    }

    // TODO: This constructor suggests that we should divide this class into 
    // a FeaturizedToken and FeaturizedTokenPair.
    // They would have different access patterns and cache different things.
    public FeatureObject(int pidx, int aidx, FeatureObject zhaoPred, FeatureObject zhaoArg, int[] parents) {
        this.parents = parents;
        this.linePath = new ArrayList<Integer>();
        this.dpPathShare = new ArrayList<Pair<Integer,DepTree.Dir>>();
        /* ZHAO:  Path. There are two basic types of path between the predicate and the argument candidates. 
         * One is the linear path (linePath) in the sequence, the other is the path in the syntactic 
         * parsing tree (dpPath). For the latter, we further divide it into four sub-types by 
         * considering the syntactic root, dpPath is the full path in the syntactic tree. */
        setDependencyPath(pidx, aidx);
        setLinePath(pidx, aidx);
        setDpPathShare(pidx, aidx, zhaoPred, zhaoArg);
    }
    
    
    // ------------------------ Getters and Setters ------------------------ //

    public ArrayList<String> getFeat() {
        return feat;
    }
    
    public void setFeat() {
        feat = new ArrayList<String>(6);
        if (idx == -1 || idx >= sent.size()) {
            for (int i = 0; i < 6; i++) {
                feat.add(NO_MORPH);
            }            
        } else {
            List<String> coNLLFeats = sent.getFeats(idx);
            if (coNLLFeats == null) {
                for (int i = 0; i < 6; i++) {
                    feat.add(NO_MORPH);
                }
            } else {
                feat.addAll(coNLLFeats);
                for (int i = feat.size() ; i < 6; i++) {
                    feat.add(NO_MORPH);
                }
            }
        }
    }
    
    public String getForm() {
        if (idx < 0) {
            return "BEGIN_NO_FORM";
        } else if (idx >= sent.size()) {
            return "END_NO_FORM";
        }
        return sent.getWord(idx);
    }
    
    public String getLemma() {
        if (idx < 0) {
            return "BEGIN_NO_LEMMA";
        } else if (idx >= sent.size()) {
            return "END_NO_LEMMA";
        }
        return sent.getLemma(idx);
    }
    
    public String getPos() {
        if (idx < 0) {
            return "BEGIN_NO_POS";
        } else if (idx >= sent.size()) {
            return "END_NO_POS";
        }
        return sent.getPosTag(idx);
    }
    
    public String getDeprel() {
        if (idx < 0) {
            return "BEGIN_NO_DEPREL";
        } else if (idx >= sent.size()) {
            return "END_NO_DEPREL";
        }
        return sent.getDeprel(idx);
    }

    public int getParent() {
        if (idx < 0) {
            return -2;
        } else if (idx >= sent.size()) {
            return -1;
        }
        return sent.getParent(idx);
    }
    
    public List<Pair<Integer, Dir>> getRootPath() {
        return rootPath;
    }
    
    private void setRootPath() {
        if (idx < 0 || idx >= sent.size()) {
            this.rootPath =  new ArrayList<Pair<Integer,Dir>>();
            this.rootPath.add(new Pair<Integer, Dir>(-1,Dir.UP));
        } else {
            this.rootPath = DepTree.getDependencyPath(idx, -1, parents);
        }
    }
    
    public ArrayList<Integer> getChildren() {
        return children;
    }

    public void setChildren() {
        if (idx < 0 || idx >= sent.size()) {
            // Something that cannot possibly have children.
            this.children = new ArrayList<Integer>();
            this.children.add(-1);
        } else {
            this.children = DepTree.getChildrenOf(parents, idx);
        }
    }    
    
    public int getFarLeftChild() {
        return farLeftChild;
    }

    public int getFarRightChild() {
        return farRightChild;
    }
    
    public int getNearLeftChild() {
        return nearLeftChild;
    }
    
    public int getNearRightChild() {
        return nearRightChild;
    }
    
    
    public void setFarthestNearestChildren() {
        // Farthest and nearest child to the left; farthest and nearest child to the right.
        ArrayList<Integer> leftChildren = new ArrayList<Integer>();
        ArrayList<Integer> rightChildren = new ArrayList<Integer>();
        // Go through children in order
        for (int child : children) {
            if (child < idx) {
                leftChildren.add(child);
            } else if (child > idx) {
                rightChildren.add(child);
            }
              // Case where child == head skipped; neither right nor left.
        }
            
        if (!leftChildren.isEmpty()) {
            this.farLeftChild = leftChildren.get(0);
            this.nearLeftChild = leftChildren.get(leftChildren.size() - 1);
        } else {
            this.farLeftChild = -2;
            this.nearLeftChild = -2;
        }
    
        if (!rightChildren.isEmpty()) {
            this.farRightChild = rightChildren.get(rightChildren.size() - 1);
            this.nearRightChild = rightChildren.get(0);
        } else {
            this.farRightChild = -2;
            this.nearRightChild = -2;
        }

    }
    
    public ArrayList<Integer> getNoFarChildren() {
        return noFarChildren;
    }
    
    public void setNoFarChildren() {
        this.noFarChildren = new ArrayList<Integer>();
        this.noFarChildren.add(nearLeftChild);
        this.noFarChildren.add(nearRightChild);
    }
    
    public int getArgHighSupport() {
        return argHighSupport;
    }
    
    public int getArgLowSupport() {
        return argLowSupport;
    }
    

    public int getPredHighSupport() {
        return predHighSupport;
    }
    
    public int getPredLowSupport() {
        return predLowSupport;
    }

    
    public void setHighLowSupport() {
        // Support features
        String parentPos;
        boolean haveArgLow = false;
        boolean havePredLow = false;
        int i;
        // TODO: This is hardcoded to the Spanish POS tags.
        String argSupport = "n";
        String predSupport = "v";

        this.argLowSupport = -1;
        this.argHighSupport = -1;
        this.predLowSupport = -1;
        this.predHighSupport = -1;
        for (Pair<Integer,Dir> a : rootPath) {
            i = a.get1();
            if (i == -1) {
                break;
            }
            parentPos = sent.getPosTag(i);
            if (parentPos.equals(argSupport)) {
                if (!haveArgLow) {
                    haveArgLow = true;
                    this.argLowSupport = i;
                    this.argHighSupport = i;
                } else {
                    this.argHighSupport = i;
                }
            } else if (parentPos.equals(predSupport)) {
                if (!havePredLow) {
                    havePredLow = true;
                    this.predLowSupport = i;
                    this.predHighSupport = i;
                } else {
                    this.predHighSupport = i;
                }
            }
            
        }
    }
    
    
    public List<Pair<Integer,Dir>> getDependencyPath() {
        return dependencyPath;
    }
    
    public void setDependencyPath(int pidx, int aidx) {
        this.dependencyPath = DepTree.getDependencyPath(pidx, aidx, parents);
    }
    
    public List<Pair<Integer, Dir>> getDpPathPred() {
        return dpPathPred;
    }
    
    public List<Pair<Integer, Dir>> getDpPathArg() {
        return this.dpPathArg;
    }
    
    public List<Pair<Integer, Dir>> getDpPathShare() {
        return dpPathShare;
    }
    
    private void setDpPathShare(int pidx, int aidx, FeatureObject zhaoPred, FeatureObject zhaoArg) {

        /* ZHAO:  Leading two paths to the root from the predicate and the argument, respectively, 
         * the common part of these two paths will be dpPathShare. */
        List<Pair<Integer, Dir>> argRootPath = zhaoArg.getRootPath();
        List<Pair<Integer, Dir>> predRootPath = zhaoPred.getRootPath();
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
        return linePath;
    }
    
    public void setLinePath(int pidx, int aidx) {
        int startIdx;
        int endIdx; 
        if (pidx < aidx) {
            startIdx = pidx;
            endIdx = aidx;
        } else {
            startIdx = aidx;
            endIdx = pidx;
        }
        while (startIdx < endIdx) {
            this.linePath.add(startIdx);
            startIdx++;
        }
        
    }
    
    public void setSiblings() {
        if (idx >= 0 && idx < sent.size()) {
            ArrayList<Integer> siblingsList = DepTree.getSiblingsOf(idx, this.parents);
            Collections.sort(siblingsList);
            int wantedIndex = siblingsList.indexOf(idx);
            int rightSiblingIdx = wantedIndex + 1;
            int leftSiblingIdx = wantedIndex - 1;
            if (leftSiblingIdx >= 0) {
                this.leftSibling = siblingsList.get(leftSiblingIdx);
            }
            if (rightSiblingIdx < siblingsList.size()) {
                this.rightSibling = siblingsList.get(rightSiblingIdx);
            }
        }
    }
    
    public int getRightSibling() {
        return rightSibling;
    }

    public int getLeftSibling() {
        return leftSibling;
    }

    public void setDirection(Dir dir) {
        this.direction = dir;
        
    }

    public Dir getDirection() {
        return direction;
    }

    
}
