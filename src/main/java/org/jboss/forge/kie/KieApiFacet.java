package org.jboss.forge.kie;

import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.project.dependencies.ScopeType;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.PackagingFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.spec.javaee.CDIFacet;

import javax.inject.Inject;
import java.util.List;

@Alias("kie.api")
@RequiresFacet({DependencyFacet.class, PackagingFacet.class, CDIFacet.class, ResourceFacet.class})
public class KieApiFacet extends BaseFacet {

    @Inject
    private Shell shell;

    @Inject
    private DependencyInstaller installer;

    @Override
    public boolean install() {
        DependencyFacet deps = project.getFacet(DependencyFacet.class);

        List<Dependency> versions = deps.resolveAvailableVersions("org.kie:kie-api:[,]");
        Dependency version = shell.promptChoiceTyped("Install which version of the KIE API?", versions, versions.get(versions.size() - 1));
        deps.setProperty("kie.api.version", version.getVersion());
        DependencyBuilder apiDep = DependencyBuilder.create("org.kie:kie-api:${kie.api.version}").setScopeType(ScopeType.COMPILE);
        DependencyBuilder droolsCoreDep = DependencyBuilder.create("org.drools:drools-core:${kie.api.version}").setScopeType(ScopeType.COMPILE);
        DependencyBuilder droolsCompilerDep = DependencyBuilder.create("org.drools:drools-compiler:${kie.api.version}").setScopeType(ScopeType.COMPILE);

        installer.install(project, apiDep);
        installer.install(project, droolsCoreDep);
        installer.install(project, droolsCompilerDep);

        return true;
    }

    @Override
    public boolean isInstalled() {
        Dependency dep = DependencyBuilder.create("org.kie:kie-api");
        return project.getFacet(DependencyFacet.class).hasEffectiveDependency(dep);
    }
}