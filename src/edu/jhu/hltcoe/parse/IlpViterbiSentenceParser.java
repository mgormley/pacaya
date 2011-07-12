package edu.jhu.hltcoe.parse;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.ZimplSolver;
import edu.jhu.hltcoe.model.DmvModel;
import edu.jhu.hltcoe.model.DmvModelFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.util.Files;
import edu.jhu.hltcoe.util.Time;

/**
 * TODO: switch this to be a wrapper class for an object implementing the ViterbiParser interface
 * @author mgormley
 *
 */
public class IlpViterbiSentenceParser extends IlpViterbiParser implements ViterbiSentenceParser {

    private static Logger log = Logger.getLogger(IlpViterbiSentenceParser.class);
        
    public IlpViterbiSentenceParser(IlpFormulation formulation, IlpSolverFactory ilpSolverFactory) {
        super(formulation, ilpSolverFactory);
    }
    
    public DepTreebank getViterbiParse(SentenceCollection sentences, Model model) {
        // TODO: could be a field
        Stopwatch stopwatch = new Stopwatch();
        
        DepTreebank treebank = new DepTreebank();
        double totalParseWeight = 0.0;
        for (Sentence sentence: sentences) {
            stopwatch.start();
            DepTree tree = getViterbiParse(sentence, model);
            totalParseWeight += parseWeight;
            stopwatch.stop();
            treebank.add(tree);
            log.debug(String.format("Avg parse time: %.3f Num sents: %d", 
                    Time.avgMs(stopwatch),
                    stopwatch.getCount()));
        }
        log.debug(String.format("Tot parse time: %.3f", 
                Time.totMs(stopwatch)));
        
        parseWeight = totalParseWeight;
        return treebank;
    }
    
    @Override
    public DepTree getViterbiParse(Sentence sentence, Model model) {
        // Create workspace
        File tempDir = Files.createTempDir("ilp_parse", workspace);
        
        // Encode sentences and model
        File zimplFile = encode(tempDir, sentence, model);
        
        // Run zimpl and then ILP solver
        ZimplSolver solver = new ZimplSolver(tempDir, ilpSolverFactory.getInstance(tempDir));
        solver.solve(zimplFile);
        Map<String,Double> result = solver.getResult();
        parseWeight = solver.getObjective();

        // Decode parses
        DepTree tree = decode(sentence, result);
        return tree;
    }

    private File encode(File tempDir, Sentence sentence, Model model) {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(sentence);
        return encode(tempDir, sentences, model);
    }
    
    protected void encodeModel(File tempDir, Model model, SentenceCollection sentences) throws FileNotFoundException {
        DmvModel dmv = (DmvModel)model;
        // Keep only the weights relevant to the single sentence in the SentenceCollection
        WeightCopier weightCopier = new WeightCopier(dmv);
        DmvModel filteredDmv = (DmvModel)(new DmvModelFactory(weightCopier)).getInstance(sentences);
        encodeDmv(tempDir, filteredDmv);
    }
    
    private DepTree decode(Sentence sentence, Map<String,Double> result) {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(sentence);
        DepTreebank depTreebank = decode(sentences, result);
        assert(depTreebank.size() == 1);
        return depTreebank.get(0);
    }
    
}
