# Afina la parada 5 en el carril correcto de Calle 5.
# El carril con coste 0 es aquel por el que el bus ya pasa de largo.

$inv = [System.Globalization.CultureInfo]::InvariantCulture
function Coord($lon, $lat) { return ($lon.ToString("F5",$inv) + "," + $lat.ToString("F5",$inv)) }

$mlat = 110900.0
$mlon = 110152.0

# Poligono del Hospital Regional (para no volver a meter la parada dentro)
$poly = @(
    @(8.309617, -73.605605), @(8.309610, -73.604245), @(8.307661, -73.603532),
    @(8.307651, -73.604961), @(8.307677, -73.604991), @(8.308786, -73.605397)
)
$polyXY = @()
foreach ($p in $poly) { $polyXY += , @( (($p[0] - 8.3086) * $mlat), (($p[1] + 73.6045) * $mlon) ) }

function DentroPoligono($px, $py) {
    $n = $polyXY.Count; $inside = $false
    for ($i = 0; $i -lt $n; $i++) {
        $j = ($i + 1) % $n
        $xi = [double]$polyXY[$i][0]; $yi = [double]$polyXY[$i][1]
        $xj = [double]$polyXY[$j][0]; $yj = [double]$polyXY[$j][1]
        if ((($yi -gt $py) -ne ($yj -gt $py)) -and ($px -lt ($xj-$xi)*($py-$yi)/($yj-$yi) + $xi)) { $inside = -not $inside }
    }
    return $inside
}

function Ruta($coords) {
    $url = "https://router.project-osrm.org/route/v1/driving/" + ($coords -join ";") + "?overview=false"
    try { $r = Invoke-RestMethod -Uri $url -TimeoutSec 45 } catch { return $null }
    if ($r.code -ne "Ok") { return $null }
    return $r.routes[0].distance
}

$p4 = "-73.6062,8.3076"
$p6 = "-73.6021,8.3106"
$lon = -73.60460

$base = Ruta @($p4, $p6)
Write-Host ""
Write-Host ("Ruta natural 4 -> 6: {0:N0} m" -f $base) -ForegroundColor Yellow
Write-Host ""
Write-Host "Barrido fino del carril de Calle 5:" -ForegroundColor Cyan

$mejor = $null
$lat = 8.30958
while ($lat -lt 8.30980) {
    $d = Ruta @($p4, (Coord $lon $lat), $p6)
    if ($d -ne $null) {
        $coste = $d - $base
        $x = ($lat - 8.3086) * $mlat
        $y = ($lon + 73.6045) * $mlon
        $dentro = if (DentroPoligono $x $y) { " DENTRO DEL HOSPITAL" } else { "" }
        Write-Host ("  lat={0}   ruta={1,5:N0} m   coste={2,5:N0} m{3}" -f $lat.ToString("F5",$inv), $d, $coste, $dentro)
        if ($coste -le 5 -and -not $dentro) {
            if ($mejor -eq $null) { $mejor = $lat }
        }
    }
    $lat = $lat + 0.00002
}

Write-Host ""
if ($mejor -ne $null) {
    Write-Host ("CARRIL CORRECTO: lat={0} lon={1}" -f $mejor.ToString("F5",$inv), $lon) -ForegroundColor Green
} else {
    Write-Host "No se encontro carril sin coste" -ForegroundColor Red
}