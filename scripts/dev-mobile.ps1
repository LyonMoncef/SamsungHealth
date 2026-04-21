# dev-mobile.ps1 — Expose WSL2 FastAPI (port 8001) on LAN for Android app testing
# Run as Administrator in PowerShell on Windows
#
# If blocked by execution policy, run with:
#   PowerShell -ExecutionPolicy Bypass -File "\\wsl.localhost\Ubuntu\home\tats\MyPersonalProjects\SamsungHealth\scripts\dev-mobile.ps1"

$wslIp = (wsl -d Ubuntu hostname -I).Trim().Split()[0]
$winIp = (Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias 'Wi-Fi' -ErrorAction SilentlyContinue).IPAddress
if (-not $winIp) {
    $winIp = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object {
        $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '172.*'
    } | Select-Object -First 1).IPAddress
}

if (-not $wslIp) {
    Write-Host 'ERROR: Could not detect WSL2 IP. Make sure WSL2 is running.' -ForegroundColor Red
    exit 1
}

Write-Host ''
Write-Host "WSL2 IP   : $wslIp"
Write-Host "Windows IP : $winIp"
Write-Host ''

# Remove existing rule (idempotent)
netsh interface portproxy delete v4tov4 listenport=8001 listenaddress=0.0.0.0 2>$null
Remove-NetFirewallRule -DisplayName 'WSL2 SamsungHealth 8001' -ErrorAction SilentlyContinue

# Add port forwarding: Windows 0.0.0.0:8001 -> WSL2:8001
netsh interface portproxy add v4tov4 listenport=8001 listenaddress=0.0.0.0 connectport=8001 connectaddress=$wslIp
New-NetFirewallRule -DisplayName 'WSL2 SamsungHealth 8001' -Direction Inbound -LocalPort 8001 -Protocol TCP -Action Allow | Out-Null

Write-Host 'Port forwarding configured:'
Write-Host "  http://${winIp}:8001  ->  WSL2 FastAPI (SamsungHealth)"
Write-Host ''
Write-Host 'Dans l app Android -> Settings -> Backend URL :'
Write-Host "  http://${winIp}:8001"
Write-Host ''
Write-Host 'Assure-toi que le serveur tourne dans WSL2 : make dev'
Write-Host ''
