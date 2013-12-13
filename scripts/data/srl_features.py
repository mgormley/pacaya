# Script for printing out feature templates

props = "w l t bc0 bc1 m1 m2 m3 m4 dr".split()
#extra_props = "lc unk ch chpre_n chsuf_n".split()
#edge_props = props + "dir dr+dir".split()
coarse_props = "t bc0 bc1".split()
list_props = "dr t bc0 dr+dir t+dir bc0+dir".split()

pos0 = "p c".split()
pos_mods = "_{-1} _{1} _{head} _{lmd} _{rmd} _{lns} _{rns} _{lmc} _{rmc} _{lnc} _{rnc}".split()
pos1 = """first(t, VERB, path(p, root))
first(t, NOUN, path(p, root))
first(t, VERB, path(root, p))
first(t, NOUN, path(root, p))""".split("\n")
pos_lists = "line(p,c) children(p) noFarChildren(p) path(p,c) path(p,root) path(c,root) path(lca(p,c),root)".split()

list_mods = "seq bag noDup".split()

other_feats = "relative(p,c) distance(p,c) geneology(p,c) DepSubCat len(path(p,c)) p.voice+a.word p.voice+a.t 1,2,3-grams(path(p,c)).word/pos continuity(path(p,c))".split()

feats = []
opt = 1

if opt == 0:
    # All possible feature unigrams: (415 total)
    pos = []
    for s in pos0:
        pos.append(s)
        for m in pos_mods:
            pos.append(s+m)
    pos += pos1
    
    for s in pos:
        for p in props:
            feats.append("%s.%s" % (s,p))
            
    for s in pos_lists:
        for p in list_props:
            for l in list_mods:
                feats.append("%s.%s.%s" % (s,p,l))
    
    for f in other_feats:
        feats.append(f)
elif opt == 1:
    # Possibly a good set for use as feature unigrams: (125 total)
    pos = []
    for s in pos0:
        pos.append(s)
        for m in pos_mods:
            pos.append(s+m)
    pos += pos1

    for s in pos0:
        for p in props:
            feats.append("%s.%s" % (s,p))
            
    for s in pos1:
        for p in coarse_props:
            feats.append("%s.%s" % (s,p))

    for s in pos0:
        for m in pos_mods:
            for p in coarse_props:
                feats.append("%s.%s" % (s+m,p))

    for s in "line(p,c) children(p) path(p,c)".split():
        for p in list_props:
            for l in ["seq"]:
                feats.append("%s.%s.%s" % (s,p,l))
    
    for f in other_feats:
        feats.append(f)
elif opt == 2:
    # Restricted Set for actual use:
    pos = []
    for s in pos0:
        pos.append(s)
        for m in pos_mods:
            pos.append(s+m)
    pos += pos1

    for s in pos0:
        for p in props:
            feats.append("%s.%s" % (s,p))
            
    for s in pos1:
        for p in coarse_props:
            feats.append("%s.%s" % (s,p))

    for s in pos0:
        for m in pos_mods:
            for p in coarse_props:
                feats.append("%s.%s" % (s+m,p))

    for s in "line(p,c) children(p) path(p,c)".split():
        for p in list_props:
            for l in ["seq"]:
                feats.append("%s.%s.%s" % (s,p,l))
    
    for f in other_feats:
        feats.append(f)
    
for f in feats:
    print f
print ""

print "Num feature unigrams: ", len(feats)
        