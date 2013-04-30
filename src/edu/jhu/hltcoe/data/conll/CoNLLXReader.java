package edu.jhu.hltcoe.data.conll;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.util.Utilities;

public class CoNLLXReader implements Iterable<CoNLLXSentence> {

    private static final Logger log = Logger.getLogger(CoNLLXReader.class);

    private List<File> files;
    
    public CoNLLXReader(File path) {
        files = Utilities.getMatchingFiles(path, ".*\\.conll");
    }
    
    public CoNLLXReader(String path) {
        this(new File(path));
    }

    @Override
    public Iterator<CoNLLXSentence> iterator() {
        if (files == null) {
            throw new IllegalStateException("loadPath must be called first");
        }
        
        return new FileListIterator<CoNLLXSentence>(files) {
            @Override
            public Iterator<CoNLLXSentence> getIteratorInstance(File file) {
                return new CoNLLXIterator(file);
            }
        };
    }
    
    public class CoNLLXIterator implements Iterator<CoNLLXSentence> {

        private CoNLLXSentence sentence;
        private BufferedReader reader;

        public CoNLLXIterator(File file) {
            try {
                reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(file), "UTF-8"));
                next();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean hasNext() {
            return sentence != null;
        }

        @Override
        public CoNLLXSentence next() {
            try {
                CoNLLXSentence curSent = sentence;
                sentence = CoNLLXSentence.readCoNLLXSentence(reader);
                return curSent;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void remove() {
            throw new RuntimeException("not implemented");
        }

    }
    
}
