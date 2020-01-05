package org.distmatrix.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple3;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class DistMatrixActor extends AbstractBehavior<DistMatrixTask> {

    public static Behavior<DistMatrixTask> create() {
        return Behaviors.setup(DistMatrixActor::new);
    }

    private DistMatrixActor(ActorContext<DistMatrixTask> context) {
        super(context);
    }

    @Override
    public Receive<DistMatrixTask> createReceive() {
        return newReceiveBuilder().onMessage(DistMatrixTask.class, this::onCompute).build();
    }

    private Behavior<DistMatrixTask> onCompute(DistMatrixTask command) {
        getContext().getLog().info("{} executing: {}", getContext().getSelf(), command);
        Future<DistMatrix> futureDistMatrix = computeDist(command.numWorkers, command.dataPoints);
        command.replyTo.tell(futureDistMatrix);
        return this;
    }

    @SuppressWarnings("unchecked")
    private Future<DistMatrix> computeDist(final int numWorkers, List<Point> dataPoints) {

        final int numPoints = dataPoints.size();
        if (numPoints <= 1) {
            return CompletableFuture.completedFuture(new DistMatrix(0));
        }

        // assign an index to the points
        final Seq<IndexedPoint> indexedPointsSeq = Seq.seq(dataPoints).zipWithIndex(IndexedPoint::of);
        final IndexedPoint[] indexedPointsArr = indexedPointsSeq.toArray(IndexedPoint[]::new);

        // create N*(N-1)/2 point pairs (half matrix)
        final int numPairs = numPoints * (numPoints - 1) / 2;

        // decide batch size
        final int batchSize = batchSizeFor(numPairs);
        getContext().getLog().info("Will use batches of size: {}", batchSize);

        // (batch-num, point-x, point-y)
        final Tuple3<Integer, IndexedPoint, IndexedPoint>[] pointsPairs = new Tuple3[numPairs];
        int index = 0;
        for (int i = 1; i < numPoints; i++) {
            var pointY = indexedPointsArr[i];
            for (int j = 0; j < i; j++, index++) {
                var pointX = indexedPointsArr[j];
                pointsPairs[index] = new Tuple3<>(index / batchSize, pointX, pointY);
            }
        }

        // partition pairs into batch-groups for the workers
        Map<Long, List<Tuple3<Integer, IndexedPoint, IndexedPoint>>> groups =
                Seq.seq(pointsPairs, 0, numPairs)
                        .groupBy(x -> (long) x.v1);

        // make actors (could use akka pool router with round robin as well, but sticking to requirements..)
        ActorRef<ComputeActor.Request>[] children = new ActorRef[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            children[i] = getContext().spawn(ComputeActor.create(), "computeActor-" + i);
        }

        // make reply target (could be an actor, but chose a non-actor since reply goes to main..)
        final DistMatrixBuilder distMatrixBuilder = new DistMatrixBuilder(numPoints, numPairs);

        // spread the work
        for (var group : groups.entrySet()) {
            int workerId = (int) (group.getKey() % numWorkers);
            var pointList = group.getValue();
            children[workerId].tell(new ComputeActor.Request(distMatrixBuilder, pointList));
        }

        // return a future
        return distMatrixBuilder;
    }

    private int batchSizeFor(final int numPairs) {
        double val = Math.max(1, Math.floor(Math.log(numPairs) / Math.log(2))); // approach log2(numPairs)
        return (int) val;
    }

    private static class DistMatrixBuilder implements ComputeActor.ResponseConsumer, Future<DistMatrix> {
        private final DistMatrix distanceMatrix;
        private final CountDownLatch latch;

        DistMatrixBuilder(int size, int numPairs) {
            this.distanceMatrix = new DistMatrix(size);
            this.latch = new CountDownLatch(numPairs);
        }

        @Override
        public void onComputeResult(List<Tuple3<IndexedPoint, IndexedPoint, Double>> distances) {
            for (Tuple3<IndexedPoint, IndexedPoint, Double> t : distances) {
                this.distanceMatrix.set(t.v1.index, t.v2.index, t.v3);
                this.latch.countDown();
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public DistMatrix get() throws InterruptedException {
            latch.await();
            return distanceMatrix;
        }

        @Override
        public DistMatrix get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            if (latch.await(timeout, unit)) {
                return distanceMatrix;
            } else {
                throw new TimeoutException("Timed out while waiting for count to zero out");
            }
        }

        @Override
        public String toString() {
            return "DistMatrixBuilder{" +
                    ", latch=" + latch.getCount() +
                    '}';
        }
    }
}
