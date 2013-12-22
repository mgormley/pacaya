package edu.jhu.srl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.featurize.TemplateFeatureExtractor;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.featurize.TemplateSets;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.matrix.DenseDoubleMatrix;
import edu.jhu.prim.sort.IntDoubleSort;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Threads;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.hash.MurmurHash3;

public class InformationGainFeatureTemplateSelector {

    private static final Logger log = Logger.getLogger(InformationGainFeatureTemplateSelector.class);

    public static class InformationGainFeatureTemplateSelectorPrm {
        /** The number of features to select. */
        public int numToSelect = 25;
        /** The value of the mod for use in the feature hashing trick. If <= 0, feature-hashing will be disabled. */
        public int featureHashMod = -1;
        /** The output file for information gains of all features. */
        public File outFile = new File("./ig-out.txt");
        /** Number of threads. */
        public int numThreads = 1;
    }
    
    private InformationGainFeatureTemplateSelectorPrm prm;
    private ExecutorService pool;

    public InformationGainFeatureTemplateSelector(InformationGainFeatureTemplateSelectorPrm prm) {
       this.prm = prm;
       this.pool = Executors.newFixedThreadPool(prm.numThreads);
    }
    
    public static class SrlFeatTemplates {
        public List<FeatTemplate> srlSenseTemplates;
        public List<FeatTemplate> srlArgTemplates;
        public List<FeatTemplate> depParseTemplates;
        public SrlFeatTemplates(List<FeatTemplate> srlSenseTemplates, List<FeatTemplate> srlArgTemplates,
                List<FeatTemplate> depParseTemplates) {
            this.srlSenseTemplates = srlSenseTemplates;
            this.srlArgTemplates = srlArgTemplates;
            this.depParseTemplates = depParseTemplates;
        }
    }
    
    public SrlFeatTemplates getFeatTemplatesForSrl(SimpleAnnoSentenceCollection sents, 
            CorpusStatisticsPrm csPrm) {
        List<ValExtractor> valExts = (List) Lists.getList(new SrlArgExtractor(), new SrlSenseExtractor(), new UnlabeledDepParseExtractor());
        List<FeatTemplate> srlSense = getFeatTemplatesForSrl(sents, csPrm, TemplateSets.getBjorkelundSenseUnigramFeatureTemplates(), new SrlSenseExtractor());
        List<FeatTemplate> srlArg = getFeatTemplatesForSrl(sents, csPrm, TemplateSets.getBjorkelundArgUnigramFeatureTemplates(), new SrlArgExtractor());
        return new SrlFeatTemplates(srlSense, srlArg, null);
    }
    
    private List<FeatTemplate> getFeatTemplatesForSrl(SimpleAnnoSentenceCollection sents, CorpusStatisticsPrm csPrm,
            List<FeatTemplate> unigrams, ValExtractor valExt) {
        List<FeatTemplate> bigrams = TemplateSets.getBigramFeatureTemplates(unigrams);
        bigrams = selectFeatureTemplates(bigrams, Lists.getList(valExt), sents, csPrm).get(0);
        List<FeatTemplate> all = new ArrayList<FeatTemplate>();
        all.addAll(unigrams);
        all.addAll(bigrams);
        return all;
    }

