package Common.ClassInfos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class IssueInfo {
    public String severity;   // High / Medium
    public String message;
    public String location;
    public String className;
    public String source;     // invocation, constructor, jsp, jsf, etc.
}
