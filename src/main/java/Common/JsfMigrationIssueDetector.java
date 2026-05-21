package Common;

import Common.ClassInfos.IssueInfo;
import Common.JsfFileInfo.JsfFileInfo;

import java.util.ArrayList;
import java.util.List;

public class JsfMigrationIssueDetector {

    private static final int EL_EXPRESSION_THRESHOLD = 20;
    private static final int COMPONENT_COUNT_THRESHOLD = 50;

    public static List<IssueInfo> detect(JsfFileInfo info) {
        List<IssueInfo> issues = new ArrayList<>();

        issues.addAll(checkpoint1_ViewStatePersistence(info));
        issues.addAll(checkpoint2_ExcessiveBeanBinding(info));
        issues.addAll(checkpoint3_HardcodedResourcePaths(info));
        issues.addAll(checkpoint5_AjaxPartialSubmit(info));
        issues.addAll(checkpoint6_LargeComponentTree(info));
        issues.addAll(checkpoint7_ContainerApiAccess(info));

        return issues;
    }

    private static List<IssueInfo> checkpoint1_ViewStatePersistence(JsfFileInfo info) {
        List<IssueInfo> issues = new ArrayList<>();
        if (!info.isTransientView) {
            IssueInfo issue = new IssueInfo();
            issue.setSeverity("High");
            issue.setMessage("View state persistence risk: <f:view> is not transient (default keeps full state on server), which increases memory overhead in cloud/containerized environments");
            issue.setLocation(info.filePath);
            issue.setClassName(new java.io.File(info.filePath).getName());
            issue.setSource("jsf");
            issue.setType("StatefulSession");
            issues.add(issue);
        }
        if (info.hasSubview) {
            IssueInfo issue = new IssueInfo();
            issue.setSeverity("High");
            issue.setMessage("View state persistence risk: <f:subview> detected, may introduce additional state management complexity in cloud migration");
            issue.setLocation(info.filePath);
            issue.setClassName(new java.io.File(info.filePath).getName());
            issue.setSource("jsf");
            issue.setType("StatefulSession");
            issues.add(issue);
        }
        return issues;
    }

    private static List<IssueInfo> checkpoint2_ExcessiveBeanBinding(JsfFileInfo info) {
        List<IssueInfo> issues = new ArrayList<>();
        if (info.elExpressions.size() > EL_EXPRESSION_THRESHOLD) {
            IssueInfo issue = new IssueInfo();
            issue.setSeverity("Medium");
            issue.setMessage("Excessive ViewScoped/SessionScoped bean binding: found " + info.elExpressions.size()
                    + " EL expressions (> " + EL_EXPRESSION_THRESHOLD + " threshold), suggesting heavy stateful bean usage that complicates cloud migration");
            issue.setLocation(info.filePath);
            issue.setClassName(new java.io.File(info.filePath).getName());
            issue.setSource("jsf");
            issue.setType("memory_replication");
            issues.add(issue);
        }
        return issues;
    }

    private static List<IssueInfo> checkpoint3_HardcodedResourcePaths(JsfFileInfo info) {
        List<IssueInfo> issues = new ArrayList<>();
        for (String path : info.hardcodedPaths) {
            IssueInfo issue = new IssueInfo();
            issue.setSeverity("High");
            issue.setMessage("Hardcoded resource path: \"" + path + "\" will break in cloud/containerized environments where absolute paths differ; use dynamic resource resolution instead");
            issue.setLocation(info.filePath);
            issue.setClassName(new java.io.File(info.filePath).getName());
            issue.setSource("jsf");
            issue.setType("FileSystemDependency");
            issues.add(issue);
        }
        return issues;
    }

    private static List<IssueInfo> checkpoint5_AjaxPartialSubmit(JsfFileInfo info) {
        List<IssueInfo> issues = new ArrayList<>();
        if (info.hasAjax) {
            IssueInfo issue = new IssueInfo();
            issue.setSeverity("Medium");
            issue.setMessage("AJAX partial submit detected: <f:ajax> or ajax=\"true\" introduces client-server state synchronization complexity");
            issue.setLocation(info.filePath);
            issue.setClassName(new java.io.File(info.filePath).getName());
            issue.setSource("jsf");
            issue.setType("StatefulSession");
            issues.add(issue);
        }
        return issues;
    }

    private static List<IssueInfo> checkpoint6_LargeComponentTree(JsfFileInfo info) {
        List<IssueInfo> issues = new ArrayList<>();
        if (info.componentCount > COMPONENT_COUNT_THRESHOLD) {
            IssueInfo issue = new IssueInfo();
            issue.setSeverity("Medium");
            issue.setMessage("Large component tree: " + info.componentCount + " components (> " + COMPONENT_COUNT_THRESHOLD
                    + " threshold) with max depth " + info.maxComponentDepth
                    + ", increases memory footprint and startup time in cloud environments");
            issue.setLocation(info.filePath);
            issue.setClassName(new java.io.File(info.filePath).getName());
            issue.setSource("jsf");
            issue.setType("memory_replication");
            issues.add(issue);
        }
        return issues;
    }

    private static List<IssueInfo> checkpoint7_ContainerApiAccess(JsfFileInfo info) {
        List<IssueInfo> issues = new ArrayList<>();
        if (info.hasSessionAccess) {
            IssueInfo issue = new IssueInfo();
            issue.setSeverity("High");
            issue.setMessage("Direct HttpSession access via EL (#{session.}) detected — breaks stateless cloud architecture; use external session store or token-based auth");
            issue.setLocation(info.filePath);
            issue.setClassName(new java.io.File(info.filePath).getName());
            issue.setSource("jsf");
            issue.setType("StatefulSession");
            issues.add(issue);
        }
        if (info.hasApplicationAccess) {
            IssueInfo issue = new IssueInfo();
            issue.setSeverity("High");
            issue.setMessage("Direct ServletContext access via EL (#{application.}) detected — application-scoped data prevents clean horizontal scaling");
            issue.setLocation(info.filePath);
            issue.setClassName(new java.io.File(info.filePath).getName());
            issue.setSource("jsf");
            issue.setType("StatefulSession");
            issues.add(issue);
        }
        if (info.hasFacesContextAccess) {
            IssueInfo issue = new IssueInfo();
            issue.setSeverity("High");
            issue.setMessage("Direct FacesContext access via EL (#{facesContext.}) detected — tightly coupled to JSF servlet container, complicates migration to stateless cloud architecture");
            issue.setLocation(info.filePath);
            issue.setClassName(new java.io.File(info.filePath).getName());
            issue.setSource("jsf");
            issue.setType("StatefulSession");
            issues.add(issue);
        }
        return issues;
    }
}
