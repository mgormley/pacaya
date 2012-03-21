package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import util.Alphabet;
import util.SparseVector;
import depparsing.globals.Constants;
import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelConverter;
import edu.jhu.hltcoe.parse.pr.DepInstance;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.parse.pr.DepSentenceDist;
import edu.jhu.hltcoe.util.Pair;
import gnu.trove.TIntHashSet;

public class IndexedDmvModel {
    
    private static Logger log = Logger.getLogger(IndexedDmvModel.class);
    
    private static class IntTuple {
        
        private final int[] x;
        
        public IntTuple(int... args) {
            x = new int[args.length];
            for (int i=0; i<args.length; i++) {
                x[i] = args[i];
            }
        }
        
        public int size() {
            return x.length;
        }
        
        public int get(int i) {
            return x[i];
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(x);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            IntTuple other = (IntTuple) obj;
            if (!Arrays.equals(x, other.x))
                return false;
            return true;
        }
        
        @Override
        public String toString() {
            return Arrays.toString(x);
        }
        
    }
    
    private class Rhs extends IntTuple {
        public Rhs(int... args) {
            super(args);
        }
    }
    
    private class Param extends Pair<Rhs,Integer> {

        public Param(Rhs x, Integer y) {
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
    private int childValency = 1;
    private int decisionValency = 2;
    private Alphabet<Rhs> rhsToC;
    private ArrayList<Alphabet<Param>> sentParamToI;
    private ArrayList<Alphabet<CM>> sentCmToI;
    private int[][] sentMaxFreqSi; 
    private SentenceCollection sentences;
    private int[][] totMaxFreqCm;
    
    public IndexedDmvModel(SentenceCollection sentences) {
        this.sentences = sentences;
        numTags = sentences.getVocab().size() - 1;
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
        
        // Create the sentence max frequency counts for c,m
        int[][][] sentMaxFreqCm = new int[sentences.size()][][];
        for (int s=0; s<sentences.size(); s++) {
            sentMaxFreqCm[s] = getSentMaxFreqCm(sentences.get(s), s); 
        }
        
        // Create a map of individual parameters to sentence indices
        sentParamToI = new ArrayList<Alphabet<Param>>();
        for (int s=0; s<sentences.size(); s++) {
            Sentence sent = sentences.get(s);
            Alphabet<Param> paramToI = new Alphabet<Param>();
            for (int c=0; c<getNumConds(); c++) {
                Rhs rhs = rhsToC.lookupIndex(c);
                for (int m=0; m<getNumParams(c); m++) {
                    if (sentMaxFreqCm[s][c][m] > 0) {
                        paramToI.lookupObject(new Param(rhs,m));
                    }
                }
            }
            paramToI.stopGrowth();
            sentParamToI.add(paramToI);
            log.trace("paramToI: s=" + s + " size=" + paramToI.size() + " alphabet=" + paramToI);
        }
        
        // Create a map from c,m indices to sentence variable indices using the previous two maps
        sentCmToI = new ArrayList<Alphabet<CM>>();
        for (int s=0; s<sentences.size(); s++) {
            Alphabet<Param> paramToI = sentParamToI.get(s);
            Alphabet<CM> cmToI = new Alphabet<CM>();
            for (int i=0; i<paramToI.size(); i++) {
                Param p = paramToI.lookupIndex(i);
                Rhs rhs = p.get1();
                int m = p.get2();
                cmToI.lookupObject(new CM(rhsToC.lookupObject(rhs),m));
            }
            cmToI.stopGrowth();
            sentCmToI.add(cmToI);
            log.trace("cmToI: s=" + s + " size=" + cmToI.size() + " alphabet=" + cmToI);
        }
        
        // Create the count of max frequencies for each model parameter in term of sentence indices
        sentMaxFreqSi = new int[sentences.size()][];
        for (int s=0; s<sentences.size(); s++) {
            // Create the sentence solution 
            sentMaxFreqSi[s] = new int[getNumSentVars(s)];
            for (int c = 0; c < getNumConds(); c++) {
                for (int m = 0; m < getNumParams(c); m++) {
                    if (sentMaxFreqCm[s][c][m] > 0) {
                        int i = getSi(s, c, m);
                        sentMaxFreqSi[s][i] = sentMaxFreqCm[s][c][m];
                    }
                }
            }
        }

        // Create the count of total max frequencies for each model parameter in term of c,m
        totMaxFreqCm = new int[getNumConds()][];
        for (int c = 0; c < getNumConds(); c++) {
            totMaxFreqCm[c] = new int[getNumParams(c)];
            for (int m = 0; m < getNumParams(c); m++) {
                for (int s = 0; s < sentMaxFreqSi.length; s++) {
                    totMaxFreqCm[c][m] += sentMaxFreqCm[s][c][m];
                }
            }
        }
    }

    /**
     * Used by DmvBoundsFactory
     */
    public int[][] getTotalMaxFreqCm() {
        return totMaxFreqCm;
    }

    private int[][] getSentMaxFreqCm(Sentence sentence, int s) {
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

//    private int[] getSentMaxFreq(Sentence sentence, int s) {
//        int[] tags = sentence.getLabelIds();
//        Alphabet<Param> paramToI = sentParamToI.get(s);
//        
//        // Create the sentence solution 
//        int[] maxFreq = new int[getNumSentVars(s)];
//        
//        // Count the MAX number of times tree could contain each feature
//        for (int cIdx=0; cIdx<tags.length; cIdx++) {
//            int cTag = tags[cIdx];
//            // The root parameter can appear once for each tag in the sentence
//            maxFreq[paramToI.lookupObject(new Param(new Rhs(ROOT), cTag))] = 1;
//            
//            // Each edge has some child parameter and can appear once
//            for (int pIdx=0; pIdx<tags.length; pIdx++) {
//                if (cIdx == pIdx) {
//                    continue;
//                }
//                int pTag = tags[pIdx];
//
//                int lr = cIdx < pIdx ? Constants.LEFT : Constants.RIGHT;
//                Rhs rhs = new Rhs(CHILD, pTag, lr, 0);
//                int m = cTag;
//                maxFreq[paramToI.lookupObject(new Param(rhs, m))]++;
//            }
//            
//            // For each direction (LEFT, RIGHT)
//            for (int lr=0; lr<2; lr++) {
//                // Each decision can appear for each tag of that type in the sentence
//                
//                int numOnSide = (lr == 0) ? cIdx : tags.length - cIdx - 1;
//
//                Rhs rhs;
//                int m;
//                // stopAdj
//                rhs = new Rhs(DECISION, cTag, lr, 0);
//                m = Constants.END;
//                maxFreq[paramToI.lookupObject(new Param(rhs, m))]++;
//                // contAdj
//                rhs = new Rhs(DECISION, cTag, lr, 0);
//                m = Constants.CONT;
//                maxFreq[paramToI.lookupObject(new Param(rhs, m))]++;
//                // contNonAdj
//                rhs = new Rhs(DECISION, cTag, lr, 1);
//                m = Constants.CONT;
//                maxFreq[paramToI.lookupObject(new Param(rhs, m))] += numOnSide-1;
//                // stopNonAdj
//                rhs = new Rhs(DECISION, cTag, lr, 1);
//                m = Constants.END;
//                maxFreq[paramToI.lookupObject(new Param(rhs, m))]++;
//            }
//        }
//        return maxFreq;
//    }
    
    public int getNumConds() {
        return rhsToC.size();
    }

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

    public String getName(int c, int m) {
        Rhs rhs = rhsToC.lookupIndex(c);
        if (rhs.get(0) == ROOT) {
            return String.format("root(%d)", m);
        } else if (rhs.get(0) == CHILD) {
            return String.format("child_{%d,%d,%d}(%d)", rhs.get(1), rhs.get(2), rhs.get(3), m);
        } else if (rhs.get(0) == DECISION) {
            return String.format("dec_{%d,%d,%d}(%d)",  rhs.get(1), rhs.get(2), rhs.get(3), m);
        } else {
            throw new IllegalStateException("Unsupported type");
        }
    }

    private int getCRoot() {
        return rhsToC.lookupObject(new Rhs(ROOT));
    }
    
    private int getCChild(int parent, int lr, int cv) {
        return rhsToC.lookupObject(new Rhs(CHILD, parent, lr, cv));
    }
    
    private int getCDecision(int parent, int lr, int dv) {
        return rhsToC.lookupObject(new Rhs(DECISION, parent, lr, dv));
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
     * 
     */
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


    /**
     * Returns the frequency counts for each parameter 
     * indexed by the sentence variable indices
     * @param sentence 
     */
    public int[] getSentSol(Sentence sentence, int s, DepTree depTree) {
        int[] tags = sentence.getLabelIds();
        int[] parents = depTree.getParents();
        Alphabet<Param> paramToI = sentParamToI.get(s);
        
        // Create the sentence solution 
        int[] sentSol = new int[getNumSentVars(s)];
        
        // Count number of times tree contains each feature
        for (int cIdx=0; cIdx<parents.length; cIdx++) {
            int pIdx = parents[cIdx];
            int cTag = tags[cIdx];
            if (pIdx == WallDepTreeNode.WALL_POSITION) {
                Rhs rhs = new Rhs(ROOT);
                int m = cTag;
                sentSol[paramToI.lookupObject(new Param(rhs, m))]++;
            } else {
                int pTag = tags[pIdx];
                int lr = cIdx < pIdx ? Constants.LEFT : Constants.RIGHT;
                Rhs rhs = new Rhs(CHILD, pTag, lr, 0);
                int m = cTag;
                sentSol[paramToI.lookupObject(new Param(rhs, m))]++;
            }
            for (int lr=0; lr<2; lr++) {
                String lrs = lr == 0 ? "l" : "r";
                int numOnSide = depTree.getNodes().get(cIdx+1).getChildrenToSide(lrs).size();
                if (numOnSide == 0) {
                    // stopAdj
                    Rhs rhs = new Rhs(DECISION, cTag, lr, 0);
                    int m = Constants.END;
                    sentSol[paramToI.lookupObject(new Param(rhs, m))]++;
                } else {
                    Rhs rhs;
                    int m;
                    // contAdj
                    rhs = new Rhs(DECISION, cTag, lr, 0);
                    m = Constants.CONT;
                    sentSol[paramToI.lookupObject(new Param(rhs, m))]++;
                    if (numOnSide - 1 > 0) {
                        // contNonAdj
                        rhs = new Rhs(DECISION, cTag, lr, 1);
                        m = Constants.CONT;
                        sentSol[paramToI.lookupObject(new Param(rhs, m))] += numOnSide - 1;
                    }
                    // stopNonAdj
                    rhs = new Rhs(DECISION, cTag, lr, 1);
                    m = Constants.END;
                    sentSol[paramToI.lookupObject(new Param(rhs, m))]++;
                }
            }
        }
        return sentSol;
    }

    public DepSentenceDist getDepSentenceDist(Sentence sentence, int s, double[] sentLogProbs) {
        DepProbMatrix depProbMatrix = new DepProbMatrix(sentences.getLabelAlphabet(), decisionValency, childValency);
        depProbMatrix.fill(Double.NEGATIVE_INFINITY);
        
        // Map the sentence variable indices --> indices of DepProbMatrix
        Alphabet<Param> paramToI = sentParamToI.get(s);
        for (int i=0; i<getNumSentVars(s); i++) {
            Param p = paramToI.lookupIndex(i);
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

    private DepProbMatrix getDepProbMatrix(double[][] logProbs) {
        DepProbMatrix depProbMatrix = new DepProbMatrix(sentences.getLabelAlphabet(), decisionValency, childValency);
        depProbMatrix.fill(Double.NEGATIVE_INFINITY);
        
        // Map the sentence variable indices --> indices of DepProbMatrix
        for (int c=0; c<logProbs.length; c++) {
            Rhs rhs = rhsToC.lookupIndex(c);
            assert(logProbs[c].length == getNumParams(c));
            for (int m=0; m<logProbs[c].length; m++) {
                double logProb = logProbs[c][m];
                setDPMValue(depProbMatrix, rhs, m, logProb);
            }
        }
        return depProbMatrix;
    }

    private void setDPMValue(DepProbMatrix depProbMatrix, Rhs rhs, int m, double logProb) {
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
    
    public DmvModel getDmvModel(double[][] logProbs) {
        return DmvModelConverter.getDmvModel(getDepProbMatrix(logProbs), sentences);
    }
    
}
