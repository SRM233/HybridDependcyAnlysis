package Common;

import java.util.regex.Pattern;

public final class JspRegexConstants {

    private JspRegexConstants() {
        // Utility class, do not instantiate
    }

    // EL expression: ${...} or #{...} 
    public static final Pattern EL_PATTERN =
            Pattern.compile("(\\$|#)\\{([^}]+)}");

    // <%@ taglib prefix="x" uri="y" %> 
    public static final Pattern TAGLIB_PATTERN =
            Pattern.compile("<%@\\s*taglib\\s+prefix\\s*=\\s*\"([^\"]+)\"\\s+uri\\s*=\\s*\"([^\"]+)\"\\s*%>");

    // <%@ page import="a.b.C, d.e.F" %> 
    public static final Pattern PAGE_IMPORT_PATTERN =
            Pattern.compile("<%@\\s*page\\s+import\\s*=\\s*\"([^\"]+)\"\\s*%>");

    // <%@ include file="header.jsp" %> 
    public static final Pattern INCLUDE_DIRECTIVE_PATTERN =
            Pattern.compile("<%@\\s*include\\s+file\\s*=\\s*\"([^\"]+)\"\\s*%>");

    // <jsp:include page="header.jsp" /> 
    public static final Pattern INCLUDE_TAG_PATTERN =
            Pattern.compile("<jsp:include\\s+page\\s*=\\s*\"([^\"]+)\"\\s*/?>");

    // <jsp:useBean id="x" class="y" scope="z"> 
    public static final Pattern USE_BEAN_PATTERN =
            Pattern.compile("<jsp:useBean\\s+([^>]+)>");

    // Custom tag, e.g. <my:tag ...> 
    public static final Pattern CUSTOM_TAG_PATTERN =
            Pattern.compile("<([a-zA-Z0-9]+):([a-zA-Z0-9]+)([^>]*)>");

    // JSP scriptlet: <% ... %> (excluding <%@, <%!, <%=) 
    public static final Pattern SCRIPTLET_PATTERN =
            Pattern.compile("<%(?![@!=])((?:(?!%>).)*?)%>", Pattern.DOTALL);

    // JSP expression: <%= ... %> 
    public static final Pattern EXPRESSION_PATTERN =
            Pattern.compile("<%=((?:(?!%>).)*?)%>", Pattern.DOTALL);

    // JSP declaration: <%! ... %> 
    public static final Pattern DECLARATION_PATTERN =
            Pattern.compile("<%!((?:(?!%>).)*?)%>", Pattern.DOTALL);

    // JSP directive general match: <%@ directive ... %> 
    public static final Pattern DIRECTIVE_PATTERN =
            Pattern.compile("<%@\\s*([a-zA-Z]+)\\s+([^%]*)%>");

    // JSP comment: <%-- ... --%> 
    public static final Pattern COMMENT_PATTERN =
            Pattern.compile("<%--((?:(?!--%>).)*?)--%>", Pattern.DOTALL);

    // Server-specific reference detection 
    public static final Pattern SERVER_REF_PATTERN =
            Pattern.compile("\\b(org\\.apache\\.(jasper|tomcat|catalina)|weblogic|bea\\.|com\\.bea|websphere|com\\.ibm|jboss|org\\.jboss|glassfish|org\\.glassfish|jetty|org\\.eclipse\\.jetty|resin|caucho|tomee|openejb)\\b", 
                           Pattern.CASE_INSENSITIVE);

    // Standard tag library prefix set 
    public static final java.util.Set<String> STANDARD_TAG_PREFIXES = java.util.Set.of(
        // JSTL core tag library
        "jsp", "c", "fmt", "sql", "x", "fn",
        // Spring tag library
        "spring", "form", "bind",
        // JSF tag library
        "h", "f", "ui", "t",
        // Struts tag library
        "s", "sx",
        // Other common tag libraries
        "display", "util", "tiles", "sitemesh", "stripes"
    );
}

