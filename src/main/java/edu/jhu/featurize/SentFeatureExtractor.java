package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.berkeley.nlp.PCFGLA.smoothing.SrlBerkeleySignatureBuilder;
import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.util.Pair;

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
    }
    
    // Parameters for feature extraction.
    private SentFeatureExtractorPrm prm;
    
    private final CoNLL09Sentence sent;
    private final CorpusStatistics cs;
    private final SrlBerkeleySignatureBuilder sig;
    private final int[] parents;
    private ArrayList<ZhaoObject> zhaoSentence;
    private ZhaoObject zhaoHeadDefault;
    private ZhaoObject zhaoTailDefault;
        
    public SentFeatureExtractor(SentFeatureExtractorPrm prm, CoNLL09Sentence sent, CorpusStatistics cs) {
        this.prm = prm;
        this.sent = sent;
        this.cs = cs;
        this.sig = cs.sig;
        if (!prm.biasOnly) {
            // Syntactic parents of all the words in this sentence, in order (idx 0 is -1)
            this.parents = getParents(sent);
            if (prm.useZhaoFeats || prm.useDepPathFeats) {
                this.zhaoSentence = createZhaoSentence();
                this.zhaoHeadDefault = new ZhaoObject("BEGIN");
                this.zhaoTailDefault = new ZhaoObject("END");
            }
        } else {
            this.parents = null;
        }
    }
    
    private ArrayList<ZhaoObject> createZhaoSentence() {
        ArrayList<ZhaoObject> _zhaoSentence = new ArrayList<ZhaoObject>();
        for (Integer i = 0; i < sent.size(); i++) {
            _zhaoSentence.add(new ZhaoObject(i, parents, sent, cs));
        }
        return _zhaoSentence;
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
        return feats;
    }
    
    public ArrayList<String> createSenseFeatureSet(int idx) {
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
        // feats = getNuguesFeatures();
        return feats;
    }
    
    
    public void addSimpleSoloFeatures(int idx, ArrayList<String> feats) {
        String wordForm = sent.get(idx).getForm();
        //System.out.println("word is " + wordForm);
        Set <String> a = sig.getSimpleUnkFeatures(wordForm, idx, cs.prm.language);
        for (String c : a) {
            feats.add(c);
        }
    }
    
    
    public void addSimplePairFeatures(int pidx, int aidx, ArrayList<String> feats) {
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
    
    public boolean capitalized(String wordForm) {
        if (wordForm.length() == 0) {
            return true;
        }
        char ch = wordForm.charAt(0);
        if (Character.isUpperCase(ch)) {
            return true;
        }
        return false;
    }
    
    public void addNaradowskySoloFeatures(int idx, ArrayList<String> feats) {
        CoNLL09Token word = sent.get(idx);
        String wordForm = decideForm(word.getForm(), idx);
        String wordPos;
        if (cs.prm.useGoldSyntax) {
            wordPos = word.getPos();
        } else {
            wordPos = word.getPpos();
        }

        feats.add("head_" + wordForm + "_word");
        feats.add("head_" + wordPos + "_tag");
        feats.add("head_" + wordForm + "_" + wordPos + "_wordtag");
        feats.add("slen_" + sent.size());
        if (prm.withSupervision) {
            String wordLemma = word.getLemma();
            feats.add("head_" + wordLemma + "_lemma");
        }
        String cap;
        if (capitalized(wordForm)) {
            cap = "UC";
        } else {
            cap = "LC";
        }
        feats.add("head_" + cap + "_caps");
    }
    
    
    public void addNaradowskyPairFeatures(int pidx, int aidx, ArrayList<String> feats) {
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

    public void addDependencyPathFeatures(int pidx, int aidx, ArrayList<String> feats) {
        ZhaoObject zhaoPred = getZhaoObject(pidx);
        ZhaoObject zhaoArg = getZhaoObject(aidx);
        ZhaoObject zhaoPredArgPair = new ZhaoObject(pidx, aidx, zhaoPred, zhaoArg, parents);
        List<Pair<Integer, Dir>> betweenPath = zhaoPredArgPair.getBetweenPath();
        String feat;
        ArrayList<String> depRelPathWord = new ArrayList<String>();
        ArrayList<ZhaoObject> betweenPathTokens = getTokens(betweenPath);
        for (ZhaoObject t : betweenPathTokens) {
            depRelPathWord.add(t.getForm());
        }
        feat = buildString(depRelPathWord);
        feats.add(feat);
        feat = buildString(bag(depRelPathWord));
        feats.add(feat);
    }
    
    public void addZhaoSoloFeatures(int idx, ArrayList<String> feats) {
        // Zhao doesn't have 'solo' features for SRL, just sense.
        /* This is the feature set for ENGLISH sense classification given
         * by Zhao et al 2009.  Spanish sense classification is not given.
         */
        ZhaoObject p = getZhaoObject(idx);
        ZhaoObject p1 = getZhaoObject(idx + 1);
        ZhaoObject pm1 = getZhaoObject(idx - 1);
  
        addZhaoUnsupervisedSoloFeats(p, p1, pm1, feats);
        if (prm.withSupervision) {
            addZhaoSupervisedSoloFeats(p, p1, feats);
        }
    }   

    public void addZhaoUnsupervisedSoloFeats(ZhaoObject p, ZhaoObject p1, ZhaoObject pm1, ArrayList<String> feats) {
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
    
    public void addZhaoSupervisedSoloFeats(ZhaoObject p, ZhaoObject p1, ArrayList<String> feats) {
        String feat;
        // p.lm.pos
        feat = getZhaoObject(p.getFarLeftChild()).getPos();
        feats.add(feat);
        // p.rm.pos
        feat = getZhaoObject(p.getFarRightChild()).getPos();
        feats.add(feat);
        // p.lemma
        feat = p.getLemma();
        feats.add(feat);
        // p.lemma + p.lemma1
        feat = p.getLemma() + p1.getLemma();
        feats.add(feat);
        // p.lemma + p.children.dprel.noDup 
        ArrayList<String> depRelChildren = new ArrayList<String>();
        ArrayList<ZhaoObject> pChildren = getTokens(p.getChildren());
        for (ZhaoObject child : pChildren) {
            depRelChildren.add(child.getDeprel());
        }
        feat = buildString(noDup(depRelChildren));
        feats.add(feat);
        // Er...what?  Sense given for sense prediction?
        // p.lemma + p.currentSense
    }
    
    public void addZhaoPairFeatures(int pidx, int aidx, ArrayList<String> feats) {
        /* NOTE:  Not sure about they're "Semantic Connection" features.  What do they correspond to in CoNLL data? 
         * From paper: "This includes semantic head (semhead), left(right) farthest(nearest) 
         * semantic child (semlm, semln, semrm, semrn). 
         * We say a predicate is its argument's semantic head, and the latter is the former's child. 
         * Features related to this type may track the current semantic parsing status." */

        ZhaoObject zhaoPred = getZhaoObject(pidx);
        ZhaoObject zhaoArg = getZhaoObject(aidx);
        ZhaoObject zhaoPredArgPair = new ZhaoObject(pidx, aidx, zhaoPred, zhaoArg, parents);
        ZhaoObject zhaoPredLast = getZhaoObject(pidx - 1);
        ZhaoObject zhaoPredNext = getZhaoObject(pidx + 1);
        //ZhaoObject zhaoPredParent = getZhaoObject(zhaoPred.getParent());
        ZhaoObject zhaoArgLast = getZhaoObject(aidx - 1);
        ZhaoObject zhaoArgNext = getZhaoObject(aidx + 1);
        ZhaoObject zhaoArgParent = getZhaoObject(zhaoArg.getParent());
                
        ArrayList<Integer> predChildren = zhaoPred.getChildren();
        ArrayList<Integer> argChildren = zhaoArg.getChildren();
        List<Pair<Integer, Dir>> betweenPath = zhaoPredArgPair.getBetweenPath();
        
        // Initialize Path structures.
        //List<Pair<Integer, Dir>> dpPathPred = zhaoPredArgPair.getDpPathPred();
        List<Pair<Integer, Dir>> dpPathArg = zhaoPredArgPair.getDpPathArg();
        ArrayList<Integer> linePath = zhaoPredArgPair.getLinePath();

        ArrayList<ZhaoObject> predChildrenTokens = getTokens(predChildren);
        ArrayList<ZhaoObject> argChildrenTokens = getTokens(argChildren);
        ArrayList<ZhaoObject> betweenPathTokens = getTokens(betweenPath);
        ArrayList<ZhaoObject> linePathCoNLL = getTokens(linePath);
        
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

    private ZhaoObject getZhaoObject(int idx) {
        if (idx < 0) {
            return zhaoHeadDefault;
        } else if (idx >= zhaoSentence.size()) {
            return zhaoTailDefault;
        }
        return zhaoSentence.get(idx);
    }

    private ArrayList<ZhaoObject> getTokens(List<Pair<Integer, Dir>> path) {
        ArrayList<ZhaoObject> pathTokens = new ArrayList<ZhaoObject>();
        for (Pair<Integer,Dir> p : path) {
            pathTokens.add(getZhaoObject(p.get1()));
        }
        return pathTokens;
    }

    private ArrayList<ZhaoObject> getTokens(ArrayList<Integer> children) {
        ArrayList<ZhaoObject> childrenTokens = new ArrayList<ZhaoObject>();
        for (int child : children) {
            childrenTokens.add(getZhaoObject(child));
        }
        return childrenTokens;
    }

    private void addZhaoUnsupervisedCombinedFeats(ArrayList<Integer> linePath, ArrayList<ZhaoObject> linePathCoNLL, ArrayList<String> feats) {
        // ------- Combined features (unsupervised) ------- 
        String feat;
        // a:p|linePath.distance 
        feat = Integer.toString(linePath.size());
        feats.add(feat);
        // a:p|linePath.form.seq 
        ArrayList<String> linePathForm = new ArrayList<String>();
        for (ZhaoObject t : linePathCoNLL) {
            linePathForm.add(t.getForm());
        }
        feat = buildString(linePathForm);
        feats.add(feat);
    }

    private void addZhaoUnsupervisedArgFeats(ZhaoObject zhaoArg, ZhaoObject zhaoArgNext, ZhaoObject zhaoArgLast,
            ArrayList<ZhaoObject> argChildrenTokens, ArrayList<String> feats) {
        // ------- Argument features (unsupervised) ------- 
        String feat;
        // a.lm.form
        feat = getZhaoObject(zhaoArg.getFarLeftChild()).getForm();
        feats.add(feat);
        // a_1.form
        feat = zhaoArgLast.getForm();
        feats.add(feat);
        // a.form + a1.form
        feat = zhaoArg.getForm() + "_" + zhaoArgNext.getForm();
        feats.add(feat);
        // a.form + a.children.pos 
        ArrayList<String> argChildrenPos = new ArrayList<String>();
        for (ZhaoObject child : argChildrenTokens) {
            if (cs.prm.useGoldSyntax) {
                argChildrenPos.add(child.getPos());
            } else {
                argChildrenPos.add(child.getPpos());
            }
        }
        feat = zhaoArg.getForm()  + "_" + buildString(argChildrenPos);
        feats.add(feat);
        // a1.pos + a.pos.seq
        if (cs.prm.useGoldSyntax) {
            feat = zhaoArgNext.getPos() + "_" + zhaoArg.getPos();
        } else {
            feat = zhaoArgNext.getPpos() + "_" + zhaoArg.getPpos();
          
        }
        feats.add(feat);
    }

    private void addZhaoUnsupervisedPredFeats(ZhaoObject zhaoPred, ZhaoObject zhaoPredLast, ZhaoObject zhaoPredNext, ArrayList<String> feats) {
        // ------- Predicate features (unsupervised) ------- 
        String feat12;
        // p.pos_1 + p.pos
        if (cs.prm.useGoldSyntax) {
            feat12 = zhaoPredLast.getPos() + "_" + zhaoPred.getPos();
        } else {
            feat12 = zhaoPredLast.getPpos() + "_" + zhaoPred.getPpos();
        }
        feats.add(feat12);
        String feat13;
        // p.pos1
        if (cs.prm.useGoldSyntax) {
            feat13 = zhaoPredNext.getPos();
        } else {
            feat13 = zhaoPredNext.getPpos();            
        }
        feats.add(feat13);
    }

    
    private void addZhaoSupervisedCombinedFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ArrayList<String> feats, ArrayList<ZhaoObject> betweenPathTokens,
            ArrayList<ZhaoObject> linePathCoNLL, List<Pair<Integer, Dir>> dpPathArg) {
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
        for (ZhaoObject t : betweenPathTokens) {
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
        for (ZhaoObject t : linePathCoNLL) {
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
            dpPathLemma.add(getZhaoObject(dpP.get1()).getLemma());            
        }
        // a:p|dpPathArgu.lemma.seq 
        feat = buildString(dpPathLemma);
        feats.add(feat);
        // a:p|dpPathArgu.lemma.bag
        feat = buildString(bag(dpPathLemma));
        feats.add(feat);
    }

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

    private void addZhaoSupervisedArgFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast,
            ZhaoObject zhaoPredNext, ZhaoObject zhaoArgLast, ZhaoObject zhaoArgNext, ZhaoObject zhaoArgParent, ArrayList<ZhaoObject> argChildrenTokens, ArrayList<String> feats) {
        // ------- Argument features (supervised) ------- 
        getFirstThirdSupArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, zhaoArgParent, argChildrenTokens, feats);
        getSecondThirdSupArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, zhaoArgParent, argChildrenTokens, feats);
        getThirdThirdSupArgFeats(zhaoPred, zhaoArg, zhaoPredLast, zhaoPredNext, zhaoArgLast, zhaoArgNext, zhaoArgParent, argChildrenTokens, feats);
    }


    private void getFirstThirdSupArgFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast,
            ZhaoObject zhaoPredNext, ZhaoObject zhaoArgLast, ZhaoObject zhaoArgNext, ZhaoObject zhaoArgParent, 
            ArrayList<ZhaoObject> argChildrenTokens, ArrayList<String> feats) {
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
    
    private ArrayList<String> getChildrenFeat3(ArrayList<ZhaoObject> childrenTokens) {
        ArrayList<String> childrenFeat3 = new ArrayList<String>();
        for (ZhaoObject child : childrenTokens) {
            childrenFeat3.add(child.getFeat().get(2));
        }
        return childrenFeat3;
    }

    private ArrayList<String> getChildrenFeat1(ArrayList<ZhaoObject> childrenTokens) {
        ArrayList<String> childrenFeat1 = new ArrayList<String>();
        for (ZhaoObject child : childrenTokens) {
            childrenFeat1.add(child.getFeat().get(0));
        }
        return childrenFeat1;
    }

    private void getSecondThirdSupArgFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast,
            ZhaoObject zhaoPredNext, ZhaoObject zhaoArgLast, ZhaoObject zhaoArgNext, ZhaoObject zhaoArgParent, 
            ArrayList<ZhaoObject> argChildrenTokens, ArrayList<String> feats) {
        ZhaoObject argLm = getZhaoObject(zhaoArg.getFarLeftChild());
        ZhaoObject argRm = getZhaoObject(zhaoArg.getFarRightChild());
        ZhaoObject argRn = getZhaoObject(zhaoArg.getNearRightChild());
        //ZhaoObject argLn = getZhaoObject(zhaoArg.getNearLeftChild());
        String feat;
        // a.h.lemma
        feats.add(zhaoArgParent.getLemma());
        // a.lm.dprel + a.form
        feats.add(argLm.getDeprel() + "_" + zhaoArg.getForm());
        // a.lm_1.lemma
        feats.add(getZhaoObject(zhaoArgLast.getFarLeftChild()).getLemma());
        // a.lmn.pos (n=0,1) 
        feats.add(argLm.getPos());
        feats.add(getZhaoObject(zhaoArgNext.getFarLeftChild()).getPos());
        // a.noFarChildren.pos.bag + a.rm.form 
        ArrayList<Integer> noFarChildren = zhaoArg.getNoFarChildren();
        ArrayList<String> noFarChildrenPos = new ArrayList<String>();
        for (Integer i : noFarChildren) {
            noFarChildrenPos.add(getZhaoObject(i).getPos()); 
        }
        ArrayList<String> argNoFarChildrenBag = bag(noFarChildrenPos);
        feat = buildString(argNoFarChildrenBag) + argRm.getForm();
        feats.add(feat);
        // a.pphead.lemma
        feats.add(getZhaoObject(zhaoArg.getPhead()).getLemma());
        // a.rm.dprel + a.form
        feats.add(argRm.getDeprel() + "_" + zhaoArg.getForm());
        // a.rm_1.form 
        feats.add(getZhaoObject(zhaoArgLast.getFarRightChild()).getForm());
        // a.rm.lemma
        feats.add(argRm.getLemma());
        // a.rn.dprel + a.form 
        feats.add(argRn.getDeprel() + "_" + zhaoArg.getForm());        
    }
    
    private void getThirdThirdSupArgFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast,
            ZhaoObject zhaoPredNext, ZhaoObject zhaoArgLast, ZhaoObject zhaoArgNext, ZhaoObject zhaoArgParent, 
            ArrayList<ZhaoObject> argChildrenTokens, ArrayList<String> feats) {
        String feat;
        // a.lowSupportVerb.lemma 
        feats.add(getZhaoObject(zhaoArg.getArgLowSupport()).getLemma());
        // a.lemma + a.h.form 
        feats.add(zhaoArg.getLemma() + "_" + zhaoArgParent.getForm());
        // a.lemma + a.pphead.form 
        feats.add(zhaoArg.getLemma() + "_" + getZhaoObject(zhaoArg.getPhead()).getForm());
        // a1.lemma
        feats.add(zhaoArgNext.getLemma());
        // a.pos + a.children.dprel.bag
        ArrayList<String> argChildrenDeprel = new ArrayList<String>(); 
        for (ZhaoObject child : argChildrenTokens) {
            argChildrenDeprel.add(child.getDeprel());
        }
        ArrayList<String> argChildrenDeprelBag = bag(argChildrenDeprel);
        feat = zhaoArg.getPos() + buildString(argChildrenDeprelBag);
        feats.add(feat);
    }



    private void addZhaoSupervisedPredFeats(ZhaoObject zhaoPred, ZhaoObject zhaoArg, ZhaoObject zhaoPredLast, ZhaoObject zhaoPredNext, ArrayList<ZhaoObject> predChildrenTokens, ArrayList<String> feats) {
        // ------- Predicate features (supervised) ------- 
        // p.currentSense + p.lemma 
        feats.add(zhaoPred.getPred() + "_" + zhaoPred.getLemma());
        // p.currentSense + p.pos 
        feats.add(zhaoPred.getPred() + "_" + zhaoPred.getPos());
        // p.currentSense + a.pos 
        feats.add(zhaoPred.getPred() + "_" + zhaoArg.getPos());
        // p_1.FEAT1
        feats.add(zhaoPredLast.getFeat().get(0));
        // p.FEAT2
        feats.add(zhaoPred.getFeat().get(1));
        // p1.FEAT3
        feats.add(zhaoPredNext.getFeat().get(2));
        // NOTE:  This is supposed to be p.semrm.semdprel  What is this?  
        // I'm not sure.  Here's just a guess.
        feats.add(getZhaoObject(zhaoPred.getFarRightChild()).getDeprel());
        // p.lm.dprel        
        feats.add(getZhaoObject(zhaoPred.getFarLeftChild()).getDeprel());
        // p.form + p.children.dprel.bag 
        ArrayList<String> predChildrenDeprel = new ArrayList<String>();
        for (ZhaoObject child : predChildrenTokens) {
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

    public ArrayList<String> bag(ArrayList<String> elements) {
        // bag, which removes all duplicated strings and sort the rest
        return (ArrayList<String>) asSortedList(new HashSet<String>(elements));
    }
    
    public ArrayList<String> noDup(ArrayList<String> argChildrenFeat1) {
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
