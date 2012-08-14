org.spase.tools
======

Collection of tools for procesisng and using SPASE description and SPASE services.

- org.spase.tools.Collator</dt><dd>Separate each SPASE resource description in a file into a separate file stored in a folder tree according to the Resource ID.
- org.spase.tools.Downloader</dt><dd>Obtains a list of URLs associated with a resource by querying a registry server, then downloads and packages all the source files. The collection of files is packaged into a zip file and written to the output file.
- org.spase.tools.Profiler</dt><dd>Create resource profiles for SPASE resource descriptions. Profiles all have a common schema which can be used in a solr search engine.
- org.spase.tools.RefCheck</dt><dd>SPASE Resource Description reference checker. Can check both resource identifiers and URLs for referential integrity.
- org.spase.tools.XMLGrep</dt><dd>XML Parser, XPath generator and search tool.


See "LICENSE.TXT" for important licensing nformation.

# Build/Installation

Apache Ant can be used to build this project.

  1) cd to "project"
  2) run "ant dist" to build a full distribution
     or "ant build" to compile source only.

output is placed in the "build" folder.

# Project Tree

+ bin   Command line programs or scripts for the project.
+ build Output from a build is placed here. Created by "build" task.
+ conf  Configuration file used by applications. Often bundled with a build.
+ doc   Documentation for the project or generated applications.
+ dist  Output from a distribution build. Create by a "dist" task. A distribution includes libraries, exectables and documentation.
+ lib   Libraries that the project is dependent upon. These are typically covered by individual licenses.
+ project   Build configuration files. 
+ src    Source code files. Projects the contain multiple tools will typically have sub-folders.
+ xsl   XML Stylesheet files used by applications. Often bundled with a build.


For documentation regarding installation of the software, 
either point your favorite browser to the ./doc/index.html
file or view the PDF file located at ./doc/XXX.pdf (where 
XXX is name of the package).

