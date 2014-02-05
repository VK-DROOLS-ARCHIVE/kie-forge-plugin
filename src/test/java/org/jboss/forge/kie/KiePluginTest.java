package org.jboss.forge.kie;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.kie.KiePlugin;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;

public class KiePluginTest extends AbstractShellTest
{
   @Deployment
   public static JavaArchive getDeployment()
   {
      return AbstractShellTest.getDeployment()
            .addPackages(true, KiePlugin.class.getPackage());
   }

   @Test
   public void testDefaultCommand() throws Exception
   {
      getShell().execute("kie");
   }

   @Test
   public void testCommand() throws Exception
   {
      getShell().execute("kie command");
   }

   @Test
   public void testPrompt() throws Exception
   {
      queueInputLines("y");
      getShell().execute("kie prompt foo bar");
   }
}
