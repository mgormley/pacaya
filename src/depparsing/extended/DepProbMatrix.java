package depparsing.extended;

import static depparsing.globals.Constants.LEFT;
import static depparsing.globals.Constants.RIGHT;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

import model.AbstractCountTable;
import edu.jhu.hltcoe.util.Alphabet;
import util.ArrayMath;
import util.LogSummer;
import depparsing.model.NonterminalMap;
import depparsing.util.Lambda;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;

/**
 * MRG: This class was modified to remove its dependence on the corpus.
 * 
 * Container for the Model A probabilities specified in Noah Smith's thesis.
 * These parameters, for hypothetical POS tags x and x' are given below:
 * 
 * probability                rule
 * root(x)                   S -> R0[x]
 * 
 * stop(x,right,0)           R0[x] -> L0[x]
 * continue(x,right,0)       R0[x] -> Rc0[x]
 * child(x,right,x',0)       Rc0[x] -> R1[x] R0[x']
 * ...
 * stop(x,right,V)           RV[x] -> L0[x]
 * continue(x,right,V)       RV[x] -> RcV[x]
 * child(x,right,x',Vc)      RcV[x] -> RV[x] R0[x']
 * 
 * stop(x,left,0)            L0[x] -> x
 * continue(x,left,0)        L0[x] -> Lc0[x]
 * child(x,left,x',0)        Lc0[x] -> R0[x'] L1[x]
 * ...
 * stop(x,left,V)            LV[x] -> x
 * continue(x,left,V)        LV[x] -> LcV[x]
 * child(x,left,x',Vc)       LcV[x] -> R0[x'] LV[x]
 * 
 * Note: Max child valency may not be the same as max decision valency;
 *       max of the two valences determines the number of rules in the grammar,
 *       and the size of inside-outside arrays, but the number of parameters
 *       depends on both valences; parameters for some rules may be identical.
 */
@Deprecated
public class DepProbMatrix extends AbstractCountTable implements Serializable {

	/**
	 *  So that this class can be serializable,
	 *  which means we can save these objects to files
	 */
	private final static long serialVersionUID = 42L;
	
	/**
	 * Number of POS tags.
	 */
	public final int numTags;
	
	/**
	 *  Root probabilities,
	 *  indexed by POS tag.
	 */
	public final double root[];

	/**
	 *  Child probabilities,
	 *  indexed by child POS tag, parent POS tag, direction, and child existence.
	 */
    public final double child[][][][];

	/** 
	 * Stop/continue probabilities,
	 * indexed by POS tag, direction, child existence, and the decision (stop/continue).
	 */
	public final double decision[][][][];
		
	public final NonterminalMap nontermMap;

    protected final Alphabet<Label> tagAlphabet;
	
	/**
	 * Creates a new dependency matrix, with all parameters set to 0.
	 * 
	 * @param numTags # of POS tags in the model
	 */
	public DepProbMatrix(Alphabet<Label> tagAlphabet, int decisionValency, int childValency) {
		this.numTags = tagAlphabet.size();
		this.tagAlphabet = tagAlphabet;
		root = new double[numTags];
		child = new double[numTags][numTags][2][childValency];
		decision = new double[numTags][2][decisionValency][2];
		nontermMap = new NonterminalMap(decisionValency, childValency);
	}
	
	public void getChildNormalizers(double[][][] childNorms) {
		ArrayMath.set(childNorms, Double.NEGATIVE_INFINITY);
		// Compute child and decision normalizers
		for(int dir = 0; dir < 2; dir++) {
			for(int s = 0; s < numTags; s++) {
				for(int j = 0; j < numTags; j++)
					for(int v = 0; v < nontermMap.childValency; v++)
						LogSummer.sum(childNorms[s][dir], v, child[j][s][dir][v]);
			}
		}
	}
	
