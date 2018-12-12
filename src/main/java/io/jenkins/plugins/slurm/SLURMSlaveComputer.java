package io.jenkins.plugins.slurm;

import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.jenkinsci.remoting.RoleChecker;

public class SLURMSlaveComputer extends SlaveComputer {
    protected static final Logger LOGGER=Logger.getLogger(SLURMSlaveComputer.class.getName());
    
    public SLURMSlaveComputer(SLURMSlave slave) {
        super(slave);
    }
    
    @Override
    public SLURMSlave getNode() {
        return (SLURMSlave)super.getNode();
    }
    
}