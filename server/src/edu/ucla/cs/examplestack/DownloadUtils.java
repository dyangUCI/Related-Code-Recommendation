package edu.ucla.cs.examplestack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class DownloadUtils {
	public static String download(String link) {
		URL url= null;
		BufferedReader in = null;
		try {
	        url = new URL(link);
			in = new BufferedReader(
			        new InputStreamReader(url.openStream()));
			String content = "";
			String inputLine;
		    while ((inputLine = in.readLine()) != null) {
		    	content += inputLine + System.lineSeparator();
		    }
			return content;
		} catch (IOException e) {
			// fail safe
			System.err.println("Fails to download from " + link);
		} finally {
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return "";
	}
	
	public static void main(String[] args) {
		String link = "https://api.github.com/repos/Programming-Systems-Lab/dyclink";
		System.out.println(DownloadUtils.download(link));
	}
} 
