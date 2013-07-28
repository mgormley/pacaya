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

    private SentFeatureExtractorPrm prm;
    private CoNLL09Sentence sent;
    private ArrayList<Integer> children;
    private int parent;
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
    private String pos;
    
    public ZhaoObject(int idx, int[] parents, CoNLL09Sentence sent, SentFeatureExtractorPrm prm, String support) {
        super(sent.get(idx));
        this.prm = prm;
        this.sent = sent;
        this.parents = parents;
        // Basic strings available from input.
        // These are concatenated in different ways to create features.
        CoNLL09Token word = sent.get(idx);
        String form = word.getForm();
        String lemma = word.getLemma();
        if (prm.useGoldSyntax) {
            this.pos = word.getPos();
        } else {
            this.pos = word.getPpos();            
        }
        List<String> feats = word.getFeat();
        String sense = word.getPred();
        System.out.println(idx);
        setRootPath(idx);
        setParent(word);
        setChildren(parents);
        setFarthestNearestChildren();
        setHighLowSupport(support);
    }

    public ZhaoObject(int pidx, int aidx, ZhaoObject zhaoPred, ZhaoObject zhaoArg, int[] parents) {
        super(zhaoPred);
        this.parents = parents;
        this.linePath = new ArrayList<Integer>();
        this.dpPathShare = new ArrayList<Pair<Integer,DepTree.Dir>>();
        setBetweenPath(pidx, aidx);
        setLinePath(pidx, aidx);
        setDpPathShare(pidx, aidx, zhaoPred, zhaoArg);
        
    }
        
    public ZhaoObject(String input) {
        super(-1, input, input, input, input, input, null, null, -2, -2, input, input, false, input, null);
    }
    
    
    // ------------------------ Getters and Setters ------------------------ //
   
    @Override 
    public String getPos() {
        return this.pos;
    }
    
    private List<Pair<Integer, Dir>> getRootPath() {
        return this.rootPath;
    }
    
    private void setRootPath(int idx) {
        System.out.println(parents);
        this.rootPath = DepTree.getDependencyPath(idx, -1, parents);
    }

    public int getParent() {
        return this.parent;
    }
    
    public void setParent(CoNLL09Token t) {
        if (prm.useGoldSyntax) {
            this.parent = t.getHead() - 1;
        } else {
            this.parent = t.getPhead() - 1;
        }
    }
    
    public ArrayList<Integer> getChildren() {
        return this.children;
    }

    public void setChildren(int[] parents) {
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
            if (child < parent) {
                leftChildren.add(child);
            } else if (child > parent) {
                    rightChildren.add(child);
                }
              // Case where child == head skipped; neither right nor left.
            }
            
        if (!leftChildren.isEmpty()) {
            this.farLeftChild = leftChildren.get(0);
            this.nearLeftChild = leftChildren.get(leftChildren.size() - 1);
        }
    
        if (!rightChildren.isEmpty()) {
            this.farRightChild = rightChildren.get(rightChildren.size() - 1);
            this.nearRightChild = rightChildren.get(0);
        }
    }
    
    public int getHighSupport() {
        return this.highSupport;
    }
    
    public int getLowSupport() {
        return this.lowSupport;
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
            
            if (!prm.useGoldSyntax) {
                parentPos = sent.get(i).getPpos();
            } else {
                parentPos = sent.get(i).getPos();
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
        return this.betweenPath;
    }
    
    public void setBetweenPath(int pidx, int aidx) {
        this.betweenPath = DepTree.getDependencyPath(aidx, pidx, parents);
    }
    
    public List<Pair<Integer, Dir>> getDpPathPred() {
        return this.dpPathPred;
    }
    
    public List<Pair<Integer, Dir>> getDpPathArg() {
        return this.dpPathArg;
    }
    
    public List<Pair<Integer, Dir>> getDpPathShare() {
        return this.dpPathShare;
    }
    
    private void setDpPathShare(int pidx, int aidx, ZhaoObject zhaoPred, ZhaoObject zhaoArg) {
        /* Leading two paths to the root from the predicate and the argument, respectively, 
         * the common part of these two paths will be dpPathShare. */
        List<Pair<Integer, Dir>> argRootPath = zhaoArg.getRootPath();
        List<Pair<Integer, Dir>> predRootPath = zhaoPred.getRootPath();
        int i = argRootPath.size() - 1;
        int j = predRootPath.size() - 1;
        Pair<Integer,DepTree.Dir> argP = argRootPath.get(i);
        Pair<Integer,DepTree.Dir> predP = predRootPath.get(j);
        while (argP.equals(predP) && i > -1 && j > -1) {
            dpPathShare.add(argP);
            argP = argRootPath.get(i);
            predP = predRootPath.get(j);
            i--;
            j--;
        }
        // Reverse, so path goes towards the root.
        Collections.reverse(dpPathShare);
        int r = dpPathShare.get(0).get1();
        this.dpPathPred = DepTree.getDependencyPath(pidx, r, parents);
        this.dpPathArg = DepTree.getDependencyPath(aidx, r, parents);
    }
    
    public ArrayList<Integer> getLinePath() {
        return this.linePath;
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
