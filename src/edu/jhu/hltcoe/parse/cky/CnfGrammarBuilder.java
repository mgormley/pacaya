package edu.jhu.hltcoe.parse.cky;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import edu.jhu.hltcoe.util.Alphabet;

public class CnfGrammarBuilder {

	private ArrayList<Rule> allRules;
	private ArrayList<Rule> lexRules;
	private ArrayList<Rule> unaryRules;
	private ArrayList<Rule> binaryRules;

	private Alphabet<String> lexAlphabet;
	private Alphabet<String> ntAlphabet;
	
	public CnfGrammarBuilder() {
		allRules = new ArrayList<Rule>();
		lexRules = new ArrayList<Rule>();
		unaryRules = new ArrayList<Rule>();
		binaryRules = new ArrayList<Rule>();
		
		lexAlphabet = new Alphabet<String>();
		ntAlphabet = new Alphabet<String>();
	}
	
	public CnfGrammar getGrammar() {
		return new CnfGrammar(allRules, lexAlphabet, ntAlphabet);
	}

	public void loadFromResource(String resourceName) {
		InputStream inputStream = this.getClass().getResourceAsStream(
				resourceName);
		if (inputStream == null) {
			throw new RuntimeException("Unable to find resource: "
					+ resourceName);
		}
		try {
			loadFromInputStream(inputStream);
			inputStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void loadFromInputStream(InputStream inputStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		boolean readingLexicalRules = false;
		
		Pattern spaceRegex = Pattern.compile("\\s+");
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.startsWith("#") || line.equals("")) {
				// Skip comments and empty lines.
				continue;
			}
			if (line.equals("===")) {
				readingLexicalRules = true;
				continue;
			}
			String[] splits = spaceRegex.split(line);
			Rule r;
			if (splits.length == 3) {
				// Unary rule.
				if (readingLexicalRules) {
					r = getLexicalRule(splits[0], splits[1], Double.parseDouble(splits[2]));
					lexRules.add(r);
				} else {
					r = getUnaryRule(splits[0], splits[1], Double.parseDouble(splits[2]));
					unaryRules.add(r);
				}
			} else if (splits.length == 4 && !readingLexicalRules) {
				// Binary rule.
				r = getBinaryRule(splits[0], splits[1], splits[2], Double.parseDouble(splits[3]));
				binaryRules.add(r);
			} else {
				throw new RuntimeException("Invalid line: " + line);
			}
			allRules.add(r);
		}
	}

	private Rule getLexicalRule(String parentStr, String childStr, double logProb) {
		int parent = ntAlphabet.lookupIndex(parentStr);
		int child = lexAlphabet.lookupIndex(childStr);
		return new Rule(parent, child, Rule.LEXICAL_RULE, logProb, ntAlphabet, lexAlphabet);
	}

	private Rule getUnaryRule(String parentStr, String childStr, double logProb) {
		int parent = ntAlphabet.lookupIndex(parentStr);
		int child = lexAlphabet.lookupIndex(childStr);
		return new Rule(parent, child, Rule.UNARY_RULE, logProb, ntAlphabet, lexAlphabet);
	}

	private Rule getBinaryRule(String parentStr, String leftChildStr, String rightChildStr, double logProb) {
		int parent = ntAlphabet.lookupIndex(parentStr);
		int leftChild = lexAlphabet.lookupIndex(leftChildStr);
		int rightChild = lexAlphabet.lookupIndex(rightChildStr);
		return new Rule(parent, leftChild, rightChild, logProb, ntAlphabet, lexAlphabet);
	}

}
