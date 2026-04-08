param(
    [string]$EnvFile = "$PSScriptRoot/env/superkiller.env",
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Command
)

if (-not (Test-Path -LiteralPath $EnvFile)) {
    Write-Error "Environment file not found: $EnvFile"
    Write-Host "Copy scripts/env/superkiller.env.example to scripts/env/superkiller.env first."
    exit 1
}

$loaded = 0
Get-Content -LiteralPath $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
        return
    }
    $pair = $line -split '=', 2
    if ($pair.Count -ne 2) {
        return
    }
    $key = $pair[0].Trim()
    $value = $pair[1].Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    if (-not [string]::IsNullOrWhiteSpace($key)) {
        Set-Item -Path "Env:$key" -Value $value
        $loaded++
    }
}

Write-Host "Loaded $loaded environment variables from $EnvFile"

if ($Command.Count -gt 0) {
    & $Command[0] @($Command[1..($Command.Count - 1)])
    exit $LASTEXITCODE
}

Write-Host "Variables were loaded into the current PowerShell session."
