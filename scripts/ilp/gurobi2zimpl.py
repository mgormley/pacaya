import sys
import re
from optparse import OptionParser

def get_var_map(tbl_file):
    var_map = {}
    for line in open(tbl_file, 'r'):
        splits = line.split()
        if splits[1] == 'v':
            short_var = splits[3]
            zimpl_var = splits[4]
            # Drop the quotes
            zimpl_var = zimpl_var[1:-1]
            var_map[short_var] = zimpl_var    
    return var_map

def get_zimpl_varmap(gurobi_file, tbl_file):
    var_map = get_var_map(tbl_file)

    gurobi_lines = open(gurobi_file, 'r').readlines()
    if len(gurobi_lines) == 0:
        sys.stderr.write("Error: empty gurobi solution\n")
        sys.exit(1)
        
    zimpl_varmap = {}
    # Parse gurobi solution file
    for line in gurobi_lines:
        if line.startswith("#"):
            # Skip comments
            continue
        line = line.strip()
        splits = line.split()
        lp_name = splits[0]
        value = splits[1]
        zimpl_name = var_map[lp_name]
        zimpl_varmap[zimpl_name] = value
    return zimpl_varmap

if __name__ == "__main__":
    usage = "%s [gurobi sol] [zimpl tbl]" % (sys.argv[0])

    parser = OptionParser(usage=usage)
    (options, args) = parser.parse_args(sys.argv)

    if len(args) != 3:
        print usage
        sys.exit(1)

    gurobi_file = args[1]
    tbl_file = args[2]
    
    zimpl_varmap = get_zimpl_varmap(gurobi_file, tbl_file)
    for zimpl_name,value in sorted(zimpl_varmap.items()):
        print "%s\t%s" % (zimpl_name, value)
