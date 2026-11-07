# Erzeugt das App-Icon (blauer Kreis, Briefumschlag, goldener Stern) als
#   packaging/app.ico                                    (für die .exe)
#   src/main/resources/.../icon-16|32|48|256.png         (Fenster-Icon in JavaFX)
# Nur nötig, wenn sich das Icon ändern soll; build-exe.ps1 ruft es auf, falls app.ico fehlt.
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$resDir = Join-Path $root "src\main\resources\com\example\phishingforcompliments"

function New-IconBitmap([int]$s) {
    $bmp = New-Object System.Drawing.Bitmap $s, $s, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)

    $blue = [System.Drawing.ColorTranslator]::FromHtml("#1F5FBF")
    $g.FillEllipse((New-Object System.Drawing.SolidBrush $blue), 0, 0, $s - 1, $s - 1)

    # Briefumschlag
    $ex = 0.20 * $s; $ey = 0.34 * $s; $ew = 0.60 * $s; $eh = 0.40 * $s
    $g.FillRectangle([System.Drawing.Brushes]::White, [single]$ex, [single]$ey, [single]$ew, [single]$eh)
    $penWidth = [Math]::Max(1.0, $s / 22.0)
    $pen = New-Object System.Drawing.Pen $blue, ([single]$penWidth)
    $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    $flap = [System.Drawing.PointF[]]@(
        (New-Object System.Drawing.PointF ([single]$ex), ([single]$ey)),
        (New-Object System.Drawing.PointF ([single]($ex + $ew / 2)), ([single]($ey + $eh * 0.58))),
        (New-Object System.Drawing.PointF ([single]($ex + $ew)), ([single]$ey)))
    $g.DrawLines($pen, $flap)

    # Goldener Stern oben rechts
    $cx = 0.76 * $s; $cy = 0.25 * $s; $outer = 0.19 * $s; $inner = $outer * 0.45
    $pts = New-Object System.Collections.Generic.List[System.Drawing.PointF]
    for ($i = 0; $i -lt 10; $i++) {
        $r = if ($i % 2 -eq 0) { $outer } else { $inner }
        $a = [Math]::PI / 2 + $i * [Math]::PI / 5
        $pts.Add((New-Object System.Drawing.PointF ([single]($cx + [Math]::Cos($a) * $r)), ([single]($cy - [Math]::Sin($a) * $r))))
    }
    $gold = [System.Drawing.ColorTranslator]::FromHtml("#F2C230")
    $g.FillPolygon((New-Object System.Drawing.SolidBrush $gold), $pts.ToArray())
    if ($s -ge 32) {
        $outline = New-Object System.Drawing.Pen ([System.Drawing.ColorTranslator]::FromHtml("#B8860B")), ([single][Math]::Max(1.0, $s / 64.0))
        $g.DrawPolygon($outline, $pts.ToArray())
    }
    $g.Dispose()
    return $bmp
}

function Get-PngBytes($bmp) {
    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    return $ms.ToArray()
}

# Klassischer 32-bit-DIB-Eintrag (für kleine Größen am kompatibelsten)
function Get-DibBytes($bmp) {
    $s = $bmp.Width
    $rect = New-Object System.Drawing.Rectangle 0, 0, $s, $s
    $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $stride = $data.Stride
    $raw = New-Object byte[] ($stride * $s)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $raw, 0, $raw.Length)
    $bmp.UnlockBits($data)

    $maskRow = [int]([Math]::Floor(($s + 31) / 32) * 4)
    $ms = New-Object System.IO.MemoryStream
    $w = New-Object System.IO.BinaryWriter $ms
    $w.Write([int]40); $w.Write([int]$s); $w.Write([int]($s * 2)); $w.Write([int16]1); $w.Write([int16]32)
    $w.Write([int]0); $w.Write([int]($s * $s * 4 + $maskRow * $s)); $w.Write([int]0); $w.Write([int]0); $w.Write([int]0); $w.Write([int]0)
    for ($y = $s - 1; $y -ge 0; $y--) { $w.Write($raw, $y * $stride, $s * 4) }   # BGRA, von unten nach oben
    $w.Write((New-Object byte[] ($maskRow * $s)))                                 # AND-Maske (Alpha genügt)
    $w.Flush()
    return $ms.ToArray()
}

$sizes = 16, 24, 32, 48, 64, 128, 256
$images = @()
foreach ($s in $sizes) {
    $bmp = New-IconBitmap $s
    $bytes = if ($s -ge 128) { Get-PngBytes $bmp } else { Get-DibBytes $bmp }
    $images += , @($s, $bytes)
    if (@(16, 32, 48, 256) -contains $s) {
        $bmp.Save((Join-Path $resDir "icon-$s.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    }
    $bmp.Dispose()
}

# ICO-Datei zusammensetzen
$ms = New-Object System.IO.MemoryStream
$w = New-Object System.IO.BinaryWriter $ms
$w.Write([int16]0); $w.Write([int16]1); $w.Write([int16]$images.Count)
$offset = 6 + 16 * $images.Count
foreach ($img in $images) {
    $s = $img[0]; $len = $img[1].Length
    $dim = if ($s -ge 256) { 0 } else { $s }
    $w.Write([byte]$dim); $w.Write([byte]$dim); $w.Write([byte]0); $w.Write([byte]0)
    $w.Write([int16]1); $w.Write([int16]32); $w.Write([int]$len); $w.Write([int]$offset)
    $offset += $len
}
foreach ($img in $images) { $w.Write([byte[]]$img[1]) }
$w.Flush()
[System.IO.File]::WriteAllBytes((Join-Path $PSScriptRoot "app.ico"), $ms.ToArray())
Write-Output "Icon erzeugt: $(Join-Path $PSScriptRoot 'app.ico') ($($ms.Length) Bytes)"
