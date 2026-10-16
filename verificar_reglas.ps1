# Verificacion de reglas de seguridad de Firestore.
# Corre contra el emulador local. No toca datos reales.
#
# Uso:
#   1) Levanta el emulador:  firebase emulators:start --only firestore,auth
#   2) Ejecuta:              powershell -File verificar_reglas.ps1

$ErrorActionPreference = "Stop"
$proyecto = "bienestar-c9bfa"
$db = "http://127.0.0.1:8080/v1/projects/$proyecto/databases/(default)/documents"
$authUrl = "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1"

$ok = 0; $fallos = 0

function Test-Resultado($nombre, $esperado, $codigo) {
  $bien = ($codigo -eq $esperado)
  if ($bien) { $script:ok++; Write-Host "  [OK]   $nombre" -ForegroundColor Green }
  else { $script:fallos++; Write-Host "  [FALLA] $nombre (esperado $esperado, recibido $codigo)" -ForegroundColor Red }
}

function Get-Codigo($url, $method, $headers, $body) {
  try {
    $params = @{ Uri=$url; Method=$method; Headers=$headers; ErrorAction="Stop" }
    if ($body) { $params.Body = ($body | ConvertTo-Json -Depth 8) }
    $r = Invoke-WebRequest @params
    return $r.StatusCode
  } catch { return $_.Exception.Response.StatusCode.value__ }
}

Write-Host "`n=== VERIFICACION DE REGLAS DE SEGURIDAD ===`n" -ForegroundColor Cyan

# --- Preparacion: crear un estudiante ---
$mail = "verif_$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())@test.com"
$t = Invoke-RestMethod -Uri "$authUrl/accounts:signUp?key=fake-api-key" -Method Post `
     -ContentType "application/json" `
     -Body (@{email=$mail; password="clave123"; returnSecureToken=$true}|ConvertTo-Json)
$uid = $t.localId
$hdr = @{ "Content-Type"="application/json"; "Authorization"="Bearer $($t.idToken)" }

Write-Host "[1] Escalada de privilegios" -ForegroundColor Yellow
$body = @{ fields = @{
    rol    = @{ stringValue = "ADMIN" }
    nombre = @{ stringValue = "Auto-Admin" }
} }
$c = Get-Codigo "$db/usuarios/$uid" "Patch" $hdr $body
Test-Resultado "Estudiante NO crea su usuario con rol ADMIN (403)" 403 $c

$c = Get-Codigo "$db/usuarios/$uid" "Patch" $hdr (@{fields=@{rol=@{stringValue="ADMIN"}}})
Test-Resultado "Estudiante NO cambia su propio rol (403)" 403 $c

$c = Get-Codigo "$db/usuarios/$uid" "Patch" $hdr (@{fields=@{telefono=@{stringValue="3001234567"}}})
# 403 es lo correcto: el doc del usuario lo crea el Admin, no el usuario.
# Si el doc no existe, resource.data no resuelve y la regla deniega.
Test-Resultado "Estudiante no crea nada sin doc previo (403)" 403 $c

Write-Host "`n[2] Aislamiento de datos" -ForegroundColor Yellow
$c = Get-Codigo "$db/notas/notaAjena" "Get" $hdr $null
Test-Resultado "Estudiante NO lee notas ajenas (403)" 403 $c

$c = Get-Codigo "$db/citas/citaAjena" "Get" $hdr $null
Test-Resultado "Estudiante NO lee citas ajenas (403)" 403 $c

$c = Get-Codigo "$db/reportes_emocionales/reporteAjeno" "Get" $hdr $null
Test-Resultado "Estudiante NO lee reportes emocionales ajenos (403)" 403 $c

Write-Host "`n[3] Sin autenticacion (Firestore responde 403, no 401)" -ForegroundColor Yellow
$anon = @{ "Content-Type"="application/json" }
$c = Get-Codigo "$db/usuarios" "Get" $anon $null
Test-Resultado "Anonimo NO lee el directorio de usuarios (403)" 403 $c

$c = Get-Codigo "$db/notas" "Get" $anon $null
Test-Resultado "Anonimo NO lee notas (403)" 403 $c

$c = Get-Codigo "$db/rutas_bus/ruta_principal" "Get" $anon $null
Test-Resultado "Anonimo NO lee la ruta del bus (403)" 403 $c

Write-Host "`n[4] Permisos del personal" -ForegroundColor Yellow
$c = Get-Codigo "$db/auditoria" "Get" $hdr $null
Test-Resultado "Estudiante NO lee la auditoria (403)" 403 $c

Write-Host "`n=====================================" -ForegroundColor Cyan
Write-Host "  Correctas: $ok" -ForegroundColor Green
Write-Host "  Fallidas : $fallos" -ForegroundColor $(if($fallos -gt 0){"Red"}else{"Green"})
Write-Host ""

if ($fallos -gt 0) { exit 1 } else { exit 0 }