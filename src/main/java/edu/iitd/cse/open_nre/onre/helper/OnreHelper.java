/**
 * 
 */
package edu.iitd.cse.open_nre.onre.helper;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import edu.iitd.cse.open_nre.onre.OnreGlobals;
import edu.iitd.cse.open_nre.onre.constants.OnreExtractionPartType;
import edu.iitd.cse.open_nre.onre.domain.OnreExtraction;
import edu.iitd.cse.open_nre.onre.domain.OnreExtractionPart;
import edu.iitd.cse.open_nre.onre.domain.OnrePatternNode;
import edu.iitd.cse.open_nre.onre.domain.OnrePatternTree;
import edu.iitd.cse.open_nre.onre.domain.Onre_dsDanrothSpan;
import edu.iitd.cse.open_nre.onre.domain.Onre_dsDanrothSpans;
import edu.iitd.cse.open_nre.onre.utils.OnreUtils;
import edu.iitd.cse.open_nre.onre.utils.OnreUtils_string;
import edu.iitd.cse.open_nre.onre.utils.OnreUtils_tree;

/**
 * @author harinder
 *
 */
public class OnreHelper {
	
    private static void setExtractionPart(OnrePatternTree onrePatternTree, OnrePatternNode subTreeNode, OnrePatternNode patternNode_configured, OnreExtraction onreExtraction, Onre_dsDanrothSpans danrothSpans) {

    	switch(OnreExtractionPartType.getType(patternNode_configured.word)) {
	    	case ARGUMENT: 
	    		onreExtraction.argument_headWord = new OnreExtractionPart(subTreeNode.word, subTreeNode.index, subTreeNode.posTag);  //this shall not be modified later
	    		onreExtraction.argument = new OnreExtractionPart(subTreeNode.word, subTreeNode.index, subTreeNode.posTag);
	    		break;	    	
	    	case RELATION: 
	    		onreExtraction.relation_headWord = new OnreExtractionPart(subTreeNode.word, subTreeNode.index, subTreeNode.posTag);  //this shall not be modified later
	    		onreExtraction.relation = new OnreExtractionPart(subTreeNode.word, subTreeNode.index, subTreeNode.posTag);
	    		break;
	    	case QUANTITY: 
	    		setQuantityExtractionPart(onrePatternTree, subTreeNode, onreExtraction, subTreeNode.index, danrothSpans, subTreeNode.posTag); 
	    		break;
	    	case UNKNOWN: 
	    		break;
	    }
	}
    
    /*private static String addNegationIfPossibleToQuantity(OnreExtraction onreExtraction, OnrePatternTree onrePatternTree, String quantityPhrase) {
    	OnrePatternNode quantity_parent_node = OnreUtils_tree.searchParentOfNodeInTreeByIndex(onreExtraction.quantity, onrePatternTree.root);
    	if(quantity_parent_node==null) return quantityPhrase;
		
    	String negationWord = "";
		for(OnrePatternNode child : quantity_parent_node.children) {
			if(child.dependencyLabel.equals("neg") ) {
				negationWord = child.word;
				break;
			}
		}
		
		if(!negationWord.isEmpty())
			return negationWord + " " + quantityPhrase;
		else return quantityPhrase;
    }*/
    
