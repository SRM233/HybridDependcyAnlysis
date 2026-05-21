package Common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

public class JspContentAnalyzer {

    public static JspFileInfo analyzeJspContent(File jspFile) {
        JspFileInfo result = new JspFileInfo();
        result.filePath = jspFile.getAbsolutePath();

        try {
            String content = Files.readString(jspFile.toPath());
            String contentWithoutJspComments = removeJspComments(content);

            analyzeJavaCodeBlocks(contentWithoutJspComments, result);
            analyzeCustomTags(contentWithoutJspComments, result);
            analyzeServerReferences(contentWithoutJspComments, result);
            analyzeDirectivesAndEL(contentWithoutJspComments, result);

        } catch (IOException e) {
            System.err.println("读取JSP文件失败: " + jspFile.getAbsolutePath() + " - " + e.getMessage());
        }

        return result;
    }

    private static String removeJspComments(String content) {
        return JspRegexConstants.COMMENT_PATTERN.matcher(content).replaceAll("");
    }

    private static void analyzeJavaCodeBlocks(String content, JspFileInfo info) {
        Matcher scriptletMatcher = JspRegexConstants.SCRIPTLET_PATTERN.matcher(content);
        while (scriptletMatcher.find()) {
            String scriptlet = scriptletMatcher.group(1).trim();
            if (!scriptlet.isEmpty()) {
                info.addScriptlet(scriptlet);
            }
        }

        Matcher declarationMatcher = JspRegexConstants.DECLARATION_PATTERN.matcher(content);
        while (declarationMatcher.find()) {
            String declaration = declarationMatcher.group(1).trim();
            if (!declaration.isEmpty()) {
                info.addDeclaration(declaration);
            }
        }

        Matcher expressionMatcher = JspRegexConstants.EXPRESSION_PATTERN.matcher(content);
        while (expressionMatcher.find()) {
            String expression = expressionMatcher.group(1).trim();
            if (!expression.isEmpty()) {
                info.addExpression(expression);
            }
        }
    }

    private static void analyzeCustomTags(String content, JspFileInfo info) {
        Set<String> foundPrefixes = new HashSet<>();

        Matcher tagMatcher = JspRegexConstants.CUSTOM_TAG_PATTERN.matcher(content);
        while (tagMatcher.find()) {
            String prefix = tagMatcher.group(1).toLowerCase();
            foundPrefixes.add(prefix);
        }

        for (String prefix : foundPrefixes) {
            if (!JspRegexConstants.STANDARD_TAG_PREFIXES.contains(prefix)) {
                info.addCustomTaglib(prefix);
            }
        }
    }

    private static void analyzeServerReferences(String content, JspFileInfo info) {
        Set<String> references = new HashSet<>();

        Matcher serverRefMatcher = JspRegexConstants.SERVER_REF_PATTERN.matcher(content);
        while (serverRefMatcher.find()) {
            String reference = serverRefMatcher.group();
            references.add(reference);
        }

        Matcher importMatcher = JspRegexConstants.PAGE_IMPORT_PATTERN.matcher(content);
        while (importMatcher.find()) {
            String importStr = importMatcher.group(1);
            Matcher importRefMatcher = JspRegexConstants.SERVER_REF_PATTERN.matcher(importStr);
            while (importRefMatcher.find()) {
                references.add(importRefMatcher.group());
            }
        }

        for (String ref : references) {
            info.addServerSpecificReference(ref);
        }
    }

    private static void analyzeDirectivesAndEL(String content, JspFileInfo info) {
        Matcher directiveMatcher = JspRegexConstants.DIRECTIVE_PATTERN.matcher(content);
        while (directiveMatcher.find()) {
            String directiveType = directiveMatcher.group(1);
            String directiveContent = directiveMatcher.group(2).trim();
            String fullDirective = "<%@ " + directiveType + " " + directiveContent + "%>";
            info.addDirective(fullDirective);
        }

        Matcher elMatcher = JspRegexConstants.EL_PATTERN.matcher(content);
        while (elMatcher.find()) {
            String elExpression = elMatcher.group();
            info.addElExpression(elExpression);
        }
    }
}
