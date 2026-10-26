#Requires -Version 5.1
<#
  还原入口（Windows）。按 install.sh / install.ps1 写下的记录恢复被停用的包与原桌面。
#>
param(
    [string]$TvHost = $env:TV_HOST,
    [switch]$Uninstall,
    [switch]$DryRun,
    [switch]$Help
)

$ErrorActionPreference = 'Stop'
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

function Get-PythonCommand {
    foreach ($name in 'python3', 'python') {
        $found = Get-Command $name -ErrorAction SilentlyContinue
        if ($found) { return @($found.Source) }
    }
    $launcher = Get-Command py -ErrorAction SilentlyContinue
    if ($launcher) { return @($launcher.Source, '-3') }
    throw '需要 Python 3.8+（找不到 python3 / python / py）'
}

$cli = @()
if ($TvHost) { $cli += @('--host', $TvHost) }
if ($Uninstall) { $cli += '--uninstall' }
if ($DryRun) { $cli += '--dry-run' }
if ($Help) { $cli += '--help' }

$python = @(Get-PythonCommand)
$exe = $python[0]
$prefix = @($python | Select-Object -Skip 1)
& $exe @prefix "$Root/lib/installer.py" restore @cli
exit $LASTEXITCODE
