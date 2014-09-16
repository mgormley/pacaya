package edu.jhu.nlp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.data.conll.SrlGraph.SrlEdge;
import edu.jhu.nlp.data.conll.SrlGraph.SrlPred;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.LocalObservations;
import edu.jhu.nlp.features.TemplateFeatureExtractor;
import edu.jhu.nlp.features.TemplateSets;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate0;
import edu.jhu.nlp.features.TemplateLanguage.OtherFeat;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.matrix.DenseDoubleMatrix;
import edu.jhu.prim.sort.IntDoubleSort;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.prim.vector.IntDoubleDenseVector;
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
        /** The value of the mod for use in the value hashing trick. If <= 0, feature-hashing will be disabled. */
        public int valueHashMod = 50;
        /** The output file for information gains of all features. */
        public File outFile = new File("./ig-out.txt");
        /** Number of threads. */
        public int numThreads = 1;
        /** Maximum number of sentences over which to evaluate information gain. */
        public int maxNumSentences = Integer.MAX_VALUE;
        /** Whether to do feature selection for sense features. */
        public boolean selectSense = true;
    }
    
    private InformationGainFeatureTemplateSelectorPrm prm;
    private ExecutorService pool;
    private Set<FeatTemplate> tplsWithNoFeats = new HashSet<FeatTemplate>();

    public InformationGainFeatureTemplateSelector(InformationGainFeatureTemplateSelectorPrm prm) {
       this.prm = prm;
       this.pool = Executors.newFixedThreadPool(prm.numThreads);
       log.debug("Num threads: " + prm.numThreads);
    }
    
    public static class SrlFeatTemplates {
        public List<FeatTemplate> srlSense;
        public List<FeatTemplate> srlArg;
        public List<FeatTemplate> depParse;
        public SrlFeatTemplates(List<FeatTemplate> srlSense, List<FeatTemplate> srlArg,
                List<FeatTemplate> depParse) {
            this.srlSense = srlSense;
            this.srlArg = srlArg;
            this.depParse = depParse;
        }
    }
    
    public SrlFeatTemplates getFeatTemplatesForSrl(AnnoSentenceCollection sents, 
            CorpusStatisticsPrm csPrm, SrlFeatTemplates sft) {
        List<FeatTemplate> srlSense = prm.selectSense ? getFeatTemplatesForSrl(sents, csPrm, sft.srlSense, new SrlSenseExtractor()) : sft.srlSense;
        List<FeatTemplate> srlArg = getFeatTemplatesForSrl(sents, csPrm, sft.srlArg, new SrlArgExtractor());
        // Note we do NOT do feature selection on the dependency parsing templates. 
        return new SrlFeatTemplates(srlSense, srlArg, sft.depParse);
    }
    
    private List<FeatTemplate> getFeatTemplatesForSrl(AnnoSentenceCollection sents, CorpusStatisticsPrm csPrm,
            List<FeatTemplate> unigrams, ValExtractor valExt) {
        int numUni = 45;
        List<FeatTemplate> selUnigrams = selectFeatureTemplates(unigrams, Lists.getList(valExt), sents, csPrm, numUni).get(0);
        // Don't include PathGrams feature.
        boolean removedPathGrams = selUnigrams.remove(new FeatTemplate0(OtherFeat.PATH_GRAMS));
        if (removedPathGrams) { log.debug("Not allowing PathGrams feature in feature bigrams."); }
        assert selUnigrams.size() <= numUni : "selUnigrams.size(): " + selUnigrams.size();
        List<FeatTemplate> bigrams = TemplateSets.getBigramFeatureTemplates(selUnigrams);
        assert bigrams.size() <= numUni*(numUni-1.0)/2.0;
        bigrams = selectFeatureTemplates(bigrams, Lists.getList(valExt), sents, csPrm, prm.numToSelect).get(0);
        // Add ALL unigrams and the selected bigrams.
        List<FeatTemplate> all = new ArrayList<FeatTemplate>();
        all.addAll(unigrams);
        all.addAll(bigrams);
        return all;
    }


