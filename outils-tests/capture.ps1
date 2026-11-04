param([string]$sortie, [int]$x = 0, [int]$y = 0, [int]$l = 0, [int]$h = 0)
Add-Type @"
using System; using System.Runtime.InteropServices;
public class Dpi { [DllImport("user32.dll")] public static extern bool SetProcessDPIAware(); [DllImport("user32.dll")] public static extern int GetSystemMetrics(int i); }
"@
[Dpi]::SetProcessDPIAware() | Out-Null
Add-Type -AssemblyName System.Drawing
$W = [Dpi]::GetSystemMetrics(0); $H = [Dpi]::GetSystemMetrics(1)
if ($l -eq 0) { $l = $W - $x }; if ($h -eq 0) { $h = $H - $y }
$img = New-Object System.Drawing.Bitmap $l, $h
$g = [System.Drawing.Graphics]::FromImage($img); $g.CopyFromScreen($x, $y, 0, 0, $img.Size)
$img.Save($sortie, [System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $img.Dispose()
"écran $W x $H"
