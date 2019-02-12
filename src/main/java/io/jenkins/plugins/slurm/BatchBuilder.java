package io.jenkins.plugins.slurm;

import com.michelin.cio.hudson.plugins.copytoslave.CopyToSlaveBuildWrapper;
import com.michelin.cio.hudson.plugins.copytoslave.CopyToMasterNotifier;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.StringBuffer;
import java.util.Scanner;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundConstructor;

public abstract class BatchBuilder extends Builder implements SimpleBuildStep {
    //raw script input
    protected String rawScript;
    //number of nodes
    protected int nodes;
    //number of tasks
    protected int tasks;
    //CPUs per task
    protected int cpusPerTask;
    //expected walltime of job
    protected int walltime;
    //queue selection
    protected String queue;
    //features/properties of node
    protected String features;
    //run job in exclusive mode
    protected boolean exclusive;
    //email notification configuration
    protected NotificationConfig notificationConfig;
    //files user wishes to recover
    protected String additionalFilesToRecover;
    //stdout file name
    //protected String outFileName;
    //stderr file name
    //protected String errFileName;
    // file name for the communication between master and slave
    //private static final String COMMUNICATION_FILE = "output";
    // name of the file where the running job output is saved - potentially not necessary any more? idk must look at
    //private static final String PROGRESS_FILE = "jobProgress";
    //private String masterWorkingDirectory;
    //private String slaveWorkingDirectory;
    
    public BatchBuilder(String rawScript, int nodes, int tasks, int cpusPerTask,
            int walltime, String queue, String features, boolean exclusive, 
            NotificationConfig notificationConfig, String additionalFilesToRecover) {
            //String outFileName, String errFileName) {
        this.rawScript=rawScript;
        this.nodes=nodes;
        this.tasks=tasks;
        this.cpusPerTask=cpusPerTask;
        this.walltime=walltime;
        this.queue=queue;
        this.features=features;
        this.exclusive=exclusive;
        this.notificationConfig=notificationConfig;
        this.additionalFilesToRecover=additionalFilesToRecover;
        //this.outFileName=outFileName;
        //this.errFileName=errFileName;
    }
    
    public String getRawScript() {
        return rawScript;
    }
    
    public int getNodes() {
        return nodes;
    }
    
    public int getTasks() {
        return tasks;
    }
    
    public int getCpusPerTask() {
        return cpusPerTask;
    }
    
    public int getWalltime() {
        return walltime;
    }
    
    public String getQueue() {
        return queue;
    }
    
    public String getFeatures() {
        return features;
    }
    
    public boolean isExclusive() {
        return exclusive;
    }
    
    public NotificationConfig getNotificationConfig() {
        return notificationConfig;
    }
    
