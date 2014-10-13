package edu.jhu.nlp.data.simple;

public interface Converter<X,Y> {
    Y convert(X x);
}