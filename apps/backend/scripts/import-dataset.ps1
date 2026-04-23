param(
    [Parameter(Mandatory = $true)]
    [string]$SourceDirectoryPath,

    [string]$BackendUrl = "http://localhost:8081",

    [string]$SourceName = "manual-import",

    [string]$DatasetType = "NORMALIZED_MEDICAL_DATA",

    [string]$Notes = "",

    [switch]$ReplaceExistingData
)

$body = @{
    sourceName = $SourceName
    datasetType = $DatasetType
    sourceDirectoryPath = $SourceDirectoryPath
    notes = $Notes
    replaceExistingData = $ReplaceExistingData.IsPresent
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "$BackendUrl/api/dataset-imports" `
    -ContentType "application/json" `
    -Body $body
