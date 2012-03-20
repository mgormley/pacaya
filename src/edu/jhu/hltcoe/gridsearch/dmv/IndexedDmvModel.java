package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import util.Alphabet;
import depparsing.globals.Constants;
import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.pr.DepInstance;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.parse.pr.DepSentenceDist;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;

public class IndexedDmvModel {
    
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
    
    private class CM extends Pair<Integer,Integer> {
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
    private int[][] sentMaxFreq; 
    private SentenceCollection sentences;

    
    @SuppressWarnings("unchecked")
    public IndexedDmvModel(SentenceCollection sentences, DmvModel model) {
        this.sentences = sentences;
        
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
        
        // Create a map of individual parameters to sentence indices
        sentParamToI = new ArrayList<Alphabet<Param>>();
        for (Sentence sent : sentences) {
            // Create the set of tags contained in this sentence
            int[] tags = sent.getLabelIds();
            TIntHashSet tagSet = new TIntHashSet(tags);

            Alphabet<Param> paramToI = new Alphabet<Param>();
            for (int c=0; c<getNumConds(); c++) {
                Rhs rhs = rhsToC.lookupIndex(c);
                for (int m=0; m<getNumParams(c); m++) {
                    if (rhs.get(0) == ROOT) {
                        if (!tagSet.contains(m)) {
                            continue;
                        }
                    } else if (rhs.get(0) == CHILD) {
                        if (!tagSet.contains(m)) {
                            continue;
                        }
                        if (!tagSet.contains(rhs.get(1))) {
                            continue;
                        }
                    } else if (rhs.get(0) == DECISION) {
                        if (!tagSet.contains(rhs.get(1))) {
                            continue;
                        }
                    } else {
                        throw new IllegalStateException("Unsupported type");
                    }
                    paramToI.lookupObject(new Param(rhs,m));
                }
            }
            paramToI.stopGrowth();
            sentParamToI.add(paramToI);
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
        }
        
        // Create the count of max frequencies for each model parameter
        sentMaxFreq = new int[sentences.size()][];
        for (int s=0; s<sentences.size(); s++) {
            sentMaxFreq[s] = new int[sentParamToI.get(s).size()];
            //TODO:
        }
    }
    
    public int getNumConds() {
        // (num root dists) + (num child dists) + (num decision dists)
        // return (1) + (numTags * 2 * childValency) + (numTags * 2 *decisionValency);
        return rhsToC.size();
    }

    public int getNumParams(int c) { 
//        if (c < (1) + (numTags * 2 * childValency)) {
//            // Root or child
//            return numTags;
//        } else {
//            // Decision (END or CONT)
//            return 2;
//        }
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
//        if (c < (1) + (numTags * 2 * childValency)) {
//            // Root
//            return String.format("root(%d)", m);
//        } else if (c < (1) + (numTags * 2 * childValency)) {
//            // Child
//            c -= 1;
//            int parent = c / (2*childValency);
//            c          = c % (2*childValency);
//            int lr     = c / childValency;
//            c          = c % childValency;
//            int cv     = c;
//            return String.format("child_{%d,%d,%d}(%d)", parent, lr, cv, m);
//        } else {
//            // Decision (END or CONT)
//            c -= (1) + (numTags * 2 * childValency);
//            int parent = c / (2 *decisionValency);
//            c          = c % (2 *decisionValency);
//            int lr     = c / decisionValency;
//            c          = c % decisionValency;
//            int dv = c;
//            return String.format("decis_{%d,%d,%d}(%d)", parent, lr, dv, m);
//        }
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
    
//    private int[] getDmvIndices(int c, int m) {
//        if (c < (1) + (numTags * 2 * childValency)) {
//            // Root
//            return new int[]{ROOT, m};
//        } else if (c < (1) + (numTags * 2 * childValency)) {
//            // Child
//            c -= 1;
//            int parent = c / (2*childValency);
//            c          = c % (2*childValency);
//            int lr     = c / childValency;
//            c          = c % childValency;
//            int cv     = c;
//            return new int[]{CHILD, parent, lr, cv, m};
//        } else {
//            // Decision (END or CONT)
//            c -= (1) + (numTags * 2 * childValency);
//            int parent = c / (2 *decisionValency);
//            c          = c % (2 *decisionValency);
//            int lr     = c / decisionValency;
//            c          = c % decisionValency;
//            int dv = c;
//            return new int[]{DECISION, parent, lr, dv, m};
//        }
//    }

    private int getCRoot() {
        return rhsToC.lookupObject(new Rhs(ROOT));
    }
    
    private int getCChild(int parent, int lr, int cv) {
//        // Skip over root
//        int c = 1;
//        // Jump to correct spot
//        c += cv;
//        c += lr * childValency;
//        c += parent * 2 * childValency;
//        return c;
        return rhsToC.lookupObject(new Rhs(CHILD, parent, lr, cv));
    }
    
    private int getCDecision(int parent, int lr, int dv) {
//        // Skip over root and child
//        int c = (1) + (numTags * 2 * childValency);
//        // Jumpt to correct spot
//        c += dv;
//        c += lr * decisionValency;
//        c += parent * 2 * decisionValency;
//        return c;
        return rhsToC.lookupObject(new Rhs(DECISION, parent, lr, dv));
    }

    public int getNumSentVars(int s) {
//        // (num root edge vars) + (num edge vars) 
//        // + (num decision vars = length * (stopAdj + stopNonAdj + contAdj + contNonAdj))
//        //return (sentence) + (sentence*sentence) + (sentence * 4);
//        int[] tags = sentence.getLabelIds();
//        int[] tagSet = new TIntHashSet(tags).toArray();
//        //Arrays.sort(tagSet);
//        int numSentTags = tagSet.length;
//        return (numSentTags) + (numSentTags * 2 * childValency * numSentTags) + (numSentTags * 2 * decisionValency * 2);
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
    public double getMaxFreq(int s, int i) {
        return sentMaxFreq[s][i];
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
            int pTag = tags[pIdx];
            if (pTag == WallDepTreeNode.WALL_POSITION) {
                Rhs rhs = new Rhs(ROOT);
                int m = cTag;
                sentSol[paramToI.lookupObject(new Param(rhs, m))]++;
            } else {
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
                    // contNonAdj
                    rhs = new Rhs(DECISION, cTag, lr, 1);
                    m = Constants.CONT;
                    sentSol[paramToI.lookupObject(new Param(rhs, m))] += numOnSide-1;
                    // stopNonAdj
                    rhs = new Rhs(DECISION, cTag, lr, 1);
                    m = Constants.END;
                    sentSol[paramToI.lookupObject(new Param(rhs, m))]++;
                }
            }
        }
        return sentSol;
    }

