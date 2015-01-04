package edu.jhu.nlp.srl;

import java.io.File;

import edu.jhu.autodiff.erma.ErmaBp;
import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.gm.data.AbstractFgExampleList;
import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReader.DatasetType;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.nlp.features.TemplateSets;
import edu.jhu.nlp.srl.SrlDecoder.SrlDecoderPrm;
import edu.jhu.nlp.srl.SrlEncoder.SrlEncoderPrm;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.RoleStructure;
import edu.jhu.nlp.tag.BrownClusterTagger;
import edu.jhu.nlp.tag.BrownClusterTagger.BrownClusterTaggerPrm;
import edu.jhu.nlp.words.PrefixAnnotator;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.Threads;
import edu.jhu.util.Timer;

public class SrlSpeedTest {
    
    // TODO: Try with ErmaBpPrm.keepTape = false.
    /**
     * Speed test results.
     * 
     * t0: Reading the data.
     * t1: Build factor graph
     * t3: the feature extraction and dot product
     * t4: Run BP
     * t5: Decoding
     * 
     * Feature hash = 100,000:
     *   s=101 n=2624 tot= 161.16 t0=9864.66 t1=2716.36 t2=Infinity t3= 218.87 t4= 914.60 t5=13738.22
     * Feature hash = 10,000:
     *   (with Narad template feats)
     *   Number of model parameters: 743524
     *   s=1001 n=24334 tot= 105.76 t0=79006.49 t1=2299.78 t2=Infinity t3= 131.78 t4= 758.23 t5=10042.92
     *   
     *   (with FastSrlFeatureExtractor)
     *   Number of model parameters: 816943
     *   s=601 n=13956 tot=  38.40 t0=14583.07 t1=2459.64 t2=Infinity t3=  41.74 t4= 662.87 t5=10540.79
     *   
     *   (with McDonald template feats)
     *   Number of model parameters: 804876
     *   s=601 n=13956 tot=  28.35 t0=8998.07 t1=2298.42 t2=Infinity t3=  30.24 t4= 639.39 t5=9911.93
     *   
     *   (with Coarse1+IG templates, only 100 sents)
     *   Number of model parameters: 532769
     *   s=101 n=2624 tot=  14.15 t0=1371.67 t1=2003.05 t2=Infinity t3=  14.82 t4= 538.81 t5=9304.96
     */
    //@Test
    public void testSpeed() {
        FastMath.useLogAddTable = true;
        Threads.initDefaultPool(1);
        for (int trial = 0; trial < 2; trial++) {
            Timer t = new Timer();
            Timer t0 = new Timer();
            Timer t1 = new Timer();
            Timer t2 = new Timer();
            Timer t3 = new Timer();
            Timer t4 = new Timer();
            Timer t5 = new Timer();
            
            t.start();
            
            t0.start();
            final AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.read(AnnoSentenceReaderSpeedTest.c09Dev, DatasetType.CONLL_2009).subList(0, 103);
            PrefixAnnotator pa = new PrefixAnnotator();
            pa.annotate(sents);
            BrownClusterTagger tagger = new BrownClusterTagger(new BrownClusterTaggerPrm());
            tagger.read(new File("/Users/mgormley/research/corpora/processed/brown_clusters/bc_out_1000/full.txt_en_1000/bc/paths"));
            tagger.annotate(sents);
            t0.stop();

            // Don't time this stuff since it's "training".
            t.stop();
            final int hash = 10000;
            final CorpusStatistics cs = new CorpusStatistics(new CorpusStatisticsPrm());
            cs.init(sents);
            FactorTemplateList ftl = new FactorTemplateList();            
            ObsFeatureConjoinerPrm ofcPrm = new ObsFeatureConjoinerPrm();
            ofcPrm.featCountCutoff = 1;
            //ofcPrm.includeUnsupportedFeatures = true;
            final ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(ofcPrm, ftl);
            ofc.init(new AbstractFgExampleList(){
                @Override
                public LFgExample get(int i) {
                    return getSrlFg(sents.get(i), cs, ofc, hash);
                }
                @Override
                public int size() {
                    return sents.size();
                }
            });
            FgModel model = new FgModel(ofc.getNumParams());
            System.out.println("Number of model parameters: " + model.getNumParams());
            t.start();
            
            int s=0;
            int n=0;
    
            for (AnnoSentence sent : sents) {
                t1.start(); 
                UFgExample ex = getSrlFg(sent, cs, ofc, hash);
                t1.stop();
                
                t2.start();
                FactorGraph fg = ex.getFgLatPred();
                t2.stop();
                
                t3.start(); 
                fg.updateFromModel(model);
                t3.stop();
                
                t4.start(); 
                Pair<ErmaBp, ErmaBpPrm> pair = runBp(fg, 1);
                ErmaBp bp = pair.get1();
                t4.stop();
                
                t5.start(); 
                SrlDecoderPrm prm = new SrlDecoderPrm();
                prm.mbrPrm.infFactory = pair.get2();
                SrlDecoder decode = new SrlDecoder(prm);
                SrlGraph graph = decode.decode(bp, ex, sent);
                s += graph.getNumArgs();
                s -= graph.getNumArgs();
                t5.stop();
                
                n+=sent.size();
                if (s++%100 == 0) {
                    t.stop();
                    System.out.println(String.format("s=%d n=%d tot=%7.2f t0=%7.2f t1=%7.2f t2=%7.2f t3=%7.2f t4=%7.2f t5=%7.2f", s, n, 
                            (n/t.totSec()), 
                            (n/t0.totSec()),
                            (n/t1.totSec()),
                            (n/t2.totSec()),
                            (n/t3.totSec()),
                            (n/t4.totSec()),
                            (n/t5.totSec()))
                            );
                    t.start();
                }
            }
            t.stop();
            
            System.out.println("Total secs: " + t.totSec());
            System.out.println("Tokens / sec: " + (sents.getNumTokens() / t.totSec()));
        }
    }
    
