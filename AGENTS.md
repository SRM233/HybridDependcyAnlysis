# AGENTS.md — HybridDependenciesAnalysis

## Build & Test

```bash
./mvnw.cmd clean install          # full build + test
./mvnw.cmd test                   # all tests
./mvnw.cmd -Dtest=SomeTest test   # single test class
./mvnw clean install -DskipTests  # skip tests
```

No linter/formatter configured. No typecheck step. Maven wrapper (`mvnw.cmd` Windows, `mvnw` Linux/Mac).

## Architecture

**Spring Boot 3.5.8 + MyBatis 3.0.5 + MySQL 8.0** — Controller → Service → Mapper → MySQL, with analysis reports stored as JSON files on disk (DB holds only metadata + path).

### Package caveats

- Base package has a typo: `com.hybriddependcyanlysis` (not "analysis")
- Shared utilities live outside base package in `Common.*` (e.g. `Common.Result`, `Common.OutputPath`, `Common.UserContextHolder`)
- Controller `@RequestMapping` prefixes: `/user`, `/sourceFolder`, `/parse`, `/StaticAnalysis`, `/dynamic`, `/reports`

### Two report-related mappers (distinct)

| Mapper | Role | Key methods |
|--------|------|-------------|
| `AnalysisReportMapper` | `/reports` queries | `getXxxReport(AnalysisResultDTO)` — 7 methods, annotated with `@Select` |
| `StaticAnalysisMapper` | analysis inserts/updates | `getXxxReport()`, `insertResult()`, `updateResult()`, `deleteXxx()` |

Both read/write `AnalysisReportDAO`. The `AnalysisResultDTO` query DTO was **not** renamed. This naming asymmetry is intentional.

### Data flow (order matters)

```
Upload ZIP → Parse (parallel: Java/JSP/JSF/XML) → Static Analysis (7 types) → View Reports
```

Parse output is written as JSON files + DB metadata. Static analysis reads those JSON files, produces report JSON files, and saves the path in `analysis_result` table. Report endpoints query the DB for the path then read + deserialize the JSON file.

### Jackson 3 with Jackson 2 imports

pom.xml declares `tools.jackson.core:jackson-core:3.0.4` (Jackson 3 groupId) but source code imports use `com.fasterxml.jackson.*` (Jackson 2 package path). Spring Boot's managed version resolves this. Import as `com.fasterxml.jackson.databind.ObjectMapper`.

### REST conventions

- `@Autowired` field injection (not constructor)
- `@Slf4j` on Controllers and Services
- `@Transactional` on service methods that write to DB
- `@Data` + `@AllArgsConstructor` + `@NoArgsConstructor` on all DAOs/DTOs
- API responses wrapped in `Common.Result<T>`: `Result.success(data)` / `Result.fail(msg)`
- JWT auth via `JwtInterceptor` → `UserContextHolder.getUserId()` — check for null

### Method naming inconsistency

Some `StaticAnalysisService` methods start with capital letter (`AnnotationCount`, `JspFileCount`, `JsfFileCount`, `FileStoreAnalysis`) while others are camelCase (`analyzeWebXml`, `persistenceAnalysis`, `ejbJarAnalysis`, `pomXmlAnalysis`, `facesXmlAnalysis`). Match existing pattern in the file you edit.

### DB schema

- Database: `hybridanalysis` at `localhost:3306`, user/pass `root/root`
- mybatis `map-underscore-to-camel-case: true` — `source_folder_id` → `sourceFolderId`
- No schema prefix in SQL queries
- `analysis_result` table stores report file paths, actual data is in the JSON files

### File output

All parse/analysis JSON files written under `{projectRoot}/output/`. Path constants in `Common.OutputPath`. Use `jsonFileService.generateJsonArray(list, absolutePath)` for parse output, `ObjectMapper.writeValue()` for analysis reports.

### Key constraint

Static analysis depends on parse output — must run `POST /parse/parseAll` (or individual parse endpoints) before running any `POST /StaticAnalysis/*` endpoint.
