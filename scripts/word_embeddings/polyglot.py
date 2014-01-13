import pickle
import numpy
import codecs
import json
from collections import Counter

verbose = False
num_to_print = 20

def print_words(prefix, words):
    print prefix + " ",
    for word in words:
        print "'%s', " % (word),
    print ""

def compute_hit_rate(word_set, conll_words, lang_short, count_words=True, allow_subwords=True):
    if count_words:
        word_count = len(conll_words)
    else:
        # TODO: This is incorrect. We're counting the number of
        # character tokens in the word types, not the number of
        # character types.
        word_count = 0
        for word in conll_words:
            for c in word:
                word_count += 1

    misses = []
    hit_count = 0
    for word in conll_words:
        if count_words:
            if word in word_set:
                hit_count += 1
            else:
                hit = False
                if allow_subwords:
                    if lang_short == "es" or lang_short == "ca":
                        splits = word.split("_")
                    elif lang_short == "zh":
                        splits = word
                    if splits:
                        # for subword in word: #.split("_"): #Splitting on _ is for es/ca.
                        subword = splits[len(splits)-1] # Only take the last subword.
                        if subword in word_set:
                            hit_count += 1
                            hit = True
                if not hit:
                    misses.append(word)
        else:
            for c in word:
                if c in word_set:
                    hit_count += 1
                else:
                    misses.append(word)

    print "hit_count=%d \nword_count=%d \nhit_rate=%f" % (hit_count, word_count, 1.0 * hit_count / word_count)
    
    misses = Counter(misses).most_common(num_to_print)
    print "Top %d misses (escaped): %s" % (num_to_print, str(misses[:num_to_print]))
    misses = [word for word,count in misses]
    print_words("Top %d misses (unescaped): " % (num_to_print), misses[:num_to_print])
    
def get_word_set(lang_short, source):
    if source == "polyglot":
        # Read polyglot embeddings
        words, embeddings = pickle.load(open('./polyglot/polyglot-%s.pkl' % (lang_short), 'rb'))
        print("Emebddings shape is {}".format(embeddings.shape))    
    elif source == "brown":
        # Read brown clusters
        brown_file = "./bc_out_250/full.txt_%s_256/paths" % (lang_short)
        brown_in = codecs.open(brown_file, 'r', 'utf-8')
        words = []
        for line in brown_in:
            splits = line.split('\t')
            words.append(splits[1])
    elif source == "unigrams":
        # Read brown clusters
        brown_file = "./bc_out_1000/full.txt_%s_1000/unigram.words" % (lang_short)
        brown_in = codecs.open(brown_file, 'r', 'utf-8')
        words = []
        for word in brown_in:
            words.append(word.strip())
    else:
        raise Exception("Unknown source: " + source)
    word_set = set(words)
    return word_set

def traditional2simplified(tword_set):
    '''Converts a set of words in traditional Chinese to simplified Chinese.'''
    t2s = json.load(codecs.open("./tongwen/dict/t2s.json", "r", "utf-8"))
    print "len(t2s) =", len(t2s)
    if verbose:
        print "len(union) =", len(set(t2s.keys()).union(set(t2s.values())))
        i = 0
        for tc, sc in t2s.items():        
            print "%d tc='%s' sc='%s'" % (i, tc, sc)
            i+=1
    return augment_with_map(tword_set, t2s)

def simplified2traditional(tword_set):
    '''Converts a set of words in simplified Chinese to traditional Chinese.'''
    s2t = json.load(codecs.open("./tongwen/dict/s2t.json", "r", "utf-8"))
    print "len(s2t) =", len(s2t)
    return augment_with_map(tword_set, s2t)

def augment_with_map(tword_set, t2s):
    num_hits = 0
    num_total = 0
    print "Original size of word set: %d" % (len(tword_set))
    sword_set = set()
    for tword in tword_set:
        sword = u""
        for tc in tword:
            sc = t2s.get(tc, None)
            num_total += 1
            if sc is not None:
                sword += sc
                num_hits += 1
            else:
                sword += tc
        assert len(sword) == len(tword)
        if verbose:
            print "tword='%s' sword='%s' eq=%s" % (tword, sword, str(tword==sword)) 
        sword_set.add(sword)
    if verbose:
        print "Set difference: "
        for word in sword_set - tword_set:
            print word
    word_set = tword_set.union(sword_set)
    print "Augmented size of word set: %d" % (len(word_set))
    print "Character token hit rate: %f" % (1.0 * num_hits / num_total)
    return word_set

def compute_conll_hit_rate(lang_short, lang_long, source):
    print "\n\nCurrent language: ",lang_long

    word_set = get_word_set(lang_short, source)
    if lang_short == 'zh':
        word_set = traditional2simplified(word_set)
        word_set = simplified2traditional(word_set)

    print_words("First %d clustering words" % (num_to_print), list(word_set)[0:num_to_print])

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
    compute_hit_rate(word_set, conll_words, lang_short)
    print "Type hit rate for", lang_long
    compute_hit_rate(word_set, set(conll_words), lang_short)


if __name__ == "__main__":
    #lang_pairs = [('en', 'English'), ('es', 'Spanish'), ('ca', 'Catalan'), ('cs', 'Czech'), ('zh', 'Chinese'), ('zh_char', 'Chinese'), ('de', 'German')]
    lang_pairs = [('zh', 'Chinese'), ('zh_char', 'Chinese')]
    lang_pairs += [('es', 'Spanish'), ('ca', 'Catalan')]
    for lang_short, lang_long in lang_pairs:
        try:
            compute_conll_hit_rate(lang_short, lang_long, "unigrams")
        except Exception as e:
            print "Failed on", lang_long
            print e
        finally:
            print ""
            
