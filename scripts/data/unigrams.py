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

def read_word_counts(input):
    total_size = os.path.getsize(input)
    read_size = 0
    
    if input.endswith(".gz"):
        zf = gzip.open(input, 'rb')
        reader = codecs.getreader("utf-8")
        text_in = reader( zf )
    else:
        text_in = codecs.open(input, 'r', 'utf-8')
    
    word_count_map = {}
    start0 = time.clock()
    start = time.clock()
    for line in text_in:
        #read_size += len(line.encode('utf-8'))
        if input.endswith(".gz"):
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
            remain = (time.clock() - start0) * 1.0 / percent_read * (1 - percent_read) / 3600.0
            print "Percent read: %f Estimated remaining (hr): %f\n" % (percent_read, remain),
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
        cum += freq
        cum_freq_hist[count] = total_count - cum 
        
    print "Map from count of occurences to cumulative number of types appearing that many times."
    for count,freq in cum_freq_hist.items():
        print "\tcount=%d freq=%d percent_total=%f" % (count, freq, 1.0 * freq / total_count)
    print ""
    
    return cum_freq_hist

def unigrams(options):
    input = options.input
    output = options.output
    term_limit = options.term_limit
    print "Options: ", options
    min_occur = options.min_occur
    
    print "Counting unigrams in %s" % (input)
    count_map = read_word_counts(input)    
    print "Map size: %d" % (len(count_map))
    
    print_freq_hist(count_map, 5)
    
    print "Converting map to list"
    hist = [(freq,term) for term,freq in count_map.items() if freq >= min_occur]
    print "Sorted list of length: %d" % (len(hist))
    hist = sorted(hist, reverse=True)
    if term_limit > 0:
        hist = hist[:term_limit]
    
    uni_out = codecs.open(output, 'w', 'utf-8')
    for freq, term in hist:
        uni_out.write(str(freq))
        uni_out.write(" ")
        uni_out.write(term)
        uni_out.write("\n")        
    uni_out.close()
    
    
if __name__ == "__main__":
    usage = "%prog"

    parser = OptionParser(usage=usage)
    #parser.add_option('--method', action="store", help="Method for choosing slots/terms: {top_freq,bottom_freq,samp,rand}")
    parser.add_option('--input', help="[Required] Input text file (tokenized).")
    parser.add_option('--output', help="[Required] Output file")
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
    
