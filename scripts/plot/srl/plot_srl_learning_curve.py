from ggplot import *
from pandas import *

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

# ------------- Plot with Matplotlib --------------------------

import numpy as np
import matplotlib as mpl
import matplotlib.ticker as mticker
import matplotlib.transforms as mtransforms

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

# Examples
from matplotlib import rc
#rc('font',**{'family':'sans-serif','sans-serif':['Helvetica']})
### for Palatino and other serif fonts use:
rc('text', usetex=True)
rc('font', family='serif')
#rc('font',**{'family':'serif','serif':['Cambria']})

#mpl.rcParams['text.usetex']=True
#mpl.rcParams['text.latex.unicode']=True

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
plt.show()

fig.savefig('samplefigure.pdf', bbox_extra_artists=(lgd,), bbox_inches='tight')
