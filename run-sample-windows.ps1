
$JAVA_VERSION = "21"
$jdkUrl = "https://download.bell-sw.com/java/21.0.4+9/bellsoft-jdk21.0.4+9-windows-amd64-lite.zip"

$jdkZipPath = "$PSScriptRoot\jdk.zip"
$downloadsPath = "$PSScriptRoot\downloads"
$jdkFolderName = "jdk-$JAVA_VERSION"
$jdkFolderPath = "$downloadsPath\$jdkFolderName"

function Get-JavaVersion {
    try {
        $javaVersionOutput = & java -version 2>&1
        if ([string]$javaVersionOutput -match 'version "(\d+)\.(\d+)\.(\d+)') {
            return [version]::new("$($matches[1]).$($matches[2])")
        }
    } catch {
        return $null
    }
}

function Is-JavaVersionInstalled {
    $javaVersion = Get-JavaVersion
    return $javaVersion -ne $null -and $javaVersion -ge [version]::new("$JAVA_VERSION.0")
}
function Is-JdkDownloadedAndUnzipped {
    return Test-Path $jdkFolderPath
}

if (Is-JavaVersionInstalled -or Test-Path $jdkFolderPath) {
    Write-Host "JDK $JAVA_VERSION or higher is already installed"
} elseif (Is-JdkDownloadedAndUnzipped) {
    Write-Host "JDK $JAVA_VERSION is already downloaded"

    $jdkBinPath = "$jdkFolderPath\bin"
    $env:PATH = "$jdkBinPath;$env:PATH"
} else {
    Write-Host "Downloading JDK..."
    Start-BitsTransfer -Source $jdkUrl -Destination $jdkZipPath -DisplayName "Downloading JDK"

    Write-Host "Unzipping JDK..."
    Expand-Archive -Path $jdkZipPath -DestinationPath $downloadsPath -Force

    $pattern = "^jdk-$JAVA_VERSION.*"
    $folder = Get-ChildItem -Path $downloadsPath -Directory | Where-Object { $_.Name -match $pattern }
    if ($folder) {
        $newFolderPath = Join-Path -Path $downloadsPath -ChildPath $jdkFolderName
        Rename-Item -Path $folder.FullName -NewName $newFolderPath -Force
    }
    Remove-Item $jdkZipPath -Force

    $jdkBinPath = "$jdkFolderPath\bin"
    $env:PATH = "$jdkBinPath;$env:PATH"
}

Write-Host "Java Version:"
& java -version

Write-Host "Setup completed."

$env:GRADLE_USER_HOME = ".gradle"

# Run Gradle command
Write-Host "Running Gradle command..."
$sample=$args[0]
& ".\gradlew.bat" --quiet --no-daemon :samples:runSample -Psample="$sample"