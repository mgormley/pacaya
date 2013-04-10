#!/usr/bin/python
#

# Example from http://matplotlib.org/examples/mplot3d/contourf3d_demo2.html

import os
from mpl_toolkits.mplot3d import axes3d
import matplotlib.pyplot as plt
from matplotlib import cm
import numpy as np


# --- Parameters ---
outfile = '/Users/mgormley/research/dep_parse/papers/coe_2013_presentation/fig/sum-to-one-curve.pdf'
# Labels.
title = ''
xlabel = ''#'$\\theta_1$'
ylabel = ''#'$\\theta_2$'
# Axes limits.
#xmin, xmax = -20.0, 0.0
#ymin, ymax = -20.0, 0.0
xmin, xmax = -2.0, 0.0
ymin, ymax = -2.0, 0.0
# Density of points (which determines smoothness) and visible grid lines.
num_points = 100.0
num_lines = 20.0
stride = int(num_points / num_lines)

# Create data.
X = np.arange(xmin, xmax, (xmax - xmin) / num_points)
Y = np.log(1.0 - np.exp(X))
##ymin, ymax = np.min(Y), np.max(Y)

# Create plot.

# Try to read the dimensions of this plot from its .info file.
infofile = outfile + ".info"
fig = None
if os.path.exists(infofile):
    info = open(infofile, 'r').read()
    size_inches = tuple(map(float, info.split()))
    if len(size_inches) == 2:
        fig = plt.figure(figsize=size_inches)
        fig.set_size_inches(size_inches)
if fig is None:
    fig = plt.figure()
        

# Create surface.
ax = fig.add_subplot(111)
ax.plot(X, Y, color='r', linewidth=2)
##ax.plot_surface(X, Y, Z, rstride=stride, cstride=stride, alpha=0.3)

# Axes limits.
ax.set_xlim(xmin, xmax)
ax.set_ylim(ymin, ymax)

# Axes labels.
ax.set_xlabel(xlabel)
ax.set_ylabel(ylabel)

ax.set_title(title)
    
plt.show()

# Save the dimensions of this plot to its .info file.
size_inches = fig.get_size_inches()
print "Size in inches:", size_inches
info = open(infofile, 'w')
info.write(str(size_inches[0]) + ' ' + str(size_inches[1]))
info.close()

fig.savefig(outfile, transparent=True, format='pdf', pad_inches=0.0, bbox_inches='tight')

# TODO: try saving/setting the viewing angle:
# http://stackoverflow.com/questions/12904912/how-to-set-camera-position-for-3d-plots-using-python-matplotlib
