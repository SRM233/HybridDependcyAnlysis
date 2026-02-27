package Common.ClassInfos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class ParameterInfo {
    public String type;
    public String name;
    public List<String> modifiers = new ArrayList<>();
}
