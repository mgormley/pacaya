#!/usr/bin/python
#

import os
import sys
import getopt
import math
import tempfile
import stat
import re
import shlex
import time
import subprocess
from subprocess import Popen
import glob
#import topsort
import topological
from util import get_new_directory
from util import get_new_file

def write_script(prefix, script, dir):
    out, script_file = get_new_file(prefix=prefix,suffix=".sh",dir=dir)
    out.write(script)
    out.write("\n")
    out.close()
    os.system("chmod u+x '%s'" % (script_file))
    return script_file

def get_files_in_dir(dirname):
    return [f for f in os.listdir(dirname) if os.path.isfile(os.path.join(dirname, f))]

# TODO: handle resource reqs: qsub -l vf=100M OR qsub -l vf=7.5G
def queue_script(script_file, cwd, name="test", prereqs=[], stdout="stdout", qsub_args=None):
    os.chdir(cwd)
    queue_command = "qsub "
    if qsub_args:
        queue_command += " " + qsub_args + " "
    else:
        #queue_command += " -q cpu.q "
        queue_command += " -q mem.q -q himem.q -l vf=15.5G "
    queue_command += " -cwd -j y -b y -V -N %s -e stderr -o %s " % (name, stdout)        
    if len(prereqs) > 0:
        queue_command += "-hold_jid %s " % (",".join(prereqs))
    queue_command += "bash '%s'" % (script_file)
    print queue_command
    subprocess.check_call(shlex.split(queue_command))

unique_num = 0
def get_unique_name(name):
    global unique_num
    unique_num += 1
    return name + str(unique_num)

class Stage:
    def __init__(self, name, completion_indicator="DONE"):
        ''' If the default completion_indicator is used, it will be created in the cwd for this stage '''
        self.name = str(name) #TODO: is this the best way to handle name's type?
        self.completion_indicator = completion_indicator
        self.prereqs = []
        self.dependents = []
        self.serial = False
        self.qsub_args = None

    def add_prereq(self, stage):
        self.prereqs.append(stage)
        stage.dependents.append(self)

    def add_prereqs(self, stages):
        for stage in stages:
            self.add_prereq(stage)
    
    def run_stage(self, exp_dir):
        os.chdir(exp_dir)
        if self.is_already_completed():
            print "Skipping completed stage: name=" + self.name + " completion_indicator=" + self.completion_indicator
            return
        self._run_stage(exp_dir)
        
    def _run_stage(self, exp_dir):
        ''' Overidden by GridShardRunnerStage '''
        script = self.create_stage_script(exp_dir)
        #TODO: this is a hack. This should wrap the experiment script
        script += "\ntouch '%s'\n" % (self.completion_indicator)        
        script_file = write_script("experiment-script", script, exp_dir)
        self.run_script(script_file, exp_dir)

    def __str__(self):
        return self.name

    def is_already_completed(self):
        if not os.path.exists(self.completion_indicator):
            return False
        for prereq in self.prereqs:
            if not prereq.is_already_completed():
                return False
        return True

    def run_script(self, script_file, cwd, stdout_filename="stdout"):
        stdout = os.path.join(cwd, stdout_filename)
        if self.serial:
            os.chdir(cwd)
            assert(os.path.exists(script_file))
            command = "bash %s" % (script_file)
            print self.name,":",command
            stdout = open(stdout, 'w')
            p = Popen(args=shlex.split(command), cwd=cwd, stderr=subprocess.STDOUT, stdout=stdout)
            retcode = p.wait()
            stdout.close()
            if (retcode != 0):
                raise subprocess.CalledProcessError(retcode, command)
            #Old way: subprocess.check_call(shlex.split(command))
        else:
            prereq_names = [prereq.name for prereq in self.prereqs]
            queue_script(script_file, cwd, self.name, prereq_names, stdout, self.qsub_args)

    def create_stage_script(self, exp_dir):
        ''' Override this method '''
        return None

