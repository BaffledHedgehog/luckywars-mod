# Polymer startup and standalone resource pack — 5 November 2026

The reported wait was a server-thread deadlock, not a large resource-pack build.
The main server's valid `polymer/resource_pack.zip` was 10,684 bytes and already
written at 16:16:54. A thread dump captured the server waiting in
`ContentGallery.chunkLoaded -> MapManager.restoreSnapshot -> getChunk` while
that same chunk was still inside Fabric's full-chunk load callback. The outer
gallery restoration had triggered loading through neighbor shape updates.
Diagnostics are retained in `build/verification/polymer-pack-2026-11-05`.

The source fix defers restoration until ordinary gallery ticks, reads only
resident chunks and uses native flags18 to avoid neighbor shape loading.
The callback only inspects its supplied chunk. Recorded container snapshots
remain exact; foreign replacements and imported same-material map writes take
ownership before pending gallery restoration. The map-write hook returns
without allocating a cell key when no old gallery cells remain.

The generated RP currently supplies five note-block model assignments, the
consumed-block model/texture and the Greedstone model. Existing lucky-block
models stay in the author's RP. These assets can be merged into one standalone
RP; Polymer's server translation remains necessary but a second distributed
pack is optional. `tools/export_block_palette.py` exports only that palette and
its dependencies, without copying historical JAR languages into the author's
RP. The prepared supplement is
`resourcepack_template/updates/standalone_polymer_2026_11_05`.

The author RP and main auto-host config are unchanged. Once the supplement is
merged into the distributed RP, `config/polymer/auto-host.json` can set
`enabled:false`. Installed Polymer0.18.2+26.3 bytecode checks this before
automatic generation; its `generateWhenDisabled` default is false. Other mods
can explicitly request generation, so this statement concerns current default
auto-host behavior. Re-export the palette after changing allocated blocks,
Polymer or other palette-consuming mods; do not assume the five assignments
are universal.

Qualified test `88b9ab74` and production `2372f3b1` are installed/prepared.
The focused native content-gallery scenario passes on the exact test archive,
including callback deferral, exact container restoration and foreign/imported
write ownership. The fixture JVM stopped normally and its file snapshots were
restored. All archive entries and retained-class linkage were checked;
10126 other source inputs and 300 author/unrelated-task paths are guarded.
Only three Java families were overlaid; unfinished TNT and author work were
excluded. Main JAR replacement used the safe installer without stopping or
starting the main server. Evidence: `build/verification/gallery-chunk-2026-11-05`.
