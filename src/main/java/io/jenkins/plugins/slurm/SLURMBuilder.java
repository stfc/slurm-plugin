package io.jenkins.plugins.slurm;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
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

/**
 * Provides an interface within the Jenkins job configuration. Stores user input,
 * and contains methods for validating said input and processing it into batch
 * scripts to be sent to the relevant HPC system. Calls SLURMSystem
 * methods to submit said scripts as a job.
 *
 * @author Eli Chadwick
 */
public class SLURMBuilder extends BatchBuilder {

    @DataBoundConstructor
    public SLURMBuilder(final String rawScript, final int nodes,
            final int tasks, final int cpusPerTask, final int walltime,
            final String queue, final String features, final boolean exclusive,
            //final NotificationConfig notificationConfig,
            final String additionalFilesToRecover) {
        super(rawScript, nodes, tasks, cpusPerTask, walltime, queue, features,
                exclusive, /*notificationConfig,*/ additionalFilesToRecover);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void perform(final Run<?, ?> run, final FilePath workspace,
            final Launcher launcher, final TaskListener listener)
            throws InterruptedException, IOException {

        AbstractBuild abstractBuild = AbstractBuild.class.cast(run);
        EnvVars env = abstractBuild.getEnvironment(listener);
        env.overrideAll(abstractBuild.getBuildVariables());

        //check job is running on a SLURMSlave (otherwise it won't work)
        Computer computer = getComputer(workspace);
        Node node = getNode(computer);
        if (!(computer instanceof SLURMSlaveComputer)) {
            throw new AbortException("Not running on a SLURM agent");
        }
        SLURMSlave slurmNode = (SLURMSlave) node;

        //verify script and resource configuration
        if (!isScriptValid(getRawScript(), slurmNode.getPrefix())) {
            throw new AbortException("No valid script entered. Script is either empty or contains no valid content (batch options inside the script are not read).");
        }
        if (!isConfigurationValid(slurmNode, listener)) {
            throw new AbortException("Configuration is invalid");
        }

        //set up SLURM system
        String communicationFile = "comms.txt";
        BatchSystem batchSystem = new SLURMSystem(run, workspace, launcher,
                listener, communicationFile);
        listener.getLogger().println("Remote: " + workspace.getRemote());

        //generate scripts and write to remote workspace
        //format options
        String formattedBatchOptions = slurmNode.formatBatchOptions(
                getNodes(), getTasks(), getCpusPerTask(), getWalltime(),
                env.expand(getQueue()), env.expand(getFeatures()), isExclusive()); //, getNotificationConfig()
        //user script
        String userScriptName = "_user_script.sh";
        String userScript = generateUserScript(getRawScript(), slurmNode.getPrefix());
        listener.getLogger().print(userScriptName + ":\n" + userScript);
        FilePath userScriptPath = new FilePath(workspace, userScriptName);
        userScriptPath.write(userScript, "utf-8");
        //system script
        String systemScriptName = "_system_script.sh";
        String systemScript = generateSystemScript(formattedBatchOptions,
                userScriptName, batchSystem.getCommunicationFile());
        listener.getLogger().print(systemScriptName + ":\n" + systemScript);
        FilePath systemScriptPath = new FilePath(workspace, systemScriptName);
        systemScriptPath.write(systemScript, "utf-8");

        listener.getLogger().println("Scripts sent to remote");

        //run job and recover artifacts
        int cpuTime = getTasks() * getCpusPerTask() * getWalltime();
        int[] output = batchSystem.submitJob(systemScriptName, cpuTime);
        int jobID = output[0];
        int exitCode = output[1];
        int computeTimeSec = output[2];

        //account for time used - node handles if there are no limits on time
        slurmNode.reduceAvailableSeconds(computeTimeSec);

        //warn if job failed
        if (exitCode != 0) {
            listener.error("SLURM job did not complete successfully. Files will be recovered before this job is aborted.");
        }

        //recover files from remote - useful to do before killing job if it's failed
        listener.getLogger().println("Recovering files from remote");
        ArrayList<String> filesToRecover = new ArrayList<String>();
        if (jobID >= 0) { //job ran successfully
            filesToRecover.add("slurm-" + jobID + "*");
        }
        filesToRecover.add("_sbatch_output.txt"); //TODO - make sbatchOutput property of SLURMSystem
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

        //clean up - important that this is done before killing the job if it's failed
        batchSystem.cleanUpFiles();

        //kill the job if it failed
        //does not prevent any code running here but changes Jenkins job status
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
