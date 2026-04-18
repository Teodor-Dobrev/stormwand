# StormWand Runbook

## Project Root
`C:\VS Code Projects\Wanda\forge-1.20.1-47.4.16-mdk`

## Modpack Target
`C:\Users\teodo\curseforge\minecraft\Instances\My MODs\mods`

## Start Dev Client
```powershell
cd 'C:\VS Code Projects\Wanda\forge-1.20.1-47.4.16-mdk'
$env:GRADLE_USER_HOME='C:\VS Code Projects\Wanda\forge-1.20.1-47.4.16-mdk\.gradle-temp'
.\gradlew.bat runClient
```

## Build Jar
```powershell
cd 'C:\VS Code Projects\Wanda\forge-1.20.1-47.4.16-mdk'
$env:GRADLE_USER_HOME='C:\VS Code Projects\Wanda\forge-1.20.1-47.4.16-mdk\.gradle-temp'
.\gradlew.bat jar
```

Output jar:
`build\libs\stormwand-1.0.0.jar`

## Deploy To Modpack (Backup + Copy)
```powershell
$srcJar = 'C:\VS Code Projects\Wanda\forge-1.20.1-47.4.16-mdk\build\libs\stormwand-1.0.0.jar'
$modsDir = 'C:\Users\teodo\curseforge\minecraft\Instances\My MODs\mods'
$todayTag = (Get-Date -Format 'yyyy.MM.dd')

$active = Join-Path $modsDir 'stormwand-1.0.0.jar'
if (Test-Path -LiteralPath $active) {
    $backup = Join-Path $modsDir "stormwand-1.0.0-backup-$todayTag.disabled"
    $n = 1
    while (Test-Path -LiteralPath $backup) {
        $backup = Join-Path $modsDir "stormwand-1.0.0-backup-$todayTag.$n.disabled"
        $n++
    }
    Move-Item -LiteralPath $active -Destination $backup
}

Copy-Item -LiteralPath $srcJar -Destination $active -Force
```

## Quick Verify In Logs
`C:\Users\teodo\curseforge\minecraft\Instances\My MODs\logs\latest.log`

Search for:
- `Found mod file stormwand-1.0.0.jar`
- `stormwand`
- `Failed to handle packet`
