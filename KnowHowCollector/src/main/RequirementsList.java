package main;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class RequirementsList {

	String name;
	List<RequirementRepresentation> requirements = new LinkedList<RequirementRepresentation>();
	public Set<String> links = new HashSet<String>();
	public RequirementsList(String name) {
		this.name = name;
	}
	
	public String pretty_print(){
		String s = "["+name+"]";
		for (RequirementRepresentation st : requirements) s += " "+st.text+";";
		return s;
	}

}
