# What

A minimal akka actor system for the distance matrix computation experiment.

# How

This implementation simply sets up actors according to the requested number of
workers, prepares the computation units as pairs of points, and then partitions these pairs into batches as lists
passed on to the actors, divided in a round robin fashion, and using a latch to count and wait for completion,
and finally print the matrix.

# Run

To test the code or play around use command line args as below to provide input to the driver code:
e.g.:
* '-w' option for number of workers
* '-p' points file path with one point per line in 'x,y' coordinate format
* '-h|--help' print usage
