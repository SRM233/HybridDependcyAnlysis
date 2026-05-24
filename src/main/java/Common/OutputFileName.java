package Common;

public class OutputFileName {
    public static final String JAVA_FILE_OUTPUT_FILE_NAME = "JavaFilesParseOutput";
    public static final String JAVA_FILE_ERROR_FILE_NAME = "JavaFilesParseError";
    public static final String JSP_PARSE_ERROR_FILE_NAME  = "jsp-parse-errors";

    public static final String JSP_OUTPUT_FILE_NAME          = "JspFilesParseOutput";
    public static final String XHTML_OUTPUT_FILE_NAME        = "XhtmlFilesParseOutput";     // Facelets / JSF XHTML
    public static final String HTML_OUTPUT_FILE_NAME         = "HtmlFilesParseOutput";      // If also parsing static HTML

    // Core deployment descriptors
    public static final String WEB_XML_OUTPUT_FILE_NAME      = "WebXmlParseOutput";
    public static final String POM_XML_OUTPUT_FILE_NAME      = "PomXmlParseOutput";
    public static final String APPLICATION_XML_OUTPUT_FILE_NAME = "ApplicationXmlParseOutput";  // EAR META-INF/application.xml

    // JSF / Jakarta Faces related
    public static final String FACES_CONFIG_OUTPUT_FILE_NAME = "FacesConfigParseOutput";

    // JPA / Persistence
    public static final String PERSISTENCE_OUTPUT_FILE_NAME  = "PersistenceXmlParseOutput";

    // EJB / other common
    public static final String EJB_JAR_XML_OUTPUT_FILE_NAME  = "EjbJarXmlParseOutput";
    public static final String JBOSS_WEB_OUTPUT_FILE_NAME    = "JbossWebParseOutput";       // If it is a legacy JBoss project
    public static final String JBOSS_APP_OUTPUT_FILE_NAME    = "JbossAppParseOutput";

    // Common Spring project (if your tool also supports)
    public static final String SPRING_MVC_SERVLET_OUTPUT_FILE_NAME = "SpringMvcServletXmlParseOutput";
    public static final String APPLICATION_CONTEXT_OUTPUT_FILE_NAME = "ApplicationContextParseOutput";

    // Optional: if output is directory grouped by type
    public static final String WEB_CONFIG_OUTPUT_DIR         = "WebConfigParsed";
    public static final String VIEW_FILES_OUTPUT_DIR         = "ViewFilesParsed";

}
