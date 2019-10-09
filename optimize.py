import bayesopt
import numpy as np
import traceback
import subprocess
import time
import os

DEVNULL = open(os.devnull, 'wb')

options = [
	'vertex.mapSource(lines).parallelism',
	'vertex.fused(flat-map, filter).parallelism',
	'vertex.group-and-aggregate-prepare.parallelism',
	'vertex.group-and-aggregate.parallelism',
	'vertex.mapSink(counts).parallelism',
	'edge.mapSource(lines)-fused(flat-map, filter).queueSize',
	'edge.fused(flat-map, filter)-group-and-aggregate-prepare.queueSize',
	'edge.group-and-aggregate-prepare-group-and-aggregate.queueSize',
	'edge.group-and-aggregate-mapSink(counts).queueSize'
]

# Function for testing.
def run_pipeline(Xin):
	params = ['/usr/bin/java', '-cp', 'wordcount-1.0-SNAPSHOT.jar', 'com.hazelcast.jet.WordCount']
	try:
		for i, x in enumerate(Xin.tolist()):
			params.append('%s=%d' % (options[i], x))
		start_time = time.time()
		subprocess.call(params, stderr=DEVNULL)
		elapsed_time = time.time() - start_time
		return elapsed_time
	except Exception, err:
		traceback.print_exc()
		pass

lower = np.array([1, 1, 1, 1, 1, 128, 128, 128, 128]).astype('double')
upper = np.array([36, 36, 36, 36, 36, 4096, 4096, 4096, 4096]).astype('double')

y_out, x_out, error = bayesopt.optimize(run_pipeline, len(options), lower, upper, {})



defaults = np.array([36, 36, 36, 36, 36, 1024, 1024, 1024, 1024])
print("Running with defaults: %s" % defaults)
print(run_pipeline(defaults))
print("Running with best outcome %s" % x_out)
print(run_pipeline(x_out))