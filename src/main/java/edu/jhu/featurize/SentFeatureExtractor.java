package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
import edu.jhu.util.Triple;

/**
 * Feature extraction from the observations on a particular sentence.
 * 
 * @author mmitchell
 * @author mgormley
 */
public class SentFeatureExtractor {

    private static final Logger log = Logger.getLogger(SentFeatureExtractor.class);

    private static final String NO_MORPH = "NO_MORPH"; 

    /**
     * Parameters for the SentFeatureExtractor.
     * @author mgormley
     * @author mmitchell
     */
    public static class SentFeatureExtractorPrm {
        /** For testing only: this will ensure that the only feature returned is the bias feature. */
        public boolean biasOnly = false;
        public boolean isProjective = false;
        public boolean withSupervision = true;
        /** Whether to add the "Simple" features. */
        public boolean useSimpleFeats = true;
        /** Whether to add the "Naradowsky" features. */
        public boolean useNaradFeats = true;
        /** Whether to add the "Zhao" features. */
        public boolean useZhaoFeats = true;
        public boolean useDepPathFeats = true;
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
    
    // Package private for testing.
    int[] getParents(CoNLL09Sentence sent) {
        if (cs.prm.useGoldSyntax) {
            return sent.getParentsFromHead();
        } else {
            return sent.getParentsFromPhead();
        }
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
        if (prm.biasOnly) { return feats; }
        
        if (prm.useSimpleFeats) {
            addSimpleSoloFeatures(idx, feats);
        }
        if (prm.useNaradFeats) {
            addNaradowskySoloFeatures(idx, feats);
        }
        if (prm.useZhaoFeats) {
            addZhaoSoloFeatures(idx, feats);
        }
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
        if (prm.biasOnly) { return feats; }
        
        if (prm.useSimpleFeats) {
            addSimplePairFeatures(pidx, aidx, feats);
        }
        if (prm.useNaradFeats) {
            addNaradowskyPairFeatures(pidx, aidx, feats);            
        }
        if (prm.useZhaoFeats) {
            addZhaoPairFeatures(pidx, aidx, feats);
        }
        if (prm.useDepPathFeats) {
            addDependencyPathFeatures(pidx, aidx, feats);
        }
        // feats = getNuguesFeatures();
        return feats;
    }
    
    
    public void addSimpleSoloFeatures(int idx, BinaryStrFVBuilder feats) {
        String wordForm = sent.get(idx).getForm();
        //System.out.println("word is " + wordForm);
        Set <String> a = sig.getSimpleUnkFeatures(wordForm, idx, cs.prm.language);
        for (String c : a) {
            feats.add(c);
        }
    }
    
    
    public void addSimplePairFeatures(int pidx, int aidx, BinaryStrFVBuilder feats) {
        String predForm = sent.get(pidx).getForm();
        String argForm = sent.get(aidx).getForm();
        //System.out.println("pred is " + predForm);
        //System.out.println("arg is " + argForm);
        Set <String> a = sig.getSimpleUnkFeatures(predForm, pidx, cs.prm.language);
        for (String c : a) {
            feats.add(c);
        }
        Set <String> b = sig.getSimpleUnkFeatures(argForm, aidx, cs.prm.language);
        for (String c : b) {
            feats.add(c);
        }

    }
    
