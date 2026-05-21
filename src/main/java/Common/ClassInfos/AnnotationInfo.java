package Common.ClassInfos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class AnnotationInfo {
    private String name;
    private Map<String, String> values = new HashMap<>();

    public void addValue(String key, String value) {
        values.put(key, value);
    }
}
