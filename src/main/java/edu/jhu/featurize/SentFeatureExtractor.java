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
import edu.jhu.util.Triple;

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
    
    // Package private for testing.
    int[] getParents(CoNLL09Sentence sent) {
        if (prm.useGoldSyntax) {
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
        ZhaoObject zhaoPred = new ZhaoObject(pidx, parents, sent, prm, "v");
        ZhaoObject zhaoArg = new ZhaoObject(aidx, parents, sent, prm, "n");
        ZhaoObject zhaoPredArgPair = new ZhaoObject(pidx, aidx, zhaoPred, zhaoArg, parents);
        ZhaoObject zhaoPredLast;
        ZhaoObject zhaoPredNext;
        ZhaoObject zhaoArgLast;
        ZhaoObject zhaoArgNext;
        
        if (pidx > 0) {
            zhaoPredLast = new ZhaoObject(pidx - 1, parents, sent, prm, "v");
        } else {
            zhaoPredLast = new ZhaoObject("BEGIN");
        }
        
        if (pidx < sent.size()) {
            zhaoPredNext = new ZhaoObject(pidx + 1, parents, sent, prm, "v");
        } else {
            zhaoPredNext = new ZhaoObject("END");
        }
        
        if (aidx > 0) {
            zhaoArgLast = new ZhaoObject(aidx - 1, parents, sent, prm, "n");
        } else {
            zhaoArgLast = new ZhaoObject("BEGIN");
        }
        
        if (aidx < sent.size()) {
            zhaoArgNext = new ZhaoObject(aidx + 1, parents, sent, prm, "n");
        } else {
            zhaoArgNext = new ZhaoObject("END");
        }
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

                
        // Initialize Syntactic Connection structures.
        /* ZHANG:  Syntactic Connection. This includes syntactic head (h), left(right) farthest(nearest) child (lm,ln,rm,rn), 
         *  and high(low) support verb or noun.
         *  From the predicate or the argument to the syntactic root along with the syntactic tree, 
         *  the first verb(noun) that is met is called as the low support verb(noun), 
         *  and the nearest one to the root is called as the high support verb(noun).*/
        ArrayList<Integer> predChildren = zhaoPred.getChildren();
        // TBD:  predNoFarChildren
        ArrayList<Integer> argChildren = zhaoArg.getChildren();
        // TBD:  argNoFarChildren
        List<Pair<Integer, Dir>> betweenPath = zhaoPredArgPair.getBetweenPath();
        /* Semantic Connection. This includes semantic head (semhead), left(right) farthest(nearest) semantic child (semlm, semln, semrm, semrn). We say a predicate is its argument���s semantic head, and the latter is the former���s child. Features related to this type may track the current semantic parsing status.
        
        // Initialize Path structures.
        /* ZHANG:  Path. There are two basic types of path between the predicate and the argument candidates. 
         * One is the linear path (linePath) in the sequence, the other is the path in the syntactic 
         * parsing tree (dpPath). For the latter, we further divide it into four sub-types by 
         * considering the syntactic root, dpPath is the full path in the syntactic tree. */
        List<Pair<Integer, Dir>> dpPathPred = zhaoPredArgPair.getDpPathPred();
        List<Pair<Integer, Dir>> dpPathArg = zhaoPredArgPair.getDpPathArg();
        ArrayList<Integer> linePath = zhaoPredArgPair.getLinePath();
        
        ArrayList<CoNLL09Token> argChildrenTokens = getTokens(argChildren);                
        ArrayList<CoNLL09Token> betweenPathTokens = getTokens(betweenPath);
        ArrayList<CoNLL09Token> linePathCoNLL = getTokens(linePath);
        
        
        /* TBD: Family. Two types of children sets for the predicate or argument candidate are considered, 
         * the first includes all syntactic children (children), the second also includes all but 
         * excludes the left most and the right most children (noFarChildren). */
        
        // Add the supervised features
        if (prm.withSupervision) {
            addZhaoSupervisedPredFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, feats);
            addZhaoSupervisedArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, argChildrenTokens, feats);
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
            pathTokens.add(sent.get(p.get1()));
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

    /*private List<Pair<Integer, Integer>> getZhaoSyntacticConnectionStructures(int predHead, int argHead, List<Pair<Integer, Dir>> predRootPath, List<Pair<Integer, Dir>> argRootPath, ArrayList<Integer> argChildren, ArrayList<Integer> predChildren) {
        List<Pair<Integer, Integer>> returnList = new ArrayList<Pair<Integer,Integer>>();
        //Pair<Pair<Integer,Integer>,Pair<Integer,Integer>> argFarNearChildren = 
        syntacticConnectionFarthestNearestChildren(argHead, argChildren, returnList);
        // Pair<Pair<Integer,Integer>,Pair<Integer,Integer>> predFarNearChildren = 
        syntacticConnectionFarthestNearestChildren(predHead, predChildren, returnList);
        // Pair<Integer,Integer> argSupport = 
        syntacticConnectionHighLowSupport("n", argRootPath, returnList);
        //Pair<Integer,Integer> predSupport = 
        syntacticConnectionHighLowSupport("v", predRootPath, returnList); 
        //returnList.add(argFarNearChildren.get1());
        //returnList.add(argFarNearChildren.get2());
        //returnList.add(predFarNearChildren.get1());
        //returnList.add(predFarNearChildren.get2());
        //returnList.add(argSupport);
        //returnList.add(predSupport);
        return returnList;
        
    }*/

    /*private Pair<Pair<List<Pair<Integer, Dir>>, List<Pair<Integer, Dir>>>, ArrayList<Integer>> 
    getPathStructures(int pidx, int aidx, CoNLL09Token pred, CoNLL09Token arg, List<Pair<Integer, Dir>> predRootPath, List<Pair<Integer, Dir>> argRootPath) {
        // Initialize dpPath structures.
        /* ZHANG:  Assume that dpPathShare starts from a node r', 
         * then dpPathPred is from the predicate to r', and dpPathArg is from the argument to r'. */
/*        Pair<List<Pair<Integer,Dir>>,List<Pair<Integer,Dir>>> dpSharePaths = getdpSharePaths(pred, arg, argRootPath, predRootPath);

        // Initialize linePath structure.
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
        return new Pair<Pair<List<Pair<Integer, Dir>>, List<Pair<Integer, Dir>>>, ArrayList<Integer>>(dpSharePaths, linePath);
        
    }*/

  /*  private Pair<List<Pair<Integer,Dir>>,List<Pair<Integer,Dir>>> getdpSharePaths(CoNLL09Token pred, CoNLL09Token arg, List<Pair<Integer, Dir>> argRootPath,
            List<Pair<Integer, Dir>> predRootPath) {
        /* Leading two paths to the root from the predicate and the argument, respectively, 
         * the common part of these two paths will be dpPathShare. */
     /*   List<Pair<Integer,DepTree.Dir>> dpPathShare = new ArrayList<Pair<Integer,DepTree.Dir>>();
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
        List<Pair<Integer,Dir>> dpPathPred = DepTree.getDependencyPath(pred.getId(), r, parents);
        List<Pair<Integer,Dir>> dpPathArg = DepTree.getDependencyPath(arg.getId(), r, parents);
        Pair<List<Pair<Integer,Dir>>,List<Pair<Integer,Dir>>> returnPair = new Pair<List<Pair<Integer, Dir>>, List<Pair<Integer, Dir>>>(dpPathPred, dpPathArg);
        return returnPair;
        
    }*/

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
        feat = linePathForm.toString();
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
        feat = zhaoArg.getForm() + zhaoArgNext.getForm();
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
        feat = zhaoArg.getForm() + argChildrenPos.toString();
        feats.add(feat);
        // a1.pos + a.pos.seq
        feat = zhaoArgNext.getPos() + zhaoArg.getPos();
        feats.add(feat);
    }

    private void addZhaoUnsupervisedPredFeats(ZhaoObject zhaoPred, ZhaoObject zhaoPredLast, ZhaoObject zhaoPredNext, BinaryStrFVBuilder feats) {
        // ------- Predicate features (unsupervised) ------- 
        // p.pos_1 + p.pos
        String feat12 = zhaoPredLast.getPos() + zhaoPred.getPos();
        feats.add(feat12);
        // p.pos1
        String feat13 = zhaoPredNext.getPos();
        feats.add(feat13);
    }

    
    private void addZhaoSupervisedCombinedFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, BinaryStrFVBuilder feats, ArrayList<CoNLL09Token> betweenPathTokens,
            ArrayList<CoNLL09Token> linePathCoNLL, List<Pair<Integer, Dir>> dpPathArg) {
    /*private void addZhaoSupervisedCombinedFeats(String predLemma, String argLemma, List<String> argFeats,
            List<String> predFeats, BinaryStrFVBuilder feats, ArrayList<CoNLL09Token> betweenPathTokens, ArrayList<CoNLL09Token> linePathCoNLL, List<Pair<Integer, Dir>> dpPathArg) {*/
        // ------- Combined features (supervised) ------- 
        String feat;
        // a.lemma + p.lemma 
        feat = zhaoArg.getLemma() + zhaoPred.getLemma();
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
        feat = depRelPath + zhaoPred.getFeat().get(0);
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

    private void addZhaoSupervisedArgFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast,
            ZhaoObject zhaoPredNext, ZhaoObject zhaoArgLast, ZhaoObject zhaoArgNext, ArrayList<CoNLL09Token> argChildrenTokens, BinaryStrFVBuilder feats) {
    
    /*private void addZhaoSupervisedArgFeats(int argHead, String argForm, String argLemma, String argPos, List<String> argFeats,
            ArrayList<CoNLL09Token> argChildrenTokens, CoNLL09Token argLm, CoNLL09Token argRm, CoNLL09Token argRn,
            int lastAidx, int nextAidx, BinaryStrFVBuilder feats) {*/
        String feat;
        // ------- Argument features (supervised) ------- 
        String a1 = zhaoArg.getFeat().get(0);
        String a2 = zhaoArg.getFeat().get(1);
        String a3 = zhaoArg.getFeat().get(2);
        String a4 = zhaoArg.getFeat().get(3);
        String a5 = zhaoArg.getFeat().get(4);
        String a6 = zhaoArg.getFeat().get(5);
        String last_a1 = zhaoArgLast.getFeat().get(0);
        String last_a2 = zhaoArgLast.getFeat().get(1);
        String next_a1 = zhaoArgNext.getFeat().get(0);
        String next_a2 = zhaoArgNext.getFeat().get(1);
        String next_a3 = zhaoArgNext.getFeat().get(2);
        CoNLL09Token argLm = sent.get(zhaoArg.getFarLeftChild());
        CoNLL09Token argRm = sent.get(zhaoArg.getFarRightChild());
        CoNLL09Token argRn = sent.get(zhaoArg.getNearRightChild());
        
        // a.FEAT1 + a.FEAT3 + a.FEAT4 + a.FEAT5 + a.FEAT6 
        feat = a1 + a3 + a4 + a5 + a6;
        feats.add(feat);
        // a_1.FEAT2 + a.FEAT2 
        feat = last_a2 + a2;
        feats.add(feat);
        // a.FEAT3 + a1.FEAT3
        feat = a3 + next_a3;
        feats.add(feat);
        // a.FEAT3 + a.h.FEAT3 
        feat = sent.get(zhaoArg.getParent()).getFeat().get(2);
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
        feat = sent.get(zhaoArg.getParent()).getLemma();
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
        feat = argRm + zhaoArg.getForm();
        feats.add(feat);
        // TBD: a.rm_1.form 
        // a.rm.lemma
        feat = argRm.getLemma();
        feats.add(feat);
        // a.rn.dprel + a.form 
        feat = argRn.getDeprel() + zhaoArg.getForm();
        feats.add(feat);
        // a.lowSupportVerb.lemma 
        feat = sent.get(zhaoArg.getLowSupport()).getLemma();
        feats.add(feat);
        // a.lemma + a.h.form 
        feat = zhaoArg.getLemma() + sent.get(zhaoArg.getParent()).getForm();
        feats.add(feat);
        // a.lemma + a.pphead.form 
        // a1.lemma
        feat = zhaoArgNext.getLemma();
        feats.add(feat);
        // a.pos + a.children.dprel.bag
        ArrayList<String> argChildrenDeprel = new ArrayList<String>(); 
        for (CoNLL09Token child : argChildrenTokens) {
            argChildrenDeprel.add(child.getDeprel());
        }
        List<String> argChildrenDeprelBag = bag(argChildrenDeprel);
        feat = zhaoArg.getPos() + argChildrenDeprelBag.toString();
        feats.add(feat);
        
    }

    private void addZhaoSupervisedPredFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast, ZhaoObject zhaoPredNext, BinaryStrFVBuilder feats) {
        // ------- Predicate features (supervised) ------- 
        // p.currentSense + p.lemma 
        feats.add(zhaoPred.getPred() + zhaoPred.getLemma());
        // p.currentSense + p.pos 
        feats.add(zhaoPred.getPred() + zhaoPred.getPos());
        // p.currentSense + a.pos 
        feats.add(zhaoPred.getPred() + zhaoArg.getPos());
        // p_1 .FEAT1
        if (zhaoPredLast.getFeat() == null) {
            feats.add(zhaoPredLast.getForm());
        } else {
            feats.add(zhaoPredLast.getFeat().get(0));
        }
        // p.FEAT2
        System.out.println(zhaoPred.getFeat().get(0));
        feats.add(zhaoPred.getFeat().get(1));
        // p1.FEAT3
        if (zhaoPredNext.getFeat() == null) {
            feats.add(zhaoPredNext.getForm());
        } else {
            feats.add(zhaoPredNext.getFeat().get(2));
        }
        // TBD:  p.semrm.semdprel  What is this?            
        // p.lm.dprel        
        feats.add(sent.get(zhaoPred.getFarLeftChild()).getDeprel());
        // p.form + p.children.dprel.bag 
        ArrayList<String> depPredChildren = new ArrayList<String>();
        for (Integer child : zhaoPred.getChildren()) {
            depPredChildren.add(sent.get(child).getDeprel());
        }
        String bagDepPredChildren = bag(depPredChildren).toString();
        feats.add(zhaoPred.getForm() + bagDepPredChildren);
        // p.lemma_n (n = -1, 0) 
        feats.add(zhaoPredLast.getLemma());
        feats.add(zhaoPred.getLemma());
        // p.lemma + p.lemma1
        feats.add(zhaoPred.getLemma() + zhaoPredNext.getLemma());
        // p.pos + p.children.dprel.bag 
        feats.add(zhaoPred.getPos() + bagDepPredChildren);
        
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
