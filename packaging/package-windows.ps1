[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$JdkHome,

    [ValidateSet('Image', 'Installer')]
    [string]$Package = 'Image',

    [string]$WixBin
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

if ($env:OS -ne 'Windows_NT') {
    throw 'Run this script on Windows.'
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkDirectory = (Resolve-Path -LiteralPath $JdkHome).Path
$jpackage = Join-Path $jdkDirectory 'bin\jpackage.exe'
$releaseFile = Join-Path $jdkDirectory 'release'

if (-not (Test-Path -LiteralPath $jpackage -PathType Leaf)) {
    throw 'jpackage.exe was not found. Specify the home directory of a full JDK 21 or newer.'
}
$releaseInfo = Get-Content -LiteralPath $releaseFile -Raw
if ($releaseInfo -notmatch 'JAVA_VERSION="(\d+)') {
    throw 'Cannot determine the JDK version from its release file.'
}
if ([int]$Matches[1] -lt 21) {
    throw 'A JDK 21 or newer is required.'
}

[xml]$pom = Get-Content -LiteralPath (Join-Path $projectRoot 'pom.xml') -Raw -Encoding UTF8
$version = [string]$pom.project.version
if ($version -notmatch '^\d+\.\d+\.\d+$') {
    throw 'The version in pom.xml must contain three numbers, for example 1.0.0.'
}

$jar = Join-Path $projectRoot 'target\alchemap.jar'
if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) {
    throw 'target\alchemap.jar was not found. Run mvn clean package first.'
}

$originalPath = $env:Path
try {
    if ($Package -eq 'Installer') {
        if (-not [string]::IsNullOrWhiteSpace($WixBin)) {
            $wixDirectory = (Resolve-Path -LiteralPath $WixBin).Path
            $env:Path = $wixDirectory + ';' + $originalPath
        }
        if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue) -or
            -not (Get-Command light.exe -ErrorAction SilentlyContinue)) {
            throw 'WiX 3 build tools were not found. Use -WixBin with the directory containing candle.exe and light.exe.'
        }
    }

    $buildDirectory = Join-Path $projectRoot 'target\windows-package'
    $packageInput = Join-Path $buildDirectory 'input'
    $imageParent = Join-Path $buildDirectory 'image'
    $imageDirectory = Join-Path $imageParent 'Alchemap'
    $distribution = Join-Path $projectRoot 'dist'

    # This directory contains only generated packaging files.
    if (Test-Path -LiteralPath $buildDirectory) {
        Remove-Item -LiteralPath $buildDirectory -Recurse -Force
    }
    New-Item -ItemType Directory -Path $packageInput, $imageParent, $distribution -Force | Out-Null
    Copy-Item -LiteralPath $jar -Destination (Join-Path $packageInput 'alchemap.jar')

    $imageArguments = @(
        '--type', 'app-image',
        '--name', 'Alchemap',
        '--app-version', $version,
        '--vendor', 'nydrawandcry',
        '--description', 'Alchemy laboratory for D&D',
        '--input', $packageInput,
        '--main-jar', 'alchemap.jar',
        '--main-class', 'AlchemapApplication',
        '--dest', $imageParent,
        '--java-options', '-Dfile.encoding=UTF-8',
        '--jlink-options', '--strip-native-commands --strip-debug --no-man-pages --no-header-files --bind-services'
    )

    Write-Host 'Creating the application with its own Java runtime...'
    & $jpackage @imageArguments
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage app-image failed with exit code $LASTEXITCODE."
    }

    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'README-Windows.txt') -Destination $imageDirectory
    $zip = Join-Path $distribution "Alchemap-$version-windows.zip"
    Compress-Archive -LiteralPath $imageDirectory -DestinationPath $zip -Force
    Write-Host "Archive: $zip"
    Write-Host "Application: $(Join-Path $imageDirectory 'Alchemap.exe')"

    if ($Package -eq 'Installer') {
        $installerArguments = @(
            '--type', 'exe',
            '--name', 'Alchemap',
            '--app-version', $version,
            '--vendor', 'nydrawandcry',
            '--app-image', $imageDirectory,
            '--dest', $distribution,
            '--install-dir', 'Programs\Alchemap',
            '--win-per-user-install',
            '--win-menu',
            '--win-menu-group', 'Alchemap',
            '--win-shortcut',
            '--win-dir-chooser',
            '--win-upgrade-uuid', 'fa2435eb-db40-4c7c-bfde-a4f621ed8ffa'
        )
        # Keep this upgrade UUID for future versions of this application.
        Write-Host 'Creating the Windows installer...'
        & $jpackage @installerArguments
        if ($LASTEXITCODE -ne 0) {
            throw "jpackage installer failed with exit code $LASTEXITCODE."
        }
        Write-Host "Installer: $(Join-Path $distribution "Alchemap-$version.exe")"
    }
} finally {
    $env:Path = $originalPath
}
