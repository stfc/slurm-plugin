package io.jenkins.plugins.slurm;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.ArrayList;

public class SLURMBuilder extends BatchBuilder {

    @DataBoundConstructor
    public SLURMBuilder(final String rawScript, final int nodes,
            final int tasks, final int cpusPerTask, final int walltime,
            final String queue, final String features, final boolean exclusive,
            final NotificationConfig notificationConfig,
            final String additionalFilesToRecover) {
        super(rawScript, nodes, tasks, cpusPerTask, walltime, queue, features, 
                exclusive, notificationConfig, additionalFilesToRecover);
    }

    @Override
    public void perform(final Run<?, ?> run, final FilePath workspace,
            final Launcher launcher, final TaskListener listener)
            throws InterruptedException, IOException {

        //get computer and batch system set up and check all valid
        Computer computer = getComputer(workspace);
        Node node = getNode(computer);
        if (!(computer instanceof SLURMSlaveComputer)) {
            throw new AbortException("Computer is not a SLURM node computer");
        }
        SLURMSlave slurmNode = (SLURMSlave) node;
        if (!isScriptValid(getRawScript(), slurmNode.getPrefix())) {
            throw new AbortException("No valid script entered. Script is either empty or contains no valid content (batch options inside the script are not read).");
        }
        if (!isConfigurationValid(slurmNode, listener)) {
            throw new AbortException("Configuration is invalid");
        }

        String communicationFile = "comms.txt";
        BatchSystem batchSystem = new SLURMSystem(run, workspace, launcher,
                listener, communicationFile);
        String formattedBatchOptions = slurmNode.formatBatchOptions(
                getNodes(), getTasks(), getCpusPerTask(), getWalltime(),
                getQueue(), getFeatures(), isExclusive(), getNotificationConfig());

        //generate scripts, write to file and copy to remote
        String userScriptName = "_user_script.sh";
        String userScript = generateUserScript(getRawScript(), slurmNode.getPrefix());
        String systemScriptName = "_system_script.sh";
        String systemScript = generateSystemScript(formattedBatchOptions, 
                userScriptName, batchSystem.getCommunicationFile());
        listener.getLogger().print(systemScriptName + ":\n" + systemScript);
        listener.getLogger().print(userScriptName + ":\n" + userScript);

        writeScriptToFile(systemScript, systemScriptName);
        writeScriptToFile(userScript, userScriptName);
        listener.getLogger().println("Remote: " + workspace.getRemote());
        sendFilesToRemote(systemScriptName + "," + userScriptName, 
                run, workspace, launcher, listener);
        listener.getLogger().println("Scripts sent to remote");

        //run job and recover artifacts
        int[] output = batchSystem.submitJob(systemScriptName, getWalltime());
        int jobID = output[0];
        int exitCode = output[1];
        int computeTimeSec = output[2];
        slurmNode.reduceAvailableSeconds(computeTimeSec);
        if (exitCode != 0) {
            listener.error("SLURM job did not complete successfully. Files will be recovered before this job is aborted.");
        }
        listener.getLogger().println("Recovering files from remote");
        ArrayList<String> filesToRecover = new ArrayList<String>();
        if (jobID >= 0) {
            filesToRecover.add("slurm-" + jobID + "*");
        }
        /*
        if (outFileName != null && outFileName.length()>0) {
            filesToRecover.add(outFileName);
        }
        if (errFileName != null && errFileName.length()>0) {
            filesToRecover.add(errFileName);
        }
        */
        filesToRecover.add("_sbatch_output.txt"); //TODO - make sbatchOutput a property of SLURMSystem
        filesToRecover.add(batchSystem.getCommunicationFile()); //TODO - remove this, purely for debug
        String filesToRecoverString;
        if (getAdditionalFilesToRecover() != null && !getAdditionalFilesToRecover().isEmpty()) {
            filesToRecoverString = getAdditionalFilesToRecover() + "," + String.join(",", filesToRecover);
        } else {
            filesToRecoverString = String.join(",", filesToRecover);
        }
        String recoveryDestination = run.getRootDir().getAbsolutePath();
        listener.getLogger().println("Recovery destination: " + recoveryDestination);
        recoverFiles(filesToRecoverString, recoveryDestination, run, workspace,
                launcher, listener);
        batchSystem.cleanUpFiles();
        if (exitCode != 0) {
            throw new AbortException("SLURM job did not complete successfully");
        }
    }

    @Override
    public final DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Symbol("submitSLURMJob")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public final String getDisplayName() {
            return "Run SLURM script";
        }

        @Override
        public final boolean isApplicable(final Class<? extends AbstractProject> type) {
            return true;
        }

        //public FormValidation doCheckField(@QueryParameter String value) {
         //   if (value.isGood()) return FormValidation.ok();
          //  else return FormValidation.error(Messaaaaaaages.xx.y.z());
        //}

        public final FormValidation doCheckNodes(@QueryParameter final int value) {
            if (value >= 1) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            }
        }

        public final FormValidation doCheckTasks(@QueryParameter final int value) {
            if (value >= 1) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            }
        }

        public final FormValidation doCheckCpusPerTask(@QueryParameter final int value) {
            if (value >= 1) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            }
        }

        public final FormValidation doCheckWalltime(@QueryParameter final int value) {
            if (value >= 1) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(Messages.errors_NotPositiveInteger());
            }
        }
    }
}
