# Verifica si la ruta del bus entra al Hospital Regional de Aguachica.
# Criterio: algun punto de la polilinea OSRM cae dentro del poligono del hospital.

$mlat = 110900.0
$mlon = 110152.0

$poly = @(
    @(8.309617, -73.605605),
    @(8.309610, -73.604245),
    @(8.307661, -73.603532),
    @(8.307651, -73.604961),
    @(8.307677, -73.604991),
    @(8.308786, -73.605397)
)

# + , fuerza un array de arrays en vez de aplanar
$polyXY = @()
foreach ($p in $poly) { $polyXY += , @( (($p[0] - 8.3086) * $mlat), (($p[1] + 73.6045) * $mlon) ) }

function DistPuntoSegmento($px, $py, $ax, $ay, $bx, $by) {
    $dx = $bx - $ax; $dy = $by - $ay
    if ([Math]::Abs($dx) -lt 1e-9 -and [Math]::Abs($dy) -lt 1e-9) {
        return [Math]::Sqrt(($px-$ax)*($px-$ax) + ($py-$ay)*($py-$ay))
    }
    $t = (($px-$ax)*$dx + ($py-$ay)*$dy) / ($dx*$dx + $dy*$dy)
    if ($t -lt 0) { $t = 0 } elseif ($t -gt 1) { $t = 1 }
    $cx = $ax + $t*$dx; $cy = $ay + $t*$dy
    return [Math]::Sqrt(($px-$cx)*($px-$cx) + ($py-$cy)*($py-$cy))
}

function DentroPoligono($px, $py) {
    $n = $polyXY.Count; $inside = $false
    for ($i = 0; $i -lt $n; $i++) {
        $j = ($i + 1) % $n
        $xi = [double]$polyXY[$i][0]; $yi = [double]$polyXY[$i][1]
        $xj = [double]$polyXY[$j][0]; $yj = [double]$polyXY[$j][1]
        if ((($yi -gt $py) -ne ($yj -gt $py)) -and
            ($px -lt ($xj-$xi)*($py-$yi)/($yj-$yi) + $xi)) { $inside = -not $inside }
    }
    return $inside
}

function DistanciaAlHospital($px, $py) {
    $d = 1e18
    for ($k = 0; $k -lt $polyXY.Count; $k++) {
        $k2 = ($k + 1) % $polyXY.Count
        $de = DistPuntoSegmento $px $py $polyXY[$k][0] $polyXY[$k][1] $polyXY[$k2][0] $polyXY[$k2][1]
        if ($de -lt $d) { $d = $de }
    }
    return $d
}

function Evaluar($nombre, $coords) {
    $url = "https://router.project-osrm.org/route/v1/driving/" + ($coords -join ";") + "?overview=full&geometries=geojson&steps=true"
    try { $r = Invoke-RestMethod -Uri $url -TimeoutSec 45 }
    catch { Write-Host ("  {0,-30} ERROR de red" -f $nombre); return }

    $coordsRespuesta = $r.routes[0].geometry.coordinates
    $dentro = 0
    $minDist = 1e18
    for ($i = 0; $i -lt $coordsRespuesta.Count; $i++) {
        $lon = [double]$coordsRespuesta[$i][0]
        $lat = [double]$coordsRespuesta[$i][1]
        $px = ($lat - 8.3086) * $mlat
        $py = ($lon + 73.6045) * $mlon
        if (DentroPoligono $px $py) { $dentro++ }
        $d = DistanciaAlHospital $px $py
        if ($d -lt $minDist) { $minDist = $d }
    }
    $sinNombre = 0
    foreach ($leg in $r.routes[0].legs) { foreach ($s in $leg.steps) {
        if ([string]::IsNullOrWhiteSpace($s.name) -and $s.distance -gt 25) { $sinNombre += $s.distance } } }

    $veredicto = if ($dentro -gt 0) { "ENTRA AL HOSPITAL ($dentro pts)" }
                 else { "rodea, min {0:N0} m del hospital" -f $minDist }
    Write-Host ("  {0,-30} {1,6:N0} m   sinNombre={2,4:N0} m   {3}" -f $nombre, $r.routes[0].distance, $sinNombre, $veredicto)
}

Write-Host ""
Write-Host "1) ESTADO DE LAS PARADAS" -ForegroundColor Cyan
$paradas = @(
    @("1 Parque Nuevo Amanecer", 8.3005,  -73.6150),
    @("2 Patinodromo",            8.3035,  -73.6112),
    @("3 Calle 16 Cra 14",        8.3060,  -73.6085),
    @("4 Calle 11 Cra 14",        8.3076,  -73.6062),
    @("5 Cra 14 con Calle 7",     8.3091,  -73.6042),
    @("5 PROPUESTA Calle 5", 8.30967, -73.60460),
    @("6 Calle 8 Cra 24",         8.3106,  -73.6021),
    @("7 Calle 10 Cra 27A",       8.3121,  -73.6002),
    @("8 Calle 10 Cra 32",        8.3134,  -73.5986),
    @("9 Cancha San Martin",      8.3141,  -73.5974),
    @("UPC sede",                 8.3149249, -73.5963296)
)
foreach ($s in $paradas) {
    $x = ($s[1] - 8.3086) * $mlat; $y = ($s[2] + 73.6045) * $mlon
    if (DentroPoligono $x $y) {
        Write-Host ("  {0,-26} DENTRO DEL HOSPITAL (a {1:N0} m de la entrada)" -f $s[0], (DistanciaAlHospital $x $y)) -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "2) TRAMO AFECTADO 4 -> 5 -> 6" -ForegroundColor Cyan
Evaluar "ACTUAL"        @("-73.6062,8.3076", "-73.6042,8.3091",  "-73.6021,8.3106")

Evaluar "p5 Calle 5 C"  @("-73.6062,8.3076", "-73.60460,8.30967","-73.6021,8.3106")
Evaluar "p5 Calle 5 E"  @("-73.6062,8.3076", "-73.60380,8.30980","-73.6021,8.3106")

Write-Host ""
Write-Host "3) RUTA COMPLETA 1 -> UPC" -ForegroundColor Cyan
Evaluar "ACTUAL"        @("-73.6150,8.3005","-73.6112,8.3035","-73.6085,8.3060","-73.6062,8.3076","-73.6042,8.3091","-73.6021,8.3106","-73.6002,8.3121","-73.5986,8.3134","-73.5974,8.3141","-73.5963296,8.3149249")
Evaluar "CON PARADA CORREGIDA" @("-73.6150,8.3005","-73.6112,8.3035","-73.6085,8.3060","-73.6062,8.3076","-73.60460,8.30967","-73.6021,8.3106","-73.6002,8.3121","-73.5986,8.3134","-73.5974,8.3141","-73.5963296,8.3149249")