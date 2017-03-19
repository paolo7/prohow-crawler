package main;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.hp.hpl.jena.sparql.util.Utils; 

public class SaveFiles {

	/*"oa":      "http://www.w3.org/ns/oa#",
    "dc":      "http://purl.org/dc/elements/1.1/",
    "dcterms": "http://purl.org/dc/terms/",
    "dctypes": "http://purl.org/dc/dcmitype/",
    "foaf":    "http://xmlns.com/foaf/0.1/",
    "rdf":     "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs":    "http://www.w3.org/2000/01/rdf-schema#",
    "skos":    "http://www.w3.org/2004/02/skos/core#",
    "xsd":     "http://www.w3.org/2001/XMLSchema#",
    "iana":    "http://www.iana.org/assignments/relation/",
    "owl":     "http://www.w3.org/2002/07/owl#",
    "as":      "http://www.w3.org/ns/activitystreams#",
    "schema":  "http://schema.org/",*/
	
/*
@prefix w: <http://w3id.org/prohowlinks#> .
@prefix oa: <http://www.w3.org/ns/oa#> .
@prefix prohow: <http://w3id.org/prohow#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix dbo: <http://dbpedia.org/ontology/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
    */
	
	private String[] result_folder;
	private String base_uri;
	private long sequence = 0;
	private Long max_file_size;
	
	public String type_task = "prohow:task"; 
	public String type_main = "prohow:instruction_set";
	public String type_complete_instructions = "prohow:complete_instructions";
	
	public String type_req_cons = "prohow:consumable"; 
	public String type_req = "prohow:requirement";
	
	public String rel_has_step = "prohow:has_step";
	public String rel_has_method = "prohow:has_method";
	public String rel_requires = "prohow:requires";
	public String rel_requires_sufficient = "prohow:requires_one";
	
	public String rel_has_link = "prohow:linked_to";
	
	
	// web annotations
	public String rel_has_body = "oa:hasBody";
	public String rel_has_target = "oa:hasTarget";
	public String rel_creation_time = "xsd:dateTime";
	public String rel_subclass_property = "rdfs:subClassOf";
	public String rel_same_as = "owl:sameAs";
	
	public Map<String,Integer> ttl_file_indexes = new HashMap<String,Integer>();
	public Map<String,Integer> source_file_indexes = new HashMap<String,Integer>();	
	
	public Map<String,String> subclass_relations = new HashMap<String,String>();
	
	public SaveFiles(String result_folder, String base_uri, Long max_file_size) {
		String[] path = {result_folder};
		this.result_folder = path;
		this.base_uri = base_uri;
		this.max_file_size = max_file_size;
	}
	
	protected void createFolders(String[] nestedFolders){
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
	
	public void save_hierarchy() throws IOException{
		createFolders(result_folder);
		File hierarchy_file = new File(result_folder[0]+File.separator+"class_hierarchy.ttl");
		hierarchy_file.createNewFile();
		Writer hierarchy_writer = new OutputStreamWriter(new FileOutputStream(hierarchy_file, false), "UTF-8");
		for (String key : subclass_relations.keySet()){
			hierarchy_writer.write("<"+key+"> "+rel_subclass_property+" <"+subclass_relations.get(key)+"> .\n");
		}
		hierarchy_writer.close();
	}
	
/*	public void zip_and_destroy(File f) throws IOException{
		FileOutputStream dest = new FileOutputStream(f.getAbsolutePath()+".zip");
	    ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest)); 
	    ZipEntry entry = new ZipEntry(f.getAbsolutePath());
	    out.putNextEntry(entry);
	    int count;
	    while((count = origin.read(data, 0, BUFFER)) != -1) {
	       out.write(data, 0, count);
	    } 
	}*/
	public static void zip_and_destroy_file(File inputFile) {
        try {
        	String zipFilePath = inputFile.getAbsolutePath()+".zip";
            FileOutputStream fileOutputStream = new FileOutputStream(zipFilePath);
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
            ZipEntry zipEntry = new ZipEntry(inputFile.getName());
            zipOutputStream.putNextEntry(zipEntry);
            FileInputStream fileInputStream = new FileInputStream(inputFile);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buf)) > 0) {
                zipOutputStream.write(buf, 0, bytesRead);
            }
            zipOutputStream.closeEntry();
            zipOutputStream.close();
            fileOutputStream.close();
            fileInputStream.close();
            inputFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
	
