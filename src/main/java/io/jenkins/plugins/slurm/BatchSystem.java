package io.jenkins.plugins.slurm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;

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

    protected final AbstractBuild<?, ?> getAbstractBuild() {
        return abstractBuild;
    }

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

    //submits the the job to the batch system
    public abstract int[] submitJob(String jobFileName, int walltime)
            throws InterruptedException, IOException;

    /*
     * cleans up the files created by the batch system
     * @throws InterruptedException
     */
    public abstract void cleanUpFiles() throws InterruptedException;

}
