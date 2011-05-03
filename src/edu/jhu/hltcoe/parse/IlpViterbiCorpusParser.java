package edu.jhu.hltcoe.parse;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.dna.common.statistic.Stopwatch;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.ClGurobiIlpSolver;
import edu.jhu.hltcoe.ilp.ZimplSolver;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.util.Command;
import edu.jhu.hltcoe.util.Time;

public class IlpViterbiCorpusParser extends IlpViterbiParser {

    private static Logger log = Logger.getLogger(IlpViterbiCorpusParser.class);
    
    public IlpViterbiCorpusParser(IlpFormulation formulation) {
        super(formulation);
    }
    
    @Override
    public DepTreebank getViterbiParse(SentenceCollection sentences, Model model) {
        Stopwatch stopwatch = new Stopwatch();
        stopwatch.start();
        
        // Create workspace
        File tempDir = Command.createTempDir("ilp_parse", workspace);
        
        // Encode sentences and model
        File zimplFile = encode(tempDir, sentences, model);
        
        // Run zimpl and then ILP solver
        ZimplSolver solver = new ZimplSolver(tempDir, new ClGurobiIlpSolver(tempDir));
        solver.solve(zimplFile);
        Map<String,Double> result = solver.getResult();
        
        // Decode parses
        DepTreebank depTreebank = decode(sentences, result);
        
        stopwatch.stop();
        log.debug(String.format("Avg parse time: %.3f Num sents: %d", 
                Time.totMs(stopwatch) / sentences.size(), 
                sentences.size()));
        log.debug(String.format("Tot parse time: %.3f Num sents: %d", 
                Time.totMs(stopwatch), 
                sentences.size()));
        return depTreebank;
    }
    
    public DepTree getViterbiParse(Sentence sentence, Model model) {
        throw new NotImplementedException();
    }

}
