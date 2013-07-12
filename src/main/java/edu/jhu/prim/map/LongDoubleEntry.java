package edu.jhu.prim.map;

/**
 * An entry in a primitives map from longs to doubles.
 * @author mgormley
 */
public interface LongDoubleEntry {
	long index();
	double get();
}