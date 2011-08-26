/**
 * Obtains a list of URLs by quering a registry server, 
 * then downloads and packages all the source files.
 * The collection of files is packaged into a zip file
 * and streamed back to the request.
 * <p>
 * Can be deployed as a servlet, bean or run on the command line.
 *<p>
 * Development funded by NASA's VMO project at UCLA.
 *
 * @author Todd King
 * @version 1.00 2006-12-21
 */

package org.spase.tools;

import java.util.ArrayList;

import org.w3c.dom.Document;
import java.net.URI;

/*
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.Enumeration;


import org.w3c.dom.Node;
import javax.xml.transform.dom.DOMSource;

import java.net.URLConnection;
import java.net.URLEncoder;
*/

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

import java.net.URL;

// import org.apache.commons.cli.*;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

public class Downloader
{
	private String	mVersion = "1.0.0";
	
	Boolean	mVerbose = false;
	String	mService = "http://www.spase-group.org/registry/downloader";
	String	mOutput = "resource.zip";
	String	mCheckFile = null;
	String	mIdentifier = null;
	String	mStartDate = null;
	String	mStopDate = null;
	
	// create the Options
	Options mOptions = new Options();

	public Downloader() 
	{
		mOptions.addOption( "h", "help", false, "Display this text" );
		mOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step" );
		mOptions.addOption( "o", "output", true, "Output filename (default: " + mOutput + ").");
		mOptions.addOption( "s", "service", true, "The URL to the registry service which generates download packages (default: " + mService + ").");
		mOptions.addOption( "c", "check", true, "Check a download package.");
		mOptions.addOption( "i", "id", true, "The identifier of the resource.");
		mOptions.addOption( "b", "startdate", true, "The start date of the desired time span.");
		mOptions.addOption( "e", "stopdate", true, "The stop date of the desired time span.");
	}
		
   /** 
	 * Command-line interface.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0
	 * @since		1.0
	 **/
	public static void main(String args[])
   {
		Downloader me = new Downloader();
		boolean	ready = true;

		if (args.length < 1) {
			me.showHelp();
			System.exit(1);
		}
		
		// create the command line parser
		CommandLineParser parser = new PosixParser();
		try { // parse the command line arguments
         CommandLine line = parser.parse(me.mOptions, args);

			if(line.hasOption("h")) me.showHelp();
			if(line.hasOption("v")) me.mVerbose = true;
			if(line.hasOption("o")) me.mOutput = line.getOptionValue("o");
			if(line.hasOption("s")) me.mService = line.getOptionValue("s");
			if(line.hasOption("c")) me.mCheckFile = line.getOptionValue("c");
			if(line.hasOption("i")) me.mIdentifier = line.getOptionValue("i");
			if(line.hasOption("b")) me.mStartDate = line.getOptionValue("b");
			if(line.hasOption("e")) me.mStopDate = line.getOptionValue("e");
			
			if(me.mIdentifier == null && me.mCheckFile == null) {
				System.out.println("A Resource ID (option -i) to download or a file to check (options -c) must be passed.");
				System.exit(1);
			}
			
			if(me.mIdentifier != null) me.download(me.mOutput, me.mIdentifier, me.mStartDate, me.mStopDate);
			if(me.mCheckFile != null) me.checkFile(me.mCheckFile);
			
			if(me.mVerbose) System.out.print("Output written to: " + me.mOutput);
		} catch(Exception e) {
			e.printStackTrace();
		}
			
   }

