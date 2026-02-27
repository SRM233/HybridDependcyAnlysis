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
    public String fullName;           // 全限定名 com.haroon.model.Student
    public String simpleName;         // Student
    public String packageName;        // com.haroon.model
    public String kind;               // "Class" 或 "Interface"
    public List<String> modifiers = new ArrayList<>();
    public List<AnnotationInfo> annotations = new ArrayList<>();
    public List<FieldInfo> fields = new ArrayList<>();
    public List<MethodInfo> methods = new ArrayList<>();
    public List<IssueInfo> issues = new ArrayList<>();   // 该类触发的所有 Issue
    public boolean isGeneratedFromJsp = false;   // 新增
    public String originalJspFile;               // 可选：记录原始 JSP 名
}
