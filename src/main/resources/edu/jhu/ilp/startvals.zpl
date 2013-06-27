set Food := { "Oatmeal", "Chicken", "Eggs", "Milk", "Pie", "Pork" };

var xvar integer >= 0 priority 1 startval 1;
var yvar integer >= 0 priority 2;
var foodVals[Food] integer startval 7;
var otherVals[<f> in Food] integer;

subto asdf: xvar + yvar <= -ln(0.22E-44);
subto food: forall <f> in Food: foodVals[f] <= otherVals[f];
subto other: forall <f> in Food: otherVals[f] <= yvar;

sos testsos: type1: 1*xvar + 2*yvar;

maximize goal: -1E37*xvar + ln(4.9E-323)*yvar + (sum <f> in Food : foodVals[f]);


