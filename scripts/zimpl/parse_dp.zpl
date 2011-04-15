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



# The domain of Arcs is pairs of token indices
set Arcs[<s> in Sents] := {<i,j> in Tokens[s] * Tokens[s] with i != j};
set TempArcs := union <s> in Sents: Arcs[s];
set AllArcs := { <s,i,j> in Sents*TempArcs with <i,j> in Arcs[s] }; # TODO: this should be horribly slow

# Dynamic Programming indicator variables
# In the DP formulation arc[] was originally named trap[], but they are equivalent
set DerivArcs[<s> in Sents] := {<i,j,k> in {Tokens[s] * Tokens[s] * Tokens[s]} with i <= j and j <= k and i < k and j > 0};
set TempDerivArcs := union <s> in Sents: DerivArcs[s];
set AllDerivArcs := { <s,i,j,k> in Sents*TempDerivArcs with <i,j,k> in DerivArcs[s] }; # TODO: this should be horribly slow

set Triangles[<s> in Sents] := {<i,j> in Tokens[s] * Tokens[s]};
set TempTriangles := union <s> in Sents: Triangles[s];
set AllTriangles := { <s,i,j> in Sents*TempTriangles with <i,j> in Triangles[s] }; # TODO: this should be horribly slow

set DerivTris[<s> in Sents] := {<i,j,k> in {Tokens[s] * Tokens[s] * Tokens[s]} with (i < j and j <= k) or (k <= j and j < i)};
set TempDerivTris := union <s> in Sents: DerivTris[s];
set AllDerivTris := { <s,i,j,k> in Sents*TempDerivTris with <i,j,k> in DerivTris[s] }; # TODO: this should be horribly slow



# ---------- Dependency Tree Constraints ----------
var arc[AllArcs] binary;
var deriv_arc[AllDerivArcs] binary;
var tri[AllTriangles] binary;
var deriv_tri[AllDerivTris] binary;
#var fintri[AllTriangles] binary;

# A trapazoid from i --> j indicates an arc from parent i to child j

# This constraint says there must be a single final triangle from the
# wall to the end of the sentence.
subto acceptance_criteria:
      forall <s> in Sents:      
      	     tri[s,0,Length[s]] == 1;

# The DP should really start with two separate variables for
# tri[s,1,1] (for the right and left triangles), but instead, we just
# cheat and say that they are both indicated by the same variable and
# the constraints for constructing initial trapezoids should just work.

# This constraint says that each of the zero length triangles is initialized to being on.
subto initialize:
      forall <s,i,j> in AllTriangles with i == j:
      	     tri[s,i,j] == 1;

# Create indicator variables for which derivation we use to create arc[s,i,k] or arc[s,k,i]
# if deriv_arc[s,i,j,k] == 1 then tri[s,i,j-1] == 1 AND tri[s,k,j] == 1
subto deriv_attach:
      forall <s,i,j,k> in AllDerivArcs:
      	     2*deriv_arc[s,i,j,k] <= tri[s,i,j-1] + tri[s,k,j];

# if arc[s,i,k] == 1 then tri[s,i,j-1] and tri[s,j,k] for some valid j (this is like an OR)
#TODO: In the DP attach_right and attach_left are different because the final triangle is on the head side only. Does that matter?
subto attach_right:
      forall <s,i,k> in AllArcs with i < k:
     	    arc[s,i,k]  <= sum <j> in Tokens[s] with <i,j,k> in DerivArcs[s]: deriv_arc[s,i,j,k];
subto attach_left:
      forall <s,i,k> in AllArcs with i < k:
      	    arc[s,k,i]  <= sum <j> in Tokens[s] with <i,j,k> in DerivArcs[s]: deriv_arc[s,i,j,k];

# Create indicator variables for the derivation of tri[s,i,k] or tri[s,k,i]
# if deriv_tri[s,i,j,k] == 1 then arc[s,i,j] == 1 and tri[s,j,k] == 1
# The case of i < j corresponds to a complete_right, and j < i corresponds to complete_left
subto deriv_complete_right:
      forall <s,i,j,k> in AllDerivTris:
      	     2*deriv_tri[s,i,j,k] <= arc[s,i,j] + tri[s,j,k];

# Complete right/left
subto complete_right_left:
      forall <s> in Sents:
      	     forall <i,k> in {Tokens[s] * Tokens[s]} with (i < k) or (k < i):
	     	    tri[s,i,k] <= sum <j> in Tokens[s] with <i,j,k> in DerivTris[s]: deriv_tri[s,i,j,k];

# TODO: Remove
# # arc[s,i,j] and arc[s,j,i] cannot be on at the same time
# subto valid_traps:
#       forall <s,i,j> in AllArcs with i < j:
#       	     arc[s,i,j] + arc[s,j,i] == 1;

# TODO: Should switch to the other initialization
# TODO: this could be taken care of by the parameter weights (i.e. this should have probability 0)
# arc[s,i,0] must be off for all i != 0, since that would indicate that i is the parent of the wall
subto valid_traps_wall:
      forall <s> in Sents:
      	     forall <i> in {1 to Length[s]}:
	     	    arc[s,i,0] == 0;


# # -------------

# # Other tree constraints
# # Each node should have a parent (except the wall)
# subto one_incoming_arc:
#     forall <s> in Sents:
#         forall <j> in { 1 to Length[s] }:
# 	    sum <i> in { 0 to Length[s] } with i != j: arc[s,i,j] == 1;

# # The wall has no incoming arcs
# subto no_parent_for_wall:
#     forall <s> in Sents:
#        forall <i> in { 1 to Length[s] }: 
#            arc[s,i,0] == 0;

# # ==================================================
# # ==== Option 1: Projective parsing ====
# # O(n^2) constraints 
# # If arc[s,i,j] == 1, then Word[s,i] must dominate all the children under that arc.
# # 
# # This constraint ensures that Word[s,i] is an ancestor of each of the nodes under the arc.
# subto proj_parse_dominate:
#     forall <s,i,j> in AllArcs with abs(i-j) > 1:
#         vif arc[s,i,j] == 1 then 
#             (sum <k,l> in {min(i,j) to max(i,j)}*{min(i,j) to max(i,j)} with k != l and i != l: arc[s,k,l]) == abs(i-j) 
#         end;

# # This constraint ensures that descendents of Word[s,i] are not parents of nodes outside the range [i,j]
# subto proj_parse_no_illegal_parents:
#     forall <s,i,j> in AllArcs with abs(i-j) > 1:
#         vif arc[s,i,j] == 1 then 
#             (sum <k,l> in Arcs[s] with (k > min(i,j) and k < max(i,j)) and (l <= min(i,j) or l >= max(i,j)): arc[s,k,l]) == 0 
#         end;

# # This constraint ensures that Word[s,i]'s parent is not among the nodes under the arc.
# subto proj_parse_parent:
#     forall <s,i,j> in AllArcs:
#         vif arc[s,i,j] == 1 then 
#             (sum <k> in {min(i,j) to max(i,j)} with i != k: arc[s,k,i]) == 0 
#         end;


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