    public void addNaradowskySoloFeatures(int idx, BinaryStrFVBuilder feats) {
        CoNLL09Token word = sent.get(idx);
        String wordForm = decideForm(word.getForm(), idx);
        String wordPos;
        if (cs.prm.useGoldSyntax) {
            wordPos = word.getPos();
        } else {
            wordPos = word.getPpos();
        }

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
        String predPos;
        String argPos;
        if (cs.prm.useGoldSyntax) {
            predPos = pred.getPos();
            argPos = arg.getPos();
        } else {
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

    public void addDependencyPathFeatures(int pidx, int aidx, BinaryStrFVBuilder feats) {
        ZhaoObject zhaoPred = new ZhaoObject(pidx, parents, sent, cs, "v");
        ZhaoObject zhaoArg = new ZhaoObject(aidx, parents, sent, cs, "n");
        ZhaoObject zhaoPredArgPair = new ZhaoObject(pidx, aidx, zhaoPred, zhaoArg, parents);
        List<Pair<Integer, Dir>> betweenPath = zhaoPredArgPair.getBetweenPath();
        String feat;
        ArrayList<String> depRelPathWord = new ArrayList<String>();
        ArrayList<CoNLL09Token> betweenPathTokens = getTokens(betweenPath);
        for (CoNLL09Token t : betweenPathTokens) {
            if (t == null) {
                depRelPathWord.add("WALL");
            } else {
                depRelPathWord.add(t.getForm());
            }
        }
        feat = StringUtils.join(depRelPathWord, "_");
        feats.add(feat);
        feat = StringUtils.join(bag(depRelPathWord), "_");
        feats.add(feat);
    }
    
    public void addZhaoSoloFeatures(int idx, BinaryStrFVBuilder feats) {
        // TODO:
    }    
    
    public void addZhaoPairFeatures(int pidx, int aidx, BinaryStrFVBuilder feats) {
        /* NOTE:  Not sure about they're "Semantic Connection" features.  What do they correspond to in CoNLL data? 
         * From paper: "This includes semantic head (semhead), left(right) farthest(nearest) 
         * semantic child (semlm, semln, semrm, semrn). 
         * We say a predicate is its argument's semantic head, and the latter is the former's child. 
         * Features related to this type may track the current semantic parsing status." */

        ZhaoObject zhaoPred = new ZhaoObject(pidx, parents, sent, cs, "v");
        ZhaoObject zhaoArg = new ZhaoObject(aidx, parents, sent, cs, "n");
        ZhaoObject zhaoPredArgPair = new ZhaoObject(pidx, aidx, zhaoPred, zhaoArg, parents);
        ZhaoObject zhaoPredLast;
        ZhaoObject zhaoPredNext;
        ZhaoObject zhaoPredParent;
        ZhaoObject zhaoArgLast;
        ZhaoObject zhaoArgNext;
        ZhaoObject zhaoArgParent;
        
        if (pidx - 1 > 0) {
            zhaoPredLast = new ZhaoObject(pidx - 1, parents, sent, cs, "v");
        } else {
            zhaoPredLast = new ZhaoObject("BEGIN");
        }
        
        if (pidx + 1 < sent.size()) {
            zhaoPredNext = new ZhaoObject(pidx + 1, parents, sent, cs, "v");
        } else {
            zhaoPredNext = new ZhaoObject("END");
        }
        
        if (zhaoPred.getParent() < 0) {
            zhaoPredParent = new ZhaoObject("BEGIN");
        } else {
            zhaoPredParent = new ZhaoObject(zhaoPred.getParent(), parents, sent, cs, "v");
        }
        
        if (aidx - 1 > 0) {
            zhaoArgLast = new ZhaoObject(aidx - 1, parents, sent, cs, "n");
        } else {
            zhaoArgLast = new ZhaoObject("BEGIN");
        }
        
        if (aidx + 1 < sent.size()) {
            zhaoArgNext = new ZhaoObject(aidx + 1, parents, sent, cs, "n");
        } else {
            zhaoArgNext = new ZhaoObject("END");
        }
        
        if (zhaoArg.getParent() < 0) {
            zhaoArgParent = new ZhaoObject("BEGIN");
        } else {
            zhaoArgParent = new ZhaoObject(zhaoArg.getParent(), parents, sent, cs, "v");
        }
        
        ArrayList<Integer> predChildren = zhaoPred.getChildren();
        ArrayList<Integer> argChildren = zhaoArg.getChildren();
        List<Pair<Integer, Dir>> betweenPath = zhaoPredArgPair.getBetweenPath();
        
        // Initialize Path structures.
        List<Pair<Integer, Dir>> dpPathPred = zhaoPredArgPair.getDpPathPred();
        List<Pair<Integer, Dir>> dpPathArg = zhaoPredArgPair.getDpPathArg();
        ArrayList<Integer> linePath = zhaoPredArgPair.getLinePath();
        
        ArrayList<CoNLL09Token> argChildrenTokens = getTokens(argChildren);
        ArrayList<CoNLL09Token> betweenPathTokens = getTokens(betweenPath);
        ArrayList<CoNLL09Token> linePathCoNLL = getTokens(linePath);
        
        // Add the supervised features
        if (prm.withSupervision) {
            addZhaoSupervisedPredFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, feats);
            addZhaoSupervisedArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, zhaoArgParent, argChildrenTokens, feats);
            addZhaoSupervisedCombinedFeats(zhaoPred, zhaoArg, feats, betweenPathTokens, linePathCoNLL, dpPathArg); 
        }
        
        // Add the unsupervised features
        addZhaoUnsupervisedPredFeats(zhaoPred, zhaoPredLast, zhaoPredNext, feats);
        addZhaoUnsupervisedArgFeats(zhaoArg, zhaoArgLast, zhaoArgNext, argChildrenTokens, feats);
        addZhaoUnsupervisedCombinedFeats(linePath, linePathCoNLL, feats);
    }

