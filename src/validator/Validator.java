/**
 * Validates a SPASE description using the appropriate version of the data model.
 *
 * @author Todd King
 * @version $Id: Validator.java 1 2010-04-30 17:24:57Z todd-king $
 */

package org.spase.tools;

import java.io.PrintStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Hashtable;

import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;

// import org.apache.commons.cli.*;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

public class Validator extends DefaultHandler
{
	private String	mVersion = "1.0.3";
	
	private String	mXsdVersion = "2.1.0";
	private String	mXsdUrl = null;
	private boolean mVerbose = false;
	private Schema	mSchema = null;
	private boolean mRecurse = false;
	String	mExtension = ".xml";

	// For parsering	
	private int		mLine;
	private String	mLastError = "";
	private String	mLastWarning = "";
	private String mPathName = "";
	private boolean mPrintName = true;
			
	// create the Options
	Options mOptions = new Options();

	public Validator() 
	{
		mOptions.addOption( "h", "help", false, "Display this text" );
		mOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step" );
		mOptions.addOption( "s", "schema", true, "URL or Path to the XML schema document (XSD) to use for checking files.");
		mOptions.addOption( "n", "version", true, "Version of standard schema to use available from www.spase-group.org (default: " + mXsdVersion + ")..");
		mOptions.addOption( "r", "recurse", false, "Recursively process all files starting at path.");
		mOptions.addOption( "x", "ext", true, "File name extension for filtering files when processing folders (default: " + mExtension + ")");
	}
		
