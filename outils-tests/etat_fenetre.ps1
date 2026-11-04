Add-Type @"
using System; using System.Runtime.InteropServices;
public class W2 { [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h); }
"@
Get-Process msedge -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -like "*GPR*" } | ForEach-Object { "reduite=" + [W2]::IsIconic($_.MainWindowHandle) + " titre=" + $_.MainWindowTitle }
