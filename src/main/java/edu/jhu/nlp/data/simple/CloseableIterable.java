package edu.jhu.nlp.data.simple;

import java.io.Closeable;

public interface CloseableIterable<T> extends Closeable, Iterable<T> {

}
