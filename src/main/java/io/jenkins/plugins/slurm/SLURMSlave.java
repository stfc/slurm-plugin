package io.jenkins.plugins.slurm;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SLURMSlave extends BatchSlave {
    private static final Logger LOGGER = Logger.getLogger(SLURMSlave.class.getName());
    private final String prefix = "#SBATCH";

    @DataBoundConstructor
    public SLURMSlave(final String name, final String nodeDescription,
            final String remoteFS, final String numExecutors, final Mode mode,
            final String labelString, final ComputerLauncher launcher,
            final RetentionStrategy retentionStrategy,
            final List<? extends NodeProperty<?>> nodeProperties,
            final ResourceConfig resourceConfig)
            throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode,
                labelString, launcher, retentionStrategy,
                nodeProperties, resourceConfig);
    }

    public final String getPrefix() {
        return prefix;
    }

    @Override
    public final Computer createComputer() {
        LOGGER.info("Creating a new SLURM Slave");
        return new SLURMSlaveComputer(this);
    }

    //ideally would be static, but isn't due to general BatchSlave calls in BatchBuilder.generateScript
    public final String formatBatchOptions(final int nodes, final int tasks,
            final int cpusPerTask, final int walltime, final String queue,
            final String features, final boolean exclusive,
            final NotificationConfig notificationConfig) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(prefix + " -N " + nodes + "\n");
        buffer.append(prefix + " -n " + tasks + "\n");
        buffer.append(prefix + " -c " + cpusPerTask + "\n");
        buffer.append(prefix + " -t " + walltime + "\n");
        buffer.append(prefix + " -W #added by SLURM plugin\n");
        if (queue != null && !queue.isEmpty()) {
            buffer.append(prefix + " -p " + queue + "\n");
        }
        if (features != null && !features.isEmpty()) {
            buffer.append(prefix + " -C " + features + "\n");
        }
        if (exclusive) {
            buffer.append(prefix + " --exclusive \n");
        }
        //NotificationConfig must be formatted by this class as it is not specific to any batch system
        if (notificationConfig != null) { 
            if (notificationConfig.isNotifyStartTicked()
                    || notificationConfig.isNotifyEndTicked()
                    || notificationConfig.isNotifyAbortTicked()) {
                buffer.append(prefix + " --mail-type=");
                ArrayList<String> conditions = new ArrayList<String>();
                if (notificationConfig.isNotifyStartTicked()) {
                    conditions.add("BEGIN");
                }
                if (notificationConfig.isNotifyEndTicked()) {
                    conditions.add("END");
                }
                if (notificationConfig.isNotifyAbortTicked()) {
                    conditions.add("FAIL");
                }
                buffer.append(String.join(",", conditions));
                buffer.append("\n");
                buffer.append(prefix + " --mail-user=" + notificationConfig.getNotificationMailingList() + "\n");
            }
        }
        String finalString = buffer.toString();
        return finalString;
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
        public String getDisplayName() {
            return "SLURM Slave";
        }

        @Override
        public boolean isInstantiable() {
            return true;
        }
    }
}