class GridShardRunnerStage(Stage):

    def __init__(self, name, input_shard_prefix, new_output_prefix=None, completion_indicator="DONE"):
        Stage.__init__(self, name, completion_indicator)
        # TODO: move this
        self.files = self.get_files(input_shard_prefix)
        self.input_shard_prefix = input_shard_prefix
        self.new_output_prefix = new_output_prefix

    def get_files(self, shard_prefix):
        directory = os.path.dirname(shard_prefix)
        prefix = os.path.basename(shard_prefix)
        if directory == '':
            directory = '.'
        files = glob.glob(os.path.join(directory, prefix))
        assert len(files) != 0, "No files matching shard_prefix: " + shard_prefix
        files.sort()
        return files

    # TODO: should throw an exception if there's an error in a prereq
    def wait_for_prereqs(self, prereqs):
        prereqs_complete = False
        while not prereqs_complete:
            indicators = [os.path.exists(prereq.completion_indicator) for prereq in prereqs]
            prereqs_complete = reduce(lambda x,y: x and y, indicators)
            time.sleep(3)

    def _run_stage(self, exp_dir):
        #TODO: this could maybe be a Thread call (but adds complications for topological sort assumptions)
        self.wait_for_prereqs(self.prereqs)
        stages = self.get_shard_stages()
        for stage in stages:
            shard_exp_dir = os.path.join(exp_dir, "shard-%s" % (stage.name))
            os.mkdir(shard_exp_dir)
            stage.run_stage(shard_exp_dir)

        self.prereqs.extend(stages)
        Stage._run_stage(self, exp_dir)

    def get_shard_stages(self):
        stages = []
        shard_number = 0
        for input_file in self.files:
            # Are there shard numbers?
            matcher = re.compile('(\d+)\.shard').search(input_file)
            if matcher:
                # Get the shard number.
                shard_number = matcher.group(1)
                if self.new_output_prefix:
                    new_shard = '%s_%s.shard' % (self.new_output_prefix, shard_number)
            else:
                shard_number += 1
                if self.new_output_prefix:
                    new_shard =  os.path.join(self.new_output_prefix, os.path.basename(input_file))

            shard_stage = self.create_shard_stage(shard_number, input_file, new_shard)
            stages.append(shard_stage)
        return stages

    def create_stage_script(self, cwd):
        ''' Create a script that will do nothing. This will be run by Stage.run_stage(). '''
        return ""

    def create_shard_stage(self, shard_number, input_file, output_file):
        name = "%s-%s" % (self.name, str(shard_number))
        return ShardStage(name, input_file, output_file, self) 

    def create_shard_script(self, cwd, input_file, output_file):
        ''' Override this method '''
        #if self.new_output_prefix:
        #    command = '%s' % (self.script_name.replace('%INPUT_SHARD%', input_file).replace('%OUTPUT_FILE%', new_shard))
        #else:
        #    command = '%s' % (self.script_name.replace('%INPUT_SHARD%', input_file))
        return None

class ShardStage(Stage):
    '''Helper class for GridShardRunnerStage'''
    
    def __init__(self, name, input_file, output_file, grid_shard_runner_stage):
        Stage.__init__(self, name)
        self.input_file = input_file
        self.output_file = output_file
        self.grid_shard_runner_stage = grid_shard_runner_stage

    def create_stage_script(self, cwd):
        return self.grid_shard_runner_stage.create_shard_script(cwd, self.input_file, self.output_file)

class ScriptStringStage(Stage):
    
    def __init__(self, name, script, completion_indicator="DONE"):
        Stage.__init__(self, name, completion_indicator)
        self.script = script

    def create_stage_script(self, exp_dir):
        return self.script

class RootStage(Stage):
    
    def __init__(self):
        Stage.__init__(self, "root_stage")
        
    def run_stage(self, exp_dir):
        # Intentionally a no-op
        pass

