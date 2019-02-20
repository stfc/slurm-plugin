package io.jenkins.plugins.slurm;

import com.michelin.cio.hudson.plugins.copytoslave.CopyToMasterNotifier;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Shell;
import jenkins.util.BuildListenerAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * @author Eli Chadwick
 */
public class SLURMSystem extends BatchSystem {

    public SLURMSystem(final Run<?, ?> run, final FilePath workspace, 
            final Launcher launcher, final TaskListener listener,
            final String communicationFile) { 
        super(run, workspace, launcher, listener, communicationFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] submitJob(String jobFileName, int walltime) 
            throws InterruptedException, IOException {
        String sbatchOutputFile = "_sbatch_output.txt";
        //submit the job to SLURM - method from LSF plugin
        //save stdout and exit code of sbatch to sbatchOutputFile
        Shell shell = new Shell("#!/bin/bash +xl\n" + "cd " + getRemoteWorkingDirectory() + "\n"
                            + "module avail >/dev/null 2>&1\n" //rebuilding module cache
                            + "chmod 755 " + jobFileName + "\n"
                            + "sbatch " + jobFileName + " > " + sbatchOutputFile + " 2>&1\n"
                            + "echo $? >> " + sbatchOutputFile + "\n");
        shell.perform(getAbstractBuild(), getLauncher(), getBuildListener());

        //TODO - make separate function for recovering info?
        //get exit code by retrieving communicationFile
        CopyToMasterNotifier copyFileToMaster = 
                new CopyToMasterNotifier(getCommunicationFile() + "," + sbatchOutputFile, 
                        "", true, getMasterWorkingDirectory(), true);
        BuildListenerAdapter fakeListener = new BuildListenerAdapter(TaskListener.NULL);
        copyFileToMaster.perform(getAbstractBuild(), getLauncher(), fakeListener);

        BufferedReader fileReader = new BufferedReader(new InputStreamReader(
                new FileInputStream(getMasterWorkingDirectory() + "/" + sbatchOutputFile), "utf-8"));
        int jobID = -1;
        int exitCode = -1;
        float computeTimeSec = 0;
        boolean failedToSubmit = false;
        //recover job ID / check for job submission failure
        try {
            String firstLine = fileReader.readLine();
            if (firstLine.contains("Batch job submission failed")) {
                failedToSubmit = true;
                getListener().getLogger().println(firstLine);
            } else { //submitted successfully
                String[] splitLine = firstLine.split(" ");
                jobID = Integer.parseInt(splitLine[splitLine.length - 1]);
                getListener().getLogger().println("Job ID: " + jobID);
            }
        } catch (NullPointerException | NumberFormatException e) {
            getListener().getLogger().println("Could not recover job ID: " + e.getMessage());
        }
        //recover sbatch exit code
        try {
            String line = "";
            String lastLine = "";
            while ((line = fileReader.readLine()) != null) {
                lastLine = line;
            }
            exitCode = Integer.parseInt(lastLine);
        } catch (NumberFormatException e) {
            getListener().getLogger().println("Could not recover sbatch exit code: " + e.getMessage());
        }
        fileReader.close();
        //retrieve user script exit code & time information
        try {
            File file = new File(getMasterWorkingDirectory() + "/" + getCommunicationFile());
            Scanner scanner = new Scanner(file, "utf-8");
            int exitCodeInternal = scanner.nextInt(); //first line of file should be exit code
            if (exitCodeInternal != exitCode) {
                getListener().getLogger().println("WARNING: Exit code of user script does not equal sbatch exit code");
            }
            String line = scanner.nextLine(); //empty line before `times` output //TODO - make this nicer...
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                String[] split = line.split("\\p{Alpha}\\s*");
                float userTimeMin = Float.parseFloat(split[0]);
                float userTimeSec = Float.parseFloat(split[1]);
                float sysTimeMin = Float.parseFloat(split[2]);
                float sysTimeSec = Float.parseFloat(split[3]);
                getListener().getLogger().println(userTimeMin + " " + userTimeSec + " " + sysTimeMin + " " + sysTimeSec);
                computeTimeSec += userTimeMin * 60 + userTimeSec + sysTimeMin * 60 + sysTimeSec;
            }
            getListener().getLogger().println("Total compute time: " + computeTimeSec + " seconds");
        } catch (FileNotFoundException e) {
            if (failedToSubmit) { //if it didn't submit in the first place, no time will have been used
                computeTimeSec = 0;
            } else {
                getListener().getLogger().println("WARNING: Runtime information could not be retrieved. The job may have timed out.");
                computeTimeSec = walltime * 60; //TODO - update this if walltime info is updated
            }
        }
        int[] output = {jobID, exitCode, (int) Math.ceil(computeTimeSec)};
        return output;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void cleanUpFiles() throws InterruptedException {
        getListener().getLogger().println("Cleaning up workspace");
        if (getRemoteWorkingDirectory().contains("workspace")) {
            Shell shell = new Shell("#!/bin/bash +x\n"
                                   + "echo " + getRemoteWorkingDirectory() + "\n"
                                   + "mkdir -p /tmp/jenkins\n"
                                   + "[ -d \"" + getRemoteWorkingDirectory() + "\" ] && mv " + getRemoteWorkingDirectory() + " /tmp/jenkins/\n"
                                   + "rm -rf /tmp/jenkins\n");
            shell.perform(getAbstractBuild(), getLauncher(), getBuildListener());
        } else {
            getListener().getLogger().println("Something is wrong - remote directory does not contain 'workspace': " + getRemoteWorkingDirectory());
        }
    }
}
