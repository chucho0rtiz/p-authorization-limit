$baseUrl = "http://localhost:8080"
$customerId = "CUS-002"

$transactionIdA = "TX-CONC-A-" + (Get-Date -Format "yyyyMMddHHmmss")
$transactionIdB = "TX-CONC-B-" + (Get-Date -Format "yyyyMMddHHmmss")
$amountA = 25000
$amountB = 26000

$jsonA = "{\`"transactionId\`":\`"$transactionIdA\`",\`"customerId\`":\`"$customerId\`",\`"amount\`":$amountA}"
$jsonB = "{\`"transactionId\`":\`"$transactionIdB\`",\`"customerId\`":\`"$customerId\`",\`"amount\`":$amountB}"

Write-Host "Disparando dos autorizaciones concurrentes sobre $customerId ..."
Write-Host "  A: $transactionIdA -> $amountA"
Write-Host "  B: $transactionIdB -> $amountB"
Write-Host ""

$jobA = Start-Job -ScriptBlock { param($url, $body) curl.exe -s -X POST "$url/authorizations" -H "Content-Type: application/json" -d $body } -ArgumentList $baseUrl, $jsonA
$jobB = Start-Job -ScriptBlock { param($url, $body) curl.exe -s -X POST "$url/authorizations" -H "Content-Type: application/json" -d $body } -ArgumentList $baseUrl, $jsonB

Wait-Job $jobA, $jobB | Out-Null

Write-Host "Resultado A:"
Receive-Job $jobA
Write-Host ""
Write-Host "Resultado B:"
Receive-Job $jobB
Write-Host ""

Remove-Job $jobA, $jobB

Write-Host "Saldo final de $customerId :"
curl.exe -s "$baseUrl/customers/$customerId/limit"
Write-Host ""
