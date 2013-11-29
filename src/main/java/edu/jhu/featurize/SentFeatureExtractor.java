package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.berkeley.nlp.PCFGLA.smoothing.SrlBerkeleySignatureBuilder;
import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.srl.CorpusStatistics;

/**
 * Feature extraction from the observations on a particular sentence.
 * 
 * @author mmitchell
 * @author mgormley
 */
public class SentFeatureExtractor {

    //private static final Logger log = Logger.getLogger(SentFeatureExtractor.class);
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
        // NOTE: We default to false on these features since including these
        // would break a lot of unit tests, which expect the default to be 
        // only Zhao+Narad features.
        /** Whether to add the "Bjorkelund" features. */
        public boolean useBjorkelundFeats = false;
    }
    
    // Parameters for feature extraction.
    private SentFeatureExtractorPrm prm;
    
    private final SimpleAnnoSentence sent;
    private final CorpusStatistics cs;
    private final SrlBerkeleySignatureBuilder sig;
    private final int[] parents;
    private ArrayList<FeatureObject> featuredSentence;
    private FeatureObject featuredHeadDefault;
    private FeatureObject featuredTailDefault;
        
    public SentFeatureExtractor(SentFeatureExtractorPrm prm, SimpleAnnoSentence sent, CorpusStatistics cs) {
        this.prm = prm;
        this.sent = sent;
        this.cs = cs;
        this.sig = cs.sig;
        if (!prm.biasOnly) {
            // Syntactic parents of all the words in this sentence, in order (idx 0 is -1)
            this.parents = getParents(sent);
            if (prm.useZhaoFeats || prm.useDepPathFeats) {
                this.featuredSentence = createZhaoSentence();
                this.featuredHeadDefault = new FeatureObject(-1, parents, sent);
                this.featuredTailDefault = new FeatureObject(sent.size(), parents, sent);
            }
        } else {
            this.parents = null;
        }
    }
    
    private ArrayList<FeatureObject> createZhaoSentence() {
        ArrayList<FeatureObject> _featuredSentence = new ArrayList<FeatureObject>();
        for (Integer i = 0; i < sent.size(); i++) {
            _featuredSentence.add(new FeatureObject(i, parents, sent));
        }
        return _featuredSentence;
    }

    // Package private for testing.
    int[] getParents(SimpleAnnoSentence sent) {
        return sent.getParents();
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
    public ArrayList<String> createFeatureSet(int idx) {
        ArrayList<String> feats = new ArrayList<String>();
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
        if (prm.useBjorkelundFeats) {
            addBjorkelundSoloFeatures(idx, feats);
        }
        return feats;
    }
    
    public ArrayList<String> createSenseFeatureSet(int idx) {
        ArrayList<String> feats = createFeatureSet(idx);
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
    public ArrayList<String> createFeatureSet(int pidx, int aidx) {
        ArrayList<String> feats = new ArrayList<String>();
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
        if (prm.useBjorkelundFeats) {
            addBjorkelundPairFeatures(pidx, aidx, feats);
        }
        return feats;
    }

    
    // ---------- Meg's "Simple" features. ---------- 
    public void addSimpleSoloFeatures(int idx, ArrayList<String> feats) {
        String wordForm = sent.getWord(idx);
        Set <String> a = sig.getSimpleUnkFeatures(wordForm, idx, cs.prm.language);
        for (String c : a) {
            feats.add(c);
        }
    }
    
    public void addSimplePairFeatures(int pidx, int aidx, ArrayList<String> feats) {
        String predForm = sent.getWord(pidx);
        String argForm = sent.getWord(aidx);
        Set <String> a = sig.getSimpleUnkFeatures(predForm, pidx, cs.prm.language);
        for (String c : a) {
            feats.add(c);
        }
        Set <String> b = sig.getSimpleUnkFeatures(argForm, aidx, cs.prm.language);
        for (String c : b) {
            feats.add(c);
        }

    }

    
    // ---------- Meg's Dependency Path features. ---------- 
    public void addDependencyPathFeatures(int pidx, int aidx, ArrayList<String> feats) {
        FeatureObject featurePred = getFeatureObject(pidx);
        FeatureObject featureArg = getFeatureObject(aidx);
        FeatureObject featurePredArgPair = new FeatureObject(pidx, aidx, featurePred, featureArg, parents);
        List<Pair<Integer, Dir>> betweenPath = featurePredArgPair.getBetweenPath();
        String feat;
        ArrayList<String> depRelPathWord = new ArrayList<String>();
        ArrayList<FeatureObject> betweenPathTokens = getTokens(betweenPath);
        for (FeatureObject t : betweenPathTokens) {
            depRelPathWord.add(t.getForm());
        }
        feat = buildString(depRelPathWord);
        feats.add(feat);
        feat = buildString(bag(depRelPathWord));
        feats.add(feat);
    }    

    
    // ---------- Naradowsky et al.'s 2011 ACL features. ----------
    public void addNaradowskySenseFeatures(int idx, ArrayList<String> feats) {
        String word = sent.getWord(idx);
        String wordForm = decideForm(word, idx);
        String wordPos = sent.getPosTag(idx);
        String wordLemma = sent.getLemma(idx);

        feats.add("head_" + wordForm + "_word");
        feats.add("head_" + wordLemma + "_lemma");
        feats.add("head_" + wordPos + "_tag");
        feats.add("head_" + wordForm + "_" + wordPos + "_wordtag");
        String cap;
        if (capitalized(wordForm)) {
            cap = "UC";
        } else {
            cap = "LC";
        }
        feats.add("head_" + cap + "_caps");    }
    
    public void addNaradowskySoloFeatures(int idx, ArrayList<String> feats) {
        addNaradowskySenseFeatures(idx, feats);
    }

    /** Made up by Meg based on pair and sense features. 
    public void addNaradowskySoloFeatures(int idx, ArrayList<String> feats) {
        addNaradowskySenseFeatures(idx, feats);
        String word = sent.getWord(idx);
        String wordForm = decideForm(word, idx);
        String wordPos = sent.getPosTag(idx);

        feats.add("head_" + wordForm + "_dep_" + wordPos + "_wordpos");
        feats.add("slen_" + sent.size());    
        feats.add("head_" + wordForm + "_word");
        feats.add("head_" + wordPos + "_tag");
    } **/
    
    
    public void addNaradowskyPairFeatures(int pidx, int aidx, ArrayList<String> feats) {
        String predWord = sent.getWord(pidx);
        String argWord = sent.getWord(aidx);
        String predForm = decideForm(predWord, pidx);
        String argForm = decideForm(argWord, aidx);
        String predPos = sent.getPosTag(pidx);
        String argPos = sent.getPosTag(aidx);
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
            List<String> predFeats = sent.getFeats(pidx);
            List<String> argFeats = sent.getFeats(aidx);
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
    
    
    // ---------- Zhao et al's 2009 CoNLL features. ----------     
    public void addZhaoSenseFeatures(int idx, ArrayList<String> feats) {
        /* This is the feature set for ENGLISH sense classification given
         * by Zhao et al 2009.  Spanish sense classification is not given. */
        FeatureObject p = getFeatureObject(idx);
        FeatureObject p1 = getFeatureObject(idx + 1);
        FeatureObject pm1 = getFeatureObject(idx - 1);

        addZhaoUnsupervisedSenseFeats(p, p1, pm1, feats);
        if (prm.withSupervision) {
            addZhaoSupervisedSenseFeats(p, p1, feats);
        }
    }
    
    public void addZhaoUnsupervisedSenseFeats(FeatureObject p, FeatureObject p1, FeatureObject pm1, ArrayList<String> feats) {
        String feat;
        // p.form
        feat = p.getForm();
        feats.add(feat);
        // p.form−1 + p.form 
        feat = pm1.getForm() + p.getForm();
        feats.add(feat);
        // p.form + p.form1
        feat = p.getForm() + p1.getForm();
        feats.add(feat); 
    }
    
    public void addZhaoSupervisedSenseFeats(FeatureObject p, FeatureObject p1, ArrayList<String> feats) {
        String feat;
        // p.lm.pos
        feat = getFeatureObject(p.getFarLeftChild()).getPos();
        feats.add(feat);
        // p.rm.pos
        feat = getFeatureObject(p.getFarRightChild()).getPos();
        feats.add(feat);
        // p.lemma
        feat = p.getLemma();
        feats.add(feat);
        // p.lemma + p.lemma1
        feat = p.getLemma() + p1.getLemma();
        feats.add(feat);
        // p.lemma + p.children.dprel.noDup 
        ArrayList<String> depRelChildren = new ArrayList<String>();
        ArrayList<FeatureObject> pChildren = getTokens(p.getChildren());
        for (FeatureObject child : pChildren) {
            depRelChildren.add(child.getDeprel());
        }
        feat = buildString(noDup(depRelChildren));
        feats.add(feat);
        // Er...what?  Sense given for sense prediction?
        // p.lemma + p.currentSense
    }
    
    public void addZhaoSoloFeatures(int idx, ArrayList<String> feats) {
        // Zhao doesn't have 'solo' features for SRL, just sense.
        addZhaoSenseFeatures(idx, feats);
    }   
    
    public void addZhaoPairFeatures(int pidx, int aidx, ArrayList<String> feats) {
        /* NOTE:  Not sure about they're "Semantic Connection" features.  What do they correspond to in CoNLL data? 
         * From paper: "This includes semantic head (semhead), left(right) farthest(nearest) 
         * semantic child (semlm, semln, semrm, semrn). 
         * We say a predicate is its argument's semantic head, and the latter is the former's child. 
         * Features related to this type may track the current semantic parsing status." */

        FeatureObject zhaoPred = getFeatureObject(pidx);
        FeatureObject zhaoArg = getFeatureObject(aidx);
        FeatureObject zhaoPredArgPair = new FeatureObject(pidx, aidx, zhaoPred, zhaoArg, parents);
        FeatureObject zhaoPredLast = getFeatureObject(pidx - 1);
        FeatureObject zhaoPredNext = getFeatureObject(pidx + 1);
        //FeatureObject zhaoPredParent = getFeatureObject(zhaoPred.getParent());
        FeatureObject zhaoArgLast = getFeatureObject(aidx - 1);
        FeatureObject zhaoArgNext = getFeatureObject(aidx + 1);
        FeatureObject zhaoArgParent = getFeatureObject(zhaoArg.getParent());
                
        ArrayList<Integer> predChildren = zhaoPred.getChildren();
        ArrayList<Integer> argChildren = zhaoArg.getChildren();
        List<Pair<Integer, Dir>> betweenPath = zhaoPredArgPair.getBetweenPath();
        
        // Initialize Path structures.
        //List<Pair<Integer, Dir>> dpPathPred = zhaoPredArgPair.getDpPathPred();
        List<Pair<Integer, Dir>> dpPathArg = zhaoPredArgPair.getDpPathArg();
        ArrayList<Integer> linePath = zhaoPredArgPair.getLinePath();

        ArrayList<FeatureObject> predChildrenTokens = getTokens(predChildren);
        ArrayList<FeatureObject> argChildrenTokens = getTokens(argChildren);
        ArrayList<FeatureObject> betweenPathTokens = getTokens(betweenPath);
        ArrayList<FeatureObject> linePathCoNLL = getTokens(linePath);
        
        // Add the supervised features
        if (prm.withSupervision) {
            addZhaoSupervisedPredFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, predChildrenTokens, feats);
            addZhaoSupervisedArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, zhaoArgParent, argChildrenTokens, feats);
            addZhaoSupervisedCombinedFeats(zhaoPred, zhaoArg, feats, betweenPathTokens, linePathCoNLL, dpPathArg); 
        }
        
        // Add the unsupervised features
        addZhaoUnsupervisedPredFeats(zhaoPred, zhaoPredLast, zhaoPredNext, feats);
        addZhaoUnsupervisedArgFeats(zhaoArg, zhaoArgLast, zhaoArgNext, argChildrenTokens, feats);
        addZhaoUnsupervisedCombinedFeats(linePath, linePathCoNLL, feats);
    }
    
    private void addZhaoUnsupervisedCombinedFeats(ArrayList<Integer> linePath, ArrayList<FeatureObject> linePathCoNLL, ArrayList<String> feats) {
        // ------- Combined features (unsupervised) ------- 
        String feat;
        // a:p|linePath.distance 
        feat = Integer.toString(linePath.size());
        feats.add(feat);
        // a:p|linePath.form.seq 
        ArrayList<String> linePathForm = new ArrayList<String>();
        for (FeatureObject t : linePathCoNLL) {
            linePathForm.add(t.getForm());
        }
        feat = buildString(linePathForm);
        feats.add(feat);
    }

    private void addZhaoUnsupervisedArgFeats(FeatureObject zhaoArg, FeatureObject zhaoArgNext, FeatureObject zhaoArgLast,
            ArrayList<FeatureObject> argChildrenTokens, ArrayList<String> feats) {
        // ------- Argument features (unsupervised) ------- 
        String feat;
        // a.lm.form
        feat = getFeatureObject(zhaoArg.getFarLeftChild()).getForm();
        feats.add(feat);
        // a_1.form
        feat = zhaoArgLast.getForm();
        feats.add(feat);
        // a.form + a1.form
        feat = zhaoArg.getForm() + "_" + zhaoArgNext.getForm();
        feats.add(feat);
        // a.form + a.children.pos 
        ArrayList<String> argChildrenPos = new ArrayList<String>();
        for (FeatureObject child : argChildrenTokens) {
            argChildrenPos.add(child.getPos());
        }
        feat = zhaoArg.getForm()  + "_" + buildString(argChildrenPos);
        feats.add(feat);
        // a1.pos + a.pos.seq
        feat = zhaoArgNext.getPos() + "_" + zhaoArg.getPos();
        feats.add(feat);
    }

    private void addZhaoUnsupervisedPredFeats(FeatureObject zhaoPred, FeatureObject zhaoPredLast, FeatureObject zhaoPredNext, ArrayList<String> feats) {
        // ------- Predicate features (unsupervised) ------- 
        String feat12;
        // p.pos_1 + p.pos
        feat12 = zhaoPredLast.getPos() + "_" + zhaoPred.getPos();
        feats.add(feat12);
        String feat13;
        // p.pos1
        feat13 = zhaoPredNext.getPos();
        feats.add(feat13);
    }
    
    private void addZhaoSupervisedCombinedFeats(FeatureObject zhaoPred, FeatureObject zhaoArg, ArrayList<String> feats, ArrayList<FeatureObject> betweenPathTokens,
            ArrayList<FeatureObject> linePathCoNLL, List<Pair<Integer, Dir>> dpPathArg) {
        // ------- Combined features (supervised) -------
        String feat;
        // a.lemma + p.lemma 
        feat = zhaoArg.getLemma() + "_" + zhaoPred.getLemma();
        feats.add(feat);
        // (a:p|dpPath.dprel) + p.FEAT1 
        // a:p|dpPath.lemma.seq 
        // a:p|dpPath.lemma.bag 
        ArrayList<String> depRelPath = new ArrayList<String>();
        ArrayList<String> depRelPathLemma = new ArrayList<String>();
        for (FeatureObject t : betweenPathTokens) {
            depRelPath.add(t.getDeprel());
            depRelPathLemma.add(t.getLemma());
        }

        feat = buildString(depRelPath) + zhaoPred.getFeat().get(0);        
        feats.add(feat);
        feat = buildString(depRelPathLemma);
        feats.add(feat);
        ArrayList<String> depRelPathLemmaBag = bag(depRelPathLemma);
        feat = buildString(depRelPathLemmaBag);
        feats.add(feat);
        // a:p|linePath.FEAT1.bag 
        // a:p|linePath.lemma.seq 
        // a:p|linePath.dprel.seq 
        ArrayList<String> linePathFeat = new ArrayList<String>();
        ArrayList<String> linePathLemma = new ArrayList<String>();
        ArrayList<String> linePathDeprel = new ArrayList<String>();
        for (FeatureObject t : linePathCoNLL) {
            linePathFeat.add(t.getFeat().get(0));
            linePathLemma.add(t.getLemma());
            linePathDeprel.add(t.getDeprel());
        }
        ArrayList<String> linePathFeatBag = bag(linePathFeat);
        feat = buildString(linePathFeatBag);
        feats.add(feat);
        feat = buildString(linePathLemma);
        feats.add(feat);
        feat = buildString(linePathDeprel);
        feats.add(feat);
        ArrayList<String> dpPathLemma = new ArrayList<String>();
        for (Pair<Integer, Dir> dpP : dpPathArg) {
            dpPathLemma.add(getFeatureObject(dpP.get1()).getLemma());            
        }
        // a:p|dpPathArgu.lemma.seq 
        feat = buildString(dpPathLemma);
        feats.add(feat);
        // a:p|dpPathArgu.lemma.bag
        feat = buildString(bag(dpPathLemma));
        feats.add(feat);
    }
    
    private void addZhaoSupervisedArgFeats(FeatureObject zhaoPred, FeatureObject zhaoArg, FeatureObject zhaoPredLast,
            FeatureObject zhaoPredNext, FeatureObject zhaoArgLast, FeatureObject zhaoArgNext, FeatureObject zhaoArgParent, ArrayList<FeatureObject> argChildrenTokens, ArrayList<String> feats) {
        // ------- Argument features (supervised) ------- 
        getFirstThirdSupArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, zhaoArgParent, argChildrenTokens, feats);
        getSecondThirdSupArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, zhaoArgParent, argChildrenTokens, feats);
        getThirdThirdSupArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, zhaoArgParent, argChildrenTokens, feats);
    }

    private void getFirstThirdSupArgFeats(FeatureObject zhaoPred, FeatureObject zhaoArg, FeatureObject zhaoPredLast,
            FeatureObject zhaoPredNext, FeatureObject zhaoArgLast, FeatureObject zhaoArgNext, FeatureObject zhaoArgParent, 
            ArrayList<FeatureObject> argChildrenTokens, ArrayList<String> feats) {
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
        //String last_a1 = zhaoArgLastFeats.get(0);
        String last_a2 = zhaoArgLastFeats.get(1);
        List<String> zhaoArgNextFeats = zhaoArgNext.getFeat();
        //String next_a1 = zhaoArgNextFeats.get(0);
        //String next_a2 = zhaoArgNextFeats.get(1);
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
        feat = zhaoArgParent.getFeat().get(2);
        feats.add(feat);
        // a.children.FEAT1.noDup 
        ArrayList<String> argChildrenFeat1 = getChildrenFeat1(argChildrenTokens);
        ArrayList<String> argChildrenFeat1NoDup = noDup(argChildrenFeat1);
        feat = buildString(argChildrenFeat1NoDup);
        feats.add(feat);
        // a.children.FEAT3.bag 
        ArrayList<String> argChildrenFeat3 = getChildrenFeat3(argChildrenTokens);
        ArrayList<String> argChildrenFeat3Bag = bag(argChildrenFeat3);
        feat = buildString(argChildrenFeat3Bag);
        feats.add(feat);
    }
    
    private void getSecondThirdSupArgFeats(FeatureObject zhaoPred, FeatureObject zhaoArg, FeatureObject zhaoPredLast,
            FeatureObject zhaoPredNext, FeatureObject zhaoArgLast, FeatureObject zhaoArgNext, FeatureObject zhaoArgParent, 
            ArrayList<FeatureObject> argChildrenTokens, ArrayList<String> feats) {
        FeatureObject argLm = getFeatureObject(zhaoArg.getFarLeftChild());
        FeatureObject argRm = getFeatureObject(zhaoArg.getFarRightChild());
        FeatureObject argRn = getFeatureObject(zhaoArg.getNearRightChild());
        //FeatureObject argLn = getFeatureObject(zhaoArg.getNearLeftChild());
        String feat;
        // a.h.lemma
        feats.add(zhaoArgParent.getLemma());
        // a.lm.dprel + a.form
        feats.add(argLm.getDeprel() + "_" + zhaoArg.getForm());
        // a.lm_1.lemma
        feats.add(getFeatureObject(zhaoArgLast.getFarLeftChild()).getLemma());
        // a.lmn.pos (n=0,1) 
        feats.add(argLm.getPos());
        feats.add(getFeatureObject(zhaoArgNext.getFarLeftChild()).getPos());
        // a.noFarChildren.pos.bag + a.rm.form 
        ArrayList<Integer> noFarChildren = zhaoArg.getNoFarChildren();
        ArrayList<String> noFarChildrenPos = new ArrayList<String>();
        for (Integer i : noFarChildren) {
            noFarChildrenPos.add(getFeatureObject(i).getPos()); 
        }
        ArrayList<String> argNoFarChildrenBag = bag(noFarChildrenPos);
        feat = buildString(argNoFarChildrenBag) + argRm.getForm();
        feats.add(feat);
        // a.pphead.lemma
        feats.add(getFeatureObject(zhaoArg.getParent()).getLemma());
        // a.rm.dprel + a.form
        feats.add(argRm.getDeprel() + "_" + zhaoArg.getForm());
        // a.rm_1.form 
        feats.add(getFeatureObject(zhaoArgLast.getFarRightChild()).getForm());
        // a.rm.lemma
        feats.add(argRm.getLemma());
        // a.rn.dprel + a.form 
        feats.add(argRn.getDeprel() + "_" + zhaoArg.getForm());        
    }
    
    private void getThirdThirdSupArgFeats(FeatureObject zhaoPred, FeatureObject zhaoArg, FeatureObject zhaoPredLast,
            FeatureObject zhaoPredNext, FeatureObject zhaoArgLast, FeatureObject zhaoArgNext, FeatureObject zhaoArgParent, 
            ArrayList<FeatureObject> argChildrenTokens, ArrayList<String> feats) {
        String feat;
        // a.lowSupportVerb.lemma 
        feats.add(getFeatureObject(zhaoArg.getArgLowSupport()).getLemma());
        // a.lemma + a.h.form 
        feats.add(zhaoArg.getLemma() + "_" + zhaoArgParent.getForm());
        // a.lemma + a.pphead.form 
        feats.add(zhaoArg.getLemma() + "_" + getFeatureObject(zhaoArg.getParent()).getForm());
        // a1.lemma
        feats.add(zhaoArgNext.getLemma());
        // a.pos + a.children.dprel.bag
        ArrayList<String> argChildrenDeprel = new ArrayList<String>(); 
        for (FeatureObject child : argChildrenTokens) {
            argChildrenDeprel.add(child.getDeprel());
        }
        ArrayList<String> argChildrenDeprelBag = bag(argChildrenDeprel);
        feat = zhaoArg.getPos() + buildString(argChildrenDeprelBag);
        feats.add(feat);
    }

    private void addZhaoSupervisedPredFeats(FeatureObject zhaoPred, FeatureObject zhaoArg, FeatureObject zhaoPredLast, FeatureObject zhaoPredNext, ArrayList<FeatureObject> predChildrenTokens, ArrayList<String> feats) {
        // ------- Predicate features (supervised) ------- 
        
        // NOTE: We cannot include these features in our model since they would have
        // to use the gold predicate sense.
        // 
        //        // p.currentSense + p.lemma 
        //        feats.add(zhaoPred.getSense() + "_" + zhaoPred.getLemma());
        //        // p.currentSense + p.pos 
        //        feats.add(zhaoPred.getSense() + "_" + zhaoPred.getPos());
        //        // p.currentSense + a.pos 
        //        feats.add(zhaoPred.getSense() + "_" + zhaoArg.getPos());
        
        // p_1.FEAT1
        feats.add(zhaoPredLast.getFeat().get(0));
        // p.FEAT2
        feats.add(zhaoPred.getFeat().get(1));
        // p1.FEAT3
        feats.add(zhaoPredNext.getFeat().get(2));
        // NOTE:  This is supposed to be p.semrm.semdprel  What is this?  
        // I'm not sure.  Here's just a guess.
        feats.add(getFeatureObject(zhaoPred.getFarRightChild()).getDeprel());
        // p.lm.dprel        
        feats.add(getFeatureObject(zhaoPred.getFarLeftChild()).getDeprel());
        // p.form + p.children.dprel.bag 
        ArrayList<String> predChildrenDeprel = new ArrayList<String>();
        for (FeatureObject child : predChildrenTokens) {
            predChildrenDeprel.add(child.getDeprel());
        }
        String bagDepPredChildren = buildString(bag(predChildrenDeprel));
        feats.add(zhaoPred.getForm() + "_" + bagDepPredChildren);
        // p.lemma_n (n = -1, 0) 
        feats.add(zhaoPredLast.getLemma());
        feats.add(zhaoPred.getLemma());
        // p.lemma + p.lemma1
        feats.add(zhaoPred.getLemma() + "_" + zhaoPredNext.getLemma());
        // p.pos + p.children.dprel.bag 
        feats.add(zhaoPred.getPos() + "_" + bagDepPredChildren);
    }

    
    // ---------- Bjorkelund et al, CoNLL2009 features. ---------- 
    public void addBjorkelundSenseFeatures(int idx, ArrayList<String> feats) {
        FeatureObject pred = getFeatureObject(idx);
        FeatureObject predParent = getFeatureObject(pred.getParent());
        ArrayList<Integer> predChildren = pred.getChildren();
        // PredWord, PredPOS, PredDeprel, PredFeats
        addBjorkelundGenericFeatures(idx, feats, "Pred");
        // predParentWord, predParentPOS, predParentFeats
        addBjorkelundPredParentFeatures(predParent, feats);
        // ChildDepSet, ChildWordSet, ChildPOSSet
        addBjorkelundPredChildFeatures(predChildren, feats);
        // TODO: DepSubCat: the subcategorization frame of the predicate, e.g. OBJ+OPRD+SUB.
    }
    
    public void addBjorkelundSoloFeatures(int idx, ArrayList<String> feats) {
        /* No 'solo' features, per se, just Sense features for a solo predicate.
         * But, some features are the same type for predicate and argument. 
         * These are defined in add BjorkelundGenericFeatures. */
        addBjorkelundSenseFeatures(idx, feats);
    }
    
    public void addBjorkelundPairFeatures(int pidx, int aidx, ArrayList<String> feats) {
        /* From Bjorkelund et al, CoNLL 2009.
         * Prefixes: 
         * Pred:  the predicate
         * PredParent:  the parent of the predicate
         * Arg:  the argument
         * Left:  the leftmost dependent of the argument
         * Right:   the rightmost dependent of the argument
         * LeftSibling:  the left sibling of the argument
         * RightSibling:  the right sibling of the argument
         */        
        String feat;
        FeatureObject zhaoPred = getFeatureObject(pidx);
        FeatureObject zhaoArg = getFeatureObject(aidx);
        FeatureObject zhaoPredArgPair = new FeatureObject(pidx, aidx, zhaoPred, zhaoArg, parents);
        FeatureObject predParent = getFeatureObject(zhaoPred.getParent());
        ArrayList<Integer> predChildren = zhaoPred.getChildren();
        FeatureObject argLeftSibling = getFeatureObject(zhaoArg.getLeftSibling());
        FeatureObject argRightSibling = getFeatureObject(zhaoArg.getRightSibling());
        FeatureObject argLeftDependent = getFeatureObject(zhaoArg.getFarLeftChild());
        FeatureObject argRightDependent = getFeatureObject(zhaoArg.getFarRightChild());
        
        // PredWord, PredPOS, PredFeats, PredDeprel
        addBjorkelundGenericFeatures(pidx, feats, "Pred");
        // ArgWord, ArgPOS, ArgFeats, ArgDeprel
        addBjorkelundGenericFeatures(aidx, feats, "Arg");
        // PredParentWord, PredParentPOS, PredParentFeats
        addBjorkelundPredParentFeatures(predParent, feats);
        // ChildDepSet, ChildWordSet, ChildPOSSet
        addBjorkelundPredChildFeatures(predChildren, feats);
        // LeftWord, LeftPOS, LeftFeats
        addBjorkelundDependentFeats(argLeftDependent, feats, "Left");
        // RightWord, RightPOS, RightFeats
        addBjorkelundDependentFeats(argRightDependent, feats, "Right");
        // LeftSiblingWord, LeftSiblingPOS, LeftSiblingFeats
        addBjorkelundSiblingFeats(argLeftSibling, feats, "Left");
        // RightSiblingWord, RightSiblingPOS, RightSiblingFeats
        addBjorkelundSiblingFeats(argRightSibling, feats, "Right");
        // DeprelPath, POSPath
        addBjorkelundPathFeats(zhaoPredArgPair, feats);
        // Position
        addBjorkelundPositionFeat(pidx, aidx, feats);
        
        // PredLemma
        feat = zhaoPred.getLemma();
        feats.add("PredLemma:" + feat);
        // TODO: Sense: the value of the Pred column, e.g. plan.01.
        // TODO: DepSubCat: the subcategorization frame of the predicate, e.g. OBJ+OPRD+SUB.
    }
    
    public void addBjorkelundGenericFeatures(int idx, ArrayList<String> feats, String type) {
        String feat;
        FeatureObject bjorkWord = getFeatureObject(idx);
        // ArgWord, PredWord
        feat = bjorkWord.getForm();
        feats.add(type + ":" + feat);
        // ArgPOS, PredPOS,
        feat = bjorkWord.getPos();
        feats.add(type + ":" + feat);
        // ArgFeats, PredFeats
        feat = buildString(bjorkWord.getFeat());
        feats.add(type + ":" + feat);
        // ArgDeprel, PredDeprel
        feat = bjorkWord.getDeprel();
        feats.add(type + ":" + feat);
    }

    public void addBjorkelundPositionFeat(int pidx, int aidx, ArrayList<String> feats) {
        String feat;
        // Position: the position of the argument with respect to the predicate, i.e. before, on, or after.
        if (pidx < aidx) {
            feat = "before";
        } else if (pidx > aidx) {
            feat = "after";
        } else {
            feat = "on";
        }
        feats.add("Position:" + feat);        
    }

    public void addBjorkelundPathFeats(FeatureObject zhaoPredArgPair, ArrayList<String> feats) {
        String feat;
        List<Pair<Integer, Dir>> betweenPath = zhaoPredArgPair.getBetweenPath();
        // DeprelPath: the path from predicate to argument concatenating dependency labels with the
        // direction of the edge, e.g. OBJ↑OPRD↓SUB↓.
        ArrayList<String> depRelPath = new ArrayList<String>();
        // POSPath: same as DeprelPath, but dependency labels are exchanged for POS tags, e.g. NN↑NNS↓NNP↓.
        ArrayList<String> posPath = new ArrayList<String>();
        ArrayList<FeatureObject> betweenPathTokens = getTokens(betweenPath);
        for (int i = 0; i < betweenPathTokens.size(); i++) {
            FeatureObject t = betweenPathTokens.get(i);
            depRelPath.add(t.getDeprel() + ":" + betweenPath.get(i).get2());
            posPath.add(t.getPos() + ":" + betweenPath.get(i).get2());
        }
        feat = buildString(depRelPath);
        feats.add("DeprelPath:" + feat);
        feat = buildString(posPath);
        feats.add("PosPath:" + feat);
    }

    public void addBjorkelundSiblingFeats(FeatureObject argSibling, ArrayList<String> feats, String dir) {
        String feat;
        // LeftSiblingWord, RightSiblingWord
        feat = argSibling.getForm();
        feats.add(dir + "SiblingWord:" + feat);
        // LeftSiblingPOS, RightSiblingPOS
        feat = argSibling.getPos();
        feats.add(dir + "SiblingPos:" + feat);
        // LeftSiblingFeats, RightSiblingFeats
        feat = buildString(argSibling.getFeat());
        feats.add(dir + "SiblingFeats:" + feat);
    }

    public void addBjorkelundDependentFeats(FeatureObject dependent, ArrayList<String> feats, String dir) {
        String feat;
        // LeftWord, RightWord
        feat = dependent.getForm();
        feats.add(dir + "Word:" + feat);
        // LeftPOS, RightPOS
        feat = dependent.getPos();
        feats.add(dir + "POS:" + feat);
        // LeftFeats, RightFeats
        feat = buildString(dependent.getFeat());
        feats.add(dir + "Feats:" + feat);
    }

    public void addBjorkelundPredChildFeatures(ArrayList<Integer> predChildren, ArrayList<String> feats) {
        String feat;
        // ChildDepSet: the set of dependency labels of the children of the predicate, e.g. {OBJ, SUB}.
        ArrayList<String> childDepSet = new ArrayList<String>();
        // ChildPOSSet: the set of POS tags of the children of the predicate, e.g. {NN, NNS}.
        ArrayList<String> childPosSet = new ArrayList<String>();
        // ChildWordSet: the set of words (Form) of the children of the predicate, e.g. {fish, me}.
        ArrayList<String> childWordSet = new ArrayList<String>();
        for (int child : predChildren) {
            childDepSet.add(getFeatureObject(child).getDeprel());
            childPosSet.add(getFeatureObject(child).getPos());
            childWordSet.add(getFeatureObject(child).getForm());
        }
        feat = buildString(childDepSet);
        feats.add("ChildDepSet:" + feat);
        feat = buildString(childPosSet);
        feats.add("ChildPOSSet:" + feat);
        feat = buildString(childWordSet);
        feats.add("ChildWordSet:" + feat);        
    }

    public void addBjorkelundPredParentFeatures(FeatureObject predParent, ArrayList<String> feats) {
        String feat;
        // PredParentWord
        feat = predParent.getForm();
        feats.add("PredParentWord:" + feat);
        // PredParentPOS
        feat = predParent.getPos();
        feats.add("PredParentPos:" + feat);
        // PredParentFeats
        feat = buildString(predParent.getFeat());
        feats.add("PredParentFeats:" + feat);        
    }

    

    // ---------- Helper functions ---------- 

    private String buildString(ArrayList<String> input) {
        StringBuilder buffer = new StringBuilder(50);
        int j = input.size();
        if (j > 0) {
            buffer.append(input.get(0));
            for (int i = 1; i < j; i++) {
                buffer.append("_").append(input.get(i));
            }
        }
        return buffer.toString();
    }
    
    private ArrayList<String> getChildrenFeat3(ArrayList<FeatureObject> childrenTokens) {
        ArrayList<String> childrenFeat3 = new ArrayList<String>();
        for (FeatureObject child : childrenTokens) {
            childrenFeat3.add(child.getFeat().get(2));
        }
        return childrenFeat3;
    }

    private ArrayList<String> getChildrenFeat1(ArrayList<FeatureObject> childrenTokens) {
        ArrayList<String> childrenFeat1 = new ArrayList<String>();
        for (FeatureObject child : childrenTokens) {
            childrenFeat1.add(child.getFeat().get(0));
        }
        return childrenFeat1;
    }

    private ArrayList<String> bag(ArrayList<String> elements) {
        // bag, which removes all duplicated strings and sort the rest
        return (ArrayList<String>) asSortedList(new HashSet<String>(elements));
    }
    
    private ArrayList<String> noDup(ArrayList<String> argChildrenFeat1) {
        // noDup, which removes all duplicated neighbored strings.
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
    
    private FeatureObject getFeatureObject(int idx) {
        if (idx < 0) {
            return featuredHeadDefault;
        } else if (idx >= featuredSentence.size()) {
            return featuredTailDefault;
        }
        return featuredSentence.get(idx);
    }
    
    private boolean capitalized(String wordForm) {
        if (wordForm.length() == 0) {
            return true;
        }
        char ch = wordForm.charAt(0);
        if (Character.isUpperCase(ch)) {
            return true;
        }
        return false;
    }

    private ArrayList<FeatureObject> getTokens(List<Pair<Integer, Dir>> path) {
        ArrayList<FeatureObject> pathTokens = new ArrayList<FeatureObject>();
        for (Pair<Integer,Dir> p : path) {
            pathTokens.add(getFeatureObject(p.get1()));
        }
        return pathTokens;
    }

    private ArrayList<FeatureObject> getTokens(ArrayList<Integer> children) {
        ArrayList<FeatureObject> childrenTokens = new ArrayList<FeatureObject>();
        for (int child : children) {
            childrenTokens.add(getFeatureObject(child));
        }
        return childrenTokens;
    }
    
    private static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
      List<T> list = new ArrayList<T>(c);
      Collections.sort(list);
      return list;
    }
    
    private String decideForm(String wordForm, int idx) {
        String cleanWord = cs.normalize.clean(wordForm);

        if (!cs.knownWords.contains(cleanWord)) {
            String unkWord = cs.sig.getSignature(wordForm, idx, cs.prm.language);
            unkWord = cs.normalize.escape(unkWord);
            if (!cs.knownUnks.contains(unkWord)) {
                unkWord = "UNK";
                return unkWord;
            }
        }
        
        return cleanWord;
    }
    
}
