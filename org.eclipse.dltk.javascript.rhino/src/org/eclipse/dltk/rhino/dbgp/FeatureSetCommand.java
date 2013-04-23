/**
 * 
 */
package org.eclipse.dltk.rhino.dbgp;

import java.util.HashMap;

import org.eclipse.dltk.rhino.dbgp.DBGPDebugger.Command;

final class FeatureSetCommand extends DBGPDebugger.Command {

	// get|set maximum depth that the debugger engine may return when sending
	// arrays, hashs or object structures to the IDE.
	final String MAX_DEPTH = "max_depth"; //$NON-NLS-1$

	/**
	 * 
	 */
	private final DBGPDebugger debugger;

	/**
	 * @param debugger
	 */
	FeatureSetCommand(DBGPDebugger debugger) {
		this.debugger = debugger;
	}

	void parseAndExecute(String command, HashMap options) {
		
		String featureName = (String)options.get("-n");
		if(MAX_DEPTH.equals(featureName)){
			String featureValue = (String)options.get("-v");
			debugger.setMaxDepth(Integer.valueOf(featureValue));
		}
		
		this.debugger.printResponse("<response command=\"feature_set\"\r\n"
				+ "          feature_name=\""+featureName+"\"\r\n"
				+ "          success=\"1\"\r\n" + "          transaction_id=\""
				+ options.get("-i") + "\">\r\n" + "</response>\r\n" + "");
		
	}
}