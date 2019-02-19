package io.jenkins.plugins.slurm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;

/**
 * @author Eli Chadwick
 */

public abstract class BatchSystem {

    //these required for Shell
    private final AbstractBuild<?, ?> abstractBuild;
    private final BuildListener buildListener;
    private final Run<?, ?> run;
    private final FilePath workspace;
    private final Launcher launcher;
    private final TaskListener listener;
    // every file created by the batch system should have this name
    private final String communicationFile;
    private final String masterWorkingDirectory;
    private final String remoteWorkingDirectory;

    /**
     * @param run                 as provided to {@link BatchBuilder#perform(Run, FilePath, Launcher, TaskListener)}
     * @param workspace           as above
     * @param launcher            as above
     * @param listener            as above
     * @param communicationFile   file to write the exit code and time information
     *                            to for later recovery
     */
    public BatchSystem(final Run<?, ?> run, final FilePath workspace,
            final Launcher launcher, final TaskListener listener,
            final String communicationFile) {
        this.run = run;
        this.workspace = workspace;
        this.launcher = launcher;
        this.listener = listener;
        if (run instanceof AbstractBuild) {
            this.abstractBuild = AbstractBuild.class.cast(run);
        } else {
            this.abstractBuild = null;
        }
        if (listener instanceof BuildListener) {
            this.buildListener = BuildListener.class.cast(listener);
        } else {
            this.buildListener = null;
        }
        this.communicationFile = communicationFile;
        this.masterWorkingDirectory = run.getRootDir().getAbsolutePath();
        this.remoteWorkingDirectory = workspace.getRemote();
    }

    /**
     * Get the build, cast to an {@link AbstractBuild}.
     * @return The run property cast to an {@link AbstractBuild}, null if this is not possible
     */
    protected final AbstractBuild<?, ?> getAbstractBuild() {
        return abstractBuild;
    }

    /**
     * Get the build's listener, cast to a {@link BuildListener}.
     * @return the listener property cast to a {@link BuildListener}, null if this is not possible
     */
    protected final BuildListener getBuildListener() {
        return buildListener;
    }

    /**
     * Get the build this is part of.
     */
    protected final Run<?, ?> getRun() {
        return run;
    }

    /**
     * Get the workspace of the build.
     */
    protected final FilePath getWorkspace() {
        return workspace;
    }

    /**
     * Get the launcher of the build.
     */
    protected final Launcher getLauncher() {
        return launcher;
    }

    /**
     * Get the listener of the build - a place to send output
     */
    protected final TaskListener getListener() {
        return listener;
    }

    /**
     * Get the name of the file used for recovering job information
     */
    public final String getCommunicationFile() {
        return communicationFile;
    }

    /**
     * Get the working directory on the master.
     */
    public final String getMasterWorkingDirectory() {
        return masterWorkingDirectory;
    }

    /**
     * Get the working directory on the remote.
     */
    public final String getRemoteWorkingDirectory() {
        return remoteWorkingDirectory;
    }

    /**
     * Submit a job to the batch system.
     *
     * @param jobFileName   Name of script to submit (must already exist on the remote)
     * @param walltime      Walltime requested for job (not passed to HPC 
     *                      submission command)
     * @return Array containing the job ID, exit code and time taken.
     * @throws InterruptedException
     * @throws IOException
     */
    public abstract int[] submitJob(String jobFileName, int walltime)
            throws InterruptedException, IOException;

    /**
     * Clean up files created by the Jenkins job
     * @throws InterruptedException
     */
    public abstract void cleanUpFiles() throws InterruptedException;

}
