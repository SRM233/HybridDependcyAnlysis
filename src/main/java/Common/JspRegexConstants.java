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
}

