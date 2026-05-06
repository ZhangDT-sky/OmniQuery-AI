param(
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "root",
    [string]$MysqlHost = "localhost",
    [int]$MysqlPort = 3306,
    [string]$Database = "omniquery_demo",
    [switch]$SkipInit,
    [switch]$Build
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "omniquery-backend"
$sqlFile = Join-Path $PSScriptRoot "mysql-demo.sql"

if (-not $SkipInit) {
    $env:MYSQL_PWD = $MysqlPassword
    Get-Content -LiteralPath $sqlFile -Raw | mysql -h $MysqlHost -P $MysqlPort -u $MysqlUser
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
}

if ($Build) {
    $pom = Join-Path $backendDir "pom.xml"
    mvn -f $pom test package
}

$env:OMNIQUERY_MYSQL_URL = "jdbc:mysql://${MysqlHost}:${MysqlPort}/${Database}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
$env:OMNIQUERY_MYSQL_USERNAME = $MysqlUser
$env:OMNIQUERY_MYSQL_PASSWORD = $MysqlPassword

if (-not $env:MODEL_API_KEY) {
    Write-Warning "MODEL_API_KEY is not set. The app can still run without the llm profile, but this demo starts mysql,llm,vector-rag."
}

if (-not $env:EMBEDDING_API_KEY) {
    Write-Warning "EMBEDDING_API_KEY is not set. vector-rag indexing will fall back to keyword retrieval if embedding fails."
}

$jar = Join-Path $backendDir "omniquery-api\target\omniquery-api-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jar)) {
    throw "Backend jar not found: $jar. Run this script with -Build first."
}

java -jar $jar --spring.profiles.active=mysql,llm,vector-rag
