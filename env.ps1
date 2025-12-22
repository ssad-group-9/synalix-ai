Get-Content .env1.local | ForEach-Object {
    if ($_ -match "^\s*#") { return } # 忽略注释行
    if ($_ -match "^\s*$") { return } # 忽略空行
    $line = $_ -split "="
    $key = $line[0].Trim()
    $value = $line[1].Trim()
    [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
}