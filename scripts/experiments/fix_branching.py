#!/usr/bin/python
#


import os 
import sys
import re

lines = open(sys.argv[1], 'r').readlines()
repart = re.compile(r"c=.* m=.* name=\S*\s.*")
for i,line in enumerate(lines):
    if (line.find("Regret") != -1 and i+1 < len(lines) and lines[i+1].find("FullStrong") != -1):
        pass
    else:
        match = repart.search(line)
        print match.group(0)
        
    
