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
    //processes per node
    protected int processesPerNode;
    //expected walltime of job
    protected int walltime;
    //queue selection
    protected String queue;
    //run job in exclusive mode
    protected boolean exclusive;
    //email notification configuration
    protected NotificationConfig notificationConfig;
    //stdout file name
    protected String outFileName;
    //stderr file name
    protected String errFileName;
    // file name for the communication between master and slave
    //private static final String COMMUNICATION_FILE = "output";
    // name of the file where the running job output is saved - potentially not necessary any more? idk must look at
    //private static final String PROGRESS_FILE = "jobProgress";
    //private String masterWorkingDirectory;
    //private String slaveWorkingDirectory;
    
    public BatchBuilder(String rawScript, int nodes, int processesPerNode,
            int walltime, String queue, boolean exclusive, 
            NotificationConfig notificationConfig,
            String outFileName, String errFileName) {
        this.rawScript=rawScript;
        this.nodes=nodes;
        this.processesPerNode=processesPerNode;
        this.walltime=walltime;
        this.queue=queue;
        this.exclusive=exclusive;
        this.notificationConfig=notificationConfig;
        this.outFileName=outFileName;
        this.errFileName=errFileName;
    }
    
    public String getRawScript() {
        return rawScript;
    }
    
    public int getNodes() {
        return nodes;
    }
    
    public int getProcessesPerNode() {
        return processesPerNode;
    }
    
    public int getWalltime() {
        return walltime;
    }
    
    public String getQueue() {
        return queue;
    }
    
    public boolean isExclusive() {
        return exclusive;
    }
    
    public NotificationConfig getNotificationConfig() {
        return notificationConfig;
    }
    
    public String getOutFileName() {
        return outFileName;
    }
    
    public String getErrFileName() {
        return errFileName;
    }
    
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
    
    public String generateScript(String rawScript, BatchSlave node, TaskListener listener) throws AbortException {
        String prefix = node.getPrefix();
        String filteredScript = filterScript(rawScript,prefix);
        if (filteredScript.trim().length()==0) {
            throw new AbortException("Script has no valid content");
        }
        if (!isConfigurationValid(node,listener)) {
            throw new AbortException("Configuration is invalid");
        }
        String formattedBatchOptions = node.formatBatchOptions(
                nodes, processesPerNode,walltime, queue, exclusive, 
                notificationConfig, outFileName, errFileName);
        String finalScript = "#!/bin/bash +x\n"
                            + "Script automatically generated by SLURM Plugin\n"
                            + formattedBatchOptions
                            + filteredScript
                            + "echo \"output\" > " + outFileName + "\n"
                            + "echo \"error\" > " + errFileName + "\n"
                            + "#End of automatically generated script\n";
        return finalScript;
    }
    
    public void writeScriptToFile(String script, String fileName) throws IOException {
        PrintWriter writer = new PrintWriter("work/userContent/"+fileName,"UTF-8"); //TODO - put in specific folder?
        writer.print(script);
        writer.close();
    }
    
    public void sendFileToRemote(String fileName, Run<?,?> run, FilePath workspace, 
            Launcher launcher, TaskListener listener) throws InterruptedException, IOException, AbortException {
        AbstractBuild build = null;
        BuildListener buildListener = null;
        CopyToSlaveBuildWrapper copyToSlave =
                new CopyToSlaveBuildWrapper(fileName,"",true,false,workspace.getRemote(),false);
        try {
            build=AbstractBuild.class.cast(run);
            buildListener=BuildListener.class.cast(listener);
            copyToSlave.setUp(build,launcher,buildListener);
        } catch (Exception e) {
            throw new AbortException(e.getClass().getSimpleName()+": "+e.getMessage());
        }
    }
    
    public void recoverFiles(String filesToRecover, String recoveryDestination, 
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
            TaskListener listener) throws InterruptedException,IOException; // {
        /*listener.getLogger().println("Testing 123");
        Computer computer=workspace.toComputer();
        if (computer==null) {
            throw new AbortException("Computer is null");
        }
        Node node=computer.getNode(); //should not be null?
        if (node==null) {
            throw new AbortException("Node is null");
        }
        try { //TODO - remove this later, only a debug thing
            listener.getLogger().println(computer.getNode().getDisplayName());
        } catch (NullPointerException e) {
            listener.getLogger().println("WARNING: No display name found for node");
        }
        if (!(computer instanceof SLURMSlaveComputer)) { //TODO - generalise to BatchSlaveComputer
            throw new AbortException("Computer is not a SLURM Slave");
            //return false;
        }
        SLURMSlave slave = (SLURMSlave)node; //TODO - generalise by node type
        String prefix=slave.getPrefix(); 
        String filteredScript=filterScript(rawScript,prefix);
        if (filteredScript.trim().length()==0) {
            throw new AbortException("Script has no valid content");
            //return false;
        }
        if (!isConfigurationValid(slave,listener)) {
            throw new AbortException("Configuration is invalid");
        }
        //generate batch script
        String formattedBatchOptions = slave.formatBatchOptions( //TODO - any way to get this call to the SLURMSlave class rather than going through the node instance w/o losing generality?
            nodes, processesPerNode, walltime, queue, exclusive, 
            notificationConfig, outFileName, errFileName);
        String finalScript = "#!/bin/bash +x\n" 
                           + "#Script automatically generated by SLURM Plugin\n"
                           + formattedBatchOptions
                           + filteredScript
                           + "echo \"output\" > " + outFileName + "\n"
                           + "echo \"error\" > " + errFileName + "\n"
                           + "#End of automatically generated script\n";
        listener.getLogger().print(finalScript);
        String jobFileName="script";
        PrintWriter writer = new PrintWriter("work/userContent/"+jobFileName,"UTF-8"); //I think this writes to the master
        writer.print(finalScript); //may throw IOException
        writer.close();
        listener.getLogger().println("Remote: "+workspace.getRemote());
        //stealing from LSF plugin - a usage of Copy to Slave plugin
        //TODO - confirm that this is secure if Copy to Slave plugin has JENKINS_HOME option disabled (it is by default)
        CopyToSlaveBuildWrapper copyToSlave = new CopyToSlaveBuildWrapper(jobFileName,"", true, false,
                                                    workspace.getRemote(),false);
        AbstractBuild build = null;
        BuildListener buildListener = null;
        if (run instanceof AbstractBuild && listener instanceof BuildListener) {
            build = AbstractBuild.class.cast(run);
            buildListener = BuildListener.class.cast(listener);
            copyToSlave.setUp(build,launcher,buildListener);
        }
        else {
            throw new AbortException("Cannot copy file to node"); //TODO - make this a user-friendly error
        }
        
        listener.getLogger().println("Script sent to node");
        BatchSystem batchSystem = new SLURMSystem(run,workspace,launcher,listener);
        batchSystem.submitJob(jobFileName);
        
        //TODO - contain these in separate functions
        //copy files back from slave
        listener.getLogger().println("Recovering files from slave");
        String filesToRecover="comms.txt";
        if (outFileName != null && outFileName.length()>0)
            filesToRecover += ","+outFileName;
        if (errFileName != null && errFileName.length()>0)
            filesToRecover += ","+errFileName;
        String recoverDestination=build.getRootDir().getAbsolutePath(); //default from LSF, seems to be where artifacts appear, TODO - custom set?
        listener.getLogger().println("Recovery destination: "+recoverDestination);
        CopyToMasterNotifier copyFilesToMaster = new CopyToMasterNotifier(filesToRecover,"",
                                                        true,recoverDestination,true);
        copyFilesToMaster.perform(build,launcher,buildListener);
        
        ArtifactArchiver archiver = new ArtifactArchiver(filesToRecover);
        archiver.perform(run,workspace,launcher,listener);
        //return true;
    }*/
    
    public String filterScript(String script, String prefix) {
        if (script==null) {
            throw new RuntimeException("Script is null");
        }
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
    
    public boolean isConfigurationValid(BatchSlave node, TaskListener listener) {
        ResourceConfig config=node.getResourceConfig();
        if (nodes<1 || nodes>config.getMaxNodesPerJob()) {
            listener.error("'Nodes' selection exceeds acceptable range (1-"+config.getMaxNodesPerJob()+")");
            return false;
        }
        if (processesPerNode<1 || processesPerNode>config.getMaxProcessesPerNode()) {
            listener.error("'Processes per node' selection exceeds acceptable range (1-"+config.getMaxProcessesPerNode()+")");
            return false;
        }
        if (walltime<1 || walltime>config.getMaxWalltimePerJob()) {
            listener.error("'Walltime' selection exceeds acceptable range (1-"+config.getMaxWalltimePerJob()+")");
            return false;
        }
        else if (config.getAvailableMinutes()-walltime<0) {
            listener.error("System has insufficient available walltime for this job. Please contact your administrator.");
            return false;
        }
        if (queue==null) {
            queue=""; //TODO - set a default? Warn about this?
        }
        else if (config.getAvailableQueues()!=null && config.getAvailableQueues().length()>0){
            if (!config.getAvailableQueues().contains(queue)) { //TODO - make availQueues an array? Make sure full queue name fits an entry
                listener.error("Queue is not available or does not exist");
                return false;
            }
        }
        if (notificationConfig==null) {
            notificationConfig = new NotificationConfig(false,false,false,""); //initialise to prevent later NullPointerException
        }
        if (!notificationConfig.isValid()) {
            listener.error("Notifications selected but no email recipients entered");
            return false;
        }
        //TODO - verify out and err filenames (and don't forget about exclusive mode!!)
        return true;
    }
    /*
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Symbol("submitJob")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public String getDisplayName() {
            return "Run batch script";
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> type) {
            return true;
        }
        
        //public FormValidation doCheckField(@QueryParameter String value) {
         //   if (value.isGood()) return FormValidation.ok();
          //  else return FormValidation.error(Messaaaaaaages.xx.y.z());
        //}
        
        public FormValidation doCheckNodes(@QueryParameter int value) {
            if (value >= 1) 
                return FormValidation.ok();
            else
                return FormValidation.error(Messages.errors_NotPositiveInteger());
        }
        
        public FormValidation doCheckProcessesPerNode(@QueryParameter int value) {
            if (value >= 1) 
                return FormValidation.ok();
            else
                return FormValidation.error(Messages.errors_NotPositiveInteger());
        }
        
        
        public FormValidation doCheckWalltime(@QueryParameter int value) {
            if (value >= 1) 
                return FormValidation.ok();
            else
                return FormValidation.error(Messages.errors_NotPositiveInteger());
        }       
    }*/
    
}