Add-Type -AssemblyName System.Drawing
$src = "C:\Users\User\.gemini\antigravity\brain\bfa7e1fa-2281-4bf0-be36-50029a51440e\app_logo_simplenotes_1765748901514.png"
$dest = "c:\ProjectVSCode\SimpleNotes\app\src\main\res\drawable\app_logo.png"
Write-Host "Reading from $src"
$img = [System.Drawing.Image]::FromFile($src)
Write-Host "Saving to $dest"
$img.Save($dest, [System.Drawing.Imaging.ImageFormat]::Png)
$img.Dispose()
Write-Host "Done"
