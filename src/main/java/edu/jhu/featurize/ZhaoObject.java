package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.util.Pair;

public class ZhaoObject extends CoNLL09Token {
    
    /* Feature constructor based on CoNLL 09:
     * "Multilingual Dependency Learning:
     * A Huge Feature Engineering Method to Semantic Dependency Parsing"
     * Hai Zhao, Wenliang Chen, Chunyu Kit, Guodong Zhou */    

    private SentFeatureExtractorPrm prm;
    private CoNLL09Sentence sent;
    private int idx;
    private int parent;
    private String pos;
    private List<String> feat;
    private CoNLL09Token word;
    private ArrayList<Integer> children;
    private Integer farRightChild;
    private Integer farLeftChild;
    private Integer nearLeftChild;
    private Integer nearRightChild;
    private List<Pair<Integer, Dir>> rootPath;
    private int lowSupport;
    private int highSupport;
    private List<Pair<Integer, Dir>> betweenPath;
    private int[] parents;
    private ArrayList<Integer> linePath;
    private ArrayList<Pair<Integer, Dir>> dpPathShare;
    private List<Pair<Integer, Dir>> dpPathPred;
    private List<Pair<Integer, Dir>> dpPathArg;
    
    public ZhaoObject(int idx, int[] parents, CoNLL09Sentence sent, SentFeatureExtractorPrm prm, String support) {
        super(sent.get(idx));
        /* Call CoNLL09Token so that following ZHANG we can get Word Property features.
         * Includes:
         * 1. word form, 
         * 2. lemma, 
         * 3. part-of-speech tag (PoS), 
         * 4. FEAT (additional morphological features), 
         * 5. syntactic dependency label (dprel), 
         * 6. semantic dependency label (semdprel) 
         * 7. and characters (char) in the word form (only suitable for Chinese and Japanese). */
        this.idx = idx;
        this.prm = prm;
        this.sent = sent;
        this.parents = parents;
        // Basic strings available from input.
        // These are concatenated in different ways to create features.
        this.word = sent.get(idx);
        if (prm.useGoldSyntax) {
            this.pos = word.getPos();
        } else {
            this.pos = word.getPpos();            
        }
        setFeat();
        setRootPath();
        setParent();
        setChildren();
        /* ZHANG:  Syntactic Connection. This includes syntactic head (h), left(right) farthest(nearest) child (lm,ln,rm,rn), 
         * and high(low) support verb or noun.
         * From the predicate or the argument to the syntactic root along with the syntactic tree, 
         * the first verb(noun) that is met is called as the low support verb(noun), 
         * and the nearest one to the root is called as the high support verb(noun). */
        setFarthestNearestChildren();
        setHighLowSupport(support);
    }

    public ZhaoObject(int pidx, int aidx, ZhaoObject zhaoPred, ZhaoObject zhaoArg, int[] parents) {
        super(zhaoPred);
        this.parents = parents;
        this.linePath = new ArrayList<Integer>();
        this.dpPathShare = new ArrayList<Pair<Integer,DepTree.Dir>>();
        /* ZHANG:  Path. There are two basic types of path between the predicate and the argument candidates. 
         * One is the linear path (linePath) in the sequence, the other is the path in the syntactic 
         * parsing tree (dpPath). For the latter, we further divide it into four sub-types by 
         * considering the syntactic root, dpPath is the full path in the syntactic tree. */
        setBetweenPath(pidx, aidx);
        setLinePath(pidx, aidx);
        setDpPathShare(pidx, aidx, zhaoPred, zhaoArg);
        
    }
        
    public ZhaoObject(String input) {
        super(-1, input, input, input, input, input, null, null, -2, -2, input, input, false, input, null);
        setFeat();
    }
    
    
    // ------------------------ Getters and Setters ------------------------ //
   
    @Override 
    public String getPos() {
        return pos;
    }
    
    @Override
    public List<String> getFeat() {
        return feat;
    }
    
    public void setFeat() {
        feat = this.getFeat();
        if (feat == null) {
            feat = new ArrayList<String>();
            for (int i = 0; i < 6; i++) {
                feat.add("NO_MORPH");
            }
        } else if (feat.size() < 6) {
            for (int i = feat.size() - 1 ; i < 6; i++) {
                feat.add("NO_MORPH");
            }
        }
    }
    
    private List<Pair<Integer, Dir>> getRootPath() {
        return rootPath;
    }
    
    private void setRootPath() {
        this.rootPath = DepTree.getDependencyPath(idx, -1, parents);
    }

    public int getParent() {
        return parent;
    }
    
    public void setParent() {
        if (prm.useGoldSyntax) {
            this.parent = word.getHead() - 1;
        } else {
            this.parent = word.getPhead() - 1;
        }
    }
    
    public ArrayList<Integer> getChildren() {
        return children;
    }

    public void setChildren() {
        this.children = DepTree.getChildrenOf(parents, parent);
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
            this.farLeftChild = idx;
            this.nearLeftChild = idx;
        }
    
        if (!rightChildren.isEmpty()) {
            this.farRightChild = rightChildren.get(rightChildren.size() - 1);
            this.nearRightChild = rightChildren.get(0);
        } else {
            this.farRightChild = idx;
            this.nearRightChild = idx;
        }

    }
    
    public int getHighSupport() {
        return highSupport;
    }
    
    public int getLowSupport() {
        return lowSupport;
    }
    
    public void setHighLowSupport(String support) {
        // Support features
        String parentPos;
        boolean haveLow = false;
        int i;
      
        for (Pair<Integer,Dir> a : rootPath) {
            i = a.get1();
            if (i == -1) {
                break;
            }
            
            if (prm.useGoldSyntax) {
                parentPos = sent.get(i).getPos();
            } else {
                parentPos = sent.get(i).getPpos();
            }
            if (parentPos.equals(support)) {
                if (!haveLow) {
                    haveLow = true;
                    this.lowSupport = i;
                    this.highSupport = i;
                } else {
                    this.highSupport = i;
                }
            }
            
        }
    }
    
    
    public List<Pair<Integer,Dir>> getBetweenPath() {
        return betweenPath;
    }
    
    public void setBetweenPath(int pidx, int aidx) {
        this.betweenPath = DepTree.getDependencyPath(aidx, pidx, parents);
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
    
    private void setDpPathShare(int pidx, int aidx, ZhaoObject zhaoPred, ZhaoObject zhaoArg) {
        /* ZHANG:  Leading two paths to the root from the predicate and the argument, respectively, 
         * the common part of these two paths will be dpPathShare. */
        List<Pair<Integer, Dir>> argRootPath = zhaoArg.getRootPath();
        List<Pair<Integer, Dir>> predRootPath = zhaoPred.getRootPath();
        int i = argRootPath.size() - 1;
        int j = predRootPath.size() - 1;
        Pair<Integer,DepTree.Dir> argP = argRootPath.get(i);
        Pair<Integer,DepTree.Dir> predP = predRootPath.get(j);
        while (argP.equals(predP) && i > -1 && j > -1) {
            this.dpPathShare.add(argP);
            argP = argRootPath.get(i);
            predP = predRootPath.get(j);
            i--;
            j--;
        }
        /* ZHANG:  Assume that dpPathShare starts from a node r', 
         * then dpPathPred is from the predicate to r', and dpPathArg is from the argument to r'. */
        // Reverse, so path goes towards the root.
        Collections.reverse(this.dpPathShare);
        int r = this.dpPathShare.get(0).get1();
        this.dpPathPred = DepTree.getDependencyPath(pidx, r, parents);
        this.dpPathArg = DepTree.getDependencyPath(aidx, r, parents);
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

}
