package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.berkeley.nlp.PCFGLA.smoothing.SrlBerkeleySignatureBuilder;
import edu.jhu.data.DepTree;
import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.gm.BinaryStrFVBuilder;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Pair;

/**
 * Feature extraction from the observations on a particular sentence.
 * 
 * @author mmitchell
 * @author mgormley
 */
public class SentFeatureExtractor {

    private static final Logger log = Logger.getLogger(SentFeatureExtractor.class); 

    /**
     * Parameters for the SentFeatureExtractor.
     * @author mgormley
     * @author mmitchell
     */
    public static class SentFeatureExtractorPrm {
        public boolean useGoldSyntax = false;
        public String language = "es";
        /**
         * Cutoff for OOV words. (This is actually used in CorpusStatistics, but
         * we'll just put it here for now.)
         */
        public int cutoff = 3;
        /**
         * Whether to normalize and clean words.
         */
        public boolean normalizeWords = false;
        /** For testing only: this will ensure that the only feature returned is the bias feature. */
        public boolean biasOnly = false;
        public boolean isProjective = false;
        public boolean withSupervision = true;
    }
    
    // Parameters for feature extraction.
    private SentFeatureExtractorPrm prm;
    
    private final CoNLL09Sentence sent;
    private final CorpusStatistics cs;
    private Alphabet<String> alphabet;
    private final SrlBerkeleySignatureBuilder sig;
    private final int[] parents;
        
    public SentFeatureExtractor(SentFeatureExtractorPrm prm, CoNLL09Sentence sent, CorpusStatistics cs, Alphabet<String> alphabet) {
        this.prm = prm;
        this.sent = sent;
        this.cs = cs;
        this.alphabet = alphabet;
        this.sig = cs.sig;
        // Syntactic parents of all the words in this sentence, in order (idx 0 is -1)
        this.parents = getParents(sent);
    }
    
    public int[] getParents(CoNLL09Sentence sent) {
        int[] __parents__ = new int[sent.size()+1];
        CoNLL09Token t;
        // All the parents.  Parents of the Root is -1.
        //__parents__[0] = -1;
        for (int i = 0; i < sent.size(); i++) {
            t = sent.get(i);
            int parent = t.getHead() - 1;
            __parents__[i] = parent;
        }
        return __parents__;
    }

    public int getSentSize() {
        return sent.size();
    }
    
    // ----------------- Extracting Features on the Observations ONLY -----------------

    /**
     * Creates a feature set for the given word position.
     * 
     * This defines a feature function of the form f(x, i), where x is a vector
     * representing all the observations about a sentence and i is a position in
     * the sentence.
     * 
     * Examples where this feature function would be used include a unary factor
     * on a Sense variable in SRL, or a syntactic Link variable where the parent
     * is the "Wall" node.
     * 
     * @param idx The position of a word in the sentence.
     * @return The features.
     */
    public BinaryStrFVBuilder createFeatureSet(int idx) {
        BinaryStrFVBuilder feats = new BinaryStrFVBuilder(alphabet);
        feats.add("BIAS_FEATURE");
        if (prm.biasOnly) { return feats; }
        
        addSimpleSoloFeatures(idx, feats);
        addNaradowskySoloFeatures(idx, feats);
        addZhaoSoloFeatures(idx, feats);
        return feats;
    }
    
