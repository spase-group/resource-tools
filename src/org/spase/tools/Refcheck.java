/**
 * Reads a SPASE resource description, extracts each resource 
 * identifier and URL, then performs a referenential integrity check
 * on each. 
 * <p>
 * Development funded by NASA's VMO project at UCLA.
 *
 * @author Todd King
 * @version $Id: RefCheck.java 1 2010-04-30 17:24:57Z todd-king $
 */

package org.spase.tools;

import igpp.util.Encode;
import igpp.util.Text;

import igpp.web.SimpleFTP;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.FileFilter;
import java.io.IOException;

import org.w3c.dom.Node;
import org.w3c.dom.Document;

// import org.apache.commons.cli.*;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

public class Refcheck
{
	private String	mVersion = "1.0.0";

	String	mRegistry = "http://www.spase-group.org/registry/resolver";
	boolean	mVerbose = false;
	String	mExtension = ".xml";
	boolean	mRecurse = false;
	boolean	mListOnly = false;
	boolean	mCheckURL = false;
	boolean	mCheckID = false;
	String	mDirectory = null;
	
	// For Local resource ID lists
	ArrayList<String>	mLocalList = new ArrayList<String>();
	
	// create the Options
	Options mOptions = new Options();

	public Refcheck() 
	{
		mOptions.addOption( "h", "help", false, "Display this text" );
		mOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step" );
		mOptions.addOption( "i", "identifier", false, "Check each identifier in the resource description.");
		mOptions.addOption( "l", "list", false, "List all identifiers and URLs. Do not perform referential checks.");
		mOptions.addOption( "u", "urlcheck", false, "Check each all URLs in the resource description.");
		mOptions.addOption( "r", "recurse", false, "Recursively process all files starting at path.");
		mOptions.addOption( "s", "service", true, "The URL to the registry service to look-up resource identifiers (default: " + mRegistry + ").");
		mOptions.addOption( "x", "ext", true, "File name extension for filtering files when processing folders (default: " + mExtension + ")");
		mOptions.addOption( "d", "dir", true, "Directory containing local resource descriptions. Directory is recursively searched for resource descriptions.");
	}
		
   /** 
    * Command line entry point.
	 **/
	public static void main(String args[])
   {
		Refcheck me = new Refcheck();
		
		if(args.length < 1) {
			me.showHelp();
			return;
		}
		
		// create the command line parser
		CommandLineParser parser = new PosixParser();
		try { // parse the command line arguments
         CommandLine line = parser.parse(me.mOptions, args);

			if(line.hasOption("h")) me.showHelp();
			if(line.hasOption("v")) me.mVerbose = true;
			if(line.hasOption("r")) me.mRecurse = true;
			if(line.hasOption("l")) me.mListOnly = true;
			if(line.hasOption("u")) me.mCheckURL = true;
			if(line.hasOption("i")) me.mCheckID = true;
			if(line.hasOption("x")) me.mExtension = line.getOptionValue("x");
			if(line.hasOption("s")) me.mRegistry = line.getOptionValue("s");
			if(line.hasOption("d")) me.mDirectory = line.getOptionValue("d");
			
			if(me.mVerbose) System.out.println("Resolver service: " + me.mRegistry);
				
			// Load local descriptions
			if(me.mDirectory != null) {
				if(me.mVerbose) System.out.println("Loading local IDs from: " + me.mDirectory);
				me.loadResourceIndex(me.mDirectory);
			}
				
			// Load resource at path
			for(String p : line.getArgs()) { System.out.println("\n"); me.check(p); }
		} catch( ParseException e ) { // oops, something went wrong
	      System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
		} catch(Exception e) {
			e.printStackTrace();
		}
   }

	/**
	 * Display help information.
    *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @since           1.0
	**/
	public void showHelp()
	{
		System.out.println("");
		System.out.println(getClass().getName() + "; Version: " + mVersion);
		System.out.println("SPASE Resource Description reference checker.");
		System.out.println("");
		System.out.println("Can check both resource identifiers and URLs for referenential integrity.");
		System.out.println("Can also produce lists of identifiers and URLs.");
		System.out.println("");
		
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java " + getClass().getName() + " [options] file\n", mOptions );

		System.out.println("");
		System.out.println("Acknowledgements:");
		System.out.println("Development funded by NASA's VMO project at UCLA.");
		System.out.println("");
		System.out.println("Example");
		System.out.println("To check that all Resource IDs in all files can be resolved:");
		System.out.println("");
		System.out.println("   java " + getClass().getName() + " -i -r *");
		System.out.println("");
		System.out.println("To check all Resource IDs and URLs use the command:");
		System.out.println("");
		System.out.println("   java " + getClass().getName() + " -i -r *");
		System.out.println("");
		System.out.println("To see detailed information while checking add the verbose flag (-v):");
		System.out.println("");
		System.out.println("   java " + getClass().getName() + " -v -i -r *");
	}
	
