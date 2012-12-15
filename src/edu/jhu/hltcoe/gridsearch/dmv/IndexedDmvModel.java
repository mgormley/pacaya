package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.Set;

import org.apache.log4j.Logger;

import util.Alphabet;
import depparsing.globals.Constants;
import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.gridsearch.cpt.IndexedCpt;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.cky.DepInstance;
import edu.jhu.hltcoe.parse.cky.DepProbMatrix;
import edu.jhu.hltcoe.parse.cky.DepSentenceDist;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.IntTuple;
import edu.jhu.hltcoe.util.Pair;

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
    
    
    private static final int ROOT = 0;
    private static final int CHILD = 1;
    private static final int DECISION = 2;
    
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
        this.corpus = corpus;
        this.alphabet = corpus.getLabelAlphabet();

        numTags = alphabet.size();
        log.trace("numTags: " + numTags);
        
        // Create map of DepProbMatrix right hand sides to integers
        rhsToC = new Alphabet<Rhs>();
        rhsToC.startGrowth();
        // Create for DepProbMatrix
        rhsToC.lookupObject(new Rhs(ROOT));
        for(int p = 0; p < numTags; p++)
            for(int dir = 0; dir < 2; dir++) 
                for(int cv = 0; cv < childValency; cv++)
                    rhsToC.lookupObject(new Rhs(CHILD, p, dir, cv));
        for(int p = 0; p < numTags; p++)
            for(int dir = 0; dir < 2; dir++)
                for(int dv = 0; dv < decisionValency; dv++)
                    rhsToC.lookupObject(new Rhs(DECISION, p, dir, dv));
        rhsToC.stopGrowth();
        log.trace("rhsToC: size=" + rhsToC.size() + " alphabet=" + rhsToC);
        
        sentMaxFreqCms = new int[corpus.size()][][];
        for (int s=0; s<corpus.size(); s++) {
            sentMaxFreqCms[s] = getSentMaxFreqCm(corpus.getSentence(s)); 
        }
        
        // Create a map of individual parameters to sentence indices
        sentParamToI = new ArrayList<Alphabet<ParamId>>();
        for (int s=0; s<corpus.size(); s++) {
            Alphabet<ParamId> paramToI = new Alphabet<ParamId>();
            for (int c=0; c<getNumConds(); c++) {
                Rhs rhs = rhsToC.lookupIndex(c);
                for (int m=0; m<getNumParams(c); m++) {
                    if (sentMaxFreqCms[s][c][m] > 0) {
                        paramToI.lookupObject(new ParamId(rhs,m));
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
                ParamId p = paramToI.lookupIndex(i);
                Rhs rhs = p.get1();
                int m = p.get2();
                cmToI.lookupObject(new CM(rhsToC.lookupObject(rhs),m));
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

        // Count total number of parameters
        numTotalParams = 0;
        for (int c = 0; c < getNumConds(); c++) {
            for (int m = 0; m < getNumParams(c); m++) {
                numTotalParams++;
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
            maxFreq[rhsToC.lookupObject(new Rhs(ROOT))][cTag] = 1;
            
            // Each edge has some child parameter and can appear once
            for (int pIdx=0; pIdx<tags.length; pIdx++) {
                if (cIdx == pIdx) {
                    continue;
                }
                int pTag = tags[pIdx];

                int lr = cIdx < pIdx ? Constants.LEFT : Constants.RIGHT;
                Rhs rhs = new Rhs(CHILD, pTag, lr, 0);
                int m = cTag;
                maxFreq[rhsToC.lookupObject(rhs)][m]++;
            }
            
            // For each direction (LEFT, RIGHT)
            for (int lr=0; lr<2; lr++) {
                // Each decision can appear for each tag of that type in the sentence
                
                int numOnSide = (lr == 0) ? cIdx : tags.length - cIdx - 1;

                Rhs rhs;
                int m;
                // stopAdj
                rhs = new Rhs(DECISION, cTag, lr, 0);
                m = Constants.END;
                maxFreq[rhsToC.lookupObject(rhs)][m]++;
                if (numOnSide > 0) {
                    // contAdj
                    rhs = new Rhs(DECISION, cTag, lr, 0);
                    m = Constants.CONT;
                    maxFreq[rhsToC.lookupObject(rhs)][m]++;
                    // contNonAdj
                    rhs = new Rhs(DECISION, cTag, lr, 1);
                    m = Constants.CONT;
                    maxFreq[rhsToC.lookupObject(rhs)][m] += numOnSide-1;
                    // stopNonAdj
                    rhs = new Rhs(DECISION, cTag, lr, 1);
                    m = Constants.END;
                    maxFreq[rhsToC.lookupObject(rhs)][m]++;
                }
            }
        }
        return maxFreq;
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.gridsearch.dmv.IndexedCpt#getNumConds()
     */
    public int getNumConds() {
        return rhsToC.size();
    }

    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.gridsearch.dmv.IndexedCpt#getNumTotalParams()
     */
    public int getNumTotalParams() {
        return numTotalParams;
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.gridsearch.dmv.IndexedCpt#getNumParams(int)
     */
    public int getNumParams(int c) { 
        Rhs rhs = rhsToC.lookupIndex(c);
        if (rhs.get(0) == ROOT || rhs.get(0) == CHILD) {
            return numTags;
        } else if (rhs.get(0) == DECISION) {
            return 2;
        } else {
            throw new IllegalStateException("Unsupported type");
        }
    }

    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.gridsearch.dmv.IndexedCpt#getName(int, int)
     */
    public String getName(int c, int m) {
        Rhs rhs = rhsToC.lookupIndex(c);
        if (rhs.get(0) == ROOT) {
            String cTag = alphabet.lookupIndex(m).getLabel();
            return String.format("root(%s)", cTag);
        } else if (rhs.get(0) == CHILD) {
            String pTag = alphabet.lookupIndex(rhs.get(1)).getLabel();
            String lr = rhs.get(2) == Constants.LEFT ? "l" : "r";
            String cTag = alphabet.lookupIndex(m).getLabel();
            return String.format("child_{%s,%s,%d}(%s)", pTag, lr, rhs.get(3), cTag);
        } else if (rhs.get(0) == DECISION) {
            String pTag = alphabet.lookupIndex(rhs.get(1)).getLabel();
            String lr = (rhs.get(2) == Constants.LEFT) ? "l" : "r";
            String sc = (m == Constants.END) ? "s" : "c";
            return String.format("dec_{%s,%s,%d}(%s)",  pTag, lr, rhs.get(3), sc);
        } else {
            throw new IllegalStateException("Unsupported type");
        }
    }

    public int getCRoot() {
        return rhsToC.lookupObject(new Rhs(ROOT));
    }
    
    public int getCChild(int pTag, int lr, int cv) {
        return rhsToC.lookupObject(new Rhs(CHILD, pTag, lr, cv));
    }
    
    public int getCDecision(int pTag, int lr, int dv) {
        return rhsToC.lookupObject(new Rhs(DECISION, pTag, lr, dv));
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
     * @see edu.jhu.hltcoe.gridsearch.dmv.IndexedCpt#getTotalMaxFreqCm()
     */
    public int[][] getTotUnsupervisedMaxFreqCm() {
        return unsupMaxTotFreqCm;
    }

    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.gridsearch.dmv.IndexedCpt#getTotalMaxFreqCm(int, int)
     */
    public int getTotUnsupervisedMaxFreqCm(int c, int m) {
        return unsupMaxTotFreqCm[c][m];
    }
    
    /* (non-Javadoc)
     * @see edu.jhu.hltcoe.gridsearch.dmv.IndexedCpt#getNumNonZeroMaxFreqCms()
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
        return sentCmToI.get(s).lookupIndex(i).get1();
    }

    /**
     * Gets the index of the model parameter for the i^th sentence variable
     * @param s Sentence index
     * @param i Sentence variable index
     */
    public int getM(int s, int i) {
        return sentParamToI.get(s).lookupIndex(i).get2();
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
        return sentCmToI.get(s).lookupObject(new CM(c,m));
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
                int lr = cIdx < pIdx ? Constants.LEFT : Constants.RIGHT;
                dmv.child[cTag][pTag][lr][0]++;
            }
            for (int lr=0; lr<2; lr++) {
                String lrs = lr == 0 ? "l" : "r";
                int numOnSide = depTree.getNodes().get(cIdx+1).getChildrenToSide(lrs).size();
                if (numOnSide == 0) {
                    // stopAdj
                    dmv.decision[cTag][lr][0][Constants.END]++;
                } else {
                    // contAdj
                    dmv.decision[cTag][lr][0][Constants.CONT]++;
                    if (numOnSide - 1 > 0) {
                        // contNonAdj
                        dmv.decision[cTag][lr][1][Constants.CONT] += numOnSide - 1;
                    }
                    // stopNonAdj
                    dmv.decision[cTag][lr][1][Constants.END]++;
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
                totFreqCm[rhsToC.lookupObject(rhs)][m]++;
            } else {
                int pTag = tags[pIdx];
                int lr = cIdx < pIdx ? Constants.LEFT : Constants.RIGHT;
                Rhs rhs = new Rhs(CHILD, pTag, lr, 0);
                int m = cTag;
                totFreqCm[rhsToC.lookupObject(rhs)][m]++;
            }
            for (int lr=0; lr<2; lr++) {
                String lrs = lr == 0 ? "l" : "r";
                int numOnSide = depTree.getNodes().get(cIdx+1).getChildrenToSide(lrs).size();
                if (numOnSide == 0) {
                    // stopAdj
                    Rhs rhs = new Rhs(DECISION, cTag, lr, 0);
                    int m = Constants.END;
                    totFreqCm[rhsToC.lookupObject(rhs)][m]++;
                } else {
                    Rhs rhs;
                    int m;
                    // contAdj
                    rhs = new Rhs(DECISION, cTag, lr, 0);
                    m = Constants.CONT;
                    totFreqCm[rhsToC.lookupObject(rhs)][m]++;
                    if (numOnSide - 1 > 0) {
                        // contNonAdj
                        rhs = new Rhs(DECISION, cTag, lr, 1);
                        m = Constants.CONT;
                        totFreqCm[rhsToC.lookupObject(rhs)][m] += numOnSide - 1;
                    }
                    // stopNonAdj
                    rhs = new Rhs(DECISION, cTag, lr, 1);
                    m = Constants.END;
                    totFreqCm[rhsToC.lookupObject(rhs)][m]++;
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
                sentSol[paramToI.lookupObject(new ParamId(rhs, m))]++;
            } else {
                int pTag = tags[pIdx];
                int lr = cIdx < pIdx ? Constants.LEFT : Constants.RIGHT;
                Rhs rhs = new Rhs(CHILD, pTag, lr, 0);
                int m = cTag;
                sentSol[paramToI.lookupObject(new ParamId(rhs, m))]++;
            }
            for (int lr=0; lr<2; lr++) {
                String lrs = lr == 0 ? "l" : "r";
                int numOnSide = depTree.getNodes().get(cIdx+1).getChildrenToSide(lrs).size();
                if (numOnSide == 0) {
                    // stopAdj
                    Rhs rhs = new Rhs(DECISION, cTag, lr, 0);
                    int m = Constants.END;
                    sentSol[paramToI.lookupObject(new ParamId(rhs, m))]++;
                } else {
                    Rhs rhs;
                    int m;
                    // contAdj
                    rhs = new Rhs(DECISION, cTag, lr, 0);
                    m = Constants.CONT;
                    sentSol[paramToI.lookupObject(new ParamId(rhs, m))]++;
                    if (numOnSide - 1 > 0) {
                        // contNonAdj
                        rhs = new Rhs(DECISION, cTag, lr, 1);
                        m = Constants.CONT;
                        sentSol[paramToI.lookupObject(new ParamId(rhs, m))] += numOnSide - 1;
                    }
                    // stopNonAdj
                    rhs = new Rhs(DECISION, cTag, lr, 1);
                    m = Constants.END;
                    sentSol[paramToI.lookupObject(new ParamId(rhs, m))]++;
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
    public DepSentenceDist getDepSentenceDist(Sentence sentence, int s, double[] sentLogProbs) {
        DepProbMatrix depProbMatrix = new DepProbMatrix(alphabet, decisionValency, childValency);
        depProbMatrix.fill(Double.NEGATIVE_INFINITY);
        
        // Map the sentence variable indices --> indices of DepProbMatrix
        Alphabet<ParamId> paramToI = sentParamToI.get(s);
        for (int i=0; i<getNumSentVars(s); i++) {
            ParamId p = paramToI.lookupIndex(i);
            Rhs rhs = p.get1();
            int m = p.get2();
            double logProb = sentLogProbs[i];
            setDPMValue(depProbMatrix, rhs, m, logProb);
        }
        
        log.trace("depProbMatrix: " + depProbMatrix);
        DepInstance depInst = new DepInstance(sentence.getLabelIds());
        DepSentenceDist sd = new DepSentenceDist(depInst, depProbMatrix);
        return sd;
    }
    
    public DmvModel getDmvModel(double[][] logProbs) {
        DmvModel dmv = new DmvModel(alphabet);
        dmv.fill(Double.NEGATIVE_INFINITY);
        
        // Map the sentence variable indices --> indices of DepProbMatrix
        for (int c=0; c<logProbs.length; c++) {
            Rhs rhs = rhsToC.lookupIndex(c);
            assert(logProbs[c].length == getNumParams(c));
            for (int m=0; m<logProbs[c].length; m++) {
                double logProb = logProbs[c][m];
                setDPMValue(dmv, rhs, m, logProb);
            }
        }
        return dmv;
    }

    private static void setDPMValue(DepProbMatrix depProbMatrix, ParamId param, double logProb) {
        Rhs rhs = param.get1();
        int m = param.get2();
        setDPMValue(depProbMatrix, rhs, m, logProb);
    }
    
    private static void setDPMValue(DepProbMatrix depProbMatrix, Rhs rhs, int m, double logProb) {
        log.trace(String.format("setDPMValue: rhs=%s m=%d logProb=%f", rhs.toString(), m, logProb));
        if (rhs.get(0) == ROOT) {
            depProbMatrix.root[m] = logProb;
        } else if (rhs.get(0) == CHILD) {
            // For reference: child[cTag][pTag][lr][childValency];
            depProbMatrix.child[m][rhs.get(1)][rhs.get(2)][rhs.get(3)] = logProb;
        } else if (rhs.get(0) == DECISION) {
            // For reference: decision[pTag][lr][decisionValency][m];
            depProbMatrix.decision[rhs.get(1)][rhs.get(2)][rhs.get(3)][m] = logProb;
        } else {
            throw new IllegalStateException("Unsupported type");
        }
    }
    
    public static double getDPMValue(DepProbMatrix depProbMatrix, ParamId param) {
        Rhs rhs = param.get1();
        int m = param.get2();
        return getDPMValue(depProbMatrix, rhs, m);
    }
    
    public static double getDPMValue(DepProbMatrix depProbMatrix, Rhs rhs, int m) {
        //log.trace(String.format("getDPMValue: rhs=%s m=%d logProb=%f", rhs.toString(), m));
        if (rhs.get(0) == ROOT) {
            return depProbMatrix.root[m];
        } else if (rhs.get(0) == CHILD) {
            // For reference: child[cTag][pTag][lr][childValency];
            return depProbMatrix.child[m][rhs.get(1)][rhs.get(2)][rhs.get(3)];
        } else if (rhs.get(0) == DECISION) {
            // For reference: decision[pTag][lr][decisionValency][m];
            return depProbMatrix.decision[rhs.get(1)][rhs.get(2)][rhs.get(3)][m];
        } else {
            throw new IllegalStateException("Unsupported type");
        }
    }

    public double[][] getCmLogProbs(DepProbMatrix dpm) {
        double[][] logProbs = new double[getNumConds()][];
        for (int c=0; c<logProbs.length; c++) {
            Rhs rhs = rhsToC.lookupIndex(c);
            logProbs[c] = new double[getNumParams(c)];
            for (int m=0; m<logProbs[c].length; m++) {
                logProbs[c][m] = getDPMValue(dpm, rhs, m);
            }
        }
        return logProbs;
    }
    
}