    public static LFgExample getSrlFg(AnnoSentence sent, CorpusStatistics cs, ObsFeatureConjoiner ofc, int numParams) {        
        SrlEncoderPrm prm = new SrlEncoderPrm();
        prm.srlFePrm.featureHashMod = numParams;
        prm.srlFePrm.fePrm.useTemplates = true;
        //prm.srlFePrm.fePrm.pairTemplates = TemplateSets.getNaradowskyArgUnigramFeatureTemplates();;
        prm.srlFePrm.fePrm.pairTemplates = TemplateSets.getFromResource("/edu/jhu/nlp/features/coarse1-arg-feats-igconll09en.txt");
        prm.srlFePrm.fePrm.soloTemplates = TemplateSets.getNaradowskySenseUnigramFeatureTemplates();
        prm.srlPrm.allowPredArgSelfLoops = true;
        prm.srlPrm.binarySenseRoleFactors = true;
        prm.srlPrm.predictPredPos = true;
        prm.srlPrm.predictSense = false;
        prm.srlPrm.roleStructure = RoleStructure.ALL_PAIRS;
        prm.srlPrm.unaryFactors = true;
        SrlEncoder encode = new SrlEncoder(prm , cs, ofc);
        return encode.encode(sent, sent.getSrlGraph());
    }
    
    public static Pair<ErmaBp, ErmaBpPrm> runBp(FactorGraph fg, int numIters) {
        ErmaBpPrm bpPrm = new ErmaBpPrm();
        bpPrm.maxIterations = numIters;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        //bpPrm.s = Algebras.REAL_ALGEBRA;
        ErmaBp bp = new ErmaBp(fg, bpPrm);
        bp.run();
        for (Var v : fg.getVars()) {
            bp.getMarginals(v);
        }
        return new Pair<ErmaBp, ErmaBpPrm>(bp, bpPrm);
    }
    
//    private static class SrlFgExampleList extends AbstractFgExampleList {
//
//        private CorpusStatistics 
//        private AnnoSentenceCollection sents;
//        
//        public SrlFgExampleList(AnnoSentenceCollection sents) {
//            this.sents = sents;
//        }
//
//        @Override
//        public LFgExample get(int i) {
//            return getSrlFg(sents.get(i), cs, ofc, numParams);
//        }
//
//        @Override
//        public int size() {
//            return sents.size();
//        }
//        
//    }
    
    public static void main(String[] args) {
        (new SrlSpeedTest()).testSpeed();
    }
    
}
