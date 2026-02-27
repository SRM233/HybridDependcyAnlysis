package Common;

public class OutputFileName {
    public static final String JAVA_FILE_OUTPUT_FILE_NAME = "JavaFilesParseOutput";
    public static final String JAVA_FILE_ERROR_FILE_NAME = "JavaFilesParseError";
    public static final String JSP_PARSE_ERROR_FILE_NAME  = "jsp-parse-errors";

    public static final String JSP_OUTPUT_FILE_NAME          = "JspFilesParseOutput";
    public static final String XHTML_OUTPUT_FILE_NAME        = "XhtmlFilesParseOutput";     // Facelets / JSF XHTML
    public static final String HTML_OUTPUT_FILE_NAME         = "HtmlFilesParseOutput";      // 如果也解析静态HTML

    // 核心部署描述符
    public static final String WEB_XML_OUTPUT_FILE_NAME      = "WebXmlParseOutput";
    public static final String APPLICATION_XML_OUTPUT_FILE_NAME = "ApplicationXmlParseOutput";  // EAR 的 META-INF/application.xml

    // JSF / Jakarta Faces 相关
    public static final String FACES_CONFIG_OUTPUT_FILE_NAME = "FacesConfigParseOutput";

    // JPA / 持久化
    public static final String PERSISTENCE_OUTPUT_FILE_NAME  = "PersistenceXmlParseOutput";

    // EJB / 其他常见
    public static final String EJB_JAR_XML_OUTPUT_FILE_NAME  = "EjbJarXmlParseOutput";
    public static final String JBOSS_WEB_OUTPUT_FILE_NAME    = "JbossWebParseOutput";       // 如果是老JBoss项目
    public static final String JBOSS_APP_OUTPUT_FILE_NAME    = "JbossAppParseOutput";

    // Spring 项目常见（如果你的工具也支持）
    public static final String SPRING_MVC_SERVLET_OUTPUT_FILE_NAME = "SpringMvcServletXmlParseOutput";
    public static final String APPLICATION_CONTEXT_OUTPUT_FILE_NAME = "ApplicationContextParseOutput";

    // 可选：如果输出是按类型分组的目录名
    public static final String WEB_CONFIG_OUTPUT_DIR         = "WebConfigParsed";
    public static final String VIEW_FILES_OUTPUT_DIR         = "ViewFilesParsed";

}
