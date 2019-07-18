package edu.ucla.cs.examplestack;

import java.util.ArrayList;

public class CloneStack {
	public int id;
	public ArrayList<Clone> clones;
	
	public CloneStack(int id, ArrayList<Clone> clones) {
		this.id = id;
		this.clones = clones;
	}
}
