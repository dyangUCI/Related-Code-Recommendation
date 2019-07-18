package edu.ucla.cs.examplestack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebSocket
public class ExampleStackHandler {
	Session session = null;
	HashMap<Integer, CloneStackManager> managers = new HashMap<Integer, CloneStackManager>();
	CloneStackManager currentManager = null;
	
	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		this.session = null;
		System.out.println("Close: statusCode=" + statusCode + ", reason="
				+ reason);
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		System.out.println("Error: " + t.getMessage());
	}
	
	@OnWebSocketConnect
	public void onConnect(Session session) {
		this.session = session;
		System.out.println("Connect: "
				+ session.getRemoteAddress().getAddress());
	}
	
	@OnWebSocketMessage
	public void onMessage(String message) {
		System.out.println("Message: " + message);
		
		// handle different types of messages
		if(message.startsWith("SO Link: ")) {
			String url = message.substring(message.indexOf(':') + 2);
			process(url);
		} else if (message.startsWith("ClickedClone: ")) {
			int cloneGroupId = Integer.parseInt(message.substring(14));
			currentManager = managers.get(cloneGroupId);
		} else if (message.startsWith("User Selection: ")) {
			// truncate the message
			message = message.substring(message.indexOf(':') + 2);
			// remove the span tags
			message = message.replaceAll("\\<span.*?\\>", "");
			message = message.replaceAll("\\<\\/span\\>", "");
			// call the selectCodeOption method in the manger and reconstruct the stack
			currentManager.selectCodeOption(message);
			CloneStack customized_stack = currentManager.customizeSideBar();
			
			// send the customized template with filtered GitHub clones to the front end
			sendJSONMessage(customized_stack, "Update");
		} else if (message.startsWith("Undo")) {
			// call the undoSelection method in the manger and reconstruct the stack
			currentManager.undoSelection();
			CloneStack customized_stack = currentManager.customizeSideBar();
			
			// send the customized template with filtered GitHub clones to the front end
			sendJSONMessage(customized_stack, "Update");
		}
	}
	
	private void process(String url) {
		String postId = url.substring(url.indexOf("#") + 1);
		
		if(!ExampleStackServer.cloneMap.containsKey(postId)) {
			return;
		}
		
		// query the SO database to find the raw post
//		MySQLAccess access = new MySQLAccess();
//		access.connect();
//		String post = access.getRawPost(postId);
//		access.close();
		
		String post = null;
		try {
			File postFile = new File(ExampleStackServer.soPostPath + File.separator + postId + ".txt");
			post = FileUtils.readFileToString(postFile, Charset.defaultCharset());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		if(post == null) {
			// cannot find the post in our database
			return;
		}
		
		HashMap<Integer, String> clonedSnippets = new HashMap<Integer, String>();
		ArrayList<String> snippets = getCode(post); 
		ArrayList<String> cloneGroupPaths = ExampleStackServer.cloneMap.get(postId);
		System.out.println("Clone groups: ");
		for(String path : cloneGroupPaths) {
			System.out.println(path);
			
			File dir = new File(path);
			if(!dir.exists() || !dir.isDirectory()) {
				continue;
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
					continue;
				}
				
				// match code snippet with method
				String soCode = FileUtils.readFileToString(soFile, Charset.defaultCharset());
				for(int i = 0; i < snippets.size(); i++) {
					String snippet = snippets.get(i);
					snippet = StringEscapeUtils.unescapeHtml4(snippet);
					String[] ss = soCode.split(System.lineSeparator());
					String soMethod = "";
					boolean isIncomplete = false;
					if(ss[1].startsWith("public void foo(){")) {
						isIncomplete = true;
					}
					int start = isIncomplete ? 2 : 1;
					int end = isIncomplete ? ss.length - 3 : ss.length - 2; 
					for(; start < end; start++) {
						soMethod += ss[start] + System.lineSeparator();
					}
					soMethod += ss[end];
					if(snippet.contains(soMethod)) {
						// grab clone group index from the path of the clone folder
						int id = Integer.parseInt(path.substring(path.lastIndexOf('-') + 1));
						String replacement = "<div class=\"clone\" id=\"" + id + "\" style=\"background-color: #ffff99\">" + soMethod + "</div>";
						String highlighted_snippet = snippet.replace(soMethod, replacement);
						snippets.set(i, highlighted_snippet);
						clonedSnippets.put(i, highlighted_snippet);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// send the SO snippets with highlighted Java methods that have GitHub clones
		sendJSONMessage(clonedSnippets, "ClonedSnippets");
		
		// do program differencing and construct the templates for each clone group
		ArrayList<CloneStack> stacks = new ArrayList<CloneStack>();
		for(String path : cloneGroupPaths) {
			CloneStackManager manager = new CloneStackManager(path);
			CloneStack stack = manager.constructSidebar();
			if(stack != null) {
				stacks.add(stack);
				managers.put(stack.id, manager);
			}
		}
		
		// send the list of clone stacks to the front end
		sendJSONMessage(stacks, "CloneStacks");
	}
	
	private void sendJSONMessage(Object sentObject, String header) {
		// send the JSON message to the Chrome plugin
		ObjectMapper mapper = new ObjectMapper();
        try {
        	String jsonMessage = mapper.writeValueAsString(sentObject);
            System.out.println(jsonMessage);
            session.getRemote().sendString(header + ": " + jsonMessage);
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	private ArrayList<String> getCode(String body) {
		ArrayList<String> codes = new ArrayList<>();
		String start = "<code>", end = "</code>";
		int s = 0;
		while (true) {
			s = body.indexOf(start, s);
			if (s == -1)
				break;
			s += start.length();
			int e = body.indexOf(end, s);
			if (e == -1)
				break;
			codes.add(body.substring(s, e).trim());
			s = e + end.length();
		}
		return codes;
	}
}