    /** 
	 * Validate a SPASE resource description using a specified version of the
	 * data dictionary.  
	 *<p>
	 * Usage:<blockquote>
	 *     Validate url version
	 * </blockquote>
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 */
	public static void main(String args[])
   {
		Validator me = new Validator();
		   
		if (args.length < 1) {
			me.showHelp();
			System.exit(1);
		}

		CommandLineParser parser = new PosixParser();
		try { // parse the command line arguments
         CommandLine line = parser.parse(me.mOptions, args);

			if(line.hasOption("h")) me.showHelp();
			if(line.hasOption("v")) me.mVerbose = true;
			if(line.hasOption("r")) me.mRecurse = true;
			if(line.hasOption("s")) me.mXsdUrl = line.getOptionValue("s");
			if(line.hasOption("n")) me.mXsdVersion = line.getOptionValue("n");
			if(line.hasOption("x")) me.mExtension = line.getOptionValue("x");
				
			// Load schema if not set
			if(me.mXsdUrl != null) {	// Load schema from "local" source
				me.loadSchema(me.mXsdUrl);
			} else {	// Load from network source - using version number
				me.loadSchemaFromVersion(me.mXsdVersion);
			}
			
			// Process all files
			for(String p : line.getArgs()) { me.validate(p); }
			
		} catch( ParseException e ) { // oops, something went wrong
	      System.out.println( "Parsing failed.  Reason: " + e.getMessage() );
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
   }

	/**
	 * Display help information.
	 **/
	public void showHelp()
	{
		System.out.println("");
		System.out.println(getClass().getName() + "; Version: " + mVersion);
		System.out.println("SPASE Resource Description grammar checker.");
		System.out.println("");
		System.out.println("Checks a resource description for compliance to a specified version");
		System.out.println("of the SPASE data model.");
		System.out.println("");
		
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java " + getClass().getName() + " [options] file", mOptions);

		System.out.println("");
		System.out.println("Acknowledgements:");
		System.out.println("Development funded by NASA's VMO project at UCLA.");
		System.out.println("");
		System.out.println("Example");
		System.out.println("To validate the SPASE description in the file \"example.xml\" to version 2.0.0 of the SPASE schema use the command:");
		System.out.println("");
		System.out.println("   java " + getClass().getName() + " -n 2.0.0 example.xml");
		System.out.println("");
		System.out.println("The schema will be loaded from the web site www.spase-group.org. If you are not one a network you can use a local XML Schema document with the command:");
		System.out.println("");
		System.out.println("   java " + getClass().getName() + " -s spase-2_0_0.xsd example.xml");
	}
	
    /** 
	 * Set the schema location based on version number to use.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 10/07/08
	 * @since		1.0
	 */
 	public void loadSchemaFromVersion(String version) {
 		version = version.replace(".", "_");
	 	loadSchema("http://www.spase-group.org/data/schema/spase-" + version + ".xsd");
	}
	
    /** 
	 * Load a schema for validating an XML document.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 **/
	public void loadSchema(String xsdUrl)
	{
		if(mVerbose) println("Loading schema: " + xsdUrl);
		try {
			String language = XMLConstants.W3C_XML_SCHEMA_NS_URI;
			SchemaFactory schemaFactory = SchemaFactory.newInstance(language);
			StreamSource ss = null;
			try {	// Try as URL
				ss = new StreamSource(xsdUrl);
			} catch(Exception e) {
				try {	// Try as local file
					ss = new StreamSource(new FileInputStream(xsdUrl));
				} catch (Exception e2) {
					throw e2;
				}
			} finally {
				mSchema = schemaFactory.newSchema(ss);
			}
		} catch(Exception e) {
			println("Using URL: " + xsdUrl);
			println("Error: " + e.getMessage());
		}
	}
         
	/** 
	 * Perform a validation on an XML document or all documents in
	 * a directory sing the the currently loaded schema. 
	 * Optinally scan a directory recursively.
	 * Errors are caught and output with the line number where the error
	 * occurred. After processing the XML document is output in an table format
	 * with each line numbered and those lines with errors highlighted.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 **/
	public void validate(String path)
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
					validateFile(resourcePath);
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
					if(mVerbose) println("Scanning directory: " + list[y].getCanonicalPath());
					validate(list[y].getCanonicalPath());			
				}
			}
		}
	}

    /** 
	 * Perform a validation on an XML document using the the currently loaded 
	 * schema. Errors are caught and output with the line number where the error
	 * occurred. After processing the XML document is output in an table format
	 * with each line numbered and those lines with errors highlighted.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 */
	public void validateFile(String path)
		throws Exception
	{        
		boolean	good = true;
		boolean	printName = true;
		InputStream	inputStream = null;
		
		// Use a validating parser with namespaces
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);	// 'false' so we can validate with our specified schema
		factory.setNamespaceAware(true);
		factory.setSchema(mSchema);
		
		// Validate the file
		// open document
		if(mVerbose) print("Validating: ");
		try {
			URL file = new URL(path);
			inputStream = file.openStream();
		} catch(Exception e) {
			// Try as a file
			inputStream = new FileInputStream(path);
		}
		
		mPathName = path;
		if(mVerbose) { print(mPathName + " "); mPrintName = false; }
		
		try {
			Reader reader = new InputStreamReader(inputStream, "UTF-8");
     	   InputSource source = new InputSource(reader);
  	      source.setEncoding("UTF-8");
			
			// Parse the input
			SAXParser saxParser = factory.newSAXParser();
			// saxParser.setProperty("http://xml.org/sax/features/validation", true);
			// saxParser.parse(inputStream, this);
			saxParser.parse(source, this);
		} catch (SAXParseException err) {
			// Error generated by the parser
			if(mPrintName) { println(mPathName); mPrintName = false; }
			println("Line " + err.getLineNumber() + " " + err.getMessage());
			println("");
			good = false;
		} catch (SAXException err) {
			// Error generated by this application
			// (or a parser-initialization error)
			if(mPrintName) { println(mPathName); mPrintName = false; }
			println("Error: " + err.getMessage());
			println("");
			good = false;
		} catch (ParserConfigurationException pce) {
			// Parser with specified options can't be built
			if(mPrintName) { println(mPathName); mPrintName = false; }
			pce.printStackTrace();
		} catch (IOException err) {
			// I/O error
			if(mPrintName) { println(mPathName); mPrintName = false; }
			println("Error: " + err.getMessage());
			println("");
			good = false;
		}
		
		inputStream.close();
		
		if(good) { if(mVerbose) println("[OK]"); }
		else { println("[FAILED]"); }
		
		mPathName = "";
    }

    //===========================================================
    // SAX DocumentHandler methods
    //===========================================================

    /** 
	 * Called by the XML parser when the URL for document to validate
	 * is set.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 */
    public void setDocumentLocator(Locator l)
    {
      // Save this to resolve relative URIs or to give diagnostics.
    	// if(withHTML()) println("<b>");
    	// println(l.getSystemId());
    	// if(withHTML()) println("</b><p>");
    }

    /** 
	 * Called by the XML parser at the start or processing.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 */
    public void startDocument()
    throws SAXException
    {
    	mLine = 0;
    }

    /** 
	 * Called by the XML parser when validation is complete.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 */
	public void endDocument()
    throws SAXException
    {
    }

    /** 
	 * Called by the XML parser when a opening element is encountered.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 */
    public void startElement(String namespaceURI,
                             String lName, // local name
                             String qName, // qualified name
                             Attributes attrs)
    throws SAXException
    {
    }

    /** 
	 * Called by the XML parser when a closing element is encountered.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 */
    public void endElement(String namespaceURI,
                           String sName, // simple name
                           String qName  // qualified name
                          )
    throws SAXException
    {
    }

    /** 
	 * Called by the XML parser when a character is encountered
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 */
    public void characters(char buf[], int offset, int len)
    throws SAXException
    {
    }

    /** 
	 * Called by the XML parser when a processing instruction is encountered.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 */
    public void processingInstruction(String target, String data)
    throws SAXException
    {
    }

    //===========================================================
    // SAX ErrorHandler methods
    //===========================================================

    /** 
	 * Called by the XML parser when a validation error occurs.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 */
    public void error(SAXParseException err)
    throws SAXParseException
    {
    	String buffer = "";
    	
  		if(mPrintName) { println(mPathName); mPrintName = false; }
		buffer = "Line " + err.getLineNumber() + ": " + err.getMessage();
    	if(mLastError.compareTo(buffer) == 0) return;	// Repeated error
    	
    	println(buffer);
    	println("");
    	mLastError = buffer;
    }

    /** 
	 * Called by the XML parser when a validation warning occurs.
	 *
	 * @author Todd King
	 * @author UCLA/IGPP
	 * @version     1.0, 07/18/06
	 * @since		1.0
	 */
    public void warning(SAXParseException err)
    throws SAXParseException
    {
		String	buffer = "";
		buffer = "Line " + err.getLineNumber() + ": " + err.getMessage();
		
		if(mLastWarning.compareTo(buffer) == 0) return;	// Repeated warning
		
		if(mPrintName) { println(mPathName); mPrintName = false; }
		println(buffer);
		println("");
		mLastWarning = buffer;    	
    }
    
    //===========================================================
    // Utlity functions
    //===========================================================
	public void println(String line)
	{
		System.out.println(line);
	}
	
	public void print(String line)
	{
		System.out.print(line);
	}
}