	public double getNormalizers(double[][][] childNorms, double[][][] decisionNorms) {
		ArrayMath.set(childNorms, Double.NEGATIVE_INFINITY);
		// Compute child and decision normalizers
		getChildNormalizers(childNorms);
		for(int dir = 0; dir < 2; dir++)
			for(int s = 0; s < numTags; s++)
				for(int v = 0; v < nontermMap.decisionValency; v++)
					decisionNorms[s][dir][v] = LogSummer.sumAll(decision[s][dir][v]);
		
		// Compute root normalizer
		return LogSummer.sumAll(root);
	}
	
	public void checkForChildNegInfTotals(double[][][] childTotals) {
		double total;
		for(int dir = 0; dir < 2; dir++) {
			for(int s = 0; s < numTags; s++) {
				for(int v = 0; v < nontermMap.childValency; v++){
					total = childTotals[s][dir][v];
					if(Double.isInfinite(total) && total < 0) {
						double equalProb = Math.log(1/(double)numTags);
						for(int j = 0; j < numTags; j++)
							child[j][s][dir][v] = equalProb;
						total = 0;
					}
					childTotals[s][dir][v] = total;
				}
			}
		}
	}

	public double checkForNegInfTotals(double rootTotal, double[][][] childTotals, double[][][] decisionTotals) {
		// If all root probs are -Infinity, set them all equal
		if(Double.isInfinite(rootTotal) && rootTotal < 0) {
			ArrayMath.set(root, Math.log(1/(double)numTags));
			rootTotal = 0;
		}
		
		// Check for totals that are -Infinity, and set all corresponding parameters equal
		checkForChildNegInfTotals(childTotals);
		double total;
		for(int dir = 0; dir < 2; dir++) {
			for(int s = 0; s < numTags; s++) {
				for(int v = 0; v < nontermMap.decisionValency; v++) {
					total = decisionTotals[s][dir][v];
					if(Double.isInfinite(total) && total < 0) {
						double equalProb = Math.log(1/(double)2);
						for(int choice = 0; choice < 2; choice++)
							decision[s][dir][v][choice] = equalProb;
						total = 0;
					}
					decisionTotals[s][dir][v] = total;
				}
			}
		}
		
		return rootTotal;
	}
	
	public void logNormalizeChildren() {
		double[][][] childTotals = new double[numTags][2][nontermMap.childValency];	
		checkForChildNegInfTotals(childTotals);
		
		applyToChildNormGroups(new Lambda.Two<Double, Double, Double>() {
			public Double call(Double a, Double b) {
				assert(!Double.isInfinite(b)) : "total = " + b;
				return a - b;
			}
		}, childTotals);
	}
	
	public void logNormalize() {
		double[][][] childTotals = new double[numTags][2][nontermMap.childValency];
		double[][][] decisionTotals = new double[numTags][2][nontermMap.decisionValency];
		double rootTotal = getNormalizers(childTotals, decisionTotals);
		rootTotal = checkForNegInfTotals(rootTotal, childTotals, decisionTotals);
		
		// Subtract normalizers
		applyToNormGroups(new Lambda.Two<Double, Double, Double>() {
			public Double call(Double a, Double b) {
				assert(!Double.isInfinite(b)) : "total = " + b;
				return a - b;
			}
		}, rootTotal, childTotals, decisionTotals);
		
		// Check correctness of normalization
		assert(isNormalized(1e-10) == "") : isNormalized(1e-10);
	}
	
