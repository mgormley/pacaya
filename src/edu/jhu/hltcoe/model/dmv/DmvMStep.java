package edu.jhu.hltcoe.model.dmv;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreeNode;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.math.Multinomials;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel.ChooseRhs;
import edu.jhu.hltcoe.model.dmv.DmvModel.StopRhs;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.train.MStep;
import edu.jhu.hltcoe.train.TrainCorpus;
import edu.jhu.hltcoe.util.Utilities;

public class DmvMStep implements MStep<DepTreebank> {

    private double lambda;
    
    public DmvMStep(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public Model getModel(TrainCorpus corpus, DepTreebank treebank) {
        return getModel(((DmvTrainCorpus)corpus).getVocab(), treebank);
    }
    
    public Model getModel(Set<Label> vocab, DepTreebank treebank) {
        DmvWeightGenerator weightGen = new MLDmvWeightGenerator(getChooseCounts(treebank), getStopCounts(treebank), lambda);
        DmvModelFactory dmvFactory = new DmvModelFactory(weightGen);

        SentenceCollection sentences = treebank.getSentences();
        
        DmvModel dmv = (DmvModel)dmvFactory.getInstance(vocab);
        return dmv;
    }

    private Map<StopRhs, Map<Boolean, Integer>> getStopCounts(DepTreebank treebank) {
        Map<StopRhs,Map<Boolean,Integer>> stopCounts = new HashMap<StopRhs,Map<Boolean,Integer>>();
        for (DepTree tree : treebank) {
            for (DepTreeNode parentNode : tree) {
                Label parent = parentNode.getLabel();
                for (String lr : DmvModelFactory.leftRight) {
                    List<? extends DepTreeNode> sideChildren = parentNode.getChildrenToSide(lr);
                    StopRhs triple;
                    if (sideChildren.size() == 0) {
                        triple = new StopRhs(parent, lr, true); // Adjacent
                        Utilities.increment(stopCounts, triple, true, 1); // Did stop
                    } else {
                        triple = new StopRhs(parent, lr, true); // Adjacent
                        Utilities.increment(stopCounts, triple, false, 1); // Did not stop
    
                        triple = new StopRhs(parent, lr, false); // Non-adjacent
                        Utilities.increment(stopCounts, triple, false, sideChildren.size()-1); // Did not stop
                        Utilities.increment(stopCounts, triple, true, 1); // Did stop
                    }
                }
            }
        }
        return stopCounts;
    }

    private Map<ChooseRhs, Map<Label, Integer>> getChooseCounts(DepTreebank treebank) {
        Map<ChooseRhs,Map<Label,Integer>> chooseCounts = new HashMap<ChooseRhs,Map<Label,Integer>>();
        for (DepTree tree : treebank) {
            for (DepTreeNode parentNode : tree) {
                Label parent = parentNode.getLabel();
                for (String lr : DmvModelFactory.leftRight) {
                    List<? extends DepTreeNode> sideChildren = parentNode.getChildrenToSide(lr);
                    ChooseRhs pair = new ChooseRhs(parent, lr);
                    for (DepTreeNode childNode : sideChildren) {
                        Label child = childNode.getLabel();
                        Utilities.increment(chooseCounts, pair, child, 1);
                    }
                }
            }
        }
        return chooseCounts;
    }

    public static class MLDmvWeightGenerator implements DmvWeightGenerator {
        
        private Map<ChooseRhs,Map<Label,Integer>> chooseCounts;
        private Map<StopRhs,Map<Boolean,Integer>> stopCounts;
        private double lambda;
        
        public MLDmvWeightGenerator(Map<ChooseRhs, Map<Label, Integer>> chooseCounts,
                Map<StopRhs, Map<Boolean, Integer>> stopCounts, double lambda) {
            this.chooseCounts = chooseCounts;
            this.stopCounts = stopCounts;
            this.lambda = lambda;
        }

        @Override
        public LabeledMultinomial<Label> getChooseMulti(ChooseRhs pair, List<Label> children) {
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
            return new LabeledMultinomial<Label>(children, mult);
        }

        @Override
        public double getStopWeight(StopRhs triple) {
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
                // Unless lambda equals zero
                if (lambda == 0) {
                    return 0.5;
                }
            }
            double weight = (numStop + lambda) / (numStop + numNotStop + 2*lambda);
            return weight;
        }
        
    }

}
