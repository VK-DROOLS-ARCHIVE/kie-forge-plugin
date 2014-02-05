package org.jboss.forge.kie;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.*;

import org.jboss.forge.kie.completers.KBaseNameCommandCompleter;
import org.jboss.forge.kie.completers.KSessionCommandCompleter;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.Field;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.util.Refactory;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.project.services.ResourceFactory;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.events.ResourceCreated;
import org.jboss.forge.resources.events.ResourceDeleted;
import org.jboss.forge.resources.events.ResourceEvent;
import org.jboss.forge.resources.events.ResourceModified;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.*;
import org.jboss.forge.shell.plugins.*;
import org.jboss.forge.shell.util.ResourceUtil;
import org.jboss.seam.render.TemplateCompiler;
import org.jboss.seam.render.template.CompiledTemplateResource;
import org.kie.api.KieBase;
import org.kie.api.cdi.KBase;
import org.kie.api.cdi.KSession;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;

/**
 *
 */
@RequiresProject
@Alias("kie")
public class KiePlugin implements Plugin {
	@Inject
	private ShellPrompt prompt;

	@Inject
	private Project project;

    @Inject
    private Event<InstallFacets> installFacetsEvent;

    @Inject
    private Event<ResourceEvent> event;

    @Inject
    private TemplateCompiler compiler;

    @Inject
    private Shell shell;

    @Inject
    private ResourceFactory resourceFactory;

    public KiePlugin() {
        //resourceFactory.scan(, resourceFactory.getManagerInstance());
    }

    @SetupCommand
    public void setup(final PipeOut out)
    {
        if (!project.hasFacet(KieApiFacet.class))
        {
            installFacetsEvent.fire(new InstallFacets(KieApiFacet.class));
            if (!project.hasFacet(KieApiFacet.class))
            {
                throw new RuntimeException("Could not install KIE API");
            }
        }
        else {
            ShellMessages.success(out, "KIE API is installed.");
        }
    }

	@Command("kmodule")
	public void kmodule(@PipeIn String in, PipeOut out) throws Throwable{
		createKModuleXML(out);
	}

    @Command("add-kbase")
    public void kbase(@PipeIn String in, PipeOut out,
                      @Option(name = "default", required = false, flagOnly = true) boolean defaultKBase,
                      @Option(name = "named", required = true) String kbaseName,
                      @Option(name = "package",
                              required = false,
                              type= PromptType.JAVA_PACKAGE)
                        String kbasePkg) throws Throwable {
        ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
        FileResource kmoduleResource = resourceFacet.getResource("META-INF/kmodule.xml");

        if  ( kmoduleResource == null || !kmoduleResource.exists() ) {
            out.println(ShellColor.BLUE, "kmodule.xml is missing, running the 'kmodule' command....");
            kmodule(in, out);
        }

        Node root = XMLParser.parse(kmoduleResource.getResourceInputStream());
        List<Node> kbaseNodes = root.get("kbase");
        for (Node kbase : kbaseNodes){
            if (kbaseName.equalsIgnoreCase(kbase.getAttribute("name"))){
                out.println(ShellColor.RED, "Another kbase exists with name '" + kbaseName + "'. Cannot add.");
                return;
            }
        }
        if (defaultKBase) {
            for (Node kbase : kbaseNodes){
                if ("true".equalsIgnoreCase(kbase.getAttribute("default"))){
                    out.println(ShellColor.RED, "Another kbase ('"+kbase.getAttribute("name")+"') is marked as default. Cannot add.");
                    return;
                }
            }
        }
        Node kbaseNode = root.createChild("kbase");
        kbaseNode.attribute("name", kbaseName);
        if ( kbasePkg == null || kbasePkg.length() == 0) {
            kbasePkg = kbaseName;
        }
        if (defaultKBase) {
            kbaseNode.attribute("default", true);
        }
        kbaseNode.attribute("package", kbasePkg);
        kmoduleResource.setContents(XMLParser.toXMLString(root));
        event.fire(new ResourceModified(kmoduleResource));
        shell.execute("pick-up " + kmoduleResource.getFullyQualifiedName());

        createPkgDir(kbasePkg, resourceFacet );
    }

