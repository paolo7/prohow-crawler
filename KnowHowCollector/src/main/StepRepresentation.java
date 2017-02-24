package main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StepRepresentation {

	public final String text;
	public String abstract_description = null;
	public Set<MethodRepresentation> methods = new HashSet<MethodRepresentation>();
	public Set<String> links = new HashSet<String>();
	public StepRepresentation(String text) {
		this.text = text;
	}
	
	public void set_abstract_description(String s){
		abstract_description = s;
	}
	
	public String pretty_print() {
		String s = "Step: "+text+"\n";
		if (methods.size() > 0){
			s += "   - Methods:\n";
			for (MethodRepresentation method : methods){
				s += "      - "+method.pretty_print()+"\n";
			   }
			}
		return s;
		}
}
