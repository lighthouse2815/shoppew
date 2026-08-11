# Performance baseline

This document records the Phase 17 local performance baseline and the code/configuration paths that affect it. It is deliberately a smoke baseline, not a production service-level objective (SLO), capacity result, or sizing recommendation.

## Scope and test environment

The measurements below were captured on 2026-08-11 from a Windows development host against the Docker Compose backend at `http://localhost:28080`. Spring Boot, PostgreSQL 17, and Redis 8 were running locally through Docker Desktop. Requests crossed the host-to-container port mapping; they did not cross a production reverse proxy, TLS terminator, CDN, or geographic network.

The database snapshot was intentionally small development/smoke data:

| Relation | Rows relevant to the snapshot |
| --- | ---: |
| Products | 23 |
| Active products | 12 |
| Variants | 23 |
| Shops | 15 |

The runs used warm-up requests before timing, so the JVM, connection pool, and PostgreSQL buffers could already be warm. Docker resource limits, host CPU and memory specifications, background host load, production data distribution, and concurrent write traffic were not normalized. Results are therefore useful for detecting a large regression on the same local setup, but must not be extrapolated to production traffic or catalog size.

## Recorded results

| Scenario | Request | Warm-ups | Measured load | HTTP result | Wall time | Throughput | p50 | p95 | p99 | Max |
| --- | --- | ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Public catalog | `GET /api/v1/public/products?page=0&size=20` | 10 | 100 requests, concurrency 10 | 100 x `200`; 0 failures | 1,036.4 ms | 96.5 req/s | 79.5 ms | 196.3 ms | 233.2 ms | 268.2 ms |
| Unicode product search | `GET /api/v1/public/products?q=%C3%A1o&page=0&size=20` | 5 | 100 requests, concurrency 10 | 100 x `200`; 0 failures | 347.8 ms | 287.5 req/s | 28.9 ms | 74.8 ms | 86.0 ms | 105.9 ms |

For the catalog run, measured requests appended a harmless unique `run=0` through `run=99` query parameter. Spring ignores that unknown parameter; changing the URL only prevents an accidental intermediary URL-cache hit. The search term was the UTF-8 string `áo`, verified as Unicode code points `[225, 111]`, and its measured requests reused the same encoded URL.

The two rows are not an A/B comparison of query strategies. They used different warm-up counts and result selectivity over a very small dataset. In particular, the faster search row does not by itself prove the gain attributable to any one index.

The `áo` query returned zero matching products in this snapshot. A separate cold, read-only `EXPLAIN ANALYZE` of the exact search selected a sequential scan, which is a reasonable planner choice for only 23 product rows; it reported 1,030.6 ms planning time and 113.7 ms execution time with zero result rows. That one-off plan observation is neither included in the concurrent HTTP table nor a capacity benchmark. It also makes explicit that having the V10/V11 indexes present is not evidence that PostgreSQL used them for this tiny-data request.

## Rate-limit verification

Public product-list and search-suggestion reads share the `public-search` one-minute rate-limit namespace. The local default is `APP_RATE_LIMIT_SEARCH_PER_MINUTE=180`, keyed by the request remote address and coordinated through Redis.

One separate, intentional cumulative run confirmed the boundary: requests through number 180 in the active window were allowed, and subsequent requests returned `429 RATE_LIMITED`. This was expected protection behavior, not a failed benchmark. The response includes `Retry-After: 60`; when Redis is unavailable, the backend uses a bounded per-instance fallback instead of silently disabling the limit.

Run each benchmark row in a fresh rate-limit window. The 10 + 100 catalog requests and 5 + 100 search requests are individually below 180, but running them back-to-back in the same window can correctly produce `429` responses. A unique `run` query value does not create a new rate-limit identity.

## Implementation audit

The following findings are from a read-only review of the current source and configuration. They describe implementation posture; only the HTTP numbers above are runtime latency evidence.

