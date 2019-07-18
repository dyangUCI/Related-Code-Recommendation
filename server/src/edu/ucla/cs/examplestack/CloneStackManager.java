package edu.ucla.cs.examplestack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class constructs a clone stack and keeps track of its status
 * 
 * @author troy
 *
 */
public class CloneStackManager {
//	private HashMap<Point, ArrayList<String>> allDiffs = new HashMap<Point, ArrayList<String>>();
//	private HashMap<String, HashMap<Point, String>> diffMaps = new HashMap<String, HashMap<Point, String>>();
	private HashMap<String, String> srcHtmlMap = new HashMap<String, String>();
	private HashMap<String, String> srcCodeMap = new HashMap<String, String>();
	private ArrayList<String> user_selections = new ArrayList<String>();
	private ArrayList<ArrayList<String>> unselected = new ArrayList<ArrayList<String>>();
	private ArrayList<Clone> clones = new ArrayList<Clone>();
	private String soCode = "";
	
	int cloneGroupId;
	String cloneGroupPath;
	
	public CloneStackManager(String cloneGroupPath) {
		this.cloneGroupPath = cloneGroupPath;
		this.cloneGroupId = Integer.parseInt(cloneGroupPath.substring(cloneGroupPath.lastIndexOf('-') + 1));
	}
	
	public CloneStack constructSidebar() {
		File dir = new File(cloneGroupPath);
		if(!dir.exists() || !dir.isDirectory()) {
			return null;
		}
		
		// read the cloned method in the given snippet
		File soFile = null;
		try {
			for(File f : dir.listFiles()) {
				if(f.getName().startsWith("so-") && !f.getName().endsWith("~")) {
					soFile = f;
					break;
				}
			}
			
			if(soFile == null) {
				return null;
			}
			
			soCode = FileUtils.readFileToString(soFile, Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// grep all urls
		File urlFile = new File(cloneGroupPath + File.separator + "gh-urls.txt");
		HashMap<String, String> urls = new HashMap<String, String>();
		try {
			List<String> lines = FileUtils.readLines(urlFile, Charset.defaultCharset());
			for(String line : lines) {
				String range = line.substring(line.indexOf("#L"));
				String link = line.substring(line.indexOf("https:"));
				urls.put(range, link);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		File[] files = dir.listFiles();
		for(int i = 0; i < files.length; i++) {
			File f = files[i];
			if(!f.getName().startsWith("carved-") || f.getName().endsWith("~")) {
				// skip those cached files
				continue;
			}
			try {
				String code = FileUtils.readFileToString(f, Charset.defaultCharset());
				// get the GitHub link
				String ghFileName = f.getName();
				String ghFileNameNoExt = ghFileName.substring(0,
						ghFileName.indexOf("."));
				String startLineS = ghFileNameNoExt.split("-")[4];
				String endLineS = ghFileNameNoExt.split("-")[5];
				String range = "#L" + startLineS + "-L" + endLineS;
				String ghLink = urls.get(range);
				if(ghLink == null) {
					ghLink = "";
				}
				
				srcCodeMap.put(ghLink, code);
				
				// get the numbers of watches, stars, and contributors using the GitHub Repository API
				int watch = 0;
				int star = 0;
				int fork = 0;
				if(!ghLink.isEmpty()) {
					String temp = ghLink.substring(ghLink.indexOf("github.com") + 11);
					String usrname = temp.substring(0, temp.indexOf('/'));
					temp = temp.substring(temp.indexOf('/') + 1);
					String reponame = temp.substring(0, temp.indexOf('/'));
					String apiRequest = "https://api.github.com/repos/" + usrname + "/" + reponame;  
					String repo_data = DownloadUtils.download(apiRequest);
					if(!repo_data.isEmpty()) {
						ObjectMapper mapper = new ObjectMapper();
						JsonNode root = mapper.readTree(repo_data);
						star = root.path("stargazers_count").asInt();
						watch = root.path("subscribers_count").asInt();
						fork = root.path("forks_count").asInt();
					}
				}
				
				String ghCodeFragment = code.substring(code.indexOf("\n") + 1, code.lastIndexOf("\n"));
				String soCodeFragment = soCode.substring(soCode.indexOf("\n") + 1, soCode.lastIndexOf("\n"));
				clones.add(new Clone(ghCodeFragment, soCodeFragment, ghLink, watch, star, fork));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// sort the clones by the number of stars
		Collections.sort(clones, new Comparator<Clone> () {
			public int compare(Clone c1, Clone c2) {
				if(c1.star == c2.star) {
					if(c1.watch == c2.watch) {
						return c2.fork - c1.fork;
					} else {
						return c2.watch - c1.watch; 
					}
				} else {
					return c2.star - c1.star;
				}
			}
		});
		
		CloneStack stack = new CloneStack(cloneGroupId, clones);
		return stack;
	}
	
	public CloneStack customizeSideBar() {
		// only keep the clones that have the user selected options
		ArrayList<Clone> clones2 = new ArrayList<Clone>();
		clones2.addAll(clones);
		Iterator<Clone> iter = clones2.iterator();
		while(iter.hasNext()) {
			Clone next = iter.next();
			String link = next.url;
			if(srcCodeMap.containsKey(link)) {
				String code = srcCodeMap.get(link);
				// canonicalize the source code of this clone
				code = code.replaceAll(" ", "");
				
				// check if this clone contains all user selections
				boolean containsAll = true;
				for(String sel : user_selections) {
					if(sel.equals("empty")) continue;
					sel = sel.replaceAll(" ", "");
					if(!code.contains(sel)) {
						containsAll = false;
						break;
					}
				}
				
				if (containsAll) {
					// check whether the clone does not contain all deleted or non-inserted code
					for(ArrayList<String> unsels : unselected) {
						for(String unsel : unsels) {
							unsel = unsel.replaceAll(" ", "");
							if(code.contains(unsel)) {
								containsAll = false;
								break;
							}
						}	
					}
					
					if(!containsAll) {
						// contains all user selection but also contains a deleted/non-inserted code
						iter.remove();
					}
				} else {
					// does not contain a user selection
					iter.remove();
				}
			} else {
				iter.remove();
			}
		}
		
		CloneStack stack = new CloneStack(cloneGroupId, clones2);
		return stack;
	}
	
	public void selectCodeOption(String message) {
		String[] ss = message.split("@@@");
		String sel = ss[0];
		sel = sel.substring(0, sel.indexOf(" :"));
		if(sel.equals("empty")) {
			user_selections.add("empty");
			if(ss.length > 1) {
				ArrayList<String> unsels = new ArrayList<String>();
				for(int j = 1; j < ss.length; j++) {
					String unsel = ss[j];
					unsel = unsel.substring(0, unsel.indexOf(" :"));
					unsels.add(unsel);
				}
				unselected.add(unsels);
			}
		} else {
			user_selections.add(StringEscapeUtils.unescapeHtml4(sel));
		}
	}
	
	public void undoSelection() {
		// remove the last selection
		if(!user_selections.isEmpty()) {
			String last = user_selections.get(user_selections.size() - 1);
			if(last.equals("empty")) {
				unselected.remove(unselected.size() - 1);
			}
			user_selections.remove(user_selections.size() - 1);
		}
	}
}
