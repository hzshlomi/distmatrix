package org.distmatrix.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.jooq.lambda.tuple.Tuple3;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class ComputeActor extends AbstractBehavior<ComputeActor.Request> {

    public static Behavior<Request> create() {
        return Behaviors.setup(ComputeActor::new);
    }

    private ComputeActor(ActorContext<Request> context) {
        super(context);
    }

    @Override
    public Receive<Request> createReceive() {
        return newReceiveBuilder().onMessage(Request.class, this::onCompute).build();
    }

    private Behavior<Request> onCompute(Request request) {
        getContext().getLog().info("{} executing: {}", getContext().getSelf(), request);
        Response response = new Response();
        response.distances = computeDistances(request.points);
        request.replyTo.accept(response);
        return this;
    }

    private List<Tuple3<IndexedPoint, IndexedPoint, Double>>
    computeDistances(List<Tuple3<Integer, IndexedPoint, IndexedPoint>> pointPairs) {
        List<Tuple3<IndexedPoint, IndexedPoint, Double>> distances = new LinkedList<>();
        double distance;
        for (Tuple3<Integer, IndexedPoint, IndexedPoint> pointPair : pointPairs) {
            var p1 = pointPair.v2.point;
            var p2 = pointPair.v3.point;
            distance = Math.hypot(p1.x - p2.x, p1.y - p2.y);
            distances.add(new Tuple3<>(pointPair.v2, pointPair.v3, distance));
        }
        return distances;
    }

    public static class Request {
        public final ResponseConsumer replyTo;
        public final List<Tuple3<Integer, IndexedPoint, IndexedPoint>> points;

        Request(ResponseConsumer replyTo, List<Tuple3<Integer, IndexedPoint, IndexedPoint>> points) {
            this.replyTo = replyTo;
            this.points = points;
        }

        @Override
        public String toString() {
            return "Request{" +
                    "replyTo=" + replyTo +
                    ", points=" + points +
                    '}';
        }
    }

    public class Response {
        @SuppressWarnings("unused")
        public ActorRef<Request> computeActor() {
            return getContext().getSelf();
        }

        public List<Tuple3<IndexedPoint, IndexedPoint, Double>> distances; // (x,y,distance)
    }

    public interface ResponseConsumer extends Consumer<Response> {
        @Override
        default void accept(ComputeActor.Response response) {
            onComputeResult(response.distances);
        }

        void onComputeResult(List<Tuple3<IndexedPoint, IndexedPoint, Double>> distances);
    }
}
