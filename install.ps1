#Requires -Version 5.1
<#
  安装入口（Windows）。逻辑只在 lib/installer.py 里写一份，这里只做参数映射，
  所以 bash 与 PowerShell 两个平台的命令顺序不会各写一遍再走样。
#>
param(
    [string]$TvHost = $env:TV_HOST,
    [string]$Apk,
    [string]$List,
    [switch]$NoDebloat,
    [switch]$KeepStockHome,
    [string]$StockHome,
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
if ($Apk) { $cli += @('--apk', $Apk) }
if ($List) { $cli += @('--list', $List) }
if ($NoDebloat) { $cli += '--no-debloat' }
if ($KeepStockHome) { $cli += '--keep-stock-home' }
if ($StockHome) { $cli += @('--stock-home', $StockHome) }
if ($DryRun) { $cli += '--dry-run' }
if ($Help) { $cli += '--help' }

$python = @(Get-PythonCommand)
$exe = $python[0]
$prefix = @($python | Select-Object -Skip 1)
& $exe @prefix "$Root/lib/installer.py" install @cli
exit $LASTEXITCODE