    @Command("add-ksession")
    public void ksession(@PipeIn String in, PipeOut out,
                      @Option(name = "named",
                              required = true) String ksessionName,
                      @Option(name = "type",
                              required = false,
                              completer = KSessionCommandCompleter.class,
                              defaultValue = "stateful") String ksessionType,
                      @Option(name = "default",
                              required = false,
                              flagOnly = true) boolean defaultKSession,
                      @Option(name = "kbase",
                              completer = KBaseNameCommandCompleter.class,
                              required = true) String kbaseName
            ) throws Throwable {
        ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
        FileResource kmoduleResource = resourceFacet.getResource("META-INF/kmodule.xml");

        if  ( kmoduleResource == null || !kmoduleResource.exists() ) {
            out.println(ShellColor.RED, "Specified kbase ('" + kbaseName + "') not found!. Cannot create ksession.");
            return;
        }

        Node root = XMLParser.parse(kmoduleResource.getResourceInputStream());
        List<Node> kbaseNodes = root.get("kbase");
        for (Node kbase : kbaseNodes){
            if (kbaseName.equalsIgnoreCase(kbase.getAttribute("name"))){

                List<Node> ksessionNodes = kbase.get("ksession");
                for (Node kiesession : ksessionNodes){
                    if (ksessionName.equalsIgnoreCase(kiesession.getAttribute("name"))){
                        out.println(ShellColor.RED, "Another ksession exists with name '" + ksessionName + "'. Cannot add.");
                        return;
                    }
                }
                Node ksessionNode = kbase.createChild("ksession");
                ksessionNode.attribute("name", ksessionName);
                if (defaultKSession) {
                    ksessionNode.attribute("default", true);
                }
                ksessionNode.attribute("type", ksessionType);
                kmoduleResource.setContents(XMLParser.toXMLString(root));
                event.fire(new ResourceModified(kmoduleResource));
                shell.execute("pick-up " + kmoduleResource.getFullyQualifiedName());
                return;
            }
        }
        if ( kbaseNodes.size() == 0) {
            out.println(ShellColor.RED, "No kbase definitions found!. Cannot create ksession.");
        } else {
            out.println(ShellColor.RED, "Specified kbase ('"+kbaseName+"') not found!. Cannot create ksession.");
        }
    }

    private void createPkgDir(String kbasePkg, ResourceFacet resourceFacet) {
        kbasePkg = kbasePkg.replace('.', File.separatorChar);
        File f = new File(resourceFacet.getResourceFolder().getFullyQualifiedName(), kbasePkg);
        resourceFacet.getResourceFolder().createFrom(f);
    }

    private void createKModuleXML(PipeOut out) throws Throwable{
        ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
        FileResource kmoduleResource = resourceFacet.getResource("META-INF/kmodule.xml");
        if  ( kmoduleResource != null && kmoduleResource.exists() ) {
            boolean overwrite = prompt.promptBoolean("kmodule.xml is already present, override?", false);
            if (overwrite) {
                kmoduleResource.delete();
                event.fire(new ResourceDeleted(kmoduleResource));
            } else {
                return;
            }
        }
        CompiledTemplateResource pluginSource = compiler.compileResource(getClass().getResourceAsStream(
                "/templates/KModuleTemplate.jv"));

        kmoduleResource = resourceFacet.createResource(pluginSource.render().toCharArray(), "META-INF/kmodule.xml");
        out.println(kmoduleResource.toString());
        event.fire(new ResourceCreated(kmoduleResource));
        /**
         * Pick up the generated resource.
         */
        shell.execute("pick-up " + kmoduleResource.getFullyQualifiedName());
	}