    /**
     * Creates a feature set for the given pair of word positions.
     * 
     * This defines a feature function of the form f(x, i, j), where x is a
     * vector representing all the observations about a sentence and i and j are
     * positions in the sentence.
     * 
     * Examples where this feature function would be used include factors
     * including a Role or Link variable in the SRL model, where both the parent
     * and child are tokens in the sentence.
     * 
     * @param pidx The "parent" position.
     * @param aidx The "child" position.
     * @return The features.
     */
    public BinaryStrFVBuilder createFeatureSet(int pidx, int aidx) {
        BinaryStrFVBuilder feats = new BinaryStrFVBuilder(alphabet);
        feats.add("BIAS_FEATURE");
        if (prm.biasOnly) { return feats; }
        // Are feats updated without even returning them?
        addSimplePairFeatures(pidx, aidx, feats);
        addNaradowskyPairFeatures(pidx, aidx, feats);
        addZhaoPairFeatures(pidx, aidx, feats);
        // feats = getNuguesFeatures();
        return feats;
    }
    
    
    public void addSimpleSoloFeatures(int idx, BinaryStrFVBuilder feats) {
        String wordForm = sent.get(idx).getForm();
        System.out.println("word is " + wordForm);
        Set <String> a = sig.getSimpleUnkFeatures(wordForm, idx, prm.language);
        for (String c : a) {
            feats.add(c);
        }
    }
    
    
    public void addSimplePairFeatures(int pidx, int aidx, BinaryStrFVBuilder feats) {
        String predForm = sent.get(pidx).getForm();
        String argForm = sent.get(aidx).getForm();
        System.out.println("pred is " + predForm);
        System.out.println("arg is " + argForm);
        Set <String> a = sig.getSimpleUnkFeatures(predForm, pidx, prm.language);
        for (String c : a) {
            feats.add(c);
        }
        Set <String> b = sig.getSimpleUnkFeatures(argForm, aidx, prm.language);
        for (String c : a) {
            feats.add(c);
        }

    }
    
    public void addNaradowskySoloFeatures(int idx, BinaryStrFVBuilder feats) {
        CoNLL09Token word = sent.get(idx);
        String wordForm = decideForm(word.getForm(), idx);
        String wordPos = word.getPos();

        feats.add("head_" + wordForm + "_word");
        feats.add("arg_" + wordPos + "_tag");
        feats.add("slen_" + sent.size());
        if (prm.withSupervision) {
            List<String> wordFeats = word.getFeat();
            if (wordFeats == null) {
                wordFeats = new ArrayList<String>();
                wordFeats.add("_");
            }
            for (String m1 : wordFeats) {
                feats.add(m1 + "_morph");
            }
        }
    }
    
    public void addNaradowskyPairFeatures(int pidx, int aidx, BinaryStrFVBuilder feats) {
        CoNLL09Token pred = sent.get(pidx);
        CoNLL09Token arg = sent.get(aidx);
        String predForm = decideForm(pred.getForm(), pidx);
        String argForm = decideForm(arg.getForm(), aidx);
        String predPos = pred.getPos();
        String argPos = arg.getPos();
        if (!prm.useGoldSyntax) {
            predPos = pred.getPpos();
            argPos = arg.getPpos();
        } 
        String dir;
        int dist = Math.abs(aidx - pidx);
        if (aidx > pidx) 
            dir = "RIGHT";
        else if (aidx < pidx) 
            dir = "LEFT";
        else 
            dir = "SAME";
    
        feats.add("head_" + predForm + "dep_" + argForm + "_word");
        feats.add("head_" + predPos + "_dep_" + argPos + "_pos");
        feats.add("head_" + predForm + "_dep_" + argPos + "_wordpos");
        feats.add("head_" + predPos + "_dep_" + argForm + "_posword");
        feats.add("head_" + predForm + "_dep_" + argForm + "_head_" + predPos + "_dep_" + argPos + "_wordwordpospos");
    
        feats.add("head_" + predPos + "_dep_" + argPos + "_dist_" + dist + "_posdist");
        feats.add("head_" + predPos + "_dep_" + argPos + "_dir_" + dir + "_posdir");
        feats.add("head_" + predPos + "_dist_" + dist + "_dir_" + dir + "_posdistdir");
        feats.add("head_" + argPos + "_dist_" + dist + "_dir_" + dir + "_posdistdir");
    
        feats.add("slen_" + sent.size());
        feats.add("dir_" + dir);
        feats.add("dist_" + dist);
        feats.add("dir_dist_" + dir + dist);
    
        feats.add("head_" + predForm + "_word");
        feats.add("head_" + predPos + "_tag");
        feats.add("arg_" + argForm + "_word");
        feats.add("arg_" + argPos + "_tag");
        
        
        if (prm.withSupervision) {
            List<String> predFeats = pred.getFeat();
            List<String> argFeats = arg.getFeat();
            if (predFeats == null) {
                predFeats = new ArrayList<String>();
                predFeats.add("_");
            }
            if (argFeats == null) {
                argFeats = new ArrayList<String>();
                argFeats.add("_");
            }
            for (String m1 : predFeats) {
                for (String m2 : argFeats) {
                    feats.add(m1 + "_" + m2 + "_morph");
                }
            }
        }
    }

