package io.jenkins.plugins.slurm;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.slaves.NodeProperty;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public abstract class BatchSlave extends Slave {
    private static final Logger LOGGER = Logger.getLogger(BatchSlave.class.getName());
    //subclasses must define/implement logger and prefix
    private ResourceConfig resourceConfig;
    
    public BatchSlave(String name, String nodeDescription, String remoteFS, 
            String numExecutors, Mode mode, String labelString, 
            ComputerLauncher launcher, RetentionStrategy retentionStrategy, 
            List<? extends NodeProperty<?>> nodeProperties, 
            ResourceConfig resourceConfig) 
            throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString, launcher, retentionStrategy, nodeProperties);
        this.resourceConfig=resourceConfig;
    }
    
    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }
    
    public abstract String getPrefix();
    
    @Override
    public abstract Computer createComputer();
    
    public abstract String formatBatchOptions(int nodes, int tasks, int cpusPerTask,
            int walltime, String queue, boolean exclusive, 
            NotificationConfig notificationConfig);
            //String outFileName, String errFileName);
    
    
    
    //terminate the slave
    public void terminate() {
        LOGGER.log(Level.INFO, "Terminating slave {0}", getNodeName());
        try {
            Jenkins.getInstance().removeNode(this);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to terminate instance: "
                    + getNodeName(), e);
        }
    }
    
    public void reduceAvailableSeconds(int time) {
        resourceConfig.reduceAvailableSeconds(time);
    }
    
    //subclasses must define formatBatchOptions function
}