	/**
	 * Builds a String describing any parameter sets that aren't
	 * normalized to within threshold of 0.
	 * Returns the empty string if normalization is within threshold
	 * of perfect for all normalization classes.
	 * 
	 * @param threshold max allowed deviation from perfect normalization
	 * @return          description of normalization errors
	 */
	public String isNormalized(double threshold) {
		String message = "";

		double[][][] childTotals = new double[numTags][2][nontermMap.childValency];
		double[][][] decisionTotals = new double[numTags][2][nontermMap.decisionValency];
		double rootTotal = getNormalizers(childTotals, decisionTotals);
		
		// Check root probabilities
		if(Math.abs(rootTotal) > threshold)
			message += "root " + rootTotal + "\n";
		
		for(int dir = 0; dir < 2; dir++) {
			for(int s = 0; s < numTags; s++) {
				for (int v = 0; v < nontermMap.childValency; v++) {	
					// Check child probabilities
					if(Math.abs(childTotals[s][dir][v]) > threshold)
						message += "child (parent = " + s + ", dir = " + dir + ", valency = " + v + ") " + childTotals[s][dir][v] + "\n";
				}
				
				// Check stop probabilities
				for(int v = 0; v < nontermMap.decisionValency; v++) {
					if(Math.abs(decisionTotals[s][dir][v]) > threshold)
						message += "stop/continue (pos = " + s + ", dir = " + dir + ", valency = " + v + ") " + decisionTotals[s][dir][v] +  "\n";
				}
			}
		}
		
		return message;
	}
	
	public void assertNormalized(double threshold) {
	    String msg = isNormalized(threshold);
	    assert msg.equals("") : msg;
	}

	public void addChildBackoff(double backoffWeight) {
		// Set up arrays for collecting backoff counts for child probs
		int numTags = tagAlphabet.size();
		// Backing off parent tag so p[child][parent][direction][valence]--> p[child][direction][valence]
		//Index by child tag, direction valence
		double[][][] backoffs = new double[][][]{};
		//Index by direction valence
		double[][] backoffNorms = new double[][]{};
		if(backoffWeight > 0) {
			backoffs = new double[numTags][2][nontermMap.childValency];
			util.ArrayMath.set(backoffs, Double.NEGATIVE_INFINITY);
			backoffNorms = new double[2][nontermMap.childValency];
			util.ArrayMath.set(backoffNorms, Double.NEGATIVE_INFINITY);
		}

		// Collect backoff counts
		for(int p = 0; p < numTags; p++) {
			for(int c = 0; c < numTags; c++) {
				for(int dir = 0; dir < 2; dir++) {
					for(int v = 0; v < nontermMap.childValency; v++) {
						double nextTerm = child[c][p][dir][v];
						LogSummer.sum(backoffs[c][dir], v, nextTerm);
						LogSummer.sum(backoffNorms[dir], v, nextTerm);
					}
				}
			}
		}

		// Check for norm groups in the backoff where no elements occurred;
		// set all probs equal within each of these groups
		for(int dir = 0; dir < 2; dir++) {
			for(int v = 0; v < nontermMap.childValency; v++) {
				if(Double.isInfinite(backoffNorms[dir][v]) && backoffNorms[dir][v] < 0) {
					double eqProb = Math.log(1/(double)numTags);
					for(int c = 0; c < numTags; c++)
						backoffs[c][dir][v] = eqProb;
					backoffNorms[dir][v] = 0;
				}
			}
		}	

		// Normalize standard child probs
		logNormalizeChildren();

		// Add in the backoff, normalizing it in the process
		double logBW = Math.log(backoffWeight);
		double omLogBW = Math.log(1 - backoffWeight);
		for(int p = 0; p < numTags; p++) {
			for(int c = 0; c < numTags; c++) {
				for(int dir = 0; dir < 2; dir++) {
					for(int v = 0; v < nontermMap.childValency; v++) {
						child[c][p][dir][v] = LogSummer.sum(omLogBW + child[c][p][dir][v],
								logBW + backoffs[c][dir][v] - backoffNorms[dir][v]);
					}
				}
			}
		}
	}
	
	@Override
	public void clear(){
		fill(Double.NEGATIVE_INFINITY);
	}
	
	/**
	 * Sets all model parameters to the given value.
	 */
	public void fill(double value) {
		ArrayMath.set(root, value);
		ArrayMath.set(child, value);
		ArrayMath.set(decision, value);
	}
	
	/**
     * Sets all root model parameters to the given value.
     */
    public void fillRoot(double value) {
        ArrayMath.set(root, value);
    }
    
