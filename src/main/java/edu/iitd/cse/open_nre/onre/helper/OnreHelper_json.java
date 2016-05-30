/**
 * 
 */
package edu.iitd.cse.open_nre.onre.helper;

import java.util.LinkedList;
import java.util.Queue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.iitd.cse.open_nre.onre.domain.OnrePatternNode;
import edu.iitd.cse.open_nre.onre.domain.OnrePatternTree;
import edu.iitd.cse.open_nre.onre.runner.Onre_runMe;
import edu.knowitall.tool.parse.graph.DependencyGraph;

/**
 * @author harinder
 *
 */
public class OnreHelper_json {
	
	public static OnrePatternTree getOnrePatternTree(String jsonDepTree) {
		OnrePatternTree onrePatternTree = null;
		try{
			Gson gson = new GsonBuilder().create();
			onrePatternTree = gson.fromJson(jsonDepTree, OnrePatternTree.class);
			
			OnrePatternNode root = onrePatternTree.root;
			
			Queue<OnrePatternNode> myQ = new LinkedList<>();
			myQ.add(root);
			
			while(!myQ.isEmpty()) {
				OnrePatternNode currNode = myQ.remove();
				
				for(OnrePatternNode child : currNode.children) {
					child.parent = currNode;
					myQ.add(child);
				}
			}
		}catch(Exception e){
			//System.err.println("Exception: " + e.getMessage() + ": skipping-returning null");
			return null;
		}
		
		return onrePatternTree;
	}
	
	public static String getJsonString(String line) {
		String jsonString = null;
		
		try{
			DependencyGraph depGraph = Onre_runMe.getDepGraph(line);
			DependencyGraph simplifiedGraph = OnreHelper_graph.simplifyGraph(depGraph);
			OnrePatternTree onrePatternTree = OnreHelper_graph.convertGraph2PatternTree(simplifiedGraph);
			Gson gson = new GsonBuilder().create();
			jsonString = gson.toJson(onrePatternTree);
		}catch(Exception e) {
			return null;
		}
		
		return jsonString;
	}
}
