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
    private NotificationConfig notificationConfig;
    //files user wishes to recover
    private String additionalFilesToRecover;

    public BatchBuilder(final String rawScript, final int nodes,
            final int tasks, final int cpusPerTask, final int walltime,
            final String queue, final String features, final boolean exclusive,
            final NotificationConfig notificationConfig,
            final String additionalFilesToRecover) {
        this.rawScript = rawScript;
        this.nodes = nodes;
        this.tasks = tasks;
        this.cpusPerTask = cpusPerTask;
        this.walltime = walltime;
        this.queue = queue;
        this.features = features;
        this.exclusive = exclusive;
        this.notificationConfig = notificationConfig;
        this.additionalFilesToRecover = additionalFilesToRecover;
    }

    public final String getRawScript() {
        return rawScript;
    }

    public final int getNodes() {
        return nodes;
    }

    public final int getTasks() {
        return tasks;
    }

    public final int getCpusPerTask() {
        return cpusPerTask;
    }

    public final int getWalltime() {
        return walltime;
    }

    public final String getQueue() {
        return queue;
    }

    public final String getFeatures() {
        return features;
    }

    public final boolean isExclusive() {
        return exclusive;
    }

    public final NotificationConfig getNotificationConfig() {
        return notificationConfig;
    }

    public final String getAdditionalFilesToRecover() {
        return additionalFilesToRecover;
    }
    /*
    public String getOutFileName() {
        return outFileName;
    }

    public String getErrFileName() {
        return errFileName;
    }
    */
    public final Computer getComputer(final FilePath workspace) throws AbortException {
        Computer computer = workspace.toComputer();
        if (computer == null) {
            throw new AbortException("Computer is null");
        }
        return computer;
    }

    public final Node getNode(final Computer computer) throws AbortException {
        Node node = computer.getNode();
        if (node == null) {
            throw new AbortException("Node is null");
        }
        return node;
    }

    protected String generateSystemScript(final String formattedBatchOptions,
            final String userScriptName, final String communicationFile) {
        /* Create script of format:
         *  #shebang
         *  #SLURM options
         *  #etc
         *  chmod 755 [userScriptName]
         *  ret=1
         *  {
         *      ./[userScriptName] &&
         *      ret=$? &&
         *      echo $ret > [communicationFile]
         *  } || {
         *      ret=$?
         *      echo $ret > [communicationFile]
         *  }
         *  times >> [communicationFile]
         *  exit $ret
         */
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

    protected String generateUserScript(final String script, final String prefix) {
        String filteredScript = filterScript(script, prefix);
        String userScript = "#!/bin/bash -xe\n"
                          + "#Script automatically generated by SLURM Plugin. User-entered content follows this comment. \n"
                          + filteredScript
                          + "#End of user-entered content.\n";
        return userScript;
    }

    protected final void writeScriptToFile(final String script, final String fileName) throws IOException {
        PrintWriter writer = new PrintWriter("work/userContent/" + fileName, "UTF-8");
        writer.print(script);
        writer.close();
    }

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

    public abstract void perform(Run<?, ?> run, FilePath workspace, Launcher launcher,
            TaskListener listener) throws InterruptedException, IOException;

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
        if (notificationConfig == null) { //initialise to prevent later NullPointerException
            notificationConfig = new NotificationConfig(false, false, false, "");
        }
        if (!notificationConfig.isValid()) {
            listener.error("Notifications selected but no email recipients entered");
            return false;
        }
        return true;
    }
}
