package io.jenkins.plugins.slurm;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Describable;
import hudson.util.FormValidation;
import java.io.Serializable;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundConstructor;

public class ResourceConfig implements Describable<ResourceConfig>,Serializable {
    private int maxNodesPerJob;
    private int maxProcessesPerNode;
    private int maxWalltimePerJob;
    private int availableMinutes;
    private String availableQueues;

    @DataBoundConstructor
    public ResourceConfig(int maxNodesPerJob, int maxProcessesPerNode, 
        int maxWalltimePerJob, int availableMinutes, String availableQueues) {
        this.maxNodesPerJob=maxNodesPerJob;
        this.maxProcessesPerNode=maxProcessesPerNode;
        this.maxWalltimePerJob=maxWalltimePerJob;
        this.availableMinutes=availableMinutes;
        this.availableQueues=availableQueues;
    }
    
    public int getMaxNodesPerJob() {
        return maxNodesPerJob;
    }
    
    public int getMaxProcessesPerNode() {
        return maxProcessesPerNode;
    }
    
    public int getMaxWalltimePerJob() {
        return maxWalltimePerJob;
    }
    
    public int getAvailableMinutes() {
        return availableMinutes;
    }
    
    public String getAvailableQueues() {
        return availableQueues;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
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
                return FormValidation.error("Positive integer required");
            else if (value >= 50) //TODO - add limits based on hardware/our own limits
                return FormValidation.error("Exceeds maximum nodes (50)");
            else
                return FormValidation.ok();
        }
        
        public FormValidation doCheckMaxProcessesPerNode(@QueryParameter int value) {
            if (value <= 0)
                return FormValidation.error("Positive integer required");
            else
                return FormValidation.ok();
        }
        
        public FormValidation doCheckMaxWalltimePerJob(@QueryParameter int value) {
            if (value <= 0)
                return FormValidation.error("Positive integer required");
            else
                return FormValidation.ok();
        }
        
        public FormValidation doCheckAvailableMinutes(@QueryParameter int value) {
            if (value <= 0)
                return FormValidation.error("Positive integer required");
            else
                return FormValidation.ok();
        }
        
        public FormValidation doCheckAvailableQueues(@QueryParameter String value) {
            if (value.length()==0)
                return FormValidation.warning("No queues entered - queues specified in jobs running on this node will not be checked for validity");
            else
                return FormValidation.ok();
        }
    }
}