package edu.jhu.hltcoe.parse.pr;

import static depparsing.globals.Constants.CHILD;
import static depparsing.globals.Constants.CHOICE;
import static depparsing.globals.Constants.CONT;
import static depparsing.globals.Constants.END;
import static depparsing.globals.Constants.LEFT;
import static depparsing.globals.Constants.RIGHT;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import util.CountAlphabet;
import depparsing.decoding.BinaryRhs;
import depparsing.decoding.RuleRhs;
import depparsing.decoding.UnaryRhs;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;

/**
 * MRG: This class was modified to break ties randomly.
 */
public class CKYParser {

	private static final double PROB_EQUALS_TOLERANCE = 1e-13;
	private static final boolean BREAK_TIES_RANDOMLY = true;
	
    /**
	 * For use in ensuring code updates are backward compatible
	 * (for comparison to Noah's original model A). 
	 */
	private static void printScores(double[][][][] scores, DepSentenceDist sd,
			CountAlphabet<String> tagAlphabet, String scoresFile) throws IOException {
		BufferedWriter outWriter = new BufferedWriter(new FileWriter(scoresFile));
		for(int i = 0; i < scores.length; i++) {
			for(int j = i + 1; j < scores[i].length; j++) {
				HashMap<String,Integer> kVals = new HashMap<String,Integer>();
				for(int k = 0; k < scores[i][j].length; k++) {
					String str;
					if(sd.nontermMap.isLeftChildIndex(k)) {
						if(k - sd.nontermMap.getNontermIndex(LEFT, CHILD) == 0)
							str = "Lc0";
						else str = "Lc";
					} else if(sd.nontermMap.isRightChildIndex(k)) {
						if(k - sd.nontermMap.getNontermIndex(RIGHT, CHILD) == 0)
							str = "Rc0";
						else str = "Rc";
					} else if(sd.nontermMap.isLeftChoiceIndex(k)) {
						if(k - sd.nontermMap.getNontermIndex(LEFT, CHOICE) == 0)
							str = "L0";
						else str = "L";
					} else if(sd.nontermMap.isRightChoiceIndex(k)) {
						if(k - sd.nontermMap.getNontermIndex(RIGHT, CHOICE) == 0)
							str = "N";
						else str = "R";
					} else {
						throw new RuntimeException("Should never get here in ridiculous temp code");
					}
					kVals.put(str, k);
				}
				String[] nonterms = new String[]{"N", "L0", "Lc0", "Rc0", "Lc", "Rc", "L", "R"};
				for(String nonterm : nonterms) {
					outWriter.write("[start, end) = [" + i + ", " + j + "), top = " + nonterm + "\n");
					int finalLen = scores[i][j][kVals.get(nonterm)].length;
					for(int l = 0; l < finalLen; l++) {
						String tag = tagAlphabet.lookupIndex(sd.depInst.postags[l]);
						outWriter.write(tag + ":" + scores[i][j][kVals.get(nonterm)][l] + " ");
					}
					outWriter.write("\n");
				}
				outWriter.write("\n");
			}
			outWriter.write("\n");
		}

		outWriter.close();
	}
	
	/**
	 * Call this method instead of the parseSentence that has 4 parameters
	 * if you want to avoid printing the scores matrix for the sentence.
	 */
	public static double parseSentence(DepSentenceDist sd, int[] parse) {
		try { return parseSentence(sd, parse, null, null); } catch(IOException e) { /* Will never happen */ };
		return Double.NEGATIVE_INFINITY;
	}
	
