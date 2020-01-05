package org.distmatrix.actor;

import akka.actor.typed.ActorRef;

import java.util.List;
import java.util.concurrent.Future;

public class DistMatrixTask {
    public final ActorRef<Future<DistMatrix>> replyTo;
    public final int numWorkers;
    public final List<Point> dataPoints;

    public DistMatrixTask(ActorRef<Future<DistMatrix>> replyTo, int numWorkers, List<Point> dataPoints) {
        this.replyTo = replyTo;
        this.numWorkers = numWorkers;
        this.dataPoints = dataPoints;
    }

    @Override
    public String toString() {
        return "DistMatrixTask{" +
                "numWorkers=" + numWorkers +
                ", dataPoints=" + dataPoints +
                '}';
    }
}
