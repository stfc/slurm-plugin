package io.jenkins.plugins.slurm;

import hudson.slaves.SlaveComputer;

import java.util.logging.Logger;

/**
 * A computer corresponding to the BatchSlave node class.
 * @author Eli Chadwick
 */
public class BatchSlaveComputer extends SlaveComputer {
    //implementation currently redundant, but may later be useful
    protected static final Logger LOGGER = Logger.getLogger(BatchSlaveComputer.class.getName());

    /**
     * @param slave   agent to spawn a computer for
     */
    public BatchSlaveComputer(final BatchSlave slave) {
        super(slave);
    }

    /**
     * Returns the {@link hudson.model.Node} that this computer represents.
     * @return null if the configuration has changed and the node is removed,
     * yet the corresponding Computer is not yet gone.
     */
    @Override
    public BatchSlave getNode() {
        return (BatchSlave) super.getNode();
    }
}
