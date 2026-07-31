param([Parameter(Mandatory = $true)][string]$Key)
$headers = @{ Authorization = "Bearer $Key"; Accept = "application/json" }
try {
    $r = Invoke-RestMethod -Uri "https://api.deepseek.com/user/balance" -Headers $headers -TimeoutSec 15
    $r | ConvertTo-Json -Depth 5
} catch {
    Write-Error "请求失败: $($_.Exception.Message)"
    exit 1
}
