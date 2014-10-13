# Replaces existing template unigrams (v1) with a new naming scheme (v2).
# usage: python repl_tpls.py v1.txt v2.txt
#

import sys
import os
from glob import glob

def cmp_len(a, b):
    return len(a) - len(b)

def sort_by_len(ll):
    ll.sort(cmp=cmp_len)

def get_tpls(path):
    with open(path, "r") as ff:
        tpls = ff.readlines()
        # Remove whitespace
        return [x.strip() for x in tpls]

if __name__ == "__main__":    
    v1 = sys.argv[1]
    v2 = sys.argv[2]

    
    v1_tpls = get_tpls(v1)
    v2_tpls = get_tpls(v2)
    
    repls = zip(v1_tpls,v2_tpls)

    def cmp_len_first(a, b):
        return len(b[0]) - len(a[0])
    repls.sort(cmp=cmp_len_first)
    
    pre_repls = [
        (".dir.deprel.", ".deprel.dir."),
        (".dir.pos.", ".pos.dir."),
        ("p.word.1", "p.1.word"),
        ("p.word.-1", "p.-1.word"),
        ("2gram.bc1", "bc1.2gram"),
        ("3gram.bc1", "bc1.3gram"),
        ("p.currentSense", "lemmaSense(p)"),
        ("p.lemmaSense", "lemmaSense(p)"),
    ]
    repls = pre_repls + repls
    
    print "First 10 in map:"
    for a,b in repls[0:10]:
        print a,b    
    assert dict(repls)["p.morpho"] == "morpho(p)"
        
    file_list = glob("src/main/resources/edu/jhu/nlp/features/*.txt")

    print "Num files:", len(file_list)
    print "Num repls:", len(repls)
    print "Num cross:", len(file_list)*len(repls)

    i = 0
    for ff in file_list:
        print "File:",ff
        with open(ff, "r") as f:
            flines1 = f.readlines()
        flines2 = flines1
        for a,b in repls:
            flines = flines2
            flines2 = []
            for fstr in flines:
                if not fstr.startswith("#"):
                    fstr = fstr.replace(a,b)
                flines2.append(fstr)                
            i+=1
            if i % 10000 == 0:
                print i

        with open(ff, "w") as f:
            for fstr in flines2:
                f.write(fstr)
            

