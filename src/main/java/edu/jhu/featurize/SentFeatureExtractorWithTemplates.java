package edu.jhu.featurize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.jhu.data.DepTree.Dir;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.srl.CorpusStatistics;

@Deprecated
public class SentFeatureExtractorWithTemplates extends SentFeatureExtractor {


    //private static final Logger log = Logger.getLogger(SentFeatureExtractor.class);
    public static class SentFeatureExtractorWithTemplatesPrm extends SentFeatureExtractorPrm {
        /** Switches for feature templates. */
        public boolean useTemplates = false;
        public boolean formFeats = true;
        public boolean lemmaFeats = true;
        public boolean tagFeats = true;
        public boolean morphFeats = true;
        public boolean deprelFeats = true;
        public boolean childrenFeats = true;
        public boolean pathFeats = true;
        public boolean syntacticConnectionFeats = true;
        /** Whether to add all possible features using the templates defined above. **/
        public boolean useAllTemplates = false;
    }
    
    private SentFeatureExtractorWithTemplatesPrm prm;

    public SentFeatureExtractorWithTemplates(SentFeatureExtractorWithTemplatesPrm prm, AnnoSentence sent, CorpusStatistics cs) {
        super(prm, sent, cs);
        this.prm = prm;
        if (prm.useAllTemplates) {
            this.prm.formFeats = true;
            this.prm.lemmaFeats = true;
            this.prm.tagFeats = true;
            this.prm.morphFeats = true;
            this.prm.deprelFeats = true;
            this.prm.childrenFeats = true;
            this.prm.pathFeats = true;
            this.prm.syntacticConnectionFeats = true;
        }
    }

    public void addTemplateSoloFeatures(int idx, ArrayList<String> feats) {
        // TODO Auto-generated method stub
        
    }

