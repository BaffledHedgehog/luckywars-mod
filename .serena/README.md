# Java codebase MCP

Serena provides Java symbol lookup, method bodies, implementations and references.
Use it for Java navigation; keep `rg` for exact text, JSON and legacy datapack functions.
Call `initial_instructions` once after connecting. Request individual symbols rather
than complete large classes. Editing remains with the normal repository tools.

## Local installation

Codex server name: `luckywars_codebase`, stdio transport, explicit project path.
Its registration is in `C:/Users/yarik_temp/.codex/config.toml`; unrelated Codex
settings are preserved. `codex mcp get luckywars_codebase` shows its configuration.

The isolated Python environment is at
`C:/Users/yarik_temp/.codex/mcp/serena/venv` (`serena-agent==2.1.0`).
Serena state is under the adjacent `state` directory. No API key is required.

Java uses a dedicated copy of the installed Red Hat Java language server
(`jdtls-1.58.0`), plus the project's `runtime/jdk-25`.
Machine-specific paths are in ignored `project.local.yml`. That file overrides
the complete `ls_specific_settings` map, so keep the general Java settings there
as well when changing it. Do not commit machine paths or Serena caches.

The local Java workspace is `C:/Users/yarik_temp/.codex/mcp/serena/java-navigation`.
It links the real Java source directory and uses the 144 resolved dependency JARs
from this project's existing VS Code Java classpath. Sources are not copied.
New Java files are discovered in the same source tree. A source-only Eclipse
workspace avoids Gradle's initial whole-repository resource scan, which otherwise
walks the large verification/build directories before applying resource filters.

After changing Gradle dependencies, let VS Code finish its Java/Gradle refresh,
run the local helper below, then reconnect MCP before relying on references:

```powershell
& C:/Users/yarik_temp/.codex/mcp/serena/venv/Scripts/python.exe C:/Users/yarik_temp/.codex/mcp/serena/refresh-java-workspace.py
```

The helper reads the existing IDE classpath and updates only the private MCP
workspace and ignored local configuration. Its captured IDE workspace path may
need updating if VS Code creates a different workspace-storage directory.

Gradle import, background compilation, annotation processing and automatic
build-configuration updates are disabled in the local workspace. Lombok AST
transforms are disabled for the MCP process with `-Dlombok.disable=true`: the
project does not use Lombok, and its extra AST work slowed initial indexing.
The JVM heap limit is 1536 MB; this is a separate developer process, not Minecraft.

MCP exposes navigation tools only through Codex's `enabled_tools` list; the project
also uses `read_only: true`. Generated builds, worlds and binary assets are excluded.
The first Java import can take longer than subsequent lookups.

Connection validation uses actual MCP requests: initialization, project config,
`LbcManaLevelups` symbol overview, the `resolve` method body and references to
`apply`, including the call from `LbcLoadouts`. Local receipts are under
`C:/Users/yarik_temp/.codex/tmp/codebase-mcp-setup/`.

After adding the server, reopen the Codex session/extension if the new tools are
absent. This does not require a Minecraft server restart. To remove the local
registration: `codex mcp remove luckywars_codebase`.

Configuration references:
[Codex MCP](https://developers.openai.com/codex/mcp/),
[Serena configuration](https://oraios.github.io/serena/02-usage/050_configuration.html),
[Serena clients](https://oraios.github.io/serena/02-usage/030_clients.html).