//    public List<List<FeatTemplate>> selectFeatureTemplates(List<FeatTemplate> allTpls, List<ValExtractor> valExts, AnnoSentenceCollection sents, 
//            CorpusStatisticsPrm csPrm) {  
//        return selectFeatureTemplates(allTpls, valExts, sents, csPrm, prm.numToSelect);
//    }
    
    public List<List<FeatTemplate>> selectFeatureTemplates(List<FeatTemplate> allTpls, List<ValExtractor> valExts, AnnoSentenceCollection sents, 
            CorpusStatisticsPrm csPrm, int numToSelect) {      
        if (allTpls.size() <= numToSelect) {
            List<List<FeatTemplate>> selected = new ArrayList<List<FeatTemplate>>();
            for (int c=0; c<valExts.size(); c++) {            
                selected.add(allTpls);
            }
            return selected;
        }

        // Subselect the sentences.
        if (prm.maxNumSentences < sents.size()) {
            log.info("Using only the first "+prm.maxNumSentences+" sentences for information gain calculations.");
            sents = sents.subList(0, prm.maxNumSentences);
        }
        
        // Initialize.
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(sents);

        for (int c = 0; c < valExts.size(); c++) {
            ValExtractor valExt = valExts.get(c);
            valExt.init(sents, prm.valueHashMod);
            log.info(String.format("Value Extractor: c=%d name=%s numVals=%d", c, valExt.getName(), valExt.getNumVals()));
        }
                
        // Compute information gain.
        log.info("Computing information gain for feature templates.");
        Pair<double[][], int[]> pair = computeInformationGain(allTpls, valExts, sents, cs);
        double[][] ig = pair.get1();
        int[] featCount = pair.get2();
                
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
                selected.add(filterFeatTemplates(allTpls, valExts.get(c), ig[c], featCount, writer, numToSelect));
            }
            
            if (writer != null) {
                writer.close();
            }
            return selected;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Pair<double[][], int[]> computeInformationGain(List<FeatTemplate> allTpls, List<ValExtractor> valExts,
            AnnoSentenceCollection sents, CorpusStatistics cs) {
        // Information gain, indexed by ValExtractor index, and template index.
        double[][] ig = new double[valExts.size()][allTpls.size()];
        // Feature count for each template.
        int[] featCount = new int[allTpls.size()];
        if (prm.numThreads == 1) { 
            for (int t=0; t<allTpls.size(); t++) {
                computeInformationGain(t, allTpls, valExts, sents, cs, ig, featCount);
            }
        } else {
            List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
            for (int t=0; t<allTpls.size(); t++) {
                tasks.add(new IGComputer(t, allTpls, valExts, sents, cs, ig, featCount));
            }
            Threads.invokeAndAwaitAll(pool, tasks);
        }
        return new Pair<double[][], int[]>(ig, featCount);
    }
    
    private class IGComputer implements Callable<Object> {
        int t; List<FeatTemplate> allTpls; List<ValExtractor> valExts;
        AnnoSentenceCollection sents; CorpusStatistics cs; double[][] ig;
        int[] featCount;
        public IGComputer(int t, List<FeatTemplate> allTpls, List<ValExtractor> valExts,
                AnnoSentenceCollection sents, CorpusStatistics cs, double[][] ig, 
                int[] featCount) {
            super();
            this.t = t;
            this.allTpls = allTpls;
            this.valExts = valExts;
            this.sents = sents;
            this.cs = cs;
            this.ig = ig;
            this.featCount = featCount;
        }
        @Override
        public Object call() throws Exception {
            computeInformationGain(t, allTpls, valExts, sents, cs, ig, featCount);
            return null;
        }
    }

    private void computeInformationGain(int t, List<FeatTemplate> allTpls, List<ValExtractor> valExts,
            AnnoSentenceCollection sents, CorpusStatistics cs, double[][] ig, int[] featCount) {
        FeatTemplate tpl = allTpls.get(t);

        final IntDoubleDenseVector[][] counts = getCountsArray(valExts);
        Alphabet<String> alphabet = new Alphabet<String>();
        for (int i=0; i<sents.size(); i++) {                
            AnnoSentence sent = sents.get(i);
            TemplateFeatureExtractor featExt = new TemplateFeatureExtractor(sent, cs);

            for (int pidx=-1; pidx<sent.size(); pidx++) {
                for (int cidx=-1; cidx<sent.size(); cidx++) {
                    
                    // Feature Extraction.
                    List<String> feats = new ArrayList<String>();
                    featExt.addFeatures(Lists.getList(tpl), LocalObservations.newPidxCidx(pidx, cidx), feats);
                    if (feats.size() == 0) {
                        if (!tplsWithNoFeats.contains(tpl)) {
                            log.warn("No features extracted for template: " + tpl.getName());
                            tplsWithNoFeats.add(tpl);
                        }
                    }
                    FeatureVector fv = new FeatureVector();
                    for (int j=0; j<feats.size(); j++) {                                
                        String featName = feats.get(j);
                        int featIdx;
                        if (prm.featureHashMod > 0) {
                            String data = featName;
                            featIdx = FastMath.mod(MurmurHash3.murmurhash3_x86_32(data, 0, data.length(), 123456789), prm.featureHashMod);
                            featIdx = alphabet.lookupIndex(Integer.toString(featIdx));
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
        featCount[t] = alphabet.size();
        
        if (t % 10 == 0) {
            log.debug(String.format("Processed feature template %d of %d: %s #feats=%d", t, allTpls.size(), tpl.getName(), alphabet.size()));
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
     * @param featCount 
     */
    private List<FeatTemplate> filterFeatTemplates(List<FeatTemplate> allTpls, ValExtractor valExt, double[] ig, int[] featCount, Writer writer, int numToSelect) throws IOException {            
        int[] indices = IntDoubleSort.getIntIndexArray(ig.length);
        double[] values = DoubleArrays.copyOf(ig);
        IntDoubleSort.sortValuesDesc(values, indices);
        
        if (writer != null) {
            for (int i=0; i<allTpls.size(); i++) {
                int t = indices[i];
                String selected = (i < numToSelect) ? "Y" : "N";
                writer.write(String.format("%s\t%s\t%s\t%d\t%f\n", selected, valExt.getName(), allTpls.get(t).getName(), featCount[t], ig[t]));
            }
            writer.write("\n");
        }
        
        // Select feature templates.
        List<FeatTemplate> selected = new ArrayList<FeatTemplate>();
        for (int t=0; t<Math.min(numToSelect, indices.length); t++) {
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
                // TODO: Remove.
                //int size = (prm.featureHashMod > 0) ? prm.featureHashMod : 1024;
                counts[c][v] = new IntDoubleDenseVector();
            }
        }
        return counts;
    }

    public void shutdown() {
        Threads.shutdownSafelyOrDie(pool);
    }
   
    public interface ValExtractor {
        public void init(AnnoSentenceCollection sents, int valueHashMod);
        public Object getName();
        public int getNumVals();
        public int getValIdx(AnnoSentence sent, int pidx, int cidx);
    }
    
    public static abstract class AbtractValExtractor implements ValExtractor {
        
        private Alphabet<Object> valAlphabet = new Alphabet<Object>();
        private int valueHashMod = -1;
        
        @Override
        public void init(AnnoSentenceCollection sents, int valueHashMod) {
            this.valueHashMod = valueHashMod;
            for (int i=0; i<sents.size(); i++) {                
                AnnoSentence sent = sents.get(i);
                for (int pidx=-1; pidx<sent.size(); pidx++) {
                    for (int cidx=-1; cidx<sent.size(); cidx++) {
                        this.getValIdx(sent, pidx, cidx);
                    }
                }
            }
            valAlphabet.stopGrowth();
        }
        
        public int getValIdx(AnnoSentence sent, int pidx, int cidx) {
            String val = getVal(sent, pidx, cidx);
            if (val == null) {
                return -1;
            } else {
                String data = val;
                if (valueHashMod > 0) {
                    int idx = FastMath.mod(MurmurHash3.murmurhash3_x86_32(data, 0, data.length(), 123456789), valueHashMod);
                    return valAlphabet.lookupIndex(idx);
                } else {
                    return valAlphabet.lookupIndex(val);
                }
            }
        }
        
        public int getNumVals() {
            return valAlphabet.size();
        }

        public abstract String getVal(AnnoSentence sent, int pidx, int cidx);

    }
    
    public static class SrlArgExtractor extends AbtractValExtractor {

        @Override
        public Object getName() {
            return "SrlArgExtractor";
        }

        @Override
        public String getVal(AnnoSentence sent, int pidx, int aidx) {
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
        public String getVal(AnnoSentence sent, int pidx, int aidx) {
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
        public String getVal(AnnoSentence sent, int pidx, int aidx) {
            if (aidx == -1) {
                return null;
            }
            return sent.getParent(aidx) == pidx ? "1" : "0";
        }
        
    }
    
}
