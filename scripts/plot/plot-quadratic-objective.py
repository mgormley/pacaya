#!/usr/bin/python
#

# Example from http://matplotlib.org/examples/mplot3d/contourf3d_demo2.html

import os
from mpl_toolkits.mplot3d import axes3d
import matplotlib.pyplot as plt
from matplotlib import cm
import numpy as np

def scale_axes(xmin, xmax, scaler):
    newmin = xmin - contour_scaler * (xmax - xmin)
    newmax = xmax + contour_scaler * (xmax - xmin)
    return newmin, newmax

def generate(X, Y, phi):
    R = 1 - np.sqrt(X**2 + Y**2)
    return np.cos(2 * np.pi * X + phi) * R

def generate2(X, Y, phi, beta):
    # Start with a nonconvex (not-so-bumpy) surface.
    R = 1 - np.sqrt(X**2 + Y**2)
    Z = np.cos(2 * np.pi * X + phi) * R
    # Add some bumpyness.
    Z = Z + (X * Y * np.sin(X*beta) * np.cos(Y*beta) ) * 2.0
    # Pull the outside edges down.
    Z = Z - np.abs((X*2)**2 + (Y*2)**2) / 5.0
    return Z

# --- Parameters ---
outfile = '/Users/mgormley/research/dep_parse/papers/coe_2013_presentation/fig/objective-surface.pdf'
# Labels.
title = ''
xlabel = ''
ylabel = ''
zlabel = ''
# Axes limits.
#xmin, xmax = -20.0, 0.0
#ymin, ymax = -20.0, 0.0
xmin, xmax = -15.0, 0.0
ymin, ymax = 0.0, 4.0
# Contour params.
add_contours = False
contour_scaler = 0.2
# Density of points (which determines smoothness) and visible grid lines.
num_points = 100.0
num_lines = 20.0
stride = int(num_points / num_lines)

# Create data.
X = np.arange(xmin, xmax, (xmax - xmin) / num_points)
Y = np.arange(ymin, ymax, (ymax - ymin) / num_points)
X, Y = np.meshgrid(X, Y)
#Z = np.sin(np.sqrt(X**2 + Y**2))
# Z = X * Y * np.sin(X) * np.cos(Y) + X * Y 
# Z = generate(X/20+10, Y/20+10, 0.0)
# Z = generate2(X, Y, 10.0, 10.0)
Z = X * Y

zmin, zmax = np.min(Z), np.max(Z)

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
ax = fig.gca(projection='3d')
ax.plot_surface(X, Y, Z, rstride=stride, cstride=stride, alpha=0.3)

if add_contours:
    # Push the limits out slightly for a clear view of the contours.
    xmin, xmax = scale_axes(xmin, xmax, contour_scaler)
    ymin, ymax = scale_axes(ymin, ymax, contour_scaler)
    zmin, zmax = scale_axes(zmin, zmax, contour_scaler)
    # Add the contour plots to the walls.
    cset = ax.contour(X, Y, Z, zdir='z', offset=zmin, cmap=cm.coolwarm)
    cset = ax.contour(X, Y, Z, zdir='x', offset=xmin, cmap=cm.coolwarm)
    cset = ax.contour(X, Y, Z, zdir='y', offset=ymax, cmap=cm.coolwarm)

# Axes limits.
ax.set_xlim(xmin, xmax)
ax.set_ylim(ymin, ymax)
ax.set_zlim(zmin, zmax)

# Axes labels.
ax.set_xlabel(xlabel)
ax.set_ylabel(ylabel)
ax.set_zlabel(zlabel)

ax.set_title(title)
    
plt.show()

# Save the dimensions of this plot to its .info file.
size_inches = fig.get_size_inches()
print "Size in inches:", size_inches
info = open(infofile, 'w')
info.write(str(size_inches[0]) + ' ' + str(size_inches[1]))
info.close()

fig.savefig(outfile, transparent=True, format='pdf', pad_inches=0.0, bbox_inches='tight')

