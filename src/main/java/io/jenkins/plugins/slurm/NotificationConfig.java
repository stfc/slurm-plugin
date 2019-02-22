package io.jenkins.plugins.slurm;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Stores configuration for notifying users when their HPC job starts, ends or
 * is aborted.
 * @author Eli Chadwick
 */
public class NotificationConfig implements Describable<NotificationConfig> {
    /**
     * Notify user when the HPC job starts.
     */
    private boolean notifyStartTicked;

    /**
     * Notify user when the HPC job ends.
     */
    private boolean notifyEndTicked;

    /**
     * Notify user when the HPC job is aborted.
     */
    private boolean notifyAbortTicked;

    /**
     * Usernames or email addresses to send notifications to.
     */
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

    /**
     * Check validity of the configuration. Returns false if notifications are
     * requested for particular events but no username/email has been entered.
     */
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
