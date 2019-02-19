package io.jenkins.plugins.slurm;

import com.michelin.cio.hudson.plugins.copytoslave.CopyToMasterNotifier;
import com.michelin.cio.hudson.plugins.copytoslave.CopyToSlaveBuildWrapper;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * @author Eli Chadwick
 */
public abstract class BatchBuilder extends Builder implements SimpleBuildStep {
    //raw script input
    private String rawScript;
    //number of nodes
    private int nodes;
    //number of tasks
    private int tasks;
    //CPUs per task
    private int cpusPerTask;
    //expected walltime of job
    private int walltime;
    //queue selection
    private String queue;
    //features/properties of node
    private String features;
    //run job in exclusive mode
    private boolean exclusive;
    //email notification configuration
    //private NotificationConfig notificationConfig;
    //files user wishes to recover
    private String additionalFilesToRecover;

    /**
     * @param rawScript                script to be run
     * @param nodes                    number of nodes to reserve
     * @param tasks                    number of tasks to run
     * @param cpusPerTask              number of CPUs to reserve per task
     * @param walltime                 walltime required for batch job
     * @param queue                    batch system queue to use
     * @param features                 specific node properties required
     * @param exclusive                require exclusive use of reserved nodes
     * @param additionalFilesToRecover extra files that are not recovered by default
     */
    public BatchBuilder(final String rawScript, final int nodes,
            final int tasks, final int cpusPerTask, final int walltime,
            final String queue, final String features, final boolean exclusive,
            //final NotificationConfig notificationConfig,
            final String additionalFilesToRecover) {
        this.rawScript = rawScript;
        this.nodes = nodes;
        this.tasks = tasks;
        this.cpusPerTask = cpusPerTask;
        this.walltime = walltime;
        this.queue = queue;
        this.features = features;
        this.exclusive = exclusive;
        //this.notificationConfig = notificationConfig;
        this.additionalFilesToRecover = additionalFilesToRecover;
    }

    /**
     * Get the raw script, as entered by user (i.e. without any filtering of invalid lines).
     */
    public final String getRawScript() {
        return rawScript;
    }

    /**
     * Get the number of nodes requested for the HPC job.
     */
    public final int getNodes() {
        return nodes;
    }

    /**
     * Get the number of tasks requested for the HPC job.
     */
    public final int getTasks() {
        return tasks;
    }

    /**
     * Get the number of nodes requested for the HPC job.
     */
    public final int getCpusPerTask() {
        return cpusPerTask;
    }

    /**
     * Get the walltime requested for the HPC job.
     */
    public final int getWalltime() {
        return walltime;
    }

    /**
     * Get the queue requested for the HPC job.
     */
    public final String getQueue() {
        return queue;
    }

    /**
     * Get the node features requested for the HPC job. May contain non-alphanumeric characters.
     */
    public final String getFeatures() {
        return features;
    }
    
    /**
     * Returns true if exclusive mode is requested for the HPC job.
     */
    public final boolean isExclusive() {
        return exclusive;
    }

    //public final NotificationConfig getNotificationConfig() {
    //    return notificationConfig;
    //}

    /**
     * Get the string of requested files to recover.
     */
    public final String getAdditionalFilesToRecover() {
        return additionalFilesToRecover;
    }

    /**
     * Get the computer on which this Jenkins job is running.
     *
     * @param workspace   as provided to {@link #perform(Run, FilePath, Launcher, TaskListener)}
     */
    public final Computer getComputer(final FilePath workspace) throws AbortException {
        Computer computer = workspace.toComputer();
        if (computer == null) {
            throw new AbortException("Computer is null");
        }
        return computer;
    }

    /**
     * Get the node on which this Jenkins job is running.
     *
     * @param computer   the computer on which the job is running.
     * Can be found with {@link #getComputer(FilePath)}).
     */
    public final Node getNode(final Computer computer) throws AbortException {
        Node node = computer.getNode();
        if (node == null) {
            throw new AbortException("Node is null");
        }
        return node;
    }

    /**
     * {@inheritDoc}
     * Run the HPC job.
     */
    public abstract void perform(Run<?, ?> run, FilePath workspace, Launcher launcher,
            TaskListener listener) throws InterruptedException, IOException;
    
