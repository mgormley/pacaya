import sys
from ggplot import *
from pandas import *

import numpy as np
import matplotlib as mpl
import matplotlib.ticker as mticker
import matplotlib.transforms as mtransforms
import pylab 
from matplotlib import rcParams
from pylab import Polygon
from matplotlib import rc

# Create some dummy filler data.
d = {'F1' : [1., 2., 3., 4.],
     'propTrainExamples' : [0.2, 0.4, 0.8, 1.0],
     
     }
d['method'] = 'method1'

df = DataFrame(d)
df2 = df.copy()
df2['method'] = 'method2'
df = concat([df, df2])

df['F1'] = np.log(df['propTrainExamples'] * 1000) * 10 + np.random.normal(0, 1, len(df))

print df

# Plot the learning curve.

# ------------- Plot with ggplot --------------------------

p = ggplot(aes(x='propTrainExamples', y='F1', colour='method'), data=df) + \
    geom_point() + \
    geom_line() + \
    labs("Proportion of examples used for training", "SRL F1") + \
    theme_bw()

print p

# ------------- Plot with Pandas --------------------------

df.plot()

# ------------- Plot with Matplotlib --------------------------

#rc('font',**{'family':'sans-serif','sans-serif':['Helvetica']})
### for Palatino and other serif fonts use:
rc('text', usetex=True)
rc('font', family='serif')
#rc('font',**{'family':'serif','serif':['Cambria']})

#mpl.rcParams['text.usetex']=True
#mpl.rcParams['text.latex.unicode']=True

# The following three methods are from:
# http://messymind.net/2012/07/making-matplotlib-look-like-ggplot/

def rstyle(ax): 
    """Styles an axes to appear like ggplot2
    Must be called after all plot and axis manipulation operations have been carried out (needs to know final tick spacing)
    """
    #set the style of the major and minor grid lines, filled blocks
    ax.grid(True, 'major', color='w', linestyle='-', linewidth=1.4)
    ax.grid(True, 'minor', color='0.92', linestyle='-', linewidth=0.7)
    ax.patch.set_facecolor('0.85')
    ax.set_axisbelow(True)
    
    #set minor tick spacing to 1/2 of the major ticks
    ax.xaxis.set_minor_locator(MultipleLocator( (plt.xticks()[0][1]-plt.xticks()[0][0]) / 2.0 ))
    ax.yaxis.set_minor_locator(MultipleLocator( (plt.yticks()[0][1]-plt.yticks()[0][0]) / 2.0 ))
    
    #remove axis border
    for child in ax.get_children():
        if isinstance(child, mpl.spines.Spine):
            child.set_alpha(0)
       
    #restyle the tick lines
    for line in ax.get_xticklines() + ax.get_yticklines():
        line.set_markersize(5)
        line.set_color("gray")
        line.set_markeredgewidth(1.4)
    
    #remove the minor tick lines    
    for line in ax.xaxis.get_ticklines(minor=True) + ax.yaxis.get_ticklines(minor=True):
        line.set_markersize(0)
    
    #only show bottom left ticks, pointing out of axis
    rcParams['xtick.direction'] = 'out'
    rcParams['ytick.direction'] = 'out'
    ax.xaxis.set_ticks_position('bottom')
    ax.yaxis.set_ticks_position('left')
    
    
    if ax.legend_ <> None:
        lg = ax.legend_
        lg.get_frame().set_linewidth(0)
        lg.get_frame().set_alpha(0.5)
        
        
def rhist(ax, data, **keywords):
    """Creates a histogram with default style parameters to look like ggplot2
    Is equivalent to calling ax.hist and accepts the same keyword parameters.
    If style parameters are explicitly defined, they will not be overwritten
    """
    
    defaults = {
                'facecolor' : '0.3',
                'edgecolor' : '0.28',
                'linewidth' : '1',
                'bins' : 100
                }
    
    for k, v in defaults.items():
        if k not in keywords: keywords[k] = v
    
    return ax.hist(data, **keywords)


