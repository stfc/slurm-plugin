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
import java.util.ArrayList;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.StringBuffer;
import java.util.Scanner;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundConstructor;

public class SLURMBuilder extends BatchBuilder {
    
    @DataBoundConstructor
    public SLURMBuilder(String rawScript, int nodes, int tasks, int cpusPerTask,
            int walltime, String queue, boolean exclusive, 
            NotificationConfig notificationConfig,
            String outFileName, String errFileName) {
        super(rawScript,nodes,tasks,cpusPerTask,walltime,queue,exclusive,
                notificationConfig,outFileName,errFileName);
    }
    
    @Override
    public void perform(Run<?,?> run, FilePath workspace, Launcher launcher, 
            TaskListener listener) throws InterruptedException, IOException {
        Computer computer = getComputer(workspace);
        Node node = getNode(computer);
        try { //TODO - remove this later, only a debug thing
            listener.getLogger().println(computer.getNode().getDisplayName());
        } catch (NullPointerException e) {
            listener.getLogger().println("WARNING: No display name found for node");
        }
        if (!(computer instanceof SLURMSlaveComputer)) {
            throw new AbortException("Computer is not a SLURM node computer");
        }
        SLURMSlave slurmNode = (SLURMSlave) node;
        String finalScript = generateScript(rawScript,slurmNode,listener);
        listener.getLogger().print(finalScript);
        String jobFileName = "script"; //TODO - figure out naming convention
        writeScriptToFile(finalScript,jobFileName);
        listener.getLogger().println("Remote: "+workspace.getRemote());
        sendFileToRemote(jobFileName,run,workspace,launcher,listener);
        listener.getLogger().println("Script sent to remote");
        BatchSystem batchSystem = new SLURMSystem(run,workspace,launcher,listener);
        boolean result = batchSystem.submitJob(jobFileName); //TODO - make waitFor() clearer
        listener.getLogger().println("Recovering files from remote");
        ArrayList<String> filesToRecover = new ArrayList<String>();
        if (outFileName != null && outFileName.length()>0) {
            filesToRecover.add(outFileName);
        }
        if (errFileName != null && errFileName.length()>0) {
            filesToRecover.add(errFileName);
        }
        String filesToRecoverString = String.join(",",filesToRecover);
        String recoveryDestination=run.getRootDir().getAbsolutePath();
        listener.getLogger().println("Recovery destination: "+recoveryDestination);
        recoverFiles(filesToRecoverString,recoveryDestination,run,workspace,launcher,listener);
        batchSystem.cleanUpFiles();
        if (!result) {
            throw new AbortException("SLURM job did not complete successfully");
        }
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Symbol("submitSLURMJob")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public String getDisplayName() {
            return "Run SLURM script";
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
        
        public FormValidation doCheckTasks(@QueryParameter int value) {
            if (value >= 1) 
                return FormValidation.ok();
            else
                return FormValidation.error(Messages.errors_NotPositiveInteger());
        }
        
        public FormValidation doCheckCpusPerTask(@QueryParameter int value) {
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