	/**
	 * Returns the probability of the Viterbi dependency parse of the sentence represented by the posNums.
	 * The parse itself is written to the "parse" input variable.
	 */
	public static double parseSentence(DepSentenceDist sd, int[] parse,
			CountAlphabet<String> tagAlphabet, String scoresFile) throws IOException {
		int numWords = sd.depInst.numWords;
		
		// Scores matrix stores the max probability that we have a chunk [start, end),
		// headed by a particular nonterminal and index in the sentence
		double scores[][][][] = new double[numWords + 1][numWords + 1][sd.nontermMap.numNontermTypes][numWords];
		RuleRhs back[][][][] = new RuleRhs[numWords + 1][numWords + 1][sd.nontermMap.numNontermTypes][numWords];
		double jitter[][][][] = new double[numWords + 1][numWords + 1][sd.nontermMap.numNontermTypes][numWords];
		boolean hasJitter[][][][] = new boolean[numWords + 1][numWords + 1][sd.nontermMap.numNontermTypes][numWords];
		
		// Set all scores to -Infinity initially
		for(int i = 0; i <= numWords; i++)
			for(int j = 0; j <= numWords; j++)
				for(int k = 0; k < sd.nontermMap.numNontermTypes; k++)
					for(int l = 0; l < numWords; l++)
						scores[i][j][k][l] = Double.NEGATIVE_INFINITY;

		// Take care of terminals
		for(int i = 0; i < numWords; i++) {
			// Each terminal could be produced by any of:
			// stop(w,left,j): Lj[w] -> w,
			// for j in 0, ..., sd.nontermMap.maxValency - 1
			int baseIndexL =  sd.nontermMap.getNontermIndex(LEFT, CHOICE);
			for(int v = 0; v < sd.nontermMap.maxValency; v++) {
				int dv = Math.min(v, sd.nontermMap.decisionValency - 1);
				scores[i][i + 1][baseIndexL + v][i] = sd.decision[i][LEFT][dv][END];
				back[i][i + 1][baseIndexL + v][i] = new UnaryRhs(i, -1);
			}
			
			// Note that in fact only some terminals can be produced by each type of valency ---
			// but we don't need to worry about this since it will resolve itself in the traceback
		
			// Take care of unary rules directly above terminal rules
			int baseIndexR = sd.nontermMap.getNontermIndex(RIGHT, CHOICE);
			for(int v = 0; v < sd.nontermMap.maxValency; v++) {
				int dv = Math.min(v, sd.nontermMap.decisionValency - 1);
				
				// L0[w] -> w is produced by
				// stop(x,right,j): Rj[w] -> L0[w]
				scores[i][i + 1][baseIndexR + v][i] = 
					sd.decision[i][RIGHT][dv][END] + scores[i][i + 1][baseIndexL][i];
				back[i][i + 1][baseIndexR + v][i] = new UnaryRhs(i, baseIndexL);
				
				// Other Li are only produced only by binary rules
			}
		}
		
		if(tagAlphabet != null)
			printScores(scores, sd, tagAlphabet, scoresFile);

		for(int span = 2; span <= numWords; span++) {
			for(int begin = 0; begin <= numWords - span; begin++) {
				int end = begin + span;
				for(int split = begin + 1; split <= end - 1; split++) {
					// Take care of binary rules
					addChildProbs(begin, end, split, sd, back, scores, jitter, hasJitter);
				}
				// Take care of unary rules
				addStopProbs(begin, end, sd, back, scores, jitter, hasJitter);
			}
		}
		
		// root(x): S -> N[x]
		int topWord = -1;
		int rootLoc = -1;
		double sentenceProb = Double.NEGATIVE_INFINITY;
		double sentenceJitter = 0.0;
		boolean sentenceHasJitter = false;
		int startValency = sd.nontermMap.getNontermIndex(RIGHT, CHOICE);
		for(int i = 0; i < numWords; i++) {
			RuleRhs backLhs = back[0][numWords][startValency][i];
			if(backLhs != null) {
				double prob = sd.root[i] + scores[0][numWords][startValency][i];
				double jit = 0.0;
				boolean hasJit = false;
				
                // Compare sentenceProb and prob. If they are equal break the tie by 
				// comparing the jitter.
				int diff = Utilities.compare(sentenceProb, prob, PROB_EQUALS_TOLERANCE);
				if (BREAK_TIES_RANDOMLY && diff == 0) {
				    jit = Prng.nextDouble();
                    hasJit = true;
				    if (!sentenceHasJitter) {
				        // Lazily create the jitter.
				        sentenceJitter = Prng.nextDouble();
                        sentenceHasJitter = true;
				    }
				    diff = Double.compare(sentenceJitter, jit);
				}
                if(diff < 0) {
					sentenceProb = prob;
					rootLoc = backLhs.head;
					topWord = i;
                    sentenceJitter = jit;
                    sentenceHasJitter = hasJit;
				}
			}
		}
		assert(topWord >= 0 && topWord < numWords) : "No path to S.";
		
		// Trace back from S to get parse
		Arrays.fill(parse, -1);
		traceback(-1, topWord, 0, numWords, rootLoc, back, parse, sd);
		for(int i = 0; i < numWords; i++) {
			// Make sure all parent assignments are within appropriate range
			assert(parse[i] > 0) : "Failed to assign all words a parent.";
			assert(parse[i] <= numWords) : "Assigned a parent too large for sentence.";
			if(rootLoc != i) {
				assert(parse[i] != i + 1) : "Assigned word as its own parent.";
			}
		}
		parse[rootLoc] = 0;
		
		//System.out.println(tree.toString());
		//tree.PrintBinaryTree();
		
		return sentenceProb;
	}

