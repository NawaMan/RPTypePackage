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

import net.nawaman.script.java.JavaEngine;

/** 
 * Code replacer from Java to RefParser
 * 
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 **/
public class RegParser2Java extends CR_RegParser_JavaLiked {

	static public final String         Name     = "RegParser2Java";
	static public final RegParser2Java Instance = new RegParser2Java();
	
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
	protected RegParser2Java(String pRefParser, boolean pIsForCodeBody_AndNotForOthers) {
		super(pRefParser, pIsForCodeBody_AndNotForOthers);
	}

	/** Constructs a CodeReplace for RegParser language. */
	protected RegParser2Java() {
		super();
	}
	
	/**{@inheritDoc}*/ @Override
	public String getName() {
		return Name;
	}
	
	/**{@inheritDoc}*/ @Override
	public boolean isTargetEngine(EngineSpecifier pESpecifier) {
		return JavaEngine.ShortName.equals(pESpecifier.getName());
	}

	/**{@inheritDoc}*/ @Override
	public String getNew(Class<?> pType, String pName, String pValue) {
		String Type = (pType == null)?null:pType.getCanonicalName(); if(Type == null) Type = "Object";
		return String.format("final %s %s = %s;", Type, pName, pValue);
	}
	
	// Parsing ---------------------------------------------------------------------------------------------------------
	
	/**{@inheritDoc}*/ @Override
	public ReplaceParseResult parseCode(String pCode) {
		int[] IEEnds = JavaEngine.getImportEndAndElementEndAndAnnotationEnd(pCode);
		String Prefix = pCode.substring(0, IEEnds[2]);
		String Body   = pCode.substring(IEEnds[2]);
		return new ReplaceParseResult.Simple(Prefix, Body, "");
	}	
}
