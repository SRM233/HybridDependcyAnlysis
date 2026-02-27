package Common.ClassInfos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class MethodInfo {
    public String name;
    public String returnType;
    public List<String> modifiers = new ArrayList<>();
    public List<ParameterInfo> parameters = new ArrayList<>();
}
