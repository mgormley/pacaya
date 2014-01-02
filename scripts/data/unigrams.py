#!/usr/bin/python
import codecs
import copy
from optparse import OptionParser
import sys
import os
import shutil
import subprocess
import re
import random
import glob
import time
import gzip

def read_word_counts(in_text):
    total_size = os.path.getsize(in_text)
    read_size = 0
    
    if in_text.endswith(".gz"):
        zf = gzip.open(in_text, 'rb')
        reader = codecs.getreader("utf-8")
        text_in = reader( zf )
    else:
        text_in = codecs.open(in_text, 'r', 'utf-8')
    
    word_count_map = {}
    start0 = time.clock()
    start = time.clock()
    for line in text_in:
        #read_size += len(line.encode('utf-8'))
        if in_text.endswith(".gz"):
            # Hack fileobj from: http://stackoverflow.com/questions/1704458/get-uncompressed-size-of-a-gz-file-in-python
            read_size = zf.fileobj.tell()
        else:
            read_size = text_in.tell()
        #print read_size
        line = line.strip()
        for word in line.split("\s+"):
            if len(word) > 0:
                count = word_count_map.get(word, 0)
                word_count_map[word] = count + 1
        elapsed = time.clock() - start
        if elapsed > 1:
            percent_read = 1.0 * read_size / total_size
            remain = (time.clock() - start0) * 1.0 / percent_read * (1 - percent_read) / 60.0
            print "Percent read: %f Estimated remaining (min): %f\n" % (percent_read, remain),
            start = time.clock()
    print ""

    text_in.close()
    return word_count_map
               
def to_sorted_term_hist(count_map):
    hist = [(freq,term) for term,freq in count_map.items()]
    return sorted(hist, reverse=True)

def print_freq_hist(count_map, max_count=5):
    # Map from count of occurences to frequency of types appearing that many times.
    freq_hist = {}
    for term,count in count_map.items():
        if count <= max_count:
            freq = freq_hist.get(count, 0)
            freq_hist[count] = freq + 1
    
    print "Map from count of occurences to frequency of types appearing that many times."
    for count,freq in freq_hist.items():
        print "\tcount=%d freq=%d" % (count, freq)
    print ""

    # Get total count.
    total_count = len(count_map) #sum(count_map.values())
    print "Total count:", total_count
    
    # Map from count to cumulative number of types occuring that many times or more.
    cum_freq_hist = {}
    cum = 0
    for count in sorted(freq_hist.keys()):
        freq = freq_hist[count]
        cum_freq_hist[count] = total_count - cum 
        cum += freq
        
    print "Map from count of occurences to cumulative number of types appearing that many times."
    for count,freq in cum_freq_hist.items():
        print "\tcount=%d freq=%d percent_total=%f" % (count, freq, 1.0 * freq / total_count)
    print ""
    
    return cum_freq_hist

def unigrams(options):
    in_text = options.in_text
    out_counts = options.out_counts
    out_words = options.out_words
    term_limit = options.term_limit
    print "Options: ", options
    min_occur = options.min_occur
    
    print "Counting unigrams in %s" % (in_text)
    count_map = read_word_counts(in_text)    
    print "Map size: %d" % (len(count_map))
    
    cum_freq_hist = print_freq_hist(count_map, 10)
    if min_occur == -1 and term_limit > 0:        
        for count in sorted(cum_freq_hist.keys()):
            if cum_freq_hist[count] >= term_limit:
                min_occur = count
        if min_occur > 0:
            print "Chose min_occur=%d to retain %d types" % (min_occur, cum_freq_hist[min_occur])
        
    print "Converting map to list"
    hist = [(freq,term) for term,freq in count_map.items() if freq >= min_occur]
    print "Sorting list of length: %d" % (len(hist))
    hist = sorted(hist, reverse=True)
    if term_limit > 0:
        print "Slicing to list of only %d types" % (term_limit)
        hist = hist[:term_limit]
    
    if out_counts:
        print "Writing counts to %s" % (out_counts)
        uni_out = codecs.open(out_counts, 'w', 'utf-8')
        for freq, term in hist:
            uni_out.write(str(freq))
            uni_out.write(" ")
            uni_out.write(term)
            uni_out.write("\n")        
        uni_out.close()
            
    if out_words:
        print "Writing words only to %s" % (out_words)
        uni_out = codecs.open(out_words, 'w', 'utf-8')
        for _, term in hist:
            uni_out.write(term)
            uni_out.write("\n")        
        uni_out.close()    
    
if __name__ == "__main__":
    usage = "%prog"

    parser = OptionParser(usage=usage)
    #parser.add_option('--method', action="store", help="Method for choosing slots/terms: {top_freq,bottom_freq,samp,rand}")
    parser.add_option('--in_text', help="[Required] Input text file (tokenized).")
    parser.add_option('--out_counts', help="Output file for counts")
    parser.add_option('--out_words', help="Output file for words only without counts")
    parser.add_option('--term_limit', action="store", type="int", default=-1, help="[Required] The max number of terms to include")
    parser.add_option('--min_occur', action="store", type="int", default=-1, help="The minimum number of occurences for a term to keep")
    #parser.add_option('--min_term_length', action="store", type="int", help="The min number of characters in a term (prior to stemming)")
    #parser.add_option( '--stem_terms', action="store_true", help="Stem the terms")
    #parser.add_option( '--lower', action="store_true", help="Changes terms and slots to lowercase")
    (options, args) = parser.parse_args(sys.argv)

    if len(args) != 1:
        parser.print_help()
        sys.exit(1)

    seed = 718648291
    random.seed(seed)
    
    unigrams(options)
    
