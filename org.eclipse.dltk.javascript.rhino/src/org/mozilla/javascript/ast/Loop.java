/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Steve Yegge
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.javascript.ast;

/**
 * Abstract base type for loops.
 */
public abstract class Loop extends Scope {

	protected AstNode body;
	protected int lp = -1;
	protected int rp = -1;

	public Loop() {
	}

	public Loop(int pos) {
		super(pos);
	}

	public Loop(int pos, int len) {
		super(pos, len);
	}

	/**
	 * Returns loop body
	 */
	public AstNode getBody() {
		return body;
	}

	/**
	 * Sets loop body. Sets the parent of the body to this loop node, and
	 * updates its offset to be relative. Extends the length of this node to
	 * include the body.
	 */
	public void setBody(AstNode body) {
		this.body = body;
		int end = body.getPosition() + body.getLength();
		this.setLength(end - this.getPosition());
		body.setParent(this);
	}

	/**
	 * Returns left paren position, -1 if missing
	 */
	public int getLp() {
		return lp;
	}

	/**
	 * Sets left paren position
	 */
	public void setLp(int lp) {
		this.lp = lp;
	}

	/**
	 * Returns right paren position, -1 if missing
	 */
	public int getRp() {
		return rp;
	}

	/**
	 * Sets right paren position
	 */
	public void setRp(int rp) {
		this.rp = rp;
	}

	/**
	 * Sets both paren positions
	 */
	public void setParens(int lp, int rp) {
		this.lp = lp;
		this.rp = rp;
	}
}
