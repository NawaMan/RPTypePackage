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

package net.nawaman.regparser.codereplace;

import net.nawaman.regparser.Util;

/**
 * Code replacer for RegParser to a Java liked language.
 * 
 * Java liked language is defined as how a token is recognized in general. In this case, the following token types must be: 
 * 	1. String    literal is starts with " and ends with ". The " is escaped with \" and \ is escaped with \\. 
 * 	2. Character literal is starts with ' and ends with '. The ' is escaped with \' and \ is escaped with \\.
 *  3. Comments are
 *      3.1 Line comment: Starts with //
 *      3.2 Line comment: Starts with /* and ends with * /
 *  4. The rest of the token types cannot be ambiguous with RegParser replaced code (those $$...$$ things). 
 *
 * @author Nawapunth Manusitthipol
 **/
abstract public class CR_RegParser_JavaLiked extends CR_RegParser {
	
	/** Code of the parser for extracting code replacement */
	static String SimpleCodePraserStr = 
			"[:WhiteSpace:]*" +
			"(" +
			"	[^[:\":][:':][:/:][:$:][:~:][:#:][:%:][:*:][:&:][:^:][:@:][:::][:-:]]*" +
			"	(" +
			"		[:\":]([^[:\":]]|[:\\:][:\":])*[:\":]"+
			"		|"+
			"		[:':]([^[:':]]|[:\\:][:':])*[:':]"+
			"		|"+
			"		[:/:][:/:][^[:NewLine:]]*[:NewLine:]"+
			"		|"+
			"		[:/:][:*:](^[:*:][:/:])*[:*:][:/:]" +
			"		|"+
			"		(#ToBeReplaced:!"+CR_RegParser.PT_ToBeReplaced+"!)"+
			"		||"+
			"		." +
			"	)" +
			"	[^[:\":][:':][:/:][:$:][:~:][:#:][:%:][:*:][:&:][:^:][:@:][:::][:-:]]*" +
			"	||" +
			"	[[:\":][:':][:/:][:$:][:~:][:#:][:%:][:*:][:&:][:^:][:@:][:::][:-:]]"+
			")*" +
			"[^[:\":][:':][:/:][:$:][:~:][:#:][:%:][:*:][:&:][:^:][:@:][:::][:-:]]*";

	/**
	 * Constructs a CodeReplace for RegParser language.
	 * 
	 * NOTE:
	 *   If pIsForCodeBody_AndNotForOthers is true, the pRefParser is the string RegParser of the code body. The code
	 *       body must make use of a type called !ToBeReplaced!. The parse result of the type must be on the first level
	 *       of the final parse result. 
	 *   If pIsForCodeBody_AndNotForOthers is false, the pRefParser is the string RegParser of other token that will not
	 *       be replaced.
	 **/
	protected CR_RegParser_JavaLiked(String pRefParser, boolean pIsForCodeBody_AndNotForOthers) {
		super(pRefParser, pIsForCodeBody_AndNotForOthers);
	}

	/** Constructs a CodeReplace for RegParser language. */
	protected CR_RegParser_JavaLiked() {
		super(SimpleCodePraserStr, true);
	}

	/**{@inheritDoc}*/ @Override public String getEngineIdentifierLine(EngineSpecifier pESpecifier) {
		return String.format("// @%s%s:\n", pESpecifier.Name, (pESpecifier.Param == null)?"":String.format("(%s)", pESpecifier.Param));
	}
	/**{@inheritDoc}*/ @Override public String getCommentOf(String pComment) { return String.format("/*%s*/",pComment); }
	/**{@inheritDoc}*/ @Override public String getNull()                     { return "null"; }
	/**{@inheritDoc}*/ @Override public String getString(String pStrLiteral) { return String.format("\"%s\"",Util.escapeText(pStrLiteral)); }
	/**{@inheritDoc}*/ @Override public String getVar(String pVariableName)  { return pVariableName; }
	
	/**{@inheritDoc}*/ @Override public String getCall(String pObject, String pMethod, String ... pParams) {
		return String.format("%s.%s(%s)", pObject, pMethod, Util.toString(pParams, "", "", ","));
	}
	
}
