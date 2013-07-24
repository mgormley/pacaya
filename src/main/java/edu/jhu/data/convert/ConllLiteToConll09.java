package edu.jhu.data.convert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.data.conll.CoNLL09Writer;
import edu.jhu.data.conll.ConllLiteFileReader;
import edu.jhu.data.conll.ConllLiteSentence;
import edu.jhu.data.conll.ConllLiteToken;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;

/**
 * Converts CoNLL Lite format to CoNLL-2009.
 *  
 * @author mgormley
 *
 */
public class ConllLiteToConll09 {

    private static final Logger log = Logger.getLogger(ConllLiteToConll09.class);

    @Opt(hasArg = true, required = true, description = "CoNLL Lite input file")
    public static File input;
    @Opt(hasArg = true, required = true, description = "CoNLL 09 output file")
    public static File output;

    public static CoNLL09Sentence conllLiteToConll09(ConllLiteSentence slite) {
        SrlGraph srl = slite.getSrlGraph();
        ArrayList<CoNLL09Token> tokens = new  ArrayList<CoNLL09Token>();
        for (int i=0; i<slite.size(); i++) {
            ConllLiteToken tlite = slite.get(i);
            // Just add "dummy" heads and pheads that form a  right branching structure.
            tokens.add(new CoNLL09Token(i+1, tlite.getForm(), "_", "_", "_", "_", null, null, i, i, "_", "_", false, "_", null));
        }
        CoNLL09Sentence s09 = new CoNLL09Sentence(tokens);
        s09.setColsFromSrlGraph(srl, false, true);
        return s09;
    }
    
    public void run() throws IOException {

        CoNLL09Writer writer = new CoNLL09Writer(output);
        ConllLiteFileReader reader = new ConllLiteFileReader(input);
        for (ConllLiteSentence slite : reader) {
            CoNLL09Sentence s09 = conllLiteToConll09(slite);
            writer.write(s09);
        }
        
        reader.close();
        writer.close();
    }
    
    public static void main(String[] args) throws IOException {
        ArgParser parser = new ArgParser(ConllLiteToConll09.class);
        parser.addClass(ConllLiteToConll09.class);
        try {
            parser.parseArgs(args);
        } catch (ParseException e) {
            log.error(e.getMessage());
            parser.printUsage();
            System.exit(1);
        }
                
        ConllLiteToConll09 pipeline = new ConllLiteToConll09();
        pipeline.run();
    }

}