    /* Basically the idea here is, gather every possible feature piece (POS, lemma, etc.) to place in a pair;
     * then make those pairs. We use a window of length 3 to look at the previous word, current word,
     * and next word for features, on both the first word (pred candidate) and second word (arg candidate). */
    public void addTemplatePairFeatures(int pidx, int aidx, ArrayList<String> feats) { 
        /* Features considered:
         * formFeats
         * lemmaFeats
         * tagFeats
         * morphFeats
         * deprelFeats
         * childrenFeats
         * pathFeats
         * syntacticConnectionFeats */
        // all features that are created, 
        // from combinations of features of each type.
        HashMap<Integer,ArrayList<String>> allFeats = new HashMap<Integer,ArrayList<String>>(); 
        // predicate object
        FeaturizedToken predObject;
        // argument object
        FeaturizedToken argObject;
        // pred-argument dependency path object
        FeaturizedTokenPair predArgPathObject;
        
        // feature pieces from the predicate object
        ArrayList<String> predPieces = new ArrayList<String>();
        // feature pieces from the argument object
        ArrayList<String> argPieces = new ArrayList<String>();
        // feature pieces from pred-arg path object.
        HashMap<String, HashMap<String, ArrayList<String>>> predArgPathPieces = new HashMap<String, HashMap<String, ArrayList<String>>>();
        // feature pieces from syntactic connection objects.
        ArrayList<String> predSynConnectPieces = new ArrayList<String>();
        ArrayList<String> argSynConnectPieces = new ArrayList<String>();
        // feature pieces from children objects.
        HashMap<String, HashMap<String, ArrayList<String>>> childrenPieces = new HashMap<String, HashMap<String, ArrayList<String>>>();
        // the newly constructed features to add into feats.
        // these appear in isolation, get combined together, 
        // with one another, etc.
        ArrayList<String> predFeats;
        ArrayList<String> argFeats;
        ArrayList<String> predSynConnectFeats;
        ArrayList<String> argSynConnectFeats;
        ArrayList<String> pathFeats;
        ArrayList<String> childrenFeats;
        // holder for features combining all of the above
        ArrayList<String> newFeats;

        int x = 0;
        // For pred and arg in isolation features,
        // use a window of size 3:  previous word, current word, next word.
        for (int n=-1; n<=1; n++) {
            // Get the pred and arg feature objects.
            predObject = getFeatureObject(pidx + n);
            argObject = getFeatureObject(aidx + n);
            // store the feature pieces for preds and args.
            predPieces = makePieces(predObject, n, "p");
            argPieces = makePieces(argObject, n, "a");
            // assemble the single features
            // for preds and args
            predFeats = makeFeatures(predPieces);
            argFeats = makeFeatures(argPieces);
            // add them to our list of things to combine.
            allFeats.put(x, predFeats);
            x++;
            allFeats.put(x, argFeats);
            x++;
            if (prm.syntacticConnectionFeats) {
                /*
                 * Syntactic Connection. This includes syntactic head (h), 
                 * left(right) farthest(nearest) child (lm,ln,rm,rn), 
                 * and high(low) support verb or noun. 
                 * Support verb(noun):  From the predicate or the argument 
                 * to the syntactic root along with the syntactic tree, 
                 * the first verb(noun) that is met is called as the low 
                 * support verb(noun), and the nearest one to the root is 
                 * called as the high support verb(noun).
                 * MM:  Also including sibling.
                 */
                predSynConnectPieces = makeSynConnectPieces(predObject, n);
                argSynConnectPieces = makeSynConnectPieces(argObject, n);
                // assemble the single features
                // for preds and args
                predSynConnectFeats = makeFeatures(predSynConnectPieces);
                argSynConnectFeats = makeFeatures(argSynConnectPieces);
                // add them to our list of things to combine.
                allFeats.put(x, predSynConnectFeats);
                x++;
                allFeats.put(x, argSynConnectFeats);
                x++;
            }
            // The following features are only using the current context,
            // not a sliding window around pred/arg.
            // We could do this, but Zhao et al don't seem to,
            // and it would result in a bazillion more features....
            if (n == 0) {
                if (prm.pathFeats) {
                    /* Path.
                     * From Zhao et al:
                     * There are two basic types of path between the predicate and the 
                     * argument candidates. One is the linear path (linePath) in the sequence, 
                     * the other is the path in the syntactic parsing tree (dpPath). 
                     * For the latter, we further divide it into four sub-types by considering 
                     * the syntactic root, dpPath is the full path in the syntactic tree. 
                     * Leading two paths to the root from the predicate and the argument, 
                     * respectively, the common part of these two paths will be dpPathShare. 
                     * Assume that dpPathShare starts from a node r′, then dpPathPred is 
                     * from the predicate to r′, and dpPathArg is from the argument to r′.
                     */
                    predArgPathObject = getFeatureObject(pidx, aidx);
                    // get the feature pieces for the pred-arg path features.
                    predArgPathPieces = makePathPieces(predArgPathObject);
                    // make features out of these pieces.
                    pathFeats = makeFeaturesConcat(predArgPathPieces);
                    // add them to our list of things to combine.
                    allFeats.put(x, pathFeats);
                    x++;
                }
                if (prm.childrenFeats) {
                    /*
                     * Family. Two types of children sets for the predicate or argument 
                     * candidate are considered, the first includes all syntactic children 
                     * (children), the second also includes all but excludes the left most 
                     * and the right most children (noFarChildren).
                     */
                    // get the feature pieces for the pred arg "children" features.
                    childrenPieces = makeFamilyPieces(predObject, argObject);
                    // make features out of these pieces.
                    childrenFeats = makeFeaturesConcat(childrenPieces);
                    // add them to our list of things to combine.
                    allFeats.put(x, childrenFeats);
                    x++;
                }
            }
        }
        
        // now, combine everything.
        ArrayList<String> featureList1;
        ArrayList<String> featureList2;
        Iterator<Entry<Integer, ArrayList<String>>> it1 = allFeats.entrySet().iterator();
        while (it1.hasNext()) {
            Map.Entry pairs1 = it1.next();
            int key1 = (Integer) pairs1.getKey();
            featureList1 = (ArrayList<String>) pairs1.getValue();
            newFeats = makeFeatures(featureList1);
            feats.addAll(newFeats);
            for (Entry<Integer, ArrayList<String>> entry2 : allFeats.entrySet()) {
                int key2 = entry2.getKey();
                if (key1 != key2) { 
                    featureList2 = entry2.getValue();
                    newFeats = makeFeatures(featureList1, featureList2);
                    feats.addAll(newFeats);
                }
            }
        }
    }
    
