package edu.jhu.nlp.data.convert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.data.conll.CoNLL09FileReader;
import edu.jhu.nlp.data.conll.CoNLL09Sentence;
import edu.jhu.nlp.data.conll.CoNLL09Token;
import edu.jhu.nlp.data.conll.ConllLiteSentence;
import edu.jhu.nlp.data.conll.ConllLiteToken;
import edu.jhu.nlp.data.conll.ConllLiteWriter;
import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.util.cli.ArgParser;
import edu.jhu.util.cli.Opt;

/**
 * Converts CoNLL-2009 format to CoNLL Lite.
 *  
 * @author mgormley
 *
 */
public class Conll09ToConllLite {

    private static final Logger log = LoggerFactory.getLogger(Conll09ToConllLite.class);

    @Opt(hasArg = true, required = true, description = "CoNLL 09 input file")
    public static File input;
    @Opt(hasArg = true, required = true, description = "CoNLL Lite output file")
    public static File output;

    public static ConllLiteSentence conll09ToConllLite(CoNLL09Sentence s09) {
        SrlGraph srl = s09.getSrlGraph();
        ArrayList<ConllLiteToken> tokens = new  ArrayList<ConllLiteToken>();
        for (int i=0; i<s09.size(); i++) {
            CoNLL09Token t09 = s09.get(i);
            tokens.add(new ConllLiteToken(Integer.toString(i+1), t09.getForm(), new ArrayList<String[]>(), new ArrayList<String>()));
        }
        ConllLiteSentence slite  = new ConllLiteSentence(tokens);
        slite.setEdgesFromSrlGraph(srl);
        return slite;
    }
    
    public void run() throws IOException {
        ConllLiteWriter writer = new ConllLiteWriter(output);
        CoNLL09FileReader reader = new CoNLL09FileReader(input);
        for (CoNLL09Sentence s09 : reader) {
            ConllLiteSentence slite = conll09ToConllLite(s09);
            writer.write(slite);
        }        
        reader.close();
        writer.close();
    }
    
    public static void main(String[] args) throws IOException {
        ArgParser parser = new ArgParser(Conll09ToConllLite.class);
        parser.registerClass(Conll09ToConllLite.class);
        try {
            parser.parseArgs(args);
        } catch (ParseException e) {
            log.error(e.getMessage());
            parser.printUsage();
            System.exit(1);
        }
        
        Conll09ToConllLite pipeline = new Conll09ToConllLite();
        pipeline.run();
    }

}
