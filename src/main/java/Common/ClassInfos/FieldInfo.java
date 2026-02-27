package Common.ClassInfos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class FieldInfo {
    public String name;
    public String type;
    public List<String> modifiers = new ArrayList<>();
    public List<AnnotationInfo> annotations = new ArrayList<>();
}
