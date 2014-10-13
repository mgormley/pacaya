#!/bin/bash
#
# Clips out the middle of a file and writes the new version to a file with the .clip extension.
#
# Example usage: 
# find archive_acl_13/bnb_005/ -name "stdout" | xargs -n 1 ../scripts/experiments/clip-file.sh
#

(head -n 1000; tail -n 1000) < $1 > $1.clip
