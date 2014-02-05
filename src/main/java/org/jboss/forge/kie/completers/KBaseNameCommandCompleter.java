package org.jboss.forge.kie.completers;

import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.completer.SimpleTokenCompleter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KBaseNameCommandCompleter extends SimpleTokenCompleter
{
    @Inject
    private Project project;

    @Override
    public Iterable<?> getCompletionTokens()
    {
        ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
        FileResource kmoduleResource = resourceFacet.getResource("META-INF/kmodule.xml");

        if  ( kmoduleResource == null || !kmoduleResource.exists() ) {
            return Arrays.asList("kmodule.xml not found!");
        }

        Node root = XMLParser.parse(kmoduleResource.getResourceInputStream());
        List<Node> kbaseNodes = root.get("kbase");
        List<String> kbaseNameList = new ArrayList<String>();
        for (Node kbase : kbaseNodes){
            kbaseNameList.add(kbase.getAttribute("name"));
        }

        if ( kbaseNameList.size() == 0) {
            return Arrays.asList("No kbases found! Please use add-kbase command to create kbase.");
        }
        return kbaseNameList;
    }
}
