package Common.JspFileInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class JspFileInfo {
    public String filePath;
    public List<String> namespaces = new ArrayList<>();
    public List<String> includes = new ArrayList<>();
    public List<String> beans = new ArrayList<>();
    
    // JSP content analysis fields
    private List<String> customTaglibs = new ArrayList<>();
    private List<String> serverSpecificReferences = new ArrayList<>();
    private int javaCodeBlockCount = 0;
    private int javaCodeLineCount = 0;
    private List<String> directives = new ArrayList<>();
    private List<String> elExpressions = new ArrayList<>();
    private List<String> scriptlets = new ArrayList<>();
    private List<String> declarations = new ArrayList<>();
    private List<String> expressions = new ArrayList<>();
    
    // Helper methods
    public void addCustomTaglib(String taglib) {
        if (!customTaglibs.contains(taglib)) {
            customTaglibs.add(taglib);
        }
    }
    
    public void addServerSpecificReference(String reference) {
        if (!serverSpecificReferences.contains(reference)) {
            serverSpecificReferences.add(reference);
        }
    }
    
    public void addDirective(String directive) {
        if (!directives.contains(directive)) {
            directives.add(directive);
        }
    }
    
    public void addElExpression(String expression) {
        if (!elExpressions.contains(expression)) {
            elExpressions.add(expression);
        }
    }
    
    public void addScriptlet(String scriptlet) {
        scriptlets.add(scriptlet);
        javaCodeBlockCount++;
        javaCodeLineCount += countLines(scriptlet);
    }
    
    public void addDeclaration(String declaration) {
        declarations.add(declaration);
        javaCodeBlockCount++;
        javaCodeLineCount += countLines(declaration);
    }
    
    public void addExpression(String expression) {
        expressions.add(expression);
        javaCodeBlockCount++;
        javaCodeLineCount += countLines(expression);
    }
    
    private int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\r\n|\r|\n").length;
    }
}
