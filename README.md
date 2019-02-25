This is a Jenkins plugin that allows jobs to be submitted to HPC systems running SLURM.

The plugin adds a new node type, 'SLURM Agent,' and a new build step, 'Run SLURM 
script' (which can only be run on instances of SLURM agents). The agent configuration
can be used to limit the amount of compute resource that can be 
used by a single job. When run, the Jenkins job will wait for the SLURM job to finish, 
and recover output files at the end.

There are a number of generalised base classes in the plugin - these may serve 
as a jumping-off point for other batch systems (with the SLURM implementation 
as a reference).

You must also have the [Copy To 
Slave](https://wiki.jenkins.io/display/JENKINS/Copy+To+Slave+Plugin) plugin 
installed to use this plugin.

This plugin was influenced by the 
[lsf-cloud](https://github.com/LaisvydasLT/lsf-cloud) and 
[pbs](https://github.com/biouno/pbs-plugin) plugins.