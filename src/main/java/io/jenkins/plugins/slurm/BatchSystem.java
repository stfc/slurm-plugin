/*
 * The MIT License
 *
 * Copyright 2015 Laisvydas Skurevicius.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.slurm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;

/**
 *
 * @author Laisvydas Skurevicius
 */
public abstract class BatchSystem {

    //these required for Shell
    protected final AbstractBuild<?,?> build;
    protected final BuildListener blistener;
    protected final Run<?, ?> run;
    protected final FilePath workspace;
    protected final Launcher launcher;
    protected final TaskListener listener;
    // every file created by the batch system should have this name
    protected final String communicationFile;
    //protected final CopyToMasterNotifier copyFileToMaster;
    protected final String masterWorkingDirectory;
    protected final String remoteWorkingDirectory;
    
    public BatchSystem(Run<?, ?> run, FilePath workspace, Launcher launcher,
            TaskListener listener, String communicationFile) {//, String COMMUNICATION_FILE, String masterWorkingDirectory) 
        //this.COMMUNICATION_FILE = COMMUNICATION_FILE;
        //this.copyFileToMaster = new CopyToMasterNotifier(COMMUNICATION_FILE,
               // "", true, masterWorkingDirectory, true);
        this.run = run;
        this.workspace = workspace;
        this.launcher = launcher;
        this.listener = listener;
        if (run instanceof AbstractBuild)
            this.build=AbstractBuild.class.cast(run);
        else
            this.build=null;
        if (listener instanceof BuildListener)
            this.blistener=BuildListener.class.cast(listener);
        else
            this.blistener=null;
        this.communicationFile = communicationFile;
        this.masterWorkingDirectory = run.getRootDir().getAbsolutePath();
        this.remoteWorkingDirectory = workspace.getRemote();
    }
    
    //TODO - ADD GETTERS!
    
    public String getCommunicationFile() {
        return communicationFile;
    }

    
    //submits the the job to the batch system
    public abstract int[] submitJob(String jobFileName) throws InterruptedException, IOException;

    /*
     * cleans up the files created by the batch system
     * @param jobId
     * @throws InterruptedException 
     */
    public abstract void cleanUpFiles() throws InterruptedException;
            
            
            
            
            
            // U N C H E C K E D C O N T E N T
            //TODO - check all this content
            
    /**
     * @param jobId
     * @return the job status of the specified job id
     * @throws IOException
     * @throws InterruptedException
     
    public abstract String getJobStatus(String jobId)
            throws IOException, InterruptedException;

    /**
     * kills the job with specified job id in the batch system
     *
     * @param jobId the identifier of the job
     * @throws InterruptedException
     
    public abstract void killJob(String jobId) throws InterruptedException;

    /**
     * executes the appropriate actions depending on the status of the job
     * (prints the appropriate messages)
     *
     * @param jobStatus the status of the job
     
    public abstract void processStatus(String jobStatus);

    /**
     * prints the error log to the slave console
     *
     * @throws InterruptedException
     
    public abstract void printErrorLog() throws InterruptedException;

    /**
     * prints the exit code to the slave console
     *
     * @param jobId the identifier of the job
     * @throws InterruptedException
     * @throws IOException
     
    public abstract void printExitCode(String jobId)
            throws InterruptedException, IOException;

    /**
     * creates the job output file of the running job in the slave
     *
     * @param jobId the identifier of the job
     * @param outputFileName name of the created output file
     * @throws InterruptedException
     * @throws IOException
     
    public abstract void createJobProgressFile(String jobId,
            String outputFileName) throws InterruptedException, IOException;

    /**
     * creates a formatted job output file of the running job in the slave from
     * the specified output file (created by createJobProgressFile method)
     *
     * @param outputFileName the output file that should be formatted
     * @param offset number of lines that should be skipped from the given
     * output file
     * @param numberOfLines the total amount of lines of the given output file
     * @throws InterruptedException
     
    public abstract void createFormattedRunningJobOutputFile(
            String outputFileName, int offset, int numberOfLines)
            throws InterruptedException, IOException;

    /**
     * creates the final job output file of the finished job in the slave
     *
     * @param jobId the identifier of the job
     * @param offset number of lines that should be skipped from the final
     * output
     * @throws InterruptedException
     
    public abstract void createFinishedJobOutputFile(String jobId, int offset)
            throws InterruptedException;

    /**
     * cleans up the files created by the batch system
     * @param jobId
     * @throws InterruptedException 
     
    public abstract void cleanUpFiles(String jobId) 
            throws InterruptedException;
    /**
     * @param jobStatus the status of the job
     * @return true if the given job status is a running status
     
    public abstract boolean isRunningStatus(String jobStatus);

    /**
     * @param jobStatus the status of the job
     * @return true if the given job status is an ending state
     
    public abstract boolean isEndStatus(String jobStatus);

    /**
     * @param jobStatus the status of the job
     * @return true if the job exited with errors
     
    public abstract boolean jobExitedWithErrors(String jobStatus);

    /**
     * @param jobStatus the status of the job
     * @return true if the job completed successfully without errors
     
    public abstract boolean jobCompletedSuccessfully(String jobStatus);
    */
}