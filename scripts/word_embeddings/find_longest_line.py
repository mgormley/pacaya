import sys

def get_longest_line(filename):
    large_line = ''
    large_line_len = 0
    large_line_i = 0

    i=0
    with open(filename, 'r') as f:
        for line in f:
            i += 1
            if len(line) > large_line_len:
                large_line_len = len(line)
                large_line = line
                large_line_i = i

    return i, large_line_len, large_line

print get_longest_line(sys.argv[1])
