/* ***** BEGIN LICENSE BLOCK *****
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
 *   Norris Boyd
 *   Roger Lawrence
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

package org.mozilla.javascript.optimizer;

import org.mozilla.javascript.*;
import org.mozilla.javascript.ast.ScriptNode;

class Optimizer {

	static final int NoType = 0;
	static final int NumberType = 1;
	static final int AnyType = 3;

	// It is assumed that (NumberType | AnyType) == AnyType

	void optimize(ScriptNode scriptOrFn) {
		// run on one function at a time for now
		int functionCount = scriptOrFn.getFunctionCount();
		for (int i = 0; i != functionCount; ++i) {
			OptFunctionNode f = OptFunctionNode.get(scriptOrFn, i);
			optimizeFunction(f);
		}
	}

	private void optimizeFunction(OptFunctionNode theFunction) {
		if (theFunction.fnode.requiresActivation())
			return;

		inDirectCallFunction = theFunction.isTargetOfDirectCall();
		this.theFunction = theFunction;

		ObjArray statementsArray = new ObjArray();
		buildStatementList_r(theFunction.fnode, statementsArray);
		Node[] theStatementNodes = new Node[statementsArray.size()];
		statementsArray.toArray(theStatementNodes);

		Block.runFlowAnalyzes(theFunction, theStatementNodes);

		if (!theFunction.fnode.requiresActivation()) {
			/*
			 * Now that we know which local vars are in fact always Numbers, we
			 * re-write the tree to take advantage of that. Any arithmetic or
			 * assignment op involving just Number typed vars is marked so that
			 * the codegen will generate non-object code.
			 */
			parameterUsedInNumberContext = false;
			for (int i = 0; i < theStatementNodes.length; i++) {
				rewriteForNumberVariables(theStatementNodes[i], NumberType);
			}
			theFunction.setParameterNumberContext(parameterUsedInNumberContext);
		}

	}

	/*
	 * Each directCall parameter is passed as a pair of values - an object and a
	 * double. The value passed depends on the type of value available at the
	 * call site. If a double is available, the object in java/lang/Void.TYPE is
	 * passed as the object value, and if an object value is available, then 0.0
	 * is passed as the double value.
	 * 
	 * The receiving routine always tests the object value before proceeding. If
	 * the parameter is being accessed in a 'Number Context' then the code
	 * sequence is : if ("parameter_objectValue" == java/lang/Void.TYPE)
	 * ...fine..., use the parameter_doubleValue else
	 * toNumber(parameter_objectValue)
	 * 
	 * and if the parameter is being referenced in an Object context, the code
	 * is if ("parameter_objectValue" == java/lang/Void.TYPE) new
	 * Double(parameter_doubleValue) else ...fine..., use the
	 * parameter_objectValue
	 * 
	 * If the receiving code never uses the doubleValue, it is converted on
	 * entry to a Double instead.
	 */

	/*
	 * We're referencing a node in a Number context (i.e. we'd prefer it was a
	 * double value). If the node is a parameter in a directCall function, mark
	 * it as being referenced in this context.
	 */
	private void markDCPNumberContext(Node n) {
		if (inDirectCallFunction && n.getType() == Token.GETVAR) {
			int varIndex = theFunction.getVarIndex(n);
			if (theFunction.isParameter(varIndex)) {
				parameterUsedInNumberContext = true;
			}
		}
	}

	private boolean convertParameter(Node n) {
		if (inDirectCallFunction && n.getType() == Token.GETVAR) {
			int varIndex = theFunction.getVarIndex(n);
			if (theFunction.isParameter(varIndex)) {
				n.removeProp(Node.ISNUMBER_PROP);
				return true;
			}
		}
		return false;
	}

	private int rewriteForNumberVariables(Node n, int desired) {
		switch (n.getType()) {
		case Token.EXPR_VOID: {
			Node child = n.getFirstChild();
			int type = rewriteForNumberVariables(child, NumberType);
			if (type == NumberType)
				n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
			return NoType;
		}
		case Token.NUMBER:
			n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
			return NumberType;

		case Token.GETVAR: {
			int varIndex = theFunction.getVarIndex(n);
			if (inDirectCallFunction && theFunction.isParameter(varIndex)
					&& desired == NumberType) {
				n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
				return NumberType;
			} else if (theFunction.isNumberVar(varIndex)) {
				n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
				return NumberType;
			}
			return NoType;
		}

		case Token.INC:
		case Token.DEC: {
			Node child = n.getFirstChild();
			// "child" will be GETVAR or GETPROP or GETELEM
			if (child.getType() == Token.GETVAR) {
				if (rewriteForNumberVariables(child, NumberType) == NumberType
						&& !convertParameter(child)) {
					n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
					markDCPNumberContext(child);
					return NumberType;
				}
				return NoType;
			} else if (child.getType() == Token.GETELEM) {
				return rewriteForNumberVariables(child, NumberType);
			}
			return NoType;
		}
		case Token.SETVAR: {
			Node lChild = n.getFirstChild();
			Node rChild = lChild.getNext();
			int rType = rewriteForNumberVariables(rChild, NumberType);
			int varIndex = theFunction.getVarIndex(n);
			if (inDirectCallFunction && theFunction.isParameter(varIndex)) {
				if (rType == NumberType) {
					if (!convertParameter(rChild)) {
						n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
						return NumberType;
					}
					markDCPNumberContext(rChild);
					return NoType;
				} else
					return rType;
			} else if (theFunction.isNumberVar(varIndex)) {
				if (rType != NumberType) {
					n.removeChild(rChild);
					n.addChildToBack(new Node(Token.TO_DOUBLE, rChild));
				}
				n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
				markDCPNumberContext(rChild);
				return NumberType;
			} else {
				if (rType == NumberType) {
					if (!convertParameter(rChild)) {
						n.removeChild(rChild);
						n.addChildToBack(new Node(Token.TO_OBJECT, rChild));
					}
				}
				return NoType;
			}
		}
		case Token.LE:
		case Token.LT:
		case Token.GE:
		case Token.GT: {
			Node lChild = n.getFirstChild();
			Node rChild = lChild.getNext();
			int lType = rewriteForNumberVariables(lChild, NumberType);
			int rType = rewriteForNumberVariables(rChild, NumberType);
			markDCPNumberContext(lChild);
			markDCPNumberContext(rChild);

			if (convertParameter(lChild)) {
				if (convertParameter(rChild)) {
					return NoType;
				} else if (rType == NumberType) {
					n.putIntProp(Node.ISNUMBER_PROP, Node.RIGHT);
				}
			} else if (convertParameter(rChild)) {
				if (lType == NumberType) {
					n.putIntProp(Node.ISNUMBER_PROP, Node.LEFT);
				}
			} else {
				if (lType == NumberType) {
					if (rType == NumberType) {
						n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
					} else {
						n.putIntProp(Node.ISNUMBER_PROP, Node.LEFT);
					}
				} else {
					if (rType == NumberType) {
						n.putIntProp(Node.ISNUMBER_PROP, Node.RIGHT);
					}
				}
			}
			// we actually build a boolean value
			return NoType;
		}

		case Token.ADD: {
			Node lChild = n.getFirstChild();
			Node rChild = lChild.getNext();
			int lType = rewriteForNumberVariables(lChild, NumberType);
			int rType = rewriteForNumberVariables(rChild, NumberType);

			if (convertParameter(lChild)) {
				if (convertParameter(rChild)) {
					return NoType;
				} else {
					if (rType == NumberType) {
						n.putIntProp(Node.ISNUMBER_PROP, Node.RIGHT);
					}
				}
			} else {
				if (convertParameter(rChild)) {
					if (lType == NumberType) {
						n.putIntProp(Node.ISNUMBER_PROP, Node.LEFT);
					}
				} else {
					if (lType == NumberType) {
						if (rType == NumberType) {
							n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
							return NumberType;
						} else {
							n.putIntProp(Node.ISNUMBER_PROP, Node.LEFT);
						}
					} else {
						if (rType == NumberType) {
							n.putIntProp(Node.ISNUMBER_PROP, Node.RIGHT);
						}
					}
				}
			}
			return NoType;
		}

		case Token.BITXOR:
		case Token.BITOR:
		case Token.BITAND:
		case Token.RSH:
		case Token.LSH:
		case Token.SUB:
		case Token.MUL:
		case Token.DIV:
		case Token.MOD: {
			Node lChild = n.getFirstChild();
			Node rChild = lChild.getNext();
			int lType = rewriteForNumberVariables(lChild, NumberType);
			int rType = rewriteForNumberVariables(rChild, NumberType);
			markDCPNumberContext(lChild);
			markDCPNumberContext(rChild);
			if (lType == NumberType) {
				if (rType == NumberType) {
					n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
					return NumberType;
				} else {
					if (!convertParameter(rChild)) {
						n.removeChild(rChild);
						n.addChildToBack(new Node(Token.TO_DOUBLE, rChild));
						n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
					}
					return NumberType;
				}
			} else {
				if (rType == NumberType) {
					if (!convertParameter(lChild)) {
						n.removeChild(lChild);
						n.addChildToFront(new Node(Token.TO_DOUBLE, lChild));
						n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
					}
					return NumberType;
				} else {
					if (!convertParameter(lChild)) {
						n.removeChild(lChild);
						n.addChildToFront(new Node(Token.TO_DOUBLE, lChild));
					}
					if (!convertParameter(rChild)) {
						n.removeChild(rChild);
						n.addChildToBack(new Node(Token.TO_DOUBLE, rChild));
					}
					n.putIntProp(Node.ISNUMBER_PROP, Node.BOTH);
					return NumberType;
				}
			}
		}
		case Token.SETELEM:
		case Token.SETELEM_OP: {
			Node arrayBase = n.getFirstChild();
			Node arrayIndex = arrayBase.getNext();
			Node rValue = arrayIndex.getNext();
			int baseType = rewriteForNumberVariables(arrayBase, NumberType);
			if (baseType == NumberType) {// can never happen ???
				if (!convertParameter(arrayBase)) {
					n.removeChild(arrayBase);
					n.addChildToFront(new Node(Token.TO_OBJECT, arrayBase));
				}
			}
			int indexType = rewriteForNumberVariables(arrayIndex, NumberType);
			if (indexType == NumberType) {
				if (!convertParameter(arrayIndex)) {
					// setting the ISNUMBER_PROP signals the codegen
					// to use the OptRuntime.setObjectIndex that takes
					// a double index
					n.putIntProp(Node.ISNUMBER_PROP, Node.LEFT);
				}
			}
			int rValueType = rewriteForNumberVariables(rValue, NumberType);
			if (rValueType == NumberType) {
				if (!convertParameter(rValue)) {
					n.removeChild(rValue);
					n.addChildToBack(new Node(Token.TO_OBJECT, rValue));
				}
			}
			return NoType;
		}
		case Token.GETELEM: {
			Node arrayBase = n.getFirstChild();
			Node arrayIndex = arrayBase.getNext();
			int baseType = rewriteForNumberVariables(arrayBase, NumberType);
			if (baseType == NumberType) {// can never happen ???
				if (!convertParameter(arrayBase)) {
					n.removeChild(arrayBase);
					n.addChildToFront(new Node(Token.TO_OBJECT, arrayBase));
				}
			}
			int indexType = rewriteForNumberVariables(arrayIndex, NumberType);
			if (indexType == NumberType) {
				if (!convertParameter(arrayIndex)) {
					// setting the ISNUMBER_PROP signals the codegen
					// to use the OptRuntime.getObjectIndex that takes
					// a double index
					n.putIntProp(Node.ISNUMBER_PROP, Node.RIGHT);
				}
			}
			return NoType;
		}
		case Token.CALL: {
			Node child = n.getFirstChild(); // the function node
			// must be an object
			rewriteAsObjectChildren(child, child.getFirstChild());
			child = child.getNext(); // the first arg

			OptFunctionNode target = (OptFunctionNode) n
					.getProp(Node.DIRECTCALL_PROP);
			if (target != null) {
				/*
				 * we leave each child as a Number if it can be. The codegen
				 * will handle moving the pairs of parameters.
				 */
				while (child != null) {
					int type = rewriteForNumberVariables(child, NumberType);
					if (type == NumberType) {
						markDCPNumberContext(child);
					}
					child = child.getNext();
				}
			} else {
				rewriteAsObjectChildren(n, child);
			}
			return NoType;
		}
		default: {
			rewriteAsObjectChildren(n, n.getFirstChild());
			return NoType;
		}
		}
	}

	private void rewriteAsObjectChildren(Node n, Node child) {
		// Force optimized children to be objects
		while (child != null) {
			Node nextChild = child.getNext();
			int type = rewriteForNumberVariables(child, NoType);
			if (type == NumberType) {
				if (!convertParameter(child)) {
					n.removeChild(child);
					Node nuChild = new Node(Token.TO_OBJECT, child);
					if (nextChild == null)
						n.addChildToBack(nuChild);
					else
						n.addChildBefore(nuChild, nextChild);
				}
			}
			child = nextChild;
		}
	}

	private static void buildStatementList_r(Node node, ObjArray statements) {
		int type = node.getType();
		if (type == Token.BLOCK || type == Token.LOCAL_BLOCK
				|| type == Token.LOOP || type == Token.FUNCTION) {
			Node child = node.getFirstChild();
			while (child != null) {
				buildStatementList_r(child, statements);
				child = child.getNext();
			}
		} else {
			statements.add(node);
		}
	}

	private boolean inDirectCallFunction;
	OptFunctionNode theFunction;
	private boolean parameterUsedInNumberContext;
}