	private static void addStopProbs(int begin, int end, DepSentenceDist sd,
			RuleRhs[][][][] back, double[][][][] score, 
			double[][][][] jitter, boolean[][][][] hasJitter) {
		// A[begin, end) -> B[begin, end)
		int A, B;
		for(int i = begin; i < end; i++) {
			// continue: Lj[x] -> Lcj[x]
			// (and similarly for right)
			// end: Rj[x] -> L0[x]
			// (end for left has already been taken care of in terminal rules section)
			// Note that order matters here, since continue on left can feed into stop on right
			int[] dirs = new int[]{LEFT, RIGHT, RIGHT};
			int[] choices = new int[]{CONT, CONT, END};
			for(int k = 0; k < dirs.length; k++) {
				int dir = dirs[k];
				int choice = choices[k];

				for(int v = 0; v < sd.nontermMap.maxValency; v++) {
					int dv = Math.min(v, sd.nontermMap.decisionValency - 1);
					A = sd.nontermMap.getNontermIndex(dir,CHOICE) + v;

					if(choice == CONT) B = sd.nontermMap.getNontermIndex(dir,CHILD) + v;
					else B = sd.nontermMap.getNontermIndex(LEFT,CHOICE);
					RuleRhs backLhs = back[begin][end][B][i];

					if(backLhs != null) {
						double prob = sd.decision[i][dir][dv][choice] +
						score[begin][end][B][i];
						double jit = 0.0;
						boolean hasJit = false;

		                // Compare the best prob to the current prob. If they are equal break the tie by 
		                // comparing the jitter.
						double diff = Utilities.compare(score[begin][end][A][i], prob, PROB_EQUALS_TOLERANCE);
						if (BREAK_TIES_RANDOMLY && diff == 0) {
						    jit = Prng.nextDouble();
						    hasJit = true;
						    if (!hasJitter[begin][end][A][i]) {
						        jitter[begin][end][A][i] = Prng.nextDouble();
                                hasJitter[begin][end][A][i] = true;
						    }
						    diff = Double.compare(jitter[begin][end][A][i], jit);
						}
						if(diff < 0) {
							score[begin][end][A][i] = prob;
							back[begin][end][A][i] = new UnaryRhs(backLhs.head, B);
							jitter[begin][end][A][i] = jit;
                            hasJitter[begin][end][A][i] = hasJit;
						}
					}
				}
			}
		}
	}

