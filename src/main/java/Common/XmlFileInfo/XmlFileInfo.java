package Common.XmlFileInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class XmlFileInfo{
    public String fileType;
    public String filePath;
    public Map<String, Object> data = new HashMap<>();
}
