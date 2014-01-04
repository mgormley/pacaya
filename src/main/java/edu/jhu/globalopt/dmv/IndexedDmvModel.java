package edu.jhu.globalopt.dmv;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.WallDepTreeNode;
import edu.jhu.globalopt.cpt.IndexedCpt;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.model.dmv.DmvSentParamCache;
import edu.jhu.prim.tuple.IntTuple;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.train.dmv.DmvTrainCorpus;
import edu.jhu.util.Alphabet;

public class IndexedDmvModel implements IndexedCpt {
    
    private static final Logger log = Logger.getLogger(IndexedDmvModel.class);
    
    public static class Rhs extends IntTuple {
        public Rhs(int... args) {
            super(args);
        }
    }
    
    public static class ParamId extends Pair<Rhs,Integer> {

        public ParamId(Rhs x, Integer y) {
            super(x, y);
        }
        
    }
    
    public static class Param extends Pair<ParamId, Double> {

        public Param(ParamId x, Double y) {
            super(x, y);
        }
        
    }
    
    public static class CM extends Pair<Integer,Integer> {
        public CM(Integer x, Integer y) {
            super(x, y);
        }
    }
    
    public static final int ROOT = 0;
    public static final int CHILD = 1;
    public static final int DECISION = 2;
    
    private int numTags;
    private final int childValency = 1;
    private final int decisionValency = 2;
    private Alphabet<Rhs> rhsToC;
    private ArrayList<Alphabet<ParamId>> sentParamToI;
    private ArrayList<Alphabet<CM>> sentCmToI;
    private int[][] sentMaxFreqSi; 
    private int[][] unsupMaxTotFreqCm;
    private int numTotalParams;
    private int numNZUnsupMaxFreqCms;
    private Alphabet<Label> alphabet;
    private DmvTrainCorpus corpus;
    private int[][][] sentMaxFreqCms; 
    
    public IndexedDmvModel(DmvTrainCorpus corpus) {
        this(corpus, false);
    }
    