	private static void addChildProbs(int begin, int end, int split, DepSentenceDist sd,
							   RuleRhs[][][][] back, double[][][][] score, 
							   double[][][][] jitter, boolean[][][][] hasJitter) {
		
		// A[begin, end) -> B[begin, split) C[split, end)
		RuleRhs leftBack, rightBack, parentBack;
		int childIndex, parentIndex;
		int indexB, indexC, nontermB, nontermC;
		for(int leftIndex = begin; leftIndex < split; leftIndex++) {
			for(int rightIndex = split; rightIndex < end; rightIndex++) {
				int vIncr = 1;
				
				// for i in 0, ..., model.valencey - 2
				// Lci[x] -> L(i + 1)[x] R0[x'] // 
				// and for i = model.valencey - 1
				// Lci[x] -> Li[x] R0[x']
				// (and similarly for right)
				for(int v = 0; v < sd.nontermMap.maxValency; v++) {
					int cv = Math.min(v, sd.nontermMap.childValency - 1);
					if(v == sd.nontermMap.maxValency - 1) vIncr = 0;
				
					for(int dir = 0; dir < 2; dir++) {
						if(dir == LEFT) {
							nontermB = sd.nontermMap.getNontermIndex(RIGHT,CHOICE);
							childIndex = leftIndex;
							leftBack = back[begin][split][nontermB][childIndex];
							indexB = childIndex;

							nontermC = sd.nontermMap.getNontermIndex(LEFT,CHOICE) + v + vIncr;
							parentIndex = rightIndex;
							rightBack = back[split][end][nontermC][parentIndex];
							parentBack = rightBack;
							indexC = parentIndex;
						} else {
							nontermB = sd.nontermMap.getNontermIndex(RIGHT,CHOICE) + v + vIncr;
							parentIndex = leftIndex;
							leftBack = back[begin][split][nontermB][parentIndex];
							parentBack = leftBack;
							indexB = parentIndex;

							nontermC = sd.nontermMap.getNontermIndex(RIGHT,CHOICE);
							childIndex = rightIndex;
							rightBack = back[split][end][nontermC][childIndex];
							indexC = childIndex;
						}

						if(leftBack != null && rightBack != null) {
							double prob = sd.child[childIndex][parentIndex][cv] +
							score[begin][split][nontermB][indexB] +
							score[split][end][nontermC][indexC];
							double jit = 0.0;
							boolean hasJit = false;
                            int lhsIndex = sd.nontermMap.getNontermIndex(dir,CHILD) + v;

	                        // Compare the best prob to the current prob. If they are equal break the tie by 
	                        // comparing the jitter.
							double diff = Utilities.compare(score[begin][end][lhsIndex][parentIndex], prob, PROB_EQUALS_TOLERANCE);
							if (BREAK_TIES_RANDOMLY && diff == 0) {
							    jit = Prng.nextDouble();
							    hasJit = true;
							    if (!hasJitter[begin][end][lhsIndex][parentIndex]) {
							        jitter[begin][end][lhsIndex][parentIndex] = Prng.nextDouble();
							        hasJitter[begin][end][lhsIndex][parentIndex] = true;
							    }
							    diff = Double.compare(jitter[begin][end][lhsIndex][parentIndex], jit);
							}
							if(diff < 0) {
								score[begin][end][lhsIndex][parentIndex] = prob;
								back[begin][end][lhsIndex][parentIndex] =
									new BinaryRhs(parentBack.head, split, nontermB, nontermC, childIndex);
							    jitter[begin][end][lhsIndex][parentIndex] = jit;
                                hasJitter[begin][end][lhsIndex][parentIndex] = hasJit;
							}
						}
					}
				}
			}
		}
	}

