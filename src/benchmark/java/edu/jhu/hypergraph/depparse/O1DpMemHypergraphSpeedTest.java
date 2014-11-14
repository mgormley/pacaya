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
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentenceReaderSpeedTest;
import edu.jhu.util.Timer;
import edu.jhu.util.semiring.Algebras;
import edu.jhu.util.semiring.LogViterbiSemiring;
import edu.jhu.util.semiring.Semiring;

public class O1DpMemHypergraphSpeedTest {

    /**
     * Comparison of bottom-up for-loops for hyperedge enumeration vs. building a hypergraph edge
     * list explicitly in memory.
     * 
     * LOG_SEMIRING: (inside/outside/marginals)
     * 
     * opt == 1: Total secs: 24.296 Tokens / sec: 2333.0589397431677
     * 
     * opt == 0: Total secs: 34.312 Tokens / sec: 1652.0167871298672
     * 
     * LogViterbiSemiring: (inside only)
     * 
     * opt == 1: Total secs: 3.578 Tokens / sec: 15842.370039128005
     * 
     * opt == 0: Total secs: 15.465 Tokens / sec: 3665.308761720013
     * 
     * opt == 2: Total secs: 2.98 Tokens / sec: 19021.476510067114
     */
    @Test
    public void testSpeed() {
        final int opt = 0;
        Semiring a = new LogViterbiSemiring();
        AnnoSentenceCollection sents = AnnoSentenceReaderSpeedTest.readPtbYmConllx();
        
        Timer t = new Timer();
        t.start();
        int s=0;
        int n=0;
        for (AnnoSentence sent : sents) {
            double[] root = new double[sent.size()];
            double[][] child = new double[sent.size()][sent.size()];
            Hypergraph hg;
            O1DpHypergraph o1hg = new O1DpHypergraph(root, child, a, false);
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
                // Just iterate over the hyperedges.
                o1hg.applyTopoSort(new HyperedgeFn() {                    
                    @Override
                    public void apply(Hyperedge e) { }
                });
            }
            Scores scores = new Scores();
            Hyperalgo.insideAlgorithm(hg, o1hg.getPotentials(), a, scores);
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
