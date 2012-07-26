/**
 * Reads a SPASE resource description, extracts each resource 
 * description and places it the the appropriate location 
 * in the file system according to its resource ID.
 * <p>
 * Development funded by NASA's VMO project at UCLA.
 *
 * @author Todd King 
 * @version $Id: Collator.java 1 2010-04-30 17:24:57Z todd-king $
 */

package org.spase.tools;

import igpp.util.Encode;

import igpp.xml.Pair;
import igpp.xml.XMLGrep;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.net.URL;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.FileFilter;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;


// import org.apache.commons.cli.*;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

public class Collator
{
	private String	mVersion = "1.0.0";

	String	mBasePath = ".";
	boolean	mVerifyOnly = false;
	boolean	mVerbose = false;
	String	mExtension = ".xml";
	boolean	mRecurse = false;

	// create the Options
	Options mOptions = new Options();

	public Collator() 
	{
		mOptions.addOption( "h", "help", false, "Display this text" );
		mOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step" );
		mOptions.addOption( "k", "check", false, "Check files, but do not write collated output.");
		mOptions.addOption( "r", "recurse", false, "Recursively process all files starting at path.");
		mOptions.addOption( "b", "base", true, "Base path for collated output (default: " + mBasePath + ").");
		mOptions.addOption( "x", "ext", true, "File name extension for filtering files when processing folders (default: " + mExtension + ")");
	}
		
   /** 
	 * Command-line interface.
	 *
	 * @since		1.0
	 **/
	public static void main(String args[])
   {
		Collator me = new Collator();
		
		if(args.length < 1) {
			me.showHelp();
			return;
		}
		
		// create the command line parser
		CommandLineParser parser = new PosixParser();
		try { // parse the command line arguments
         CommandLine line = parser.parse(me.mOptions, args);

			if(line.hasOption("h")) me.showHelp();
			if(line.hasOption("k")) me.mVerifyOnly = true;
			if(line.hasOption("v")) me.mVerbose = true;
			if(line.hasOption("r")) me.mRecurse = true;
			if(line.hasOption("b")) me.mBasePath = line.getOptionValue("b");
			if(line.hasOption("x")) me.mExtension = line.getOptionValue("x");
			
			// Load resource at path
			for(String p : line.getArgs()) { me.distill(p); }
		} catch( ParseException e ) { // oops, something went wrong
	      System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
		} catch(Exception e) {
			e.printStackTrace();
		}
   }

	/**
	 * Display help information.
    *
	 * @since		1.0
	 **/
   public void showHelp()
   {
		System.out.println("");
		System.out.println(getClass().getName() + "; Version: " + mVersion);
 		System.out.println("Separate each SPASE resource description in a file into separate files stored");
 		System.out.println("in a folder tree according to the Resource ID.");
 		System.out.println("");
 		System.out.println("Optionally recursively scan a directory for all files with a given extension");
 		System.out.println("and process each file.");
		System.out.println("");

		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java " + getClass().getName() + " [options] file\n", mOptions );

		System.out.println("");
		System.out.println("Acknowledgements:");
		System.out.println("Development funded by NASA's VMO project at UCLA.");
		System.out.println("");
		System.out.println("Example");
 		System.out.println("Suppose a file (example.xml) contains three SPASE descriptions like:");
		System.out.println("");
		System.out.println("<Spase>");
   	System.out.println("   <Version>2.0.0</Version>");
   	System.out.println("   <NumericalData>");
  		System.out.println("      <ResourceID>spase://VMO/NumericalData/GeoTail/LEP/PT60S</ResourceID>");
  		System.out.println("         ... Details omitted ...");
  		System.out.println("   </NumericalData>");
		System.out.println("   <NumericalData>");
		System.out.println("      <ResourceID>spase://VMO/NumericalData/GeoTail/MGF/PT60S</ResourceID>");
		System.out.println("          ... Details omitted ...");
		System.out.println("   </NumericalData>");
		System.out.println("   <NumericalData>");
		System.out.println("      <ResourceID>spase://VMO/NumericalData/GeoTail/CPI/PT60S</ResourceID>");
		System.out.println("          ... Details omitted ...");
		System.out.println("   </NumericalData>");
		System.out.println("</Spase>");
		System.out.println("");
		System.out.println("The a command like:");
		System.out.println("");
		System.out.println("   java " + getClass().getName() + " example.xml");
		System.out.println("");
		System.out.println("will generate three files with the path names of:");
		System.out.println("");
		System.out.println("      ./VMO/NumericalData/GeoTail/LEP/PT60S.xml");
		System.out.println("      ./VMO/NumericalData/GeoTail/MGF/PT60S.xml");
		System.out.println("      ./VMO/NumericalData/GeoTail/CPI/PT60S.xml");
		System.out.println("");
		System.out.println("Which each will contain the description for each corresponding resource.");
	}

