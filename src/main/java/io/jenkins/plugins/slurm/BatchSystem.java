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

    //TODO - move abstractBuild and buildListener to SLURMSystem?
    /**
     * The run property cast to an {@link AbstractBuild}.
     */
    private final AbstractBuild<?, ?> abstractBuild; //required for Shell calls
    
    /**
     * The listener property cast to a {@link BuildListener}.
     */
    private final BuildListener buildListener; //required for Shell calls
    
    /**
     * The build this is part of.
     */
    private final Run<?, ?> run;
    
    /**
     * The workspace of the build.
     */
    private final FilePath workspace;
    
    /**
     * The launcher of the build.
     */
    private final Launcher launcher;
    
    /**
     * Get the listener of the build - a place to send output
     */
    private final TaskListener listener;
    
    /**
     * The name of the file used for recovering job information
     */
    private final String communicationFile;

    /**
     * The working directory on the master.
     */
    private final String masterWorkingDirectory;

    /**
     * The working directory on the remote.
     */
    private final String remoteWorkingDirectory;

    /**
     * Sole constructor.
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

    protected final Run<?, ?> getRun() {
        return run;
    }

    protected final FilePath getWorkspace() {
        return workspace;
    }

    protected final Launcher getLauncher() {
        return launcher;
    }

    protected final TaskListener getListener() {
        return listener;
    }


    public final String getCommunicationFile() {
        return communicationFile;
    }
    
    public final String getMasterWorkingDirectory() {
        return masterWorkingDirectory;
    }

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