    private ArrayList<CoNLL09Token> getTokens(List<Pair<Integer, Dir>> path) {
        ArrayList<CoNLL09Token> pathTokens = new ArrayList<CoNLL09Token>();
        for (Pair<Integer,Dir> p : path) {
            if (p.get1() == -1) {
                pathTokens.add(null);
            } else {
                pathTokens.add(sent.get(p.get1()));
            }
        }
        return pathTokens;
    }

    private ArrayList<CoNLL09Token> getTokens(ArrayList<Integer> children) {
        ArrayList<CoNLL09Token> childrenTokens = new ArrayList<CoNLL09Token>();
        for (int child : children) {
            childrenTokens.add(sent.get(child));
        }
        return childrenTokens;
    }

    private void addZhaoUnsupervisedCombinedFeats(ArrayList<Integer> linePath, ArrayList<CoNLL09Token> linePathCoNLL, BinaryStrFVBuilder feats) {
        // ------- Combined features (unsupervised) ------- 
        String feat;
        // a:p|linePath.distance 
        feat = Integer.toString(linePath.size());
        feats.add(feat);
        // a:p|linePath.form.seq 
        ArrayList<String> linePathForm = new ArrayList<String>();
        for (CoNLL09Token t : linePathCoNLL) {
            linePathForm.add(t.getForm());
        }
        feat = StringUtils.join(linePathForm, "_");
        feats.add(feat);
    }

    private void addZhaoUnsupervisedArgFeats(ZhaoObject zhaoArg, ZhaoObject zhaoArgNext, ZhaoObject zhaoArgLast,
            ArrayList<CoNLL09Token> argChildrenTokens, BinaryStrFVBuilder feats) {
        // ------- Argument features (unsupervised) ------- 
        String feat;
        // a.lm.form
        feat = sent.get(zhaoArg.getFarLeftChild()).getForm();
        feats.add(feat);
        // a_1.form
        feat = zhaoArgLast.getForm();
        feats.add(feat);
        // a.form + a1.form
        feat = zhaoArg.getForm() + "_" + zhaoArgNext.getForm();
        feats.add(feat);
        // a.form + a.children.pos 
        List<String> argChildrenPos = new ArrayList<String>();
        for (CoNLL09Token child : argChildrenTokens) {
            if (cs.prm.useGoldSyntax) {
                argChildrenPos.add(child.getPos());
            } else {
                argChildrenPos.add(child.getPpos());
            }
        }
        feat = zhaoArg.getForm()  + "_" + StringUtils.join(argChildrenPos, "_");
        feats.add(feat);
        // a1.pos + a.pos.seq
        feat = zhaoArgNext.getPos() + "_" + zhaoArg.getPos();
        feats.add(feat);
    }