| Layer | Current behavior | Performance consequence or follow-up |
| --- | --- | --- |
| PostgreSQL full-text search | Flyway V10 creates `idx_products_search`, a GIN index over the `simple` text vector for product name and descriptions. | Supports the full-text branch of keyword search without introducing a second search authority. The tiny-data plan observation above did not use this index. |
| Partial-name search | Flyway V11 enables `pg_trgm` and creates the GIN index `idx_products_name_trgm` on product name. | Supports the `ILIKE '%query%'` fallback used by product search and suggestions. A managed production database must allow or pre-install `pg_trgm`; the tiny-data plan observation above did not use this index. |
| Active variant price | V11 creates partial index `idx_product_variants_active_price` on `(product_id, price) WHERE status = 'ACTIVE'`. | Matches the active-variant minimum-price lookup used in the public search query. |
| Migration state | The rebuilt local runtime reported latest Flyway state `11|true`, and both V11 index names were present. | Confirms the measured runtime had the performance migration applied; it is not merely checked-in SQL. |
| Query bounds | Product search clamps page size to 1-100; suggestions clamp results to 1-10. | Prevents an unbounded response, but does not replace high-cardinality query testing. |
| Query shape | Each product search performs an ID page query, a `count(*)` query, and a detailed product fetch in requested order. | Avoids loading an unbounded entity graph. The count query and offset pagination remain scale-test candidates on production-like cardinality. |
| Data access | Hikari defaults to maximum pool size 20 and minimum idle 2; Hibernate batch fetch size is 50 and Open Session in View is disabled. | These are explicit local defaults, not production pool tuning. Pool size must be reconciled with replica count and the database connection budget. |
| Application cache | The PostgreSQL search path has no application-level result cache. Redis coordinates abuse limits but is not the catalog source of truth. | Warm-ups can still warm JVM and database caches. The recorded success is not evidence of Redis-served catalog responses. |
| Web delivery | Storefront catalog media uses Next Image with responsive `sizes`; Seller and Admin routes use `React.lazy`/`Suspense`, and non-critical native images use lazy/async loading where present. | Static code review confirms splitting and image-loading primitives, but no Lighthouse, Core Web Vitals, or production CDN measurement was recorded in this baseline. |

Compose does not impose a benchmark CPU/memory budget, and this run did not collect query plans, database CPU/I/O, garbage-collection pauses, connection-pool saturation, or write-path latency. Before defining a production SLO or capacity target, repeat representative read and checkout mixes with production-like cardinality, controlled resources, TLS/proxy behavior, sustained duration, and server-side telemetry.

## Reproduce the local HTTP baseline

Start from a healthy local stack and ensure there is active catalog data. The catalog smoke creates isolated synthetic data; it is write-capable and should only be run against the local development environment.

```powershell
docker compose up -d --build
Invoke-RestMethod http://localhost:28080/actuator/health/readiness

# Optional when the local database has no suitable active catalog data.
.\scripts\smoke-catalog.ps1
```

The following harness is compatible with Windows PowerShell 5.1. It uses one persistent `HttpClient`, a shared work queue, and exactly the requested number of workers. Per-request time begins when a worker dequeues its request; wall time covers the measured concurrent run. Paste the whole block into PowerShell from the repository root:

