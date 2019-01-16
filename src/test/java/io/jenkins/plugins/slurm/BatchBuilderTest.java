package io.jenkins.plugins.slurm;

import hudson.model.Descriptor.FormException;
import hudson.model.Node;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import java.io.IOException;
import java.util.Collections;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BatchBuilderTest {//tests methods in BatchBuilder
    private static SLURMSlave slave; //generalise pls!!

    /* //this doesn't work. TODO - note in docs that this doesn't work
    @BeforeClass
    public static void launchSLURMSlave() throws FormException, IOException { //generalise?
        slave = new SLURMSlave("test","","/home/escg/jenkins/","1",Node.Mode.NORMAL,"test",
                        new SSHLauncher("ui3.scarf.rl.ac.uk",22,"ssh_scarf","","","","",10,5,10,null),
                        new BatchRetentionStrategy(1), 
                        Collections.<NodeProperty<?>>emptyList(),
                        new ResourceConfig(4,4,5,50,"testqueue"));
    }
    */
            
    @Test
    public void filterScript_ThrowsExceptionOnNullScript() {
        //generic non-failing constructor
        BatchBuilder builder=new SLURMBuilder("test", 1, 1, 1, 1, "queue", false, 
            new NotificationConfig(true, true, true, "test@test.com"), "out.o", "err.e");
            
        boolean thrown=false;
        
        try {
            builder.filterScript(null,"arbitraryPrefix");
        }
        catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(),"Script is null");
            thrown=true; //only set if message is correct
        }
        
        Assert.assertTrue("No exception was thrown",thrown);
    }
    
    @Test
    public void filterScript_CorrectlyFiltersScriptWithInvalidLines() {
        //generic non-failing constructor
        BatchBuilder builder=new SLURMBuilder("test", 1, 1, 1, 1, "queue", false, 
            new NotificationConfig(true, true, true, "test@test.com"), "out.o", "err.e");
            
        String script="start of script\n"
              +"prefix line\n"
              +"prefix\n"
              +"prefixLine\n"
              +"script content\n"
              +"prefix\n"
              +"end of script with prefix late in line\n";
        String expectedFilteredScript="start of script\n"
                              +"script content\n"
                              +"end of script with prefix late in line\n";
        
        String actualFilteredScript=builder.filterScript(script,"prefix");
        
        Assert.assertEquals(actualFilteredScript,expectedFilteredScript);
    }
    
}