    private void addZhaoUnsupervisedPredFeats(ZhaoObject zhaoPred, ZhaoObject zhaoPredLast, ZhaoObject zhaoPredNext, BinaryStrFVBuilder feats) {
        // ------- Predicate features (unsupervised) ------- 
        // p.pos_1 + p.pos
        String feat12 = zhaoPredLast.getPos() + "_" + zhaoPred.getPos();
        feats.add(feat12);
        // p.pos1
        String feat13 = zhaoPredNext.getPos();
        feats.add(feat13);
    }

    
    private void addZhaoSupervisedCombinedFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, BinaryStrFVBuilder feats, ArrayList<CoNLL09Token> betweenPathTokens,
            ArrayList<CoNLL09Token> linePathCoNLL, List<Pair<Integer, Dir>> dpPathArg) {
        // ------- Combined features (supervised) ------- 
        String feat;
        // a.lemma + p.lemma 
        feat = zhaoArg.getLemma()  + "_" +  zhaoPred.getLemma();
        feats.add(feat);
        // (a:p|dpPath.dprel) + p.FEAT1 
        // a:p|dpPath.lemma.seq 
        // a:p|dpPath.lemma.bag 
        ArrayList<String> depRelPath = new ArrayList<String>();
        ArrayList<String> depRelPathLemma = new ArrayList<String>();
        for (CoNLL09Token t : betweenPathTokens) {
            if (t == null) {
                depRelPath.add("WALL");
                depRelPathLemma.add("WALL");
            } else {
                depRelPath.add(t.getDeprel());
                depRelPathLemma.add(t.getLemma());
            }
        }
        feat = StringUtils.join(depRelPath, "_")  + "_" +  zhaoPred.getFeat().get(0);
        feats.add(feat);
        feat = StringUtils.join(depRelPathLemma, "_");
        feats.add(feat);
        feat = StringUtils.join(bag(depRelPathLemma), "_");
        feats.add(feat);
        
        // a:p|linePath.FEAT1.bag 
        // a:p|linePath.lemma.seq 
        // a:p|linePath.dprel.seq 
        ArrayList<String> linePathFeat = new ArrayList<String>();
        ArrayList<String> linePathLemma = new ArrayList<String>();
        ArrayList<String> linePathDeprel = new ArrayList<String>();
        for (CoNLL09Token t : linePathCoNLL) {
            List<String> tFeat1 = t.getFeat();
            if (tFeat1 == null) {
                linePathFeat.add(NO_MORPH);
            } else {
                linePathFeat.add(t.getFeat().get(0));
            }
            linePathLemma.add(t.getLemma());
            linePathDeprel.add(t.getDeprel());
        }
        List<String> linePathFeatBag = bag(linePathFeat);
        feat = StringUtils.join(linePathFeatBag, "_");
        feats.add(feat);
        feat = StringUtils.join(linePathLemma, "_");
        feats.add(feat);
        feat = StringUtils.join(linePathDeprel, "_");
        feats.add(feat);
        ArrayList<String> dpPathLemma = new ArrayList<String>();
        for (Pair<Integer, Dir> dpP : dpPathArg) {
            dpPathLemma.add(sent.get(dpP.get1()).getLemma());            
        }
        // a:p|dpPathArgu.lemma.seq 
        feat = StringUtils.join(dpPathLemma, "_");
        feats.add(feat);
        // a:p|dpPathArgu.lemma.bag
        feat = StringUtils.join(bag(dpPathLemma), "_");
        feats.add(feat);
    }

    private void addZhaoSupervisedArgFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast,
            ZhaoObject zhaoPredNext, ZhaoObject zhaoArgLast, ZhaoObject zhaoArgNext, ZhaoObject zhaoArgParent, ArrayList<CoNLL09Token> argChildrenTokens, BinaryStrFVBuilder feats) {
        // ------- Argument features (supervised) ------- 
        getFirstThirdSupArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, zhaoArgParent, argChildrenTokens, feats);
        getSecondThirdSupArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, zhaoArgParent, argChildrenTokens, feats);
        getThirdThirdSupArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, zhaoArgParent, argChildrenTokens, feats);
    }


    private void getFirstThirdSupArgFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast,
            ZhaoObject zhaoPredNext, ZhaoObject zhaoArgLast, ZhaoObject zhaoArgNext, ZhaoObject zhaoArgParent, 
            ArrayList<CoNLL09Token> argChildrenTokens, BinaryStrFVBuilder feats) {
        String feat;
        List<String> zhaoArgFeats = zhaoArg.getFeat();
        String a1; String a2; String a3; String a4; String a5; String a6;
        a1 = zhaoArgFeats.get(0);
        a2 = zhaoArgFeats.get(1);
        a3 = zhaoArgFeats.get(2);
        a4 = zhaoArgFeats.get(3);
        a5 = zhaoArgFeats.get(4);
        a6 = zhaoArgFeats.get(5);
        List<String> zhaoArgLastFeats = zhaoArgLast.getFeat();
        String last_a1 = zhaoArgLastFeats.get(0);
        String last_a2 = zhaoArgLastFeats.get(1);
        List<String> zhaoArgNextFeats = zhaoArgNext.getFeat();
        String next_a1 = zhaoArgNextFeats.get(0);
        String next_a2 = zhaoArgNextFeats.get(1);
        String next_a3 = zhaoArgNextFeats.get(2);
        // a.FEAT1 + a.FEAT3 + a.FEAT4 + a.FEAT5 + a.FEAT6 
        feat = a1 + "_" + a3 + "_" + a4 + "_" + a5 + "_" + a6;
        feats.add(feat);
        // a_1.FEAT2 + a.FEAT2 
        feat = last_a2 + "_" + a2;
        feats.add(feat);
        // a.FEAT3 + a1.FEAT3
        feat = a3 + "_" + next_a3;
        feats.add(feat);
        // a.FEAT3 + a.h.FEAT3 
        if (zhaoArgParent.getFeat() == null) {
            feat = "PARENT_NO_MORPH";
        } else {
            feat = zhaoArgParent.getFeat().get(2);
        }
        feats.add(feat);
        // a.children.FEAT1.noDup 
        ArrayList<String> argChildrenFeat1 = getChildrenFeat1(argChildrenTokens);
        List<String> argChildrenFeat1NoDup = noDup(argChildrenFeat1);
        feat = StringUtils.join(argChildrenFeat1NoDup, "_");
        feats.add(feat);
        // a.children.FEAT3.bag 
        ArrayList<String> argChildrenFeat3 = getChildrenFeat3(argChildrenTokens);
        List<String> argChildrenFeat3Bag = bag(argChildrenFeat3);
        feat = StringUtils.join(argChildrenFeat3Bag, "_");
        feats.add(feat);
    }
    
    private ArrayList<String> getChildrenFeat3(ArrayList<CoNLL09Token> childrenTokens) {
        ArrayList<String> childrenFeat3 = new ArrayList<String>();
        for (CoNLL09Token child : childrenTokens) {
            if (child.getFeat() == null || child.getFeat().size() < 3) {
                childrenFeat3.add("CHILD_NO_MORPH");
            } else {
                childrenFeat3.add(child.getFeat().get(2));
            }
        }
        return childrenFeat3;
    }

    private ArrayList<String> getChildrenFeat1(ArrayList<CoNLL09Token> childrenTokens) {
        ArrayList<String> childrenFeat1 = new ArrayList<String>();
        for (CoNLL09Token child : childrenTokens) {
            if (child.getFeat() == null) {
                childrenFeat1.add("CHILD_NO_MORPH");
            } else {
                childrenFeat1.add(child.getFeat().get(0));
            }
        }
        return childrenFeat1;
    }

    private void getSecondThirdSupArgFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast,
            ZhaoObject zhaoPredNext, ZhaoObject zhaoArgLast, ZhaoObject zhaoArgNext, ZhaoObject zhaoArgParent, 
            ArrayList<CoNLL09Token> argChildrenTokens, BinaryStrFVBuilder feats) {
        CoNLL09Token argLm = sent.get(zhaoArg.getFarLeftChild());
        CoNLL09Token argRm = sent.get(zhaoArg.getFarRightChild());
        CoNLL09Token argRn = sent.get(zhaoArg.getNearRightChild());
        CoNLL09Token argLn = sent.get(zhaoArg.getNearLeftChild());
        String feat;
        // a.h.lemma
        feats.add(zhaoArgParent.getLemma());
        // a.lm.dprel + a.form
        feats.add(argLm.getDeprel() + "_" + zhaoArg.getForm());
        // a.lm_1.lemma
        if (zhaoArgLast.getFarLeftChild() < 0) {
            feats.add("NO_LM_CHILD_LEMMA");
        } else {
            feats.add(sent.get(zhaoArgLast.getFarLeftChild()).getLemma());
        }
        // a.lmn.pos (n=0,1) 
        feats.add(argLm.getPos());
        if (zhaoArgNext.getFarLeftChild() < 0) {
            feats.add("NO_LM_CHILD_POS");
        } else {
            feats.add(sent.get(zhaoArgNext.getFarLeftChild()).getPos());
        }
        // a.noFarChildren.pos.bag + a.rm.form 
        ArrayList<Integer> noFarChildren = zhaoArg.getNoFarChildren();
        ArrayList<String> noFarChildrenPos = new ArrayList<String>();
        if (cs.prm.useGoldSyntax) {
            for (Integer i : noFarChildren) {
                if (i < 0) {
                    feats.add("NO_FARCHILD_POS");
                } else {
                    noFarChildrenPos.add(sent.get(i).getPos()); 
                }
            }
        } else {
            for (Integer j : noFarChildren) {
                if (j < 0) {
                    feats.add("NO_FARCHILD_POS");
                } else {
                    noFarChildrenPos.add(sent.get(j).getPpos()); 
                }
            }
        }
        List<String> argNoFarChildrenBag = bag(noFarChildrenPos);
        feat = StringUtils.join(argNoFarChildrenBag, "_") + argRm.getForm();
        feats.add(feat);
        // a.pphead.lemma
        if (zhaoArg.getPhead() < 0) {
            feats.add("NO_PHEAD_LEMMA"); 
        } else {
            feats.add(sent.get(zhaoArg.getPhead()).getLemma());
        }
        // a.rm.dprel + a.form
        feats.add(argRm + "_" + zhaoArg.getForm());
        // a.rm_1.form 
        if (zhaoArgLast.getFarRightChild() < 0) {
            feats.add("NO_FARRIGHTCHILD_FORM");
        } else {
            feats.add(sent.get(zhaoArgLast.getFarRightChild()).getForm());
        }
        // a.rm.lemma
        feats.add(argRm.getLemma());
        // a.rn.dprel + a.form 
        feats.add(argRn.getDeprel() + "_" + zhaoArg.getForm());        
    }
    
    private void getThirdThirdSupArgFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast,
            ZhaoObject zhaoPredNext, ZhaoObject zhaoArgLast, ZhaoObject zhaoArgNext, ZhaoObject zhaoArgParent, 
            ArrayList<CoNLL09Token> argChildrenTokens, BinaryStrFVBuilder feats) {
        String feat;
        // a.lowSupportVerb.lemma 
        if (zhaoArg.getLowSupport() < 0) {
            feats.add("NO_LOWSUPPORT_LEMMA");
        } else {
            feats.add(sent.get(zhaoArg.getLowSupport()).getLemma());
        }
        // a.lemma + a.h.form 
        feats.add(zhaoArg.getLemma() + "_" + zhaoArgParent.getForm());
        // a.lemma + a.pphead.form 
        feats.add(zhaoArg.getLemma() + "_" + sent.get(zhaoArg.getPhead()).getForm());
        // a1.lemma
        feats.add(zhaoArgNext.getLemma());
        // a.pos + a.children.dprel.bag
        ArrayList<String> argChildrenDeprel = new ArrayList<String>(); 
        for (CoNLL09Token child : argChildrenTokens) {
            argChildrenDeprel.add(child.getDeprel());
        }
        List<String> argChildrenDeprelBag = bag(argChildrenDeprel);
        feat = zhaoArg.getPos() + StringUtils.join(argChildrenDeprelBag, "_");
        feats.add(feat);
    }



    private void addZhaoSupervisedPredFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast, ZhaoObject zhaoPredNext, BinaryStrFVBuilder feats) {
        // ------- Predicate features (supervised) ------- 
        // p.currentSense + p.lemma 
        feats.add(zhaoPred.getPred() + "_" + zhaoPred.getLemma());
        // p.currentSense + p.pos 
        feats.add(zhaoPred.getPred() + "_" + zhaoPred.getPos());
        // p.currentSense + a.pos 
        feats.add(zhaoPred.getPred() + "_" + zhaoArg.getPos());
        // p_1.FEAT1
        if (zhaoPredLast.getFeat() == null) {
            feats.add(zhaoPredLast.getForm());
        } else {
            feats.add(zhaoPredLast.getFeat().get(0));
        }
        // p.FEAT2
        feats.add(zhaoPred.getFeat().get(1));
        // p1.FEAT3
        if (zhaoPredNext.getFeat() == null) {
            feats.add(zhaoPredNext.getForm());
        } else {
            feats.add(zhaoPredNext.getFeat().get(2));
        }
        // NOTE:  This is supposed to be p.semrm.semdprel  What is this?  
        // I'm not sure.  Here's just a guess.
        if (zhaoPred.getFarRightChild() < 0) {
            feats.add("NO_FARRIGHT_DEPREL");
        } else {
            feats.add(sent.get(zhaoPred.getFarRightChild()).getDeprel());
        }
        // p.lm.dprel        
        if (zhaoPred.getFarLeftChild() < 0) {
            feats.add("NO_FARLEFT_DEPREL");
        } else {
            feats.add(sent.get(zhaoPred.getFarLeftChild()).getDeprel());
        }
        // p.form + p.children.dprel.bag 
        ArrayList<String> depPredChildren = new ArrayList<String>();
        for (Integer child : zhaoPred.getChildren()) {
            depPredChildren.add(sent.get(child).getDeprel());
        }
        String bagDepPredChildren = StringUtils.join(bag(depPredChildren), "_");
        feats.add(zhaoPred.getForm() + "_" + bagDepPredChildren);
        // p.lemma_n (n = -1, 0) 
        feats.add(zhaoPredLast.getLemma());
        feats.add(zhaoPred.getLemma());
        // p.lemma + p.lemma1
        feats.add(zhaoPred.getLemma() + "_" + zhaoPredNext.getLemma());
        // p.pos + p.children.dprel.bag 
        feats.add(zhaoPred.getPos() + "_" + bagDepPredChildren);
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
    
    private String decideForm(String wordForm, int idx) {
        String cleanWord = cs.normalize.clean(wordForm);

        if (!cs.knownWords.contains(cleanWord)) {
            String unkWord = cs.sig.getSignature(cleanWord, idx, cs.prm.language);
            unkWord = cs.normalize.escape(unkWord);
            if (!cs.knownUnks.contains(unkWord)) {
                unkWord = "UNK";
                return unkWord;
            }
        }
        
        return cleanWord;
    }
    
}
