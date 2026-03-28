package Common;

public class OutputPath {
    public static final String OUTPUT_BASE_DIR = "output";

    // 所有相对路径都不带前缀分隔符，统一在 resolve 时处理
    public static final String JAVA_PARSE_RESULT_PATH             = "java-parse-output.json";
    public static final String JSP_PARSE_RESULT_PATH              = "jsp-parse-output.json";          // ← 加 .json
    public static final String JSF_PARSE_RESULT_PATH              = "jsf-parse-output.json";           // ← 加 .json
    public static final String WEB_XML_PARSE_RESULT_PATH          = "web-xml-parse-output.json";       // ← 加 .json
    public static final String POM_XML_PARSE_RESULT_PATH          = "pom-xml-parse-output.json";       // ← 加 .json
    public static final String PERSISTENCE_PARSE_RESULT_PATH  = "persistence-xml-parse-output.json"; // ← 加 .json
    public static final String EJB_JAR_PARSE_RESULT_PATH          = "ejb-jar-xml-parse-output.json";   // ← 加 .json
    public static final String FACES_CONFIG_PARSE_RESULT_PATH     = "faces-config-parse-output.json";  // ← 加 .json
    public static final String EAR_APPLICATION_PARSE_RESULT_PATH  = "application-xml-parse-output.json"; // ← 加 .json
    
    // Issue 报告
    public static final String ISSUES_PARSE_RESULT_PATH           = "issues-output.json";

    // 错误日志（保持不带 .json，可能是纯文本日志或目录）
    public static final String JAVA_PARSE_ERROR_LOG_PATH          = "java-parse-errors.txt";
    public static final String JSP_PARSE_ERROR_LOG_PATH           = "jsp-parse-errors.txt";

    // 如果以后有更多错误日志，也可以统一风格，例如：
    public static final String JSF_PARSE_ERROR_LOG_PATH           = "jsf-parse-errors";
    public static final String WEB_XML_PARSE_ERROR_LOG_PATH       = "web-xml-parse-errors";




}