    public void addZhaoSoloFeatures(int idx, BinaryStrFVBuilder feats) {
        // TODO:
    }    
    
    public void addZhaoPairFeatures(int pidx, int aidx, BinaryStrFVBuilder feats) {
        int lastPidx = pidx - 1;
        int nextPidx = pidx + 1;
        int lastAidx = aidx - 1;
        int nextAidx = aidx + 1;
        // Features based on CoNLL 09:
        // "Multilingual Dependency Learning:
        // A Huge Feature Engineering Method to Semantic Dependency Parsing"
        // Hai Zhao, Wenliang Chen, Chunyu Kit, Guodong Zhou
        // Feature template 1:  Syntactic path based on semantic dependencies
        // Features based on CoNLL 09:
        // "Multilingual Dependency Learning:
        // A Huge Feature Engineering Method to Semantic Dependency Parsing"
        // Hai Zhao, Wenliang Chen, Chunyu Kit, Guodong Zhou
        /*
         * Word Property. This type of elements include:
         * 1. word form, 
         * 2. lemma, 
         * 3. part-of-speech tag (PoS), 
         * 4. FEAT (additional morphological features), 
         * 5. syntactic dependency label (dprel), 
         * 6. semantic dependency label (semdprel) 
         * 7. and characters (char) in the word form (only suitable for Chinese and Japanese).
         * 
         * MEG:  (1), (3), (4) all in Naradowsky.
         * What is (6)?  We don't have this in Spanish, English?
         */
                
        CoNLL09Token pred = sent.get(pidx);
        CoNLL09Token arg = sent.get(aidx);
        // 2:  Lemmas
        String predForm = pred.getForm();
        String argForm = arg.getForm();
        String predLemma = pred.getLemma();
        String argLemma = arg.getLemma();
        String predPos = pred.getPpos();
        String argPos = arg.getPpos();
        if (prm.useGoldSyntax) {
            predPos = pred.getPos();
            argPos = arg.getPos();
        }
        List<String> predFeats = pred.getFeat();
        List<String> argFeats = arg.getFeat();
        int predHead;
        int argHead;
        if (prm.useGoldSyntax) {
            predHead = pred.getHead() - 1;
            argHead = arg.getHead() - 1;
        } else {
            predHead = pred.getPhead() - 1;
            argHead = arg.getPhead() - 1;
        }
        String predSense = pred.getPred();
        String argSense = arg.getPred();
        
        List<Pair<Integer,Dir>> betweenPath = DepTree.getDependencyPath(aidx, pidx, parents);
        List<Pair<Integer,Dir>> predRootPath = DepTree.getDependencyPath(pidx, -1, parents);
        List<Pair<Integer,Dir>> argRootPath = DepTree.getDependencyPath(aidx, -1, parents);
        //System.out.println("predRootPath is ");
        //System.out.println(predRootPath);
        //System.out.println("argRootPath is ");
        
        
        /* Syntactic Connection. This includes syntactic head (h), left(right) farthest(nearest) child (lm,ln,rm,rn), 
         * and high(low) support verb or noun.
         *  From the predicate or the argument to the syntactic root along with the syntactic tree, 
         *  the first verb(noun) that is met is called as the low support verb(noun), 
         *  and the nearest one to the root is called as the high support verb(noun).*/
        ArrayList<Integer> predChildren = new ArrayList<Integer>(); 
        ArrayList<Integer> predNoFarChildren = new ArrayList<Integer>(); 
        predChildren = DepTree.getChildrenOf(parents, predHead);
        // TBD:  predNoFarChildren
        ArrayList<Integer> argChildren = new ArrayList<Integer>(); 
        ArrayList<Integer> argNoFarChildren = new ArrayList<Integer>(); 
        //System.out.println("Getting children of " + argHead);
        argChildren = DepTree.getChildrenOf(parents, argHead);
        // TBD:  argNoFarChildren
        
        //System.out.println("Looking through arg children ");
        //System.out.println(argChildren);
        Pair<Pair<Integer,Integer>,Pair<Integer,Integer>> argFarNearChildren = syntacticConnectionFarthestNearestChildren(argHead, argChildren);
        Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>  predFarNearChildren = syntacticConnectionFarthestNearestChildren(predHead, predChildren);
        
        //System.out.println(argRootPath);
        Pair<Integer,Integer> argSupport = syntacticConnectionHighLowSupport("n", argRootPath);
        Pair<Integer,Integer>  predSupport = syntacticConnectionHighLowSupport("v", predRootPath);        
        
        /* Semantic Connection. This includes semantic head (semhead), left(right) farthest(nearest) semantic child (semlm, semln, semrm, semrn). We say a predicate is its argument���s semantic head, and the latter is the former���s child. Features related to this type may track the current semantic parsing status.
        
        /* Path. There are two basic types of path between the predicate and the argument candidates. 
         * One is the linear path (linePath) in the sequence */
        /* the other is the path in the syntactic 
         * parsing tree (dpPath). For the latter, we further divide it into four sub-types by 
         * considering the syntactic root, dpPath is the full path in the syntactic tree. */
        
        /* Leading two paths to the root from the predicate and the argument, respectively, 
         * the common part of these two paths will be dpPathShare. */
        List<Pair<Integer,DepTree.Dir>> dpPathShare = new ArrayList<Pair<Integer,DepTree.Dir>>();
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
        //System.out.println("r is ");
        //System.out.println(r);
        /* Assume that dpPathShare starts from a node r', 
         * then dpPathPred is from the predicate to r', and dpPathArg is from the argument to r'. */
        List<Pair<Integer,Dir>> dpPathPred = DepTree.getDependencyPath(pred.getId(), r, parents);
        List<Pair<Integer,Dir>> dpPathArg = DepTree.getDependencyPath(arg.getId(), r, parents);

        //System.out.println("dpPathPred is ");
        //System.out.println(dpPathPred);
        //System.out.println("dpPathArg is ");
        //System.out.println(dpPathArg);
        ArrayList<Integer> linePath = new ArrayList<Integer>();
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
            linePath.add(startIdx);
            startIdx++;
        }

                
        /* TBD: Family. Two types of children sets for the predicate or argument candidate are considered, 
         * the first includes all syntactic children (children), the second also includes all but 
         * excludes the left most and the right most children (noFarChildren). */
        
