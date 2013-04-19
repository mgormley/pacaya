package edu.jhu.hltcoe.parse.cky;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.hltcoe.util.Pair;

public class CkyPcfgParserTest {

    @Test
	public void testSimpleSentence1() {
		// time flies like an arrow.
		CnfGrammarBuilder builder = new CnfGrammarBuilder();
		builder.loadFromResource(CnfGrammarBuilderTest.timeFliesGrammarResource);
		
		CnfGrammar grammar = builder.getGrammar();
				
		Pair<CfgTree, Double> pair = parseSentence("time flies like an arrow", grammar);
		CfgTree tree = pair.get1();
		double logProb = pair.get2();
		
		System.out.println(logProb);
		System.out.println(tree);
		System.out.println(tree.getAsPennTreebankString());
		String goldTree = "(S (NP (N time)) (VP (V flies) (PP (IN like) (NP (DT an) (N arrow)))))";
		String treeStr = tree.getAsPennTreebankString().replaceAll("\\s+", " ");
		Assert.assertEquals(goldTree, treeStr);
		Assert.assertEquals(-11.0, logProb, 1e-13);
	}
    
    @Test
	public void testSimpleSentence2() {
		// an arrow flies like time
		CnfGrammarBuilder builder = new CnfGrammarBuilder();
		builder.loadFromResource(CnfGrammarBuilderTest.timeFliesGrammarResource);
		
		CnfGrammar grammar = builder.getGrammar();
				
		Pair<CfgTree, Double> pair = parseSentence("an arrow flies like time", grammar);
		CfgTree tree = pair.get1();
		double logProb = pair.get2();
		
		System.out.println(logProb);
		System.out.println(tree);
		System.out.println(tree.getAsPennTreebankString());
		String treeStr = tree.getAsPennTreebankString().replaceAll("\\s+", " ");
		//System.out.println(treeStr);
		String goldTree = "(S (NP (DT an) (N arrow)) (VP (V flies) (PP (IN like) (NP (N time)))))";
		Assert.assertEquals(goldTree, treeStr);
		Assert.assertEquals(-11.0, logProb, 1e-13);
	}
	
	public static Pair<CfgTree, Double> parseSentence(String sentence, CnfGrammar grammar) {
		String[] tokens = sentence.split(" ");
		int[] sent = grammar.getLexAlphabet().lookupIndices(tokens);
		System.out.println(Arrays.toString(sent));
		Chart chart = CkyPcfgParser.parseSentence(sent, grammar);
		return chart.getViterbiParse();
	}
}
