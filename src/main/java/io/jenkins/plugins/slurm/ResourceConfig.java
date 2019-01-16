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
    private int cpusPerNode;
    private int maxWalltimePerJob;
    private int availableMinutes;
    private String availableQueues;

    @DataBoundConstructor
    public ResourceConfig(int maxNodesPerJob, int cpusPerNode, 
        int maxWalltimePerJob, int availableMinutes, String availableQueues) {
        this.maxNodesPerJob=maxNodesPerJob;
        this.cpusPerNode=cpusPerNode;
        this.maxWalltimePerJob=maxWalltimePerJob;
        this.availableMinutes=availableMinutes;
        this.availableQueues=availableQueues;
    }
    
    public int getMaxNodesPerJob() {
        return maxNodesPerJob;
    }
    
    public int getCpusPerNode() {
        return cpusPerNode;
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
        
        public FormValidation doCheckMaxWalltimePerJob(@QueryParameter int value) {
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
            if (value.length()==0)
                return FormValidation.warning("No queues entered - queues specified in jobs running on this node will not be checked for validity");
            else
                return FormValidation.ok();
        }
    }
}