        // Initialize ArrayLists we will use for features.
        int predLm = predFarNearChildren.get1().get1();
        CoNLL09Token argLm = sent.get(argFarNearChildren.get1().get1());
        CoNLL09Token argRm = sent.get(argFarNearChildren.get2().get1());
        CoNLL09Token argRn = sent.get(argFarNearChildren.get2().get2());
        ArrayList<CoNLL09Token> argChildrenTokens = new ArrayList<CoNLL09Token>();
        for (int child : argChildren) {
            argChildrenTokens.add(sent.get(child));
        }
        ArrayList<CoNLL09Token> betweenPathTokens = new ArrayList<CoNLL09Token>();
        for (Pair<Integer,Dir> p : betweenPath) {
            betweenPathTokens.add(sent.get(p.get1()));
        }
        ArrayList<CoNLL09Token> linePathCoNLL = new ArrayList<CoNLL09Token>();
        CoNLL09Token token;
        for (int p : linePath) {
            token = sent.get(p);
            linePathCoNLL.add(token);
        }

        
        String feat = null;
        // Add the supervised features
        if (prm.withSupervision) {
            // ------- Predicate features (supervised) ------- 
            // p.currentSense + p.lemma 
            feats.add(predSense + predLemma);
            // p.currentSense + p.pos 
            feats.add(predSense + predPos);
            // p.currentSense + a.pos 
            feats.add(predSense + argPos);
            // p_1 .FEAT1
            feats.add(sent.get(lastPidx).getFeat().get(0));
            // p.FEAT2
            feats.add(predFeats.get(1));
            // p1 .FEAT3
            feats.add(sent.get(nextPidx).getFeat().get(2));
            // TBD:  p.semrm.semdprel  What is this?            
            // p.lm.dprel
            feats.add(sent.get(predLm).getDeprel());
            // p.form + p.children.dprel.bag 
            ArrayList<String> depPredChildren = new ArrayList<String>();
            for (Integer child : predChildren) {
                depPredChildren.add(sent.get(child).getDeprel());
            }
            String bagDepPredChildren = bag(depPredChildren).toString();
            feats.add(predForm + bagDepPredChildren);
            // p.lemma_n (n = -1, 0) 
            feats.add(sent.get(lastPidx).getLemma());
            feats.add(predLemma);
            // p.lemma + p.lemma1
            feats.add(predLemma + sent.get(nextPidx).getLemma());
            // p.pos + p.children.dprel.bag 
            feats.add(predPos + bagDepPredChildren);
            
            // ------- Argument features (supervised) ------- 
            // a.FEAT1 + a.FEAT3 + a.FEAT4 + a.FEAT5 + a.FEAT6 
            feat = argFeats.get(0) + argFeats.get(2) + argFeats.get(3) + argFeats.get(4) + argFeats.get(5);
            feats.add(feat);
            // a_1.FEAT2 + a.FEAT2 
            feat = sent.get(lastAidx).getFeat().get(1) + argFeats.get(1);
            feats.add(feat);
            // a.FEAT3 + a1.FEAT3
            feat = argFeats.get(2) + sent.get(nextAidx).getFeat().get(2);
            feats.add(feat);
            // a.FEAT3 + a.h.FEAT3 
            feat = sent.get(argHead).getFeat().get(2);
            feats.add(feat);
            // a.children.FEAT1.noDup 
            ArrayList<String> argChildrenFeat1 = new ArrayList<String>();
            for (CoNLL09Token child : argChildrenTokens) {
                argChildrenFeat1.add(child.getFeat().get(0));
            }
            List<String> argChildrenFeat1NoDup = noDup(argChildrenFeat1);
            feat = argChildrenFeat1NoDup.toString();
            feats.add(feat);
            // a.children.FEAT3.bag 
            ArrayList<String> argChildrenFeat3 = new ArrayList<String>();
            for (CoNLL09Token child : argChildrenTokens) {
                argChildrenFeat3.add(child.getFeat().get(2));
            }
            List<String> argChildrenFeat3Bag = bag(argChildrenFeat3);
            feat = argChildrenFeat3Bag.toString();
            feats.add(feat);
            // a.h.lemma
            feat = sent.get(argHead).getLemma();
            feats.add(feat);
            // a.lm.dprel + a.form
            feat = argLm.getDeprel();
            feats.add(feat);
            // TBD: a.lm_1.lemma
            // a.lmn.pos (n=0,1) 
            feat = argLm.getPos();
            feats.add(feat);
            // TBD: a.noFarChildren.pos.bag + a.rm.form 
            // TBD: a.pphead.lemma
            // a.rm.dprel + a.form
            feat = argRm.getDeprel() + argForm;
            feats.add(feat);
            // TBD: a.rm_1.form 
            // a.rm.lemma
            feat = argRm.getLemma();
            feats.add(feat);
            // a.rn.dprel + a.form 
            feat = argRn.getDeprel() + argForm;
            feats.add(feat);
            // a.lowSupportVerb.lemma 
            feat = sent.get(argSupport.get1()).getLemma();
            feats.add(feat);
            // a.lemma + a.h.form 
            feat = argLemma + sent.get(argHead).getForm();
            feats.add(feat);
            // a.lemma + a.pphead.form 
            // a1.lemma
            feat = sent.get(nextAidx).getLemma();
            feats.add(feat);
            // a.pos + a.children.dprel.bag
            ArrayList<String> argChildrenDeprel = new ArrayList<String>(); 
            for (CoNLL09Token child : argChildrenTokens) {
                argChildrenDeprel.add(child.getDeprel());
            }
            List<String> argChildrenDeprelBag = bag(argChildrenDeprel);
            feat = argPos + argChildrenDeprelBag.toString();
            feats.add(feat);
            
            // ------- Combined features (supervised) ------- 
            // a.lemma + p.lemma 
            feat = argLemma + predLemma;
            feats.add(feat);
            // (a:p|dpPath.dprel) + p.FEAT1 
            // a:p|dpPath.lemma.seq 
            // a:p|dpPath.lemma.bag 

            ArrayList<String> depRelPath = new ArrayList<String>();
            ArrayList<String> depRelPathLemma = new ArrayList<String>();
            for (CoNLL09Token t : betweenPathTokens) {
                depRelPath.add(t.getDeprel());
                depRelPathLemma.add(t.getLemma());
            }
            feat = depRelPath + predFeats.get(0);
            feats.add(feat);
            feat = depRelPathLemma.toString();
            feats.add(feat);
            feat = bag(depRelPathLemma).toString();
            feats.add(feat);
            
            // a:p|linePath.FEAT1.bag 
            // a:p|linePath.lemma.seq 
            // a:p|linePath.dprel.seq 
            ArrayList<String> linePathFeat = new ArrayList<String>();
            ArrayList<String> linePathLemma = new ArrayList<String>();
            ArrayList<String> linePathDeprel = new ArrayList<String>();
            for (CoNLL09Token t : linePathCoNLL) {
                linePathFeat.add(t.getFeat().get(0));
                linePathLemma.add(t.getLemma());
                linePathDeprel.add(t.getDeprel());
            }
            feats.add(feat);
            List<String> linePathFeatBag = bag(linePathFeat);
            feat = linePathFeatBag.toString();
            feats.add(feat);
            feat = linePathLemma.toString();
            feats.add(feat);
            feat = linePathDeprel.toString();
            feats.add(feat);
            ArrayList<String> dpPathLemma = new ArrayList<String>();
            for (Pair<Integer, Dir> dpP : dpPathArg) {
                dpPathLemma.add(sent.get(dpP.get1()).getLemma());
                
            }
            // a:p|dpPathArgu.lemma.seq 
            feat = dpPathLemma.toString();
            feats.add(feat);
            // a:p|dpPathArgu.lemma.bag
            feat = bag(dpPathLemma).toString();
            feats.add(feat);

            
        }
        