    private ArrayList<String> makeSynConnectPieces(FeaturizedToken featureObject, int n) {
        /*
         * Syntactic Connection. This includes syntactic head (h), 
         * left(right) farthest(nearest) child (lm,ln,rm,rn), 
         * and high(low) support verb or noun. 
         * Support verb(noun):  From the predicate or the argument 
         * to the syntactic root along with the syntactic tree, 
         * the first verb(noun) that is met is called as the low 
         * support verb(noun), and the nearest one to the root is 
         * called as the high support verb(noun).
         * MM:  I will also include right and left 'sibling' here.
         */
        // tmp holder for each set of constructed feature pieces
        ArrayList<String> newFeaturePieces = new ArrayList<String>();
        // holds tmp feature objects for each object (child, support word).
        int newIdx;
        FeaturizedToken newObject;
        // holds all feature pieces
        ArrayList<String> featurePieces = new ArrayList<String>();
        
        // pred high support
        newIdx = featureObject.getHighSupportVerb();
        newObject = getFeatureObject(newIdx);
        newFeaturePieces = makePieces(newObject, n, "highSupportPred");
        featurePieces.addAll(newFeaturePieces);
        // pred low support
        newIdx = featureObject.getLowSupportVerb();
        newObject = getFeatureObject(newIdx);
        newFeaturePieces = makePieces(newObject, n, "lowSupportPred");
        featurePieces.addAll(newFeaturePieces);
        // arg high support pieces
        newIdx = featureObject.getHighSupportNoun();
        newObject = getFeatureObject(newIdx);
        newFeaturePieces = makePieces(newObject, n, "highSupportArg");
        featurePieces.addAll(newFeaturePieces);
        // arg low support pieces
        newIdx = featureObject.getLowSupportNoun();
        newObject = getFeatureObject(newIdx);
        newFeaturePieces = makePieces(newObject, n, "lowSupportArg");
        featurePieces.addAll(newFeaturePieces);
        // parent pieces
        newIdx = featureObject.getParent();
        newObject = getFeatureObject(newIdx);
        newFeaturePieces = makePieces(newObject, n, "parent");
        featurePieces.addAll(newFeaturePieces);
        // far left child pieces
        newIdx = featureObject.getFarLeftChild();
        newObject = getFeatureObject(newIdx);
        newFeaturePieces = makePieces(newObject, n, "flchild");
        featurePieces.addAll(newFeaturePieces);
        // far right child pieces
        newIdx = featureObject.getFarRightChild();
        newObject = getFeatureObject(newIdx);
        newFeaturePieces = makePieces(newObject, n, "frchild");
        featurePieces.addAll(newFeaturePieces);
        // near left child pieces
        newIdx = featureObject.getNearLeftChild();
        newObject = getFeatureObject(newIdx);
        newFeaturePieces = makePieces(newObject, n, "nlchild");
        featurePieces.addAll(newFeaturePieces);
        // near right child pieces
        newIdx = featureObject.getNearRightChild();
        newObject = getFeatureObject(newIdx);
        newFeaturePieces = makePieces(newObject, n, "nrchild");
        featurePieces.addAll(newFeaturePieces);
        // left sibling pieces
        newIdx = featureObject.getNearLeftSibling();
        newObject = getFeatureObject(newIdx);
        newFeaturePieces = makePieces(newObject, n, "lsibling");
        featurePieces.addAll(newFeaturePieces);
        // right sibling pieces
        newIdx = featureObject.getNearRightSibling();
        newObject = getFeatureObject(newIdx);
        newFeaturePieces = makePieces(newObject, n, "rsibling");
        featurePieces.addAll(newFeaturePieces);

        return featurePieces;
    }

    private HashMap<String, HashMap<String, ArrayList<String>>> makeFamilyPieces(FeaturizedToken predObject, FeaturizedToken argObject) {
        // holds tmp feature objects for each group of objects (children, siblings).
        ArrayList<FeaturizedToken> newObjectList = new ArrayList<FeaturizedToken>();
        // holds tmp feature pieces for each group
        HashMap<String, HashMap<String, ArrayList<String>>> featurePieces = new HashMap<String, HashMap<String, ArrayList<String>>>();
        HashMap<String, ArrayList<String>> newFeaturePieces = new HashMap<String, ArrayList<String>>();
        
        // pred children features
        newObjectList = getFeatureObjectList(predObject.getChildren());
        newFeaturePieces = makeListPieces(newObjectList);
        featurePieces.put("p.children", newFeaturePieces);
        // pred noFarChildren features
        newObjectList = getFeatureObjectList(predObject.getNoFarChildren());
        newFeaturePieces = makeListPieces(newObjectList);
        featurePieces.put("p.noFarChildren", newFeaturePieces);
        // arg children features
        newObjectList = getFeatureObjectList(argObject.getChildren());
        newFeaturePieces = makeListPieces(newObjectList);
        featurePieces.put("a.children", newFeaturePieces);
        // arg noFarChildren features
        newObjectList = getFeatureObjectList(argObject.getNoFarChildren());
        newFeaturePieces = makeListPieces(newObjectList);
        featurePieces.put("a.noFarChildren", newFeaturePieces);

        return featurePieces;
    }
    
