package io.jenkins.plugins.slurm;

import java.util.logging.Logger;

/**
 * @author Eli Chadwick
 */
public class SLURMSlaveComputer extends BatchSlaveComputer {
    protected static final Logger LOGGER = Logger.getLogger(SLURMSlaveComputer.class.getName());

    /**
     * @param slave   node to spawn a computer for
     */
    public SLURMSlaveComputer(final SLURMSlave slave) {
        super(slave);
    }

    /**
     * Returns the {@link hudson.model.Node} that this computer represents.
     * @return null if the configuration has changed and the node is removed,
     * yet the corresponding Computer is not yet gone.
     */
    @Override
    public SLURMSlave getNode() {
        return (SLURMSlave) super.getNode();
    }
}