        // Add the unsupervised features
        // ------- Predicate features (unsupervised) ------- 
         // p.pos_1 + p.pos
         String feat12 = sent.get(lastPidx).getPpos() + predPos;
         if (prm.useGoldSyntax) {
             feat12 = sent.get(lastPidx).getPos() + predPos;
         }
         feats.add(feat12);
         // p.pos1
         String feat13 = sent.get(nextPidx).getPpos();
         if (prm.useGoldSyntax) {
             feat13 = sent.get(nextPidx).getPos();
         }
         feats.add(feat13);
        

        // ------- Argument features (unsupervised) ------- 
        // a.lm.form
        feat = argLm.getForm();
        feats.add(feat);
        // a_1.form
        feat = sent.get(lastAidx).getForm();
        feats.add(feat);
        // a.form + a1.form
        feat = argForm + sent.get(nextAidx).getForm();
        feats.add(feat);
        // a.form + a.children.pos 
        List<String> argChildrenPos = new ArrayList<String>();
        for (CoNLL09Token child : argChildrenTokens) {
            if (prm.useGoldSyntax) {
                argChildrenPos.add(child.getPos());
            } else {
                argChildrenPos.add(child.getPpos());
            }
        }
        feat = argForm + argChildrenPos.toString();
        feats.add(feat);
        // a1.pos + a.pos.seq
        if (prm.useGoldSyntax) {
            feat = sent.get(nextAidx).getPos() + argPos;            
        } else {
            feat = sent.get(nextAidx).getPpos() + argPos;            
        }
        feats.add(feat);
        
