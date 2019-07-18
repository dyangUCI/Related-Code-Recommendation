package edu.ucla.cs.examplestack;

public class Clone {
	public String ghCode; // the github fragment with diffs in html
	public String soCode; // the original SO fragment with diffs in html
	public String url;
	public int watch;
	public int star;
	public int fork;
	
	public Clone(String ghCode, String soCode, String url, int watch, int star, int fork) {
		this.ghCode = ghCode;
		this.soCode = soCode;
		this.url = url;
		this.watch = watch;
		this.star = star;
		this.fork = fork;
	}
}