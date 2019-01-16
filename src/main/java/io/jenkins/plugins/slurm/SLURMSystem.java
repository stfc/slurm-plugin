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

import com.michelin.cio.hudson.plugins.copytoslave.CopyToMasterNotifier;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Shell;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import jenkins.util.BuildListenerAdapter;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Laisvydas Skurevicius
 */
public class SLURMSystem extends BatchSystem {

    //private static final Set ENDING_STATES = new HashSet();

    //static {
    //    ENDING_STATES.add("DONE");
    //    ENDING_STATES.add("EXIT");
    //}

    private static final BuildListenerAdapter fakeListener
            = new BuildListenerAdapter(TaskListener.NULL);

    public SLURMSystem(Run<?, ?> run, FilePath workspace, Launcher launcher,
            TaskListener listener) {//, String COMMUNICATION_FILE, String masterWorkingDirectory
        super(run, workspace, launcher, listener); //, COMMUNICATION_FILE, masterWorkingDirectory
    }

    //submit job to SLURM
    //should return true/false according to result of Shell.perform
    @Override
    public boolean submitJob(String jobFileName) throws InterruptedException, IOException {
        // submits the job to SLURM - method from LSF plugin
        Shell shell = new Shell("#!/bin/bash +x\n" + "cd "+remoteWorkingDirectory+"\n"
                            + "chmod 755 " + jobFileName +"\n"
                            + "sbatch " + jobFileName /*+ " | tee "+communicationFile*/+"\n"
                            + "echo $? > "+communicationFile);
        shell.perform(build, launcher, blistener);
        
        //get exit code by retrieving communicationFile
        CopyToMasterNotifier copyFileToMaster = new CopyToMasterNotifier(communicationFile,"",true,masterWorkingDirectory,true);
        BuildListenerAdapter fakeListener = new BuildListenerAdapter(TaskListener.NULL);
        copyFileToMaster.perform(build,launcher,fakeListener);
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(
                new FileInputStream(masterWorkingDirectory + "/" + communicationFile),"utf-8"));
        String currentLine;
        String lastLine="";
        while ((currentLine=fileReader.readLine()) != null) {
            lastLine=currentLine;
        }
        fileReader.close();
        int exitCode;
        try {
            exitCode = Integer.parseInt(lastLine);       
        }
        catch (NumberFormatException e) {
            throw new AbortException(e.getMessage());
        }
        return exitCode == 0;
        
