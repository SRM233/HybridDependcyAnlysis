# Hybrid Dependency Analysis Platform — Backend

## Overview

A Spring Boot 3.5.8 backend that analyzes legacy Java EE applications for cloud migration readiness. The system parses source code (Java, JSP, JSF, XML configuration files) using multiple parsing tools, applies detection rules to identify containerization obstacles, and generates structured JSON reports with actionable migration suggestions.

This is an academic Final Year Project (FYP).

## Architecture

```
Controllers (@RestController)
    ↓
Services (@Service)
    ↓
Mappers (@Mapper + XML)
    ↓
MySQL 8.0 + JSON Files
```

### Layers

| Layer | Package | Role |
|-------|---------|------|
| Controller | `com.hybriddependcyanlysis.Controller` | REST endpoints under `/user`, `/sourceFolder`, `/parse`, `/StaticAnalysis`, `/reports`, `/dynamic` |
| Service | `com.hybriddependcyanlysis.Service` + `Impl` | Business logic: parsing orchestration, detection rules, analysis modules |
| Core Engine | `Common.Util.ParsingUtil` | Multi-format source code parsing using Spoon, Jasper, Jsoup, DOM, Regex |
| Mapper | `com.hybriddependcyanlysis.Mapper` | MyBatis data access with XML SQL mapping under `resources/mapper/` |
| Data | MySQL | 17 tables for users, projects, parse metadata, and analysis paths |
| Files | Disk | Parse output and analysis reports stored as JSON under `{project}/output/` |

## Parsing Tools

| Tool | Target | Mode |
|------|--------|------|
| Spoon 10.3.0 | `.java` files | noClasspath AST for legacy code with missing dependencies |
| Jasper (Tomcat embedded) + JavaParser | `.jsp` files | Compile to servlet source → extract imports and EL calls |
| Jsoup 1.11.3 | `.xhtml` (JSF) | Fault-tolerant HTML parser for Facelets with malformed markup |
| DOM (JAXP) | XML configs | Strict XML parsing for `web.xml`, `persistence.xml`, `ejb-jar.xml`, `faces-config.xml`, `application.xml`, `pom.xml` |
| Regex (13 precompiled patterns) | `.jsp` files | Fallback when Jasper compilation fails; supplements content extraction |

## Detection & Analysis

**7 AST-level detection rules** applied during parsing (see `ParsingUtil`):
- Invocation: `System.exit()`, File I/O, JNDI `lookup()`
- Constructor: `HttpSession`, file I/O classes, `InitialContext`, raw Socket

**10 post-parse analysis modules** operating on structured JSON (see `StaticAnalysisServiceImpl`):
Annotation Count, Web XML, File Store, Persistence, EJB, POM, Faces Config, JSP File Count, JSF File Count, JSP Content, JSF Content

## API Endpoints (54 total)

| Prefix | POST | GET | DELETE | Auth |
|--------|------|-----|--------|------|
| `/user` | register, logout | login, getUser | delete | except register/login |
| `/sourceFolder` | upload | getFolders | 3 endpoints | yes |
| `/parse` | 11 (parse each type + parseAll) | - | 9 | yes |
| `/StaticAnalysis` | 10 analysis + content | - | 9 | yes |
| `/reports` | - | 7 (get reports) | - | yes |
| `/dynamic` | 2 | - | - | yes |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.5.8 |
| ORM | MyBatis 3.0.5 + PageHelper 2.1.1 |
| Database | MySQL 8.0+ |
| Authentication | JWT (io.jsonwebtoken 0.11.5) |
| JSON | Jackson 3.0.4 |
| Code Analysis | Spoon 10.3.0, JavaParser 3.25.10, PMD 7.23.0 |
| Build | Maven + Wrapper (`mvnw.cmd` / `mvnw`) |

## Quick Start

```bash
# 1. Create database
mysql -u root -p -e "CREATE DATABASE hybridanalysis CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. Configure database connection in src/main/resources/application.yml

# 3. Build
./mvnw.cmd clean package -DskipTests

# 4. Run
java -jar target/HybridDependcyAnlysis-0.0.1-SNAPSHOT.jar

# 5. Frontend repo: E:\FYP\FrontEnd\hybrid-analysis-frontend
```

## Testing

```bash
./mvnw.cmd test          # all tests
./mvnw.cmd -Dtest=SecurityTest test    # single class
```

38 JUnit 5 tests: 22 security (JWT, UserContext isolation, password storage) + 15 performance (regex throughput, JSON serialization, file I/O benchmarks) + 1 context load.

## Academic Disclaimer

This project is developed for academic purposes as a Final Year Project (FYP). It is not intended for production deployment. Known limitations include plain-text password storage, the absence of runtime dependency analysis, and a pattern-based rule engine that may produce false positives.
