/**
 * 
 */
package org.eclipse.dltk.rhino.dbgp;

import java.util.HashMap;

final class UpdateBreakPointCommand extends DBGPDebugger.Command {
	/**
	 * 
	 */
	private final DBGPDebugger debugger;

	/**
	 * @param debugger
	 */
	UpdateBreakPointCommand(DBGPDebugger debugger) {
		this.debugger = debugger;
	}

	void parseAndExecute(String command, HashMap options) {

		String id = (String) options.get("-d");
		String newState = (String) options.get("-s");
		String newLine = (String) options.get("-n");
		String hitValue = (String) options.get("-h");
		String hitCondition = (String) options.get("-o");
		String condEString = (String) options.get("--");

		if (condEString != null) {
			condEString = Base64Helper.decodeString(condEString);
		}

		try {
			this.debugger.getStackManager().updateBreakpoint(id, newState,
					newLine, hitValue, hitCondition, condEString);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.debugger
				.printResponse("<response command=\"breakpoint_update\"\r\n"
						+ " transaction_id=\"" + options.get("-i") + "\">\r\n"
						+ " id=\"" + id + "\" state=\"" + newState + "\" "
						+ "</response>\r\n" + "");
	}
}