#!/bin/bash
# Be sure to source setupenv.sh before running to get zimpl on the path
#

zimpl-3.1.0.darwin.x86.gnu.opt -v5 parse_flow.zpl 

if [[ $? != 0 ]] ;
then echo "Error"; exit 1;
fi

gurobi_cl ResultFile=parse_flow.sol parse_flow.lp 
