package edu.jhu.nlp.data.concrete;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;

import edu.jhu.hlt.concrete.Communication;
import edu.jhu.hlt.concrete.Constituent;
import edu.jhu.hlt.concrete.Dependency;
import edu.jhu.hlt.concrete.DependencyParse;
import edu.jhu.hlt.concrete.EntityMention;
import edu.jhu.hlt.concrete.EntityMentionSet;
import edu.jhu.hlt.concrete.MentionArgument;
import edu.jhu.hlt.concrete.Parse;
import edu.jhu.hlt.concrete.Section;
import edu.jhu.hlt.concrete.SectionSegmentation;
import edu.jhu.hlt.concrete.Sentence;
import edu.jhu.hlt.concrete.SentenceSegmentation;
import edu.jhu.hlt.concrete.SituationMention;
import edu.jhu.hlt.concrete.SituationMentionSet;
import edu.jhu.hlt.concrete.TaggedToken;
import edu.jhu.hlt.concrete.Token;
import edu.jhu.hlt.concrete.TokenList;
import edu.jhu.hlt.concrete.TokenRefSequence;
import edu.jhu.hlt.concrete.TokenTagging;
import edu.jhu.hlt.concrete.Tokenization;
import edu.jhu.hlt.concrete.TokenizationKind;
import edu.jhu.hlt.concrete.UUID;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.NerMentions;
import edu.jhu.nlp.data.RelationMention;
import edu.jhu.nlp.data.RelationMentions;
import edu.jhu.nlp.data.Span;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.relations.RelationsEncoder;
import edu.jhu.nlp.relations.RelationsOptions;
import edu.jhu.parse.cky.data.NaryTree;
import edu.jhu.prim.Primitives.MutableInt;
import edu.jhu.prim.map.IntIntHashMap;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.Lambda.FnO1ToVoid;

/**
 * Reader of Concrete protocol buffer files.
 *  
 * @author mgormley
 */
public class ReConcreteReader extends ConcreteReader {

    private static final Logger log = Logger.getLogger(ReConcreteReader.class);

    public ReConcreteReader(ConcreteReaderPrm prm) {
        super(prm);
    }

    /**
     * Converts each sentence in communication to a {@link AnnoSentence}
     * and adds it to annoSents.
     */
    public void addSentences(Communication comm, AnnoSentenceCollection aSents) {
        AnnoSentenceCollection tmpSents = new AnnoSentenceCollection(); 
        super.addSentences(comm, tmpSents);
        
        if (comm.getEntityMentionSetsSize() > 0 && comm.getSituationMentionSetsSize() > 0) {                
            // TODO: This is a hack to replicate the PM13 setting. Think of a better way to incorporate this.
            for (AnnoSentence aSent : tmpSents) {     
                if (RelationsOptions.shortenEntityMentions) {
                    for (NerMention m : aSent.getNamedEntities()) {
                        // Set the end of the span to be the head token.
                        m.getSpan().setEnd(m.getHead()+1);
                    }
                    aSent.getNamedEntities().sort();                       
                }
                // Add the named entity pairs.
                RelationsEncoder.addNePairsAndRelLabels(aSent);
            }
            tmpSents = RelationsEncoder.getSingletons(tmpSents);
            // Deterministically shuffle the positive and negative examples for this communication.
            Collections.shuffle(tmpSents, new Random(1234567890));
        }
        
        aSents.addAll(tmpSents);
    }
    
}
