#!/usr/bin/env python
import pytest
from scrape_crossval import hyperparam_argmax, exp_to_series, series_to_exp
from pypipeline import experiment_runner 
from pypipeline.experiment_runner import ExpParams

def test_exp_to_series():
    e1 = ExpParams(a="A", b="B", c=3.1, d=2)
    s = exp_to_series(e1)
    e2 = series_to_exp(s)
    assert e1.params == e2.params

def test_hyperparam_argmax():
    dev_list = [ExpParams(a="aa", b="A", c=1.0, d=5),
                ExpParams(a="aa", b="B", c=2.0, d=6),
                ExpParams(a="bb", b="A", c=3.0, d=7),
                ExpParams(a="bb", b="B", c=4.0, d=8),
                ExpParams(a="cc", b="A", c=4.0, d=8),
                ExpParams(a="cc", b="B", c=4.0, d=8),
                ]
    train_keys = list('ab')
    hyperparam_keys = list('b')
    argmax_key = 'c'
    test_list = hyperparam_argmax(dev_list, train_keys, hyperparam_keys, argmax_key)

    assert len(test_list) == 3
    
    acol = map(lambda x: x.get('a'), test_list)
    assert 'aa' in acol
    assert 'bb' in acol
    assert 'cc' in acol
    
    ccol = map(lambda x: x.get('c'), test_list)
    assert set([2, 4, 4]) == set(ccol)
    
if __name__ == "__main__":
    print "Warning: this could take a while if searching from the current directory."
    pytest.main()
