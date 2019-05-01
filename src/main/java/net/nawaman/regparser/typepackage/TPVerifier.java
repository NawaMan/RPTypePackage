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

import net.nawaman.regparser.PTypeProvider;
import net.nawaman.regparser.ParseResult;
import net.nawaman.regparser.RPVerifier;
import net.nawaman.script.CompileOption;
import net.nawaman.script.Function;
import net.nawaman.script.ProblemContainer;
import net.nawaman.script.Signature;

/**
 * RPVerifier from a function
 * 
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 **/
public class TPVerifier implements RPVerifier, Serializable {
	
	/** Signature for the verifier function */
	static public final Signature SVerifier = new Signature.Simple("Verifier", Boolean.class, false,
							PTypePackage.class,
							ParseResult.class,
							ParseResult.class,
							String.class,
							PTypeProvider.class
						);

	static public final String PNTypePackage        = "$TPackage";
	static public final String PNHostParseResult    = "$HostResult";
	static public final String PNThisParseResult    = "$ThisResult";
	static public final String PNParam              = "$Param";
	static public final String PNTypeProvider       = "$TProvider";
	
	/** Constructor of TPVerifier */
	public TPVerifier(PTypePackage pTypePackage, String pTypeName, String pCode) {
		this(pTypePackage, pTypeName, pCode, null, null);
	}
	/** Constructor of TPVerifier */
	public TPVerifier(PTypePackage pTypePackage, String pTypeName, String pCode, CompileOption pOption, ProblemContainer pResult) {
		this(pTypePackage,
			PTypePackage.newReplacedFunction(
				pTypeName,
				new Signature.Simple(((pTypeName == null)?"":pTypeName) + SVerifier.getName(), SVerifier),
				new String[] { PNTypePackage, PNHostParseResult, PNThisParseResult, PNParam, PNTypeProvider },
				pCode, pOption, pResult,
				PNThisParseResult, PNTypePackage, null, null
			)
		);
	}
	/** Constructor of TPVerifier */
	public TPVerifier(PTypePackage pTypePackage, Function pVerifyFunction) {
		this.TypePackage    = pTypePackage; 
		this.VerifyFunction = pVerifyFunction;
		Signature.Simple.canAImplementsB(this.VerifyFunction.getSignature(), SVerifier);
	}
	
	PTypePackage TypePackage    = null;
	Function     VerifyFunction = null;

	/** Validate the parse result */
	public boolean validate(ParseResult pHostResult, ParseResult pThisResult, String pParam, PTypeProvider pProvider) {
		PTypeProvider TP = PTypeProvider.Library.getEither(this.TypePackage, pProvider);
		return (Boolean)this.VerifyFunction.run(this.TypePackage, pHostResult, pThisResult, pParam, TP);
	}

}