    private static void setQuantityExtractionPart(OnrePatternTree onrePatternTree, OnrePatternNode subTreeNode, OnreExtraction onreExtraction, int index, Onre_dsDanrothSpans danrothSpans, String posTag) {
    	Onre_dsDanrothSpan danrothSpan = OnreHelper_DanrothQuantifier.getQuantity(subTreeNode, danrothSpans);
    	if(danrothSpan == null) return;
    	
    	String quantityPhrase = danrothSpan.phrase;
    	if(quantityPhrase == null) return;
    	
    	/*if(OnreGlobals.arg_onre_isSeedFact) { //saving value and unit separately in case we want to generate a seed fact
    		onreExtraction.quantity = new OnreExtractionPart(danrothSpan.value.toString());
    		onreExtraction.additional_info = new OnreExtractionPart(danrothSpan.unit);
    		return;
    	}*/
    	
    	
    	onreExtraction.q_unit = danrothSpan.unit; //this shall not be modified later
    	onreExtraction.q_value = danrothSpan.value; //this shall not be modified later
    	
    	onreExtraction.quantity = new OnreExtractionPart(quantityPhrase, index, posTag);
    	
    	//IMPORTANT-CHANGE: DIDN'T WORK :Add sibling of quantity if that sibling is connected by "neg" depLabel
    	/*String newQuantityPhrase = addNegationIfPossibleToQuantity(onreExtraction, onrePatternTree, quantityPhrase);
    	
    	if(!newQuantityPhrase.equals(quantityPhrase)) {
    		onreExtraction.quantity.text = newQuantityPhrase;	
    	}*/
    	
    	
    	if(!quantityPhrase.contains("per cent") && !quantityPhrase.contains("percent") && !quantityPhrase.contains("%")) return;
    	
    	//finding percent node
    	OnrePatternNode node_percent = null;
    	node_percent = OnreUtils_tree.searchNodeInTreeByText("percent", subTreeNode);
    	if(node_percent == null) node_percent = OnreUtils_tree.searchNodeInTreeByText("cent", subTreeNode);
    	if(node_percent == null) node_percent = OnreUtils_tree.searchNodeInTreeByText("%", subTreeNode);
    	if(node_percent == null) return;
    	
    	onreExtraction.quantity_percent = new OnreExtractionPart(node_percent.word, node_percent.index);
    }
	
	public static OnrePatternNode findPatternSubTree(OnrePatternTree onrePatternTree, OnrePatternNode patternNode_sentence, 
			OnrePatternNode patternNode_configured, OnreExtraction onreExtraction, Onre_dsDanrothSpans danrothSpans) {

		if (patternNode_sentence.matches(patternNode_configured) 
				&& matchChildren(onrePatternTree, patternNode_sentence, patternNode_configured, onreExtraction, danrothSpans)) {
			
			setExtractionPart(onrePatternTree, patternNode_sentence, patternNode_configured, onreExtraction, danrothSpans);
			return patternNode_sentence;
		}

    	OnrePatternNode result = null;
    	
    	for (OnrePatternNode child : patternNode_sentence.children) {
    		result = findPatternSubTree(onrePatternTree, child, patternNode_configured, onreExtraction, danrothSpans);
    		if (result != null && matchChildren(onrePatternTree, patternNode_sentence, result, onreExtraction, danrothSpans)) return result;
    	}

    	return result;
    }
	
	private static boolean matchChildren(OnrePatternTree onrePatternTree, OnrePatternNode patternNode_sentence, 
			OnrePatternNode patternNode_configured, OnreExtraction onreExtraction, Onre_dsDanrothSpans danrothSpans) {
    	
		if (patternNode_sentence.matches(patternNode_configured)) 
			setExtractionPart(onrePatternTree, patternNode_sentence, patternNode_configured, onreExtraction, danrothSpans);
		else return false;
		
    	if (patternNode_sentence.children.size() < patternNode_configured.children.size()) return false;

    	boolean result = true;
    	int index_sentence = 0;

    	for (int index_config = 0; index_config < patternNode_configured.children.size(); index_config++) {

    		// Skip non-matching children in the tree.
    		while (index_sentence < patternNode_sentence.children.size()
    		      && !(result = matchChildren(onrePatternTree, patternNode_sentence.children.get(index_sentence), 
    		    		  patternNode_configured.children.get(index_config), onreExtraction, danrothSpans))) {
    			index_sentence++;
    		}

    		if (!result) return result;
    	}

    	return result;
    }
	
