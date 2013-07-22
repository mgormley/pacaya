package edu.jhu.util.math;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import edu.jhu.util.Prng;

public class LabeledMultinomial<T> extends TreeMap<T, Double> implements Map<T, Double> {

    private static final long serialVersionUID = 7106636199881587459L;

    public LabeledMultinomial() {
        super();
    }

    public LabeledMultinomial(LabeledMultinomial<T> multi) {
        super(multi);
    }

    public LabeledMultinomial(List<T> children, double[] chooseMulti) {
        this();
        put(children, chooseMulti);
    }

    public void put(List<T> vocabList, double[] multinomial) {
        if (vocabList.size() != multinomial.length) {
            throw new IllegalArgumentException("vocabList.size() != multinomial.length");
        }
        for (int i = 0; i < multinomial.length; i++) {
            this.put(vocabList.get(i), multinomial[i]);
        }
    }

    public T sampleFromMultinomial() {
        return sampleFromMultinomial(Prng.javaRandom);
    }

    public T sampleFromMultinomial(Random random) {
        double rand = random.nextDouble();
        double sum = 0.0;
        for (Entry<T, Double> entry : this.entrySet()) {
            sum += entry.getValue();
            if (rand <= sum) {
                return entry.getKey();
            }
        }
        throw new RuntimeException("Multinomial doesn't sum to 1.0: " + this);
    }

}
