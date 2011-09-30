var x integer >= 0 priority 1 startval 1;
var y integer >= 0 priority 2 startval 2;
subto asdf: x + y <= -ln(0.22E-44);
#subto xqwer: x == -1E308;
#subto yqwer: y == 1E308;
sos testsos: type1: 1*x + 2*y;

#param z := +infinity;
maximize goal: -1E308*x + ln(4.9E-323)*y;

