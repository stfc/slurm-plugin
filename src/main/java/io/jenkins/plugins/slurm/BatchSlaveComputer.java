package io.jenkins.plugins.slurm;

import hudson.slaves.SlaveComputer;

import java.util.logging.Logger;

public class BatchSlaveComputer extends SlaveComputer {
    //implementation currently redundant, but may later be useful
    protected static final Logger LOGGER = Logger.getLogger(BatchSlaveComputer.class.getName());

    public BatchSlaveComputer(final BatchSlave slave) {
        super(slave);
    }

    @Override
    public BatchSlave getNode() {
        return (BatchSlave) super.getNode();
    }
}
