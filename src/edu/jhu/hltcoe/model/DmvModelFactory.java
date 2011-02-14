package edu.jhu.hltcoe.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
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
    
    public interface WeightGenerator {
        double getStopWeight(Triple<Label, String, Boolean> triple);
        double[] getChooseMulti(Label parent, List<Label> children);
    }
    
    public static class RandomWeightGenerator implements WeightGenerator {

        @Override
        public double getStopWeight(Triple<Label, String, Boolean> triple) {
            return Prng.random.nextDouble();
        }
        
        @Override
        public double[] getChooseMulti(Label parent, List<Label> children) {
            return Multinomials.randomMultinomial(children.size());
        }
        
    }

    @Override
    public Model getInstance(SentenceCollection sentences) {
        DmvModel dmv = new DmvModel();
        Set<Label> vocab = getVocab(sentences);
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

        Map<Label, Set<Label>> parentChildMap = getParentChildMap(sentences);
        for (Entry<Label, Set<Label>> entry : parentChildMap.entrySet()) {
            Label parent = entry.getKey();
            List<Label> children = new ArrayList<Label>(entry.getValue());
            double[] multinomial = weightGen.getChooseMulti(parent, children);
            for (int i = 0; i < multinomial.length; i++) {
                Pair<Label, Label> pair = new Pair<Label, Label>(parent, children.get(i));
                dmv.putChooseWeight(pair, multinomial[i]);
            }
        }

        return dmv;
    }

    public static Set<Label> getVocab(SentenceCollection sentences) {
        Set<Label> vocab = new HashSet<Label>();
        for (Sentence sent : sentences) {
            for (Label label : sent) {
                vocab.add(label);
            }
        }
        return vocab;
    }

    public static Map<Label, Set<Label>> getParentChildMap(SentenceCollection sentences) {
        Map<Label, Set<Label>> map = new HashMap<Label, Set<Label>>();
        for (Sentence sent : sentences) {
            for (int i = 0; i < sent.size(); i++) {
                for (int j = 0; i < sent.size(); j++) {
                    if (j != i) {
                        Label parent = sent.get(i);
                        Label child = sent.get(j);
                        Utilities.addToSet(map, parent, child);
                    }
                }
            }
        }
        return map;
    }
}
