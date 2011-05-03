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
import edu.jhu.hltcoe.util.Triple;
import edu.jhu.hltcoe.util.Utilities;

public class DmvMStep implements MStep<DepTreebank> {

    @Override
    public Model getModel(DepTreebank treebank) {
        WeightGenerator weightGen = new MLWeightGenerator(getChooseCounts(treebank), getStopCounts(treebank));
        DmvModelFactory dmvFactory = new DmvModelFactory(weightGen);

        // TODO: this is a huge waste of computation (but kind of convenient)
        SentenceCollection sentences = new SentenceCollection(treebank);
        
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

    private Map<Label, Map<Label, Integer>> getChooseCounts(DepTreebank treebank) {
        Map<Label,Map<Label,Integer>> chooseCounts = new HashMap<Label,Map<Label,Integer>>();
        for (DepTree tree : treebank) {
            for (DepTreeNode parentNode : tree) {
                Label parent = parentNode.getLabel();
                for (DepTreeNode childNode : parentNode.getChildren()) {
                    Label child = childNode.getLabel();
                    Utilities.increment(chooseCounts, parent, child, 1);
                }
            }
        }
        return chooseCounts;
    }

    public static class MLWeightGenerator implements WeightGenerator {
        
        private Map<Label,Map<Label,Integer>> chooseCounts;
        private Map<Triple<Label,String,Boolean>,Map<Boolean,Integer>> stopCounts;
        
        public MLWeightGenerator(Map<Label, Map<Label, Integer>> chooseCounts,
                Map<Triple<Label, String, Boolean>, Map<Boolean, Integer>> stopCounts) {
            this.chooseCounts = chooseCounts;
            this.stopCounts = stopCounts;
        }

        @Override
        public double[] getChooseMulti(Label parent, List<Label> children) {
            Map<Label,Integer> childCounts = chooseCounts.get(parent);
            double[] mult = new double[children.size()];
            for (int i=0; i<mult.length; i++) {
                mult[i] = childCounts.get(children.get(i));
            }
            Multinomials.normalizeProps(mult);
            return mult;
        }

        @Override
        public double getStopWeight(Triple<Label, String, Boolean> triple) {
            Map<Boolean,Integer> map = stopCounts.get(triple);
            double numStop = Utilities.safeGet(map, Boolean.TRUE);
            double numNotStop = Utilities.safeGet(map, Boolean.FALSE);
            double weight = numStop / (numStop + numNotStop);
            return weight;
        }
        
    }

}
