import random
import math
import os
import pypipeline.experiment_runner

def hyperopt(train_exp, hyper_list, test_list):
    train_exps = [train_exp + hyper for hyper in hyper_list]    
    best_exp = GetBestModel(train_exps, "devF1", "modelOut")
    test_exps = [best_exp + test for test in test_list]
    
    for te in train_exps:
        te.add_dependent(best_exp)
    for te in test_exps:
        te.add_prereq(best_exp)

    return train_exps + [best_exp] + test_exps

def get_max_exp(exp_dirs, eval_key):    
    max_val = float('-inf')
    max_exp = None 
    for exp_dir in exp_dirs:
        exp = experiment_runner.ExpParams()
        exp.read(os.path.join(exp_dir, "outparams.txt"))
        val = exp.get(eval_key)
        if val >= max_val:
            max_val = val
            max_exp = exp

def loguniform_exp(minexp, maxexp, base):
    '''Sample from a distribution where the log of the distribution is uniform. 
       The min value is base**minexp and the max value is base**maxexp.
    '''
    return base**random.uniform(minexp, maxexp)

def loguniform_val(minval, maxval):
    '''Sample from a distribution where the log of the distribution is uniform.
       The min value is minval and the max value is maxval. 
    '''
    # We can use any base here (we choose e) since b^{(1/log(b)*U(min,max))} = e^{U(min,max)}.
    minexp = math.log(minval)
    maxexp = math.log(maxval)
    return loguniform_exp(minexp, maxexp, math.e)

def loguniform_seg(minval, maxval, alpha=1.0, beta=7.2):
    '''Samples which can look like they came from a uniform distribution or from a 
    log uniform distribution. This is accomplished by sampling from different segments 
    of the logarithm curve.
        
    alpha and beta determine the portion of the log curve to use.
    
    When alpha = 1, we can range from linear to log by adjusting alpha.
    1    < beta <= 2.7  is nearly linear
    2.7  < beta <= 7.2  has a very slight curve
    7.2  < beta <= 51.8 medium curve
    51.8 < beta <= 2601 hard curve
    2601 < beta         log curve
    '''
    assert alpha < beta
    s = loguniform_val(alpha, beta)
    s = (s - alpha) / (beta - alpha)
    s = s * (maxval - minval) + minval
    return s
                
