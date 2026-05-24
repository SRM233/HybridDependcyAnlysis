package Common;

public class OutputPath {
    public static final String OUTPUT_BASE_DIR = "output";

    // All relative paths are without prefix separator, handled uniformly in resolve
    public static final String JAVA_PARSE_RESULT_PATH             = "java-parse-output.json";
    public static final String JSP_PARSE_RESULT_PATH              = "jsp-parse-output.json";          // <- add .json
    public static final String JSF_PARSE_RESULT_PATH              = "jsf-parse-output.json";           // <- add .json
    public static final String WEB_XML_PARSE_RESULT_PATH          = "web-xml-parse-output.json";       // <- add .json
    public static final String POM_XML_PARSE_RESULT_PATH          = "pom-xml-parse-output.json";       // <- add .json
    public static final String PERSISTENCE_PARSE_RESULT_PATH  = "persistence-xml-parse-output.json"; // <- add .json
    public static final String EJB_JAR_PARSE_RESULT_PATH          = "ejb-jar-xml-parse-output.json";   // <- add .json
    public static final String FACES_CONFIG_PARSE_RESULT_PATH     = "faces-config-parse-output.json";  // <- add .json
    public static final String EAR_APPLICATION_PARSE_RESULT_PATH  = "application-xml-parse-output.json"; // <- add .json
    
    // Issue report
    public static final String ISSUES_PARSE_RESULT_PATH           = "issues-output.json";

    // Error logs (kept without .json, may be plaintext log or directory)
    public static final String JAVA_PARSE_ERROR_LOG_PATH          = "java-parse-errors.txt";
    public static final String JSP_PARSE_ERROR_LOG_PATH           = "jsp-parse-errors.txt";

    // If more error logs are needed in future, can standardize style, e.g.:
    public static final String JSF_PARSE_ERROR_LOG_PATH           = "jsf-parse-errors";
    public static final String WEB_XML_PARSE_ERROR_LOG_PATH       = "web-xml-parse-errors";




}