```powershell
Add-Type -AssemblyName System.Net.Http
Add-Type -ReferencedAssemblies System.Net.Http -TypeDefinition @'
using System;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.Globalization;
using System.Net.Http;
using System.Threading.Tasks;

public sealed class ShoppewHttpSample
{
    public int StatusCode { get; set; }
    public double Milliseconds { get; set; }
    public string Error { get; set; }
}

public sealed class ShoppewHttpRun
{
    public ShoppewHttpSample[] Samples { get; set; }
    public double WallMilliseconds { get; set; }
}

public static class ShoppewHttpBenchmark
{
    public static ShoppewHttpRun Run(
        string uri,
        int warmups,
        int requestCount,
        int concurrency,
        bool appendRunParameter)
    {
        return RunAsync(uri, warmups, requestCount, concurrency, appendRunParameter)
            .GetAwaiter().GetResult();
    }

    private static async Task<ShoppewHttpRun> RunAsync(
        string uri,
        int warmups,
        int requestCount,
        int concurrency,
        bool appendRunParameter)
    {
        var handler = new HttpClientHandler();
        handler.MaxConnectionsPerServer = concurrency;
        using (handler)
        using (var client = new HttpClient(handler))
        {
            client.Timeout = TimeSpan.FromSeconds(30);
            for (var i = 0; i < warmups; i++)
            {
                using (var response = await client.GetAsync(uri).ConfigureAwait(false))
                {
                    if ((int)response.StatusCode != 200)
                        throw new InvalidOperationException("Warm-up returned HTTP " + (int)response.StatusCode);
                }
            }

            var work = new ConcurrentQueue<int>();
            for (var i = 0; i < requestCount; i++) work.Enqueue(i);
            var samples = new ConcurrentBag<ShoppewHttpSample>();
            var workers = new Task[concurrency];
            var wall = Stopwatch.StartNew();
            for (var i = 0; i < concurrency; i++)
                workers[i] = Worker(client, uri, appendRunParameter, work, samples);
            await Task.WhenAll(workers).ConfigureAwait(false);
            wall.Stop();

            return new ShoppewHttpRun {
                Samples = samples.ToArray(),
                WallMilliseconds = wall.Elapsed.TotalMilliseconds
            };
        }
    }

    private static async Task Worker(
        HttpClient client,
        string uri,
        bool appendRunParameter,
        ConcurrentQueue<int> work,
        ConcurrentBag<ShoppewHttpSample> samples)
    {
        int index;
        while (work.TryDequeue(out index))
        {
            var target = appendRunParameter
                ? uri + "&run=" + index.ToString(CultureInfo.InvariantCulture)
                : uri;
            var timer = Stopwatch.StartNew();
            try
            {
                using (var response = await client.GetAsync(target).ConfigureAwait(false))
                {
                    timer.Stop();
                    samples.Add(new ShoppewHttpSample {
                        StatusCode = (int)response.StatusCode,
                        Milliseconds = timer.Elapsed.TotalMilliseconds
                    });
                }
            }
            catch (Exception exception)
            {
                timer.Stop();
                samples.Add(new ShoppewHttpSample {
                    StatusCode = 0,
                    Milliseconds = timer.Elapsed.TotalMilliseconds,
                    Error = exception.GetType().Name
                });
            }
        }
    }
}
'@

function Format-ShoppewHttpRun {
    param([ShoppewHttpRun]$Run)

    $latency = @($Run.Samples.Milliseconds | Sort-Object)
    $percentile = {
        param([double]$Fraction)
        $index = [Math]::Max(0, [Math]::Ceiling($latency.Count * $Fraction) - 1)
        [Math]::Round($latency[$index], 1)
    }
    $statuses = ($Run.Samples | Group-Object StatusCode | Sort-Object Name |
        ForEach-Object { "{0}x{1}" -f $_.Count, $_.Name }) -join ', '

    [pscustomobject]@{
        Requests       = $Run.Samples.Count
        Statuses       = $statuses
        Failures       = @($Run.Samples | Where-Object StatusCode -ne 200).Count
        WallMs         = [Math]::Round($Run.WallMilliseconds, 1)
        RequestsPerSec = [Math]::Round($Run.Samples.Count * 1000 / $Run.WallMilliseconds, 1)
        P50Ms          = & $percentile 0.50
        P95Ms          = & $percentile 0.95
        P99Ms          = & $percentile 0.99
        MaxMs          = [Math]::Round($latency[-1], 1)
    }
}

$catalog = [ShoppewHttpBenchmark]::Run(
    'http://localhost:28080/api/v1/public/products?page=0&size=20',
    10, 100, 10, $true)
Format-ShoppewHttpRun $catalog

# Start the next row only after the one-minute public-search window has expired.
Start-Sleep -Seconds 61

[int[]][char[]]'áo' # Must print 225 and 111.
$search = [ShoppewHttpBenchmark]::Run(
    'http://localhost:28080/api/v1/public/products?q=%C3%A1o&page=0&size=20',
    5, 100, 10, $false)
Format-ShoppewHttpRun $search
```

Latency varies between runs. Preserve the URI, data snapshot, warm-up count, request count, concurrency, rate-limit policy, Docker resources, and host conditions when using this as a regression comparison. Do not tune or disable the production guardrails merely to reproduce a local throughput number.

To verify the migration and index state on the default local Compose database:

```powershell
docker compose exec -T postgres psql -U shoppew -d shoppew -Atc `
  "SELECT version || '|' || success::text FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1;"

docker compose exec -T postgres psql -U shoppew -d shoppew -Atc `
  "SELECT indexname FROM pg_indexes WHERE indexname IN ('idx_products_name_trgm', 'idx_product_variants_active_price') ORDER BY indexname;"
```

The expected migration line for this source revision is `11|true`. If `.env` overrides the default PostgreSQL user or database, use those effective values in the verification command.
