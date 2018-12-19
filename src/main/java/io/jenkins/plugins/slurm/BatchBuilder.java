package io.jenkins.plugins.slurm;

import com.michelin.cio.hudson.plugins.copytoslave.CopyToSlaveBuildWrapper;
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
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.StringBuffer;
import java.util.Scanner;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundConstructor;

public class BatchBuilder extends Builder implements SimpleBuildStep {
    //raw script input
    private String rawScript;
    //number of nodes
    private int nodes;
    //processes per node
    private int processesPerNode;
    //expected walltime of job
    private int walltime;
    //queue selection
    private String queue;
    //run job in exclusive mode
    private boolean exclusive;
    //email notification configuration
    private NotificationConfig notificationConfig;
    //stdout file name
    private String outFileName;
    //stderr file name
    private String errFileName;
    // file name for the communication between master and slave
    //private static final String COMMUNICATION_FILE = "output";
    // name of the file where the running job output is saved - potentially not necessary any more? idk must look at
    //private static final String PROGRESS_FILE = "jobProgress";
    //private String masterWorkingDirectory;
    //private String slaveWorkingDirectory;
    
    @DataBoundConstructor
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
    
    public void perform(Run<?,?> run, FilePath workspace, Launcher launcher,
            TaskListener listener) throws InterruptedException,IOException {
        listener.getLogger().println("Testing 123");
        Computer computer=workspace.toComputer();
        if (computer==null) {
            throw new AbortException("Computer is null");
        }
        Node node=computer.getNode(); //should not be null?
        if (node==null) {
            throw new AbortException("Node is null");
        }
        try {
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
        String finalScript = "#Script automatically generated by SLURM Plugin\n"
                           + formattedBatchOptions
                           + filteredScript
                           + "#End of automatically generated script\n";
        listener.getLogger().print(finalScript);
        String jobFileName="script";
        PrintWriter writer = new PrintWriter("work/userContent/"+jobFileName,"UTF-8"); //I think this writes to the master
        writer.print(finalScript); //may throw IOException
        writer.close();
        listener.getLogger().println("Remote: "+workspace.getRemote());
        //stealing from LSF plugin - a usage of Copy to Slave plugin - don't install directly onto Jenkins as it has a major security flaw...        
        //TODO - make this more secure for the master somehow?? Has anyone done this?????
        CopyToSlaveBuildWrapper copyToSlave = new CopyToSlaveBuildWrapper(jobFileName,"", true, false,
                                                    workspace.getRemote(),false);
        if (run instanceof AbstractBuild && listener instanceof BuildListener)
            copyToSlave.setUp(AbstractBuild.class.cast(run),launcher,BuildListener.class.cast(listener));
        else
            throw new AbortException("Cannot copy to slave"); //TODO - make this a user-friendly error
        
        listener.getLogger().println("Script written");
        BatchSystem batchSystem = new SLURMSystem(run,workspace,launcher,listener);
        batchSystem.submitJob(jobFileName);
        //return true;
    }
    
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
    
    public boolean isConfigurationValid(SLURMSlave node, TaskListener listener) {
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
    }
}