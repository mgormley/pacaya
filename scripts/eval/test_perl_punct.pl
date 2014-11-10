
require Encode;

use strict ;
use warnings;
use Getopt::Std ;

sub is_uni_punct
{
  my ($word) = @_ ;
  return 0+scalar(Encode::decode_utf8($word)=~ /^\p{Punctuation}+$/) ;
}

print "Testing \$ \n";
print "-" . is_uni_punct("-") . "\n";
print "-" . is_uni_punct("\$") . "\n";
print "-" . is_uni_punct("M") . "\n";
