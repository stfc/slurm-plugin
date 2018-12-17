package io.jenkins.plugins.slurm;

import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.jenkinsci.remoting.RoleChecker;

public class BatchSlaveComputer extends SlaveComputer {
    //implementation currently redundant, but may later be useful
    protected static final Logger LOGGER=Logger.getLogger(BatchSlaveComputer.class.getName());
    
    public BatchSlaveComputer(BatchSlave slave) {
        super(slave);
    }
    
    @Override
    public BatchSlave getNode() {
        return (BatchSlave)super.getNode();
    }
    
}