	/** 
	 * Read all SPASE resource descriptions at the given path.
	 * Extract each resource description is then extracted and
	 * re-written in the location defined by the Resource Identifier.
	 * The files with the given extension is parsed for resource descriptions.
	 * The path to the resources can be recusively searched.
    *
    * @param path     the pathname of the file to parse.
    *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @since           1.0
	**/
	public void check(String path)
		throws Exception
	{
		if(mListOnly) mVerbose = true;	// Make verbose
		if(path == null) return;
				
		// File name filter
	   File filePath = new File(path);
	   File[] list = new File[1];
	   if(filePath.isDirectory()) {
			list = filePath.listFiles(new FileFilter()	
		   	{ 
		   		public boolean accept(File pathname) { return pathname.getName().endsWith(mExtension); } 
		   	} 
		   	);
	   } else {
	   	list[0] = filePath;
	   }

		String resourcePath;
		if(list != null) {	// Found some files to process
			for(File item : list) {
				resourcePath = item.getCanonicalPath();
				try {
					checkFile(resourcePath);
				} catch(Exception e) {
					System.out.println("Error parsing: " + resourcePath);
					System.out.println(e.getMessage());
				}
			}		
		}
		
		// Now recurse if asked to
		if(mRecurse) {
		   list = filePath.listFiles(new FileFilter()	
		   	{ 
		   		public boolean accept(File pathname) { return (pathname.isDirectory() && !pathname.getName().startsWith(".")); } 
		   	} 
		   	);
			if(list != null) {	// Found some files to process
				for(int y = 0; y < list.length; y++) {
					check(list[y].getCanonicalPath());			
				}
			}
		}
	}
	
