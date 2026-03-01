package Common.ClassInfos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class JavaClassInfo {
    private List<String> imports = new ArrayList<>();
    private List<AnnotationInfo> annotations = new ArrayList<>();
    private String fullName;           // 全限定名 com.haroon.model.Student
    private String simpleName;         // Student
    private String packageName;        // com.haroon.model
    private String kind;               // "Class" 或 "Interface"
//    private String fullSourceCode;
    private List<String> modifiers = new ArrayList<>();
    private List<FieldInfo> fields = new ArrayList<>();
    private List<MethodInfo> methods = new ArrayList<>();
    private List<IssueInfo> issues = new ArrayList<>();   // 该类触发的所有 Issue
    private boolean isGeneratedFromJsp = false;   // 新增
    private String originalJspFile;               // 可选：记录原始 JSP 名


    public void addModifier(String modifier) {
        this.modifiers.add(modifier);
    }

    public void addAnnotation(AnnotationInfo annotation) {
        this.annotations.add(annotation);
    }

    public void addField(FieldInfo field) {
        this.fields.add(field);
    }

    public void addMethod(MethodInfo method) {
        this.methods.add(method);
    }

    public void addIssue(IssueInfo issue) {
        this.issues.add(issue);
    }

    public boolean getIsGeneratedFromJsp() {
        return this.isGeneratedFromJsp;
    }

    public void setIsGeneratedFromJsp(boolean b) {
        this.isGeneratedFromJsp = b;
    }

    public void addImport(String imp) {
        this.imports.add(imp);
    }


}
