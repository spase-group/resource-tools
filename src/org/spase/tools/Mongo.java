/**
 * Reads a SPASE resource description and generates Mongo (JSON) version. 
 * <p>
 * Development funded by NASA's VMO project at UCLA.
 *
 * @author Todd King
 * @version $Id: RefCheck.java 1 2014-01-07 17:24:57Z todd-king $
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.FileFilter;
import java.io.IOException;

import org.w3c.dom.Node;
import org.w3c.dom.Document;

import java.util.Iterator;



// import org.apache.commons.cli.*;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

public class Mongo
{
	private String	mVersion = "1.0.0";

	boolean	mVerbose = false;
	String	mExtension = ".xml";
	boolean	mRecurse = false;
	
	// create the Options
	Options mOptions = new Options();

    /** The Character '&amp;'. */
    public static final Character AMP   = new Character('&');

    /** The Character '''. */
    public static final Character APOS  = new Character('\'');

    /** The Character '!'. */
    public static final Character BANG  = new Character('!');

    /** The Character '='. */
    public static final Character EQ    = new Character('=');

    /** The Character '>'. */
    public static final Character GT    = new Character('>');

    /** The Character '&lt;'. */
    public static final Character LT    = new Character('<');

    /** The Character '?'. */
    public static final Character QUEST = new Character('?');

    /** The Character '"'. */
    public static final Character QUOT  = new Character('"');

    /** The Character '/'. */
    public static final Character SLASH = new Character('/');

	public Mongo() 
	{
		mOptions.addOption( "h", "help", false, "Display this text" );
		mOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step" );
		mOptions.addOption( "r", "recurse", false, "Recursively process all files starting at path.");
	}
		
   /** 
    * Command line entry point.
	 **/
	public static void main(String args[])
   {
		Mongo me = new Mongo();
		
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
			
			// Load resource at path
			for(String p : line.getArgs()) { System.out.println(""); me.process(p); }
			
		} catch( ParseException e ) { // oops, something went wrong
	      System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
		} catch(Exception e) {
			e.printStackTrace();
		}
   }

	/**
	 * Display help information.
    *
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
	 * Read all SPASE resource descriptions at the given path
	 * and generate a Mongo (JSON) version for each file.
	 * The path to the resources can be recursively searched.
     *
     * @param path     the pathname of the file to parse.
     *
	 * @since           1.0
	**/
	public void process(String path)
		throws Exception
	{
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
					String buffer = readFile(resourcePath);
					System.out.println(org.json.XML.toJSONObject(buffer).toString());
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
					process(list[y].getCanonicalPath());			
				}
			}
		}
	}
	
	public String readFile(String path)
	{
		String buffer = "";
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
			String b;
			while((b = reader.readLine()) != null) {
				buffer += b;
			}
			reader.close();
		} catch(Exception e) {
			System.out.print(e.getMessage());
		}
		
		return buffer;
	}
}
