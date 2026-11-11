$ErrorActionPreference = 'Stop'
$wrapperJar = Join-Path $PSScriptRoot 'wrapper\gradle-wrapper.jar'
$expectedHash = '2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046'
if (-not (Test-Path $wrapperJar)) {
    $temporaryJar = "$wrapperJar.$([guid]::NewGuid().ToString('N')).tmp"
    try {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -UseBasicParsing -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar' -OutFile $temporaryJar
        if ((Get-FileHash $temporaryJar -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedHash) {
            throw 'Gradle wrapper checksum mismatch.'
        }
        Move-Item $temporaryJar $wrapperJar
    } finally {
        if (Test-Path $temporaryJar) { Remove-Item $temporaryJar }
    }
}
