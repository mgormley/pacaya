package edu.jhu.featurize;

public class MutableInt {
    
        int value = 1; // Start at 1 since we're counting
        public void increment () { ++value;      }
        public int  get ()       { return value; }

}
