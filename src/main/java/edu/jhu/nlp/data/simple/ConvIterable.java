package edu.jhu.nlp.data.simple;

import java.util.Iterator;

public class ConvIterable<X,Y> implements Iterable<Y> {

    private final Iterator<X> iter;
    private final Converter<X, Y> conv;

    public ConvIterable(Iterable<X> iter, Converter<X,Y> conv) {
        this.iter = iter.iterator();
        this.conv = conv;            
    }
    
    @Override
    public Iterator<Y> iterator() {
        return new YIter();
    }
    
    private class YIter implements Iterator<Y> {

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Y next() {
            return conv.convert(iter.next());
        }

        @Override
        public void remove() {
            iter.remove();
        }
    }
    
    public static <T,S> ConvIterable<T,S> getInstance(Iterable<T> iter, Converter<T,S> conv) {
        return new ConvIterable<T, S>(iter, conv);
    }
}