    public DepSentenceDist getDepSentenceDist(Sentence sentence, int s, double[] sentParams) {
        DepProbMatrix depProbMatrix = new DepProbMatrix(sentences.getLabelAlphabet(), decisionValency, childValency);
        depProbMatrix.fill(Double.NEGATIVE_INFINITY);
        
        // Map the sentence variable indices --> indices of DepProbMatrix
        Alphabet<Param> paramToI = sentParamToI.get(s);
        for (int i=0; i<sentParams.length; i++) {
            Param p = paramToI.lookupIndex(i);
            Rhs rhs = p.get1();
            int m = p.get2();
            double logProb = sentParams[i];
            setDPMValue(depProbMatrix, rhs, m, logProb);
        }
        
        DepInstance depInst = new DepInstance(sentence.getLabelIds());
        DepSentenceDist sd = new DepSentenceDist(depInst, depProbMatrix);
        return sd;
    }

    private DepProbMatrix getDepProbMatrix(double[][] modelParams) {
        DepProbMatrix depProbMatrix = new DepProbMatrix(sentences.getLabelAlphabet(), decisionValency, childValency);
        depProbMatrix.fill(Double.NEGATIVE_INFINITY);
        
        // Map the sentence variable indices --> indices of DepProbMatrix
        for (int c=0; c<modelParams.length; c++) {
            Rhs rhs = rhsToC.lookupIndex(c);
            assert(modelParams[c].length == getNumParams(c));
            for (int m=0; m<modelParams[c].length; m++) {
                double logProb = modelParams[c][m];
                setDPMValue(depProbMatrix, rhs, m, logProb);
            }
        }
        return depProbMatrix;
    }

    private void setDPMValue(DepProbMatrix depProbMatrix, Rhs rhs, int m, double logProb) {
        if (rhs.get(0) == ROOT) {
            depProbMatrix.root[m] = logProb;
        } else if (rhs.get(0) == CHILD) {
            // For reference: child = new double[numTags][numTags][2][childValency];
            depProbMatrix.child[rhs.get(1)][m][rhs.get(2)][rhs.get(3)] = logProb;
        } else if (rhs.get(0) == DECISION) {
            // For reference: decision = new double[numTags][2][decisionValency][2];
            depProbMatrix.decision[rhs.get(1)][rhs.get(2)][rhs.get(3)][m] = logProb;
        } else {
            throw new IllegalStateException("Unsupported type");
        }
    }
    
    public DmvModel getDmvModel(double[][] modelParams) {
        return depProbMatrixToDmvModel(getDepProbMatrix(modelParams));
    }

}
