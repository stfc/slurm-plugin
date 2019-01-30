package io.jenkins.plugins.slurm;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Describable;
import java.util.ArrayList;
import org.kohsuke.stapler.DataBoundConstructor;

public class NotificationConfig implements Describable<NotificationConfig> {
    //events to notify on
    private boolean notifyStartTicked;
    private boolean notifyEndTicked;
    private boolean notifyAbortTicked;
    //mailing list for notification  
    private String notificationMailingList;
    
    @DataBoundConstructor
    public NotificationConfig(boolean notifyStartTicked, boolean notifyEndTicked,
            boolean notifyAbortTicked, String notificationMailingList) {
        this.notifyStartTicked=notifyStartTicked;
        this.notifyEndTicked=notifyEndTicked;
        this.notifyAbortTicked=notifyAbortTicked;
        this.notificationMailingList=notificationMailingList;
    }
    
    public boolean isNotifyStartTicked() {
        return notifyStartTicked;
    }
    
    public boolean isNotifyEndTicked() {
        return notifyEndTicked;
    }
    
    public boolean isNotifyAbortTicked() {
        return notifyAbortTicked;
    }
    
    public String getNotificationMailingList() {
        return notificationMailingList;
    }
    
    public boolean isValid() {
        if (notifyStartTicked || notifyEndTicked || notifyAbortTicked) {
            if (notificationMailingList==null || notificationMailingList.isEmpty()){
                return false; //notifications selected but no mailing list entered
            }
        }
        return true;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }
    
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    public static class DescriptorImpl extends Descriptor<NotificationConfig> {
        @Override
        public String getDisplayName() {
            return "Email notification config";
        }
    }
}