	public void save_article(ArticleRepresentation article, Document source, String url) throws IOException{
		long size_limit = max_file_size.longValue();
		String lang = article.language_code;
		createFolders(result_folder);
		if(!ttl_file_indexes.containsKey(lang)) ttl_file_indexes.put(lang, new Integer(0));
		if(!source_file_indexes.containsKey(lang)) source_file_indexes.put(lang, new Integer(0));			
		int index_result_file = ttl_file_indexes.get(lang);
		File result_file = new File(result_folder[0]+File.separator+lang+"_"+index_result_file+"_rdf_result.ttl");
		result_file.createNewFile();
		while( result_file.length() > size_limit){
			index_result_file++;
			result_file = new File(result_folder[0]+File.separator+lang+"_"+index_result_file+"_rdf_result.ttl");
			result_file.createNewFile();
		}
		Writer result_writer = new OutputStreamWriter(new FileOutputStream(result_file, true), "UTF-8");
		ttl_file_indexes.put(lang, new Integer(index_result_file));
		
		File link_step_file = new File(result_folder[0]+File.separator+lang+"_rdf_step_links.ttl");
		File link_req_file = new File(result_folder[0]+File.separator+lang+"_rdf_requirement_links.ttl");
		link_step_file.createNewFile();
		link_req_file.createNewFile();
		Writer link_step_writer = new OutputStreamWriter(new FileOutputStream(link_step_file, true), "UTF-8");
		Writer link_req_writer = new OutputStreamWriter(new FileOutputStream(link_req_file, true), "UTF-8");
		
		save_article_element(article, result_writer,link_step_writer,link_req_writer);
		result_writer.close();
		link_step_writer.close();
		link_req_writer.close();
		
		// write the source html file to disk
		int index_source_file = source_file_indexes.get(lang);
		File source_files = new File(result_folder[0]+File.separator+lang+"_"+index_source_file+"source_file.txt");
		source_files.createNewFile();
		while( source_files.length() > size_limit){
			zip_and_destroy_file(source_files);
			index_source_file++;
			source_files = new File(result_folder[0]+File.separator+lang+"_"+index_source_file+"source_file.txt");
			source_files.createNewFile();
		}
		Writer source_writer = new OutputStreamWriter(new FileOutputStream(source_files, true), "UTF-8");
		source_file_indexes.put(lang, new Integer(index_source_file));
		
		source.select("script").remove();
		source.select("style").remove();
		source_writer.append("<"+article.url+"> phwqmb:has_source_html \"\"\"\n"+source.html()+"\n\"\"\" . \n");
		source_writer.close();
	}
	
	public void save_article_element(ArticleRepresentation article, Writer writer , Writer step_l_writer , Writer req_l_writer) throws IOException{
		String lc = article.language_code;
		String article_uri = mint_prefixed_uri();
		//create_type_triple(article_uri,type_complete_instructions);
		writer.write(create_type_triple(article_uri,type_main));
		if (article.title != null) {
			writer.write(create_label_triple(article_uri,article.title,lc));
		}
		if (article.abstract_description != null) {
			writer.write(create_abstract_triple(article_uri,article.abstract_description,lc));
		}
		// save annotation metadata
		// https://www.w3.org/TR/annotation-vocab/
		String annotation = mint_prefixed_uri();
		writer.write(create_triple(annotation, rel_creation_time, generate_currentxsd_datetime()));
		writer.write(create_triple(annotation, rel_has_body, article_uri));
		if (article.url != null) {
			writer.write(create_triple(annotation, rel_has_target, encapsulte_uri(article.url)));
		}
		write_methods(article.methods, article_uri, writer,step_l_writer, lc);
		write_requirements(article.ingredients,article_uri,writer,req_l_writer,lc, type_req_cons);
		write_requirements(article.requirements,article_uri,writer,req_l_writer,lc, type_req);
		String most_recent_and_specific_category = null;
		for (String cat : article.categories){
			if (cat.equals("http://www.wikihow.com/Main-Page") || cat.equals("http://www.wikihow.com/Special:Categorylisting")){				
			} else {	
				if (most_recent_and_specific_category != null) {
					subclass_relations.put(cat, most_recent_and_specific_category);
				}
				most_recent_and_specific_category = cat;
			} 
			//subclass_relations
		}
		if (most_recent_and_specific_category != null) {
			writer.write(create_type_triple(article_uri, encapsulte_uri(urlify(most_recent_and_specific_category))));
		}
		for (String link : article.language_links){
			writer.write(create_triple(encapsulte_uri(article.url.toLowerCase()), rel_same_as, encapsulte_uri(link.toLowerCase())));
		}
	}
	
	public String urlify(String s){
		return s;
	}
	
	public void write_requirements(List<RequirementsList> requirements, String uri, Writer writer, Writer req_l_writer,  String language_code, String req_type) throws IOException{
		for (RequirementsList req_list : requirements) {
			String requirements_uri = mint_prefixed_uri();
			writer.write(create_triple(uri, rel_requires, requirements_uri));
			writer.write(create_label_triple(requirements_uri, req_list.name, language_code));
			writer.write(create_type_triple(requirements_uri, type_complete_instructions));
			for (String link : req_list.links){
				req_l_writer.write(create_triple(requirements_uri,rel_has_link,link.toLowerCase()));
			}
			for (RequirementRepresentation requirement : req_list.requirements){
				String req_uri = mint_prefixed_uri();
				writer.write(create_triple(requirements_uri, rel_has_step, req_uri));
				writer.write(create_label_triple(req_uri, requirement.text, language_code));
				writer.write(create_type_triple(req_uri, req_type));
				for (String link : requirement.links){
					req_l_writer.write(create_triple(req_uri,rel_has_link,link.toLowerCase()));
				}
			}
		}
	}
	
