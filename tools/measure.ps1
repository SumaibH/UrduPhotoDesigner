param([string]$Path, [int]$CanvasTop = 307, [int]$CanvasBottom = 1998)

Add-Type -AssemblyName System.Drawing
$bmp = [System.Drawing.Bitmap]::FromFile($Path)
$w = $bmp.Width; $h = $bmp.Height
$rect = New-Object System.Drawing.Rectangle 0,0,$w,$h
$data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$bytes = New-Object byte[] ($stride * $h)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bmp.UnlockBits($data)
$bmp.Dispose()

# ---- artboard = the pure-white (255) page sitting on the ~228 grey backdrop ----
# The left align-kit, the top bar and the bottom panel are white too, so sample only
# bands that none of them occupy: rows above/below the kit for L/R, middle columns for T/B.
function Test-White([int]$x, [int]$y) {
  $i = $y * $stride + $x * 4
  return ($bytes[$i] -eq 255 -and $bytes[$i+1] -eq 255 -and $bytes[$i+2] -eq 255)
}
# Per row take the LONGEST contiguous white run: the page is ~1225px wide, far wider
# than the align kit (~126) or the FAB (~130), so it wins on every row it occupies.
$abL = $w; $abR = -1; $abT = $h; $abB = -1
$yTop = [math]::Max(0, $CanvasTop); $yBot = [math]::Min($h - 1, $CanvasBottom)
for ($y = $yTop; $y -le $yBot; $y += 2) {
  $bestLen = 0; $bestS = -1; $bestE = -1
  $runS = -1
  for ($x = 0; $x -lt $w; $x += 2) {
    if (Test-White $x $y) {
      if ($runS -lt 0) { $runS = $x }
    } else {
      if ($runS -ge 0) {
        $len = $x - $runS
        if ($len -gt $bestLen) { $bestLen = $len; $bestS = $runS; $bestE = $x - 2 }
        $runS = -1
      }
    }
  }
  if ($runS -ge 0) {
    $len = $w - $runS
    if ($len -gt $bestLen) { $bestLen = $len; $bestS = $runS; $bestE = $w - 2 }
  }
  if ($bestLen -ge 700) {
    if ($bestS -lt $abL) { $abL = $bestS }
    if ($bestE -gt $abR) { $abR = $bestE }
    if ($y -lt $abT) { $abT = $y }
    if ($y -gt $abB) { $abB = $y }
  }
}
Write-Output "ARTBOARD L=$abL T=$abT R=$abR B=$abB"

# ---- dark blobs inside the artboard, 4-connected on a 2px grid ----
$gw = [int][math]::Floor($w / 2); $gh = [int][math]::Floor($h / 2)
$lab = New-Object int[] ($gw * $gh)
$dark = New-Object bool[] ($gw * $gh)
for ($gy = 0; $gy -lt $gh; $gy++) {
  $y = $gy * 2
  if ($y -lt $abT -or $y -gt $abB) { continue }
  $row = $y * $stride
  for ($gx = 0; $gx -lt $gw; $gx++) {
    $x = $gx * 2
    if ($x -lt $abL -or $x -gt $abR) { continue }
    $i = $row + $x * 4
    if ($bytes[$i] -lt 70 -and $bytes[$i+1] -lt 70 -and $bytes[$i+2] -lt 70) { $dark[$gy * $gw + $gx] = $true }
  }
}

$next = 0
$stack = New-Object System.Collections.Generic.Stack[int]
$res = @()
for ($p = 0; $p -lt $dark.Length; $p++) {
  if (-not $dark[$p] -or $lab[$p] -ne 0) { continue }
  $next++
  $stack.Push($p); $lab[$p] = $next
  $minx = $gw; $maxx = -1; $miny = $gh; $maxy = -1; $count = 0
  while ($stack.Count -gt 0) {
    $q = $stack.Pop(); $count++
    $qx = $q % $gw; $qy = [int][math]::Floor($q / $gw)
    if ($qx -lt $minx) { $minx = $qx }; if ($qx -gt $maxx) { $maxx = $qx }
    if ($qy -lt $miny) { $miny = $qy }; if ($qy -gt $maxy) { $maxy = $qy }
    foreach ($d in @(-1, 1, -$gw, $gw)) {
      $r = $q + $d
      if ($d -eq -1 -and $qx -eq 0) { continue }
      if ($d -eq 1 -and $qx -eq $gw - 1) { continue }
      if ($r -lt 0 -or $r -ge $dark.Length) { continue }
      if ($dark[$r] -and $lab[$r] -eq 0) { $lab[$r] = $next; $stack.Push($r) }
    }
  }
  if ($count -ge 80) {
    $res += [pscustomobject]@{ L = $minx * 2; T = $miny * 2; R = $maxx * 2; B = $maxy * 2; N = $count }
  }
}

foreach ($b in ($res | Sort-Object T, L)) {
  $cx = [int](($b.L + $b.R) / 2); $cy = [int](($b.T + $b.B) / 2)
  # gaps to each artboard edge, so an alignment reads off directly
  $gl = $b.L - $abL; $gr = $abR - $b.R; $gt = $b.T - $abT; $gb = $abB - $b.B
  Write-Output ("BLOB L={0} T={1} R={2} B={3} cx={4} cy={5} | gapL={6} gapR={7} gapT={8} gapB={9}" -f $b.L, $b.T, $b.R, $b.B, $cx, $cy, $gl, $gr, $gt, $gb)
}
$acx = [int](($abL + $abR) / 2); $acy = [int](($abT + $abB) / 2)
Write-Output "ARTBOARD_CENTER cx=$acx cy=$acy"