def rbox(ax, data, **keywords):
    """Creates a ggplot2 style boxplot, is eqivalent to calling ax.boxplot with the following additions:
    
    Keyword arguments:
    colors -- array-like collection of colours for box fills
    names -- array-like collection of box names which are passed on as tick labels

    """

    hasColors = 'colors' in keywords
    if hasColors:
        colors = keywords['colors']
        keywords.pop('colors')
        
    if 'names' in keywords:
        ax.tickNames = plt.setp(ax, xticklabels=keywords['names'] )
        keywords.pop('names')
    
    bp = ax.boxplot(data, **keywords)
    pylab.setp(bp['boxes'], color='black')
    pylab.setp(bp['whiskers'], color='black', linestyle = 'solid')
    pylab.setp(bp['fliers'], color='black', alpha = 0.9, marker= 'o', markersize = 3)
    pylab.setp(bp['medians'], color='black')
    
    numBoxes = len(data)
    for i in range(numBoxes):
        box = bp['boxes'][i]
        boxX = []
        boxY = []
        for j in range(5):
          boxX.append(box.get_xdata()[j])
          boxY.append(box.get_ydata()[j])
        boxCoords = zip(boxX,boxY)
        
        if hasColors:
            boxPolygon = Polygon(boxCoords, facecolor = colors[i % len(colors)])
        else:
            boxPolygon = Polygon(boxCoords, facecolor = '0.95')
            
        ax.add_patch(boxPolygon)
    return bp

# This class is from:
# http://matplotlib.1069221.n5.nabble.com/make-autoscale-view-even-less-tight-td20327.html
# The following seems related:
# http://smithlabsoftware.googlecode.com/svn-history/r431/trunk/MosaicPlanner/Transform.py

class LooseMaxNLocator(mticker.MaxNLocator):
    """
    Select no more than N intervals at nice locations with view 
    limits loosely fitted to the data.  Unlike MaxNLocator, the 
    view limits do not necessarily coincide with tick locations.
    """

    def __init__(self, margin = 0.0, **kwargs):
        """
        Keyword arguments:
        *margin*
            Specifies the minimum size of both the lower and upper 
            margins (between the view limits and the data limits) as 
            a fraction of the data range.  Must be non-negative.
        Remaining keyword arguments are passed to MaxNLocator.
        """
        mticker.MaxNLocator.__init__(self, **kwargs)
        if margin < 0:

            raise ValueError('The margin must be non-negative.')
        self._margin = margin

    def view_limits(self, dmin, dmax):
        # begin partial duplication of MaxNLocator.view_limits
        if self._symmetric:
            maxabs = max(abs(dmin), abs(dmax))
            dmin = -maxabs
            dmax = maxabs
        dmin, dmax = mtransforms.nonsingular(dmin, dmax, expander=0.05)
        # end duplication
        margin = self._margin * (dmax - dmin)  # fraction of data range
        vmin = dmin - margin  # expand the view
        vmax = dmax + margin
        bin_boundaries = self.bin_boundaries(vmin, vmax)
            # locate ticks with MaxNLocator
        # Note: If the lines below change vmin or vmax, the bin boundaries
        # later calculated by MaxNLocator.__call__ may differ from those
        # calculated here.
        vmin = min(vmin, max(bin_boundaries[bin_boundaries <= dmin]))
            # expand view to the highest tick below or touching the data
        vmax = max(vmax, min(bin_boundaries[bin_boundaries >= dmax]))
            # expand view to the lowest tick above or touching the data
        return np.array([vmin, vmax])

fig, ax = plt.subplots()
fig.set_size_inches(4,3)

for m in df['method'].unique():
    sf = df[df.method == m]
    ax.plot(sf['propTrainExamples'], sf['F1'], 'o-', label=m)
ax.set_xlabel("Proportion of examples used for training")
ax.set_ylabel("SRL F1")
lgd = ax.legend(loc='center left', bbox_to_anchor=(1, 0.5)) #, borderaxespad=0.)
#fig.figlegend()
ax.xaxis.set_major_locator(
    LooseMaxNLocator(nbins=9, steps=[1, 2, 5, 10], margin=0.1))
ax.yaxis.set_major_locator(
    LooseMaxNLocator(nbins=9, steps=[1, 2, 5, 10], margin=0.1))
ax.autoscale_view()

rstyle(ax)

plt.show()

fig.savefig('samplefigure.pdf', bbox_extra_artists=(lgd,), bbox_inches='tight')
