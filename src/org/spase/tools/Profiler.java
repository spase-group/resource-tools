/**
 * Scans SPASE descriptions and generates profiles
 * which can be submitted to a solr search engine.
 *<p>
 * Development funded by NASA's VMO project at UCLA.
 *
 * @author Todd King
 * @version 1.00 2009-06-04
 */

package org.spase.tools;

import igpp.util.Date;

import org.spase.tools.XMLGrep;
import org.spase.tools.Pair;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Iterator;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintStream;

// import org.apache.commons.cli.*;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

public class Profiler
{
	static String	mVersion = "1.0.1";

	boolean	mVerbose = false;
	
	String	mExtension = ".xml";
	String	mRegistryID = "";
	boolean	mRecurse = false;
	String mLookup = "http://www.spase-group.org/registry/resolver";
	
	PrintStream	mOut = System.out;
	
	String	mFindPrefix = null;
	String	mFindReplace = null;

	// create the Options
	Options mAppOptions = new org.apache.commons.cli.Options();

	public Profiler() {
		mAppOptions.addOption( "h", "help", false, "Dispay this text" );
		mAppOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step." );
		mAppOptions.addOption( "r", "recurse", false, "Recurse. Process all items in the current folder. Recurse into sub-folders.");
		mAppOptions.addOption( "f", "file", true, "File. File containing a list of file names to scan." );
		mAppOptions.addOption( "o", "output", true, "Output. Output generated profiles to {file}. Default: System.out." );
		mAppOptions.addOption( "l", "lookup", true, "Lookup. The URL to the resource lookup service to resolve resource IDs. Default: " + mLookup);
		mAppOptions.addOption( "x", "extension", true, "Extension. The file name extension for files to process (default: " + mExtension + ")" );
		mAppOptions.addOption( "i", "id", true, "ID. The registry ID to set for each resource" );
	}	
   /** 
	 * Command-line interface.
	 *
	 * @since		1.0
	 **/
	public static void main(String args[])
   {
   	String	filename = null;
   	String	outfile = null;
   	String	prefix = null;
   	
		Profiler me = new Profiler();
		
		if (args.length < 1) {
			me.showHelp();
			System.exit(1);
		}

		
		CommandLineParser parser = new PosixParser();
		try { // parse the command line arguments
         CommandLine line = parser.parse(me.mAppOptions, args);

			if(line.hasOption("h")) me.showHelp();
			if(line.hasOption("v")) me.mVerbose = true;
			if(line.hasOption("r")) me.mRecurse = true;
			if(line.hasOption("f")) filename = line.getOptionValue("f");
			if(line.hasOption("o")) outfile = line.getOptionValue("o");
			if(line.hasOption("p")) prefix = line.getOptionValue("p");
			if(line.hasOption("i")) me.mRegistryID = line.getOptionValue("i");
			
			if(outfile != null) {
				me.mOut = new PrintStream(outfile);
			}
			
			// Parse prefix if given
			if(prefix != null) {
				String part[] = prefix.split("::", 2);
				if(part.length != 2) {
					System.out.println("Error in prefix. Proper syntax is \"find::replace\" which will substitute \"find\" with \"replace\".");
				} else {
					me.mFindPrefix = part[0];
					me.mFindReplace = part[1];
				}
			}
			
			
			me.mOut.println("<!--");
			me.mOut.println("  SOLR \"add\" messages for SPASE resource profiles.");
			me.mOut.println("-->");
			me.mOut.println("<add>");
			// Process all files
			if(filename != null) {	// Process items in file
				if(me.mVerbose) System.out.println("Processing list from file: " + filename);
				me.makeProfileFromFile(filename);
			}
			
			// Process other command line areguments
			for(String p : line.getArgs()) { 
				if(me.mVerbose) System.out.println("Processing: " + p);
				me.makeProfile(p);
			}
			me.mOut.println("</add>");
			
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
		System.out.println("Profile generator. Create resource profiles for SPASE resource descriptions.");
		System.out.println("Profiles all have a common schema which can be used in a solr search engine.");
		System.out.println("");
		
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java " + getClass().getName() + " [options] [file...]", mAppOptions);

		System.out.println("");
		System.out.println("Acknowledgements:");
		System.out.println("Development funded by NASA's VMO project at UCLA.");
		System.out.println("");
		System.out.println("Example");
		System.out.println("To create profiles for all resources in the current directory and below use the command:");
		System.out.println("");
		System.out.println("   java " + getClass().getName() + " -o /temp/vmo.xml -r *");
		System.out.println("");
		System.out.println("The profiles will be written to the file \"/temp/vmo.xml\". The profiles can then be posted the appropriate solr search engine.");
		System.out.println("");
	}
	

	/** 
	 * Read a list of file names from a file and load each one.
	 * Lines beginning with a "#" are considered comments and ignored
    *
    * @param path		the pathname of the file to parse.
    *
	 * @since           1.0
    **/
	public void makeProfileFromFile(String path)
		throws Exception
	{
		if(path == null) return;
		
		File file = new File(path);
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		
		String	buffer;
		while((buffer = reader.readLine()) != null) {
			if(buffer.startsWith("#")) continue;	// Comment
			makeProfile(buffer);
		}
		
	}
	
	/** 
	 * Read all SPASE resource descriptions at the given path.
	 * Resource descriptions are files that have a defined extension.
	 * The path to the resources can be recusively searched.
    *
    * @param path		the pathname of the file to parse.
    *
	 * @since           1.0
    **/
	public void makeProfile(String path)
		throws Exception
	{
		if(path == null) return;
				
		String	resourcePath = path;
		Document	doc;
		ArrayList<Node> resourceList;
		ArrayList<Pair> docIndex = new ArrayList<Pair>();
		ResourceProfile profile;
		
		File filePath = new File(resourcePath);
		
	   File[] list = null;
	   
	   if(filePath.isFile()) {
	   	list = new File[1];
	   	list[0] = filePath;
	   } else {	// try as directory
	   	list = filePath.listFiles(new FileFilter()	
	   	{ 
	   		public boolean accept(File pathname) { return pathname.getName().endsWith(mExtension); } 
	   	} 
	   	);
	   }

		if(list != null) {	// Found some files to process
			for(int y = 0; y < list.length; y++) {
				resourcePath = list[y].getCanonicalPath();
				if(mVerbose) System.out.println(resourcePath);
				try {
					doc = org.spase.tools.XMLGrep.parse(resourcePath);
					docIndex = org.spase.tools.XMLGrep.makeIndex(doc, "");
				} catch(Exception e) {
					System.out.println("Error parsing: " + resourcePath);
					System.out.println(e.getMessage());
				}
				String version = org.spase.tools.XMLGrep.getFirstValue(docIndex, "/Spase/Version", null);
				int startAt = -1;
				int endAt = -1;
				
				// Load all other resources - if any
				String[] dataTags = new String[] {"NumericalData", "DisplayData", "Catalog"};
				for(String tagName : dataTags) {
					while(true) {
						startAt = org.spase.tools.XMLGrep.findFirstElement(docIndex, "/Spase/" + tagName + "/.*", startAt);
						if(startAt == -1)	break;	// All done
						endAt = org.spase.tools.XMLGrep.findLastElement(docIndex, "/Spase/" + tagName + "/.*", startAt);
						profile = makeResourceProfile(docIndex, version, tagName, startAt, endAt);
						if(profile != null) {
							setInstrumentInfo(profile);	// Must preceed setObservatoryInfo()
							setObservatoryInfo(profile);
							profile.setAuthorityFromResourceID();
							profile.setRegistryID(mRegistryID);
							profile.setResourceType(tagName);
							profile.normalize();	// Fill in blank fields with defaults.
							profile.printSolrProfile(mOut); 
							// storeProfile(profile); 
						}
						startAt = endAt + 1;
					}
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
					makeProfile(list[y].getCanonicalPath());			
				}
			}
		}
	}
	
	/**
	 * Set the instrument information from the instrument ID
    *
    * @param profile		the {ResourceProfile} to update.
    *
	 * @since           1.0
    **/
	public void setInstrumentInfo(ResourceProfile profile)
		throws Exception
	{
		Document	doc;
		ArrayList<Pair> segment = null;
		String descURL = mLookup;	// Path to common info (Instrument/Observatory)
		
		if(profile.getInstrumentID().length() == 0) return;
		
		String part[] = profile.getInstrumentID().split("://", 2);	// Split URN. It should start with "spase:".
		//		descURL += part[1] + ".xml";	// Path portion;
		descURL += "?id=" + part[1];

		doc = org.spase.tools.XMLGrep.parse(descURL);	// Load and parse file.
		segment = org.spase.tools.XMLGrep.makeIndex(doc, "");

		profile.setInstrumentName(org.spase.tools.XMLGrep.getValues(segment, "/Spase/Instrument/ResourceHeader/ResourceName"));
		profile.setInstrumentType(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/Instrument/InstrumentType", ""));
		profile.setObservatoryID(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/Instrument/ObservatoryID", ""));
		profile.addWords(org.spase.tools.XMLGrep.getWords(segment));
	}
	
	/**
	 * Set the observatory information from the observatory ID
    *
    * @param profile		the {ResourceProfile} to update.
    *
	 * @since           1.0
    **/
	public void setObservatoryInfo(ResourceProfile profile)
		throws Exception
	{
		Document	doc;
		ArrayList<Pair> segment = null;
		String descURL = mLookup;	// Path to common info (Instrument/Observatory)
		
		if(profile.getObservatoryID().length() == 0) return;
		
		String part[] = profile.getObservatoryID().split("://", 2);	// Split URN. It should start with "spase:".
		// descURL += part[1] + ".xml";	// Path portion;
		descURL += "?id=" + part[1];
		
		doc = org.spase.tools.XMLGrep.parse(descURL);	// Load and parse file.
		segment = org.spase.tools.XMLGrep.makeIndex(doc, "");
		
		profile.setObservatoryGroup(org.spase.tools.XMLGrep.getValues(segment, "/Spase/Observatory/ObservatoryGroup"));
		profile.setObservatoryName(org.spase.tools.XMLGrep.getValues(segment, "/Spase/Observatory/ResourceHeader/ResourceName"));
		
		profile.setObservatoryType("Spacecraft");	// If Region is not specified assume to be spacecraft
		
		String region = org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/Observatory/Location/ObservatoryRegion", null);
		if(region != null) {	// Region specified
			if(region.compareToIgnoreCase("Earth.Surface") == 0) { // Groundbased
				profile.setObservatoryType("Groundbased");
				profile.setLatitude(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/Observatory/Location/Latitude", ""));
				profile.setLongitude(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/Observatory/Location/Longitude", ""));
			} else {	// Spacecraft
				profile.setObservatoryType("Spacecraft");
			}
		}
		profile.addWords(org.spase.tools.XMLGrep.getWords(segment));
	}
	
	/**
	 * Extract information from a {Pair} list of values and store in a new resource profile.
    *
    * @param list		the @link{ArrayList} of @link{Pair} calues to scan.
    * @param version		the version of SPASE metadata schema the list was extracted form.
    * @param resourceTagName		the resource type to search for.
    * @param startAt		the index of the item in the list to start the search.
    * @param endAt		the index of the item in the list to end the search.
    *
    * @return          a populated @link{ResourceProfile}
    *
	 * @since           1.0
    **/
	public ResourceProfile makeResourceProfile(ArrayList<Pair> list, String version, String resourceTagName, int startAt, int endAt)
	{
		ArrayList<Pair> segment = org.spase.tools.XMLGrep.getSegment(list, startAt, endAt);
		ResourceProfile profile = new ResourceProfile();
		
		profile.setResourceID(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/ResourceID", ""));
		profile.setResourceName(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/ResourceHeader/ResourceName", ""));
		profile.setReleaseDate(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/ResourceHeader/ReleaseDate", ""));
		
		profile.setCadence(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TemporalDescription/Cadence", ""));
		
		profile.setInstrumentID(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/InstrumentID", ""));
		
		profile.setObservedRegion(org.spase.tools.XMLGrep.getValues(segment, "/Spase/" + resourceTagName + "/ObservedRegion"));
		profile.setPhenomenonType(org.spase.tools.XMLGrep.getValues(segment, "/Spase/" + resourceTagName + "/PhenomenonType"));
		profile.setMeasurementType(org.spase.tools.XMLGrep.getValues(segment, "/Spase/" + resourceTagName + "/MeasurementType"));
		
		profile.setDescription(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/ResourceHeader/Description", ""));
		
		// Note: Catalog use "TimeSpan", others use "TemporalDescription"
		profile.setStartDate(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TimeSpan/StartDate", ""));
		
		if(version.startsWith("1.2.") || version.startsWith("1.1.")) {
			if(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TimeSpan/RelativeEndDate", null) != null) {
				profile.setStopDate(igpp.util.Date.getISO8601DateString(igpp.util.Date.parseISO8601Duration(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TimeSpan/RelativeEndDate", ""))));
			} else {
				profile.setStopDate(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TimeSpan/EndDate", ""));
			}
		} else {
			if(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TimeSpan/RelativeStopDate", null) != null) {
				profile.setStopDate(igpp.util.Date.getISO8601DateString(igpp.util.Date.parseISO8601Duration(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TimeSpan/RelativeStopDate", ""))));
			} else {
				profile.setStopDate(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TimeSpan/StopDate", ""));
			}
		}
		
		// Now try with TemporalDescription
		profile.setStartDate(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TemporalDescription/TimeSpan/StartDate", profile.getStartDate()));
		
		if(version.startsWith("1.2.") || version.startsWith("1.1.")) {
			if(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TemporalDescription/TimeSpan/RelativeEndDate", null) != null) {
				profile.setStopDate(igpp.util.Date.getISO8601DateString(igpp.util.Date.parseISO8601Duration(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TemporalDescription/TimeSpan/RelativeEndDate", ""))));
			} else {
				profile.setStopDate(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TemporalDescription/TimeSpan/EndDate", ""));
			}
		} else {
			if(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TemporalDescription/TimeSpan/RelativeStopDate", null) != null) {
				profile.setStopDate(igpp.util.Date.getISO8601DateString(igpp.util.Date.parseISO8601Duration(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TemporalDescription/TimeSpan/RelativeStopDate", ""))));
			} else {
				profile.setStopDate(org.spase.tools.XMLGrep.getFirstValue(segment, "/Spase/" + resourceTagName + "/TemporalDescription/TimeSpan/StopDate", ""));
			}
		}
		
		// Words
		profile.setWords(org.spase.tools.XMLGrep.getWords(segment));
		
		// Associations
		String[] idTags = new String[] {"InputResource", "Instrument", "Observatory", "Parent", "Prior", "Repository", "Registry"};
		for(String tagName : idTags) {
			startAt = 0;
			String pattern = ".*/" + tagName + "ID/.*";	// Find tags anywhere in xPath
			ArrayList<String> values = org.spase.tools.XMLGrep.getValues(segment, pattern);
			profile.addAssociation(values);
		}

		
		return profile;
	}
}
