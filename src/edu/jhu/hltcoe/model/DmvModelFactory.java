package edu.jhu.hltcoe.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.math.Multinomials;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Triple;
import edu.jhu.hltcoe.util.Utilities;

public class DmvModelFactory implements ModelFactory {

    public static final String[] leftRight = new String[] { "l", "r" };
    private static final boolean[] adjacent = new boolean[] { true, false };
    private WeightGenerator weightGen;
    
    public DmvModelFactory(WeightGenerator weightGen) {
        this.weightGen = weightGen;
    }

    @Override
    public Model getInstance(SentenceCollection sentences) {
        // TODO: we waste effort on parameters that cannot ever appear
        // in the corpus.
        
        DmvModel dmv = new DmvModel();
        Set<Label> vocab = sentences.getVocab();
        for (Label label : vocab) {
            for (String lr : leftRight) {
                for (boolean adj : adjacent) {
                    Triple<Label, String, Boolean> triple = new Triple<Label, String, Boolean>(label, lr, adj);
                    double weight;
                    weight = weightGen.getStopWeight(triple);
                    dmv.putStopWeight(triple, weight);
                }
            }
        }
        // OLD WAY used a parentChildMap
//        Map<Label, Set<Label>> parentChildMap = getParentChildMap(sentences);
//        for (Entry<Label, Set<Label>> entry : parentChildMap.entrySet()) {
//            Label parent = entry.getKey();
//            List<Label> children = new ArrayList<Label>(entry.getValue());
        // TODO: This is slow making a list like this
        List<Label> vocabList = new ArrayList<Label>(vocab);
        for (Label parent : vocabList) {
            for (String lr : leftRight) {
                Pair<Label,String> pair = new Pair<Label,String>(parent, lr);
                double[] multinomial = weightGen.getChooseMulti(pair, vocabList);
                for (int i = 0; i < multinomial.length; i++) {
                    Triple<Label, String, Label> triple = new Triple<Label, String, Label>(parent, lr, vocabList.get(i));
                    dmv.putChooseWeight(triple, multinomial[i]);
                }
            }
        }

        return dmv;
    }

    public static Map<Label, Set<Label>> getParentChildMap(SentenceCollection sentences) {
        Map<Label, Set<Label>> map = new HashMap<Label, Set<Label>>();
        for (Sentence sent : sentences) {
            for (int i = 0; i < sent.size(); i++) {
                Label parent = sent.get(i);
                for (int j = 0; j < sent.size(); j++) {
                    if (j != i) {
                        Label child = sent.get(j);
                        Utilities.addToSet(map, parent, child);
                    }
                }
                // Special case for Wall
                Utilities.addToSet(map, WallDepTreeNode.WALL_LABEL, parent);
                Utilities.addToSet(map, parent, WallDepTreeNode.WALL_LABEL);
            }
        }
        return map;
    }
        
    public interface WeightGenerator {
        double getStopWeight(Triple<Label, String, Boolean> triple);
        double[] getChooseMulti(Pair<Label, String> pair, List<Label> children);
    }
    
    public static class RandomWeightGenerator implements WeightGenerator {

        private double lambda;

        public RandomWeightGenerator(double lambda) {
            this.lambda = lambda;
        }
        
        @Override
        public double getStopWeight(Triple<Label, String, Boolean> triple) {
            double stop = 0.0;
            while (stop == 0.0) {
                stop = Prng.random.nextDouble();
            }
            return stop;
        }
        
        @Override
        public double[] getChooseMulti(Pair<Label, String> pair, List<Label> children) {
            // TODO: these should be randomly generated from a prior
            double[] chooseMulti = Multinomials.randomMultinomial(children.size());
            for (int i=0; i<chooseMulti.length; i++) {
                chooseMulti[i] += lambda;
            }
            Multinomials.normalizeProps(chooseMulti);
            return chooseMulti;
        }
        
    }
    
}
