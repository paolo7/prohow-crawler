package main;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class HTMLparserWikiHow {

	public static String default_step_list_name = "Main Steps";
	public static void parse(ArticleRepresentation article, Document pageDocument){
		article.title = get_title(pageDocument);
		article.abstract_description = get_abstract(pageDocument);
		// try in case the article is a simple list of steps
		List<StepRepresentation> simple_step_list = get_steps(pageDocument);
		if (simple_step_list.size() > 0){
			MethodRepresentation method = new MethodRepresentation(default_step_list_name);
			method.define_steps(simple_step_list);
			article.methods.add(method);
		} else {
			// try in case the article is a list of methods or parts
			Set<MethodRepresentation> methods = get_composite(pageDocument);
			article.methods.addAll(methods);
			/*if (methods.size() == 0){
				methods = get_composite_alternative(pageDocument);
				article.methods.addAll(methods);
			}*/
		}
		// find requirements
		article.requirements.addAll(get_requirements(pageDocument));
		article.ingredients.addAll(get_ingredients(pageDocument));	
		// find in other languages
		article.language_links.addAll(get_language_links(pageDocument));
		// find categories
		article.categories.addAll(get_categories_branch(pageDocument));
	}
	
	private static List<String> get_categories_branch(Document pageDocument){
		List<String> branch = new LinkedList<String>();
		Element breadcrumb = pageDocument.getElementById("breadcrumb");
		if(breadcrumb != null){			
			Elements hrefs = breadcrumb.select("a");
			for (Element href : hrefs) branch.add(href.attr("abs:href"));
		}
		return branch;
	}
	
	private static Set<String> get_language_links(Document pageDocument){
		Set<String> links_found = new HashSet<String>();
		Element article_info_box = pageDocument.getElementById("article_info");
		if (article_info_box != null){			
			Elements info_boxes = article_info_box.select("p.info");
			for (Element info_box : info_boxes){
				String info_box_text = info_box.text();
				if(info_box_text.contains("English") || info_box_text.contains("Français") || info_box_text.contains("Español")
						|| info_box_text.contains("Deutsch") || info_box_text.contains("Português") || info_box_text.contains("Italiano") || info_box_text.contains("Nederlands") 
						|| info_box_text.contains("Русский") || info_box_text.contains("中文") || info_box_text.contains("Čeština") || info_box_text.contains("Bahasa Indonesia") 
						|| info_box_text.contains("ไทย") || info_box_text.contains("العربية") || info_box_text.contains("हिन्दी") || info_box_text.contains("한국어") 
						|| info_box_text.contains("Tiếng Việt") || info_box_text.contains("日本語") ) {				
					Elements hrefs = info_box.select("a");
					for (Element href : hrefs) links_found.add(href.attr("abs:href"));
				}
			}
		}
		return links_found;
	}
	
	
	private static Set<MethodRepresentation> get_composite_alternative(Document pageDocument){
		Set<MethodRepresentation> methods = new HashSet<MethodRepresentation>();
		List<StepRepresentation> parts = new LinkedList<StepRepresentation>();
		//MethodRepresentation standard_method = new MethodRepresentation(default_step_list_name);
		
		Iterator<Element> composite_blocks = pageDocument.select("h2").iterator();
		while(composite_blocks.hasNext()){
			Element composite_block = composite_blocks.next();
			Iterator<Element> inner_headlines = composite_block.select("span.mw-headline").iterator();
			boolean is_method = false;
			boolean is_part = false;
			while(inner_headlines.hasNext()){
				Element inner_alt_block = inner_headlines.next();
				String inner_text = inner_alt_block.text().toLowerCase();
				if (inner_text.contains("part") || inner_text.contains("part")
						) is_part = true;
				if (inner_text.contains("method") || inner_text.contains("vorgehensweise")
						) is_method = true;
			}
			if (!is_method && !is_part) is_method = true;
			
			String block_name = composite_block.select(".mw-headline").text();
			if ((is_method && ! is_part) || (is_part && ! is_method) ) {
				Element next_sibling = composite_block.nextElementSibling();
				List<StepRepresentation> step_list = new LinkedList<StepRepresentation>();
				while(next_sibling!= null && step_list.size() == 0){
					step_list = extract_steps_list(next_sibling);
					next_sibling = next_sibling.nextElementSibling();
				}
				if (step_list.size() > 0){					
					if (is_method && ! is_part) {
						MethodRepresentation method = new MethodRepresentation(block_name);
						method.define_steps(step_list);
						methods.add(method);
					}
					if (is_part && ! is_method) {
						StepRepresentation part = new StepRepresentation(block_name);
						MethodRepresentation standard_method = new MethodRepresentation(default_step_list_name);
						standard_method.define_steps(step_list);
						part.methods.add(standard_method);
						parts.add(part);
					}
				}
				
			}
		}
		if (parts.size() > 0){
			MethodRepresentation standard_method = new MethodRepresentation(default_step_list_name);
			standard_method.define_steps(parts);
			methods.add(standard_method);
		}
		return methods;
	}
	
	private static void extract_links(Set<String> links, Elements elements){
		Elements anchors = elements.select("a");
		for (Element a : anchors){
			if ((!a.hasClass("image")) || (!a.hasClass("lightbox"))){				
				String href = a.attr("abs:href");
				if (a.text().contains("[") && href.indexOf("#_note-") > 0){
					
				} else {
					if (href.length() > 0){
						links.add(href);
					}					
				}
			}
		}
	}
	
	private static Set<MethodRepresentation> get_composite(Document pageDocument){
		Set<MethodRepresentation> methods = new HashSet<MethodRepresentation>();
		List<StepRepresentation> parts = new LinkedList<StepRepresentation>();
		//MethodRepresentation standard_method = new MethodRepresentation(default_step_list_name);
		
		Iterator<Element> composite_blocks = pageDocument.select("h3").iterator();
		while(composite_blocks.hasNext()){
			Element composite_block = composite_blocks.next();
			Iterator<Element> inner_alt_blocks = composite_block.select("div.altblock").iterator();
			boolean is_method = false;
			boolean is_part = false;
			while(inner_alt_blocks.hasNext()){
				Element inner_alt_block = inner_alt_blocks.next();
				String inner_text = inner_alt_block.text().toLowerCase(); 
				if (inner_text.contains("part") || inner_text.contains("teil") || inner_text.contains("parte") || inner_text.contains("část")
						|| inner_text.contains("bagian") || inner_text.contains("パート") || inner_text.contains("deel") || inner_text.contains("parte")
						|| inner_text.contains("часть") || inner_text.contains("جزء") || inner_text.contains("ส่วน") || inner_text.contains("phân")
						|| inner_text.contains("파트") || inner_text.contains("部分") || inner_text.contains("भाग") || inner_text.contains("phần")  
						|| inner_text.contains("Phần") 
						) is_part = true;
				if (inner_text.contains("method") || inner_text.contains("methode") || inner_text.contains("metodo") || inner_text.contains("método")
						|| inner_text.contains("metoda") || inner_text.contains("विधि") || inner_text.contains("metode") || inner_text.contains("方法")
						|| inner_text.contains("methode") || inner_text.contains("método") || inner_text.contains("метода") || inner_text.contains("طريقة") 
						|| inner_text.contains("วิธีการ") || inner_text.contains("phương pháp") || inner_text.contains("방법") || inner_text.contains("方法")
						|| inner_text.contains("méthode") || inner_text.contains("метод") || inner_text.contains("Метод")// 
						) is_method = true; //
				if(inner_text.contains("partie")){
					Element method_toc_element = pageDocument.getElementById("method_toc");
					if (method_toc_element != null){						
						String method_toc_text = method_toc_element.select("span").text();
						if (method_toc_text.toLowerCase().contains("parties")) is_part = true;
						if (method_toc_text.toLowerCase().contains("méthodes")) is_method = true;
					}
				}
				if ((!is_method) && (!is_part)){
					Element method_toc_element = pageDocument.getElementById("method_toc");
					if (method_toc_element != null){	
						is_method = true;
						//String method_toc_text = method_toc_element.select("span").text();
						//if (method_toc_text.contains("方法")) is_method = true;
						//if (method_toc_text.contains("метода")) is_method = true;
						//if (method_toc_text.contains("Methods")) is_method = true;
					}
					
				}
			}
			String block_name = composite_block.select(".mw-headline").html();
			Set<String> links = new HashSet<String>();
			extract_links(links, composite_block.select(".mw-headline"));
			if ((is_method && ! is_part) || (is_part && ! is_method) ) {
				Element next_sibling = composite_block.nextElementSibling();
				List<StepRepresentation> step_list = new LinkedList<StepRepresentation>();
				while(next_sibling!= null && step_list.size() == 0){
					step_list = extract_steps_list(next_sibling);
					next_sibling = next_sibling.nextElementSibling();
				}
				if (step_list.size() > 0){					
					if (is_method && ! is_part) {
						MethodRepresentation method = new MethodRepresentation(block_name);
						method.links.addAll(links);
						method.define_steps(step_list);
						methods.add(method);
					}
					if (is_part && ! is_method) {
						StepRepresentation part = new StepRepresentation(block_name);
						part.links.addAll(links);
						MethodRepresentation standard_method = new MethodRepresentation(default_step_list_name);
						standard_method.define_steps(step_list);
						part.methods.add(standard_method);
						parts.add(part);
					}
				}
				
			}
		}
		if (parts.size() > 0){
			MethodRepresentation standard_method = new MethodRepresentation(default_step_list_name);
			standard_method.define_steps(parts);
			methods.add(standard_method);
		}
		return methods;
	}
	
	
	private static String get_title(Document pageDocument){
		Iterator<Element> titles = pageDocument.select("h1").iterator();
		if (titles.hasNext()) {
			Element first_h1 = titles.next();
			first_h1.select("span").remove();
			return clean_string(first_h1.text());
		}
		return null;
	}

	private static String get_abstract(Document pageDocument){
		Iterator<Element> abstracts = pageDocument.select("p[id=\"method_toc\"]").iterator();
		String abstract_html = "";
		if (abstracts.hasNext()) {
			Element abstract_section = abstracts.next();
			Node next_sibling = abstract_section.nextSibling();
			while(next_sibling != null){
				if (next_sibling instanceof Element){
					Element next_sibling_elem = (Element) next_sibling;
					if (next_sibling_elem.hasClass("clearall") || next_sibling_elem.hasClass("adclear")) {
						next_sibling = null;
						break;
					}
					if (!next_sibling_elem.hasClass("script")){					
						abstract_html += next_sibling_elem.html();
					}
				} 
				next_sibling = next_sibling.nextSibling();
			}
		}
		if(abstract_html.length() > 0) return abstract_html;
		return null;
	}
	
	private static List<StepRepresentation> get_steps(Document pageDocument){
		List<StepRepresentation> steps = new LinkedList<StepRepresentation>();
		Element steps_area = pageDocument.getElementById("steps");
		if (steps_area == null) steps_area = pageDocument.getElementById("vorgehensweise");
		if (steps_area == null) steps_area = pageDocument.getElementById("passaggi");
		if (steps_area == null) steps_area = pageDocument.getElementById("pasos");
		if (steps_area == null) steps_area = pageDocument.getElementById("postup");
		if (steps_area == null) steps_area = pageDocument.getElementById("étapes");
		if (steps_area == null) steps_area = pageDocument.getElementById("चरण");
		if (steps_area == null) steps_area = pageDocument.getElementById("langkah");
		if (steps_area == null) steps_area = pageDocument.getElementById("ステップ");
		if (steps_area == null) steps_area = pageDocument.getElementById("stappen");
		if (steps_area == null) steps_area = pageDocument.getElementById("passos");
		if (steps_area == null) steps_area = pageDocument.getElementById("шаги");
		if (steps_area == null) steps_area = pageDocument.getElementById("الخطوات");
		if (steps_area == null) steps_area = pageDocument.getElementById("ขั้นตอน");
		if (steps_area == null) steps_area = pageDocument.getElementById("cácbước");
		if (steps_area == null) steps_area = pageDocument.getElementById("단계");
		if (steps_area == null) steps_area = pageDocument.getElementById("步骤");
		if (steps_area != null) {
			steps = extract_steps_list(steps_area);
		}
		return steps;
	}
	private static List<StepRepresentation> extract_steps_list(Element step_list_container){
		List<StepRepresentation> steps = new LinkedList<StepRepresentation>();
		Iterator<Element> steps_iter = step_list_container.select(".step").iterator();
		while(steps_iter.hasNext()){
			Element step = steps_iter.next().clone();
			Set<String> links = new HashSet<String>();
			extract_links(links, new Elements(step));
			String inner_html = step.html();
			String title = inner_html;
			String abstract_desc = null;
			Elements step_title = step.select("b.whb");
			if(step_title.size() > 0){
				title = step_title.text().trim();
				step_title.remove();
				abstract_desc = step.html();
			}
			StepRepresentation rep = new StepRepresentation(title);
			rep.links.addAll(links);
			if(abstract_desc != null) rep.set_abstract_description(abstract_desc);
			steps.add(rep);
		}
		return steps;
	}
	
	private static List<RequirementsList> get_requirements(Document pageDocument){
		List<RequirementsList> requirements = new LinkedList<RequirementsList>();
		Element requirement_area = pageDocument.getElementById("thingsyoullneed");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("cosechetiserviranno");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("elémentsnécessaires");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("cosasquenecesitarás");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("wasdubrauchst");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("materiaisnecessários");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("benodigdheden");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("чтовампонадобится");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("你需要准备");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("věcikterébudetepotřebovat");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("halyangandabutuhkan");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("สิ่งที่ต้องใช้");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("الأشياءالتيستحتاجإليها");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("चीजेंजिनकीआपकोआवश्यकताहोगी");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("nhữngthứbạncần");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("필요한것");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("必要なもの");
		if (requirement_area != null) {
			requirements = extract_requirements(requirement_area);
		}
		return requirements;
	}
	
	private static List<RequirementsList> get_ingredients(Document pageDocument){
		List<RequirementsList> requirements = new LinkedList<RequirementsList>();
		Element requirement_area = pageDocument.getElementById("ingredients");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("ingredienti");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("ingrédients");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("ingredientes");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("zutaten");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("ingredientes");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("ingrediënten");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("ингредиенты");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("素材");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("přísady");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("bahan");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("ส่วนผสม");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("المكونات");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("सामग्री");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("nguyênliệu");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("재료");
		if (requirement_area == null) requirement_area = pageDocument.getElementById("材料");
		if (requirement_area != null) {
			requirements = extract_requirements(requirement_area);
		}
		return requirements;
	}
		
	private static List<RequirementsList> extract_requirements(Element req_list_container){
		List<RequirementsList> requirements = new LinkedList<RequirementsList>();
		List<Element> names_list = new LinkedList<Element>();
		Elements names = req_list_container.select("h3");
		for(Element name : names) names_list.add(name);
			
		Elements lists = req_list_container.select("ul");
		if (lists.size() == 0 ) lists = req_list_container.select("ol");
		int index = 0;
		for(Element list : lists){	
			String list_name = "Requirements";
			Set<String> list_links = new HashSet<String>();
			if (names_list.size() == lists.size()){
				list_name = names_list.get(index).html();
				extract_links(list_links, new Elements(names_list.get(index)));
			}
			RequirementsList requirements_single_list = new RequirementsList(list_name);
			requirements_single_list.links.addAll(list_links);
			List<RequirementRepresentation> req_list = new LinkedList<RequirementRepresentation>();
			if (list != null){
				Iterator<Element> children = list.children().iterator();
				while (children.hasNext()){
					Element child = children.next();
					if (child != null && child.tagName() == "li"){
						Set<String> links = new HashSet<String>();
						extract_links(links, new Elements(child));
						RequirementRepresentation requirement = new RequirementRepresentation(child.html());
						requirement.links.addAll(links);
						req_list.add(requirement);
					}
				}
			}
			if (req_list.size() > 0) {
				requirements_single_list.requirements.addAll(req_list);
				requirements.add(requirements_single_list);
			}
			index++;
		}
		return requirements;
	}
	
	public static String clean_string(String s){
		return s;
	}
}
