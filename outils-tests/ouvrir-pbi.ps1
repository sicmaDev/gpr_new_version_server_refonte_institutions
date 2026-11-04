# Ferme Power BI Desktop, ouvre le fichier donné, attend le rendu, agrandit la fenêtre et fait une capture.
param([string]$fichier, [string]$capture, [int]$attente = 70)
Add-Type @"
using System; using System.Runtime.InteropServices;
public class Fen2 { [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c); [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h); }
"@
Get-Process PBIDesktop -ErrorAction SilentlyContinue | Stop-Process -Force
[Threading.Thread]::Sleep(3000)
$d = "$env:TEMP\claude\pbi"; $cible = Join-Path $d (Split-Path $fichier -Leaf); Copy-Item $fichier $cible -Force
Start-Process $cible
$t = 0; while ($t -lt 60) { $p = Get-Process PBIDesktop -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle }; if ($p) { break }; [Threading.Thread]::Sleep(2000); $t += 2 }
[Threading.Thread]::Sleep($attente * 1000)
$p = Get-Process PBIDesktop | Where-Object { $_.MainWindowTitle } | Select-Object -First 1
[Fen2]::ShowWindow($p.MainWindowHandle, 3) | Out-Null; [Fen2]::SetForegroundWindow($p.MainWindowHandle) | Out-Null
[Threading.Thread]::Sleep(4000)
& "$PSScriptRoot\capture.ps1" $capture
