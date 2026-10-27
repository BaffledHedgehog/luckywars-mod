"""Exercise real Carpet batch commands, then two native player-rule matches.

Reuse the owned localhost fixture's shutdown, JAR/config and world snapshot handling.
Additional fake-player files are restored only after its normal shutdown.
"""
from pathlib import Path
import hashlib
import json
import time
import uuid

import smoke_antigravity_input as fixture


NAMES = [f"BulkHarus{i}" for i in range(1, 17)] + ["BulkSingle"]
RESULTS = []


class BulkServer(fixture.NativeServer):
    batch_checked = False

    def expect(self, message, cursor=0, timeout=90):
        result = super().expect(message, cursor, timeout)
        if message == "[check] PASS bots" and not self.batch_checked:
            self.batch_checked = True
            self.batch()
        return result

    def command(self, command, expected):
        cursor = self.send(command)
        super().expect(expected, cursor, timeout=60)
        RESULTS.append(command)

    def presence(self, names, absent=()):
        conditions = " ".join([*(f"if entity @a[name={name}]" for name in names),
                               *(f"unless entity @a[name={name}]" for name in absent)])
        command = "execute " + conditions + " run say BULK_PRESENCE_OK"
        # Carpet joins/disconnects on scheduled tasks. Poll only the observation;
        # never retry a spawn/control action to hide a failed first invocation.
        cursor = self.send(command)
        deadline = time.monotonic() + 30
        while time.monotonic() < deadline:
            if any("[Server] BULK_PRESENCE_OK" in line for line in self.lines[cursor:]):
                RESULTS.append(command)
                return
            time.sleep(.1)
            self.send(command)
        raise AssertionError("Carpet player presence did not match the requested batch")

    def batch(self):
        # Refuse to take ownership of a preexisting connected fixture player.
        self.presence([], NAMES)
        try:
            self.command("player spawn BulkHarus 15", "Carpet spawn requests: 15; already online: 0; rejected: 0.")
            super().expect("BulkHarus15 joined the game")
            self.presence(NAMES[:15], [NAMES[15]])
            self.command("player spawn BulkHarus 15", "Carpet spawn requests: 0; already online: 15; rejected: 0.")
            self.command("player spawn BulkHarus 16", "Carpet spawn requests: 1; already online: 15; rejected: 0.")
            super().expect("BulkHarus16 joined the game")
            self.presence(NAMES[:16])
            self.command("player spawn abcdefghijklmnop 2", "generated player names must be at most 16 characters")
            self.command("player spawn invalid-name 2", "Use letters, digits or underscores")
            self.command("player spawn BulkHarus 0", "Integer must not be less than 1")
            self.command("player spawn BulkHarus 1001", "Integer must not be more than 1000")
            self.command("player BulkSingle spawn", "BulkSingle joined the game")
            self.presence(NAMES)
            # Ordinary Carpet control branches must still resolve to its fake players.
            cursor = self.send("player BulkHarus1 look north", "player BulkHarus1 attack once",
                               "execute if entity @a[name=BulkHarus1] run say BULK_CONTROL_OK")
            super().expect("[Server] BULK_CONTROL_OK", cursor)
            RESULTS.append("ordinary Carpet look/attack branches")
        finally:
            self.send(*(f"player {name} kill" for name in NAMES))
            self.presence([], NAMES)


def main():
    root = fixture.cycle.PROJECT
    server = root / "build/verification/key-reuse-server"
    fixture.cycle.require_stopped(server)
    snapshots = {}
    # Carpet's profile cache and saved fake players must not leak into the fixture.
    paths = [server / "usercache.json"]
    for name in NAMES:
        raw = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
        raw[6] = (raw[6] & 15) | 48
        raw[8] = (raw[8] & 63) | 128
        identity = str(uuid.UUID(bytes=bytes(raw)))
        for folder, suffix in (("playerdata", ".dat"), ("playerdata", ".dat_old"),
                               ("stats", ".json"), ("advancements", ".json")):
            paths.append(server / "world" / folder / (identity + suffix))
    for path in paths:
        snapshots[path] = path.read_bytes() if path.exists() else None
    fixture.NativeServer = BulkServer
    fixture.SUITES = ("player-rules",)
    fixture.PROBES = fixture.CLEANUP = ()
    try:
        fixture.run(server)
    finally:
        fixture.cycle.require_stopped(server)
        for path, original in snapshots.items():
            if original is None:
                path.unlink(missing_ok=True)
            else:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(original)
        base = root / "build/verification/antigravity-input-gameplay-results.json"
        result = json.loads(base.read_text())
        result.update(scope="native-carpet-bulk-player-command", batchChecks=RESULTS,
                      measurementLimit="Real Carpet creation/duplicate/validation/control/removal commands in lobby, followed by two actual native player-rule matches. No load benchmark.")
        (root / "build/verification/bulk-players/gameplay-results.json").write_text(
            json.dumps(result, indent=2) + "\n")


if __name__ == "__main__":
    main()
