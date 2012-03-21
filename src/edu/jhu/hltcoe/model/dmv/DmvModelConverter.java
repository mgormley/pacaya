package edu.jhu.hltcoe.model.dmv;

import java.util.List;
import java.util.Map.Entry;

import util.Alphabet;
import depparsing.globals.Constants;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.parse.pr.DepProbMatrix;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;
import edu.jhu.hltcoe.util.Utilities;

public class DmvModelConverter {

    private DmvModelConverter() {
        // private constructor
    }
        
    public static DmvModel getDmvModel(DepProbMatrix dpm, SentenceCollection sentences) {
        DmvWeightGenerator dwg = new DpmDmvWeightGenerator(sentences.getLabelAlphabet(), dpm);
        DmvModelFactory factory = new DmvModelFactory(dwg);
        return (DmvModel)factory.getInstance(sentences);
    }

    private static final class DpmDmvWeightGenerator implements DmvWeightGenerator {
        private final Alphabet<Label> tagAlphabet;
        private final DepProbMatrix dpm;

        private DpmDmvWeightGenerator(Alphabet<Label> tagAlphabet, DepProbMatrix dpm) {
            this.tagAlphabet = tagAlphabet;
            this.dpm = dpm;
        }

        @Override
        public double getStopWeight(Triple<Label, String, Boolean> triple) {
            int p = tagAlphabet.lookupObject(triple.get1());
            int dir = triple.get2().equals("l") ? Constants.LEFT : Constants.RIGHT;
            int dv = triple.get3() ? 0 : 1; 
            return dpm.decision[p][dir][dv][Constants.END];
        }

        @Override
        public LabeledMultinomial<Label> getChooseMulti(Pair<Label, String> pair, List<Label> children) {
            LabeledMultinomial<Label> mult = new LabeledMultinomial<Label>();
            for (Label child : children) {
                int c = tagAlphabet.lookupObject(child);
                if (pair.get1().equals(WallDepTreeNode.WALL_LABEL)) {
                    mult.put(child, dpm.root[c]);
                } else {
                    int p = tagAlphabet.lookupObject(pair.get1());
                    int dir = pair.get2().equals("l") ? Constants.LEFT : Constants.RIGHT;
                    int cv = 0;
                    mult.put(child, dpm.child[c][p][dir][cv]);
                }
            }
            return mult;
        }
    }
    
    /**
     * TODO: This is an expensive conversion, and DmvModel should be rewritten
     * so that it's just some double arrays.
     */
    public static DepProbMatrix getDepProbMatrix(DmvModel model, Alphabet<Label> tagAlphabet) {
        DepProbMatrix depProbMatrix = new DepProbMatrix(tagAlphabet, 2, 1);
        depProbMatrix.fill(Double.NEGATIVE_INFINITY);
        for (Entry<Pair<Label, String>, LabeledMultinomial<Label>> entry : model.getChooseWeights().entrySet()) {
            Pair<Label, String> key = entry.getKey();
            Label parent = key.get1();
            String lr = key.get2();
            LabeledMultinomial<Label> mult = entry.getValue();
            for (Entry<Label, Double> cEntry : mult.entrySet()) {
                Label child = cEntry.getKey();
                double prob = cEntry.getValue();
    
                // We use logForIlp so that our solutions are analogous to
                // IlpViterbiParser
                double logProb = Utilities.logForIlp(prob);
    
                if (child.equals(WallDepTreeNode.WALL_LABEL)) {
                    // Skip these
                    continue;
                } else if (parent.equals(WallDepTreeNode.WALL_LABEL)) {
                    int cid = tagAlphabet.lookupObject(child);
                    depProbMatrix.root[cid] = logProb;
                } else {
                    int pid = tagAlphabet.lookupObject(parent);
                    int dir = lr.equals("l") ? Constants.LEFT : Constants.RIGHT;
                    int cid = tagAlphabet.lookupObject(child);
                    int v = 0;
                    depProbMatrix.child[cid][pid][dir][v] = logProb;
                }
            }
        }
    
        for (Entry<Triple<Label, String, Boolean>, Double> entry : model.getStopWeights().entrySet()) {
            Triple<Label, String, Boolean> key = entry.getKey();
            double stopProb = entry.getValue();
            Label parent = key.get1();
            String lr = key.get2();
            boolean adjacent = key.get3();
    
            int pid = tagAlphabet.lookupObject(parent);
            int dir = lr.equals("l") ? Constants.LEFT : Constants.RIGHT;
            // Note this is backwards from how adjacency is encoded for the ILPs
            int kids = adjacent ? 0 : 1;
    
            // We use logForIlp so that our solutions are analogous to
            // IlpViterbiParser
            double stopLogProb = Utilities.logForIlp(stopProb);
            double contLogProb = Utilities.logForIlp(1.0 - stopProb);
    
            if (!parent.equals(WallDepTreeNode.WALL_LABEL)) {
                depProbMatrix.decision[pid][dir][kids][Constants.END] = stopLogProb;
                depProbMatrix.decision[pid][dir][kids][Constants.CONT] = contLogProb;
            }
        }
    
        return depProbMatrix;
    }

}
