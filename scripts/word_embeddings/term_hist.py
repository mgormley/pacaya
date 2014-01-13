import codecs

def compute_hit_rate(word_set, conll_words, count_words=False):
    if count_words:
        word_count = len(conll_words)
    else:
        word_count = 0
        for word in conll_words:
            for c in word:
                word_count += 1

    misses = []
    hit_count = 0
    for word in conll_words:
        for c in word:
            if c in word_set:
                hit_count += 1
            else:
                misses.append(word)
        #if word in word_set:
        #    hit_count += 1
        #else:
        #        misses.append(word)

    print "hit_count=%d \nword_count=%d \nhit_rate=%f" % (hit_count, word_count, 1.0 * hit_count / word_count)
    print "First N misses:"#, misses[:100]
    for word in misses[:100]:
        print word,
    print ""

def get_word_set(lang_short, source):
    if source == "polyglot":
        # Read polyglot embeddings
        words, embeddings = pickle.load(open('./polyglot/polyglot-%s.pkl' % (lang_short), 'rb'))
        print("Emebddings shape is {}".format(embeddings.shape))    
    else:
        # Read brown clusters
        brown_file = "./bc_out_250/full.txt_%s_256/paths" % (lang_short)
        brown_in = codecs.open(brown_file, 'r', 'utf-8')
        words = []
        for line in brown_in:
            splits = line.split('\t')
            words.append(splits[1])
    word_set = set(words)
    return word_set

def compute_conll_hit_rate(lang_short, lang_long, source):
    print "\n\nCurrent language: ",lang_long

    word_set = get_word_set(lang_short, source)

    token_count = 0
    if lang_short in ['es', 'ca', 'de', 'cs']:
        num = 3
    else:
        num = 4
    conll_en = "/export/common/data/corpora/LDC/LDC2012T0%d/data/CoNLL2009-ST-%s/CoNLL2009-ST-%s-train.txt" % (num, lang_long, lang_long)
    conll_in = codecs.open(conll_en, 'r', 'utf-8')

    conll_words = []
    for line in conll_in:
        splits = line.split('\t')
        if len(splits) >= 2:
            word = splits[1]
            conll_words.append(word)
            token_count += 1
    print "token_count=%d" % (token_count)

    print "Token hit rate for", lang_long
    compute_hit_rate(word_set, conll_words)
    print "Type hit rate for", lang_long
    compute_hit_rate(word_set, set(conll_words))


if __name__ == "__main__":
    lang_pairs = [('en', 'English'), ('es', 'Spanish'), ('ca', 'Catalan'), ('cs', 'Czech'), ('zh', 'Chinese'), ('zh_char', 'Chinese'), ('de', 'German')]
    #lang_pairs = [('zh', 'Chinese'), ('zh_char', 'Chinese')]
    for lang_short, lang_long in lang_pairs:
        try:
            compute_conll_hit_rate(lang_short, lang_long, "brown")
        except Exception as e:
            print "Failed on", lang_long
            print e
        finally:
            print ""
            