	/**
	 * Find each identifier and URL in the SPASE description contained in 
	 * a file. Then check if the identifier or URL is valid.
    *
    * @param path     the pathname of the file to parse.
    *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @since           1.0
	**/
	public void checkFile(String path)
	{
		try {
			PrintStream out = null;
			boolean	show = false;
			boolean	ok = true;
			String	prefix = "";
			
			if(mVerbose) System.out.println("Checking: " + path);

			Document doc = XMLGrep.parse(path);
			ArrayList<Pair> docIndex = XMLGrep.makeIndex(doc, "");
			 		
			if(mCheckID || mListOnly) {
		 		ArrayList<String>	idList = igpp.util.Text.uniqueList(XMLGrep.getValues(docIndex, ".*/.*ID"), true);
				if(idList != null) {
					for(String id : idList) {
						show = false;
						if(mVerbose) show = true;
						if(!mListOnly) {	// Perform lookup check
							if(mCheckID) {
								if(lookupID(id)) {
									if(mVerbose) prefix = "     [VALID] ";
								} else {
									show = true;
									ok = false;
									prefix = "   [INVALID] ";
								}
							} else {
								prefix = "   [SKIPPED] ";
							}
						}
						if(show) System.out.println(prefix + "ID: " + id);
					}
				}
			}
			
			// Check URLs
			if(mCheckURL || mListOnly) {
				ArrayList<String> urlList = igpp.util.Text.uniqueList(XMLGrep.getValues(docIndex, ".*/URL"), true);
				if(urlList != null) {
					for(String url : urlList) {
						show = false;
						if(mVerbose) show = true;
						if(!mListOnly) {	// Perform URL check
							if(mCheckURL) {
								if(checkURL(url)) {
									if(mVerbose) prefix = "     [VALID] ";
								} else {
									show = true;
									ok = false;
									prefix = "   [INVALID] ";
								}
							} else {
								prefix = "   [SKIPPED] ";
							}
						}
						if(show) System.out.println(prefix + "URL: " + url);
					}
				}
			}

			if(ok) { 
				if(!mListOnly) System.out.print("[OK]"); 
			} else { 
				System.out.print("[FAILED]"); 
			}
			System.out.println(" " + path);
		} catch( IOException ioe ) {
		  ioe.printStackTrace(); 
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/** 
	 * Read all SPASE resource descriptions at the given path.
	 * Extract each resource identifiers and add to global list.
	 * Recursively searches for resource descriptions.
    *
    * @param path     the pathname of the file to parse.
    *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @since           1.0
	**/
	public void loadResourceIndex(String path)
		throws Exception
	{
		// File name filter
	   File filePath = new File(path);
	   File[] list = new File[1];
	   if(filePath.isDirectory()) {
			list = filePath.listFiles(new FileFilter()	
		   	{ 
		   		public boolean accept(File pathname) { return pathname.getName().endsWith(mExtension); } 
		   	} 
		   	);
	   } else {
	   	list[0] = filePath;
	   }

		String resourcePath;
		if(list != null) {	// Found some files to process
			for(File item : list) {
				resourcePath = item.getCanonicalPath();
				try {
					Document doc = XMLGrep.parse(resourcePath);
					ArrayList<Pair> docIndex = XMLGrep.makeIndex(doc, "");
			 		ArrayList<String>	values = XMLGrep.getValues(docIndex, ".*/ResourceID");
			 		if(values != null && ! values.isEmpty()) mLocalList.addAll(values);
				} catch(Exception e) {
					System.out.println("Error parsing: " + resourcePath);
					System.out.println("Reason: " + e.getMessage());
				}
			}		
		}
		
		// Now recurse through tree
	   list = filePath.listFiles(new FileFilter()	
	   	{ 
	   		public boolean accept(File pathname) { return (pathname.isDirectory() && !pathname.getName().startsWith(".")); } 
	   	} 
	   	);
		if(list != null) {	// Found some files to process
			for(int y = 0; y < list.length; y++) {
				loadResourceIndex(list[y].getCanonicalPath());			
			}
		}
	}
	
	/** 
	 * Lookup an identifier
	 *
    * @param id     the resource ID string to lookup at the Registry service.
    *
	 * @return <code>true</code> if valid, <code>false</code> otherwise.
    *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @since           1.0
	**/
	public boolean lookupID(String id)
	{
		boolean valid = false;
		
		// if(mVerbose) System.out.println("Looking for: " + id);
		if(igpp.util.Text.isInList(id, mLocalList)) { // Valid
			if(mVerbose) System.out.println("Found in local list.");
			return true;
		}
		
		// Query server
		try {
			Document doc = XMLGrep.parse(mRegistry + "?c=yes&i=" + id);
			ArrayList<Pair> docIndex = XMLGrep.makeIndex(doc, "");
			// XMLGrep.writeXMLTagged(doc);
	 		// if(XMLGrep.findFirstElement(docIndex, "Known", 0) != -1) valid = true;
	 		if(XMLGrep.getFirstValue(docIndex, ".*/Known", null) != null) valid = true;
		} catch(Exception e) {
			valid = false;
			if(mVerbose) System.out.println(e.getMessage());
		}
		
		return valid;
	}
	
	/** 
	 * Check a URL by attempting to establish a connection
	 * and retrieve the header information.
	 *
    * @param urlSpec    the URL to check.
    *
    * @return <code>true</code> if valid, <code>false</code> otherwise.
    *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @since           1.0
	**/
	public boolean checkURL(String urlSpec)
	{
		boolean valid = false;
		try {
			URL url = new URL(urlSpec);
			
			if(url.getProtocol().compareToIgnoreCase("ftp") == 0) {
				valid = checkFTP(urlSpec);					
			} else {	// Try as URL
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				
				con.setRequestMethod("HEAD");
				con.connect();
		   	con.disconnect();
		   	
		   	valid = true;
			}
		} catch(Exception e) {
			valid = false;
			if(mVerbose) System.out.println(e.getMessage());
		}
		
		return valid;
	}

	/** 
	 * Check an FTP protocol URL by attempting to establish an annonymous connection.
	 *
    * @param urlSpec     the URL of an FTP request.
    *
	 * @return <code>true</code> if valid, <code>false</code> otherwise.
    *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @since           1.0
	**/
	public boolean checkFTP(String urlSpec)
	{
		SimpleFTP ftp = new SimpleFTP();
		boolean status = true;
		
		try {
			String	username = null;
			String	password = null;
			URL url  = new URL(urlSpec);
			
			// Get port information - if specified
			int port = url.getPort();
			// if(port != -1) ftp.setPort(port);
			
			// Get user information - if specified
			String userInfo = url.getUserInfo();
			if(userInfo != null) {
				String part[] = userInfo.split(":", 2);
				username = part[0];
				if(part.length > 1) password = part[1];
			}

			// Get pathname to remote file
      	String pathName = url.getFile();
      	if(pathName.startsWith("/")) pathName = pathName.substring(1);	// Drop leading slash
      	
      	// Ready to go
      	ftp.setVerboseMode(false);
      	ftp.connect(url.getHost(), username, password);
      	ftp.setVerboseMode(false);
      	
      	// ftp.getFile(pathName, out);
		
		} catch(Exception e) {
			System.out.println("Error while adding: " + urlSpec +"\nReason: " + e.getMessage() + "\n");
			status = false;
		} finally {
			if(ftp.isConnected()) {
				ftp.disconnect();
			}
		}
		
		return status;	
	}
}
