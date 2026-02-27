package Common.JsfFileInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class JsfFileInfo{
    public String filePath;
    public List<String> namespaces = new ArrayList<>();
    public List<String> includes = new ArrayList<>();
    public List<String> beans = new ArrayList<>();
}
