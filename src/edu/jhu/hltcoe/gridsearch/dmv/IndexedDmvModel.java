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
    
    
    private static final int ROOT = 0;
    private static final int CHILD = 1;
    private static final int DECISION = 2;
    
    private int numTags;
    private int childValency = 1;
    private int decisionValency = 2;
    private Alphabet<Rhs> rhsToC;
    private ArrayList<Alphabet<Param>> sentCmToI;

    
    @SuppressWarnings("unchecked")
    public IndexedDmvModel(SentenceCollection sentences, DmvModel model) {
        rhsToC = new Alphabet<Rhs>();
        rhsToC.startGrowth();
        // Create for DepProbMatrix
        rhsToC.lookupObject(new Rhs(ROOT));
        for(int p = 0; p < numTags; p++)
            for(int dir = 0; dir < 2; dir++) 
                for(int v = 0; v < childValency; v++)
                    rhsToC.lookupObject(new Rhs(CHILD, p, dir, v));
        for(int p = 0; p < numTags; p++)
            for(int dir = 0; dir < 2; dir++)
                for(int kids = 0; kids < decisionValency; kids++)
                    rhsToC.lookupObject(new Rhs(DECISION, p, dir, kids));
        rhsToC.stopGrowth();
        
        sentCmToI = new ArrayList<Alphabet<Param>>();
        for (Sentence sent : sentences) {
            // Create the set of tags contained in this sentence
            int[] tags = sent.getLabelIds();
            TIntHashSet tagSet = new TIntHashSet(tags);

            Alphabet<Param> alphabet = new Alphabet<Param>();
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
                    alphabet.lookupObject(new Param(rhs,m));
                }
            }
            alphabet.stopGrowth();
            sentCmToI.add(alphabet);
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
        Object o = rhsToC.lookupIndex(c);
        if (o instanceof Pair<?, ?>) {
            return numTags;
        } else if (o instanceof Triple<?, ?, ?>) {
            return 2;
        }
    }

    public String getName(int c, int m) {
        if (c < (1) + (numTags * 2 * childValency)) {
            // Root
            return String.format("root(%d)", m);
        } else if (c < (1) + (numTags * 2 * childValency)) {
            // Child
            c -= 1;
            int parent = c / (2*childValency);
            c          = c % (2*childValency);
            int lr     = c / childValency;
            c          = c % childValency;
            int cv     = c;
            return String.format("child_{%d,%d,%d}(%d)", parent, lr, cv, m);
        } else {
            // Decision (END or CONT)
            c -= (1) + (numTags * 2 * childValency);
            int parent = c / (2 *decisionValency);
            c          = c % (2 *decisionValency);
            int lr     = c / decisionValency;
            c          = c % decisionValency;
            int dv = c;
            return String.format("decis_{%d,%d,%d}(%d)", parent, lr, dv, m);
        }
//        Object o = rhsToC.lookupIndex(c);
//        if (o instanceof Pair<?, ?>) {                        
//            Pair<Label, String> pair = (Pair<Label, String>) o;
//            Label parent = pair.get1();
//            if (parent.equals(WallDepTreeNode.WALL_LABEL)) {
//                return String.format("root(%d)", m);
//            } else {
//                return String.format("child_{%d,%d,%d}(%d)", parent, lr, cv, m);
//            }
//        } else if (o instanceof Triple<?, ?, ?>) {
//            Triple<Label, String, Boolean> triple = (Triple<Label, String, Boolean>) o;
//            return 2;
//        }
    }
    
    private int[] getDmvIndices(int c, int m) {
        if (c < (1) + (numTags * 2 * childValency)) {
            // Root
            return new int[]{ROOT, m};
        } else if (c < (1) + (numTags * 2 * childValency)) {
            // Child
            c -= 1;
            int parent = c / (2*childValency);
            c          = c % (2*childValency);
            int lr     = c / childValency;
            c          = c % childValency;
            int cv     = c;
            return new int[]{CHILD, parent, lr, cv, m};
        } else {
            // Decision (END or CONT)
            c -= (1) + (numTags * 2 * childValency);
            int parent = c / (2 *decisionValency);
            c          = c % (2 *decisionValency);
            int lr     = c / decisionValency;
            c          = c % decisionValency;
            int dv = c;
            return new int[]{DECISION, parent, lr, dv, m};
        }
    }

    private int getCRoot() {
        return 0;
    }
    
    private int getCChild(int parent, int lr, int cv) {
        // Skip over root
        int c = 1;
        // Jump to correct spot
        c += cv;
        c += lr * childValency;
        c += parent * 2 * childValency;
        return c;
    }
    
    private int getCDecision(int parent, int lr, int dv) {
        // Skip over root and child
        int c = (1) + (numTags * 2 * childValency);
        // Jumpt to correct spot
        c += dv;
        c += lr * decisionValency;
        c += parent * 2 * decisionValency;
        return c;
    }

    public int getNumSentVars(Sentence sentence) {
        // (num root edge vars) + (num edge vars) 
        // + (num decision vars = length * (stopAdj + stopNonAdj + contAdj + contNonAdj))
        //return (sentence) + (sentence*sentence) + (sentence * 4);
        int[] tags = sentence.getLabelIds();
        int[] tagSet = new TIntHashSet(tags).toArray();
        //Arrays.sort(tagSet);
        int numSentTags = tagSet.length;
        return (numSentTags) + (numSentTags * 2 * childValency * numSentTags) + (numSentTags * 2 * decisionValency * 2);
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
        return sentCmToI.get(s).lookupIndex(i).get2();
    }

    /**
     * 
     */
    public double getMaxFreq(int s, int i) {
        return sentMaxFreq.get(s).get(i);
    }

    /**
     * Returns the frequency counts for each parameter 
     * indexed by the sentence variable indices
     * @param sentence 
     */
    public int[] getSentSol(Sentence sentence, DepTree depTree) {
        int[] tags = sentence.getLabelIds();
        int[] parents = depTree.getParents();
        
        // Get sentence solution in terms of c,m indices
        int[][] sentSolCm = new int[getNumConds()][];
        for (int c=0; c<getNumConds(); c++) {
            sentSolCm[c] = new int[getNumParams(c)];
        }
        // Count number of times tree contains c,m
        for (int cIdx=0; cIdx<parents.length; cIdx++) {
            int pIdx = parents[cIdx];
            int cTag = tags[cIdx];
            int pTag = tags[pIdx];
            if (pTag == WallDepTreeNode.WALL_POSITION) {
                int c = getCRoot();
                int m = cTag;
                sentSolCm[c][m]++;
            } else {
                int lr = cIdx < pIdx ? Constants.LEFT : Constants.RIGHT;
                int c = getCChild(pTag, lr, 0);
                int m = cTag;
                sentSolCm[c][m]++;
            }
            for (int lr=0; lr<2; lr++) {
                String lrs = lr == 0 ? "l" : "r";
                int numOnSide = depTree.getNodes().get(cIdx+1).getChildrenToSide(lrs).size();
                if (numOnSide == 0) {
                    // stopAdj
                    int c = getCDecision(cTag, lr, 0);
                    int m = Constants.END;
                    sentSolCm[c][m]++;
                } else {
                    int c;
                    int m;
                    // contAdj
                    c = getCDecision(cTag, lr, 0);
                    m = Constants.CONT;
                    sentSolCm[c][m]++;
                    // contNonAdj
                    c = getCDecision(cTag, lr, 1);
                    m = Constants.CONT;
                    sentSolCm[c][m] += numOnSide-1;
                    // stopNonAdj
                    c = getCDecision(cTag, lr, 1);
                    m = Constants.END;
                    sentSolCm[c][m]++;
                }
            }
        }
        
        // Convert c,m indices to s,i indices
    }

    public DepSentenceDist getDepSentenceDist(Sentence sentence, double[] sentParams) {
        // We map the sentence variable indices --> c,m indices --> indices of DepProbMatrix
        
        int[] tags = new int[sentence.size()];
        DepInstance depInstance = new DepInstance(tags);
        DepSentenceDist sd = null; //new DepSentenceDist();
        return sd;
    }

    private DepProbMatrix getDepProbMatrix(double[][] modelParams) {
        // TODO Auto-generated method stub
        return null;
    }
    
    public DmvModel getDmvModel(double[][] modelParams) {
        return depProbMatrixToDmvModel(getDepProbMatrix(modelParams));
    }

}