    /**
     * Filter unexpected HPC options, whitespace and other invalid lines from 
     * the input script.
     *
     * @param script   script to be filtered
     * @param prefix   lines beginning with this string will be filtered out
     * @return a script with invalid lines filtered out
     */
    protected String filterScript(final String script, final String prefix) {
        Scanner scanner = new Scanner(script);
        StringBuffer buffer = new StringBuffer();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.indexOf(prefix) == 0) { //only check start of line
                continue;
            } else {
                buffer.append(line + "\n");
            }
        }
        String filteredScript = buffer.toString();
        return filteredScript;
    }

    /**
     * Check if the script contains any content that is not removed 
     * by filtering (i.e. if the output of {@link #filterScript(String,String)}
     * is not empty or entirely whitespace). Returning true does not necessarily
     * mean the script is valid bash or that the script will run.
     *
     * @param script   script to check
     * @param prefix   lines beginning with this string are invalid
     * @return true if filtered script contains non-whitespace content, 
     *         false otherwise (and false if the script is empty to begin with)
     */
    public final boolean isScriptValid(final String script, final String prefix) {
        if (script == null || script.isEmpty()) {
            return false;
        }
        String filteredScript = filterScript(script, prefix);
        if (filteredScript.trim().length() == 0) {
            return false;
        }
        return true;
    }

    /**
     * Check if configuration of resources requested is valid. Configuration is 
     * checked against resource limits imposed on the node, where applicable.
     * Where an input is optional and not entered by the user, set to default value.
     *
     * @param node       the node on which the Jenkins job is running
     * @param listener   as provided to {@link #perform(Run, FilePath, Launcher, TaskListener)}
     * @return true if all parts of configuration are valid, false if anything is invalid
     */
    public final boolean isConfigurationValid(final BatchSlave node, final TaskListener listener) {
        ResourceConfig config = node.getResourceConfig();
        if (config != null) {
            if (nodes < 1 || nodes > config.getMaxNodesPerJob()) {
                listener.error("'Nodes' selection is not within acceptable range (1-" + config.getMaxNodesPerJob() + ")");
                return false;
            }
            if (tasks < 1) { //TODO - implement max tasks?
                listener.error("'Number of tasks' selection is not within acceptable range (must be positive)");
                return false;
            }
            if (cpusPerTask < 1) { //TODO - implement max CPUs per task?
                listener.error("'CPUs per task' selection is not within acceptable range (must be positive)");
                return false;
            }
            if (tasks * cpusPerTask > nodes * config.getCpusPerNode()) {
                listener.error("Total CPUs requested (" + tasks + "*" + cpusPerTask + "=" + tasks * cpusPerTask + ") exceed CPUs available on " + nodes + " nodes.");
                return false;
            }
            if (walltime < 1 || walltime * tasks * cpusPerTask > config.getMaxCpuTimePerJob()) {
                listener.error("Walltime requested is too high (total CPU time requested may not exceed " + config.getMaxCpuTimePerJob() + " minutes)"); //TODO - fix this warning
                return false;
            } else if (config.getAvailableMinutes() - walltime * tasks * cpusPerTask < 0) {
                listener.error("System has insufficient available CPU time for this job. Please contact your Jenkins administrator.");
                return false;
            }
            if (queue == null || queue.trim().isEmpty()) {
                queue = ""; //TODO - set a default? Warn about this? Make function for subclasses for setting default queue or throwing error?
            } else if (config.getAvailableQueues() != null 
                    && !config.getAvailableQueues().isEmpty()) {
                if (!config.getAvailableQueues().contains(queue)) { //TODO - make availQueues an array? Make sure full queue name fits an entry
                    listener.error("Queue is not available or does not exist");
                    return false;
                }
            }
        }
        if (features == null || features.trim().isEmpty()) {
            features = "";
        }
        /*
        if (notificationConfig == null) { //initialise to prevent later NullPointerException
            notificationConfig = new NotificationConfig(false, false, false, "");
        }
        if (!notificationConfig.isValid()) {
            listener.error("Notifications selected but no email recipients entered");
            return false;
        }
        */
        return true;
    }
    
    /**
     * Create the 'system' script to send to HPC. The 'system' script runs the
     * 'user' script, recovers the exit code and records the time taken. 
     * The format is:
     * <p>
     * <pre>
     * {@code
     * #shebang
     * #formatted HPC options
     * chmod 755 [userScriptName]
     * ret=1
     * {
     *     ./[userScriptName] &&
     *     ret=$? &&
     *     echo $ret > [communicationFile]
     * } || {
     *     ret=$?
     *     echo $ret > [communicationFile]
     * }
     * times >> [communicationFile]
     * exit $ret
     * }
     * </pre>
     *
     * @param formattedBatchOptions a string of formatted HPC options
     *                              according to the relevant batch system
     * @param userScriptName        name of the 'user' script
     * @param communicationFile     file to write the exit code and time information
     *                              to for later recovery
     * @return A bash script with formatted HPC options at the top.
     */
    protected String generateSystemScript(final String formattedBatchOptions,
            final String userScriptName, final String communicationFile) {
        String systemScript = "#!/bin/bash -xe\n"
                           + "#Script automatically generated by SLURM Plugin\n"
                           + formattedBatchOptions
                           + "chmod 755 " + userScriptName + "\n"
                           + "ret=1\n"
                           + "{\n"
                           + "./" + userScriptName + " &&\n"
                           + "ret=$? &&\n"
                           + "echo $ret > " + communicationFile + "\n"
                           + "} || {\n"
                           + "ret=$?\n"
                           + "echo $ret > " + communicationFile + "\n"
                           + "}\n"
                           + "times >> " + communicationFile + "\n"
                           + "exit $ret\n"
                           + "#End of automatically generated script\n";
        return systemScript;
    }

    /**
     * Create the 'user' script to send to HPC. Contains the script content 
     * entered by the user, with HPC options and blank space filtered out.
     *
     * @param script   script to filter and wrap
     * @param prefix   the batch system prefix used to enter HPC options in the 
     *                 script, e.g. #SBATCH
     * @return A bash script with user-entered commands but no HPC options.
     */
    protected String generateUserScript(final String script, final String prefix) {
        String filteredScript = filterScript(script, prefix);
        String userScript = "#!/bin/bash -xe\n"
                          + "#Script automatically generated by SLURM Plugin. User-entered content follows this comment. \n"
                          + filteredScript
                          + "#End of user-entered content.\n";
        return userScript;
    }

    /**
     * Write a script to file.
     *
     * @param script     the script to write to file
     * @param fileName   name of the file to be written
     * @throws IOException
     */
    protected final void writeScriptToFile(final String script, final String fileName) throws IOException {
        PrintWriter writer = new PrintWriter("work/userContent/" + fileName, "UTF-8");
        writer.print(script);
        writer.close();
    }

    /**
     * Copy files from master to a remote workspace (on an SSH slave).
     *
     * @param fileNames   file names to copy
     * @param run         as provided to {@link #perform(Run, FilePath, Launcher, TaskListener)}
     * @param workspace   as above
     * @param launcher    as above
     * @param listener    as above
     * @throws InterruptedException
     * @throws IOException
     * @throws AbortException if run cannot be converted to {@link AbstractBuild}
     * @see com.michelin.cio.hudson.plugins.copytoslave.CopyToSlaveBuildWrapper
     */
    protected void sendFilesToRemote(final String fileNames, final Run<?, ?> run,
            final FilePath workspace, final Launcher launcher,
            final TaskListener listener)
            throws InterruptedException, IOException, AbortException {
        AbstractBuild abstractBuild = null;
        BuildListener buildListener = null;
        CopyToSlaveBuildWrapper copyToSlave =
                new CopyToSlaveBuildWrapper(fileNames, "", true, false, workspace.getRemote(), false);
        try {
            abstractBuild = AbstractBuild.class.cast(run);
            buildListener = BuildListener.class.cast(listener);
            copyToSlave.setUp(abstractBuild, launcher, buildListener);
        } catch (Exception e) {
            throw new AbortException(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Copy files from remote workspace (on an SSH slave) to master.
     *
     * @param filesToRecover        file names to copy
     * @param recoveryDestination   destination on master to copy files to
     * @param run                   as provided to {@link #perform(Run, FilePath, Launcher, TaskListener)}
     * @param workspace             as above
     * @param launcher              as above
     * @param listener              as above
     * @throws InterruptedException
     * @throws IOException
     * @throws AbortException if run cannot be converted to {@link AbstractBuild}
     * @see com.michelin.cio.hudson.plugins.copytoslave.CopyToMasterNotifier
     */
    protected void recoverFiles(final String filesToRecover,
            final String recoveryDestination, final Run<?, ?> run,
            final FilePath workspace, final Launcher launcher,
            final TaskListener listener)
            throws InterruptedException, IOException, AbortException {
        AbstractBuild abstractBuild = null;
        BuildListener buildListener = null;
        CopyToMasterNotifier copyFilesToMaster =
                new CopyToMasterNotifier(filesToRecover, "", true, recoveryDestination, true);
        ArtifactArchiver archiver = new ArtifactArchiver(filesToRecover);
        try {
            abstractBuild = AbstractBuild.class.cast(run);
            buildListener = BuildListener.class.cast(listener);
            copyFilesToMaster.perform(abstractBuild, launcher, buildListener);
            archiver.perform(run, workspace, launcher, listener);
        } catch (Exception e) {
            throw new AbortException(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
