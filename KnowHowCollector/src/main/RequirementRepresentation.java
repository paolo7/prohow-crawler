package main;

import java.util.HashSet;
import java.util.Set;

import org.jsoup.select.Elements;

public class RequirementRepresentation {

	public String text;
	public Set<String> links = new HashSet<String>();
	public RequirementRepresentation(String text) {
		this.text = text;
	}

}