    public IndexedDmvModel(DmvTrainCorpus corpus, boolean skipMaxFreqAndSentParams) {
        this.corpus = corpus;
        this.alphabet = corpus.getLabelAlphabet();

        numTags = alphabet.size();
        log.trace("numTags: " + numTags);
        
        // Create map of DmvModel right hand sides to integers
        rhsToC = new Alphabet<Rhs>();
        rhsToC.startGrowth();
        // Create for DmvModel
        rhsToC.lookupIndex(new Rhs(ROOT));
        for(int p = 0; p < numTags; p++)
            for(int dir = 0; dir < 2; dir++) 
                for(int cv = 0; cv < childValency; cv++)
                    rhsToC.lookupIndex(new Rhs(CHILD, p, dir, cv));
        for(int p = 0; p < numTags; p++)
            for(int dir = 0; dir < 2; dir++)
                for(int dv = 0; dv < decisionValency; dv++)
                    rhsToC.lookupIndex(new Rhs(DECISION, p, dir, dv));
        rhsToC.stopGrowth();
        log.trace("rhsToC: size=" + rhsToC.size() + " alphabet=" + rhsToC);

        // Count total number of parameters
        numTotalParams = 0;
        for (int c = 0; c < getNumConds(); c++) {
            for (int m = 0; m < getNumParams(c); m++) {
                numTotalParams++;
            }
        }
        
        if (skipMaxFreqAndSentParams) {
            // TODO: This is just a switch to avoid the memory consumption of
            // sentMaxFreqCms which grows very large for large corpora with big 
            // models.
            return;
        }
        
        // Create the counts of the maximum number of times each parameter /could/ occur for each sentence.
        sentMaxFreqCms = new int[corpus.size()][][];
        for (int s=0; s<corpus.size(); s++) {
            sentMaxFreqCms[s] = getSentMaxFreqCm(corpus.getSentence(s)); 
        }
        
        // Create a map of individual parameters to sentence indices
        sentParamToI = new ArrayList<Alphabet<ParamId>>();
        for (int s=0; s<corpus.size(); s++) {
            Alphabet<ParamId> paramToI = new Alphabet<ParamId>();
            for (int c=0; c<getNumConds(); c++) {
                Rhs rhs = rhsToC.lookupObject(c);
                for (int m=0; m<getNumParams(c); m++) {
                    if (sentMaxFreqCms[s][c][m] > 0) {
                        paramToI.lookupIndex(new ParamId(rhs,m));
                    }
                }
            }
            paramToI.stopGrowth();
            sentParamToI.add(paramToI);
            log.trace("paramToI: s=" + s + " size=" + paramToI.size() + " alphabet=" + paramToI);
        }
        
        // Create a map from c,m indices to sentence variable indices using the previous two maps
        sentCmToI = new ArrayList<Alphabet<CM>>();
        for (int s=0; s<corpus.size(); s++) {
            Alphabet<ParamId> paramToI = sentParamToI.get(s);
            Alphabet<CM> cmToI = new Alphabet<CM>();
            for (int i=0; i<paramToI.size(); i++) {
                ParamId p = paramToI.lookupObject(i);
                Rhs rhs = p.get1();
                int m = p.get2();
                cmToI.lookupIndex(new CM(rhsToC.lookupIndex(rhs),m));
            }
            cmToI.stopGrowth();
            sentCmToI.add(cmToI);
            log.trace("cmToI: s=" + s + " size=" + cmToI.size() + " alphabet=" + cmToI);
        }
        
        // Create the count of max frequencies for each model parameter in term of sentence indices
        sentMaxFreqSi = new int[corpus.size()][];
        for (int s=0; s<corpus.size(); s++) {
            // Create the sentence solution 
            sentMaxFreqSi[s] = new int[getNumSentVars(s)];
            for (int c = 0; c < getNumConds(); c++) {
                for (int m = 0; m < getNumParams(c); m++) {
                    if (sentMaxFreqCms[s][c][m] > 0) {
                        int i = getSi(s, c, m);
                        sentMaxFreqSi[s][i] = sentMaxFreqCms[s][c][m];
                    }
                }
            }
        }
        
        // Create the count of total max frequencies for each model parameter in terms of c,m
        // and count the total number of non-zero max frequencies.
        numNZUnsupMaxFreqCms = 0;
        unsupMaxTotFreqCm = new int[getNumConds()][];
        for (int c = 0; c < getNumConds(); c++) {
            unsupMaxTotFreqCm[c] = new int[getNumParams(c)];
            for (int m = 0; m < getNumParams(c); m++) {
                for (int s = 0; s < sentMaxFreqSi.length; s++) {
                    if (!corpus.isLabeled(s)) {
                        unsupMaxTotFreqCm[c][m] += sentMaxFreqCms[s][c][m];
                    }
                }
              
                if (unsupMaxTotFreqCm[c][m] > 0) {
                    numNZUnsupMaxFreqCms++;
                }                    
            }
        }
        
        // TODO: remove
        //System.out.println("numNZUnsupMaxFreqCms: " + numNZUnsupMaxFreqCms);
        //System.out.println("unsupMaxTotFreqCm: " + Arrays.deepToString(unsupMaxTotFreqCm));
    }