	/**
	 * Display help information.
    *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0
	 * @since		1.0
	 **/
	public void showHelp()
	{
		System.out.println("");
		System.out.println(getClass().getName() + "; Version: " + mVersion);
		System.out.println("Obtains a list of URLs associated with a resource by quering");
		System.out.println("a registry server, then downloads and packages all the source files.");
		System.out.println("The collection of files is packaged into a zip file and written to");
		System.out.println("the output file.");
		System.out.println("");
		
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java " + getClass().getName() + " [options] id\n", mOptions );

		System.out.println("");
		System.out.println("Development funded by NASA's VMO project at UCLA.");
		System.out.println("");
		System.out.println("Example");
		System.out.println("To download all Granules associated with the resource with the SPASE ResourceID of:");
		System.out.println("");
		System.out.println("spase://VMO/NumericalData/AMPTE_UKS/Plasma/SWI_PT5S  ");
		System.out.println("");
		System.out.println("use the command:");
		System.out.println("");
		System.out.println("   java " + getClass().getName() + " -o example.zip -i spase://VMO/NumericalData/AMPTE_UKS/Plasma/SWI_PT5S");
		System.out.println("");
		System.out.println("which will collect all data files (granules) associated with the resource, package them in a zip file and write the file to \"example.zip\".");
		System.out.println("");
		System.out.println("To download only granules that span a time range use a command like:");
		System.out.println("");
		System.out.println("   java " + getClass().getName() + " -o example.zip -i spase://VMO/NumericalData/AMPTE_UKS/Plasma/SWI_PT5S \\");
		System.out.println("               -b 1990-12-01T00:00:00 -e 2000-01-01T00:00:00");
		System.out.println("");
		System.out.println("which will collect only those data files (granules) which contain data between ");
		System.out.println("1990-12-01T00:00:00 and 2000-01-01T00:00:00.");
	}
	
   /** 
	 * Download granules and write to a file.
	 *
    * @param pathName		the pathname to the file to write the response of the download request.
    * @param resourceID    the SPASE Resource ID of the parent resource of the granules.
    * @param startDate		the start date of the data to include in the package. If "null" there is no limit.
    * @param stopDate		the stop date of the data to include in the package. If "null" there is no limit.
    *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0
	 * @since		1.0
	 **/
   public void download(String pathName, String resourceID, String startDate, String stopDate)
   	throws Exception
   {
		if(mVerbose) { System.out.println("Writing response to: " + pathName); }

   	FileOutputStream output = new FileOutputStream(pathName);
   	
   	download(output, resourceID, startDate, stopDate);
   	
   	output.close();
   	
   	checkFile(pathName);
   }
   
   /** 
	 * Download granules and send to a pre-opened @link{ZipStream}.
	 *
    * @param output			the pre-opened Stream to write delivered package.
    * @param resourceID     the SPASE Resource ID of the parent resource of the granules.
    * @param startDate		the start date of the data to include in the package. If "null" there is no limit.
    * @param stopDate		the stop date of the data to include in the package. If "null" there is no limit.
    *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0
	 * @since		1.0
	 **/
   public void download(OutputStream output, String resourceID, String startDate, String stopDate)
   	throws Exception
   {
		InputStream stream;
		boolean  status;
		String	request;
		byte[]	buffer = new byte[2048];
		
		request = mService;
		request += "?i=" + resourceID;
		if(startDate != null) request += "&b=" + startDate;
		if(stopDate != null) request += "&e=" + stopDate;
		
		if(mVerbose) { System.out.println("Requesting: " + request); }
		
		try {	// Try as URL
			URL file = new URL(request);
			stream = file.openStream();
		} catch(Exception e) {
			System.out.println("Unable to complete request.");
			System.out.println(e.getMessage());
			return;
		}
		
		// Copy bytes to output
		int n;
		long cnt = 0;
		while((n = stream.read(buffer)) != -1) {
			if(n > 0) {	// Write bytes to output
				output.write(buffer, 0, n);
				cnt += n;
				System.out.print(" " + igpp.util.Text.toUnitizedBytes(cnt) + "        \r");
			}
		}
		
		stream.close();
   }

