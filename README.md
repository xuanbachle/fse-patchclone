# fse-patchclone
To run the project, we need a configuration file.
If we have the jar of the project called fse-patchclone.jar, then run it by:
fse-patchclone.jar path-to-configuration-file

The configuration file is located in the resources folder of the project. Basically,
it looks like this:

# Root folder point to the root where you store your data
rootFolder = /home/xuanbach/Desktop/data/alldata_withUnit/test/

# These options are for setting the stub code. If you want empty stub code, just leave them empty
# empty like this
# stubCodeBef =
# stubCodeAft =
stubCodeBef = C{\n\tA a = new A();\n\ta.abc();\n\tB c = new B(a);\n
stubCodeAft = \tC c = new C();\n}

