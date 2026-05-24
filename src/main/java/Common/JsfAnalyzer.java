package Common;

import Common.JsfFileInfo.JsfFileInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JsfAnalyzer {

    private static final Pattern EL_PATTERN = Pattern.compile("#\\{[^}]+\\}");
    private static final Pattern HARCODED_PATH_PATTERN = Pattern.compile("(?:\"|')(/\\S*?)(?:\"|')");
    private static final Pattern SESSION_ACCESS = Pattern.compile("#\\{[^}]*\\bsession\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern APPLICATION_ACCESS = Pattern.compile("#\\{[^}]*\\bapplication\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern FACES_CONTEXT_ACCESS = Pattern.compile("#\\{[^}]*\\bfacesContext\\.", Pattern.CASE_INSENSITIVE);

    private static final Set<String> KNOWN_JSF_PREFIXES = new HashSet<>(Arrays.asList(
            "h", "p", "f", "c", "ui", "a4j", "s", "rich", "t", "ice", "e",
            "o", "omnifaces", "pe", "prime", "passthrough"
    ));

    private static final Set<String> VIEW_STATE_ATTRS = new HashSet<>(Arrays.asList(
            "src", "url", "action", "href", "value", "icon"
    ));

    public static JsfFileInfo analyze(File file) {
        JsfFileInfo info = new JsfFileInfo();

        //get jsf file path
        info.filePath = file.getAbsolutePath();

        if (!file.exists() || !file.getName().toLowerCase().endsWith(".xhtml")) {
            return info;
        }

        try {


            Document doc = Jsoup.parse(file, StandardCharsets.UTF_8.name());


            doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);

            extractNamespaces(doc, info);

            extractComponentsAndEl(doc, info);

            detectViewState(doc, info);
            detectAjax(doc, info);
            detectBinding(doc, info);
            detectInputFile(doc, info);
            detectHardcodedPaths(doc, info);
            computeComponentDepth(doc, info);
            detectContainerApiAccess(info);

        } catch (IOException e) {
            System.err.println("Jsoup parse error: " + file.getAbsolutePath() + " - " + e.getMessage());
        }

        return info;
    }


    //Collects xmlns:* attributes into info.namespaces
    private static void extractNamespaces(Document doc, JsfFileInfo info) {
        Elements all = doc.getAllElements();
        for (Element el : all) {
            Attributes attrs = el.attributes();
            for (Attribute attr : attrs) {
                String key = attr.getKey().toLowerCase();
                if (key.startsWith("xmlns:")) {
                    String ns = key.substring(6) + "=" + attr.getValue();
                    if (!info.namespaces.contains(ns)) {
                        info.namespaces.add(ns);
                    }
                }
            }
        }
    }

    private static void extractComponentsAndEl(Document doc, JsfFileInfo info) {
        Set<String> seenTags = new HashSet<>();
        Elements all = doc.getAllElements();
        for (Element el : all) {
            String tag = el.tagName();
            if (!tag.contains(":")) continue;

            String prefix = tag.substring(0, tag.indexOf(':'));
            if (!KNOWN_JSF_PREFIXES.contains(prefix.toLowerCase())) continue;

            if (seenTags.add(tag)) {
                info.componentTags.add(tag);
            }
            info.componentCount++;

            Attributes attrs = el.attributes();
            for (Attribute attr : attrs) {
                String val = attr.getValue();
                Matcher m = EL_PATTERN.matcher(val);
                while (m.find()) {
                    info.elExpressions.add(m.group());
                }
            }

            String ownText = el.ownText();
            Matcher m = EL_PATTERN.matcher(ownText);
            while (m.find()) {
                info.elExpressions.add(m.group());
            }
        }
    }

    private static void detectViewState(Document doc, JsfFileInfo info) {
        Element view = null;
        for (Element el : doc.getAllElements()) {
            if ("f:view".equals(el.tagName())) {
                view = el;
                break;
            }
        }

        if (view == null) {
            info.isTransientView = false;
            return;
        }

        String transientAttr = view.attr("transient");
        info.isTransientView = !"false".equalsIgnoreCase(transientAttr);

        for (Element el : doc.getAllElements()) {
            if ("f:subview".equals(el.tagName())) {
                info.hasSubview = true;
                break;
            }
        }
    }

    private static void detectAjax(Document doc, JsfFileInfo info) {
        for (Element el : doc.getAllElements()) {
            if ("f:ajax".equals(el.tagName())) {
                info.hasAjax = true;
                break;
            }
        }

        if (!info.hasAjax) {
            for (Element el : doc.getAllElements()) {
                String ajaxVal = el.attr("ajax");
                if ("true".equalsIgnoreCase(ajaxVal)) {
                    info.hasAjax = true;
                    break;
                }
            }
        }
    }

//    checks for binding="#{...}" on any element
    private static void detectBinding(Document doc, JsfFileInfo info) {
        for (Element el : doc.getAllElements()) {
            String bindingVal = el.attr("binding");
            if (bindingVal != null && EL_PATTERN.matcher(bindingVal).find()) {
                info.hasBinding = true;
                return;
            }
        }
    }

    private static void detectInputFile(Document doc, JsfFileInfo info) {
        for (Element el : doc.getAllElements()) {
            String tag = el.tagName().toLowerCase();
            if (tag.equals("h:inputfile") || tag.equals("p:fileupload") ||
                tag.endsWith(":inputfile") || tag.endsWith(":fileupload")) {
                info.hasInputFile = true;
                return;
            }
        }
    }

    // check  src, url, action, href, value, icon attributes for hardcoded absolute paths
    private static void detectHardcodedPaths(Document doc, JsfFileInfo info) {
        Set<String> seen = new HashSet<>();
        for (Element el : doc.getAllElements()) {
            Attributes attrs = el.attributes();
            for (Attribute attr : attrs) {
                String key = attr.getKey().toLowerCase();
                if (!VIEW_STATE_ATTRS.contains(key)) continue;
                String val = attr.getValue();
                Matcher m = HARCODED_PATH_PATTERN.matcher(val);
                while (m.find()) {
                    String path = m.group(1);
                    if (!path.startsWith("/")) continue;
                    if (path.contains("{") || path.contains("}")) continue;
                    if (seen.add(path)) {
                        info.hardcodedPaths.add(path);
                    }
                }
            }
        }
    }

    private static void computeComponentDepth(Document doc, JsfFileInfo info) {
        info.maxComponentDepth = computeMaxDepth(doc.getAllElements().first(), 0, 0);
    }

    private static int computeMaxDepth(Element el, int currentDepth, int maxDepth) {
        if (el == null) return maxDepth;

        String tag = el.tagName();
        int newDepth = currentDepth;
        if (tag.contains(":")) {
            String prefix = tag.substring(0, tag.indexOf(':'));
            if (KNOWN_JSF_PREFIXES.contains(prefix.toLowerCase())) {
                newDepth = currentDepth + 1;
            }
        }

        if (newDepth > maxDepth) maxDepth = newDepth;

        for (Element child : el.children()) {
            maxDepth = computeMaxDepth(child, newDepth, maxDepth);
        }

        return maxDepth;
    }

    private static void detectContainerApiAccess(JsfFileInfo info) {
        for (String el : info.elExpressions) {
            if (SESSION_ACCESS.matcher(el).find()) info.hasSessionAccess = true;
            if (APPLICATION_ACCESS.matcher(el).find()) info.hasApplicationAccess = true;
            if (FACES_CONTEXT_ACCESS.matcher(el).find()) info.hasFacesContextAccess = true;
        }
    }
}
