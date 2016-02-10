Title: SPASE Resource Tools
Author: Todd King
Date: March 4, 2011
CSS: style.css

# igpp.docgen tool
Last update: March 4, 2011; *Author: Todd King* 

## Introduction
___________________

The SPASE (Space Physics Archive Search and Extract) Resource Toolkit contains a set of command-line
applications which can be used to generate, validate, referentially check, use and organize 
resource descriptions written in SPASE XML. The toolkit is written in Java. 

The SPSAE Resource Toolkit contains the following tools:

   **collator** - Separate each SPASE resource description in a file into a separate file stored in a folder tree according to the Resource ID.  
   **downloader** - Obtains a list of URLs associated with a resource by querying a registry server, then downloads and packages all the source files. The collection of files is packaged into a zip file and written to the output file.  
   **profiler** - Create resource profiles for SPASE resource descriptions. Profiles all have a common schema which can be used in a solr search engine.  
   **refcheck** - SPASE Resource Description reference checker. Can check both resource identifiers and URLs for referential integrity.  
   **validator** - Validate a SPASE resource description using a specified version of the data dictionary.  
   **xmlgrep** - XML Parser, XPath generator and search tool.  

## Installation
________________________

The SPASE toolkit is written entirely in Java and should run on any system with a 
Java Runtime Environment (JRE) 1.5 or higher. 
Any required extensions are included in the SPASE toolkit installation package. 
The toolkit is packaged as a self-contained executable "jar" file.

You can download the SPASE resource toolkit jar file from the SPASE web site.
The most recent release can be found at:


http://www.spase-group.org/tools/toolkit/spase-tools-2.0.0.jar

To "install" an executable JAR, copy the JAR file to a local directory. The jar file can then be run
using the "-jar" option to the "java" command. For example:

    java -jar spase-tools-2.0.0.jar [options]

The spase-tools.jar file can also be installed in a location that is in the
Java extension directory. When installed this way the tool can be run with
the "org.spase.tools" class. For example:

    java org.spase.tools [options]

## Tools
________________________

All tools are written in Java and are part of the "org.spase.tools" package. 
Each tool has a common name which can be used with the executable jar package. 
A tool can run with a command like:


    java –jar spase-tools-2.0.0.jar [toolname] [tooloptions]


Each tool also has a Java class which can be called by other programs. 
There are also wrapper scripts to run each tool using just the tool name.
Tool class names are in proper case and the wrapper scripts are in lowercase.

### Collator
________________________

Separate each SPASE resource description in a file into a separate file stored in a folder 
tree according to the Resource ID.

Optionally recursively scan a directory for all files with a given extension
and process each file.

Usage:
 
    collator [options] file
 -or-
    java org.spase.tools.Collator [options] file

Options:

  -b,--base <arg>	Base path for collated output (default: .).  
  -h,--help	Dispay this text  
  -k,--check	Check files, but do not write collated output.  
  -r,--recurse	Recursively process all files starting at path.  
  -v,--verbose	Verbose. Show status at each step  
  -x,--ext <arg>	File name extension for filtering files when processing folders (default: .xml)  

*Acknowledgements*

Development funded by NASA's VMO project at UCLA.

*Example*
Suppose a file (example.xml) contains three SPASE descriptions like:

	<Spase>
	   <Version>2.0.0</Version>
	   <NumericalData>
	      <ResourceID>spase://VMO/NumericalData/GeoTail/LEP/PT60S</ResourceID>
	          ... Details omitted ...
	   </NumericalData>
	   <NumericalData>
	      <ResourceID>spase://VMO/NumericalData/GeoTail/MGF/PT60S</ResourceID>
	          ... Details omitted ...
	   </NumericalData>
	   <NumericalData>
	      <ResourceID>spase://VMO/NumericalData/GeoTail/CPI/PT60S</ResourceID>
	          ... Details omitted ...
	   </NumericalData>
	</Spase>

Then a command like:

    collator example.zip

will generate three files with the path names of:

    ./VMO/NumericalData/GeoTail/LEP/PT60S.xml
    ./VMO/NumericalData/GeoTail/MGF/PT60S.xml
    ./VMO/NumericalData/GeoTail/CPI/PT60S.xml

With each file containing the description for the corresponding resource.

