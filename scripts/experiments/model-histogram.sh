#!/bin/bash
#
# Argument should be a model.txt file
#

cat $1 | perl -n -e 'if(/= (\d.\d).*,/) { print $1."\n"; }' | sort -n | uniq -c 

cat $1 | perl -n -e 'if(/= (\d.\d\d).*,/) { $num = $1; if ($num < 0.1) { print $1."\n";} }' | sort -n | uniq -c 