    /**
     * Sets all child model parameters to the given value.
     */
    public void fillChild(double value) {
        ArrayMath.set(child, value);
    }

    /**
     * Sets all decision model parameters to the given value.
     */
    public void fillDecision(double value) {
        ArrayMath.set(decision, value);
    }
    
	/**
	 * Copy all model parameters from the given source.
	 */
	public void fill(AbstractCountTable asource) {
		DepProbMatrix source = (DepProbMatrix)asource;
		ArrayMath.setEqual(root,source.root);
		ArrayMath.setEqual(child, source.child);
		ArrayMath.setEqual(decision, source.decision);
	}

	/**
	 * Scale the counts (represented in log space) by a factor of weight.
	 */
	public void scaleBy(double weight) {
		ArrayMath.plusEquals(child, Math.log(weight));
		ArrayMath.plusEquals(root, Math.log(weight));
		ArrayMath.plusEquals(decision, Math.log(weight));
	}

	
	/**
	 * Overwrites all parameters of this model with the values of p's parameters.
	 */
	public void copyFrom(DepProbMatrix p) {
		apply(new Lambda.Two<Double, Double, Double>() {
			public Double call(Double oldProb, Double newProb) {
				return newProb;
			}
		}, p.root, p.child, p.decision);
	}
	
	/**
	 * Adds value backoff to all model parameters.
	 * Note: This method assumes backoff is log of the backoff you want to add to the true parameters,
	 *       and that the parameters in this DepProbMatrix are logs of the true parameters.
	 */
	public void backoff(double backoff) {
		apply(new Lambda.Two<Double, Double, Double[]>() {
			public Double call(Double a, Double[] b) {
				return LogSummer.sum(a, b[0]);
			}},
			new Double[]{backoff});
	}
	
	/**
	 * Sets all NaN probs to -Infinty.
	 * (NaN can result from max likelihood initialization
	 * when total counts are zero, and so division by 0 occurs.)
	 */
	public void cleanupProbs() {
		apply(new Lambda.Two<Double, Double, Double[]>() {
			public Double call(Double a, Double[] b) {
				if(Double.isNaN(a))
					return b[0];
				return a;
			}},
			new Double[]{Double.NEGATIVE_INFINITY});
	}
	
	/**
	 * Adds the given value to all model parameters.
	 */
	public void addConstant(double prob) {
		apply(new Lambda.Two<Double, Double, Double[]>() {
			public Double call(Double a, Double[] b) {
				return a + b[0];
			}},
			new Double[]{prob});
	}

	/**
	 * Exponentiates all model parameters.
	 */
	public void convertLogToReal() {
		apply(new Lambda.Two<Double, Double, Double[]>() {
			public Double call(Double a, Double[] b) {
				return Utilities.exp(a);
			}},
			null);
	}
	
	/**
	 * Takes the (natural) log of all model parameters.
	 */
	public void convertRealToLog() {
		apply(new Lambda.Two<Double, Double, Double[]>() {
			public Double call(Double a, Double[] b) {
				return Utilities.log(a);
			}},
			null);
	}
	
	/**
	 * Sets all parameters to random numbers, then normalizes.
	 */
	public void setRandom() {		
		apply(new Lambda.Two<Double, Double, Double[]>() {
			public Double call(Double a, Double[] b) {
				return Prng.nextDouble();
			}},
			null);
		
		logNormalize();
	}
    
	public void applyToRoot(Lambda.Two<Double, Double, Double> function, Double arg) {
	    for(int i = 0; i < numTags; i++) {
            root[i] = function.call(root[i], arg);
        }
    }
	
	public void applyToChild(Lambda.Two<Double, Double, Double> function, Double arg) {
	    for(int i = 0; i < numTags; i++) {
            for(int dir = 0; dir < 2; dir++) {
                for(int j = 0; j < numTags; j++)
                    for(int v = 0; v < nontermMap.childValency; v++)
                        child[i][j][dir][v] = function.call(child[i][j][dir][v], arg);
            }
        }
    }
	
