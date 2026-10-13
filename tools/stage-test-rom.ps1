[CmdletBinding()]
param(
    [Parameter()]
    [string]$RomRoot = (Join-Path $PSScriptRoot '..\..\roms')
)

$ErrorActionPreference = 'Stop'
$archive = Join-Path $RomRoot 'NES\Super Mario Bros. (World).zip'
$destinationDirectory = Join-Path $PSScriptRoot '..\app\src\main\assets\roms'
$destination = Join-Path $destinationDirectory 'super-mario-bros-world.nes'

if (-not (Test-Path -LiteralPath $archive -PathType Leaf)) {
    throw "Authorized test archive not found: $archive`nSee ROM_LIBRARY.md for the private library layout."
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($archive)
try {
    $entries = @($zip.Entries | Where-Object { $_.Name.EndsWith('.nes', [System.StringComparison]::OrdinalIgnoreCase) })
    if ($entries.Count -ne 1) {
        throw "Expected exactly one .nes file in $archive; found $($entries.Count)."
    }
    New-Item -ItemType Directory -Force -Path $destinationDirectory | Out-Null
    [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entries[0], $destination, $true)
} finally {
    $zip.Dispose()
}

$stream = [System.IO.File]::OpenRead($destination)
try {
    $header = New-Object byte[] 4
    if ($stream.Read($header, 0, 4) -ne 4 -or
        $header[0] -ne 0x4E -or $header[1] -ne 0x45 -or
        $header[2] -ne 0x53 -or $header[3] -ne 0x1A) {
        throw 'The extracted file does not have a valid iNES header.'
    }
} finally {
    $stream.Dispose()
}

$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $destination).Hash
Write-Host "Staged authorized NES test ROM locally: $destination"
Write-Host "SHA-256: $hash"
Write-Host 'This file is ignored by Git and must never be committed.'
