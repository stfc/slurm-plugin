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

/**
 * An agent which can store limits on HPC resource usage.
 * Subclasses of {@link BatchSlave} must define their own prefix and prefix getter,
 * as well as their own logger where necessary.
 * These agents also control the formatting of batch options input by the user.
 * Subclasses must therefore implement the
 * {@link formatBatchOptions(int, int, int, int, String, String, boolean)} method.
 * @author Eli Chadwick
 */
public abstract class BatchSlave extends Slave {

    /**
     * Used for logging information.
     */
    private static final Logger LOGGER = Logger.getLogger(BatchSlave.class.getName());

    //subclasses must define/implement their own logger and prefix

    /**
     * Configuration of limits on HPC resource usage.
     */
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

    /**
     * Get the configuration of limits on HPC resource usage.
     * @return can be null
     */
    public final ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    /**
     * Get the prefix for HPC options on the batch system, e.g.&nbsp;#SBATCH.
     */
    public abstract String getPrefix();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Computer createComputer();

    /**
     * Format HPC options.
     * <p>
     * Takes user input of resource requirements and formats for relevant
     * batch system.
     *
     * @param nodes                    number of nodes to reserve
     * @param tasks                    number of tasks to run
     * @param cpusPerTask              number of CPUs to reserve per task
     * @param walltime                 walltime required for batch job
     * @param queue                    batch system queue to use
     * @param features                 specific node properties required
     * @param exclusive                require exclusive use of reserved nodes
     * @return String of formatted HPC options.
     */
    public abstract String formatBatchOptions(int nodes, int tasks, int cpusPerTask,
            int walltime, String queue, String features, boolean exclusive);
            //NotificationConfig notificationConfig);

    /**
     * Reduce the time available on the node.
     *
     * @param time   the amount of time to reduce by (seconds)
     */
    public final void reduceAvailableSeconds(final int time) {
        resourceConfig.reduceAvailableSeconds(time);
    }

    /**
     * Terminate the agent.
     *
     * @author Laisvydas Skurevicius
     */
    public final void terminate() {
        LOGGER.log(Level.INFO, "Terminating agent {0}", getNodeName());
        try {
            Jenkins.getInstance().removeNode(this);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to terminate instance: " + getNodeName(), e);
        }
    }
}
