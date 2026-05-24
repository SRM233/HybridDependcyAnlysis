package Common.ClassInfos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class JavaClassInfo {
    private List<String> imports = new ArrayList<>();
    private List<AnnotationInfo> annotations = new ArrayList<>();
    private String fullName;           // Fully qualified name e.g. com.haroon.model.Student
    private String simpleName;         // Student
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String packageName;        // com.haroon.model
    private String kind;               // "Class" or "Interface"
//    private String fullSourceCode;
    private List<String> modifiers = new ArrayList<>();
    private List<FieldInfo> fields = new ArrayList<>();
    private List<MethodInfo> methods = new ArrayList<>();
    private List<IssueInfo> issues = new ArrayList<>();   // All Issues triggered by this class


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

    public void addImport(String imp) {
        this.imports.add(imp);
    }


}
