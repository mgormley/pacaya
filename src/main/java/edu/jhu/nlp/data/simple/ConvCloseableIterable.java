package edu.jhu.nlp.data.simple;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

public class ConvCloseableIterable<X,Y> implements CloseableIterable<Y> {

    private final Iterator<X> iter;
    private final Converter<X, Y> conv;
    private Closeable closeable;

    public ConvCloseableIterable(CloseableIterable<X> citer, Converter<X,Y> conv) {
        this.closeable = citer;
        this.iter = citer.iterator();
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
    
    public void close() throws IOException {
        closeable.close();
    }
    
    public static <T,S> ConvCloseableIterable<T,S> getInstance(CloseableIterable<T> iter, Converter<T,S> conv) {
        return new ConvCloseableIterable<T, S>(iter, conv);
    }
}