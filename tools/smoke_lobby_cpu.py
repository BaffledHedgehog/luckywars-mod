"""Compare controlled lobby CPU work on a stopped isolated Fabric server."""
from pathlib import Path
import argparse
import json
import shutil
import statistics
import smoke_game_cycle as cycle


def run(server_dir, label):
    cycle.SERVER = server_dir.resolve()
    active = None
    try:
        active = cycle.Server(f"lobby-cpu-{label}")
        active.expect("LOADING -> LOBBY")
        for suite in ("bots", "lobby", "lobby-rules", "lobby-inventory", "lobby-cpu"):
            active.check(suite)
        active.stop()
        active = None
        target = cycle.OUT / f"lobby-cpu-{label}.json"
        shutil.copyfile(cycle.SERVER / "lobby-cpu.json", target)
        result = json.loads(target.read_text(encoding="utf-8"))
        for name, samples in result.items():
            if isinstance(samples, list):
                print(f"{label} {name}: median={statistics.median(samples):.1f} ns; samples={samples}")
        print("LOBBY CPU CHECKS PASSED")
    finally:
        if active is not None:
            active.stop()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--server-dir", type=Path, required=True)
    parser.add_argument("--label", choices=("baseline", "optimized"), required=True)
    args = parser.parse_args()
    run(args.server_dir, args.label)
