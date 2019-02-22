package io.jenkins.plugins.slurm;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.Serializable;

/**
 * Stores limits on HPC resource usage for a BatchSlave.
 * @author Eli Chadwick
 */
public class ResourceConfig implements Describable<ResourceConfig>, Serializable {
    /**
     * The maximum number of nodes a user can request for a job.
     */
    private int maxNodesPerJob;

    /**
     * The number of CPUs per node on the HPC system.
     */
    private int cpusPerNode;

    /**
     * The maximum amount of CPU time a user can request for a job.
     * CPU time requested = tasks * CPUs per task * walltime requested.
     */
    private int maxCpuTimePerJob;

    /**
     * The number of CPU minutes available on the HPC system.
     */
    private int availableMinutes;

    /**
     * The number of CPU seconds available on the HPC system.
     */
    private int availableSeconds;

    /**
     * Queues that users are permitted to use. If left empty, user-entered
     * queues will not be checked for validity (though queues that do not exist
     * on the HPC system will throw errors there which will be recovered).
     */
    private String availableQueues;

    @DataBoundConstructor
    public ResourceConfig(final int maxNodesPerJob, final int cpusPerNode,
            final int maxCpuTimePerJob, final int availableMinutes,
            final String availableQueues) {
        this.maxNodesPerJob = maxNodesPerJob;
        this.cpusPerNode = cpusPerNode;
        this.maxCpuTimePerJob = maxCpuTimePerJob;
        this.availableMinutes = availableMinutes;
        this.availableQueues = availableQueues;
        verifyAvailableSeconds();
    }

    public final int getMaxNodesPerJob() {
        return maxNodesPerJob;
    }

    public final int getCpusPerNode() {
        return cpusPerNode;
    }

    public final int getMaxCpuTimePerJob() {
        return maxCpuTimePerJob;
    }

    public final int getAvailableMinutes() {
        return availableMinutes;
    }

    public final int getAvailableSeconds() {
        return availableSeconds;
    }

    public final String getAvailableQueues() {
        return availableQueues;
    }

    @Override
    public final DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Check that availableMinutes and availableSeconds are consistent.
     * availableSeconds can be between 0-59 higher than availableMinutes*60.
     * If it is outside this range, it is reset to availableMinutes*60.
     * Function exists because availableMinutes can be updated by user without
     * changing availableSeconds directly.
     */
    public final void verifyAvailableSeconds() {
        if (availableSeconds < availableMinutes * 60
                || availableSeconds > availableMinutes * 60 + 59) {
            //availableMinutes has changed for some reason, so update accordingly
            this.availableSeconds = availableMinutes * 60;
        }
    }

    /**
     * Reduce the time available on the HPC system.
     *
     * @param time   the number of seconds to reduce the available time by
     */
    public final void reduceAvailableSeconds(final int time) {
        verifyAvailableSeconds();
        this.availableSeconds -= time;
        this.availableMinutes = (int) Math.floor((double) availableSeconds / 60.); //round down
        if (availableSeconds < 0 || availableMinutes < 0) { //precautionary, shouldn't go negative
            this.availableSeconds = 0;
            this.availableMinutes = 0;
        }
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends Descriptor<ResourceConfig> {
        @Override
        public final String getDisplayName() {
            return "Resource usage config";
        }

        public final FormValidation doCheckMaxNodesPerJob(@QueryParameter final int value) {
            if (value <= 0) {
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            } else {
                return FormValidation.ok();
            }
        }

        public final FormValidation doCheckCpusPerNode(@QueryParameter final int value) {
            if (value <= 0) {
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            } else {
                return FormValidation.ok();
            }
        }

        public final FormValidation doCheckMaxCpuTimePerJob(@QueryParameter final int value) {
            if (value <= 0) {
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            } else {
                return FormValidation.ok();
            }
        }

        public final FormValidation doCheckAvailableMinutes(@QueryParameter final int value) {
            if (value <= 0) {
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            } else {
                return FormValidation.ok();
            }
        }

        public final FormValidation doCheckAvailableQueues(@QueryParameter final String value) {
            if (value.isEmpty()) {
                return FormValidation.warning("No queues entered. Queues specified in jobs running on this node will not be checked for validity");
            } else {
                return FormValidation.ok();
            }
        }
    }
}
