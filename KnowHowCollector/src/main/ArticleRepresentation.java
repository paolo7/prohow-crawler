package main;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArticleRepresentation {

	public final String language_code;
	public final String url;
	public String title = null;
	public String abstract_description = null;
	public Set<MethodRepresentation> methods = new HashSet<MethodRepresentation>();
	Map<String,ArticleRepresentation> other_languages = new HashMap<String,ArticleRepresentation>();
	List<RequirementsList> requirements = new LinkedList<RequirementsList>();
	List<RequirementsList> ingredients = new LinkedList<RequirementsList>();
	Set<String> language_links = new HashSet<String>();
	List<String> categories = new LinkedList<String>();
	
	public ArticleRepresentation(String url, String language_code) {
		this.language_code = language_code;
		this.url = url;
	}
	public String pretty_print() {
		String s = "Article: ("+language_code+") "+url+"\n";
		s = s + "Title: "+title+"\n";
		s = s + "Abstract: "+abstract_description+"\n";
		if (methods != null){
			s = s + "Methods list:\n";
			for(MethodRepresentation method : methods){
				s = s + "- "+method.pretty_print();
				
			}
		}
		if(requirements.size() > 0){
			s = s + "Requirements:\n";
			for (RequirementsList requirement : requirements){
				s = s + "   * "+requirement.pretty_print()+"\n";
			}
		}
		if(ingredients.size() > 0){
			s = s + "Ingredients:\n";
			for (RequirementsList ingredient : ingredients){
				s = s + "   * "+ingredient.pretty_print()+"\n";
			}
		}
		if(language_links.size() > 0){
			s = s + "Other links:\n";
			for (String link : language_links) 
				s = s + "<"+link+">\n";
		}
		s = s + "Categories:\n";
		for (String c : categories){
			s = s + " - "+c;
		}
		s = s + "\n";
		return s;
	}
	
	//public void add_steps(List<String> steps){
	//	this.steps = steps;
	//}

}
