# Checks whether Astra co-op can reach a partner over Tailscale.
#
# Run it with no arguments:   .\scripts\coop-check.ps1
#
# Reports this machine's relay address, lists anyone else on the tailnet, and
# pings the first peer it finds so you know the tunnel works before launching
# the game. Nothing here needs editing.

$ErrorActionPreference = 'Stop'

$exe = 'C:\Program Files\Tailscale\tailscale.exe'
if (-not (Test-Path $exe)) {
    Write-Output 'Tailscale is not installed at the usual location.'
    Write-Output 'Install it first, then run this again.'
    exit 1
}

try {
    $status = & $exe status --json | ConvertFrom-Json
} catch {
    Write-Output "Could not read Tailscale status: $($_.Exception.Message)"
    Write-Output 'Is the Tailscale app running and signed in?'
    exit 1
}

if ($status.BackendState -ne 'Running') {
    Write-Output "Tailscale is installed but not connected (state: $($status.BackendState))."
    Write-Output 'Open the Tailscale tray app and sign in, then run this again.'
    exit 1
}

$me = $status.Self.TailscaleIPs[0]
Write-Output "Tailscale is connected."
Write-Output "  This machine : $($status.Self.HostName)"
Write-Output "  Relay address: $me     <-- your partner types this into the RELAY box"
Write-Output "  Relay port   : 5001    (the default; leave it alone)"
Write-Output ''

# $status.Peer is an object keyed by node id, not an array, so walk its properties.
$peers = @()
if ($status.Peer) {
    $peers = @($status.Peer.PSObject.Properties | ForEach-Object { $_.Value })
}

if ($peers.Count -eq 0) {
    Write-Output 'No peers listed on this tailnet.'
    Write-Output ''
    Write-Output 'IMPORTANT: this does NOT prove your partner cannot reach you.'
    Write-Output 'Tailscale device sharing is one-directional. If you shared THIS'
    Write-Output 'machine with them, they see it in their device list but their'
    Write-Output 'machines never join your tailnet, so nothing shows up here even'
    Write-Output 'though the connection works perfectly.'
    Write-Output ''
    Write-Output 'The only reliable test runs on THEIR machine, in PowerShell:'
    Write-Output "    Test-NetConnection $me -Port 5001"
    Write-Output '  TcpTestSucceeded : True  -> network is fine, it is a game-side problem'
    Write-Output '  TcpTestSucceeded : False -> they are not really on the tailnet yet'
    Write-Output ''
    Write-Output 'If they have not been invited at all:'
    Write-Output '  1. Open https://login.tailscale.com/admin/machines'
    Write-Output "  2. Click the ... next to $($status.Self.HostName), choose Share"
    Write-Output '  3. Send them the link; they install Tailscale, sign in with'
    Write-Output '     their OWN account, and accept it.'
    exit 0
}

Write-Output 'Peers on this tailnet:'
foreach ($p in $peers) {
    $state = if ($p.Online) { 'online' } else { 'OFFLINE' }
    Write-Output "  $($p.HostName)  $($p.TailscaleIPs[0])  $state"
}

$live = @($peers | Where-Object { $_.Online })
if ($live.Count -eq 0) {
    Write-Output ''
    Write-Output 'None of them are online right now. Ask them to open Tailscale.'
    exit 0
}

$target = $live[0].TailscaleIPs[0]
Write-Output ''
Write-Output "Pinging $($live[0].HostName) at $target ..."
& $exe ping $target

Write-Output ''
Write-Output 'If those replies came back, co-op will work:'
Write-Output '  - You   : main menu -> ASTRA CO-OP -> RUN THE RELAY HERE, then pick a role'
Write-Output "  - Them  : RELAY = $me, PORT = 5001, same ROOM code, pick the other role"
