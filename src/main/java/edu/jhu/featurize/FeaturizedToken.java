package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.prim.tuple.Pair;
import edu.stanford.nlp.util.StringUtils;

/**
 * Cache of features for a single word in a sentence, this permits indexing into
 * virtual "words" appearing before or after the sentence (BOS/EOS).
 * 
 * @author mmitchell
 */
public class FeaturizedToken {
    
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
    private String featStr;
    private ArrayList<String> feat;
    private Integer farRightChild;
    private Integer farLeftChild;
    private Integer nearLeftChild;
    private Integer nearRightChild;
    private List<Pair<Integer, Dir>> rootPath;
    private int lowSupportNoun;
    private int highSupportNoun;
    private int lowSupportVerb;
    private int highSupportVerb;
    private int[] parents;
    private ArrayList<Integer> children;
    private ArrayList<Integer> noFarChildren;
    /* Additional features owing to Bjorkelund al. 2009 */
    private int nearLeftSibling;
    private int nearRightSibling;
    private Dir direction = null; // null indicates no direction
        
    public FeaturizedToken(int idx, int[] parents, SimpleAnnoSentence sent) {
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
        this.nearLeftSibling = -1;
        this.nearRightSibling = sent.size();
        cacheSiblings();   
        // Basic strings available from input.
        // These are concatenated in different ways to create features.
        cacheFeat();
        cacheFeatStr();
        cacheRootPath();
        cacheChildren();
        /* ZHAO:  Syntactic Connection. This includes syntactic head (h), left(right) farthest(nearest) child (lm,ln,rm,rn), 
         * and high(low) support verb or noun.
         * From the predicate or the argument to the syntactic root along with the syntactic tree, 
         * the first verb(noun) that is met is called as the low support verb(noun), 
         * and the nearest one to the root is called as the high support verb(noun). */
        cacheFarthestNearestChildren();
        cacheHighLowSupport();
        /* ZHAO: Family. Two types of children sets for the predicate or argument candidate are considered, 
         * the first includes all syntactic children (children), the second also includes all but 
         * excludes the left most and the right most children (noFarChildren). */
        cacheNoFarChildren();
    }    
    
    // ------------------------ Getters and Caching Methods ------------------------ //

    public ArrayList<String> getFeat() {
        return feat;
    }
    
    private void cacheFeat() {
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

    public String getFeatStr() {
        return featStr;
    }
    
    private void cacheFeatStr() {
        featStr = StringUtils.join(feat, "_");
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
    
    private void cacheRootPath() {
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

    private void cacheChildren() {
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
    
    
    private void cacheFarthestNearestChildren() {
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
    
    private void cacheNoFarChildren() {
        this.noFarChildren = new ArrayList<Integer>();
        this.noFarChildren.add(nearLeftChild);
        this.noFarChildren.add(nearRightChild);
    }
    
    public int getHighSupportNoun() {
        return highSupportNoun;
    }
    
    public int getLowSupportNoun() {
        return lowSupportNoun;
    }
    

    public int getHighSupportVerb() {
        return highSupportVerb;
    }
    
    public int getLowSupportVerb() {
        return lowSupportVerb;
    }

    
    private void cacheHighLowSupport() {
        // Support features
        String parentPos;
        boolean haveArgLow = false;
        boolean havePredLow = false;
        int i;
        // TODO: This is hardcoded to the Spanish POS tags.
        String supportNounTag = "n";
        String supportVerbTag = "v";

        this.lowSupportNoun = -1;
        this.highSupportNoun = -1;
        this.lowSupportVerb = -1;
        this.highSupportVerb = -1;
        for (Pair<Integer,Dir> a : rootPath) {
            i = a.get1();
            if (i == -1) {
                break;
            }
            parentPos = sent.getPosTag(i);
            if (parentPos.equals(supportNounTag)) {
                if (!haveArgLow) {
                    haveArgLow = true;
                    this.lowSupportNoun = i;
                    this.highSupportNoun = i;
                } else {
                    this.highSupportNoun = i;
                }
            } else if (parentPos.equals(supportVerbTag)) {
                if (!havePredLow) {
                    havePredLow = true;
                    this.lowSupportVerb = i;
                    this.highSupportVerb = i;
                } else {
                    this.highSupportVerb = i;
                }
            }
            
        }
    }
    
    public int getNearRightSibling() {
        return nearRightSibling;
    }

    public int getNearLeftSibling() {
        return nearLeftSibling;
    }
        
    private void cacheSiblings() {
        if (idx >= 0 && idx < sent.size()) {
            ArrayList<Integer> siblingsList = DepTree.getSiblingsOf(this.parents, idx);
            Collections.sort(siblingsList);
            int wantedIndex = siblingsList.indexOf(idx);
            int rightSiblingIdx = wantedIndex + 1;
            int leftSiblingIdx = wantedIndex - 1;
            if (leftSiblingIdx >= 0) {
                this.nearLeftSibling = siblingsList.get(leftSiblingIdx);
            }
            if (rightSiblingIdx < siblingsList.size()) {
                this.nearRightSibling = siblingsList.get(rightSiblingIdx);
            }
        }
    }

    public Dir getDirection() {
        return direction;
    }

    // TODO: Remove this when possible.
    @Deprecated
    public void setDirection(Dir dir) {
        this.direction = dir;
    }
    
}