    public void applyToDecisions(Lambda.Two<Double, Double, Double> function, Double arg) {
        for(int p = 0; p < numTags; p++)
            for(int dir = 0; dir < 2; dir++)
                for(int kids = 0; kids < nontermMap.decisionValency; kids++)
                    for(int choice = 0; choice < 2; choice++)
                        decision[p][dir][kids][choice] =
                            function.call(decision[p][dir][kids][choice], arg);
    }
    
    public void applyToDecisions(Lambda.Two<Double, Double, Double> function, double[][][][] args) {
        for(int p = 0; p < numTags; p++)
            for(int dir = 0; dir < 2; dir++)
                for(int kids = 0; kids < nontermMap.decisionValency; kids++)
                    for(int choice = 0; choice < 2; choice++)
                        decision[p][dir][kids][choice] =
                            function.call(decision[p][dir][kids][choice], args[p][dir][kids][choice]);
    }
    	
	/**
     * Applies the given function to each model probability, passing in the same args each time.
     */
    public void apply(Lambda.Two<Double, Double, Double[]> function, Double[] args) {
        for(int i = 0; i < numTags; i++) {
            root[i] = function.call(root[i], args);
            
            for(int dir = 0; dir < 2; dir++) {
                for(int j = 0; j < numTags; j++)
                    for(int v = 0; v < nontermMap.childValency; v++)
                        child[i][j][dir][v] = function.call(child[i][j][dir][v], args);
                
                for(int v = 0; v < nontermMap.decisionValency; v++)
                    for(int choice = 0; choice < 2; choice++)
                        decision[i][dir][v][choice] = function.call(decision[i][dir][v][choice], args);
            }
        }
    }
    	
	/**
	 * Applies the given function to the model parameters,
	 * passing an additional parameter whose value depends on
	 * whether we're dealing with a root, child, or decision probability.
	 */
	public void apply(Lambda.Two<Double, Double, Double> function,
			double rootParam, double childParam, double decisionParam) {
		for(int i = 0; i < numTags; i++) {
			root[i] = function.call(root[i], rootParam);
			
			for(int dir = 0; dir < 2; dir++) {
				for(int j = 0; j < numTags; j++)
					for(int v = 0; v < nontermMap.childValency; v++)
						child[i][j][dir][v] = function.call(child[i][j][dir][v], childParam);
				
				for(int kids = 0; kids < nontermMap.decisionValency; kids++)
					for(int choice = 0; choice < 2; choice++)
						decision[i][dir][kids][choice] =
							function.call(decision[i][dir][kids][choice], decisionParam);
			}
		}
	}
	
	public void applyToChildNormGroups(Lambda.Two<Double, Double, Double> function,
			double[][][] childNorms) {
		for(int i = 0; i < numTags; i++)
			for(int dir = 0; dir < 2; dir++)
				for(int j = 0; j < numTags; j++)
					for(int v = 0; v < nontermMap.childValency; v++)
						child[i][j][dir][v] = function.call(child[i][j][dir][v], childNorms[j][dir][v]);
	}
	
	/**
	 * Applies the given function to the model parameters,
	 * passing an additional parameter whose value depends on
	 * the normalization group.
	 */
	public void applyToNormGroups(Lambda.Two<Double, Double, Double> function,
			double rootNorm, double[][][] childNorms, double[][][] decisionNorms) {
		applyToChildNormGroups(function, childNorms);
		for(int i = 0; i < numTags; i++) {
			root[i] = function.call(root[i], rootNorm);
			
			for(int dir = 0; dir < 2; dir++) {
				for(int kids = 0; kids < nontermMap.decisionValency; kids++)
					for(int choice = 0; choice < 2; choice++)
						decision[i][dir][kids][choice] =
							function.call(decision[i][dir][kids][choice],
									decisionNorms[i][dir][kids]);
			}
		}
	}
	