	public static OnreExtraction onreExtraction_postProcessing(OnrePatternNode patternNode_sentence, OnreExtraction onreExtraction, OnrePatternNode patternNode_configured) throws IOException {
		//if(!OnreGlobals.arg_onre_isSeedFact) OnreHelper_expansions.expandExtraction(onreExtraction, patternNode_sentence);
		OnreHelper_expansions.expandExtraction(onreExtraction, patternNode_sentence);

		/*===DIDN'T WORK===if(onreExtraction.quantity!=null && OnreUtils_string.ignoreCaseContainsPhrase(onreExtraction.argument.text, OnreHelper_DanrothQuantifier.getPhraseExceptValue(onreExtraction.quantity.text))) {
				onreExtraction.quantity.text = OnreHelper_DanrothQuantifier.getValueFromPhrase(onreExtraction.quantity.text);
				onreExtraction.q_unit = "";
		}*/
		
		if(OnreGlobals.arg_onre_isSeedFact) return onreExtraction;
		
        if(postProcessingHelper_isValueInArgOrRel(onreExtraction)) return null; 				//TODO: IMPORTANT-CHANGE #4:Don't extract if quantity value is present in the argument or relation
        if(postProcessingHelper_isUnitInArg(onreExtraction)) return null;   	   				//TODO: IMPORTANT-CHANGE #5:Don't extract if quantity unit is present in the argument
        if(postProcessingHelper_isArgWordInQuantity(onreExtraction)) return null;
    	
        if(postProcessingHelper_isAdditionalInfoAlreadyPresent(onreExtraction)) return null;		
    	
    	postProcessingHelper_numberOf(onreExtraction); 											//TODO: IMPORTANT-CHANGE #7:use [number of] if the relation phrase is exactly same as unit - have only value in the quantity part
    	
    	postProcessingHelper_removeApostrophe(onreExtraction);
    	
    	postProcessingHelper_appendHasHave(onreExtraction, patternNode_configured);
    	
    	//replaceDoubleSpaces(onreExtraction);
    	return onreExtraction;
	}
	
	public static void postProcessingHelper_appendHasHave(OnreExtraction onreExtraction, OnrePatternNode patternNode_configured) {
		boolean isNounRelation = false, containsBeVerbs = false;
		
		Queue<OnrePatternNode> q_yetToExpand = new LinkedList<OnrePatternNode>();
		q_yetToExpand.add(patternNode_configured);
		while(!q_yetToExpand.isEmpty()) {
			OnrePatternNode currNode = q_yetToExpand.remove();
			if(currNode.word.equals("{rel}") && currNode.posTag.equalsIgnoreCase("nnp|nn")) { isNounRelation = true;}
			if(currNode.word.contains("is|are|was|were") || currNode.word.equals("has|have|had")) { containsBeVerbs = true;}
			
			for(OnrePatternNode child : currNode.children) {
				 q_yetToExpand.add(child);
			}
		}
		
		if(isNounRelation && containsBeVerbs) {
			if(OnreGlobals.isSentenceInPastTense) {
				onreExtraction.relation.text = "had " + onreExtraction.relation.text;
			}
			else {
				if(!onreExtraction.argument.text.endsWith("s") && OnreGlobals.isSubjectSingular) {
					onreExtraction.relation.text = "has " + onreExtraction.relation.text;
				}
				else {
					onreExtraction.relation.text = "have " + onreExtraction.relation.text;
				}
			}
			
			if(!onreExtraction.relation.text.endsWith("of") && !onreExtraction.relation.text.contains("[number of]")) {
				onreExtraction.relation.text = onreExtraction.relation.text + " of";
			}
			
			if(OnreGlobals.negatedWord != null) {
				onreExtraction.relation.text = OnreGlobals.negatedWord + " " + onreExtraction.relation.text;
			}
			
			if(OnreGlobals.auxVerb != null) {
				onreExtraction.relation.text = OnreGlobals.auxVerb + " " + onreExtraction.relation.text;
			}
		}
	}
	
	private static void postProcessingHelper_removeApostrophe(OnreExtraction onreExtraction) {
		if(onreExtraction.argument.text.endsWith("'s")) {
			onreExtraction.argument.text = onreExtraction.argument.text.substring(0, onreExtraction.argument.text.length() - 3);
		}
	}

	/*private static void replaceDoubleSpaces(OnreExtraction onreExtraction) {
		onreExtraction.argument.text = onreExtraction.argument.text.replaceAll("  ", " ");
    	onreExtraction.relation.text = onreExtraction.relation.text.replaceAll("  ", " ");
    	if(onreExtraction.quantity_unit_plus!=null) onreExtraction.quantity_unit_plus.text = onreExtraction.quantity_unit_plus.text.replaceAll("  ", " ");
    	if(onreExtraction.quantity!=null) onreExtraction.quantity.text = onreExtraction.quantity.text.replaceAll("  ", " ");
	}*/

	private static boolean postProcessingHelper_isAdditionalInfoAlreadyPresent(OnreExtraction onreExtraction) {
		removeAdditionalInfoIfAlreadyPresent(onreExtraction);									
        if(postProcessingHelper_isAdditionalInfoStillPresent(onreExtraction)) return true;
        return false;
	}