    /** 
	 * Open a zip file and check the contents of the file.
	 *
	 * The "check" process begins with displaying the contents of the file called "acknowledgement.txt".
	 *
    * @param pathName		the pathname to the file to write the response of the download request.
    *
	 * @version     1.0
	 * @since		1.0
	 **/
	public void checkFile(String pathName)
		throws Exception
	{	 
		System.out.println("Opening file...");
		
   	FileInputStream stream = new FileInputStream(pathName);
	   ZipInputStream zip = new ZipInputStream(stream);
	   ZipEntry zipEntry;
	   int		n;
	   long	cnt = 0;
	   long	size = 0;
	   long	sizeCompressed = 0;
	   long tempSize;
	   byte[] buffer = new byte[2048];
	   
	   while((zipEntry = zip.getNextEntry()) != null) {
	   	if(zipEntry.getName().compareTo("acknowledgement.txt") == 0) {
				System.out.println("------------- Acknowledgement -------------");
	   		while((n = zip.read(buffer, 0, 2048)) != -1) {
	   			System.out.write(buffer, 0, n);
	   		}
				System.out.println("------------------------------------------");
	   	}
	   	if(zipEntry.getName().compareTo("spase.xml") == 0) {
	   		System.out.println("Found 'spase.xml'");
	   		while((n = zip.read(buffer, 0, 2048)) != -1) {
	   			// Do nothing
	   		}
	   	}
	   	
	   	cnt++;
	   	tempSize = zipEntry.getSize();
	   	if(tempSize != -1) size += tempSize;
	   	tempSize = zipEntry.getCompressedSize();
	   	if(tempSize != -1) sizeCompressed += tempSize;
	   	
	   	zip.closeEntry();
	   }
	   
	   zip.close();
	   stream.close();

	   System.out.println("Files: " + cnt);
	   System.out.println("Compressed size: " + igpp.util.Text.toUnitizedBytes(sizeCompressed));
	   System.out.println("Uncompressed size: " + igpp.util.Text.toUnitizedBytes(size));

		// Show acknowledgement
   	// showAcknowledgement(pathName);
		
		// Count files and uncompressed size
		// showMetrics(pathName);
	} 
		
    /** 
	 * Open a zip file and display a count of the number of files and uncompressed size.
	 *
    * @param pathName		the pathname to the file to write the response of the download request.
    *
	 * @version     1.0
	 * @since		1.0
	 **/
   public void showMetrics(String pathName)
   	throws Exception
   {
   	FileInputStream stream = new FileInputStream(pathName);
	   ZipInputStream zip = new ZipInputStream(stream);
	   ZipEntry zipEntry;
	   long	cnt = 0;
	   long	size = 0;
	   long	sizeCompressed = 0;
	   long tempSize;
	   
	   while((zipEntry = zip.getNextEntry()) != null) {
	   	cnt++;
	   	tempSize = zipEntry.getSize();
	   	if(tempSize != -1) size += tempSize;
	   	tempSize = zipEntry.getCompressedSize();
	   	if(tempSize != -1) sizeCompressed += tempSize;
	   	zip.closeEntry();
	   }
	   
	   zip.close();
	   stream.close();
	   
	   System.out.println("Files: " + cnt);
	   System.out.println("Compressed size: " + igpp.util.Text.toUnitizedBytes(sizeCompressed));
	   System.out.println("Uncompressed size: " + igpp.util.Text.toUnitizedBytes(size));
   }
   
    /** 
	 * Open a zip file and display the contents of the file called "acknowledgement.txt".
	 *
    * @param pathName		the pathname to the file to write the response of the download request.
    *
	 * @version     1.0
	 * @since		1.0
	 **/
   public void showAcknowledgement(String pathName)
   	throws Exception
   {
   	FileInputStream stream = new FileInputStream(pathName);
	   ZipInputStream zip = new ZipInputStream(stream);
	   ZipEntry zipEntry;
	   int		n;
	   byte[] buffer = new byte[2048];
	   
	   while((zipEntry = zip.getNextEntry()) != null) {
	   	if(zipEntry.getName().compareTo("acknowledgement.txt") == 0) {
				System.out.println("------------- Acknowledgement -------------");
	   		while((n = zip.read(buffer, 0, 2048)) != -1) {
	   			System.out.write(buffer, 0, n);
	   		}
				System.out.println("-----------------------------------");
	   	}
	   	zip.closeEntry();
	   }
	   
	   zip.close();
	   stream.close();
   }
}
   
