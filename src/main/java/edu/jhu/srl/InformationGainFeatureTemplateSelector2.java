package edu.jhu.srl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleListBuilder.CacheType;
import edu.jhu.gm.feat.FactorTemplate;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.matrix.DenseDoubleMatrix;
import edu.jhu.prim.sort.IntDoubleSort;
import edu.jhu.prim.util.Lambda.FnIntDoubleToDouble;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.SrlFgExamplesBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.collections.Lists;

public class InformationGainFeatureTemplateSelector2 {

    private static final Logger log = Logger.getLogger(InformationGainFeatureTemplateSelector2.class);

    public static class InformationGainFeatureTemplateSelectorPrm {
        public File outFile = null;
    }
    
    private InformationGainFeatureTemplateSelectorPrm prm;
    
    public InformationGainFeatureTemplateSelector2(InformationGainFeatureTemplateSelectorPrm prm) {
       this.prm = prm;
    }
    
    public void selectFeatureTemplates(SimpleAnnoSentenceCollection sents, SrlFgExampleBuilderPrm bPrm, CorpusStatisticsPrm csPrm) {

        //        - For each clique template C:
        //          - For each feature template T:
        //            - For each factor with clique template type C and value v:
        //              - Extract only the feature template T to get feature f.
        //              - Increment counts n(v, f).       
        List<FeatTemplate> soloUnigrams = bPrm.srlFePrm.fePrm.soloTemplates;
        List<FeatTemplate> pairUnigrams = bPrm.srlFePrm.fePrm.pairTemplates;
        
        List<FeatTemplate> allTpls = pairUnigrams;
        int numToSelect = 20;
        
        selectFeatureTemplates(sents, bPrm, csPrm, allTpls);
        
    }

    private void selectFeatureTemplates(SimpleAnnoSentenceCollection sents, SrlFgExampleBuilderPrm bPrm,
            CorpusStatisticsPrm csPrm, List<FeatTemplate> allTpls) {
        // Collapse all the SENSE_UNARY factors into one.
        // TODO: This hack doesn't work because all our lemma features have the incorrect IG.
        for (SimpleAnnoSentence sent : sents) {
            List<String> lemmas = sent.getLemmas();
            for (int i=0; i<lemmas.size(); i++) {
                lemmas.set(i, "COLLAPSED_LEMMA");
            }
        }
        
        // We MUST NOT cache the examples. They have to be recreated each time.
        bPrm.exPrm.cacheType = CacheType.NONE;
        
        // Populate the FactorTemplateList.
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        cs.init(sents);
        FactorTemplateList fts = new FactorTemplateList();
        {
            SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(bPrm, fts, cs);
            bPrm.srlFePrm.fePrm.soloTemplates = allTpls;
            bPrm.srlFePrm.fePrm.pairTemplates = allTpls;
            FgExampleList data = builder.getData(sents);
        }
        
        final double[][][] counts = getCountsArray(fts);
                
        double[][] ig = new double[fts.size()][allTpls.size()];
        for (int t=0; t<allTpls.size(); t++) {           
            DoubleArrays.fill(counts, 0.0);
            FeatTemplate tpl = allTpls.get(t);
            // TODO: Should we only use one of solo/pair at a time?
            bPrm.srlFePrm.fePrm.soloTemplates = Lists.getList(tpl);
            bPrm.srlFePrm.fePrm.pairTemplates = Lists.getList(tpl);
            SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(bPrm, fts, cs);              
            FgExampleList data = builder.getData(sents);
            for (int i=0; i<sents.size(); i++) {
                FgExample ex = data.get(i);
                for (int a=0; a<ex.getFgLatPred().getNumFactors(); a++) {
                    final int c = fts.getTemplateIdByKey(ex.getFgLatPred().getFactor(a).getTemplateKey());
                    if (c == -1) {
                        log.debug("Ignoring factor of type: " + ex.getFgLatPred().getFactor(a).getTemplateKey());
                        continue;
                    }
                    FeatureVector fv = ex.getObservationFeatures(a);
                    if (fv.size() == 0) {
                        log.debug("No " + tpl.getName() + " features fired for factor of type: " + ex.getFgLatPred().getFactor(a).getTemplateKey());
                    }
                    final int v = ex.getGoldConfigIdxPred(a);
                    fv.apply(new FnIntDoubleToDouble() {                                
                        @Override
                        public double call(int idx, double val) {
                            counts[c][v][idx] += val;
                            if (val != 0.0 && val != 1.0) {
                                log.warn("Warning non-binary features are not supported");
                            }
                            return val;
                        }
                    });
                }
            }
            
            for (int c=0; c<fts.size(); c++) {
                if (counts[c] == null) {
                    continue;
                }
                // Compute information gain for this (feature template, factor template) pair.                
                ig[c][t] = computeInformationGain(counts[c]);
            }
        }
        
        // For each clique template, select the feature templates with highest information gain.
        filterFeatTemplates(allTpls, fts, ig);
    }
    
    

    private void filterFeatTemplates(List<FeatTemplate> allTpls, FactorTemplateList fts, double[][] ig) {
        try {
            Writer writer = null;
            if (prm.outFile != null) {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(prm.outFile), "UTF-8"));
            }
            for (int c=0; c<fts.size(); c++) {            
                int[] indices = IntDoubleSort.getIntIndexArray(ig[c].length);
                double[] values = DoubleArrays.copyOf(ig[c]);
                IntDoubleSort.sortValuesDesc(values, indices);
                
                if (writer != null) {
                    for (int i=0; i<allTpls.size(); i++) {
                        int t = indices[i];
                        writer.write(String.format("IG_{%s, %s} = %f\n", fts.get(c).getKey(), allTpls.get(t).getName(), ig[c][t]));
                    }
                }
            }
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the counts array for a FactorTemplateList. Indexed by factor
     * template, factor configuration index, and feature. There should be 
     * one such count array per feature template.
     */
    private static double[][][] getCountsArray(FactorTemplateList fts) {
        // TODO: This could easily be over 3G if numFeats is 3 million.        
        //final double[][][] counts = new double[fts.size()][numVals][numFeats];
        final double[][][] counts = new double[fts.size()][][];
        for (int c=0; c<fts.size(); c++) {
            FactorTemplate ft = fts.get(c);
            int numFeats = ft.getAlphabet().size();
            int numVals = -1;
            if (numFeats > 0) {
                numVals = ft.getVars().calcNumConfigs();
                counts[c] = new double[numVals][numFeats];
            } else {
                counts[c] = null;
            }
            log.info(String.format("Factor template: c=%d key=%s numVals=%d numFeats=%d", c, ft.getKey(), numVals, numFeats));
        }
        return counts;
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
    public static double computeInformationGain(double[][] counts) {
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

}