class PipelineRunner:
    
    def __init__(self,name="experiments",queue=None):
        self.name = name
        self.serial = (queue == None)
        
        self.java_args = " -server -ea -Dfile.encoding=UTF8 "
        
        self.queue = queue
        if self.queue == "mem":
            self.threads = 4
            self.qsub_args = " -q mem.q -q himem.q -l num_proc=%d -l h_vmem=8G " % (self.threads)
            self.java_args += " -Xms6500m -Xmx6500m -XX:MaxPermSize=512m "
        elif self.queue == "clsp":
            self.threads = 6
            self.qsub_args = " -q all.q -pe smp %d -l cpu_arch=x86_64 -l mem_free=8G " % (self.threads)
            self.java_args += " -Xms6500m -Xmx6500m -XX:MaxPermSize=512m "
        else: # self.queue == "cpu"
            self.threads = 1
            self.qsub_args = " -q cpu.q -l num_proc=%d -l h_vmem=2G " % (self.threads)
            self.java_args += " -Xms1256m -Xmx1256m -XX:MaxPermSize=256m "

    def run_pipeline(self, root_stage):
        self.check_stages(root_stage)
        top_dir = get_new_directory(prefix=self.name, dir="exp")
        os.chdir(top_dir)
        self.add_post_processing_stage(root_stage, top_dir)
        for stage in self.get_stages_as_list(root_stage):
            if stage.name == "root_stage":
                continue
            cwd = os.path.join(top_dir, str(stage.name))
            os.mkdir(cwd)
            stage.serial = self.serial
            stage.cwd = cwd
            stage.run_stage(cwd)

    def check_stages(self, root_stage):
        all_stages = self.get_stages_as_list(root_stage)
        names = set()
        for stage in all_stages:
            matcher = re.compile('^[a-z,A-Z]').search(stage.name)
            if not matcher:
                print "Warning: stage name must begin with a letter: " + stage.name,
                stage.name = 'a'+stage.name
                print ". Changing to: " + stage.name + "."
            assert stage.name not in names, "Multiple stages have the same name: " + stage.name + "\n" + str([s.name for s in all_stages])
            names.add(stage.name)
        print "all_stages(names):",[stage.name for stage in all_stages]                    
            
    def get_stages_as_list(self, root_stage):
        partial_order = []
        all_stages = self.dfs_stages(root_stage)
        for stage in all_stages:
            for dependent in stage.dependents:
                partial_order.append((stage, dependent))
        #return topsort.topsort(partial_order)
        return topological.topological_sort(all_stages, partial_order)

    def dfs_stages(self, stage):
        stages = []
        stages.append(stage)
        #print "stage.name",stage.name
        #print "stage.dependents",[x.name for x in stage.dependents]
        for dependent in stage.dependents:
            for s in self.dfs_stages(dependent):
                if not s in stages:
                    stages.append(s)
        return stages

    # TODO: consider removing this - it is only for convenience
    def add_post_processing_stage(self, root_stage, top_dir):
        all_stages = self.dfs_stages(root_stage)
        post = self.create_post_processing_stage_script(top_dir, all_stages)
        if (post == None):
            return
        post_file = write_script("post-processing", post, top_dir)
        post_stage = ScriptStringStage("post-processing", "bash '%s'" % (post_file))
        post_stage.add_prereqs(all_stages)

    def create_post_processing_stage_script(self, top_dir, all_stages):
        ''' Override this method '''
        return None


class ExperimentStage(Stage):
    
    def __init__(self, name, experiment, exp_runner):
        Stage.__init__(self, name)
        self.exp_runner = exp_runner
        self.experiment = experiment

    def create_stage_script(self, exp_dir):
        return self.exp_runner.create_experiment_script(self.name, self.experiment, exp_dir)

    def __str__(self):
        return self.name

class ExperimentRunner(PipelineRunner):
    
    def __init__(self,name="experiments",serial=False):
        PipelineRunner.__init__(self, name, serial)
        self.qsub_args = None

    def run_experiments(self, experiments):
        root_stage = RootStage()
        for name,experiment in experiments.items():
            exp_stage = ExperimentStage(name, experiment, self)
            # Give each experiment stage the global qsub_args 
            exp_stage.qsub_args = self.qsub_args
            exp_stage.add_prereq(root_stage)
        self.run_pipeline(root_stage)

    def create_post_processing_stage_script(self, top_dir, all_stages):
        all_stages = all_stages[1:]
        exp_tuples = [(stage.name, stage.experiment) for stage in all_stages]
        return self.create_post_processing_script(top_dir, exp_tuples)
    
    def get_stages_as_list(self, root_stage):
        '''This method is overriden to give the provided order for experiments'''
        return self.dfs_stages(root_stage)
        
    def create_experiment_script(self, name, experiment, exp_dir):
        ''' Override this method '''
        return None
    
    def create_post_processing_script(self, top_dir, exp_tuples):
        ''' Override this method '''
        return None

if __name__ == '__main__':
    print "This script is not to be run directly"
