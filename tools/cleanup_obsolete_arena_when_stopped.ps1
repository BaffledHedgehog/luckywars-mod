param(
    [Parameter(Mandatory=$true)][string]$Qualification,
    [switch]$ValidateOnly
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$mainRoot = [System.IO.Directory]::GetParent($projectRoot).FullName
$evidencePath = (Resolve-Path -LiteralPath $Qualification).Path
$evidenceRoot = Join-Path $projectRoot 'build/verification'
if (-not $evidencePath.StartsWith($evidenceRoot + [System.IO.Path]::DirectorySeparatorChar)) { throw 'Qualification outside verification tree' }
$evidence = Get-Content -Raw -LiteralPath $evidencePath | ConvertFrom-Json
$properties = @{}
Get-Content -LiteralPath (Join-Path $mainRoot 'server.properties') | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') { $properties[$matches[1]] = $matches[2] }
}
$worldName = $properties['level-name']
if (-not $worldName) { $worldName = 'world' }
$worldRoot = (Resolve-Path -LiteralPath (Join-Path $mainRoot $worldName)).Path
if (-not $worldRoot.StartsWith($mainRoot + [System.IO.Path]::DirectorySeparatorChar)) { throw 'World outside main server directory' }
$targets = @(
    [System.IO.Path]::GetFullPath((Join-Path $worldRoot 'dimensions/skywars2/arena')),
    [System.IO.Path]::GetFullPath((Join-Path $worldRoot 'data/luckywars/arena-bounds.json'))
)
foreach ($target in $targets) {
    if (-not $target.StartsWith($worldRoot + [System.IO.Path]::DirectorySeparatorChar)) { throw 'Obsolete target outside world' }
    $ancestor = $target
    while ($ancestor -and $ancestor.Length -ge $worldRoot.Length) {
        if ((Test-Path -LiteralPath $ancestor) -and ((Get-Item -LiteralPath $ancestor -Force).Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
            throw "Refusing reparse point: $ancestor"
        }
        $ancestor = [System.IO.Path]::GetDirectoryName($ancestor)
    }
}
if ($ValidateOnly) {
    @{ world = $worldRoot; targets = $targets; qualification = $evidencePath } | ConvertTo-Json
    exit 0
}
$receiptDirectory = [System.IO.Path]::GetDirectoryName($evidencePath)
$installReceipt = Join-Path $receiptDirectory 'main-install.json'
$cleanupReceipt = Join-Path $receiptDirectory 'obsolete-world-cleanup.json'
$sessionPath = Join-Path $worldRoot 'session.lock'
$modPath = Join-Path $mainRoot 'mods/skywars2.jar'
$port = [int]$properties['server-port']
@{ status = 'waiting-for-install-and-normal-shutdown'; targets = $targets; workerPid = $PID } | ConvertTo-Json | Set-Content -Encoding utf8 -LiteralPath $cleanupReceipt
while ($true) {
    $session = $null
    $locked = $false
    try {
        if (-not (Test-Path -LiteralPath $installReceipt)) { Start-Sleep -Seconds 2; continue }
        try { $installed = Get-Content -Raw -LiteralPath $installReceipt | ConvertFrom-Json }
        catch { Start-Sleep -Seconds 2; continue }
        if ($installed.status -in @('superseded','cancelled-main-jar-changed')) {
            @{ status = 'cancelled-install-superseded'; targets = $targets } | ConvertTo-Json | Set-Content -Encoding utf8 -LiteralPath $cleanupReceipt
            exit 0
        }
        if ($installed.status -ne 'installed') { Start-Sleep -Seconds 2; continue }
        if ((Get-FileHash -Algorithm SHA256 -LiteralPath $modPath).Hash.ToLowerInvariant() -ne $evidence.sha256) { throw 'Installed mod differs from qualified migration' }
        try {
            $session = [System.IO.File]::Open($sessionPath,[System.IO.FileMode]::Open,[System.IO.FileAccess]::ReadWrite,[System.IO.FileShare]::ReadWrite)
            $session.Lock(0,1)
            $locked = $true
        } catch [System.IO.IOException] { Start-Sleep -Seconds 2; continue }
        $probe = [System.Net.Sockets.TcpClient]::new()
        try {
            $pending = $probe.BeginConnect('127.0.0.1',$port,$null,$null)
            $accepting = $pending.AsyncWaitHandle.WaitOne(250) -and $probe.Connected
        } finally { $probe.Dispose() }
        if ($accepting) { Start-Sleep -Seconds 2; continue }
        $removed = @()
        foreach ($target in $targets) {
            if (-not (Test-Path -LiteralPath $target)) { continue }
            $resolved = (Resolve-Path -LiteralPath $target).Path
            if ($resolved -ne $target -or -not $resolved.StartsWith($worldRoot + [System.IO.Path]::DirectorySeparatorChar)) { throw 'Unexpected resolved deletion target' }
            $item = Get-Item -LiteralPath $resolved -Force
            if ($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) { throw 'Deletion target became a reparse point' }
            if ($item.PSIsContainer) {
                if (Get-ChildItem -LiteralPath $resolved -Recurse -Force -Attributes ReparsePoint) { throw 'Obsolete world contains a reparse point' }
                $bytes = (Get-ChildItem -LiteralPath $resolved -File -Recurse -Force | Measure-Object -Property Length -Sum).Sum
                Remove-Item -LiteralPath $resolved -Recurse -Force
            } else {
                $bytes = $item.Length
                Remove-Item -LiteralPath $resolved -Force
            }
            $removed += @{ path = $resolved; bytes = $bytes }
        }
        @{ status = 'removed'; removed = $removed; workerPid = $PID; artifactSha256 = $evidence.sha256 } | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 -LiteralPath $cleanupReceipt
        exit 0
    } finally {
        if ($locked) { $session.Unlock(0,1) }
        if ($null -ne $session) { $session.Dispose() }
    }
}