    /* Gathers pieces to be used in feature templates.
     * @param featureObj a pred or argument object.
     * @return featureList a list of feature pieces to be used in the templates.
     */
    private ArrayList<String> makePieces(FeaturizedToken featureObj, Integer n, String pieceType) {        
        ArrayList<String> featureList = new ArrayList<String>();
        String feat;
        String morphFeat = "";
        String prefix = "";
        if (n == -1) {
            prefix = "Prev.";
        } else if (n == 1) {
            prefix = "Next.";
        }
        prefix += pieceType + ".";
        if (prm.formFeats) {
            feat = prefix + "Form:" + featureObj.getForm();
            featureList.add(feat);
        }
        if (prm.lemmaFeats) {
            feat = prefix + "Lemma:" + featureObj.getLemma();
            featureList.add(feat);
        }
        if (prm.tagFeats) {
            feat = prefix + "Tag:" + featureObj.getPos();
            featureList.add(feat);
        }
        if (prm.morphFeats) {
            // I'm not sure of the best way to iterate over these
            // to make different combinations.
            // Here's one way.
            List<String> morphFeats = featureObj.getFeat();
            for (int i = 0; i < morphFeats.size(); i++) {
                feat = prefix + "Feat:" + morphFeats.get(i);
                featureList.add(feat);
                if (i > 0) {
                    morphFeat = morphFeat + "_" + feat;
                    featureList.add(morphFeat);
                } else {
                    morphFeat = feat;
                }
            }
        }
        if (prm.deprelFeats) {
            feat = prefix + "Deprel:" + featureObj.getDeprel();
            featureList.add(feat);
        }
        return featureList;
    }


    /* Gathers pieces to be used in feature templates.
     * @param featureObj a pred-argument path object.
     * @return featureList a list of feature pieces to be used in the templates.
     */
    private HashMap<String, HashMap<String, ArrayList<String>>> makePathPieces(FeaturizedTokenPair featureObj) {
        // holds tmp feature objects for each path type (line, dp, etc).
        ArrayList<FeaturizedToken> newObjectList = new ArrayList<FeaturizedToken>();
        // holds tmp feature pieces for each path type (line, dp, etc).
        HashMap<String, ArrayList<String>> newFeaturePieces = new HashMap<String, ArrayList<String>>();
        // holds all feature pieces
        HashMap<String, HashMap<String, ArrayList<String>>> featurePieces = new HashMap<String, HashMap<String, ArrayList<String>>>();
        
        // straight path between words
        ArrayList<Integer> linePath = featureObj.getLinePath();
        // dependency path between words
        List<Pair<Integer, Dir>> dependencyPath = featureObj.getDependencyPath();
        List<Pair<Integer, Dir>> dpPathPred = featureObj.getDpPathPred();
        List<Pair<Integer, Dir>> dpPathArg = featureObj.getDpPathArg();
        List<Pair<Integer, Dir>> dpPathShare = featureObj.getDpPathShare();
        /* For each path, create lists of FeatureObjects for all of the words in the path,
         * then make features out of them and add them to the featurePieces list.
         */
        // linePath featurepiece lists
        newObjectList = getFeatureObjectList(linePath);
        newFeaturePieces = makeListPieces(newObjectList);
        featurePieces.put("linePath", newFeaturePieces);
        // dependencyPath featurepiece lists
        newObjectList = getFeatureObjectList(dependencyPath);
        newFeaturePieces = makeListPieces(newObjectList);
        featurePieces.put("dpPath", newFeaturePieces);
        // NOTE:  does dpPathArg usually == dependencyPath?
        // dpPathPred featurepiece lists
        newObjectList = getFeatureObjectList(dpPathPred);
        newFeaturePieces = makeListPieces(newObjectList);
        featurePieces.put("dpPathPred", newFeaturePieces);
        // dpPathArg featurepiece lists
        newObjectList = getFeatureObjectList(dpPathArg);
        newFeaturePieces = makeListPieces(newObjectList);
        featurePieces.put("dpPathArg", newFeaturePieces);
        // dpPathShare featurepiece lists
        newObjectList = getFeatureObjectList(dpPathShare);
        newFeaturePieces = makeListPieces(newObjectList);
        featurePieces.put("dpPathShare", newFeaturePieces);

        return featurePieces;
    }
    