	public void write_methods(Set<MethodRepresentation> methods, String uri, Writer writer, Writer step_l_writer, String language_code) throws IOException{
		for (MethodRepresentation method : methods) {
			String method_uri = mint_prefixed_uri();
			writer.write(create_triple(uri, rel_has_method, method_uri));
			writer.write(create_label_triple(method_uri, method.text, language_code));
			//writer.write(create_type_triple(method_uri, type_complete_instructions));
			write_steps(method.steps,method_uri,writer,step_l_writer,language_code);
			for (String link : method.links){
				step_l_writer.write(create_triple(method_uri,rel_has_link,link.toLowerCase()));
			}
		}
	}
	
	public void write_steps(List<StepRepresentation> steps, String uri, Writer writer, Writer step_l_writer, String language_code) throws IOException{
		String previous_step_uri = null;
		for (StepRepresentation step : steps) {
			String step_uri = mint_prefixed_uri();
			writer.write(create_triple(uri, rel_has_step, step_uri));
			writer.write(create_label_triple(step_uri, step.text, language_code));
			if(step.abstract_description != null){				
				writer.write(create_abstract_triple(step_uri, step.abstract_description, language_code));
			}
			write_methods(step.methods,step_uri,writer,step_l_writer,language_code);
			if(previous_step_uri != null)
				writer.write(create_triple(step_uri, rel_requires, previous_step_uri));
			previous_step_uri = step_uri;
			for (String link : step.links){
				step_l_writer.write(create_triple(step_uri,rel_has_link,link.toLowerCase()));
			}
		}
	}
	
	public String generate_currentxsd_datetime(){
		return "\""+Utils.calendarToXSDDateTimeString(Calendar.getInstance())+"\"^^xsd:dateTime";
	}
	
	public String create_type_triple(String uri, String type){
		return create_triple(uri,"rdf:type",type);
	}
	public String create_label_triple(String uri, String label,String language_code){
		return create_triple(uri,"rdfs:label",encapsulateLongLiteral(label,language_code));
	}
	public String create_abstract_triple(String uri, String label, String language_code){
		//http://dbpedia.org/ontology/abstract
		Document doc = Jsoup.parse(label);
		return create_triple(uri,"dbo:abstract",encapsulateLongLiteral(doc.text(),language_code))+create_triple(uri,"prohow:html_abstract",encapsulateLongLiteral(label,language_code));
		
	}
	
	public String encapsulte_uri(String uri){
		return "<"+uri+">";
	}
	
	public String create_triple(String subject, String predicate, String object){
		return subject+" "+predicate+" "+object+" .\n";
	}
	
	public String mint_uri(){
		sequence++;
		return base_uri+sequence+"e"+new Date().getTime();
	}
	
	public String mint_prefixed_uri(){
		sequence++;
		return "w:"+sequence+"e"+new Date().getTime();
	}

	/*public class Closer implements Closeable {
		  private Closeable closeable;

		  public <T extends Closeable> T using(T t) {
		    closeable = t;
		    return t;
		  }

		  @Override public void close() throws IOException {
		    if (closeable != null) {
		      closeable.close();
		    }
		  }
		}
	
	private static void writeUtf8ToFile(File file, boolean append, String data)
		      throws IOException {
		    boolean skipBOM = append && file.isFile() && (file.length() > 0);
		    Closer res = new Closer();
		    try {
		      OutputStream out = res.using(new FileOutputStream(file, append));
		      Writer writer = res.using(new OutputStreamWriter(out, Charset
		          .forName("UTF-8")));
		      if (!skipBOM) {
		        writer.write('\uFEFF');
		      }
		      writer.write(data);
		    } finally {
		      res.close();
		    }
		  }*/
	
	private String encapsulateLongLiteral(String longLiteral, String language_code) {
		// remove three " in a row
		if (longLiteral.indexOf("\"\"\"") > 0) {
			longLiteral = longLiteral.replaceAll("\"", "''");
		}
		// duplicate all backslashes
		longLiteral = (longLiteral.replace("\\", "a\"\"\"a")).replace("a\"\"\"a", "\\\\");
		if (longLiteral.lastIndexOf("\"") == (longLiteral.length() - 1)) {
			return "\"\"\"" + longLiteral + " \"\"\"@"+language_code;
		} else {
			return "\"\"\"" + longLiteral + "\"\"\"@"+language_code;
		}
	}

}
