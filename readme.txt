Build/Installation

Apache Ant can be used to build this project.

  1) cd to "project"
  2) run "ant dist" to build a full distribution
     or "ant build" to compile source only.

output is placed in the "build" folder.

Project Tree

+ bin   Command line programs or scripts for the project.
+ build Output from a build is placed here. Created by "build" task.
+ conf  Configuration file used by applications. Often bundled with a build.
+ dist  Output from a distribution build. Create by a "dist" task. A distribution includes libraries, exectables and documentation.
+ doc   Documentation for the project or generated applications.
+ example   Example files that can be used by the tools and applications.
+ lib   Libraries that the project is dependent upon. These are typically covered by individual licenses.
+ project   Build configuration files. 
+ src    Source code files. Projects the contain multiple tools will typically have sub-folders.
+ test   Files that can be used for testing of the tools and applications.
+ xsd   XML Schema document files used by applications. Often bundled with a build.


For documentation regarding installation of the software, 
either point your favorite browser to the ./doc/index.html
file or view the PDF file located at ./doc/XXX.pdf (where 
XXX is name of the package).
