# fse-patchclone
To run the project, we need a configuration file.
If we have the jar of the project called fse-patchclone.jar, then run it by:

                   java -jar fse-patchclone.jar path-to-configuration-file

The configuration file is located in the resources folder of the project.
You only need to change some parameters (explained in comments in the config file
provided) to make the project run on your local machine

===================Investigate data generated after running the jar===========

After running the code, it will generate the folders: 

        * changedLine: indicates the changed Line
        
        * changedLineStub: the changedLine surrounded by stub code
        
        * aftContext: context after the changed line
        
        * bftContext: context before the changed line
        
For example, if your project folder is: rootFolder/Acstylos_wpi-suite/
then all the generated folders above will be in this folder: rootFolder/Acstylos_wpi-suite/modifiedFiles/2

        

