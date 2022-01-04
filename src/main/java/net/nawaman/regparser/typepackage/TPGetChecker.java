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

package net.nawaman.regparser.typepackage;

import java.io.Serializable;

import net.nawaman.regparser.Checker;
import net.nawaman.regparser.ParserTypeProvider;
import net.nawaman.regparser.result.ParseResult;
import net.nawaman.regparser.types.CheckerProvider;
import net.nawaman.script.CompileOption;
import net.nawaman.script.Function;
import net.nawaman.script.ProblemContainer;
import net.nawaman.script.ScriptManager;
import net.nawaman.script.Signature;

/**
 * Parser Type that get its Checker from a function
 *
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 **/
public class TPGetChecker implements CheckerProvider, Serializable {
	
    private static final long serialVersionUID = 831095862697228605L;

    /** Signature for the getChecker function */
	static public final Signature SGetChecker = new Signature.Simple("GetChecker", Object.class, false,
							PTypePackage.class,
							ParseResult.class,
							String.class,
							ParserTypeProvider.class
						);

	static public final String PNTypePackage  = "$TPackage";
	static public final String PNHostResult   = "$HostResult";
	static public final String PNParam        = "$Param";
	static public final String PNTypeProvider = "$TProvider";
	
	/** Constructor of TPGetChecker */
	public TPGetChecker(PTypePackage pTypePackage, String pTypeName, String pCode) {
		this(pTypePackage, pTypeName, pCode, null, null);
	}
	/** Constructor of TPGetChecker */
	public TPGetChecker(PTypePackage pTypePackage, String pTypeName, String pCode, CompileOption pOption, ProblemContainer pResult) {
		this(pTypePackage,
			ScriptManager.Instance.newFunction(
				new Signature.Simple(((pTypeName == null)?"":pTypeName) + SGetChecker.getName(), SGetChecker),
				new String[] { PNTypePackage, PNHostResult, PNParam, PNTypeProvider },
				pCode, null, null, pOption, pResult
			)
		);
	}
	/** Constructor of TPCompiler */
	public TPGetChecker(PTypePackage pTypePackage, Function pCompileFunction) {
		this.TypePackage     = pTypePackage; 
		this.GetCheckerFunction = pCompileFunction;
		Signature.Simple.canAImplementsB(this.GetCheckerFunction.getSignature(), SGetChecker);
	}
	
	PTypePackage TypePackage        = null;
	Function     GetCheckerFunction = null;

	/** Returns the checker */
	public Checker getChecker(ParseResult pHostResult, String pParam, ParserTypeProvider pProvider) {
		ParserTypeProvider TP = ParserTypeProvider.Library.either(this.TypePackage, pProvider);
		return (Checker)this.GetCheckerFunction.run(this.TypePackage, pHostResult, pParam, TP);
	}
}