package Common;

import java.util.regex.Pattern;

public final class JspRegexConstants {

    private JspRegexConstants() {
        // 工具类，不允许实例化
    }

    /** EL 表达式：${...} 或 #{...} */
    public static final Pattern EL_PATTERN =
            Pattern.compile("(\\$|#)\\{([^}]+)}");

    /** <%@ taglib prefix="x" uri="y" %> */
    public static final Pattern TAGLIB_PATTERN =
            Pattern.compile("<%@\\s*taglib\\s+prefix\\s*=\\s*\"([^\"]+)\"\\s+uri\\s*=\\s*\"([^\"]+)\"\\s*%>");

    /** <%@ page import="a.b.C, d.e.F" %> */
    public static final Pattern PAGE_IMPORT_PATTERN =
            Pattern.compile("<%@\\s*page\\s+import\\s*=\\s*\"([^\"]+)\"\\s*%>");

    /** <%@ include file="header.jsp" %> */
    public static final Pattern INCLUDE_DIRECTIVE_PATTERN =
            Pattern.compile("<%@\\s*include\\s+file\\s*=\\s*\"([^\"]+)\"\\s*%>");

    /** <jsp:include page="header.jsp" /> */
    public static final Pattern INCLUDE_TAG_PATTERN =
            Pattern.compile("<jsp:include\\s+page\\s*=\\s*\"([^\"]+)\"\\s*/?>");

    /** <jsp:useBean id="x" class="y" scope="z"> */
    public static final Pattern USE_BEAN_PATTERN =
            Pattern.compile("<jsp:useBean\\s+([^>]+)>");

    /** 自定义标签，如 <my:tag ...> */
    public static final Pattern CUSTOM_TAG_PATTERN =
            Pattern.compile("<([a-zA-Z0-9]+):([a-zA-Z0-9]+)([^>]*)>");

    /** JSP脚本片段：<% ... %> (排除<%@, <%!, <%=) */
    public static final Pattern SCRIPTLET_PATTERN =
            Pattern.compile("<%(?![@!=])((?:(?!%>).)*?)%>", Pattern.DOTALL);

    /** JSP表达式：<%= ... %> */
    public static final Pattern EXPRESSION_PATTERN =
            Pattern.compile("<%=((?:(?!%>).)*?)%>", Pattern.DOTALL);

    /** JSP声明：<%! ... %> */
    public static final Pattern DECLARATION_PATTERN =
            Pattern.compile("<%!((?:(?!%>).)*?)%>", Pattern.DOTALL);

    /** JSP指令通用匹配：<%@ directive ... %> */
    public static final Pattern DIRECTIVE_PATTERN =
            Pattern.compile("<%@\\s*([a-zA-Z]+)\\s+([^%]*)%>");

    /** JSP注释：<%-- ... --%> */
    public static final Pattern COMMENT_PATTERN =
            Pattern.compile("<%--((?:(?!--%>).)*?)--%>", Pattern.DOTALL);

    /** 服务器特定引用检测 */
    public static final Pattern SERVER_REF_PATTERN =
            Pattern.compile("\\b(org\\.apache\\.(jasper|tomcat|catalina)|weblogic|bea\\.|com\\.bea|websphere|com\\.ibm|jboss|org\\.jboss|glassfish|org\\.glassfish|jetty|org\\.eclipse\\.jetty|resin|caucho|tomee|openejb)\\b", 
                           Pattern.CASE_INSENSITIVE);

    /** 标准标签库前缀集合 */
    public static final java.util.Set<String> STANDARD_TAG_PREFIXES = java.util.Set.of(
        // JSTL核心标签库
        "jsp", "c", "fmt", "sql", "x", "fn",
        // Spring标签库
        "spring", "form", "bind",
        // JSF标签库
        "h", "f", "ui", "t",
        // Struts标签库
        "s", "sx",
        // 其他常见标签库
        "display", "util", "tiles", "sitemesh", "stripes"
    );
}

