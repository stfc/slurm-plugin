package io.jenkins.plugins.slurm;

import java.util.logging.Logger;

public class SLURMSlaveComputer extends BatchSlaveComputer {
    protected static final Logger LOGGER = Logger.getLogger(SLURMSlaveComputer.class.getName());

    public SLURMSlaveComputer(final SLURMSlave slave) {
        super(slave);
    }

    @Override
    public SLURMSlave getNode() {
        return (SLURMSlave) super.getNode();
    }
}
