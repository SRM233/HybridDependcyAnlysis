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

    public List<String> elExpressions = new ArrayList<>();
    public List<String> componentTags = new ArrayList<>();
    public int componentCount;
    public int maxComponentDepth;
    public List<String> hardcodedPaths = new ArrayList<>();
    public boolean hasAjax;
    public boolean hasBinding;
    public boolean hasInputFile;
    public boolean isTransientView;
    public boolean hasSubview;
    public boolean hasSessionAccess;
    public boolean hasFacesContextAccess;
    public boolean hasApplicationAccess;
}