	/**
	 * Applies the given functions to the root, child, and decision probabilities respectively,
	 * passing an additional unique parameter to each model probability.
	 */
	public void apply(Lambda.Two<Double, Double, Double> function,
			double[] rootParams, double[][][][] childParams, double[][][][] decisionParams) {
		for(int i = 0; i < numTags; i++) {
			root[i] = function.call(root[i], rootParams[i]);
			
			for(int dir = 0; dir < 2; dir++) {
				for(int j = 0; j < numTags; j++)
					for(int v = 0; v < nontermMap.childValency; v++)
						child[j][i][dir][v] = function.call(child[j][i][dir][v], childParams[j][i][dir][v]);
				
				for(int kids = 0; kids < nontermMap.decisionValency; kids++)
					for(int choice = 0; choice < 2; choice++)
						decision[i][dir][kids][choice] =
							function.call(decision[i][dir][kids][choice],
									decisionParams[i][dir][kids][choice]);
			}
		}
	}
	
	/**
	 * Standard conversion for all doubles except -Infinity,
	 * which gets mapped to the "*" symbol.
	 * 
	 * @param d number representation
	 * @return  string representation of the input
	 */
	private String double2String(double d) {
		String out;
		
		if(d == Double.NEGATIVE_INFINITY) {
			out = "*";
		}	
		else out = Double.toString(d);
		
		return out;
	}
	
	@Override
	public String toString(){
		StringWriter outWriter = new StringWriter();
		try {
			printLogProbs(outWriter);
		} catch (IOException e) {
			// kind of lame, but I can't think of a better way
			throw new RuntimeException(e);
		}
		return outWriter.toString();
	}
	
	/**
	 * Prints the model parameters in a legible format.
	 */
	public void printLogProbs(String outFile) throws IOException {
		// Create output writer
		BufferedWriter outWriter = new BufferedWriter(new FileWriter(outFile));
		printLogProbs(outWriter);
		outWriter.close();
	}
	
	/**
	 * TODO: This should really live in CountAlphabet. 
	 */
	public static <T> ArrayList<T> getAllTagsStrings(Alphabet<T> tagAlphabet) {
        ArrayList<T> tags= new ArrayList<T>(tagAlphabet.size());
        for (int i = 0; i < tagAlphabet.size(); i++) {
            tags.add(tagAlphabet.lookupObject(i));
        }
        return tags;
    }
	
	private void printLogProbs(Writer outWriter) throws IOException {

		// Sort tags in alphabetical order
        ArrayList<Label> allTags = getAllTagsStrings(tagAlphabet);
        Label[] sortedTags = new Label[allTags.size()];
        sortedTags = allTags.toArray(sortedTags);
        Arrays.sort(sortedTags);
		
		// Print log probability estimates to output file
		for(Label tag : sortedTags) {
			int posNum = tagAlphabet.lookupIndex(tag);
			outWriter.write(tag + "\nroot\t" + double2String(root[posNum]) + "\n");

			for(int dir = 0; dir < 2; dir++) {
				outWriter.write(nontermMap.direction2String(dir) + "\n");
				for(int choice = 0; choice < 2; choice++) {
					outWriter.write("\t" + nontermMap.choice2String(choice) + "\n");
					for(int v = 0; v < nontermMap.decisionValency; v++) {
						outWriter.write("\t\tvalence " + v + ": " + double2String(decision[posNum][dir][v][choice]) + "\n");
					}
				}
			}
			
			for(int v = 0; v < nontermMap.childValency; v++) {
				outWriter.write("child: left, right, valence = " + v + "\n");
				for(int i = 0; i < numTags; i++) {
					outWriter.write(tagAlphabet.lookupObject(i) + ": " + 
							double2String(child[i][posNum][LEFT][v]) + ", " +
							double2String(child[i][posNum][RIGHT][v]) + "\n");
				}
			}

			outWriter.write("\n");
		}
		
	}
	
	public Alphabet<Label> getTagAlphabet() {
	    return tagAlphabet;
	}


}
