Add-Type @"
using System; using System.Runtime.InteropServices;
public class W { [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr a, int x, int y, int cx, int cy, uint f);
[DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int c); }
"@
$p = Get-Process msedge -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like "*GPR*" } | Select-Object -First 1
if ($p) { [W]::ShowWindow($p.MainWindowHandle, 9) | Out-Null; [W]::SetWindowPos($p.MainWindowHandle, [IntPtr](-1), 0, 0, 470, 690, 0x0040) | Out-Null; "ok " + $p.MainWindowTitle } else { "fenetre introuvable" }
