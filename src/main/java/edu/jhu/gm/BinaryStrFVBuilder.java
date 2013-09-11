package edu.jhu.gm;

import java.util.Iterator;

import edu.jhu.prim.map.IntDoubleEntry;
import edu.jhu.util.Alphabet;

/**
 * For building large (e.g. 20000+) sparse feature vectors consisting of 
 * binary features represented as strings.
 * 
 * @author mgormley
 */
public class BinaryStrFVBuilder implements Iterable<String> {

    private Alphabet<String> alphabet;
    private FeatureVectorBuilder fvb;
    
    public BinaryStrFVBuilder(Alphabet<String> alphabet) {
        this.alphabet = alphabet;
        this.fvb = new FeatureVectorBuilder();
    }

    public void add(String feat) {
        int index = alphabet.lookupIndex(feat);
        fvb.put(index, 1.0);
    }
    
    public FeatureVector toFeatureVector() {
        return fvb.toFeatureVector();
    }

    @Override
    public Iterator<String> iterator() {
        return new StrIter(fvb.iterator());
    }
    
    private class StrIter implements Iterator<String> {
                
        private Iterator<IntDoubleEntry> iter;

        public StrIter(Iterator<IntDoubleEntry> iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public String next() {
            IntDoubleEntry e = iter.next();
            return alphabet.lookupObject(e.index());
        }

        @Override
        public void remove() {
            throw new RuntimeException("not implemented.");
        }
        
    }

    public int size() {
        return fvb.size();
    }

    public FeatureVectorBuilder getFvb() {
        return fvb;
    }
    
}
