package io.jenkins.plugins.slurm;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.slaves.NodeProperty;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

public class SLURMSlave extends BatchSlave {
    private static final Logger LOGGER = Logger.getLogger(SLURMSlave.class.getName());
    private final String prefix = "#SBATCH";
    
    @DataBoundConstructor
    public SLURMSlave(String name, String nodeDescription, String remoteFS, 
            String numExecutors, Mode mode, String labelString, 
            ComputerLauncher launcher, RetentionStrategy retentionStrategy, 
            List<? extends NodeProperty<?>> nodeProperties, 
            ResourceConfig resourceConfig) 
            throws Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, numExecutors, mode, labelString, launcher, retentionStrategy, nodeProperties, resourceConfig);
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    @Override
    public Computer createComputer() {
        LOGGER.info("Creating a new SLURM Slave");
        return new SLURMSlaveComputer(this);
    }
    
    //ideally would be static, but isn't due to general BatchSlave calls in BatchBuilder.generateScript
    public String formatBatchOptions(int nodes, int processesPerNode,
            int walltime, String queue, boolean exclusive, 
            NotificationConfig notificationConfig,
            String outFileName, String errFileName) {
        StringBuffer buffer = new StringBuffer(); 
        buffer.append(prefix+" -N "+nodes+"\n");
        buffer.append(prefix+" -n "+processesPerNode+"\n");
        buffer.append(prefix+" -t "+walltime+"\n");
        buffer.append(prefix+" -p scarf\n");
        if (queue!=null && queue.length()>0)
            buffer.append(prefix+" -C "+queue+"\n");
        if (exclusive)
            buffer.append(prefix+" --exclusive \n");
        if (outFileName!=null && outFileName.length()>0)
            buffer.append(prefix+" -o "+outFileName+"\n");
        if (errFileName!=null && errFileName.length()>0)
            buffer.append(prefix+" -e "+errFileName+"\n");
        if (notificationConfig!=null) {
            if (notificationConfig.isNotifyStartTicked() 
                || notificationConfig.isNotifyEndTicked() 
                || notificationConfig.isNotifyAbortTicked()) {
                buffer.append(prefix+" --mail-type=");
                ArrayList<String> conditions = new ArrayList<String>();
                if (notificationConfig.isNotifyStartTicked())
                    conditions.add("BEGIN");
                if (notificationConfig.isNotifyEndTicked())
                    conditions.add("END");
                if (notificationConfig.isNotifyAbortTicked())
                    conditions.add("FAIL");
                buffer.append(String.join(",",conditions));
                buffer.append("\n");
                buffer.append(prefix+" --mail-user="+notificationConfig.getNotificationMailingList()+"\n");
            }
        }
        String finalString = buffer.toString();
        return finalString;
    }
    
    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
        public String getDisplayName() {
            return "SLURM Slave";
        }
        
        @Override
        public boolean isInstantiable() {
            return true;
        }
    }
    
}