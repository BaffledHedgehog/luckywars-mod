import java.nio.file.*;
import java.util.*;
import jdk.jfr.consumer.*;

/** Small streaming JFR summary; recordings and player/network data stay local. */
public final class AnalyzeClientProfile {
    static String quote(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    static String top(Map<String,Long> map,int count){
        var rows=map.entrySet().stream().sorted(Map.Entry.<String,Long>comparingByValue().reversed()).limit(count).toList();
        var out=new StringJoiner(",","{","}");for(var row:rows)out.add(quote(row.getKey())+":"+row.getValue());return out.toString();
    }
    public static void main(String[] args)throws Exception{
        var all=new HashMap<String,Long>();var render=new HashMap<String,Long>();var allocations=new HashMap<String,Long>();
        var waits=new HashMap<String,Long>();var events=new HashMap<String,Long>();long renderSamples=0,gcNanos=0,gcCount=0;
        double cpu=0,machine=0;long cpuCount=0;java.time.Instant first=null,last=null;
        try(var recording=new RecordingFile(Path.of(args[0]))){
            while(recording.hasMoreEvents()){
                var e=recording.readEvent();
                if(e.getEventType().getName().equals("jdk.ActiveRecording")&&e.getString("name").startsWith("skywars_")){
                    first=e.getInstant("recordingStart");last=first.plus(e.getDuration("recordingDuration"));
                    if(last.isAfter(first))break;
                }
            }
        }
        if(first==null||last==null||!last.isAfter(first))throw new IllegalArgumentException("A bounded skywars_ recording is required");
        var allocationThreads=new HashSet<Long>();long skippedAllocationSamples=0;
        try(var recording=new RecordingFile(Path.of(args[0]))){
            while(recording.hasMoreEvents()){
                var e=recording.readEvent();var name=e.getEventType().getName();events.merge(name,1L,Long::sum);
                var at=e.getStartTime();
                if(at.isAfter(last)||e.getEndTime().isBefore(first))continue;
                if(name.equals("jdk.CPULoad")){cpu+=e.getFloat("jvmUser")+e.getFloat("jvmSystem");machine+=e.getFloat("machineTotal");cpuCount++;}
                if(name.equals("jdk.GarbageCollection")){gcCount++;gcNanos+=e.getDuration().toNanos();}
                if(name.equals("jdk.ObjectAllocationSample")){
                    // The first weight may cover allocations from before JFR.start.
                    var thread=e.getThread();long id=thread==null?-1:thread.getJavaThreadId();
                    if(allocationThreads.add(id))skippedAllocationSamples++;
                    else allocations.merge(e.getClass("objectClass").getName(),e.getLong("weight"),Long::sum);
                }
                if(name.equals("jdk.ThreadSleep")||name.equals("jdk.ThreadPark")){
                    var thread=e.getThread();if(thread!=null&&thread.getJavaName().equals("Render thread")){
                        var trace=e.getStackTrace();String caller=trace==null||trace.getFrames().isEmpty()?name:trace.getFrames().getFirst().getMethod().getType().getName()+"."+trace.getFrames().getFirst().getMethod().getName();
                        var begin=at.isBefore(first)?first:at;var end=e.getEndTime().isAfter(last)?last:e.getEndTime();
                        waits.merge(caller,Math.max(0,java.time.Duration.between(begin,end).toNanos()),Long::sum);
                    }
                }
                if(!name.equals("jdk.ExecutionSample")&&!name.equals("jdk.NativeMethodSample"))continue;
                RecordedThread thread=e.getThread("sampledThread");boolean isRender=thread!=null&&thread.getJavaName().equals("Render thread");
                if(isRender)renderSamples++;var trace=e.getStackTrace();if(trace==null)continue;
                var seen=new HashSet<String>();
                for(var frame:trace.getFrames()){
                    var method=frame.getMethod();String label=method.getType().getName()+"."+method.getName();
                    if(seen.add(label)){all.merge(label,1L,Long::sum);if(isRender)render.merge(label,1L,Long::sum);}
                }
            }
        }
        double seconds=first==null?0:java.time.Duration.between(first,last).toNanos()/1e9;
        String result="{\"seconds\":"+seconds+",\"renderSamples\":"+renderSamples+",\"jvmCpuFraction\":"+(cpu/Math.max(cpuCount,1))+",\"machineCpuFraction\":"+(machine/Math.max(cpuCount,1))+
                ",\"gcCount\":"+gcCount+",\"gcDurationMs\":"+gcNanos/1e6+",\"renderInclusiveSamples\":"+top(render,45)+",\"allInclusiveSamples\":"+top(all,25)+
                ",\"sampledAllocationBytes\":"+top(allocations,25)+",\"skippedInitialAllocationSamples\":"+skippedAllocationSamples+
                ",\"allocationNote\":\"Statistical sample weights, not an exact allocation count or rate\",\"renderWaitNanos\":"+top(waits,10)+",\"eventCounts\":"+top(events,24)+"}";
        Files.writeString(Path.of(args[1]),result+"\n");System.out.println(result);
    }
}