    private int[][] getSentMaxFreqCm(Sentence sentence) {
        int[] tags = sentence.getLabelIds();
        
        // Create the sentence counts
        int[][] maxFreq = new int[getNumConds()][];
        for (int c=0; c<maxFreq.length; c++) {
            maxFreq[c] = new int[getNumParams(c)];
        }
        
        // Count the MAX number of times tree could contain each feature
        for (int cIdx=0; cIdx<tags.length; cIdx++) {
            int cTag = tags[cIdx];
            // The root parameter can appear once for each tag in the sentence
            maxFreq[rhsToC.lookupIndex(new Rhs(ROOT))][cTag] = 1;
            
            // Each edge has some child parameter and can appear once
            for (int pIdx=0; pIdx<tags.length; pIdx++) {
                if (cIdx == pIdx) {
                    continue;
                }
                int pTag = tags[pIdx];

                int lr = cIdx < pIdx ? DmvModel.LEFT : DmvModel.RIGHT;
                Rhs rhs = new Rhs(CHILD, pTag, lr, 0);
                int m = cTag;
                maxFreq[rhsToC.lookupIndex(rhs)][m]++;
            }
            
            // For each direction (LEFT, RIGHT)
            for (int lr=0; lr<2; lr++) {
                // Each decision can appear for each tag of that type in the sentence
                
                int numOnSide = (lr == 0) ? cIdx : tags.length - cIdx - 1;

                Rhs rhs;
                int m;
                // stopAdj
                rhs = new Rhs(DECISION, cTag, lr, 0);
                m = DmvModel.END;
                maxFreq[rhsToC.lookupIndex(rhs)][m]++;
                if (numOnSide > 0) {
                    // contAdj
                    rhs = new Rhs(DECISION, cTag, lr, 0);
                    m = DmvModel.CONT;
                    maxFreq[rhsToC.lookupIndex(rhs)][m]++;
                    // contNonAdj
                    rhs = new Rhs(DECISION, cTag, lr, 1);
                    m = DmvModel.CONT;
                    maxFreq[rhsToC.lookupIndex(rhs)][m] += numOnSide-1;
                    // stopNonAdj
                    rhs = new Rhs(DECISION, cTag, lr, 1);
                    m = DmvModel.END;
                    maxFreq[rhsToC.lookupIndex(rhs)][m]++;
                }
            }
        }
        return maxFreq;
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.gridsearch.dmv.IndexedCpt#getNumConds()
     */
    public int getNumConds() {
        return rhsToC.size();
    }

    /* (non-Javadoc)
     * @see edu.jhu.gridsearch.dmv.IndexedCpt#getNumTotalParams()
     */
    public int getNumTotalParams() {
        return numTotalParams;
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.gridsearch.dmv.IndexedCpt#getNumParams(int)
     */
    public int getNumParams(int c) { 
        Rhs rhs = rhsToC.lookupObject(c);
        if (rhs.get(0) == ROOT || rhs.get(0) == CHILD) {
            return numTags;
        } else if (rhs.get(0) == DECISION) {
            return 2;
        } else {
            throw new IllegalStateException("Unsupported type");
        }
    }

    /* (non-Javadoc)
     * @see edu.jhu.gridsearch.dmv.IndexedCpt#getName(int, int)
     */
    public String getName(int c, int m) {
        Rhs rhs = rhsToC.lookupObject(c);
        if (rhs.get(0) == ROOT) {
            String cTag = alphabet.lookupObject(m).getLabel();
            return String.format("root(%s)", cTag);
        } else if (rhs.get(0) == CHILD) {
            String pTag = alphabet.lookupObject(rhs.get(1)).getLabel();
            String lr = rhs.get(2) == DmvModel.LEFT ? "l" : "r";
            String cTag = alphabet.lookupObject(m).getLabel();
            return String.format("child_{%s,%s,%d}(%s)", pTag, lr, rhs.get(3), cTag);
        } else if (rhs.get(0) == DECISION) {
            String pTag = alphabet.lookupObject(rhs.get(1)).getLabel();
            String lr = (rhs.get(2) == DmvModel.LEFT) ? "l" : "r";
            String sc = (m == DmvModel.END) ? "s" : "c";
            return String.format("dec_{%s,%s,%d}(%s)",  pTag, lr, rhs.get(3), sc);
        } else {
            throw new IllegalStateException("Unsupported type");
        }
    }

    public int getCRoot() {
        return rhsToC.lookupIndex(new Rhs(ROOT));
    }
    
    public int getCChild(int pTag, int lr, int cv) {
        return rhsToC.lookupIndex(new Rhs(CHILD, pTag, lr, cv));
    }
    
    public int getCDecision(int pTag, int lr, int dv) {
        return rhsToC.lookupIndex(new Rhs(DECISION, pTag, lr, dv));
    }
    
    // TODO: maybe exposing this method is a bad idea.
    public Rhs getRhs(int c) {
        return rhsToC.lookupObject(c);
    }

    /**
     * Gets the upper bound for each feature count in the corpus.
     */
    public int[][] getTotMaxFreqCm() {
        int[][] supFreqCm = getTotSupervisedFreqCm();
        int[][] unsupMaxFreqCm = getTotUnsupervisedMaxFreqCm();
        
        int[][] totMaxFreqCm = new int[getNumConds()][];
        for (int c = 0; c < getNumConds(); c++) {
            totMaxFreqCm[c] = new int[getNumParams(c)];
            for (int m = 0; m < getNumParams(c); m++) {
                totMaxFreqCm[c][m] = supFreqCm[c][m] + unsupMaxFreqCm[c][m];              
            }
        }
        return totMaxFreqCm;
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.gridsearch.dmv.IndexedCpt#getTotalMaxFreqCm()
     */
    public int[][] getTotUnsupervisedMaxFreqCm() {
        return unsupMaxTotFreqCm;
    }

    /* (non-Javadoc)
     * @see edu.jhu.gridsearch.dmv.IndexedCpt#getTotalMaxFreqCm(int, int)
     */
    public int getTotUnsupervisedMaxFreqCm(int c, int m) {
        return unsupMaxTotFreqCm[c][m];
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.gridsearch.dmv.IndexedCpt#getNumNonZeroMaxFreqCms()
     */
    public int getNumNonZeroUnsupMaxFreqCms() {
        return numNZUnsupMaxFreqCms;
    }
    
    public int getNumSentVars(int s) {        
        return sentParamToI.get(s).size();
    }
    
    /**
     * Gets the index of the conditional distribution for the i^th sentence variable
     * @param s Sentence index
     * @param i Sentence variable index
     */
    public int getC(int s, int i) {
        return sentCmToI.get(s).lookupObject(i).get1();
    }

    /**
     * Gets the index of the model parameter for the i^th sentence variable
     * @param s Sentence index
     * @param i Sentence variable index
     */
    public int getM(int s, int i) {
        return sentParamToI.get(s).lookupObject(i).get2();
    }

    /**
     * Not used anywhere.
     */
    @Deprecated
    public int getMaxFreq(int s, int i) {
        return sentMaxFreqSi[s][i];
    }

    /** 
     * @return Sentence index i for sentence s corresponding to parameter c,m
     * or -1 if c,m is never used in sentence s.
     */
    public int getSi(int s, int c, int m) {
        return sentCmToI.get(s).lookupIndex(new CM(c,m));
    }
    
    // ------------------ Begin Feature Counts -----------------------
    
    /**
     * Get maximum likelihood DMV given this treebank, smoothed by lambda.
     */
    public static DmvModel getMleDmv(DepTreebank treebank, double lambda) {
        DmvModel dmv = new DmvModel(treebank.getAlphabet());
        getMleDmv(treebank, lambda, dmv);
        return dmv;
    }
    
    /**
     * Get maximum likelihood DMV given this treebank, smoothed by lambda.
     */
    public static void getMleDmv(DepTreebank treebank, double lambda, DmvModel dmv) {
        dmv.fill(0.0);
        for (int s = 0; s < treebank.size(); s++) {
            addSentSol(treebank.getSentences().get(s), treebank.get(s), dmv);
        }
        dmv.addConstant(lambda);
        dmv.convertRealToLog();
        dmv.logNormalize();
    }
    
    /**
     * Adds the frequency counts for each parameter to dmv model as counts.
     */
    public static void addSentSol(Sentence sentence, DepTree depTree, DmvModel dmv) {
        int[] tags = sentence.getLabelIds();
        int[] parents = depTree.getParents();
        
        // TODO: We could just use getSentSol(s, depTree) here.
        
        // Count number of times tree contains each feature
        for (int cIdx=0; cIdx<parents.length; cIdx++) {
            int pIdx = parents[cIdx];
            int cTag = tags[cIdx];
            if (pIdx == WallDepTreeNode.WALL_POSITION) {
                dmv.root[cTag]++;
            } else {
                int pTag = tags[pIdx];
                int lr = cIdx < pIdx ? DmvModel.LEFT : DmvModel.RIGHT;
                dmv.child[cTag][pTag][lr]++;
            }
            for (int lr=0; lr<2; lr++) {
                String lrs = lr == 0 ? "l" : "r";
                int numOnSide = depTree.getNodes().get(cIdx+1).getChildrenToSide(lrs).size();
                if (numOnSide == 0) {
                    // stopAdj
                    dmv.decision[cTag][lr][0][DmvModel.END]++;
                } else {
                    // contAdj
                    dmv.decision[cTag][lr][0][DmvModel.CONT]++;
                    if (numOnSide - 1 > 0) {
                        // contNonAdj
                        dmv.decision[cTag][lr][1][DmvModel.CONT] += numOnSide - 1;
                    }
                    // stopNonAdj
                    dmv.decision[cTag][lr][1][DmvModel.END]++;
                }
            }
        }
    }
    
    /**
     * Gets the lower bound for each feature count in the corpus.
     */
    public int[][] getTotSupervisedFreqCm() {
        return getTotSupervisedFreqCm(corpus);
    }
    
    public int[][] getTotSupervisedFreqCm(DmvTrainCorpus corpus) {
        int[][] totFreqCm = new int[getNumConds()][];
        for (int c = 0; c < getNumConds(); c++) {
            totFreqCm[c] = new int[getNumParams(c)];
        }
        for (int s = 0; s < corpus.size(); s++) {
            if (corpus.isLabeled(s)) {
                addSentSol(corpus.getSentence(s), corpus.getTree(s), totFreqCm);
            }
        }
        return totFreqCm;
    }
    
    public int[][] getTotFreqCm(DepTreebank treebank) {
        int[][] totFreqCm = new int[getNumConds()][];
        for (int c = 0; c < getNumConds(); c++) {
            totFreqCm[c] = new int[getNumParams(c)];
        }
        for (int s = 0; s < treebank.size(); s++) {
            addSentSol(treebank.getSentences().get(s), treebank.get(s), totFreqCm);
        }
        return totFreqCm;
    }
    
    /**
     * Adds the frequency counts for each parameter to counts, which are
     * indexed by c,m.
     */
    public void addSentSol(Sentence sentence, DepTree depTree, int[][] totFreqCm) {
        int[] tags = sentence.getLabelIds();
        int[] parents = depTree.getParents();
        
        // TODO: We could just use getSentSol(s, depTree) here.
        
        // Count number of times tree contains each feature
        for (int cIdx=0; cIdx<parents.length; cIdx++) {
            int pIdx = parents[cIdx];
            int cTag = tags[cIdx];
            if (pIdx == WallDepTreeNode.WALL_POSITION) {
                Rhs rhs = new Rhs(ROOT);
                int m = cTag;
                totFreqCm[rhsToC.lookupIndex(rhs)][m]++;
            } else {
                int pTag = tags[pIdx];
                int lr = cIdx < pIdx ? DmvModel.LEFT : DmvModel.RIGHT;
                Rhs rhs = new Rhs(CHILD, pTag, lr, 0);
                int m = cTag;
                totFreqCm[rhsToC.lookupIndex(rhs)][m]++;
            }
            for (int lr=0; lr<2; lr++) {
                String lrs = lr == 0 ? "l" : "r";
                int numOnSide = depTree.getNodes().get(cIdx+1).getChildrenToSide(lrs).size();
                if (numOnSide == 0) {
                    // stopAdj
                    Rhs rhs = new Rhs(DECISION, cTag, lr, 0);
                    int m = DmvModel.END;
                    totFreqCm[rhsToC.lookupIndex(rhs)][m]++;
                } else {
                    Rhs rhs;
                    int m;
                    // contAdj
                    rhs = new Rhs(DECISION, cTag, lr, 0);
                    m = DmvModel.CONT;
                    totFreqCm[rhsToC.lookupIndex(rhs)][m]++;
                    if (numOnSide - 1 > 0) {
                        // contNonAdj
                        rhs = new Rhs(DECISION, cTag, lr, 1);
                        m = DmvModel.CONT;
                        totFreqCm[rhsToC.lookupIndex(rhs)][m] += numOnSide - 1;
                    }
                    // stopNonAdj
                    rhs = new Rhs(DECISION, cTag, lr, 1);
                    m = DmvModel.END;
                    totFreqCm[rhsToC.lookupIndex(rhs)][m]++;
                }
            }
        }
    }

    /**
     * Returns the frequency counts for each parameter 
     * indexed by the sentence variable indices
     * @param sentence This parameter isn't necessary, but we often already have it, 
     *  and allow it to be passed in for that case.
     */
    public int[] getSentSol(Sentence sentence, int s, DepTree depTree) {
        assert(sentence == this.corpus.getSentence(s));
        int[] tags = sentence.getLabelIds();
        int[] parents = depTree.getParents();
        Alphabet<ParamId> paramToI = sentParamToI.get(s);
        
        // Create the sentence solution 
        int[] sentSol = new int[getNumSentVars(s)];
        
        // Count number of times tree contains each feature
        for (int cIdx=0; cIdx<parents.length; cIdx++) {
            int pIdx = parents[cIdx];
            int cTag = tags[cIdx];
            if (pIdx == WallDepTreeNode.WALL_POSITION) {
                Rhs rhs = new Rhs(ROOT);
                int m = cTag;
                sentSol[paramToI.lookupIndex(new ParamId(rhs, m))]++;
            } else {
                int pTag = tags[pIdx];
                int lr = cIdx < pIdx ? DmvModel.LEFT : DmvModel.RIGHT;
                Rhs rhs = new Rhs(CHILD, pTag, lr, 0);
                int m = cTag;
                sentSol[paramToI.lookupIndex(new ParamId(rhs, m))]++;
            }
            for (int lr=0; lr<2; lr++) {
                String lrs = lr == 0 ? "l" : "r";
                int numOnSide = depTree.getNodes().get(cIdx+1).getChildrenToSide(lrs).size();
                if (numOnSide == 0) {
                    // stopAdj
                    Rhs rhs = new Rhs(DECISION, cTag, lr, 0);
                    int m = DmvModel.END;
                    sentSol[paramToI.lookupIndex(new ParamId(rhs, m))]++;
                } else {
                    Rhs rhs;
                    int m;
                    // contAdj
                    rhs = new Rhs(DECISION, cTag, lr, 0);
                    m = DmvModel.CONT;
                    sentSol[paramToI.lookupIndex(new ParamId(rhs, m))]++;
                    if (numOnSide - 1 > 0) {
                        // contNonAdj
                        rhs = new Rhs(DECISION, cTag, lr, 1);
                        m = DmvModel.CONT;
                        sentSol[paramToI.lookupIndex(new ParamId(rhs, m))] += numOnSide - 1;
                    }
                    // stopNonAdj
                    rhs = new Rhs(DECISION, cTag, lr, 1);
                    m = DmvModel.END;
                    sentSol[paramToI.lookupIndex(new ParamId(rhs, m))]++;
                }
            }
        }
        return sentSol;
    }
    
    // ------------------ End Feature Counts -----------------------

    /**
     * This is currently unused.
     */
    @Deprecated
    public DmvSentParamCache getDmvSentParamCache(Sentence sentence, int s, double[] sentLogProbs) {
        DmvModel dmv = new DmvModel(alphabet);
        dmv.fill(Double.NEGATIVE_INFINITY);
        
        // Map the sentence variable indices --> indices of DmvModel
        Alphabet<ParamId> paramToI = sentParamToI.get(s);
        for (int i=0; i<getNumSentVars(s); i++) {
            ParamId p = paramToI.lookupObject(i);
            Rhs rhs = p.get1();
            int m = p.get2();
            double logProb = sentLogProbs[i];
            setDmvValue(dmv, rhs, m, logProb);
        }
        
        log.trace("dmv: " + dmv);
        return new DmvSentParamCache(dmv, sentence);
    }
    
    public DmvModel getDmvModel(double[][] logProbs) {
        DmvModel dmv = new DmvModel(alphabet);
        dmv.fill(Double.NEGATIVE_INFINITY);

        // Map the sentence variable indices --> indices of DmvModel
        for (int c=0; c<logProbs.length; c++) {
            Rhs rhs = rhsToC.lookupObject(c);
            assert(logProbs[c].length == getNumParams(c));
            for (int m=0; m<logProbs[c].length; m++) {
                double logProb = logProbs[c][m];
                setDmvValue(dmv, rhs, m, logProb);
            }
        }
        return dmv;
    }

    private static void setDPMValue(DmvModel dmv, ParamId param, double logProb) {
        Rhs rhs = param.get1();
        int m = param.get2();
        setDmvValue(dmv, rhs, m, logProb);
    }
    
    private static void setDmvValue(DmvModel dmv, Rhs rhs, int m, double logProb) {
        log.trace(String.format("setDPMValue: rhs=%s m=%d logProb=%f", rhs.toString(), m, logProb));
        if (rhs.get(0) == ROOT) {
            dmv.root[m] = logProb;
        } else if (rhs.get(0) == CHILD) {
            // For reference: child[cTag][pTag][lr];
            dmv.child[m][rhs.get(1)][rhs.get(2)] = logProb;
        } else if (rhs.get(0) == DECISION) {
            // For reference: decision[pTag][lr][decisionValency][m];
            dmv.decision[rhs.get(1)][rhs.get(2)][rhs.get(3)][m] = logProb;
        } else {
            throw new IllegalStateException("Unsupported type");
        }
    }
    
    public static double getDmvValue(DmvModel dmv, ParamId param) {
        Rhs rhs = param.get1();
        int m = param.get2();
        return getDmvValue(dmv, rhs, m);
    }
    
    public static double getDmvValue(DmvModel dmv, Rhs rhs, int m) {
        //log.trace(String.format("getDPMValue: rhs=%s m=%d logProb=%f", rhs.toString(), m));
        if (rhs.get(0) == ROOT) {
            return dmv.root[m];
        } else if (rhs.get(0) == CHILD) {
            // For reference: child[cTag][pTag][lr];
            return dmv.child[m][rhs.get(1)][rhs.get(2)];
        } else if (rhs.get(0) == DECISION) {
            // For reference: decision[pTag][lr][decisionValency][m];
            return dmv.decision[rhs.get(1)][rhs.get(2)][rhs.get(3)][m];
        } else {
            throw new IllegalStateException("Unsupported type");
        }
    }

    public double[][] getCmLogProbs(DmvModel dmv) {
        double[][] logProbs = new double[getNumConds()][];
        for (int c=0; c<logProbs.length; c++) {
            Rhs rhs = rhsToC.lookupObject(c);
            logProbs[c] = new double[getNumParams(c)];
            for (int m=0; m<logProbs[c].length; m++) {
                logProbs[c][m] = getDmvValue(dmv, rhs, m);
            }
        }
        return logProbs;
    }
    
    public Alphabet<Label> getLabelAlphabet() {
        return alphabet;
    }
    
}
