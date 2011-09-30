#!/bin/bash
# Be sure to source setupenv.sh before running to get zimpl on the path
#

#zimpl-3.1.0.darwin.x86.gnu.opt -v5 parse_flow.zpl 
zimpl-3.1.0.darwin.x86.gnu.opt -r -m simple.zpl 

if [[ $? != 0 ]] ;
then echo "Error"; exit 1;
fi

gurobi_cl ResultFile=simple.sol simple.lp 
