package edu.jhu.hltcoe.parse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import edu.jhu.hltcoe.util.Timer;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDepTreebank;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.ZimplSolver;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModel.Lr;
import edu.jhu.hltcoe.parse.relax.RelaxedDepParser;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.DelayedDeleter;
import edu.jhu.hltcoe.util.Files;
import edu.jhu.hltcoe.util.Utilities;

public class IlpViterbiParser implements DepParser, RelaxedDepParser {

    private static final Logger log = Logger.getLogger(IlpViterbiParser.class);
    
    public static final String ZIMPL_CODE_XML = "/edu/jhu/hltcoe/parse/zimpl_dep_parse.xml";
    private static final int ZIMPL_WALL_POSITION = 0;
    protected ZimplXmlCodeContainerReader codeMap;
    public static final Pattern zimplVarRegex = Pattern.compile("[#$]");
    protected IlpFormulation formulation;
    protected File workspace;
    private DelayedDeleter deleter;
    protected IlpSolverFactory ilpSolverFactory;
    protected double parseWeight;
    
    public IlpViterbiParser(IlpFormulation formulation, IlpSolverFactory ilpSolverFactory) {
        this.formulation = formulation;
        this.ilpSolverFactory = ilpSolverFactory;
        codeMap = new ZimplXmlCodeContainerReader(formulation);
        codeMap.loadZimplCodeFromResource(ZIMPL_CODE_XML);
        workspace = Files.createTempDir("workspace", new File("."));
        deleter = new DelayedDeleter(Integer.MAX_VALUE);
    }

    @Override
    public DepTreebank getViterbiParse(DmvTrainCorpus corpus, Model genericModel) {
        if (corpus.getNumLabeled() > 0) {
            throw new RuntimeException("not implemented");
        } else {
            return getViterbiParse(corpus.getSentences(), genericModel);
        }
    }
    
    @Override
    public DepTreebank getViterbiParse(SentenceCollection sentences, Model model) {
        Timer stopwatch = new Timer();
        stopwatch.start();
        
        // Encode the model/sentences as an ILP and solve
        Map<String, Double> result = solve(sentences, model);
        
        // Decode parses
        DepTreebank depTreebank = decode(sentences, result);
        
        stopwatch.stop();
        log.debug(String.format("Avg parse time: %.3f", 
                stopwatch.totMs() / sentences.size()));
        log.debug(String.format("Tot parse time: %.3f", 
                stopwatch.totMs()));
        
        return depTreebank;
    }
    
    @Override
    public RelaxedDepTreebank getRelaxedParse(DmvTrainCorpus corpus, Model genericModel) {
        if (corpus.getNumLabeled() > 0) {
            throw new RuntimeException("not implemented");
        } else {
            return getRelaxedParse(corpus.getSentences(), genericModel);
        }
    }

    public RelaxedDepTreebank getRelaxedParse(SentenceCollection sentences, Model model) {
        Timer stopwatch = new Timer();
        stopwatch.start();
        
        // Encode the model/sentences as an ILP and solve.
        Map<String, Double> result = solve(sentences, model);
        
        // Decode relaxed parses.
        RelaxedDepTreebank depTreebank = relaxedDecode(sentences, result);
        
        stopwatch.stop();
        log.debug(String.format("Avg parse time: %.3f", 
                stopwatch.totMs() / sentences.size()));
        log.debug(String.format("Tot parse time: %.3f", 
                stopwatch.totMs()));
        
        return depTreebank;
    }

    /**
     * Encode the model/sentences as an ILP and solve.
     * 
     * Side effect: stores the parseWeight
     * 
     * @return an optimal ILP solution
     */
    protected Map<String, Double> solve(SentenceCollection sentences, Model model) {
        // Create workspace
        File tempDir = Files.createTempDir("ilp_parse", workspace);
        
        // Encode sentences and model
        File zimplFile = encode(tempDir, sentences, model);
        
        // Run zimpl and then ILP solver
        ZimplSolver solver = getZimplSolver(tempDir);
        solver.solve(zimplFile);
        Map<String,Double> result = solver.getResult();
        parseWeight = solver.getObjective();
        
        deleter.delayedDelete(tempDir);
        return result;
    }

    /**
     * Extracted method for override in InitializedIlpViterbiParserWithDeltas
     */
    protected ZimplSolver getZimplSolver(File tempDir) {
        ZimplSolver solver = new ZimplSolver(tempDir, ilpSolverFactory.getInstance(tempDir));
        return solver;
    }
    
    public double getLastParseWeight() {
        return parseWeight;
    }
    
    public File getWorkspace() {
        return workspace;
    }

