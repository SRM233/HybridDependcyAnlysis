# Hybrid Dependencies Analysis

Java/Jakarta EE 传统企业级项目云迁移依赖分析工具。解析源代码（Java/JSP/JSF/XML）后执行静态分析，生成可查询的迁移评估报告。

## 架构

```
Controller → Service → Mapper(MyBatis) → MySQL
                          ↕
                    ParsingUtil (Spoon/Jsoup/Jasper)
                          ↕
                  JSON 文件（解析输出 + 分析报告）
```

分层架构：Controller 处理 REST 请求 → Service 执行业务逻辑 → Mapper 访问数据库。解析和分析结果以 JSON 文件存储于 `output/`，数据库仅保存元数据和文件路径。

## 流程

```
注册/登录 → 上传 ZIP → 全量解析 → 静态分析 → 查看报告
```

1. **用户认证** — JWT 无状态认证，`/user/register` + `/user/login` 获取 Token
2. **上传项目** — `POST /sourceFolder/upload` 上传 ZIP，自动解压
3. **源代码解析** — `POST /parse/parseAll` 并行解析 Java/JSP/JSF/XML，输出 JSON + 入库
4. **静态分析** — `POST /StaticAnalysis/annotationAnalysis` 等 7 类分析，读取解析 JSON 生成报告 JSON + 入库
5. **查看报告** — `GET /reports/AnnotationReport` 等 7 个端点查询报告

## API 端点

| 模块 | 前缀 | 端点 |
|------|------|------|
| 用户 | `/user` | register(POST), login(GET), getUser(GET), logout(POST), delete(DELETE) |
| 项目管理 | `/sourceFolder` | upload(POST), getFolders(GET), delete×3(DELETE) |
| 源码解析 | `/parse` | parseJavaFiles / parseJspFiles / parseJsfFile / parseWebXml / parsePersistenceXm / parseEjbJarXml / parseFacesConfigXml / parseApplicationXml / parsePomXmlFile / staticParseFile / parseAll (POST) + 9 个 DELETE |
| 静态分析 | `/StaticAnalysis` | annotationAnalysis / JspFilesCount / webXMlFileAnalysis / fileStoreAnalysis / persistenceAnalysis / ejbJarAnalysis / pomXmlAnalysis / facesXmlAnalysis (POST) + 7 个 DELETE |
| 动态分析 | `/dynamic` | dynamicAnalysis(POST), javaAgent(POST) |
| 分析报告 | `/reports` | AnnotationReport / WebXmlReport / FileStoreReport / PersistenceReport / EjbJarReport / PomXmlReport / FacesConfigReport (GET) |

## 技术栈

| 层面 | 技术 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.5.8 |
| ORM | MyBatis 3.0.5 + PageHelper 2.1.1 |
| 数据库 | MySQL 8.0+ |
| Java 解析 | Spoon 10.3.0, JavaParser 3.25.10 |
| HTML/XML | Jsoup 1.11.3 |
| JSP 编译 | Apache Jasper (embedded Tomcat) |
| 代码质量 | PMD 7.23.0 (core + jsp) |
| JSON | Jackson 3.0.4 |
| 认证 | JWT (jjwt 0.11.5) |
| 构建 | Maven + Wrapper (mvnw.cmd / mvnw) |

## 输出文件

所有文件生成于 `{projectRoot}/output/`：

| 文件 | 说明 |
|------|------|
| java-parse-output.json | Java 解析结果 |
| jsp-parse-output.json | JSP 解析结果 |
| jsf-parse-output.json | JSF 解析结果 |
| web-xml-parse-output.json | web.xml 解析结果 |
| persistence-xml-parse-output.json | persistence.xml 解析结果 |
| ejb-jar-xml-parse-output.json | ejb-jar.xml 解析结果 |
| faces-config-parse-output.json | faces-config.xml 解析结果 |
| application-xml-parse-output.json | application.xml 解析结果 |
| pom-xml-parse-output.json | pom.xml 解析结果 |
| issues-output.json | 综合问题报告 |
| java-parse-errors.txt | Java 解析错误日志 |
| jsp-parse-errors.txt | JSP 解析错误日志 |

## 快速开始

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE hybridanalysis CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 构建
./mvnw.cmd clean package -DskipTests

# 3. 运行（需配置 application.yml 中的数据库连接）
java -jar target/HybridDependcyAnlysis-0.0.1-SNAPSHOT.jar

# 4. 使用
curl -X POST http://localhost:8080/user/register -H 'Content-Type: application/json' -d '{"username":"test","password":"test"}'
TOKEN=$(curl -s -X GET 'http://localhost:8080/user/login?username=test&password=test' | jq -r '.data')
curl -X POST http://localhost:8080/sourceFolder/upload -H "Authorization: Bearer $TOKEN" -F 'file=@project.zip'
curl -X POST 'http://localhost:8080/parse/parseAll?sourceFolderId=1' -H "Authorization: Bearer $TOKEN"
curl -X GET http://localhost:8080/reports/AnnotationReport -H "Authorization: Bearer $TOKEN"
```

## 配置

编辑 `src/main/resources/application.yml`：

- **数据库**: `spring.datasource.url/jdbc`，默认 `root/root@localhost:3306/hybridanalysis`
- **JWT**: `config.jwt.expire`（默认 3600s），`config.jwt.secret`（HS512 密钥）
- **文件上传**: `spring.servlet.multipart.max-file-size`（默认 1024MB）

## 数据库表

`users` / `source_folders` / `files` / `java_files_parse_output` / `java_files_error` / `jsp_parse_output` / `jsp_parse_error` / `jsf_parse_output` / `jsf_parse_error` / `web_xml_parse_output` / `pom_xml_parse_output` / `persistence_xml_parse_output` / `ejb_jar_xml_parse_output` / `faces_config_xml_parse_output` / `application_xml_parse_output` / `analysis_result` / `reports`

MyBatis 配置 `map-underscore-to-camel-case: true`，无需 schema 前缀。
