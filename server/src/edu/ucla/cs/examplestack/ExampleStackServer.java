package edu.ucla.cs.examplestack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import com.github.gumtreediff.client.Run;

public class ExampleStackServer {
	
//	private static String path = "/home/troy/research/Integration-Study/dataset/all-clones-reindex";
	private static String path;
	public static String soPostPath;
	public static HashMap<String, ArrayList<String>> cloneMap = new HashMap<String, ArrayList<String>>();
	
	public static void main(String[] args) throws Exception {
		String certificatePath = "./certificate/keystore";
		// take the root directory of all clones from the command line
		if(args.length == 0) {
	        System.out.println("Proper Usage is: java -cp examplestack.jar ExampleStackServer "
	        		+ "[directory_of_all_clones] "
	        		+ "[directory_of_SO_posts] "
	        		+ "[path_of_certificate]");
	        return;
	    } else {
	    	path = args[0];
	    	soPostPath = args[1];
	    	certificatePath = args[2];
	    }
		
		// must do this init before doing GumTree diff
		Run.initGenerators();
		
		// load the clones found by SourcererCC
		File rootDir = new File(path);
		for(File cloneDir : rootDir.listFiles()) {
			for(File file : cloneDir.listFiles()) {
				String fName = file.getName();
				if(fName.startsWith("so-") && !fName.endsWith("~")) {
					String[] ss = fName.split("-");
					String postId = ss[1];
					// String methodId = ss[2];
					// cloneMap.put(postId + "-" + methodId, cloneDir.getAbsolutePath());
					ArrayList<String> cloneGroupPaths;
					if(cloneMap.containsKey(postId)) {
						cloneGroupPaths = cloneMap.get(postId);
					} else {
						cloneGroupPaths = new ArrayList<String>();
					}
					cloneGroupPaths.add(cloneDir.getAbsolutePath());
					cloneMap.put(postId, cloneGroupPaths);
				}
			}
		}
		
		final int sslport = 8080;
		Server server = new Server();
		SslContextFactory contextFactory = new SslContextFactory();
		contextFactory.setKeyStorePath(certificatePath);
        contextFactory.setKeyStorePassword("examplestackisawesome!@#");
        SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(contextFactory, org.eclipse.jetty.http.HttpVersion.HTTP_1_1.toString());
		
        HttpConfiguration config = new HttpConfiguration();
        config.setSecureScheme("https");
        config.setSecurePort(sslport);
        config.setOutputBufferSize(32786);
        config.setRequestHeaderSize(8192);
        config.setResponseHeaderSize(8192);
        HttpConfiguration sslConfiguration = new HttpConfiguration(config);
        sslConfiguration.addCustomizer(new SecureRequestCustomizer());
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(sslConfiguration);
        
        ServerConnector connector = new ServerConnector(server, sslConnectionFactory, httpConnectionFactory);
        connector.setPort(sslport);
        server.addConnector(connector);
		
        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.register(ExampleStackHandler.class);
            }
        };
        server.setHandler(wsHandler);
        server.start();
        server.join();
	}
}