        /*
        // stores the job id
        copyFileToMaster.perform(build, launcher, fakeListener);
        BufferedReader fileReader = new BufferedReader(
                new FileReader(masterWorkingDirectory + COMMUNICATION_FILE));
        String jobId = fileReader.readLine();
        jobId = jobId.substring(jobId.indexOf('<', 0)
                + 1, jobId.indexOf('>', 0));
        return jobId;
        */
    }
    
    @Override
    public void cleanUpFiles() throws InterruptedException {
        listener.getLogger().println("Cleaning up workspace");
        Shell shell = new Shell("#!/bin/bash +x\n" + "cd "+remoteWorkingDirectory+"\n"
                                + "rm -rf ./*");
        shell.perform(build, launcher, blistener);
    }
    
    
    
    
    /*
    
    
                // U N C H E C K E D C O N T E N T
                // TODO - check all this content

    @Override
    public String getJobStatus(String jobId)
            throws IOException, InterruptedException {
        Shell shell = new Shell("#!/bin/bash +x\n bjobs " + jobId + " > "
                + COMMUNICATION_FILE);
        shell.perform(build, launcher, fakeListener);
        copyFileToMaster.perform(build, launcher, fakeListener);
        BufferedReader fileReader = new BufferedReader(
                new FileReader(masterWorkingDirectory + COMMUNICATION_FILE));
        fileReader.readLine();
        return fileReader.readLine().trim().split(" ")[2];
    }

    @Override
    public void killJob(String jobId) throws InterruptedException {
        Shell shell = new Shell("#!/bin/bash +x\n bkill " + jobId);
        shell.perform(build, launcher, listener);
    }

    @Override
    public void processStatus(String jobStatus) {
        if (jobStatus.equals("PEND")) {
            listener.getLogger().println("Waiting in a queue for scheduling "
                    + "and dispatch.");
        } else if (jobStatus.equals("RUN")) {
            listener.getLogger().println("Dispatched to a host and running.");
        } else if (jobStatus.equals("DONE")) {
            listener.getLogger().println("Finished normally with zero "
                    + "exit value.");
        } else if (jobStatus.equals("EXIT")) {
            listener.getLogger().println("Finished with non-zero exit value.");
        } else if (jobStatus.equals("PSUS")) {
            listener.getLogger().println("Suspended while pending.");
        } else if (jobStatus.equals("USUS")) {
            listener.getLogger().println("Suspended by user.");
        } else if (jobStatus.equals("SSUS")) {
            listener.getLogger().println("Suspended by the LSF system.");
        } else if (jobStatus.equals("WAIT")) {
            listener.getLogger().println("Members of a chunk job that "
                    + "are waiting to run.");
        } else {
            listener.getLogger().println("Job status not recognized.");
        }
    }

    @Override
    public void printErrorLog() throws InterruptedException {
        listener.getLogger().println("Job exited with following errors:");
        Shell shell = new Shell("#!/bin/bash +x\n cat errorLog");
        shell.perform(build, launcher, listener);
    }

    @Override
    public void printExitCode(String jobId)
            throws InterruptedException, IOException {
        Shell shell = new Shell("#!/bin/bash +x\n bjobs -l "
                + jobId + " > " + COMMUNICATION_FILE);
        shell.perform(build, launcher, fakeListener);
        copyFileToMaster.perform(build, launcher, fakeListener);
        String exitCode = FileUtils.readFileToString(
                new File(masterWorkingDirectory + COMMUNICATION_FILE));
        if (exitCode.contains("Exited with exit code ")) {
            listener.getLogger().println();
            exitCode = exitCode.substring(
                    exitCode.indexOf("Exited with exit code "),
                    exitCode.length());
            exitCode = exitCode.substring(0, exitCode.indexOf(".") + 1);
            listener.getLogger().println(exitCode);
        }
    }

    @Override
    public void createJobProgressFile(String jobId, String outputFileName)
            throws InterruptedException, IOException {
        Shell shell = new Shell("#!/bin/bash +x\n bpeek "
                + jobId + " > " + outputFileName);
        shell.perform(build, launcher, listener);
    }

    @Override
    public void createFormattedRunningJobOutputFile(String outputFileName,
            int offset, int numberOfLines)
            throws InterruptedException, IOException {
        // for clearing the running job output headers
        if (offset > 2) {
            offset = offset - 2;
        } else {
            offset = offset + 2;
        }
        numberOfLines = numberOfLines - 2;
        Shell shell = new Shell("#!/bin/bash +x\n tail -n+" + offset + " "
                + outputFileName + " | head -n " + (numberOfLines - offset)
                + " > " + COMMUNICATION_FILE);
        shell.perform(build, launcher, fakeListener);
    }

    @Override
    public void createFinishedJobOutputFile(String jobId, int offset)
            throws InterruptedException {
        // because of the running job output headers
        if (offset >= 3) {
            offset = offset - 3;
        }
        Shell shell = new Shell("#!/bin/bash +x\n tail -n+" + offset
                + " LSFJOB_" + jobId + "/STDOUT" + " > " + COMMUNICATION_FILE);
        shell.perform(build, launcher, listener);
    }
    
    @Override
    public void cleanUpFiles(String jobId) throws InterruptedException {
        Shell shell = new Shell("rm -rf LSFJOB_" + jobId + " errorLog");
        shell.perform(build, launcher, fakeListener);
    }

    @Override
    public boolean isRunningStatus(String jobStatus) {
        return jobStatus.equals("RUN");
    }

    @Override
    public boolean isEndStatus(String jobStatus) {
        return ENDING_STATES.contains(jobStatus);
    }

    @Override
    public boolean jobExitedWithErrors(String jobStatus) {
        return jobStatus.equals("EXIT");
    }

    @Override
    public boolean jobCompletedSuccessfully(String jobStatus) {
        return jobStatus.equals("DONE");
    }

    */
}
