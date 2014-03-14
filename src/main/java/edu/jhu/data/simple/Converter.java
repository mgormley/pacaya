package edu.jhu.data.simple;

public interface Converter<X,Y> {
    Y convert(X x);
}