package io.jenkins.plugins.slurm;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Eli Chadwick
 */
public class NotificationConfig implements Describable<NotificationConfig> {
    //events to notify on
    private boolean notifyStartTicked;
    private boolean notifyEndTicked;
    private boolean notifyAbortTicked;
    //mailing list for notification
    private String notificationMailingList;

    @DataBoundConstructor
    public NotificationConfig(final boolean notifyStartTicked, 
            final boolean notifyEndTicked, final boolean notifyAbortTicked, 
            final String notificationMailingList) {
        this.notifyStartTicked = notifyStartTicked;
        this.notifyEndTicked = notifyEndTicked;
        this.notifyAbortTicked = notifyAbortTicked;
        this.notificationMailingList = notificationMailingList;
    }

    public final boolean isNotifyStartTicked() {
        return notifyStartTicked;
    }

    public final boolean isNotifyEndTicked() {
        return notifyEndTicked;
    }

    public final boolean isNotifyAbortTicked() {
        return notifyAbortTicked;
    }

    public final String getNotificationMailingList() {
        return notificationMailingList;
    }

    public final boolean isValid() {
        if (notifyStartTicked || notifyEndTicked || notifyAbortTicked) {
            if (notificationMailingList == null || notificationMailingList.trim().isEmpty()) {
                return false; //notifications selected but no mailing list entered
            }
        }
        return true;
    }

    @Override
    public final DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends Descriptor<NotificationConfig> {
        @Override
        public final String getDisplayName() {
            return "Email notification config";
        }
    }
}
