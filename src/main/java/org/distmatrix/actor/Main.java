package org.distmatrix.actor;

import static akka.actor.typed.javadsl.AskPattern.ask;

import akka.actor.typed.ActorSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Driver code bootstrapping a minimal (akka) actor system for the distance matrix computation.
 * The strategy taken follows the assignment requirements rather then using ready made facilities such as routers,
 * dispatchers, remoting, etc, and simply sets up actors according to the requested number of workers, prepares the
 * computation units as pairs of points, and then partitions these pairs into batches as lists passed on to the actors,
 * divided in a round robin fashion, and using a latch to count and wait for completion, and finally print the matrix.
 * <p>
 * Each actor logs the points for which it computes distances such as both the thread and actor identifiers are exposed
 * to observe the distribution of the work among the workers.
 * <p>
 * The implementation is fail-fast and does not strive to cover all the edge cases but rather puts a framework that
 * could easily be extended (using routers, dispatchers, remoting etc) to perform efficient distributed computation.
 * <p>
 * To test the code or play around use command line args as below to provide input to the driver code:
 * <pre>
 * e.g.:
 *  '-w' option for number of workers
 *  '-p' points file path with one point per line in 'x,y' coordinate format
 *  '-h|--help' print usage
 * </pre>
 * <p>
 * A few default files (see default-*.points) are provided. 'default-3.points' is used for the no-args run.
 * enjoy :)
 */
public class Main {
    static {
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
    }

    public static void main(String[] argv) throws Exception {

        // process command line
        final Args args = new Args(argv);
        if (args.usage()) {
            args.printUsage();
            return;
        }
        final int numThreads = args.numWorkers();
        final List<Point> dataPoints = getDataPoints(args.pointsFileName());

        computeDistMatrix(numThreads, dataPoints);
    }

    private static List<Point> getDataPoints(String pointsFileName) throws IOException {
        Path pointsFileNamePath = Path.of(pointsFileName).toAbsolutePath();
        System.out.println("Loading points from: " + pointsFileNamePath);
        List<String> lines = Files.readAllLines(pointsFileNamePath);
        List<Point> points = new LinkedList<>();
        for (String line : lines) {
            String[] tokens = line.split(",");
            if (tokens.length != 2) {
                throw new IllegalArgumentException("Bad point format: " + line);
            }
            points.add(Point.of(Double.parseDouble(tokens[0]), Double.parseDouble(tokens[1])));
        }
        return points;
    }

    public static void computeDistMatrix(final int numThreads, final List<Point> dataPoints) throws Exception {

        final ActorSystem<DistMatrixTask> system = ActorSystem.create(DistMatrixActor.create(), "system");

        CompletionStage<Future<DistMatrix>> response =
                ask(
                        system,
                        replyTo -> new DistMatrixTask(replyTo, numThreads, dataPoints),
                        Duration.ofMillis(1000L), // timeout to get response as opposed to actual computation result
                        system.scheduler()
                );

        response.whenComplete((r, x) -> {
            if (x != null) {
                x.printStackTrace();
            } else {
                handleResult(r);
            }
        }).toCompletableFuture().get();

        system.terminate();
    }

    private static void handleResult(Future<DistMatrix> r) {
        try {
            DistMatrix distMatrix = r.get(3000L, TimeUnit.MILLISECONDS);
            System.out.println();
            System.out.println(distMatrix);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