        // ------- Combined features (unsupervised) ------- 
        // a:p|linePath.distance 
        feat = Integer.toString(linePath.size());
        feats.add(feat);
        // a:p|linePath.form.seq 
        ArrayList<String> linePathForm = new ArrayList<String>();
        for (CoNLL09Token t : linePathCoNLL) {
            linePathForm.add(t.getForm());
        }
        feat = linePathForm.toString();
        feats.add(feat);
    }
    
    public List<String> bag(ArrayList<String> elements) {
        Set<String> bag = new HashSet<String>();
        for (String a : elements) {
            bag.add(a);
        }
        return asSortedList(bag);
    }
    
    public ArrayList<String> noDup(ArrayList<String> argChildrenFeat1) {
        ArrayList<String> noDupElements = new ArrayList<String>();
        String lastA = null;
        for (String a : argChildrenFeat1) {
            if (!a.equals(lastA)) {
                noDupElements.add(a);
            }
            lastA = a;
        }
        return noDupElements;
    }
    
    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
      List<T> list = new ArrayList<T>(c);
      Collections.sort(list);
      return list;
    }
    
    public Pair<Pair<Integer,Integer>,Pair<Integer,Integer>> syntacticConnectionFarthestNearestChildren(int head, ArrayList<Integer> children) {
        // Farthest and nearest child to the left; farthest and nearest child to the right.
        ArrayList<Integer> leftChildren = new ArrayList<Integer>();
        ArrayList<Integer> rightChildren = new ArrayList<Integer>();
        // Go through children in order
        for (int child : children) {
            if (child < head) {
                leftChildren.add(child);
            } else if (child > head) {
                    rightChildren.add(child);
                }
              // Case where child == head skipped; neither right nor left.
            }
            
        //System.out.println("leftChildren is ");
        //System.out.println(leftChildren);
        //System.out.println("rightChildren is ");
        //System.out.println(rightChildren);
        int farLeftChild = -2;
        int farRightChild = -2;
        int closeLeftChild = -2;
        int closeRightChild = -2;
        if (!leftChildren.isEmpty()) {
            farLeftChild = leftChildren.get(0);
            closeLeftChild = leftChildren.get(leftChildren.size() - 1);
        }
    
        if (!rightChildren.isEmpty()) {
            farRightChild = rightChildren.get(rightChildren.size() - 1);
            closeRightChild = rightChildren.get(0);
        }
        Pair<Integer,Integer> distLeftChildren = new Pair<Integer,Integer>(farLeftChild, closeLeftChild);
        Pair<Integer,Integer> distRightChildren = new Pair<Integer,Integer>(farRightChild, closeRightChild);
    
        return new Pair<Pair<Integer, Integer>,Pair<Integer, Integer>>(distLeftChildren, distRightChildren);

    }
    
    
    public Pair<Integer,Integer> syntacticConnectionHighLowSupport(String support,  List<Pair<Integer,Dir>> rootPath) {
        // Support features
        String parentPos;
        boolean haveLow = false;
        int lowSupport = -2;
        int highSupport = -2;
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
                    lowSupport = i;
                    highSupport = i;
                } else {
                    highSupport = i;
                }
            }
            
        }
        return new Pair<Integer, Integer>(lowSupport, highSupport);

        
    }
    
    
        
        
    
    private String decideForm(String wordForm, int idx) {
        String cleanWord = cs.normalize.clean(wordForm);

        if (!cs.knownWords.contains(cleanWord)) {
            String unkWord = cs.sig.getSignature(cleanWord, idx, prm.language);
            unkWord = cs.normalize.escape(unkWord);
            if (!cs.knownUnks.contains(unkWord)) {
                unkWord = "UNK";
                return unkWord;
            }
        }
        
        return cleanWord;
    }
    
}
