package performance;

import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import com.google.gson.JsonParser;
import java.util.*;
import jdk.jfr.consumer.*;

/** Summarizes recorded durations and stack traces, not just JFR event counts. */
public final class JfrEvidence {
    public static void main(String[] args) throws Exception {
        Map<String, Long> counts = new TreeMap<>();
        Map<String, double[]> waits = new HashMap<>();
        Instant windowStart=Instant.MIN,windowEnd=Instant.MAX;
        if (args.length>2) {
            var result=JsonParser.parseString(Files.readString(Path.of(args[2]))).getAsJsonObject();
            windowStart=Instant.parse(result.get("startedUtc").getAsString());
            windowEnd=Instant.parse(result.get("completedUtc").getAsString());
        }
        double cpuSum=0, cpuMax=0, gcMs=0, gcMax=0, pauseMs=0, pauseMax=0, heapMax=0, allocationWeight=0;
        long cpuSamples=0;
        try (RecordingFile file = new RecordingFile(Path.of(args[0]))) {
            while (file.hasMoreEvents()) {
                RecordedEvent event=file.readEvent(); String type=event.getEventType().getName();
                if (event.getEndTime().isBefore(windowStart)||event.getStartTime().isAfter(windowEnd)) continue;
                Instant clippedStart=event.getStartTime().isBefore(windowStart)?windowStart:event.getStartTime();
                Instant clippedEnd=event.getEndTime().isAfter(windowEnd)?windowEnd:event.getEndTime();
                double durationMs=Duration.between(clippedStart,clippedEnd).toNanos()/1e6;
                counts.merge(type,1L,Long::sum);
                switch (type) {
                    case "jdk.JavaMonitorEnter", "jdk.SocketRead", "jdk.SocketWrite", "jdk.ThreadPark" -> {
                        String stack=stack(event);
                        String key=type+" | "+stack;
                        double[] stats=waits.computeIfAbsent(key,k->new double[3]);
                        double ms=durationMs;
                        stats[0]++; stats[1]+=ms; stats[2]=Math.max(stats[2],ms);
                    }
                    case "jdk.CPULoad" -> {
                        double cpu=event.getFloat("jvmUser")+event.getFloat("jvmSystem");
                        cpuSum+=cpu; cpuMax=Math.max(cpuMax,cpu); cpuSamples++;
                    }
                    case "jdk.GarbageCollection" -> { gcMs+=durationMs; gcMax=Math.max(gcMax,durationMs); }
                    case "jdk.GCPhasePause" -> { pauseMs+=durationMs; pauseMax=Math.max(pauseMax,durationMs); }
                    case "jdk.GCHeapSummary" -> heapMax=Math.max(heapMax,event.getLong("heapUsed"));
                    case "jdk.ObjectAllocationSample" -> allocationWeight+=event.getLong("weight");
                    default -> { }
                }
            }
        }
        List<Map<String,Object>> top=new ArrayList<>();
        waits.entrySet().stream().sorted((a,b)->Double.compare(b.getValue()[1],a.getValue()[1])).limit(20).forEach(e->
                top.add(Map.of("eventAndStack",e.getKey(),"count",(long)e.getValue()[0],"totalMs",e.getValue()[1],"maxMs",e.getValue()[2])));
        double monitorMs=waits.entrySet().stream().filter(e->e.getKey().startsWith("jdk.JavaMonitorEnter")&&e.getKey().contains("shared.util.DatabaseConnection.withReusableConnection")).mapToDouble(e->e.getValue()[1]).sum();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("eventCounts",counts); result.put("databaseConnectionMonitorWaitMs",monitorMs);
        result.put("jvmCpuMeanFractionOfMachine",cpuSamples==0?0:cpuSum/cpuSamples); result.put("jvmCpuMaxFractionOfMachine",cpuMax);
        result.put("cpuSamples",cpuSamples); result.put("gcCollectionDurationMs",gcMs); result.put("maxGcCollectionDurationMs",gcMax);
        result.put("gcPauseMs",pauseMs); result.put("maxGcPauseMs",pauseMax);
        result.put("windowStart",windowStart.toString()); result.put("windowEnd",windowEnd.toString());
        result.put("maxSampledHeapUsedBytes",heapMax); result.put("sampledAllocationWeightBytes",allocationWeight);
        result.put("topWaitStacks",top);
        result.put("interpretation","Wait durations sum across threads and can exceed elapsed time. Only events exceeding configured thresholds are recorded. GC collection duration is not necessarily stop-the-world pause time. Heap and allocation figures are samples.");
        Files.writeString(Path.of(args[1]),new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }

    private static String stack(RecordedEvent event) {
        if (event.getStackTrace()==null) return "no recorded stack";
        List<String> frames=new ArrayList<>();
        for (RecordedFrame frame:event.getStackTrace().getFrames()) {
            frames.add(frame.getMethod().getType().getName()+"."+frame.getMethod().getName());
            if (frames.size()==16) break;
        }
        return String.join(" <- ",frames);
    }
}
