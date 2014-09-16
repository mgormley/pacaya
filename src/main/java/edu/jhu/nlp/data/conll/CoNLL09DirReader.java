package edu.jhu.nlp.data.conll;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.util.files.FileListIterator;
import edu.jhu.util.files.Files;

/**
 * Reads a file or directory of CoNLL-2009 files.
 * 
 * @author mgormley
 *
 */
// TODO: Decide whether it even makes sense to have this class.
@Deprecated
public class CoNLL09DirReader implements Iterable<CoNLL09Sentence> {

    private static final Logger log = Logger.getLogger(CoNLL09DirReader.class);

    private List<File> files;
    
    public CoNLL09DirReader(File path) {
        files = Files.getMatchingFiles(path, ".*\\.conll");
    }
    
    public CoNLL09DirReader(String path) {
        this(new File(path));
    }

    @Override
    public Iterator<CoNLL09Sentence> iterator() {
        if (files == null) {
            throw new IllegalStateException("loadPath must be called first");
        }
        
        return new FileListIterator<CoNLL09Sentence>(files) {
            @Override
            public Iterator<CoNLL09Sentence> getIteratorInstance(File file) {
                try {
                    return new CoNLL09FileReader(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
    
}
