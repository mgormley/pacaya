# ---------- Sets, Parameters, Weights ----------
param InputSent := "input.sent";

# input.sent contains "<sentence id, token id> word"
set AllTokens := { read InputSent as "<1n,2n>" };
set Sents := proj(AllTokens,<1>);
set TempTokens := proj(AllTokens,<2>);
set Tokens[<s> in Sents] := { <i> in  TempTokens with <s,i> in AllTokens };
param Length[<s> in Sents] := max(Tokens[s]);
param Word[AllTokens] := read InputSent as "<1n,2n> 3s";
set AllWords := { read InputSent as "<3s>" }; 
# This gives an error: set AllWords := {union <s,i> in AllTokens: Word[s,i]};

do print AllTokens;
do print Sents;
do forall <s> in Sents do print Tokens[s];
do forall <s,i> in AllTokens do print Word[s,i];


# The domain of Arcs is pairs of token indices
# Don't allow the wall symbol (token 0) to have a parent
set Arcs[<s> in Sents] := {<i,j> in Tokens[s] * Tokens[s] with i != j};
set TempArcs := union <s> in Sents: Arcs[s];
set AllArcs := { <s,i,j> in Sents*TempArcs with <i,j> in Arcs[s] }; # TODO: this should be horribly slow

# The following 4 lines don't work
# The domain of WordPairs is pairs of words
#set Words[<s> in Sents] := { <w> in AllWords with Word[s,i] == w};
#set WordPairs[<s> in Sents] := Words[s] * Words[s];
#set AllWordPairs := union <s> in Sents: WordPairs;

# input.chooseweights contains "<parent word, child word> weight"
param InputChooseWeights := "input.chooseweights";
set AllWordPairs := { read InputChooseWeights as "<1s, 2s>" };
param ChooseWeight[AllWordPairs] := read InputChooseWeights as "<1s, 2s> 3n";

# input.stopweights contains "<word, l/r, 0/1> weight" where 0/1 is for adjacency
param InputStopWeights := "input.stopweights";
set LR := {"l","r"};
set StopSet := AllWords * LR * {0, 1};
param StopWeight[StopSet] := read InputStopWeights as "<1s,2s,3n> 4n";

# ---------- Dependency Tree Constraints ----------
var arc[AllArcs] binary;

# Other tree constraints
# Each node should have a parent (except the wall)
subto one_incoming_arc:
    forall <s> in Sents:
        forall <j> in { 1 to Length[s] }:
	    sum <i> in { 0 to Length[s] } with i != j: arc[s,i,j] == 1;

# The wall has no incoming arcs
subto no_parent_for_wall:
    forall <s> in Sents:
       forall <i> in { 1 to Length[s] }: 
           arc[s,i,0] == 0;

# ==================================================
# ==== Option 1: Projective parsing ====
# O(n^2) constraints 
# If arc[s,i,j] == 1, then Word[s,i] must dominate all the children under that arc.
# 
# This constraint ensures that Word[s,i] is an ancestor of each of the nodes under the arc.
subto proj_parse_dominate:
    forall <s,i,j> in AllArcs with abs(i-j) > 1:
        vif arc[s,i,j] == 1 then 
            (sum <k,l> in {min(i,j) to max(i,j)}*{min(i,j) to max(i,j)} with k != l and i != l: arc[s,k,l]) == abs(i-j) 
        end;

# This constraint ensures that descendents of Word[s,i] are not parents of nodes outside the range [i,j]
subto proj_parse_no_illegal_parents:
    forall <s,i,j> in AllArcs with abs(i-j) > 1:
        vif arc[s,i,j] == 1 then 
            (sum <k,l> in Arcs[s] with (k > min(i,j) and k < max(i,j)) and (l <= min(i,j) or l >= max(i,j)): arc[s,k,l]) == 0 
        end;

# This constraint ensures that Word[s,i]'s parent is not among the nodes under the arc.
subto proj_parse_parent:
    forall <s,i,j> in AllArcs:
        vif arc[s,i,j] == 1 then 
            (sum <k> in {min(i,j) to max(i,j)} with i != k: arc[s,k,i]) == 0 
        end;

# ==== Option 2: Non-projective parsing ====
# Flow Constraints for: B is connected
# var flow[AllArcs] real >= 0;

# subto flow_sum: 
#     forall <s> in Sents:
#         Length[s] == sum <j> in { 1 to Length[s] }: flow[s,0,j];

# subto flow_diff: 
#     forall <s> in Sents:
#         forall <i> in { 1 to Length[s] }:
# 	    1 == (sum <j> in {0 to Length[s] } with i != j: flow[s,j,i])
# 	         - (sum <j> in { 0 to Length[s] } with i != j: flow[s,i,j]);

# subto flow_bound:
#     forall <s,i,j> in AllArcs:
#         flow[s,i,j] <= Length[s] * arc[s,i,j];
# ==================================================

# ---------- DMV log-likelihood ----------

# Supporting variables for DMV log-likelihood

set AllTokensLR := AllTokens * LR;
# number of arcs from <s,i> to left/right children
var numToSide[<s,i,lr> in AllTokensLR] integer >= 0 <= Length[s];
# whether a child was generate adjacent to the left/right
var genAdj[AllTokensLR] binary;
# number of non-adjacent arcs from <s,i> to left/right children
var numNA[<s,i,lr> in AllTokensLR] integer >= 0 <= Length[s] - 1;

subto numToSideLeft:
    forall <s,i> in AllTokens:
        numToSide[s,i,"l"] == sum <j> in { 0 to i-1 }: arc[s,i,j];

subto numToSideRight:
    forall <s,i> in AllTokens:
        numToSide[s,i,"r"] == sum <j> in { i+1 to Length[s] }: arc[s,i,j];

subto genAdjLeftAndRight:
    forall <s,i,lr> in AllTokensLR: 
        vif numToSide[s,i,lr] >= 1 then genAdj[s,i,lr] == 1 else genAdj[s,i,lr] == 0 end;

subto numNALeftAndRight:
    forall <s,i,lr> in AllTokensLR:
        vif numToSide[s,i,lr] - 1 > 0 then numNA[s,i,lr] == numToSide[s,i,lr] - 1 else numNA[s,i,lr] == 0 end;

maximize goal: 
    (sum <s,i,j> in AllArcs: arc[s,i,j] * log(ChooseWeight[Word[s,i],Word[s,j]]))
    + (sum <s,i,lr> in AllTokensLR: 
          (genAdj[s,i,lr] * log(StopWeight[Word[s,i],lr,0])
	   + (1 - genAdj[s,i,lr]) * log(StopWeight[Word[s,i],lr,1])
	   + genAdj[s,i,lr] * log(1 - StopWeight[Word[s,i],lr,1])
	   + numNA[s,i,lr] * log(1 - StopWeight[Word[s,i],lr,0]))
      );

