package org.mozilla.javascript.debug;

import org.mozilla.javascript.Scriptable;

public interface IDebuggerWithWatchPoints extends Debugger {

	public void access(String property, Scriptable object);

	public void modification(String property, Scriptable object);
}
