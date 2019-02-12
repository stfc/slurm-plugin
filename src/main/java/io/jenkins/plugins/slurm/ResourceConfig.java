package io.jenkins.plugins.slurm;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Describable;
import hudson.util.FormValidation;
import java.io.Serializable;
import java.lang.Math;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundConstructor;

public class ResourceConfig implements Describable<ResourceConfig>,Serializable {
    private int maxNodesPerJob;
    private int cpusPerNode;
    private int maxCpuTimePerJob;
    private int availableMinutes;
    private int availableSeconds;
    private String availableQueues;

    @DataBoundConstructor
    public ResourceConfig(int maxNodesPerJob, int cpusPerNode, 
        int maxCpuTimePerJob, int availableMinutes, String availableQueues) {
        this.maxNodesPerJob=maxNodesPerJob;
        this.cpusPerNode=cpusPerNode;
        this.maxCpuTimePerJob=maxCpuTimePerJob;
        this.availableMinutes=availableMinutes;
        this.availableQueues=availableQueues;
        verifyAvailableSeconds();
    }
    
    public int getMaxNodesPerJob() {
        return maxNodesPerJob;
    }
    
    public int getCpusPerNode() {
        return cpusPerNode;
    }
    
    public int getMaxCpuTimePerJob() {
        return maxCpuTimePerJob;
    }
    
    public int getAvailableMinutes() {
        return availableMinutes;
    }
        
    public int getAvailableSeconds() {
        return availableSeconds;
    }
    
    public String getAvailableQueues() {
        return availableQueues;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }
    
    //availableMinutes can be updated without updating availableSeconds. If config is saved without changing availableMinutes, don't want availableSeconds to change.
    //probably overly pedantic
    public void verifyAvailableSeconds() {
        if (availableSeconds < availableMinutes*60 || availableSeconds > availableMinutes*60+59) {
            this.availableSeconds = availableMinutes*60; //availableMinutes has changed for some reason, so update accordingly
        }
    }
    
    public void reduceAvailableSeconds(int time) {
        verifyAvailableSeconds();
        this.availableSeconds -= time;
        this.availableMinutes = (int)Math.floor((double)availableSeconds/60.); //always round down
        if (availableSeconds < 0 || availableMinutes < 0) { //shouldn't go negative, but if they do, set both to 0
            this.availableSeconds = 0;
            this.availableMinutes = 0;
        }
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    public static class DescriptorImpl extends Descriptor<ResourceConfig> {
        @Override
        public String getDisplayName() {
            return "Resource usage config";
        }
        
        public FormValidation doCheckMaxNodesPerJob(@QueryParameter int value) {
            if (value <= 0)
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            else
                return FormValidation.ok();
        }
        
        public FormValidation doCheckCpusPerNode(@QueryParameter int value) {
            if (value <= 0)
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            else
                return FormValidation.ok();
        }
        
        public FormValidation doCheckMaxCpuTimePerJob(@QueryParameter int value) {
            if (value <= 0)
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            else
                return FormValidation.ok();
        }
        
        public FormValidation doCheckAvailableMinutes(@QueryParameter int value) {
            if (value <= 0)
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            else
                return FormValidation.ok();
        }
        
        public FormValidation doCheckAvailableQueues(@QueryParameter String value) {
            if (value.isEmpty())
                return FormValidation.warning("No queues entered - queues specified in jobs running on this node will not be checked for validity");
            else
                return FormValidation.ok();
        }
    }
}