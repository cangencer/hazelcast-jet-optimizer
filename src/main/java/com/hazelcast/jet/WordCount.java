package com.hazelcast.jet;

import com.hazelcast.jet.config.EdgeConfig;
import com.hazelcast.jet.core.DAG;
import com.hazelcast.jet.core.Edge;
import com.hazelcast.jet.core.Vertex;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.Sources;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.hazelcast.jet.Traversers.traverseArray;
import static com.hazelcast.jet.aggregate.AggregateOperations.counting;
import static com.hazelcast.jet.function.Functions.wholeItem;

public class WordCount {

    private static Pipeline buildPipeline() {
        Pattern delimiter = Pattern.compile("\\W+");
        Pipeline p = Pipeline.create();
        p.drawFrom(Sources.<Long, String>map("lines"))
         .flatMap(e -> traverseArray(delimiter.split(e.getValue().toLowerCase())))
         .filter(word -> !word.isEmpty())
         .groupingKey(wholeItem())
         .aggregate(counting())
         .drainTo(Sinks.map("counts"));
        return p;
    }

    public static void main(String[] args) {
        Map<String, Integer> dict = parseArgs(args);
//        System.out.println("Using input:");
//        for (Entry<String, Integer> e : dict.entrySet()) {
//            System.out.println(e.getKey() + "=" + e.getValue());
//        }

        JetInstance client = Jet.newJetClient();
        try {
            Pipeline p = buildPipeline();
            DAG dag = p.toDag();

            for (Vertex vertex : dag) {
                String vertexKey = "vertex." + vertex.getName() + ".parallelism";
//                System.out.println(vertexKey);
                Integer parallelism = dict.get(vertexKey);
                if (parallelism != null) {
                    vertex.localParallelism(parallelism);
                }
                for (Edge edge : dag.getOutboundEdges(vertex.getName())) {
                    String edgeKey = "edge." + edge.getSourceName() + "-" + edge.getDestName() + ".queueSize";
//                    System.out.println(edgeKey);
                    Integer queueSize = dict.get(edgeKey);
                    if (queueSize != null) {
                        edge.setConfig(new EdgeConfig().setQueueSize(queueSize));
                    }
                }
            }

            long start = System.nanoTime();
            for (int i = 0; i < 4; i++) {
                client.newJob(dag).join();
            }
            long elapsed = System.nanoTime() - start;
            System.out.println("Elapsed " + TimeUnit.NANOSECONDS.toMillis(elapsed) + "ms");
        } finally {
            client.shutdown();
        }
    }

    private static Map<String, Integer> parseArgs(String[] args) {
        Map<String, Integer> map = new HashMap<>();
        for (String arg : args) {
            String[] split = arg.split("=");
            map.put(split[0], Integer.parseInt(split[1]));
        }
        return map;
    }
}
