package io.jenkins.plugins.slurm;

public class BatchOptionsException extends RuntimeException {

    private static final long serialVersionUID = -308538480475052665L;

    /**
     * Defaut constructor.
     */
    public BatchOptionsException() {
    }

    /**
     * Constructor with message.
     *
     * @param message exception message
     */
    public BatchOptionsException(String message) {
        super(message);
    }

    /**
     * Constructor with cause.
     *
     * @param cause exception cause
     */
    public BatchOptionsException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with message and cause.
     *
     * @param message exception message
     * @param cause exception cause
     */
    public BatchOptionsException(String message, Throwable cause) {
        super(message, cause);
    }

}