	private static void traceback(int nonterminal, int word, int begin, int end, int parent,
				           RuleRhs[][][][] back, int[] parse, DepSentenceDist sd) {
		RuleRhs rhs;
		if(nonterminal == -1) { // At root
			rhs = new UnaryRhs(parent, sd.nontermMap.getNontermIndex(RIGHT,CHOICE));
		} else rhs = back[begin][end][nonterminal][word];
		
		if(rhs.B == -1) {
			assert (begin + 1 == end) : "Traceback assigning parent too early.";
			
			// ++ for standard dep numbering
			parse[begin] = 1 + parent;
			return;
		}
		
		RuleRhs childRhs;
		if(nonterminal  == -1 || sd.nontermMap.isUnaryIndex(nonterminal)) {
			// S and other unary productions
			UnaryRhs rhsU = (UnaryRhs) rhs;
			
			childRhs = back[begin][end][rhsU.B][word];
			assert(rhsU.head == childRhs.head) : "Cannot change heads within unary rules.";
			traceback(rhsU.B, word, begin, end, parent, back, parse, sd);
		} else if(sd.nontermMap.isLeftChildIndex(nonterminal)) {
			// binary productions, left children
			BinaryRhs rhsL = (BinaryRhs) rhs;
			
			childRhs = back[begin][rhsL.split][rhsL.B][rhsL.childpos];
			traceback(rhsL.B, rhsL.childpos, begin, rhsL.split, rhsL.head, back, parse, sd);
			
			childRhs = back[rhsL.split][end][rhsL.C][word];
			assert(rhsL.head == childRhs.head): "Illegal head change in left child split.";
			traceback(rhsL.C, word, rhsL.split, end, parent, back, parse, sd);
		} else if(sd.nontermMap.isRightChildIndex(nonterminal)) {
			// binary productions, right children
			BinaryRhs rhsR = (BinaryRhs) rhs;
			
			childRhs = back[begin][rhsR.split][rhsR.B][word];
			assert(rhsR.head == childRhs.head): "Illegal head change in right child split.";
			traceback(rhsR.B, word, begin, rhsR.split, parent, back, parse, sd);
			
			childRhs = back[rhsR.split][end][rhsR.C][rhsR.childpos];
			traceback(rhsR.C, rhsR.childpos, rhsR.split, end, rhsR.head, back, parse, sd);
			return;
		} else {
			throw new RuntimeException("Invalid nonterminal \"" + nonterminal + "\"");
		}
	}

//	/**
//	 * Computes difference between gold parses and Viterbi parses
//	 * to get directed and undirected accuracy figures.
//	 */
//	public static double[] computeAccuracy(DepProbMatrix model, ArrayList<WordInstance> depInsts, int[][] parses) {
//		int totalWords = 0;
//		int numDirCorrect = 0;
//		int numUnDirCorrect = 0;
//		
//		for(int k = 0; k < depInsts.size(); k++) {
//			DepSentenceDist sd = new DepSentenceDist((DepInstance)depInsts.get(k), model.nontermMap);
//			sd.cacheModel(model);
//			
////			try {
////				model.printLogProbs("/home/jengi/newest.model");
////			} catch(IOException e) {
////				System.out.println("Printing log probs failed");
////			}
//			
//			int numWords = sd.depInst.postags.length;
//			int[] parse = new int[numWords];
//			
////			try {
////				parseSentence(sd, parse,  model.corpus.tagAlphabet, "/home/jengi/newest.scores");
////			} catch(IOException e) {
////				e.printStackTrace();
////				System.out.println("Printing scores failed");
////			}
//			
//			parseSentence(sd, parse);
//			if (parses!=null) parses[k] = parse;
//			if (parse.length != sd.depInst.parents.length) throw new AssertionError("mis-matched length of input and output");
//			int[] counts = countCorrect(model.corpus, sd.depInst, parse);
//			numDirCorrect += counts[0];
//			numUnDirCorrect += counts[1];
//			totalWords += counts[2];
//		}
//		
//		return new double[]{numDirCorrect / (double) (totalWords),
//				numUnDirCorrect / (double) (totalWords)};
//	}
//	
//	
//	/**
//	 * Computes difference between gold parses and Viterbi parses
//	 * to get directed and undirected accuracy figures.
//	 */
//	public static double[] computeAccuracy(DepSentenceDist[] sentDists, int[][] parses, DepCorpus c) {
//		int totalWords = 0;
//		int numDirCorrect = 0;
//		int numUnDirCorrect = 0;
//		
//        for(int k = 0; k < sentDists.length; k++) {
//            DepSentenceDist sd = sentDists[k];
//            int numWords = sd.depInst.postags.length;
//			int[] parse = new int[numWords];
//
//			//try {
//				//parseSentence(sd, parse,  c.tagAlphabet, "/home/jengi/newest.scores");
//				parseSentence(sd, parse);
//			//} catch(IOException e) {
//				//e.printStackTrace();
//				//System.out.println("Printing scores failed");
//			//}
//				if (parses!=null) parses[k] = parse;
//				int[] counts = countCorrect(c, sd.depInst, parse);
//				numDirCorrect += counts[0];
//				numUnDirCorrect += counts[1];
//				totalWords += counts[2];
//		}
//		
//		return new double[]{numDirCorrect / (double) (totalWords),
//				numUnDirCorrect / (double) (totalWords)};
//	}
	
//	public static int[] countCorrect(DepCorpus c, DepInstance gold, int[] guess) {
//		int numWordsCounted = 0;
//		int numDirCorrect = 0;
//		int numUnDirCorrect = 0;
//		for(int i = 0; i < gold.postags.length; i++) {
//			// Count accuracy for all non-punctuation children
//			if(!c.isPunctuation(gold.postags[i])) {
//				int goldParent = gold.parents[i];
//				int guessParent = guess[i];
//
//				boolean match = (goldParent == guessParent);
//				boolean reverseMatch = false;
//				if(guessParent != 0) {
//					// Head of guessed parent == child
//					reverseMatch = (gold.parents[guessParent - 1] == i + 1);
//				}
//				numDirCorrect += (match? 1 : 0);
//				numUnDirCorrect += (match || reverseMatch ? 1 : 0);
//				numWordsCounted++;
//			}
//		}
//		
//		return new int[]{numDirCorrect, numUnDirCorrect, numWordsCounted};
//	}
	
}