    /* Given a list of feature objects (e.g., some set of items along
     * a syntactic path), get all the possible feature pieces (POS tags, etc).
     * @return feature pieces for all objects as a list.
     */
    private HashMap<String, ArrayList<String>> makeListPieces(ArrayList<FeaturizedToken> pathList) {
        // Sets of feature pieces along the path.
        HashMap<String, ArrayList<String>> featurePieceList;
        String feat;
        String morphFeat = "";
        ArrayList<String> p;
        // For adding directionality, following Bjorkelund.
        Dir d;
        FeaturizedToken featureObj;
        // Feature pieces along all paths.
        featurePieceList = new HashMap<String, ArrayList<String>>();
        // distance feature
        String prefix = "Dist";
        feat = Integer.toString(pathList.size());
        if (!featurePieceList.containsKey(prefix)) {
            featurePieceList.put(prefix, new ArrayList<String>());
        }
        p = featurePieceList.get(prefix);
        p.add(feat);
        // not *totally* necessary.
        if (pathList.size() == 0) {
            return featurePieceList;
        }
        // NOTE:  There is probably a much faster way to do this.
        for (int x = 0; x < pathList.size(); x++) {
            featureObj = pathList.get(x);
            d = featureObj.getDirection();
            if (prm.formFeats) {
                buildPiece("Form", featureObj.getForm(), d, featurePieceList);
            }
            if (prm.lemmaFeats) {
                buildPiece("Lemma", featureObj.getLemma(), d, featurePieceList);
            }
            if (prm.tagFeats) {
                buildPiece("Pos", featureObj.getPos(), d, featurePieceList);
            }
            if (prm.morphFeats) {
                List<String> morphFeats = featureObj.getFeat();
                morphFeat = "";
                for (int i = 0; i < morphFeats.size(); i++) {
                    feat = morphFeats.get(i);
                    buildPiece("Feat", feat, d, featurePieceList);
                    if (i > 0) {
                        morphFeat += "_" + feat;
                        buildPiece("Concat.Feat", morphFeat, d, featurePieceList);
                    } else {
                        morphFeat = feat;
                    }
                }
            }
            if (prm.deprelFeats) {
                buildPiece("Deprel", featureObj.getDeprel(), d, featurePieceList);
            }
        }
        return featurePieceList;
    }    

    private void buildPiece(String prefix, String tag, Dir d, HashMap<String, ArrayList<String>> featurePieceList) {
        if (!featurePieceList.containsKey(prefix)) {
            featurePieceList.put(prefix, new ArrayList<String>());
        }
        ArrayList<String> p = featurePieceList.get(prefix);
        String feat = tag;
        p.add(feat);
        prefix += ".Dir";
        if (!featurePieceList.containsKey(prefix)) {
            featurePieceList.put(prefix, new ArrayList<String>());
        }
        p = featurePieceList.get(prefix);
        // Doesn't matter; this is just for readability.
        if (d != null) {
            feat = tag + d;
        } else {
            feat = tag;
        }
        p.add(feat);        
    }

    private ArrayList<String> makeFeaturesConcat(HashMap<String, HashMap<String, ArrayList<String>>> allPieces) {
        // Given a collected group of items for a pred or arg candidate,
        // mush them together into a feature.
        String feat;
        String prefix;
        ArrayList<String> p = new ArrayList<String>();
        ArrayList<String> featureList = new ArrayList<String>();
        for (String a : allPieces.keySet()) {
            for (Entry<String, ArrayList<String>> b : allPieces.get(a).entrySet()) {
                prefix = a + "." + b.getKey() + ".";
                p = b.getValue();
                if (p.size() > 1) {
                    feat = "Seq:" + prefix + buildString(p);
                    featureList.add(feat);
                    feat = "Bag:" + prefix + buildString(bag(p));
                    featureList.add(feat);
                    feat = "NoDup:" + prefix + buildString(noDup(p));
                    featureList.add(feat);
                } else {
                    feat = prefix + buildString(p);
                    featureList.add(feat);
                }
            }
        }
        return featureList;
    }

    private ArrayList<String> makeFeatures(ArrayList<String> featureList) {
        return featureList;
    }

    private ArrayList<String> makeFeatures(ArrayList<String> featureList1, ArrayList<String> featureList2) {
        ArrayList<String> featureList = new ArrayList<String>();
        for (String a : featureList1) {
            for (String b : featureList2) {
                String ab = a + "_" + b;
                featureList.add(ab);
            }
        }
        return featureList;
    }
    
}
