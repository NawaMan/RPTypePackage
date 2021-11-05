/*----------------------------------------------------------------------------------------------------------------------
 * Copyright (C) 2008-2019 Nawapunth Manusitthipol. Implements with and for Sun Java 1.6 JDK.
 *----------------------------------------------------------------------------------------------------------------------
 * LICENSE:
 * 
 * This file is part of Nawa's RPTypePackage.
 * 
 * The project is a free software; you can redistribute it and/or modify it under the SIMILAR terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or any later version.
 * You are only required to inform me about your modification and redistribution as or as part of commercial software
 * package. You can inform me via nawaman<at>gmail<dot>com.
 * 
 * The project is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the 
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 * ---------------------------------------------------------------------------------------------------------------------
 */

package net.nawaman.regparser.swing;

import java.util.Hashtable;

import net.nawaman.regparser.Util;
import net.nawaman.regparser.result.ParseResult;
import sun.sdn.swing.treetable.AbstractTreeTableModel;
import sun.sdn.swing.treetable.JTreeTable;
import sun.sdn.swing.treetable.TreeTableModel;

/**
 * Tree-table model for displaying parse result
 *
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 **/
public class ParseResultTreeTableModel extends AbstractTreeTableModel implements
		TreeTableModel {

	// Names of the columns.
	static protected String[] cNames = { "Text", "Name", "Type", "Start - End" };

	// Types of the columns.
	static protected Class<?>[] cTypes = { TreeTableModel.class, String.class, String.class, String.class };

	/** Constructs a tree table model */
	public ParseResultTreeTableModel(ParseResult pPResult) { super(ParseResultTreeNode.getNode(pPResult)); }
	
	// The TreeModel interface -----------------------------------------------------------------------------------------
	
	/** The parse result tree node */
	static class ParseResultTreeNode {
		private ParseResultTreeNode(ParseResult pResult, int pEIndex) { this.Result = pResult; this.EIndex = pEIndex; }
		
		ParseResult Result;
		int         EIndex;
		
		@Override public int    hashCode() {
			return (this.Result == null)?0:this.Result.hashCode() ^ (this.EIndex << 10);
		}
		@Override public String toString() {
			if(this.Result == null) return "<NO RESULT>";
			String Text = Util.escapeText((this.EIndex == -1)?this.Result.text():this.Result.textAt(this.EIndex)).toString();
			if(Text.length() >= 50) Text = Text.substring(0, 47) + "...";
			String Order = ((this.EIndex == -1)?"":"" + this.EIndex);
			while(Order.length() < 2) Order = "0" + Order;
			return Order + ": " + ("\"" + Text + "\"");
		}
		
		static private Hashtable<Integer, ParseResultTreeNode> Nodes = new Hashtable<Integer, ParseResultTreeNode>();
		
		static ParseResultTreeNode getNode(ParseResult pResult) {
			return getNode(pResult, -1);
		}
		static ParseResultTreeNode getNode(ParseResult pResult, int pEIndex) {
			ParseResultTreeNode Node = Nodes.get(((pResult == null)?0:pResult.hashCode()) + (pEIndex << 10));
			if(Node != null) return Node;
			Node = new ParseResultTreeNode(pResult, pEIndex);
			Nodes.put(Node.hashCode(), Node);
			return Node;
		}
	}

	/** Returns the number of child of the node */
	@Override public int getChildCount(Object node) {
		ParseResultTreeNode Node = (ParseResultTreeNode)node;
		if(Node.Result == null) return 0;
		if(Node.EIndex == -1)   return Node.Result.entryCount();
		return Node.Result.subResultAt(Node.EIndex).entryCount();
	}

	/** Returns the child of the node at the index i */
	@Override public Object getChild(Object node, int i) {
		ParseResultTreeNode Node = (ParseResultTreeNode)node;
		if(Node.Result == null) return null;
		if(Node.EIndex == -1) return ParseResultTreeNode.getNode(Node.Result, i);
		return ParseResultTreeNode.getNode(Node.Result.subResultAt(Node.EIndex), i);
	}

	// The superclass's implementation would work, but this is more efficient.
	/** Checks if the node is a leaf */
	@Override public boolean isLeaf(Object node) {
		ParseResultTreeNode Node = (ParseResultTreeNode)node;
		if(Node.Result == null) return  true;
		if(Node.EIndex == -1)   return false;
		return !Node.Result.entryAt(Node.EIndex).hasSubResult();
	}

	// The TreeTableNode interface. ------------------------------------------------------------------------------------

	/** Returns the number of column */    @Override public int      getColumnCount()           { return cNames.length;  }
	/** Returns the name of the column */  @Override public String   getColumnName(int column)  { return cNames[column]; }
	/** Returns the class of the column */ @Override public Class<?> getColumnClass(int column) { return cTypes[column]; }

	/** Returns the value of the node and at the column */ @Override public Object getValueAt(Object node, int column) {
		ParseResultTreeNode Node = (ParseResultTreeNode)node;
		if(Node.Result == null) return "";
		if(Node.EIndex == -1) {
			ParseResult PR = Node.Result;
			switch (column) {
				case 0:
					String Text = Util.escapeText(PR.text()).toString();
					if(Text.length() >= 50) Text = Text.substring(0, 47) + "...";
					return Text;
				case 1:
					return "<No Name>";
				case 2:
					return "<No Type>";
				case 3:
					return String.format("[%4d-%4d]", PR.startPosition(), PR.endPosition());
			}
		} else {
		    var PRE = Node.Result.entryAt(Node.EIndex);
			switch (column) {
				case 0:
					String Text = Util.escapeText(Node.Result.textAt(Node.EIndex)).toString();
					if(Text.length() >= 50) Text = Text.substring(0, 47) + "...";
					return Text;
				case 1:
					String N = PRE.name();
					return (N == null)?"<No Name>":N;
				case 2:
					String TN = PRE.typeName();
					return (TN == null)?"<No Type>":TN;
				case 3:
					return String.format("[%4d-%4d]", Node.Result.startPositionAt(Node.EIndex), Node.Result.endPositionAt(Node.EIndex));
			}
		}

		return "";
	}
	
	/** TreeTable for displaying ParseResult */
	static public class JParseResultTreeTable extends JTreeTable {

        private static final long serialVersionUID = 2939683204561587535L;
        
        public JParseResultTreeTable() {
			super(new ParseResultTreeTableModel(null));
		}
		public JParseResultTreeTable(ParseResult pResult) {
			super(new ParseResultTreeTableModel(pResult));
		}
	}
}