    public String getAdditionalFilesToRecover() {
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
    public Computer getComputer(FilePath workspace) throws AbortException {
        Computer computer = workspace.toComputer();
        if (computer==null) {
            throw new AbortException("Computer is null");
        }
        return computer;
    }
    
    public Node getNode(Computer computer) throws AbortException {
        Node node = computer.getNode();
        if (node==null) {
            throw new AbortException("Node is null");
        }
        return node;
    }
    
    protected String generateSystemScript(String formattedBatchOptions, String userScriptName, String communicationFile) {
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
                           + "chmod 755 "+userScriptName+"\n"
                           + "ret=1\n"
                           + "{\n"
                           + "./"+userScriptName+" &&\n"
                           + "ret=$? &&\n"
                           + "echo $ret > "+communicationFile+"\n"
                           + "} || {\n"
                           + "ret=$?\n"
                           + "echo $ret > "+communicationFile+"\n"
                           + "}\n"
                           + "times >> "+communicationFile+"\n"
                           + "exit $ret\n"
                           + "#End of automatically generated script\n";
        return systemScript;
    }
    
    protected String generateUserScript(String rawScript, String prefix) {
        String filteredScript = filterScript(rawScript, prefix);
        String userScript = "#!/bin/bash -xe\n"
                          + "#Script automatically generated by SLURM Plugin. User-entered content follows this comment. \n"
                          + filteredScript
                          + "#End of user-entered content.\n";
        return userScript;
    }
    
    protected void writeScriptToFile(String script, String fileName) throws IOException {
        PrintWriter writer = new PrintWriter("work/userContent/"+fileName,"UTF-8"); //TODO - put in specific folder?
        writer.print(script);
        writer.close();
    }
    
    protected void sendFilesToRemote(String fileNames, Run<?,?> run, FilePath workspace, 
            Launcher launcher, TaskListener listener) throws InterruptedException, IOException, AbortException {
        AbstractBuild build = null;
        BuildListener buildListener = null;
        CopyToSlaveBuildWrapper copyToSlave =
                new CopyToSlaveBuildWrapper(fileNames,"",true,false,workspace.getRemote(),false);
        try {
            build=AbstractBuild.class.cast(run);
            buildListener=BuildListener.class.cast(listener);
            copyToSlave.setUp(build,launcher,buildListener);
        } catch (Exception e) {
            throw new AbortException(e.getClass().getSimpleName()+": "+e.getMessage());
        }
    }
    
    protected void recoverFiles(String filesToRecover, String recoveryDestination, 
            Run<?,?> run, FilePath workspace, Launcher launcher, TaskListener listener) 
            throws InterruptedException, IOException, AbortException {
        AbstractBuild build = null;
        BuildListener buildListener = null;
        CopyToMasterNotifier copyFilesToMaster =
                new CopyToMasterNotifier(filesToRecover,"",true,recoveryDestination,true);
        ArtifactArchiver archiver = new ArtifactArchiver(filesToRecover);
        try {
            build=AbstractBuild.class.cast(run);
            buildListener=BuildListener.class.cast(listener);
            copyFilesToMaster.perform(build,launcher,buildListener);
            archiver.perform(run,workspace,launcher,listener);
        } catch (Exception e) {
            throw new AbortException(e.getClass().getSimpleName()+": "+e.getMessage());
        }
    }
    
    public abstract void perform(Run<?,?> run, FilePath workspace, Launcher launcher,
            TaskListener listener) throws InterruptedException,IOException;
    
    protected String filterScript(String script, String prefix) {
        Scanner scanner = new Scanner(script);
        StringBuffer buffer = new StringBuffer();
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.indexOf(prefix)==0) { //only check start of line
                continue;
            }
            else {
                buffer.append(line+"\n");
            }
        }
        String filteredScript=buffer.toString();
        return filteredScript;
    }
    
    public boolean isScriptValid(String script, String prefix) {
        if (script==null || script.isEmpty()) {
            return false;
        }
        String filteredScript = filterScript(script,prefix);
        if (filteredScript.trim().length()==0) {
            return false;
        }
        return true;
    }
    
    public boolean isConfigurationValid(BatchSlave node, TaskListener listener) {
        ResourceConfig config=node.getResourceConfig();
        if (config!=null) {
            if (nodes<1 || nodes>config.getMaxNodesPerJob()) {
                listener.error("'Nodes' selection is not within acceptable range (1-"+config.getMaxNodesPerJob()+")");
                return false;
            }
            if (tasks<1) { //TODO - implement max tasks?
                listener.error("'Number of tasks' selection is not within acceptable range (must be positive)");
                return false;
            }
            if (cpusPerTask<1) { //TODO - implement max CPUs per task?
                listener.error("'CPUs per task' selection is not within acceptable range (must be positive)");
                return false;
            }
            if (tasks*cpusPerTask > nodes*config.getCpusPerNode()) {
                listener.error("Total CPUs requested ("+tasks+"*"+cpusPerTask+"="+tasks*cpusPerTask+") exceed CPUs available on "+nodes+" nodes.");
                return false;
            }
            if (walltime<1 || walltime*tasks*cpusPerTask>config.getMaxCpuTimePerJob()) {
                listener.error("Walltime requested is too high (total CPU time requested may not exceed "+config.getMaxCpuTimePerJob()+" minutes)"); //TODO - fix this warning
                return false;
            }
            else if (config.getAvailableMinutes()-walltime*tasks*cpusPerTask<0) {
                listener.error("System has insufficient available CPU time for this job. Please contact your Jenkins administrator.");
                return false;
            }
            if (queue==null || queue.trim().isEmpty()) {
                queue=""; //TODO - set a default? Warn about this? Make function for subclasses for setting default queue or throwing error?
            }
            else if (config.getAvailableQueues()!=null && config.getAvailableQueues().length()>0){
                if (!config.getAvailableQueues().contains(queue)) { //TODO - make availQueues an array? Make sure full queue name fits an entry
                    listener.error("Queue is not available or does not exist");
                    return false;
                }
            }
        }
        if (features==null || features.trim().isEmpty()) {
            features="";
        }
        if (notificationConfig==null) {
            notificationConfig = new NotificationConfig(false,false,false,""); //initialise to prevent later NullPointerException
        }
        if (!notificationConfig.isValid()) {
            listener.error("Notifications selected but no email recipients entered");
            return false;
        }
        //TODO - verify out and err filenames? Including %j etc?
        return true;
    }
    
}