	/** 
	 * Read all SPASE resource descriptions at the given path.
	 * Extract each resource description is then extracted and
	 * re-written in the location defined by the Resource Identifier.
	 * The files with the given extension is parsed for resource descriptions.
	 * The path to the resources can be recusively searched.
    *
    * @param path     the pathname of the file to distill.
    *
	 * @since           1.0
	**/
	public void distill(String path)
		throws Exception
	{
		if(mVerbose) System.out.println("Distilling: " + path);
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
					separate(resourcePath);
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
					distill(list[y].getCanonicalPath());			
				}
			}
		}
	}

	/**
	 * Separate each resource description contained in a file.
	 *
	 * The file may contain multiple resource in a single <Spase> document
	 * and multiple <Spase> documents.
	 *
	 * @param pathName  the path and file name to the file to process.
	 *
	 * @since           1.0
	 **/
	public void separate(String pathName)
		throws Exception
	{
		Document doc = parse(pathName);

    	if(mVerbose && doc == null) { System.out.println("Node is null."); }
    	
    	if(doc == null) return;
    	
    	separate(doc);
	}
	
	/**
	 * Separate each resource description contained in a Node of Document.
	 *
	 * The file may contain multiple resource in a single <Spase> document
	 * and multiple <Spase> documents.
	 *
	 * @param node the starting node in a Document.
	 *
	 * @since           1.0
	 **/
	public void separate(Node node) 
		throws Exception
	{
		String version = "";
		String spaseTag = "Spase";
		
		if(! node.getNodeName().startsWith("#")) { 
			if(node.getNodeName().compareTo("Spase") == 0) {	// Spase document
			   spaseTag = getFullTag(node);
				version = getNodeValue(node, "Version");
				extractResource(node, spaseTag, version);
			}
		}
		
		if(node.hasChildNodes()) { // Output all children
			Node child = node.getFirstChild();
			Node sibling = child;
			while(sibling != null) {
				separate(sibling);
				sibling = sibling.getNextSibling();
			}
		}
	}
	
	/** 
	 * Return a string for the full tag text which includes any specified attributes.
	 *
	 * @param node	the {@link Node} to process.
	 *
	 * @since           1.0
	 **/
	public String getFullTag(Node node)
	{
		String tag = node.getNodeName();
		
		NamedNodeMap attrib = node.getAttributes();
		
		if(attrib == null) return tag;
		
		for(int i = 0; i < attrib.getLength(); i++) {
			Node item = attrib.item(i);
			tag += " " + item.getNodeName() + "=\"" + item.getNodeValue() + "\"";
		}
		
		return tag;
	}
	/**
	 * Search for a tag with a given name starting at the specified Node
	 *
	 * @param node   the {@link Node} to start the search for the tag.
	 * @param tag	  the name of the tag to find.
	 *
	 * @since           1.0
	 **/
	public String getNodeValue(Node node, String tag)
	{
		String nodeValue = null;
		
		if(! node.getNodeName().startsWith("#")) { 
			if(node.getNodeName().compareTo(tag) == 0) {	// Matching tag
		       nodeValue = getValue(node);
		       if(nodeValue != null) nodeValue = nodeValue.trim();
		       return nodeValue;
			}
		}
		
		// Recursively search
		if(node.hasChildNodes()) { // Output all children
			Node child = node.getFirstChild();
			Node sibling = child;
			while(sibling != null) {
				nodeValue = getNodeValue(sibling, tag);
				if(nodeValue != null) return nodeValue;
				sibling = sibling.getNextSibling();
			}
		}
		
		return null;
	}
	
	/**
	 * Get the content of a node which is all non-tag name nodes
	 *
	 * @param node    the {@link Node} for which the text content will be extracted.
	 *
	 * @since           1.0
	 **/
	public String getValue(Node node)
	{
		if(! node.hasChildNodes()) return ""; // No value
		
		String	value = "";
		
		Node child = node.getFirstChild();
		Node sibling = child;
		while(sibling != null) {
			if(sibling.getNodeName().startsWith("#")) { 
				value += sibling.getNodeValue();
			}
			sibling = sibling.getNextSibling();
		}
		
		return value;
	}
	
	/** 
	 * Extract each resource under a node and write to an appropriately named
	 * file.
	 *
	 * The output is wrapped with the passed Spase tag text and will include
	 * a version tag with the passed version text.
	 *
	 * @param parent	the {@link Node} to begin the extraction.
	 * @param spaseTag  the text for the opening Spase tag.
	 * @param version		the version information for the resource.
	 *
	 * @since           1.0
	 **/
	public void extractResource(Node parent, String spaseTag, String version)
		throws Exception
	{
		if(! parent.hasChildNodes()) return; // No children
		
		String	value = "";
		PrintStream out = System.out;
				
		Node child = parent.getFirstChild();
		Node sibling = child;
		while(sibling != null) {
			if( ! sibling.getNodeName().startsWith("#")) { // A tag
				if(sibling.getNodeName().compareTo("Version") == 0) { sibling = sibling.getNextSibling(); continue; }	// Skip version tags
				String resourceID = getNodeValue(sibling, "ResourceID");
				if(mVerbose) System.out.println("Processing ResourceID: " + resourceID);
				String pathname = makeResourcePath(mBasePath, resourceID);
				
				// Open file
				File file = new File(pathname);
				File dirPath = new File(file.getParent());
				if(file.exists()) { if(mVerbose) System.out.println("Overwriting: " + pathname); }
				else { if(mVerbose) System.out.println("Writing to: " + pathname); }
				if(mVerifyOnly) {
					if(mVerbose) out = System.out;
				} else {	// Create path and open file
					boolean state = dirPath.mkdirs();	// Create all necessary directories
					out = new PrintStream(new FileOutputStream(file));
				}
				
				out.println("<" + spaseTag + ">");
				out.println("<Version>" + version + "</Version>");
				extractTree(out, sibling);
				out.println("");
				out.println("</Spase>");
			}
			sibling = sibling.getNextSibling();
		}
	}
	
	/**
	 * Output all values under the passed node formatted as XML.
	 *
	 * @param out	the {@link PrintStream} to write the formatted text to.
	 * @param node	the {@link Node} where the extraction is to begin.
	 *
	 * @since           1.0
	 **/
	public void extractTree(PrintStream out, Node node)
	{
		if(node.getNodeName().compareTo("Version") == 0) return;	// Skip version tags.
		
		out.print("<" + node.getNodeName() + ">");
		
		Node sibling = node.getFirstChild();
		while(sibling != null) {
			if(sibling.getNodeName().startsWith("#")) { 
				out.print(igpp.util.Encode.htmlEncode(sibling.getNodeValue()));
			} else {	// Node name
				extractTree(out, sibling);
			}
			sibling = sibling.getNextSibling();
		}
		
		out.print("</" + node.getNodeName() + ">");
	}
	
	/** 
	 * Parse an XML document in a file.
	 *
	 * @param pathName  the path and file name to the file to process.
	 *
	 * @since           1.0
	 **/
	public Document parse(String pathName) 
	 throws Exception 
	{
		InputStream stream;
		boolean        status;
		Document		document;
		
		if(mVerbose) { System.out.println("Parsing: " + pathName); }
		
		try {	// Try as URL
			URL file = new URL(pathName);
			stream = file.openStream();
		} catch(Exception e) {
			// Try as a file
			stream = new FileInputStream(pathName);
		}
		
		document = parse(stream);
		stream.close();
		
		return document;
	}

	/** 
	 * Parse an XML document referenced by a pre-opened InputStream.
	 *
	 * @param stream	the {@link InputStream} pointing to the document to parse.
	 *
	 * @since           1.0
	 **/
    public Document parse(InputStream stream)  
       throws Exception
    {
    	  Document document;
    	  
    	  // Make sure we use the right document builder
	     System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
	     
        DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();

        // Configure parser
        factory.setValidating(false);   
        factory.setNamespaceAware(false);
        factory.setIgnoringComments(false);
        factory.setCoalescing(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(stream);
        if(document != null) document.normalizeDocument();

        return document;
    }
	
	/** 
	 * Create a full path to a resource using VxO rules. 
	 * The protocol is striped from the ResourceID and the base path
	 * is preprended.
    *
    * @param base     the base path to prepent to the authority/path portion of the resourceID.
    * @param resourceID   The resourceID to parse and munge.
    *
	 * @since           1.0
	**/
	public String makeResourcePath(String base, String resourceID)
	{
		if(base == null || resourceID == null) return null;
		if(resourceID.indexOf("..") != -1) return null;	// Gaurd against path spoofing
		
		int n = resourceID.indexOf("://");
		if(n != -1) resourceID = resourceID.substring(n+3);
		return base + "/" + resourceID + ".xml";
	}
}