    protected File encode(File tempDir, SentenceCollection sentences, Model model) {
        try {
            // Encode sentence
            encodeSentences(tempDir, sentences);
            
            // Encode model 
            encodeModel(tempDir, model, sentences);
            
            // Create .zpl file
            File zimplFile = createZimplFile(tempDir);
            
            return zimplFile;

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected File createZimplFile(File tempDir) throws FileNotFoundException {
        File zimplFile = new File(tempDir, "parse.zpl");
        PrintWriter zimplWriter;
        zimplWriter = new PrintWriter(zimplFile);
        zimplWriter.write(codeMap.getCodeSnippet("setup"));
        zimplWriter.write(codeMap.getCodeSnippet("weights"));
        if (formulation != IlpFormulation.MFLOW_NONPROJ && formulation != IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(codeMap.getCodeSnippet("deptree-general"));
        }
        if (formulation == IlpFormulation.FLOW_PROJ || formulation == IlpFormulation.FLOW_PROJ_LPRELAX) {
            zimplWriter.write(codeMap.getCodeSnippet(IlpFormulation.FLOW_NONPROJ));
        } else if (formulation == IlpFormulation.MFLOW_PROJ) {
            zimplWriter.write(codeMap.getCodeSnippet(IlpFormulation.MFLOW_NONPROJ));
        }
        String formulationId = formulation.toString().replace("-lprelax","");
        zimplWriter.write(codeMap.getCodeSnippet(formulationId));
        zimplWriter.write(codeMap.getCodeSnippet("dmv-objective-support"));
        // The -modelparam version is slightly slower but is correct on zero stop probabilities
        zimplWriter.write(codeMap.getCodeSnippet("dmv-objective-modelparam"));
        zimplWriter.close();
        return zimplFile;
    }

    private void encodeSentences(File tempDir, SentenceCollection sentences) throws FileNotFoundException {
        File sentFile = new File(tempDir, "input.sent");
        PrintWriter sentWriter = new PrintWriter(sentFile);
        for(int s=0; s<sentences.size(); s++) {
            Sentence sentence = sentences.get(s);
            sentWriter.format("%d %d %s\n", s, ZIMPL_WALL_POSITION, WallDepTreeNode.WALL_ID);
            for (int i=0; i<sentence.size(); i++) {
                Label label = sentence.get(i); 
                // Must add one to each word position
                sentWriter.format("%d %d \"%s\"\n", s, i+1, label.getLabel());
            }
        }
        sentWriter.close();
    }

    protected void encodeModel(File tempDir, Model model, SentenceCollection sentences) throws FileNotFoundException {
        //TODO: handle more than just the DMV
        DmvModel dmv = (DmvModel)model;
        encodeDmv(tempDir, dmv);
    }

    protected void encodeDmv(File tempDir, DmvModel dmv) throws FileNotFoundException {
        encodeStopWeights(tempDir, dmv);
        encodeChooseWeights(tempDir, dmv);
    }

    protected void encodeStopWeights(File tempDir, DmvModel dmv) throws FileNotFoundException {
        File stopWeightsFile = new File(tempDir, "input.stopweights");
        PrintWriter stopWeightsWriter = new PrintWriter(stopWeightsFile);
        // Decision (root) - this is required because of the ZIMPL encoding.
        // TODO: Remove this once we fix the ZIMPL encoding.
        for (Lr lr : Lr.values()) {
            for (boolean adjacent : DmvModel.ADJACENTS) {
                String leftRight = lr.toString();
                // TODO: This integer encoding of adjacency is the reverse from Constants.END and Constants.CONT.
                int adja = adjacent ? 1 : 0;
                double weight;
                if (lr == Lr.RIGHT) {
                    // We should always generate one child on the right and then stop.
                    weight = adjacent ? 0.0 : 1.0;
                } else {
                    // We should always stop immediately on the left.
                    weight = adjacent ? 1.0 : 0.0;
                }
                double logWeightStop = Utilities.logForIlp(weight);
                double logWeightNotStop = Utilities.logForIlp(1.0 - weight);
                stopWeightsWriter.format("\"%s\" %s %d %.13E %.13E %.13E\n", WallDepTreeNode.WALL_LABEL.getLabel(), leftRight, adja, weight, logWeightStop, logWeightNotStop);
            }
        }
        // Decision (normal)
        for (Label parent : dmv.getVocab()) {
            for (Lr lr : Lr.values()) {
                for (boolean adjacent : DmvModel.ADJACENTS) {
                    String leftRight = lr.toString();
                    double weight = Utilities.exp(dmv.getStopWeight(parent, lr, adjacent));
                    double logWeightStop = Utilities.logForIlp(weight);
                    double logWeightNotStop = Utilities.logForIlp(1.0 - weight);
                    // TODO: This integer encoding of adjacency is the reverse from Constants.END and Constants.CONT.
                    int adja = adjacent ? 1 : 0;
                    stopWeightsWriter.format("\"%s\" %s %d %.13E %.13E %.13E\n", parent.getLabel(), leftRight, adja, weight, logWeightStop, logWeightNotStop);
                }
            }
        }
        stopWeightsWriter.close();
    }
        
    private void encodeChooseWeights(File tempDir, DmvModel dmv) throws FileNotFoundException {
        File chooseWeightsFile = new File(tempDir, "input.chooseweights");
        PrintWriter chooseWeightsWriter = new PrintWriter(chooseWeightsFile);
        // Root
        for (Label child : dmv.getVocab()) {
            double weight = Utilities.exp(dmv.getRootWeight(child));
            double logWeight = Utilities.logForIlp(weight);
            chooseWeightsWriter.format("\"%s\" \"%s\" \"%s\" %.13E %.13E\n", 
                    WallDepTreeNode.WALL_LABEL.getLabel(), Lr.RIGHT.toString(), child.getLabel(), weight, logWeight);
            // TODO: Remove this once we've updated the zimpl code never use these weights. This is a dummy weight. 
            chooseWeightsWriter.format("\"%s\" \"%s\" \"%s\" %.13E %.13E\n", 
                    WallDepTreeNode.WALL_LABEL.getLabel(), Lr.LEFT.toString(), child.getLabel(), weight, logWeight);
        }
        // Child
        for (Label child : dmv.getVocab()) {
            for (Label parent : dmv.getVocab()) {
                for (Lr lr : Lr.values()) {
                    double weight = Utilities.exp(dmv.getChildWeight(parent, lr, child));
                    double logWeight = Utilities.logForIlp(weight);
                    chooseWeightsWriter.format("\"%s\" \"%s\" \"%s\" %.13E %.13E\n", 
                            parent.getLabel(), lr, child.getLabel(), weight, logWeight);
                }
            }
        }
        chooseWeightsWriter.close();
    }

    protected DepTreebank decode(SentenceCollection sentences, Map<String,Double> result) {
        DepTreebank depTreebank = new DepTreebank(sentences.getLabelAlphabet());
        
        int[][] parents = new int[sentences.size()][];
        for (int i=0; i<sentences.size(); i++) {
            parents[i] = new int[sentences.get(i).size()];
            Arrays.fill(parents[i], DepTree.EMPTY_POSITION);
        }
        
        for (Entry<String,Double> entry : result.entrySet()) {
            String zimplVar = entry.getKey();
            Double value = entry.getValue();
            String[] splits = zimplVarRegex.split(zimplVar);
            String varType = splits[0];
            if (varType.equals("arc")) {
                int sentId = Integer.parseInt(splits[1]);
                int parent = Integer.parseInt(splits[2]);
                int child = Integer.parseInt(splits[3]);
                long longVal = Math.round(value);
                if (longVal == 1) {
                    // Must subtract one from each position
                    parents[sentId][child-1] = parent-1;
                }
            }
        }
        
        for (int i=0; i<sentences.size(); i++) {
            DepTree tree = new DepTree(sentences.get(i), parents[i], formulation.isProjective());
            depTreebank.add(tree);
        }
        
        return depTreebank;
    }
    
    protected RelaxedDepTreebank relaxedDecode(SentenceCollection sentences, Map<String,Double> result) {
        // TODO: if we switch to handling semi-supervised corpora, then remove the next line.
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);
        RelaxedDepTreebank relaxTreebank = new RelaxedDepTreebank(corpus);
        
        for (Entry<String,Double> entry : result.entrySet()) {
            String zimplVar = entry.getKey();
            Double value = entry.getValue();
            String[] splits = zimplVarRegex.split(zimplVar);
            String varType = splits[0];
            if (varType.equals("arc")) {
                int sentId = Integer.parseInt(splits[1]);
                int parent = Integer.parseInt(splits[2]);
                int child = Integer.parseInt(splits[3]);
                if (parent == 0) {
                    relaxTreebank.getFracRoots()[sentId][child-1] = value;
                } else if (child == 0) {
                    // Skip the arcs that would indicate the parent having a head.
                    assert(Utilities.equals(value, 0.0, 1e-9));
                } else {
                    relaxTreebank.getFracChildren()[sentId][parent-1][child-1] = value;
                }
            }
        }
        
        return relaxTreebank;
    }

    @Override
    public void reset() {
        throw new RuntimeException("not implemented");
    }
    
}
