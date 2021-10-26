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

import net.nawaman.regparser.CompilationContext;
import net.nawaman.regparser.PTypeProvider;
import net.nawaman.regparser.ParseResult;
import net.nawaman.regparser.RPCompiler;
import net.nawaman.script.CompileOption;
import net.nawaman.script.Function;
import net.nawaman.script.ProblemContainer;
import net.nawaman.script.Signature;

/**
 * Compiler - for Composable type. The compilation is from a function
 *
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 **/
public class TPCompiler implements RPCompiler, Serializable {
	
    private static final long serialVersionUID = 7469878423176589404L;

    /** Signature for the compiler function */
	static public final Signature SCompiler = new Signature.Simple("Compiler", Object.class, false,
							PTypePackage.class,
							ParseResult.class,
							Integer.class,
							String.class,
							CompilationContext.class,
							PTypeProvider.class
						);

	static public final String PNTypePackage        = "$TPackage";
	static public final String PNParseResult        = "$ThisResult";
	static public final String PNEntryIndex         = "$EIndex";
	static public final String PNParam              = "$Param";
	static public final String PNCompilationContext = "$CContext";
	static public final String PNTypeProvider       = "$TProvider";
	
	/** Constructor of TPCompiler */
	public TPCompiler(PTypePackage pTypePackage, String pTypeName, String pCode) {
		this(pTypePackage, pTypeName, pCode, null, null);
	}
	/** Constructor of TPCompiler */
	public TPCompiler(PTypePackage pTypePackage, String pTypeName, String pCode, CompileOption pOption, ProblemContainer pResult) {
		this(pTypePackage,
			PTypePackage.newReplacedFunction(
				pTypeName,
				new Signature.Simple(((pTypeName == null)?"":pTypeName) + SCompiler.getName(), SCompiler),
				new String[] { PNTypePackage, PNParseResult, PNEntryIndex, PNParam, PNCompilationContext, PNTypeProvider },
				pCode, pOption, pResult,
				PNParseResult, PNTypePackage, PNEntryIndex, PNCompilationContext
			)
		);
	}
	/** Constructor of TPCompiler */
	public TPCompiler(PTypePackage pTypePackage, Function pCompileFunction) {
		this.TypePackage     = pTypePackage; 
		this.CompileFunction = pCompileFunction;
		Signature.Simple.canAImplementsB(this.CompileFunction.getSignature(), SCompiler);
	}
	
	PTypePackage TypePackage     = null;
	Function     CompileFunction = null;
	
	/** Compiles a ParseResult in to an object with a parameter */
	public Object compile(ParseResult pThisResult, int pEntryIndex, String pParam, CompilationContext pContext,
			PTypeProvider pProvider) {
		PTypeProvider TP = PTypeProvider.Library.getEither(this.TypePackage, pProvider);
		return this.CompileFunction.run(this.TypePackage, pThisResult, pEntryIndex, pParam, pContext, TP);
	}
}