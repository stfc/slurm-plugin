package io.jenkins.plugins.slurm;

import hudson.AbortException;
import hudson.model.Descriptor.FormException;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

public class ResourceConfigTest {//tests methods in ResourceConfig
    
    @Test
    public void verifyAvailableSeconds_InitialisesAvailableSecondsCorrectly() {
        ResourceConfig config = new ResourceConfig(1,1,100,100,"queue"); //availableMinutes = 100

        int expectedSeconds = 100*60;
        int actualSeconds = config.getAvailableSeconds();
        
        Assert.assertEquals(expectedSeconds,actualSeconds);
    }
    
    @Test
    public void verifyAvailableSeconds_NoChangeIfSecondsWithinRangeOfMinutes() {
        ResourceConfig config = new ResourceConfig(1,1,100,100,"queue"); //availableMinutes = 100

        config.reduceAvailableSeconds(15); //to make availableSeconds not be availableMinutes*60. Also reduces availableMinutes
        config.verifyAvailableSeconds();
        
        int expectedSeconds = 100*60-15;
        int actualSeconds = config.getAvailableSeconds();
        
        Assert.assertEquals(expectedSeconds,actualSeconds);
    }
    
    @Test
    public void reduceAvailableSeconds_CorrectlyReducesWithMinuteOverflow() {
        ResourceConfig config = new ResourceConfig(1,1,100,100,"queue"); //availableMinutes = 100

        config.reduceAvailableSeconds(15);
        
        int expectedSeconds = 100*60-15;
        int expectedMinutes = 99;
        int actualSeconds = config.getAvailableSeconds();
        int actualMinutes = config.getAvailableMinutes();
        
        Assert.assertEquals(expectedSeconds,actualSeconds);
        Assert.assertEquals(expectedMinutes,actualMinutes);
    }
    
    
    @Test
    public void reduceAvailableSeconds_CorrectlyReducesWithoutMinuteOverflow() {
        ResourceConfig config = new ResourceConfig(1,1,100,100,"queue"); //availableMinutes = 100

        config.reduceAvailableSeconds(15); //should overflow as in previous test
        config.reduceAvailableSeconds(10); //should not overflow a second time
        
        int expectedSeconds = 100*60-25;
        int expectedMinutes = 99;
        int actualSeconds = config.getAvailableSeconds();
        int actualMinutes = config.getAvailableMinutes();
        
        Assert.assertEquals(expectedSeconds,actualSeconds);
        Assert.assertEquals(expectedMinutes,actualMinutes);
    }

    
    @Test
    public void verifyAvailableSeconds_SetToZeroIfBecomesNegative() {
        ResourceConfig config = new ResourceConfig(1,1,100,1,"queue"); //availableMinutes = 1

        config.reduceAvailableSeconds(70); //should make availableSeconds negative, forcing reset to zero
        
        int expectedSeconds = 0;
        int expectedMinutes = 0;
        int actualSeconds = config.getAvailableSeconds();
        int actualMinutes = config.getAvailableMinutes();
        
        Assert.assertEquals(expectedSeconds,actualSeconds);
        Assert.assertEquals(expectedMinutes,actualMinutes);
    }
    
}