/**
 * Parses an XML file and flattens the document content.
 * Values can be retrieved using a simplied XPath that can contain regular expressions.
 * All values in the XML file can be listed with a corresponding XPath.
 * <p>
 * Development funded by NASA's VMO project at UCLA.
 *
 * @author Todd King
 * @version 1.00 2009-06-04
 */

package org.spase.tools;

import java.io.PrintStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;

import java.util.ArrayList;

import java.net.URL;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

// import org.apache.commons.cli.*;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

/**
 * Extract values from XML files using simplified XPath syntax that can contain regular expressions.
 *
 * @author Todd King
 * @author UCLA/IGPP
 * @version     1.0.0
 * @since     1.0.0
 **/
public class XMLGrep {
	private String    mVersion = "1.0.1";
	
	private static boolean mVerbose = false;
	private static boolean mXMLOutput = false;
	
	private ArrayList<String> mTagStack = new ArrayList<String>();
	
	private static String mIndent = "   ";	// Amount to indent each level of nested XML tags
	
	// create the Options
	Options mOptions = new Options();
	
	/** 
	* Creates an instance of a XML 
	*
	* @since           1.0
	**/
	public XMLGrep() 
	{
		mOptions.addOption( "h", "help", false, "Display this text." );
		mOptions.addOption( "v", "verbose", false, "Verbose. Show status at each step." );
		mOptions.addOption( "f", "find", true, "Find. Locate the value associated with an XPath." );
		mOptions.addOption( "n", "nodes", true, "Nodes. List all nodes at the given XPath." );
		mOptions.addOption( "e", "extract", true, "Extract. Extract all nodes with a given XPath." );
		mOptions.addOption( "x", "XML output", false, "XML Output. Output information as well formed XML documents." );
	}

