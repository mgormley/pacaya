from pandas import *
import time
# For randn: from pylab import *

df1 = read_csv("scripts/plot/df1.csv")
df2 = read_csv("scripts/plot/df2.csv")

df1['col2'] = df1['col2'].map(str)
df2['col2'] = df2['col2'].map(str)
print len(df1)
print df1.head()

start = time.clock()
dfj = merge(df1, df2, on="id")
stop = time.clock()
print "Time for merge: ", stop - start

start = time.clock()
dfgrouped = dfj.groupby("col4_y")

def f(df):
    col = df['col2_x']
    local_idx = df['col1_x'].argmax()
    global_idx = col.index[local_idx]
    argmax = col[global_idx]
    return Series({'min1x' : df['col1_x'].min(),
                      'mean1x' : df['col1_x'].mean(),
                      'max1x' : df['col1_x'].max(),
                      'sum1y' : df['col1_y'].sum(),
                      'count' : len(df),
                      'argMax2x' : argmax,
                      })

dfgb = dfgrouped.apply(f)
stop = time.clock()
print "Time for groupby and apply: ", stop - start
