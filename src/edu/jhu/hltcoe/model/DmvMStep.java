package edu.jhu.hltcoe.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreeNode;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.math.Multinomials;
import edu.jhu.hltcoe.model.DmvModelFactory.WeightGenerator;
import edu.jhu.hltcoe.train.MStep;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;
import edu.jhu.hltcoe.util.Utilities;

public class DmvMStep implements MStep<DepTreebank> {

    private double lambda;
    
    public DmvMStep(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public Model getModel(DepTreebank treebank) {
        WeightGenerator weightGen = new MLWeightGenerator(getChooseCounts(treebank), getStopCounts(treebank), lambda);
        DmvModelFactory dmvFactory = new DmvModelFactory(weightGen);

        // TODO: this is a huge waste of computation, since treebank is new each time (but kind of convenient)
        SentenceCollection sentences = treebank.getSentences();
        
        DmvModel dmv = (DmvModel)dmvFactory.getInstance(sentences);
        return dmv;
    }

    private Map<Triple<Label, String, Boolean>, Map<Boolean, Integer>> getStopCounts(DepTreebank treebank) {
        Map<Triple<Label,String,Boolean>,Map<Boolean,Integer>> stopCounts = new HashMap<Triple<Label,String,Boolean>,Map<Boolean,Integer>>();
        for (DepTree tree : treebank) {
            for (DepTreeNode parentNode : tree) {
                Label parent = parentNode.getLabel();
                for (String lr : DmvModelFactory.leftRight) {
                    List<DepTreeNode> sideChildren = parentNode.getChildrenToSide(lr);
                    Triple<Label, String, Boolean> triple;
                    if (sideChildren.size() == 0) {
                        triple = new Triple<Label, String, Boolean>(parent, lr, true); // Adjacent
                        Utilities.increment(stopCounts, triple, true, 1); // Did stop
                    } else {
                        triple = new Triple<Label, String, Boolean>(parent, lr, true); // Adjacent
                        Utilities.increment(stopCounts, triple, false, 1); // Did not stop
    
                        triple = new Triple<Label, String, Boolean>(parent, lr, false); // Non-adjacent
                        Utilities.increment(stopCounts, triple, false, sideChildren.size()-1); // Did not stop
                        Utilities.increment(stopCounts, triple, true, 1); // Did stop
                    }
                }
            }
        }
        return stopCounts;
    }

    private Map<Pair<Label, String>, Map<Label, Integer>> getChooseCounts(DepTreebank treebank) {
        Map<Pair<Label, String>,Map<Label,Integer>> chooseCounts = new HashMap<Pair<Label, String>,Map<Label,Integer>>();
        for (DepTree tree : treebank) {
            for (DepTreeNode parentNode : tree) {
                Label parent = parentNode.getLabel();
                for (String lr : DmvModelFactory.leftRight) {
                    List<DepTreeNode> sideChildren = parentNode.getChildrenToSide(lr);
                    Pair<Label, String> pair = new Pair<Label, String>(parent, lr);
                    for (DepTreeNode childNode : sideChildren) {
                        Label child = childNode.getLabel();
                        Utilities.increment(chooseCounts, pair, child, 1);
                    }
                }
            }
        }
        return chooseCounts;
    }

    public static class MLWeightGenerator implements WeightGenerator {
        
        private Map<Pair<Label, String>,Map<Label,Integer>> chooseCounts;
        private Map<Triple<Label,String,Boolean>,Map<Boolean,Integer>> stopCounts;
        private double lambda;
        
        public MLWeightGenerator(Map<Pair<Label, String>, Map<Label, Integer>> chooseCounts,
                Map<Triple<Label, String, Boolean>, Map<Boolean, Integer>> stopCounts, double lambda) {
            this.chooseCounts = chooseCounts;
            this.stopCounts = stopCounts;
            this.lambda = lambda;
        }

        @Override
        public double[] getChooseMulti(Pair<Label, String> pair, List<Label> children) {
            Map<Label,Integer> childCounts = chooseCounts.get(pair);
            double[] mult = new double[children.size()];
            for (int i=0; i<mult.length; i++) {
                int childCount;
                if (childCounts != null) {
                    childCount = Utilities.safeGet(childCounts, children.get(i));
                } else {
                    // Sometimes a label will only ever have been a leaf, so it will have no child counts
                    childCount = 0;
                }
                mult[i] = childCount + lambda;
            }
            Multinomials.normalizeProps(mult);
            return mult;
        }

        @Override
        public double getStopWeight(Triple<Label, String, Boolean> triple) {
            Map<Boolean,Integer> map = stopCounts.get(triple);
            double numStop;
            double numNotStop;
            if (map != null) {
                numStop = Utilities.safeGet(map, Boolean.TRUE);
                numNotStop = Utilities.safeGet(map, Boolean.FALSE);
            } else {
                // Sometimes we won't have observed this particular triple, so rely on smoothing.
                numStop = 0;
                numNotStop = 0;
            }
            double weight = (numStop + lambda) / (numStop + numNotStop + 2*lambda);
            return weight;
        }
        
    }

}