	/**
	* Entry point for testing
	*
	* @since           1.0
	**/
	public static void main(String args[]) 
	{
		int      output = 1; // Default is XML
		XMLGrep me = new XMLGrep();
		ArrayList<Pair> list = new ArrayList<Pair>();
		String	find = null;
		String	node = null;
		String	extract = null;
		
		if (args.length < 1) {
			me.showHelp();
			System.exit(1);
		}

		CommandLineParser parser = new PosixParser();
		try { // parse the command line arguments
         CommandLine line = parser.parse(me.mOptions, args);

			if(line.hasOption("h")) me.showHelp();
			if(line.hasOption("v")) me.mVerbose = true;
			if(line.hasOption("f")) find = line.getOptionValue("f");
			if(line.hasOption("n")) node = line.getOptionValue("n");
			if(line.hasOption("e")) extract = line.getOptionValue("e");
			if(line.hasOption("x")) me.mXMLOutput = true;
				
			// Process all files
			for(String p : line.getArgs()) { 
				me.makeIndex(list, me.parse(p), "");
				if(find != null) {	// Find pattern
					ArrayList<Pair> valueList = me.getPairs(list, find);
					if(valueList.isEmpty()) {
						System.out.println("Nothing found");
					}
					if(mXMLOutput) {	me.writeXMLTagged(System.out, valueList);} 
					else { me.writeTagged(System.out, valueList); }
				} else if(node != null) {	// Find nodes
					ArrayList<String> valueList = me.getChildNodeNames(list, node);
					if(valueList.isEmpty()) {
						System.out.println("Nothing found");
					}
					for(String value : valueList) {
						System.out.println(value);
					}
				} else if(extract != null) {	// Extract a family of elements
					int startAt = 0;
					int endAt = 0;
					while(true) {
						startAt = me.findFirstElement(list, extract, startAt);
						if(startAt == -1) break;	// Done
						endAt = me.findLastElement(list, extract, startAt);
						if(me.mXMLOutput) { me.writeTagged(System.out, list, startAt, endAt); }
						else { me.writeTagged(System.out, list, startAt, endAt); }
						startAt = endAt + 1;
					}
				} else {	// Write XMLPath tagged values
					if(me.mXMLOutput) { me.writeTagged(System.out, list); }
					else { me.writeTagged(System.out, list); }
				}
			}
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
		System.out.println("XML Parser, XPath generator and search tool.");
		System.out.println("");
		System.out.println("Parses an XML file and flattens the document content.");
		System.out.println("Values can be retrieved using a simplified XPath that can contain regular expressions");
		System.out.println("for node names. All values in the XML file can be listed with a corresponding XPath.");
		System.out.println("");
		
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java " + getClass().getName() + " [options] file\n", mOptions);

		System.out.println("");
		System.out.println("Acknowledgements:");
		System.out.println("Development funded by NASA's VMO project at UCLA.");
		System.out.println("");
		System.out.println("Example");
		System.out.println("To find all items in an XML file that have an XPath ending in \"ResourceID\" use the command:");
		System.out.println("");
		System.out.println("   java " + getClass().getName() + " -f \".*/ResourceID\" example.xml");
		System.out.println("");
		System.out.println("will output something like:");
		System.out.println("");
		System.out.println("/Spase/NumericalData/ResourceID: spase://VMO/NumericalData/IMP8/MAG/PT15.36S");
		System.out.println("");
		System.out.println("To list the XPath tagged list of all content in an XML file use the command:");
		System.out.println("");
   	System.out.println("   java " + getClass().getName() + " example.xml");
		System.out.println("");
		System.out.println("will generate a list like:");
		System.out.println("");
		System.out.println("/Spase/Version: 1.3.0");
		System.out.println("/Spase/NumericalData/ResourceID: spase://VMO/NumericalData/IMP8/MAG/PT15.36S");
		System.out.println("/Spase/NumericalData/ResourceHeader/ResourceName: IMP 8 Magnetic Field");
		System.out.println("/Spase/NumericalData/ResourceHeader/ReleaseDate: 2008-06-02T18:53:44Z");
		System.out.println("... and so on ...");
	}
	
	/** 
	* Parses a file containing XML into its constitute elments.
	* The path and name of the file are passed to the method which is
	* opened and parsed.
	*
	* @param pathName  the fully qualified path and name of the file to parse.
	*
	* @return          A Document containg the pased representation.
	*                  null if parsing failed.
	*
	* @since           1.0
	**/
	public static Document parse(String pathName) 
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
     * Parses a string containing XML into its constitute elments.
     *
     * @param text	the String conting the XML text.
     *
     * @return          <code>true</code> if the file could be opened;
     *                  <code>false</code> otherwise.
     *
     * @since           1.0
     **/
    public static Document parseString(String text) 
       throws Exception 
    {
    	 ByteArrayInputStream stream = new ByteArrayInputStream(text.getBytes());
       return parse(stream);
    }

    /** 
     * Parses a file containing XML into its constitute elments.
     * The file to parse must be previously opened and a InputStream
     * pointing to the file is passed.
     *
     * @param stream     a connection to a pre-opened file.
     *
     * @return          A Document containg the pased representation.
     *                  null if parsing failed.
     *
     * @since           1.0
     **/
    public static Document parse(InputStream stream)  
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
    * Generate an xpath/value index.
    *
    * @param list     The Arraylist of riplet elements to populate.
    * @param node     the starting Node in the Document.
    * @param xpath    the XPath to the starting node.
    *
	 * @since           1.0
    **/
    public static void makeIndex(ArrayList<Pair> list, Node node, String xpath)
    {
    	if(mVerbose && node == null) { System.out.println("Node is null."); }
    	
    	if(node == null) return;
    	
    	 String	nodeName;
    	 String	nodeValue;
       Node  child;
       Node  sibling;
       
       nodeName = "";
       if(! node.getNodeName().startsWith("#")) { nodeName = "/" + node.getNodeName();	} // Tag node
       
       nodeValue = "";
       if(node.getNodeValue() != null) nodeValue = node.getNodeValue().trim();
       if(nodeValue.length() > 0) { list.add(new Pair<String, String>(xpath + nodeName, node.getNodeValue())); }
       
       if(node.hasChildNodes()) { // Output all children
          child = node.getFirstChild();
          sibling = child;
          while(sibling != null) {
             makeIndex(list, sibling, xpath + nodeName);
             sibling = sibling.getNextSibling();
          }
       }
    }
   /**
    * Generate an xpath/value/node index.
    *
    * @param node     the starting Node in the Document.
    * @param xpath    the XPath to the starting node.
    *
	 * @since           1.0
    **/
    public static ArrayList<Pair> makeIndex(Node node, String xpath)
    {
    	ArrayList<Pair> list = new ArrayList<Pair>();
    	
    	if(mVerbose && node == null) { System.out.println("Node is null."); }
    	
    	if(node == null) return list;

		makeIndex(list, node, xpath);
		
		return list;    	
    }

   /**
    * Generate an xpath/value list of pairs.
    *
    * @param node     the starting Node in the Document.
    *
    * @return          an ArrayList of Nodes which are children of the passed Node.
    *
	 * @since           1.0
    **/
	 public static ArrayList<Node> getChildNodes(Node node)
	 {
		ArrayList<Node> list = new ArrayList<Node>();
		if(mVerbose && node == null) { System.out.println("Node is null."); }
		
		if(node == null) return list;
    	
       if(node.hasChildNodes()) { // Output all children
          Node sibling = node.getFirstChild();
          while(sibling != null) {
             list.add(sibling);
             sibling = sibling.getNextSibling();
          }
       }
       
       return list;
    }
    
   /**
    * Find the Node with the given XPath
    *
    * @param node     the starting Node in the Document.
    *
    * @return          an ArrayList of Nodes which are children of the passed Node.
    *
	 * @since           1.0
    **/
	 public static ArrayList<Node> getNode(Node node)
	 {
		ArrayList<Node> list = new ArrayList<Node>();
		if(mVerbose && node == null) { System.out.println("Node is null."); }
		
		if(node == null) return list;
    	
       if(node.hasChildNodes()) { // Output all children
          Node sibling = node.getFirstChild();
          while(sibling != null) {
             list.add(sibling);
             sibling = sibling.getNextSibling();
          }
       }
       
       return list;
    }
    
	/**
 	 * Finds the first value that has an XPath which matches a given pattern.
	 *
    * @param list     a list of XPath/Value pairs.
    * @param xpath    The XPath of the value to look for. 
    *                 The XPath can contain regular expressions.
    * @param defaultValue     the default value to return if no matching item is found.
    *
    * @return          a String containing the value or the default value if
    *                  no matching XPath is found.
    *
	 * @since           1.0
	**/
    public static String getFirstValue(ArrayList<Pair> list, String xpath, String defaultValue)
    {
    	if(mVerbose) System.out.println("Looking for: " + xpath);
    	
    	for(Pair<String, String> item : list) {
    		if(item.getLeft().matches(xpath)) return item.getRight();
    	}
    	
    	return defaultValue;
    }
    
	/**
 	 * Finds all values that have an XPath which matches a given pattern.
	 *
    * @param list     a list of XPath/Value pairs.
    * @param xpath    The XPath of the value to look for. 
    *                 The XPath can contain regular expressions.
    *
    * @return          a String containing the value or the default value if
    *                  no matching XPath is found.
    *
	 * @since           1.0
	**/
    public static ArrayList<String> getValues(ArrayList<Pair> list, String xpath)
    {
    	ArrayList<String> valueList = new ArrayList<String>();
    	
    	if(mVerbose) System.out.println("Looking for: " + xpath);
    	
    	for(Pair<String, String> item : list) {
    		if(item.getLeft().matches(xpath)) valueList.add(item.getRight());
    	}
    	
    	return valueList;
    }
    
	/**
 	 * Finds all pairs that have an XPath which matches a given pattern.
	 *
    * @param list     a list of XPath/Value pairs.
    * @param xpath    The XPath of the value to look for. 
    *                 The XPath can contain regular expressions.
    *
    * @return          a String containing the value or the default value if
    *                  no matching XPath is found.
    *
	 * @since           1.0
	**/
    public static ArrayList<Pair> getPairs(ArrayList<Pair> list, String xpath)
    {
    	ArrayList<Pair> pairList = new ArrayList<Pair>();
    	
    	if(mVerbose) System.out.println("Looking for: " + xpath);
    	
    	for(Pair<String, String> item : list) {
    		if(item.getLeft().matches(xpath)) pairList.add(item);
    	}
    	
    	return pairList;
    }
    
	/**
 	 * Get a list of words in the resource.
	 *
    * @param list     a list of XPath/Value pairs.
    *
    * @return          a String containing the words in the list.
    *
	 * @since           1.0
	**/
    public static ArrayList<String> getWords(ArrayList<Pair> list)
    {
		String	delimiters = "[ \\t\\.,;:?!@#$%&=<>(){}'\"/_]";
    	ArrayList<String> words = new ArrayList<String>();
    	
    	if(mVerbose) System.out.println("Extracting word list.");
    	
    	for(Pair item : list) {
    		String buffer = item.getLeft() + " " + item.getRight();
			String[]	part = buffer.split(delimiters);
			
			for(int i = 0; i < part.length; i++) {
	 			String[] split = igpp.util.Text.splitMixedCase(part[i]);	// Split Mixed case words
				for(int j = 0; j < split.length; j++) {
					if(isCommonWord(split[j])) continue;
					if(! igpp.util.Text.isInList(split[j], words)) words.add(split[j].trim());
				}
	    	}
    	}
    	
    	return words;
    }

	/**
	* Determine if a word is a common word. 
	*
	* @since           1.0
	 **/
	public static boolean isCommonWord(String value) 
	{
		if(value.length() < 2) return true;	// Any one-letter word is "common"
		if(Character.isDigit(value.charAt(0))) return true;	// Starts with a digit
		
		if(value.compareToIgnoreCase("an") == 0) return true;
		if(value.compareToIgnoreCase("and") == 0) return true;
		if(value.compareToIgnoreCase("are") == 0) return true;
		if(value.compareToIgnoreCase("at") == 0) return true;
		if(value.compareToIgnoreCase("but") == 0) return true;
		if(value.compareToIgnoreCase("by") == 0) return true;
		if(value.compareToIgnoreCase("for") == 0) return true;
		if(value.compareToIgnoreCase("he") == 0) return true;
		if(value.compareToIgnoreCase("in") == 0) return true;
		if(value.compareToIgnoreCase("is") == 0) return true;
		if(value.compareToIgnoreCase("it") == 0) return true;
		if(value.compareToIgnoreCase("our") == 0) return true;
		if(value.compareToIgnoreCase("own") == 0) return true;
		if(value.compareToIgnoreCase("of") == 0) return true;
		if(value.compareToIgnoreCase("on") == 0) return true;
		if(value.compareToIgnoreCase("or") == 0) return true;
		if(value.compareToIgnoreCase("she") == 0) return true;
		if(value.compareToIgnoreCase("the") == 0) return true;
		if(value.compareToIgnoreCase("that") == 0) return true;
		if(value.compareToIgnoreCase("to") == 0) return true;
		if(value.compareToIgnoreCase("we") == 0) return true;
		if(value.compareToIgnoreCase("was") == 0) return true;
		if(value.compareToIgnoreCase("you") == 0) return true;
		if(value.compareToIgnoreCase("your") == 0) return true;
		
		return false;
	}

	/**
 	 * Finds the names of all child nodes at a given XPath.
	 *
    * @param list     a list of XPath/Value pairs.
    * @param xpath    The XPath of the value to look for. 
    *                 The XPath can contain regular expressions.
    *
    * @return          a String containing the value or the default value if
    *                  no matching XPath is found.
    *
	 * @since           1.0
	**/
    public static ArrayList<String> getChildNodeNames(ArrayList<Pair> list, String xpath)
    {
    	ArrayList<String> valueList = new ArrayList<String>();
    	
    	if( ! xpath.endsWith("/")) xpath += "/";	// Terminate node name
    	
    	if(mVerbose) System.out.println("Looking for: " + xpath);
    	
    	for(Pair<String, String> item : list) {
    		String nodePath = item.getLeft();
    		if(nodePath.startsWith(xpath)) {	// Extract node name
    			String buffer = nodePath.substring(xpath.length());	// Drop xpath
    			String part[] = buffer.split("/", 2);
    			if( ! igpp.util.Text.isInList(part[0], valueList)) valueList.add(part[0]);
    		}
    	}
    	
    	return valueList;
    }
    
   
	/**
 	 * Extract a copy of a portion of a ArrayList.
	 *
    * @param list     a list of XPath/Value pairs.
    * @param startAt  the index to start the extract.
    * @param endAt  the index to end the extract.
    *
    * @return          an ArrayList will all items between startAt and endAt.
    *
	 * @since           1.0
	**/
    public static ArrayList<Pair> getSegment(ArrayList<Pair> list, int startAt, int endAt)
    {
    	ArrayList<Pair> newList = new ArrayList<Pair>();
    	
    	if(startAt == -1) startAt = 0;	// Start at first element
    	if(endAt == -1) endAt = 0;	// End at first element
    	if(endAt > list.size()) endAt = list.size();
    	
    	for(int i = startAt; i < endAt; i++) {
    		newList.add(list.get(i));
    	}
    	
    	return newList;
    }
    
	/**
 	 * Find the first item that has an XPath that begins with a given XPath.
 	 * Search begins at a given index in the ArrayList.
	 *
    * @param list     a list of XPath/Value pairs.
    * @param xpath    The XPath pattern of the value to look for. 
    * @param startAt  the index to start the search.
    *
    * @return          the index in the ArrayList of the first matching element
    *                  or -1 if no matching element was found.
    *
	 * @since           1.0
	**/
    public static int findFirstElement(ArrayList<Pair> list, String xpath, int startAt)
    {
    	Pair<String, String> item;
    	if(startAt < 0) startAt = 0;	// Start at first element
    	
    	if(mVerbose) System.out.println("Looking for: " + xpath);
    	
    	int n = list.size();
    	for(int i = startAt; i < n; i++) {
    		item = list.get(i);
    		if(item.getLeft().matches(xpath)) return i;
    	}
    	
    	return -1;
    }
    
	/**
 	 * Find the last item that has an XPath that begins with a given XPath.
 	 * Search begins at a given index in the ArrayList.
	 *
    * @param list     a list of XPath/Value pairs.
    * @param xpath    The XPath pattern of the value to look for. 
    * @param startAt  the index to start the search.
    *
    * @return          the index in the ArrayList of the first matching element
    *                  or -1 if no matching element was found.
    *
	 * @since           1.0
	**/
    public static int findLastElement(ArrayList<Pair> list, String xpath, int startAt)
    {
    	Pair<String, String> item;

		if(startAt < 0) startAt = 0;	// -1 means no starting point
		    	
    	if(mVerbose) System.out.println("Looking for: " + xpath);
    	
    	int n = list.size();
    	for(int i = startAt; i < n; i++) {
    		item = list.get(i);
    		if( ! item.getLeft().matches(xpath)) return i;
    	}
    	
    	return n;
    }
    
	/**
 	 * Prints the XML document to the currently define System.out.
 	 * Each node is labeled with its XPath.
	 *
    * @param node     the starting Node in the document to print.
    *
	 * @since           1.0
	**/
	public static void writeXMLTagged(Node node) 
	{ 
		writeXMLTagged(System.out, node);
	}
		
	/**
 	 * Prints the XML document to a @link{PrintStream}
 	 * Each node is labeled with its XPath.
	 *
    * @param out     the @link{PrintStream} to send the output.
    * @param node     the starting Node in the document to print.
    *
	 * @since           1.0
	**/
    public static void writeXMLTagged(PrintStream out, Node node)
    {
    	if(mVerbose && node == null) { out.println("Node is null."); }
    	
    	if(node == null) return;
    	ArrayList<Pair> list = new ArrayList<Pair>();
    	makeIndex(list, node, "");
    	writeXMLTagged(out, list);
    	
    }
    
	/**
 	 * Prints the list of XPath/Value pairs to a @link{PrintStream}
	 *
    * @param out     the @link{PrintStream} to send the output.
    * @param list     the list of pairs to print.
    * @param startAt     the index of the first pair to print.
    * @param endAt     the index of the last pair to print.
    *
	 * @since           1.0
	**/
    public static void writeXMLTagged(PrintStream out, ArrayList<Pair> list, int startAt, int endAt)
    {
    	if(list.isEmpty()) return;
    	
    	if(startAt < 0) startAt = 0;
    	if(endAt < 0) endAt = list.size();;
    	if(endAt > list.size()) endAt = list.size();
    	
    	String[] currentPath = null;
    	String[] path = null;
    	
    	Pair<String, String> item;
    	
    	int	n, i, j, k;
    	
    	for(n = startAt; n < endAt; n++) {
    		item = list.get(n);
    		
    		// Determine context
    		String buffer = item.getLeft().substring(1);	// Drop first "/"
    		int z = buffer.lastIndexOf("/");
    		if(z != -1) buffer = buffer.substring(0, z);	// Drop last node
	    	if(buffer.length() == 0) {
	    		path = null;
	    	} else {
		    	path = buffer.split("/");
	    	}
	    	
	    	if(path == null) {
 				// Close nodes to last match
 				for(j = currentPath.length; j >= 0; j--) { 
 					for(k = 0; k < j; k++) out.print(mIndent);
 					out.println("</" + currentPath[j] + ">");
 				}
	    	} else {	// Compare and act, close nodes as needed, open nodes as necessary
		    	for(i = 0; i < path.length; i++) {
		    		if(currentPath == null || i > currentPath.length-1) {
		    			for(j = i; j < path.length; j++) {	// Print remaining nodes
		    				for(k = 0; k < j; k++) {out.print(mIndent); }
		    				out.println("<" + path[j] + ">");
		    			}
		    			break;
		    		} else {
		    			if(path[i].compareTo(currentPath[i]) != 0) {	// Different
		    				// Close nodes to last match
		    				for(j = currentPath.length-1; j > i; j--) { 
		    					for(k = 0; k < j; k++) out.print(mIndent);
		    					out.println("</" + currentPath[j] + ">");
		    				}
		    				currentPath = null;	// Clear
		    				
		    				// Add new nodes
		    				for(j = i; j < path.length; j++) { 
		    					for(k = 0; k < j; k++) out.print(mIndent);
		    					out.println("<" + path[j] + ">");
		    				}
		    				
		    				break;	// all done
		    			}
		    		}
		    	}
		    	
		    	// Close nodes beyond path
		    	if(currentPath != null) {
			    	for(i = path.length; i < currentPath.length; i++) {
	  					for(k = 0; k < i; k++) out.print(mIndent);
	  					out.println("</" + currentPath[i] + ">");
			    	}
		    	}
	    	}
	    	currentPath = path;
	    		    	
    		writeXMLTagged(out, item, true);
    	}
    	
		// Close nodes
		if(currentPath != null) {
			for(j = currentPath.length-1; j >= 0; j--) { 
				for(k = 0; k < j; k++) out.print(mIndent);
				out.println("</" + currentPath[j] + ">");
			}
		}
    }
    
	/**
 	 * Prints the list of XPath/Value pairs to a @link{PrintStream}
	 *
    * @param out     the @link{PrintStream} to send the output.
    * @param list     the list of pairs to print.
    *
	 * @since           1.0
	**/
    public static void writeXMLTagged(PrintStream out, ArrayList<Pair> list) 
    {
  		writeXMLTagged(out, list, -1, -1);
    }
    
	/**
 	 * Prints an XPath/Value pair to a @link{PrintStream}
	 *
    * @param out     the @link{PrintStream} to send the output.
    * @param item     the XPath/Value pair to print.
    * @param indent	if true then tag is indented to match nesting level.
    *
	 * @since           1.0
	**/
    public static void writeXMLTagged(PrintStream out, Pair<String, String> item, boolean indent) 
    {
    	String[] part = item.getLeft().split("/");
    
    	if(indent) { for(int i = 0; i < part.length-2; i++) { out.print("   "); }	}
    	
  		out.println("<" + part[part.length-1] + ">" + item.getRight() + "</" + part[part.length-1] + ">" );
    }
    
	/**
 	 * Prints the XML document to the currently define System.out.
 	 * Each node is labeled with its XPath.
	 *
    * @param node     the starting Node in the document to print.
    *
	 * @since           1.0
	**/
	public static void writeTagged(Node node) 
	{ 
		writeTagged(System.out, node);
	}
		
	/**
 	 * Prints the XML document to a @link{PrintStream}
 	 * Each node is labeled with its XPath.
	 *
    * @param out     the @link{PrintStream} to send the output.
    * @param node     the starting Node in the document to print.
    *
	 * @since           1.0
	**/
    public static void writeTagged(PrintStream out, Node node)
    {
    	if(mVerbose && node == null) { out.println("Node is null."); }
    	
    	if(node == null) return;
    	ArrayList<Pair> list = new ArrayList<Pair>();
    	makeIndex(list, node, "");
    	writeTagged(out, list);
    	
    }
    
	/**
 	 * Prints the list of XPath/Value pairs to a @link{PrintStream}
	 *
    * @param out     the @link{PrintStream} to send the output.
    * @param list     the list of pairs to print.
    * @param startAt     the index of the first pair to print.
    * @param endAt     the index of the last pair to print.
    *
	 * @since           1.0
	**/
    public static void writeTagged(PrintStream out, ArrayList<Pair> list, int startAt, int endAt)
    {
    	if(list.isEmpty()) return;
    	
    	if(startAt < 0) startAt = 0;
    	if(endAt < 0) endAt = 0;
    	if(endAt > list.size()) endAt = list.size();
    	
    	Pair<String, String> item;
    	
    	for(int i = startAt; i < endAt; i++) {
    		item = list.get(i);
    		out.println(item.getLeft() + ": " + item.getRight());
    		
    	}
    }
    
	/**
 	 * Prints the list of XPath/Value pairs to a @link{PrintStream}
	 *
    * @param out     the @link{PrintStream} to send the output.
    * @param list     the list of pairs to print.
    *
	 * @since           1.0
	**/
    public static void writeTagged(PrintStream out, ArrayList<Pair> list) 
    {
    	for(Pair<String, String> item : list) {
    		writeTagged(out, item);
    	}
    }
    
	/**
 	 * Prints an XPath/Value pair to a @link{PrintStream}
	 *
    * @param out     the @link{PrintStream} to send the output.
    * @param item     the XPath/Value pair to print.
    *
	 * @since           1.0
	**/
    public static void writeTagged(PrintStream out, Pair item) 
    {
  		out.println(item.getLeft() + ": " + item.getRight());
    }
}