    public List<List<FeatTemplate>> selectFeatureTemplates(List<FeatTemplate> allTpls, List<ValExtractor> valExts, SimpleAnnoSentenceCollection sents, 
            CorpusStatisticsPrm csPrm) {      
        // Initialize.
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(sents);

        for (int c = 0; c < valExts.size(); c++) {
            ValExtractor valExt = valExts.get(c);
            valExt.init(sents);
            log.info(String.format("Value Extractor: c=%d name=%s numVals=%d", c, valExt.getName(), valExt.getNumVals()));
        }
                
        // Compute information gain.
        double[][] ig = computeInformationGain(allTpls, valExts, sents, cs);
                
        // Select feature templates.
        try {
            Writer writer = null;
            if (prm.outFile != null) {
                log.info("Writing information gain for feature templates (append mode) to: " + prm.outFile);
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prm.outFile, true), "UTF-8"));
            }
            
            // For each clique template, select the feature templates with highest information gain.
            List<List<FeatTemplate>> selected = new ArrayList<List<FeatTemplate>>();
            for (int c=0; c<valExts.size(); c++) {            
                selected.add(filterFeatTemplates(allTpls, valExts.get(c), ig[c], writer));
            }
            
            if (writer != null) {
                writer.close();
            }
            return selected;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private double[][] computeInformationGain(List<FeatTemplate> allTpls, List<ValExtractor> valExts,
            SimpleAnnoSentenceCollection sents, CorpusStatistics cs) {
        double[][] ig = new double[valExts.size()][allTpls.size()];
        List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
        for (int t=0; t<allTpls.size(); t++) {
            //computeInformationGain(t, allTpls, valExts, sents, cs, ig);
            tasks.add(new IGComputer(t, allTpls, valExts, sents, cs, ig));
        }
        Threads.invokeAndAwaitAll(pool, tasks);
        return ig;
    }
    
    private class IGComputer implements Callable<Object> {
        int t; List<FeatTemplate> allTpls; List<ValExtractor> valExts;
        SimpleAnnoSentenceCollection sents; CorpusStatistics cs; double[][] ig;
        public IGComputer(int t, List<FeatTemplate> allTpls, List<ValExtractor> valExts,
                SimpleAnnoSentenceCollection sents, CorpusStatistics cs, double[][] ig) {
            super();
            this.t = t;
            this.allTpls = allTpls;
            this.valExts = valExts;
            this.sents = sents;
            this.cs = cs;
            this.ig = ig;
        }
        @Override
        public Object call() throws Exception {
            computeInformationGain(t, allTpls, valExts, sents, cs, ig);
            return null;
        }
    }

    private void computeInformationGain(int t, List<FeatTemplate> allTpls, List<ValExtractor> valExts,
            SimpleAnnoSentenceCollection sents, CorpusStatistics cs, double[][] ig) {
        FeatTemplate tpl = allTpls.get(t);
        if (t % 10 == 0) {
            log.debug(String.format("Processing feature template %d of %d: %s", t, allTpls.size(), tpl.getName()));
        }
        // TODO: parallelize
        final IntDoubleDenseVector[][] counts = getCountsArray(valExts);
        //DoubleArrays.fill(counts, 0.0);
        Alphabet<String> alphabet = new Alphabet<String>();
        for (int i=0; i<sents.size(); i++) {                
            SimpleAnnoSentence sent = sents.get(i);
            TemplateFeatureExtractor featExt = new TemplateFeatureExtractor(sent, cs);

            for (int pidx=-1; pidx<sent.size(); pidx++) {
                for (int cidx=-1; cidx<sent.size(); cidx++) {
                    
                    // Feature Extraction.
                    List<String> feats = new ArrayList<String>();
                    featExt.addFeatures(Lists.getList(tpl), pidx, cidx, feats);
                    if (feats.size() == 0) {
                        log.warn("No features extractor for template: " + tpl.getName());
                    }
                    FeatureVector fv = new FeatureVector();
                    for (int j=0; j<feats.size(); j++) {                                
                        String featName = feats.get(j);
                        int featIdx;
                        if (prm.featureHashMod > 0) {
                            String data = featName;
                            featIdx = FastMath.mod(MurmurHash3.murmurhash3_x86_32(data, 0, data.length(), 123456789), prm.featureHashMod);
                        } else {
                            featIdx = alphabet.lookupIndex(featName);
                        }
                        fv.add(featIdx, 1.0);
                    }
                    
                    // For each value extractor:
                    for (int c = 0; c < valExts.size(); c++) {
                        // Value Extraction.
                        ValExtractor valExt = valExts.get(c);
                        final int valIdx = valExt.getValIdx(sent, pidx, cidx);
                        
                        if (valIdx != -1) {
                            // Increment counts of feature and value occurrences.
                            counts[c][valIdx].add(fv);
                        }
                    }
                }
            }
        }
        
        for (int c = 0; c < valExts.size(); c++) {
            // Compute information gain for this (feature template, value extractor) pair.                
            ig[c][t] = computeInformationGain(counts[c]);
        }
    }
    
    /**
     * Computes the information gain for an array of empirical counts for occurrences of v and f.
     * 
     * IG = sum_f sum_v p(v,f) log_2( p(v,f) / (p(f) p(v)) ) 
     * 
     * where all probabilities are their empirical distributions.
     * 
     * @param counts Count array indexed by v and f.
     * @return The information gain, IG.
     */
    public static double computeInformationGain(IntDoubleDenseVector[] counts) {
        double curIg = 0.0;
        final DenseDoubleMatrix cm = new DenseDoubleMatrix(counts);
        double[] vCounts = cm.getRowSums();
        double[] fCounts = cm.getColSums();
        double total = DoubleArrays.sum(vCounts);
        for (int v=0; v<cm.getNumRows(); v++) {
            for (int f=0; f<cm.getNumColumns(); f++) {
                if (cm.get(v,f) == 0) {
                    continue;
                }
                curIg += cm.get(v,f) / total * FastMath.log2(cm.get(v,f)/total * total / vCounts[v] * total / fCounts[f]);
                assert !Double.isInfinite(curIg);
                assert !Double.isNaN(curIg);
            }
        }
        return curIg;
    }

    /**
     * Select prm.numSelect feature templates for the given ValExtractor.
     * Write all information gain values to writer if given.
     */
    private List<FeatTemplate> filterFeatTemplates(List<FeatTemplate> allTpls, ValExtractor valExt, double[] ig, Writer writer) throws IOException {            
        int[] indices = IntDoubleSort.getIntIndexArray(ig.length);
        double[] values = DoubleArrays.copyOf(ig);
        IntDoubleSort.sortValuesDesc(values, indices);
        
        if (writer != null) {
            for (int i=0; i<allTpls.size(); i++) {
                int t = indices[i];
                String selected = (i < prm.numToSelect) ? "Y" : "N";
                writer.write(String.format("%s IG_{%s, %s} = %f\n", selected, valExt.getName(), allTpls.get(t).getName(), ig[t]));
            }
            writer.write("\n");
        }
        
        // Select feature templates.
        List<FeatTemplate> selected = new ArrayList<FeatTemplate>();
        for (int t=0; t<prm.numToSelect; t++) {
            selected.add(allTpls.get(indices[t]));
        }
        return selected;
    }

    /**
     * Gets the counts array for a FactorTemplateList. Indexed by factor
     * template, factor configuration index, and feature. There should be 
     * one such count array per feature template.
     */
    private IntDoubleDenseVector[][] getCountsArray(List<ValExtractor> valExts) {
        // TODO: This could easily be over 3G if numFeats is 3 million.
        // Maybe we should represent these counts sparsely.
        final IntDoubleDenseVector[][] counts = new IntDoubleDenseVector[valExts.size()][];
        for (int c=0; c<valExts.size(); c++) {
            ValExtractor valExt = valExts.get(c);
            int numVals = valExt.getNumVals();
            counts[c] = new IntDoubleDenseVector[numVals];
            for (int v=0; v<numVals; v++) {
                int size = (prm.featureHashMod > 0) ? prm.featureHashMod : 1024;
                counts[c][v] = new IntDoubleDenseVector(size);
            }
        }
        return counts;
    }

    public void shutdown() {
        Threads.shutdownSafelyOrDie(pool);
    }
   
    public interface ValExtractor {
        public void init(SimpleAnnoSentenceCollection sents);
        public Object getName();
        public int getNumVals();
        public int getValIdx(SimpleAnnoSentence sent, int pidx, int cidx);
    }
    
    public static abstract class AbtractValExtractor implements ValExtractor {
        
        private Alphabet<Object> valAlphabet = new Alphabet<Object>();
        
        @Override
        public void init(SimpleAnnoSentenceCollection sents) {
            for (int i=0; i<sents.size(); i++) {                
                SimpleAnnoSentence sent = sents.get(i);
                for (int pidx=-1; pidx<sent.size(); pidx++) {
                    for (int cidx=-1; cidx<sent.size(); cidx++) {
                        this.getValIdx(sent, pidx, cidx);
                    }
                }
            }
            valAlphabet.stopGrowth();
        }
        
        public int getValIdx(SimpleAnnoSentence sent, int pidx, int cidx) {
            Object val = getVal(sent, pidx, cidx);
            if (val == null) {
                return -1;
            } else {
                return valAlphabet.lookupIndex(val);
            }
        }
        
        public int getNumVals() {
            return valAlphabet.size();
        }

        public abstract Object getVal(SimpleAnnoSentence sent, int pidx, int cidx);

    }
    
    public static class SrlArgExtractor extends AbtractValExtractor {

        @Override
        public Object getName() {
            return "SrlArgExtractor";
        }

        @Override
        public Object getVal(SimpleAnnoSentence sent, int pidx, int aidx) {
            if (aidx == -1 || pidx == -1) {
                return null;
            }
            SrlEdge edge = sent.getSrlGraph().getEdge(pidx, aidx);
            if (edge == null) {
                return null;
            } else {
                return edge.getLabel();
            }
        }
        
    }
    
    public static class SrlSenseExtractor extends AbtractValExtractor {

        @Override
        public Object getName() {
            return "SrlSenseExtractor";
        }

        @Override
        public Object getVal(SimpleAnnoSentence sent, int pidx, int aidx) {
            if (aidx == -1 && pidx != -1) {
                SrlPred pred = sent.getSrlGraph().getPredAt(pidx);
                if (pred != null) {
                    return pred.getLabel();
                }
            }
            return null;
        }
        
    }

    public static class UnlabeledDepParseExtractor extends AbtractValExtractor {

        @Override
        public Object getName() {
            return "UnlabeledDepParseExtractor";
        }

        @Override
        public Object getVal(SimpleAnnoSentence sent, int pidx, int aidx) {
            if (aidx == -1) {
                return null;
            }
            return sent.getParent(aidx) == pidx;
        }
        
    }
    
}