	@Command("generate-cdi")
	public void generateCDI(@PipeIn String in, PipeOut out,
                            @Option(name = "package",
                                    required = true,
                                    type= PromptType.JAVA_PACKAGE)
                                    String packageName,
                            @Option(name = "named",
                                    required = true,
                                    type= PromptType.JAVA_CLASS)
                                    String name
                            ) throws Throwable{

        final JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

        String entityPackage;

        if ((packageName != null) && !"".equals(packageName))
        {
            entityPackage = packageName;
        }
        else if (getPackagePortionOfCurrentDirectory() != null)
        {
            entityPackage = getPackagePortionOfCurrentDirectory();
        }
        else
        {
            entityPackage = shell.promptCommon(
                    "In which package you'd like to create this Class, or enter for default",
                    PromptType.JAVA_PACKAGE, java.getBasePackage());
        }

        JavaClass javaClass = JavaParser.create(JavaClass.class)
                .setPackage(entityPackage)
                .setName(name)
                .setPublic()
                .addInterface(Serializable.class);
        javaClass.addImport(Inject.class);
        javaClass.addImport(KBase.class);
        javaClass.addImport(KSession.class);
        javaClass.addImport(KieBase.class);
        javaClass.addImport(KieSession.class);
        javaClass.addImport(StatelessKieSession.class);

        StringBuffer sb = new StringBuffer();
        addKieBases(javaClass, out, sb);

//        javaClass.addMethod()
//                .setName("fireRules")
//                .setReturnType(boolean.class)
//                .setBody("");

        JavaResource javaFileLocation = java.saveJavaSource(javaClass);

        shell.println("Created @JavaSourceFile [" + javaClass.getQualifiedName() + "]");

        /**
         * Pick up the generated resource.
         */
        shell.execute("pick-up " + javaFileLocation.getFullyQualifiedName());
	}

    private void addKieBases(JavaClass javaClass, PipeOut out, StringBuffer sb) {

        ResourceFacet resourceFacet = project.getFacet(ResourceFacet.class);
        FileResource kmoduleResource = resourceFacet.getResource("META-INF/kmodule.xml");

        if  ( kmoduleResource == null || !kmoduleResource.exists() ) {
            out.println(ShellColor.RED, "kmodule.xml not found! Cannot generate Code.");
            return;
        }

        Node root = XMLParser.parse(kmoduleResource.getResourceInputStream());
        List<Node> kbaseNodes = root.get("kbase");
        int kbaseCtr=1;
        int ksessionCtr=1;
        for (Node kbase : kbaseNodes){
            String kbaseName = kbase.getAttribute("name");
            String normalizedKbaseName = "kbase"+(kbaseCtr++);
            //String normalizedKbaseName = Character.toUpperCase(kbaseName.charAt(0))+kbaseName.substring(1);
            Field<JavaClass> id = javaClass.addField("private KieBase "+(normalizedKbaseName)+";");
            id.addAnnotation(Inject.class);
            id.addAnnotation(KBase.class).setStringValue("value", kbaseName);
            Refactory.createGetterAndSetter(javaClass, id);

            List<Node> ksessionNodes = kbase.get("ksession");
            for (Node ksession : ksessionNodes){
                String ksessionName = ksession.getAttribute("name");
                String normalizedKsessionName = "ksession"+(ksessionCtr++);
                String type = ksession.getAttribute("type");
                Field<JavaClass> ks = null;
                if ("stateless".equalsIgnoreCase(type)) {
                    ks = javaClass.addField("private StatelessKieSession "+(normalizedKsessionName)+";");
                }  else {
                    ks = javaClass.addField("private KieSession "+(normalizedKsessionName)+";");
                }
                ks.addAnnotation(Inject.class);
                ks.addAnnotation(KSession.class).setStringValue("value", ksessionName);
                Refactory.createGetterAndSetter(javaClass, ks);
            }
        }
    }

    @Command("generate-api")
    public void generateAPI(@PipeIn String in, PipeOut out) {

    }


    /**
     * Retrieves the package portion of the current directory if it is a package, null otherwise.
     *
     * @return String representation of the current package, or null
     */
    private String getPackagePortionOfCurrentDirectory()
    {
        for (DirectoryResource r : project.getFacet(JavaSourceFacet.class).getSourceFolders())
        {
            final DirectoryResource currentDirectory = shell.getCurrentDirectory();
            if (ResourceUtil.isChildOf(r, currentDirectory))
            {
                // Have to remember to include the last slash so it's not part of the package
                return currentDirectory.getFullyQualifiedName().replace(r.getFullyQualifiedName() + "/", "")
                        .replaceAll("/", ".");
            }
        }
        return null;
    }
}
