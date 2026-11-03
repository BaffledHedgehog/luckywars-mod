"""Exercise the actual pure-Java checkpoint timeline without starting Minecraft."""
from pathlib import Path
import json
import os
import subprocess

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build/verification/vanity-schedule-2026-11-03"
HARNESS = r'''
package me.skywars2.game;
public final class VanityScheduleProbe {
    private static void require(boolean value,String why){if(!value)throw new AssertionError(why);}
    public static void main(String[] args){
        boolean minimum=false,maximum=false;
        for(long seed=-100;seed<100;seed++){
            var clock=new VanityCheckpointSchedule(seed);
            long previous=0;
            for(int n=0;n<100;n++){
                var state=clock.state();long delta=state.nextAt()-previous;
                require(delta>=600&&delta<=12000&&delta%20==0,"inclusive whole-second interval");
                minimum|=delta==600;maximum|=delta==12000;
                var paused=clock.state();clock.tick(false);
                require(clock.state().equals(paused),"paused time cannot advance");
                clock=new VanityCheckpointSchedule(new VanityCheckpointSchedule.State(seed,state.nextAt()-1,state.ordinal(),state.nextAt()));
                require(!clock.due(),"deadline is not early");clock.tick(true);require(clock.due(),"deadline tick is included");
                var saved=clock.afterCapture();
                var replay=new VanityCheckpointSchedule(saved);
                require(replay.afterCapture().equals(saved),"an extra manual point does not reroll the future");
                replay.tick(true);replay=new VanityCheckpointSchedule(saved);
                require(replay.state().equals(saved),"rewind restores elapsed time and all future draws");
                previous=state.nextAt();clock=replay;
            }
        }
        require(minimum&&maximum,"both 30 and 600 seconds are reachable");
        var original=new VanityCheckpointSchedule(42L);
        var future=original.state();
        long[] deadlines=new long[10];
        for(int n=0;n<10;n++){
            deadlines[n]=future.nextAt();
            future=new VanityCheckpointSchedule(new VanityCheckpointSchedule.State(future.seed(),future.nextAt(),future.ordinal(),future.nextAt())).afterCapture();
        }
        var delayed=new VanityCheckpointSchedule(new VanityCheckpointSchedule.State(42L,deadlines[4]+1,0,deadlines[0]));
        require(delayed.afterCapture().nextAt()==deadlines[5],"construction delays skip past points but preserve the fixed future");
        require(new VanityCheckpointSchedule(42L).state().equals(original.state()),"acquisition seed fixes the schedule");
        require(!new VanityCheckpointSchedule(43L).state().equals(original.state()),"different acquisitions have independent schedules");
        System.out.println("PASS vanity schedule: 20000 intervals, inclusive boundaries, pause, rewind, fixed future and delayed construction");
    }
}
'''

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    java_home = Path(os.environ.get("JAVA_HOME", ROOT.parent / "runtime/jdk-25"))
    source = OUT / "VanityScheduleProbe.java"
    source.write_text(HARNESS, encoding="utf-8")
    classes = OUT / "schedule-classes"
    classes.mkdir(exist_ok=True)
    subprocess.run([str(java_home / "bin/javac.exe"), "-d", str(classes),
                    str(ROOT / "src/main/java/me/skywars2/game/VanityCheckpointSchedule.java"), str(source)], check=True)
    result = subprocess.run([str(java_home / "bin/java.exe"), "-cp", str(classes),
                             "me.skywars2.game.VanityScheduleProbe"], check=True, text=True, capture_output=True)
    print(result.stdout.strip())
    (OUT / "schedule-results.json").write_text(json.dumps({"passed": True, "output": result.stdout.strip()}, indent=2)+"\n", encoding="utf-8")

if __name__ == "__main__":
    main()
