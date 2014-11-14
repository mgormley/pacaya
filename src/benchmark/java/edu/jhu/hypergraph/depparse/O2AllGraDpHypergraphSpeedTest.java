package edu.jhu.hypergraph.depparse;

import java.util.ArrayList;

import org.junit.Test;

import edu.jhu.hypergraph.Hyperalgo;
import edu.jhu.hypergraph.Hyperalgo.Scores;
import edu.jhu.hypergraph.Hyperedge;
import edu.jhu.hypergraph.Hypergraph;
import edu.jhu.hypergraph.Hypergraph.HyperedgeFn;
import edu.jhu.hypergraph.ListHypergraph;
import edu.jhu.hypergraph.WeightedHyperedge;
import edu.jhu.hypergraph.depparse.O2AllGraDpHypergraphTest.ExplicitDependencyScorer;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.util.Timer;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;

public class O2AllGraDpHypergraphSpeedTest {

    /**
     * Comparison of bottom-up for-loops for hyperedge enumeration vs. building a hypergraph edge
     * list explicitly in memory.
     * 
     * Iteration only:
     * opt = 0: s=101 n=2309 Toks / sec: 81.73451327433628
     * opt = 1: s=2401 n=56427 Toks / sec: 6153.435114503817
     * opt = 2: s=1001 n=24623 Toks / sec: 1149.9089338252463
     *          s=1001 n=24623 Toks / sec: 1430.488584209609 << after switching to ensureTailNodeSize() in Hyperedge.
     * opt = 2 (without virtual call): s=1001 n=24623 Toks / sec: 1139.057223481519
     * 
     * With inside algorithm:
     * opt = 0: s=101 n=2309 Toks / sec: 69.59850494333253
     * opt = 1: s=101 n=2309 Toks / sec: 381.4637369899224
     * opt = 2: s=101 n=2309 Toks / sec: 297.62825470482085
     *          (final Tokens / sec: 270.1231861612142)
     */
    @Test
    public void testSpeed() {
        final int opt = 2;
        Algebra a = Algebras.LOG_SEMIRING;
        AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.readPtbYmConllx();
        
        Timer t = new Timer();
        t.start();
        int s=0;
        int n=0;
        for (AnnoSentence sent : sents) {
            int nn = sent.size();
            ExplicitDependencyScorer scorer = new ExplicitDependencyScorer(new double[nn+1][nn+1][nn+1], nn);
            Hypergraph hg;
            O2AllGraDpHypergraph o1hg = new O2AllGraDpHypergraph(scorer, a, false);
            if (opt==0) {
                final ArrayList<Hyperedge> topo = new ArrayList<>();
                o1hg.applyTopoSort(new HyperedgeFn() {                
                    @Override
                    public void apply(Hyperedge e) {
                        WeightedHyperedge we = (WeightedHyperedge) e;
                        topo.add(new WeightedHyperedge(we.getId(), we.getLabel(), we.getWeight(), we.getHeadNode(), we.getTailNodes()));
                    }
                });
                ListHypergraph lhg = new ListHypergraph(o1hg.getRoot(), o1hg.getNodes(), topo);
                hg = lhg;                
            } else if (opt == 1) {
                hg = o1hg;
            }  else if (opt == 2) {
                hg = o1hg;
                // Just iterate over the hyperedges.
                o1hg.applyTopoSort(new HyperedgeFn() {                    
                    @Override
                    public void apply(Hyperedge e) { }
                });
            }
            //Scores scores = new Scores();
            //Hyperalgo.insideAlgorithm(hg, o1hg.getPotentials(), a, scores);
            //Hyperalgo.outsideAlgorithm(hg, o1hg.getPotentials(), a, scores);
            //Hyperalgo.marginals(hg, o1hg.getPotentials(), a, scores);
            
            n+=sent.size();
            if (s++%100 == 0) {
                System.out.println("s="+s+" n=" + n + " Toks / sec: " + (n / t.totSec())); 
            }
        }
        t.stop();
        
        System.out.println("Total secs: " + t.totSec());
        System.out.println("Tokens / sec: " + (sents.getNumTokens() / t.totSec()));
    }
    
}
