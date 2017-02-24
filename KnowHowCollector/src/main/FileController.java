package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import com.hp.hpl.jena.sparql.util.Utils;

public class FileController {

	private static final String prohow = "prohow:";
	
	private static String encapsulateLongLiteral(String longLiteral) {
		// remove three " in a row
		if (longLiteral.indexOf("\"\"\"") > 0) {
			longLiteral = longLiteral.replaceAll("\"", "''");
		}
		// duplicate all backslashes
		longLiteral = (longLiteral.replace("\\", "a\"\"\"a")).replace("a\"\"\"a", "\\\\");
		if (longLiteral.lastIndexOf("\"") == (longLiteral.length() - 1)) {
			return "\"\"\"" + longLiteral + " \"\"\"@en";
		} else {
			return "\"\"\"" + longLiteral + "\"\"\"@en";
		}
	}

	private static String encapsulateURI(String stringToEncapsulate) {
		return "<" + stringToEncapsulate + ">";
	} 
	
	private static String timestamp(){
		return "\""+Utils.calendarToXSDDateTimeString(Calendar.getInstance())+"\"^^xsd:dateTime";
	}
	
	
	protected static synchronized void logPicture(String instructionString, String pictureURL, String localName) throws IOException{
		createFolders(new String[] {"data",instructionString,"logs"});
		String writer = "data\\"+instructionString+"\\logs\\imageLog.ttl";
		saveTriple(encapsulateURI(pictureURL),prohow+"has_local","\"data/"+instructionString+"/images/"+localName+"\"",writer);
		saveTriple(encapsulateURI(pictureURL),prohow+"has_extraction_timestamp",timestamp(),writer);
	}
	
	protected static synchronized void logHTML(String instructionString, String pageURL, String localName, String multilingualCode) throws IOException{
		createFolders(new String[] {"data",instructionString,"logs"});
		String writer = "data\\"+instructionString+"\\logs\\pageLog.ttl";
		if(multilingualCode != null && multilingualCode.length() > 0)
			writer = "data\\"+instructionString+"\\logs\\pageLog-"+multilingualCode+".ttl";
		saveTriple(encapsulateURI(pageURL),prohow+"has_local","\"data/"+instructionString+"/web/"+localName+"\"",writer);
		saveTriple(encapsulateURI(pageURL),prohow+"has_extraction_timestamp",timestamp(),writer);
		
	}
	
	
	
	protected static synchronized void saveTriple(String s, String p, String o, String fwriter) throws IOException {
		boolean newFile = ! new File(fwriter).isFile();
		
		FileWriter writer = new FileWriter(fwriter, true);
		
		if(newFile)
			writer.write("@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n" +
					"@prefix prohow: <http://vocab.inf.ed.ac.uk/prohow#> .\n" + s + " " + p + " " + o + " .\n");
		else
			writer.write(s + " " + p + " " + o + " .\n");
		writer.flush();
		writer.close();
	}
	
	// if the directory does not exist, create it
	protected static void createFolders(String[] nestedFolders){
		String directoryPath = null;
		for(String s : nestedFolders){
			if (directoryPath == null) directoryPath = s ;
			else directoryPath = directoryPath + "\\" + s ;
			File theDir = new File(directoryPath);
			if (!theDir.exists()) {
			    Logger.log("INFO: Creating directory: " + directoryPath);
			    boolean result = false;
			    try{
			        theDir.mkdir();
			        result = true;
			     } catch(SecurityException se){
			    	 Logger.log("ERROR: Cannot create directory: " + theDir.getAbsolutePath());
			    	 throw se;
			     }        
			     if(result) {    
			    	 Logger.log("INFO: Created directory: " + theDir.getAbsolutePath()); 
			     }
			  }
		}
	}

}
