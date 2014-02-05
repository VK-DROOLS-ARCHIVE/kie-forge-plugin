package org.jboss.forge.kie.resources;

import org.jboss.forge.project.services.ResourceFactory;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.Resource;
import org.jboss.forge.resources.ResourceHandles;

import java.io.File;
import java.util.List;

@ResourceHandles("kmodule.xml")
public class KModuleResource extends FileResource<KModuleResource>{

    protected KModuleResource(ResourceFactory factory, File file) {
        super(factory, file);
    }

    @Override
    public Resource<File> createFrom(File file) {
        return new KModuleResource(resourceFactory, file);
    }

    @Override
    protected List<Resource<?>> doListResources() {
        return null;
    }
}
