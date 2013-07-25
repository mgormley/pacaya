package edu.jhu.srl;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.data.DepTree;
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09ReadWriteTest;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FgExample;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFgExampleBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.Alphabet;

/**
 * Unit tests for {@link SrlFgExampleBuilderTest}.
 * @author mgormley
 */
public class SrlFgExampleBuilderTest {

    @Test
    public void testRoleTrainAssignment() throws Exception {
        Alphabet<Feature> alphabet = new Alphabet<Feature>();        

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        CorpusStatistics.normalizeRoleNames(sents);
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.biasOnly = true;    
        CorpusStatistics cs = new CorpusStatistics(fePrm);
        cs.init(sents);
        CoNLL09Sentence sent = sents.get(0);
        
        Alphabet<String> obsAlphabet = new Alphabet<String>();
        
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fePrm = fePrm;
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.alwaysIncludeLinkVars = true;
        SrlFgExampleBuilder builder = new SrlFgExampleBuilder(prm, alphabet, cs, obsAlphabet);
        FgExample ex = builder.getFGExample(sent);
        
        assertEquals(1, obsAlphabet.size());
        assertEquals(5*2 + 2 + 5, alphabet.size());
        
        VarConfig vc = ex.getGoldConfig();
        System.out.println(vc.toString().replace(",", "\n"));
        for (Var v : vc.getVars()) {
            RoleVar role = (RoleVar) v;
            if (role.getParent() == 2 && role.getChild() == 0) {
                assertEquals("arg0", vc.getStateName(v));
            } else if (role.getParent() == 2 && role.getChild() == 4) {
                assertEquals("arg1", vc.getStateName(v));
            } else {
                assertEquals("_", vc.getStateName(v));
            }
        }
        assertEquals(18, vc.size());
    }


    @Test
    public void testLinkTrainAssignment() throws Exception {
        Alphabet<Feature> alphabet = new Alphabet<Feature>();        

        InputStream inputStream = this.getClass().getResourceAsStream(CoNLL09ReadWriteTest.conll2009Example);
        CoNLL09FileReader cr = new CoNLL09FileReader(inputStream);
        List<CoNLL09Sentence> sents = cr.readSents(1);
        SentFeatureExtractorPrm fePrm = new SentFeatureExtractorPrm();
        fePrm.biasOnly = true;
        fePrm.useGoldSyntax = true;
        CorpusStatistics cs = new CorpusStatistics(fePrm);
        cs.init(sents);
        CoNLL09Sentence sent = sents.get(0);
        
        Alphabet<String> obsAlphabet = new Alphabet<String>();
        
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fePrm = fePrm;
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.linkVarType = VarType.PREDICTED;
        prm.fgPrm.alwaysIncludeLinkVars = true;
        SrlFgExampleBuilder builder = new SrlFgExampleBuilder(prm, alphabet, cs, obsAlphabet);
        FgExample ex = builder.getFGExample(sent);

        VarConfig vc = ex.getGoldConfig();
        System.out.println(vc.toString().replace(",", "\n"));

        assertEquals(18 + 19*18 + 19, vc.size());
        
        int[] parents = getParents(sent.size(), vc);
        System.out.println(Arrays.toString(parents));
        assertTrue(DepTree.checkIsProjective(parents));
        assertArrayEquals(new int[]{2, 2, -1, 4, 2, 4, 7, 5, 7, 8, 7, 14, 11, 11, 10, 14, 17, 15, 2}, parents);
    }

    /**
     * Decodes the parents defined by a variable assignment for a single
     * sentence.
     * 
     * @param n The sentence length.
     * @param vc The variable assignement.
     * @return The parents array.
     */
    // TODO: Maybe use this somewhere? Probably not... we should really decode
    // using MBR decoding over the link marginals.
    private static int[] getParents(int n, VarConfig vc) {
        int[] parents = new int[n];
        Arrays.fill(parents, -2);
        for (Var v : vc.getVars()) {
            if (v instanceof LinkVar) {
                LinkVar link = (LinkVar) v;
                if (vc.getState(v) == LinkVar.TRUE) {
                    if (parents[link.getChild()] != -2) {
                        throw new IllegalStateException(
                                "Multiple link vars define the same parent/child edge. Is this VarConfig for only one example?");
                    }
                    parents[link.getChild()] = link.getParent();
                }
            }
        }
        return parents;
    }

}
