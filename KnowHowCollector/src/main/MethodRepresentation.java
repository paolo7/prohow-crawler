package main;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MethodRepresentation {

	String text = null;
	List<StepRepresentation> steps = new LinkedList<StepRepresentation>();
	public Set<String> links = new HashSet<String>();
	public MethodRepresentation(String text) {
		this.text = text;
	}
	
	public String pretty_print() {
		String s = "Method: "+text+"\n";
		if (steps.size() > 0){
			s += "   - Steps:\n";
			for (StepRepresentation step : steps){
				s += "      - "+step.pretty_print()+"\n";
			   }
			}
		return s;
		}
	
    public void define_steps(List<StepRepresentation> steps){
    	this.steps = steps;
    }
}
