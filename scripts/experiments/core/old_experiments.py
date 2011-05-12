#!/usr/bin/python

import sys
import os
import getopt
import math
import tempfile
import stat
import subprocess
from subprocess import Popen
import shlex
from util import get_new_directory
from util import get_new_file

def write_script(prefix, script, dir):
    out, script_file = get_new_file(prefix=prefix,suffix=".sh",dir=dir)
    out.write(script)
    out.write("\n")
    out.close()
    os.system("chmod u+x %s" % (script_file))
    return script_file

def queue_script(name, script_file, cwd):
    if False:
        os.chdir(cwd)
        assert(os.path.exists(script_file))
        command = "bash %s" % (script_file)
        print name,":",command
        stdout = open("stdout", 'w')
        p = Popen(args=shlex.split(command), cwd=cwd, stderr=subprocess.STDOUT, stdout=stdout)
        retcode = p.wait()
        if (retcode != 0):
            raise subprocess.CalledProcessError(retcode, command)
    else:
        os.chdir(cwd)
        queue_command = "qsub -cwd -j y -b y -q mem.q -V -N e%s -e stderr -o stdout bash %s " % (name, script_file)
        #    queue_command = "qsub -cwd -j y -b y -q himem.q -V -N e%s -e stderr -o stdout bash %s " % (name, script_file)
        print queue_command
        subprocess.call(queue_command.split())

class ExperimentRunner:
    def __init__(self,name="experiments"):
        self.name = name
        self.done_file = "COMPLETED"

    def run_experiments(self, experiments):
        top_dir = get_new_directory(self.name)
        os.chdir(top_dir)
        exp_tuples = []
        
        for name in sorted(experiments.keys()):
            experiment = experiments[name]
            exp_dir = os.path.join(top_dir, str(name))
            os.mkdir(exp_dir)
            exp_tuples.append((name, experiment, exp_dir))

        self.do_preprocessing(top_dir, exp_tuples)

        for name,experiment,exp_dir in exp_tuples:
            self.run_experiment(name,experiment, exp_dir)

        self.run_post_processing(top_dir, exp_tuples)

    def run_post_processing(self, top_dir, exp_tuples):
        post = self.create_post_processing_script(top_dir, exp_tuples)
        if (post == None):
            return
        post_file = write_script("post-processing", post, top_dir)

        count_cmd = "find %s -name \"%s\" | wc -l" % (top_dir, self.done_file)
        stall = "COUNT=0\n"
        stall += "while [ $COUNT -lt %d ] ; do sleep 1; COUNT=`%s`; done\n" % (len(exp_tuples), count_cmd)
        stall += "bash " + post_file + "\n"
        stall += "touch %s\n" % (os.path.join(top_dir, self.done_file))
        stall_file = write_script("stall", stall, top_dir)
        queue_script("post", stall_file, top_dir)

    def run_experiment(self, name, experiment, exp_dir):
        os.chdir(exp_dir)
        script = self.create_experiment_script(name, experiment, exp_dir)

        #TODO: this is a hack
        script += "\ntouch %s\n" % (self.done_file)
        
        script_file = write_script("experiment-script", script, exp_dir)
        queue_script(name, script_file, exp_dir)

    def do_preprocessing(self, top_dir, exp_tuples):
        return None

    def create_experiment_script(self, name,experiment, exp_dir):
        return None

    def create_post_processing_script(self, top_dir, exp_tuples):
        return None

class DummyExperimentRunner(ExperimentRunner):

    def get_experiments(self, num_experiments):
        exps = {}
        for i in range(num_experiments):
            exps[i] = "experiment-" + str(i*i)
        return exps

    def create_experiment_script(self, name, experiment, exp_dir):
        script = 'echo Hello World.\n'
        script += 'echo "name=%d experiment=%s" > file\n' % ( name,experiment)
        return script

if __name__ == "__main__":
    pass


