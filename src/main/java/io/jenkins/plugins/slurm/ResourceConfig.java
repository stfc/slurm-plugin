package io.jenkins.plugins.slurm;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.Serializable;

public class ResourceConfig implements Describable<ResourceConfig>, Serializable {
    private int maxNodesPerJob;
    private int cpusPerNode;
    private int maxCpuTimePerJob;
    private int availableMinutes;
    private int availableSeconds;
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

    //availableMinutes can be updated without updating availableSeconds
    //if config is saved without changing availableMinutes, don't want availableSeconds to change
    //probably overly pedantic
    public final void verifyAvailableSeconds() {
        if (availableSeconds < availableMinutes * 60 
                || availableSeconds > availableMinutes * 60 + 59) {
            //availableMinutes has changed for some reason, so update accordingly
            this.availableSeconds = availableMinutes * 60;
        }
    }

    public final void reduceAvailableSeconds(final int time) {
        verifyAvailableSeconds();
        this.availableSeconds -= time;
        this.availableMinutes = (int) Math.floor((double) availableSeconds / 60.); //always round down
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
                return FormValidation.warning("No queues entered - queues specified in jobs running on this node will not be checked for validity");
            } else {
                return FormValidation.ok();
            }
        }
    }
}
