package edu.jhu.nlp.depparse;

import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.data.simple.CorpusHandler;
import edu.jhu.nlp.joint.JointNlpRunner;
import edu.jhu.prim.map.IntIntEntry;
import edu.jhu.prim.map.IntIntHashMap;
import edu.jhu.util.cli.ArgParser;

/**
 * Prints stats about a dependency tree corpus.
 * 
 * Example usage for CoNLL-X data:
 * 
 * find data/conllx/CoNLL-X/train -name "*.conll" | xargs -n 1 \
 *    java edu.jhu.nlp.depparse.TreeStatsPrinter --trainType CONLL_X \
 *    --trainUseCoNLLXPhead false --train
 * 
 * @author mgormley
 */
public class TreeStatsPrinter {

    private static final Logger log = LoggerFactory.getLogger(TreeStatsPrinter.class);

    public void run() throws IOException {
        CorpusHandler copora = new CorpusHandler();
        AnnoSentenceCollection sents = copora.getTrainGold();
        
        int numSinglyRooted = 0;
        int numMissingEdges = 0;
        IntIntHashMap numEdgesToWallHistogram = new IntIntHashMap();
        for (int i=0; i<sents.size(); i++) {
            AnnoSentence sent = sents.get(i);
            int[] parents = sent.getParents();
            int numEdgesToWall = 0;
            int numEdgesToNothing = 0;
            for (int c=0; c<parents.length; c++) {
                int p = parents[c];
                if (p == -1) {
                    numEdgesToWall++;                    
                } else if (p == -2) {
                    numEdgesToNothing++;
                }
            }
            numEdgesToWallHistogram.add(numEdgesToWall, 1);
            if (numEdgesToWall == 1) {
                numSinglyRooted++;
            } else if (numEdgesToNothing > 0) {
                numMissingEdges++;
            }
        }
        int numTotal = sents.size();

        log.info(String.format("#total=%d", numTotal));
        log.info(String.format("#single-root=%d #percent-single-root=%.4g", numSinglyRooted, 
                (double) numSinglyRooted / numTotal));
        log.info(String.format("#multi-root=%d #percent-multi-root=%.4g", (numTotal - numSinglyRooted),
                (1.0 - ((double) numSinglyRooted / numTotal))));
        StringBuilder sb = new StringBuilder();
        for (IntIntEntry e : numEdgesToWallHistogram) {
            sb.append(String.format("%d:%d, ", e.index(), e.get()));
        }
        log.info("Num edges to wall histogram: " + sb);
        log.info(String.format("#missing-edges=%d", numMissingEdges));
    }
    
    public static void main(String[] args) {
        try {
            ArgParser parser = new ArgParser(JointNlpRunner.class);
            parser.addClass(CorpusHandler.class);
            try {
                parser.parseArgs(args);
            } catch (ParseException e) {
                log.error(e.getMessage());
                parser.printUsage();
                System.exit(1);
            }
            
            TreeStatsPrinter pipeline = new TreeStatsPrinter();
            pipeline.run();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
    
}
