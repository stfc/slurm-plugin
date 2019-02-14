package io.jenkins.plugins.slurm;

import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BatchSlave extends Slave {
    private static final Logger LOGGER = Logger.getLogger(BatchSlave.class.getName());
    //subclasses must define/implement logger and prefix
    private ResourceConfig resourceConfig;

    public BatchSlave(final String name, final String nodeDescription,
            final String remoteFS, final String numExecutors, final Mode mode,
            final String labelString, final ComputerLauncher launcher, 
            final RetentionStrategy retentionStrategy,
            final List<? extends NodeProperty<?>> nodeProperties,
            final ResourceConfig resourceConfig)
            throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString,
                launcher, retentionStrategy, nodeProperties);
        this.resourceConfig = resourceConfig;
    }

    public final ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    public abstract String getPrefix();

    @Override
    public abstract Computer createComputer();

    public abstract String formatBatchOptions(int nodes, int tasks, int cpusPerTask,
            int walltime, String queue, String features, boolean exclusive,
            NotificationConfig notificationConfig);
    
    //terminate the slave
    public final void terminate() {
        LOGGER.log(Level.INFO, "Terminating slave {0}", getNodeName());
        try {
            Jenkins.getInstance().removeNode(this);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to terminate instance: " + getNodeName(), e);
        }
    }

    public final void reduceAvailableSeconds(final int time) {
        resourceConfig.reduceAvailableSeconds(time);
    }

    //subclasses must define formatBatchOptions function
}