	private static void removeAdditionalInfoIfAlreadyPresent(OnreExtraction onreExtraction) {
		if(onreExtraction.additional_info == null) return;
		if(OnreUtils_string.isIgnoreCaseContainsPhrase(onreExtraction.quantity.text, onreExtraction.additional_info.text)) {onreExtraction.additional_info = null; return;}
		if(OnreUtils_string.isIgnoreCaseContainsPhrase(onreExtraction.relation.text, onreExtraction.additional_info.text)) {onreExtraction.additional_info = null; return;}
		if(OnreUtils_string.isIgnoreCaseContainsPhrase(onreExtraction.argument.text, onreExtraction.additional_info.text)) {onreExtraction.additional_info = null; return;}
	}

	private static boolean postProcessingHelper_isAdditionalInfoStillPresent(OnreExtraction onreExtraction) {
		if(onreExtraction.additional_info == null) return false;
		if(OnreUtils_string.isIgnoreCaseContainsPhrase(onreExtraction.additional_info.text, onreExtraction.argument_headWord.text)) return true;
		if(OnreUtils_string.isIgnoreCaseContainsPhrase(onreExtraction.additional_info.text, onreExtraction.relation_headWord.text)) return true;

		return false;
	}

	private static void postProcessingHelper_numberOf(OnreExtraction onreExtraction) {
		if(onreExtraction.q_unit==null || onreExtraction.q_unit.isEmpty()) return;
		
        //if(onreExtraction.relation.text.equalsIgnoreCase(onreExtraction.q_unit)) {
		if(OnreUtils_string.isIgnoreCaseContainsPhrase(onreExtraction.relation.text, onreExtraction.q_unit)) {
        	//onreExtraction.quantity.text = onreExtraction.quantity.text.replaceAll("(?i)"+onreExtraction.relation.text, "").trim();
			onreExtraction.quantity.text = onreExtraction.quantity.text.replaceAll("(?i)"+onreExtraction.q_unit, "").trim();
        	
			if(onreExtraction.relation_headWord.posTag.matches("(?i)NNP?S")
					|| onreExtraction.quantity.posTag.matches("(?i)NNP?S")
					|| onreExtraction.relation_headWord.text.endsWith("s")
					|| onreExtraction.q_unit.endsWith("s")
					) //adding [number of] only in case of plural noun - rel headword or q_unit
        		onreExtraction.relation.text = "[number of] " + onreExtraction.relation.text;
        }
	}

	private static boolean postProcessingHelper_isUnitInArg(OnreExtraction onreExtraction) {
		if(onreExtraction.q_unit == null || onreExtraction.q_unit.isEmpty()) return false;

		if(OnreUtils_string.isIgnoreCaseContainsPhrase(onreExtraction.argument.text, onreExtraction.q_unit)) return true;
		if(OnreUtils_string.isIgnoreCaseContainsPhrase(onreExtraction.q_unit, onreExtraction.argument_headWord.text)) return true;
   		
		return false;
	}

	private static boolean postProcessingHelper_isValueInArgOrRel(OnreExtraction onreExtraction) {
		if(!OnreUtils.quantityExists(onreExtraction)) return false;
		
		String q_value = OnreHelper_DanrothQuantifier.getValueFromPhrase(onreExtraction.quantity.text);
		if(q_value == null) return false;
      
		//if(onreExtraction.argument.text.contains(q_value)) return true;
       	//if(onreExtraction.relation.text.contains(q_value)) return true;
		
		if(OnreUtils_string.isIgnoreCaseIgnoreCommaIgnoreSpaceContains(onreExtraction.argument.text, q_value)) return true;
		if(OnreUtils_string.isIgnoreCaseIgnoreCommaIgnoreSpaceContains(onreExtraction.relation.text, q_value)) return true;
		
		return false;
	}
	
	private static boolean postProcessingHelper_isArgWordInQuantity(OnreExtraction onreExtraction) {
		if(!OnreUtils.quantityExists(onreExtraction)) return false;
		String []argWords = onreExtraction.argument.text.split(" ");
		String []quantWords = onreExtraction.quantity.text.split(" ");
		
		if(argWords.length == 1) {
			String argWord = argWords[0];
			for(String quantWord : quantWords) {
				if(quantWord.equalsIgnoreCase(argWord)) {
					return true;
				}
			}
		}
		return false;
	}
	
}
