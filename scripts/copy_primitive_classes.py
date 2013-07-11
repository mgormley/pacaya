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
        
def copy_pair(dest_key, dest_val):
    tds = get_typedefs()
    src_key = tds.get("long")
    src_val = tds.get("double")
    
    repls = []
    # Prepend some replacements for generics in the unit tests. 
    if dest_key == "Int":
        repls += [("<Long,", "<Integer,")]
    if dest_val == "Int":
        repls += ["Double>", "Integer>"]
    # Conditional replacements
    if dest_val.is_integral:
        repls += [(", double delta", ""),
                  (", delta", ""),
                  (", double zeroThreshold", ""),
                  (", zeroThreshold", ""),
                  (", 1e-13", ""),
                  ]
    # Add the primary replacements.
    repls += get_typedef_repls(src_key, dest_key)
    repls += get_typedef_repls(src_val, dest_val)
    repls += [("getIntIndexArray", "getIndexArray"),
             ("int serialVersionUID", "long serialVersionUID"),
             ]
    # Other regex replacements
    add_re_subs = [(r"SafeCast\.safeIntToInt\(([^\)]+)\)", r"\1"),
               ]
    
    # Convert repls to regex replacements.
    re_subs = [(re.escape(k), v) for k, v in repls]
    re_subs += add_re_subs
    assert len(re_subs) == len(repls) + len(add_re_subs)
        
    classes = [
               "edu.jhu.util.vector.LongDoubleMap",
               "edu.jhu.util.vector.LongDoubleEntry",
                 "edu.jhu.util.vector.SortedLongDoubleMap",
                 "edu.jhu.util.vector.SortedLongDoubleMapTest",
                 "edu.jhu.util.vector.SortedLongDoubleVector",
                 "edu.jhu.util.vector.SortedLongDoubleVectorTest",
                 "edu.jhu.util.collections.PLongDoubleHashMap",
                 "edu.jhu.util.collections.PLongDoubleHashMapTest",
                 # "edu.jhu.util.collections.PLongHashSet",
                 ]
    java_dir = os.path.join("src", "main", "java")
    src_files = [os.path.join(java_dir, c.replace(".", "/") + ".java") for c in classes]
    dest_files = map(lambda f : replace_all(repls, f), src_files)
    for sf, df in zip(src_files, dest_files):
        print "Current destination file:", df
        dest_str = file_to_str(sf)
        dest_str = re_sub_all(re_subs, dest_str)
        str_to_file(dest_str, df)
    
class TypeDef:
    
    def __init__(self, name, prim, const, is_integral):
        self.name = name
        self.prim = prim
        self.const = const
        self.is_integral = is_integral
        
def get_typedef_repls(src, dest):
    return [(src.name, dest.name),
            (src.prim, dest.prim),
            (src.const, dest.const)]
    
def get_typedefs():
    return {"int" : TypeDef("Int", "int", "INT", True),
            "long" : TypeDef("Long", "long", "LONG", True),
            "double" : TypeDef("Double", "double", "DOUBLE", False),
        }
        
if __name__ == "__main__":
    tds = get_typedefs()
    copy_pair(tds.get("int"), tds.get("double"))
    copy_pair(tds.get("int"), tds.get("long"))
    
    #TODO: Should only make a subset:
    #copy_pair(tds.get("long"), tds.get("int"))

    #TODO: IntInt sort of works, but requires the removal of some duplicate methods/constructors:
    # copy_pair(tds.get("int"), tds.get("int"))

