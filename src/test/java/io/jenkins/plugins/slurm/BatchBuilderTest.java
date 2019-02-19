package io.jenkins.plugins.slurm;

import hudson.AbortException;
import hudson.model.Descriptor.FormException;
import hudson.model.Label;
import hudson.model.Slave;
import hudson.model.Node;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.plugins.sshslaves.verifiers.ManuallyTrustedKeyVerificationStrategy;
import hudson.slaves.DumbSlave;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class BatchBuilderTest {//tests methods in BatchBuilder
    
    private static final Logger LOGGER = Logger.getLogger(SLURMSlave.class.getName());

    //this doesn't work. TODO - note in docs that this doesn't work
    /*
    @BeforeClass
    public static void launchSLURMSlave() throws FormException, IOException { //generalise?
        SSHLauncher launcher = new SSHLauncher("localhost", 5000, "dummyCredentialId", null, "xyz", null, null, 30, 1, 1, new ManuallyTrustedKeyVerificationStrategy(true));
        slave = new SLURMSlave("scarf18","","/home/escg/jenkins/","1",Node.Mode.NORMAL,"scarf18",
                        launcher,
                        new BatchRetentionStrategy(1), 
                        Collections.<NodeProperty<?>>emptyList(),
                        new ResourceConfig(4,4,5,50,"testqueue"));
    }
    */
    /*
    @BeforeClass
    public static void launchComputer() throws Exception {
        DumbSlave testDumbSlave = jenkins.createOnlineSlave(Label.get("scarf"));
        Slave testSlave = (Slave)testDumbSlave;
    }
    */
    
    
    
    //tests not run for invalid resource input; this is tested elsewhere
    
    @Test
    public void isScriptValid_FalseForNullScript() {
        BatchBuilder builder=new SLURMBuilder("test", 1, 1, 1, 1, "queue", "", false, ""); 
            //new NotificationConfig(true, true, true, "test@test.com"), "");
            
        String prefix = "#TEST";
        String inputScript = null;
        boolean expectedResult = false;
        boolean actualResult = builder.isScriptValid(inputScript,prefix);
        
        Assert.assertEquals(actualResult, expectedResult);
    }
    
    @Test
    public void isScriptValid_FalseForEmptyScript() {
        BatchBuilder builder=new SLURMBuilder("test", 1, 1, 1, 1, "queue", "", false, "");
            //new NotificationConfig(true, true, true, "test@test.com"), "");
            
        String prefix = "#TEST";
        String inputScript = "";
        boolean expectedResult = false;
        boolean actualResult = builder.isScriptValid(inputScript,prefix);
        
        Assert.assertEquals(actualResult, expectedResult);
    }
    
    @Test
    public void isScriptValid_FalseForScriptOfEntirelyInvalidLines() {
        BatchBuilder builder=new SLURMBuilder("test", 1, 1, 1, 1, "queue", "", false, "");
            //new NotificationConfig(true, true, true, "test@test.com"), "");
            
        String prefix = "#TEST";
        String inputScript = "#TEST -N 1\n   \n\t\n\n";
        boolean expectedResult = false;
        boolean actualResult = builder.isScriptValid(inputScript,prefix);
        
        Assert.assertEquals(actualResult, expectedResult);
    }
    
    @Test
    public void isScriptValid_TrueForValidScript() {
        BatchBuilder builder=new SLURMBuilder("test", 1, 1, 1, 1, "queue", "", false, "");
            //new NotificationConfig(true, true, true, "test@test.com"), "");
        
        String prefix = "#TEST";
        String inputScript = "#TEST -n 1\nValid content\n";
        boolean expectedResult = true;
        boolean actualResult = builder.isScriptValid(inputScript,prefix);
        
        Assert.assertEquals(actualResult, expectedResult);
    }
    
    @Test
    public void filterScript_CorrectlyFiltersScriptWithInvalidLines() {
        //generic non-failing constructor
        BatchBuilder builder=new SLURMBuilder("test", 1, 1, 1, 1, "queue", "", false, "");
            //new NotificationConfig(true, true, true, "test@test.com"), "");
            
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
        
        String actualFilteredScript = builder.filterScript(script,"prefix");

        Assert.assertEquals(actualFilteredScript,expectedFilteredScript);
    }
    
    @Test
    public void generateSystemScript_CorrectlyGeneratesScript() {
        BatchBuilder builder=new SLURMBuilder("test", 1, 1, 1, 1, "queue", "", false, "");
            //new NotificationConfig(true, true, true, "test@test.com"), "");
            
        String formattedBatchOptions = "#SBATCH -N 1\n#SBATCH -n 1\n";
        String userScriptName = "test_script.sh";
        String communicationFile = "comms.txt";
        String expectedScript = "#!/bin/bash -xe\n"
                           + "#Script automatically generated by SLURM Plugin\n"
                           + "#SBATCH -N 1\n"
                           + "#SBATCH -n 1\n"
                           + "chmod 755 test_script.sh\n"
                           + "ret=1\n"
                           + "{\n"
                           + "./test_script.sh &&\n"
                           + "ret=$? &&\n"
                           + "echo $ret > comms.txt\n"
                           + "} || {\n"
                           + "ret=$?\n"
                           + "echo $ret > comms.txt\n"
                           + "}\n"
                           + "times >> comms.txt\n"
                           + "exit $ret\n"
                           + "#End of automatically generated script\n";
        
        String actualScript = builder.generateSystemScript(formattedBatchOptions,userScriptName,communicationFile);
        
        Assert.assertEquals(actualScript,expectedScript);
    }
    
    @Test
    public void generateUserScript_CorrectlyGeneratesScript() {
        BatchBuilder builder=new SLURMBuilder("test", 1, 1, 1, 1, "queue", "", false, "");
            //new NotificationConfig(true, true, true, "test@test.com"), "");
            
        String prefix = "#TEST";
        String inputScript = "#TEST -n 1\nTest content\n";
        String expectedScript = "#!/bin/bash -xe\n"
                              + "#Script automatically generated by SLURM Plugin. User-entered content follows this comment. \n"
                              + "Test content\n"
                              + "#End of user-entered content.\n";
        
        String actualScript = builder.generateUserScript(inputScript,prefix);
        
        Assert.assertEquals(actualScript,expectedScript);
    }

}