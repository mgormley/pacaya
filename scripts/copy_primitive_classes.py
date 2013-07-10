#!/usr/bin/python
#
# Example usage:
# find ./src/ilog/cplex/ | xargs -n 1 python ./scripts/cleanup.py

import sys
import re
import os

def file_to_str(filename):
    # Read the string from a file.
    f = open(filename, 'r')
    s = ""
    for line in f:
        s = s + line
    return s
    
def str_to_file(s, filename):
    # Write the new string to that file
    f = open(filename, 'w')
    f.write(s)
    f.close()
    
def replace_all(repls, s):
    for k,v in repls:
        s = s.replace(k, v)
    return s

def re_sub_all(re_subs, s):
    for k,v in re_subs:
        s, num_subs = re.subn(k, v, s)
        print "Number of substitutions for %s-->%s: %d" % (k, v, num_subs)
    return s
    
def copy_pair(dest_key, dest_val, dest_key_prim, dest_val_prim, dest_key_const, dest_val_const):
    repls = []
    # Prepend some replacements for generics in the unit tests. 
    if dest_key == "Int":
        repls += [("<Long,", "<Integer,")]
    if dest_val == "Int":
        repls += ["Double>", "Integer>"]
    # Add the primary replacements.
    repls += [("Long", dest_key),
             ("Double", dest_val),
             ("long", dest_key_prim),
             ("double", dest_val_prim),
             ("LONG", dest_key_const),
             ("DOUBLE", dest_val_const),
             ("getIntIndexArray", "getIndexArray"),
             ("int serialVersionUID", "long serialVersionUID"),
             ]
    # Other regex replacements
    add_re_subs = [(r"SafeCast\.safeIntToInt\(([^\)]+)\)", r"\1"),
               ]
    # Convert repls to regex replacements.
    re_subs = [(re.escape(k), v) for k, v in repls]
    re_subs += add_re_subs
    assert len(re_subs) == len(repls) + len(add_re_subs)
        
    classes = ["edu.jhu.util.vector.LongDoubleMap",
               "edu.jhu.util.vector.LongDoubleEntry",
                 "edu.jhu.util.vector.SortedLongDoubleMap",
                 "edu.jhu.util.vector.SortedLongDoubleMapTest",
                 "edu.jhu.util.vector.SortedLongDoubleVector",
                 "edu.jhu.util.vector.SortedLongDoubleVectorTest",
                 "edu.jhu.util.collections.PLongDoubleHashMap",
                 "edu.jhu.util.collections.PLongDoubleHashMapTest",
                 #"edu.jhu.util.collections.PLongHashSet",
                 ]
    java_dir = os.path.join("src", "main", "java")
    src_files = [os.path.join(java_dir, c.replace(".", "/") + ".java") for c in classes]
    dest_files = map(lambda f : replace_all(repls, f), src_files)
    for sf, df in zip(src_files, dest_files):
        print "Current destination file:", df
        dest_str = file_to_str(sf)
        dest_str = re_sub_all(re_subs, dest_str)
        str_to_file(dest_str, df)
    

if __name__ == "__main__":
    copy_pair("Int", "Double", "int", "double", "INT", "DOUBLE")

