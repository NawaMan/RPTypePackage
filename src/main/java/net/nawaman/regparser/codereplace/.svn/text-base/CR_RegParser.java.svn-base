/*----------------------------------------------------------------------------------------------------------------------
 * Copyright (C) 2008 Nawapunth Manusitthipol. Implements with and for Sun Java 1.6 JDK.
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

import java.util.HashMap;

import net.nawaman.regparser.CompilationContext;
import net.nawaman.regparser.PType;
import net.nawaman.regparser.PTypeProvider;
import net.nawaman.regparser.ParseResult;
import net.nawaman.regparser.RPCompiler;
import net.nawaman.regparser.RegParser;

/**
 * Code replacer for RegParser
 *
 * @author Nawapunth Manusitthipol
 **/
abstract public class CR_RegParser extends CodeReplacer {

	// NOTE:
	//    $$...$$ for entry text
	//    ~~...~~ for entry name
	//    %%...%% for entry type
	//    **...** for entry type name
	//    &&...&& for entry location (col, row)
	//    ^^...^^ for entry location (pointer)
	//    ::...:: for entry position
	//    @@...@@ for entry value
	//    @$...@$ for entry value as text
	//    ##...## for entry sub entry
	//    ->      access to entry in the sub entry
	//
	//    The ... inside may be
	//       1. Name of the entry  (starts with $ or #) NOTE: if the name ends with "*", get all the values 
	//       2. Index of the entry ([number])
	
	/** The name of the RegParser language */
	static public final String RegParserLanguageName = "RegParser";
	
	/** The parser type name for parsing CodeBody */
	static public final String PT_CodeBody = "CodeBody";
	
	/** The parser type name for parsing the token to be replaced */
	static public final String PT_ToBeReplaced = "ToBeReplaced";

	/** Code of the parser for extracting sub-replacement */
	static String SubParserStr =
		"("+
		"	($Link:~[:-:][:>:]~)[:WhiteSpace:]*"+
		"	(^[:@:][:$:]||($Mark:~[[:$:][:~:][:%:][:*:][:&:][:^:][:::][:@:][:#:]]~)($Mark;)){0}"+
		")?"+ 
		"("+
		"	($Kind:~[:*:]{2}~)"+
		"	($Rest:~"+
		"		[[:$:][:#:][:[:]](^(($Kind;)|[:WhiteSpace:]))+ [:*:]{3}"+
		"		~:~"+
		"		($Name:~(^[:*:]{2}.{0})*+~)"+
		"		($Kind:~[:*:]{2}~)"+
		"	~)"+
		"	||"+
		"	($Kind:~([:@:][:$:]||($Mark:~[[:$:][:~:][:%:][:*:][:&:][:^:][:::][:@:][:#:]]~)($Mark;))~)"+
		"	($Name:~[[:$:][:#:][:[:]](^(($Kind;)|[:WhiteSpace:]))+~)"+
		"	($Kind;)"+
		")";
	
	private final PTypeProvider.Extensible PTProvider;
	
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
	protected CR_RegParser(String pRefParser, boolean pIsForCodeBody_AndNotForOthers) {
		
		if((pRefParser == null) || (pRefParser.length() == 0))
			throw new IllegalArgumentException("Regular Parser for code body or for selecting other token must be provided.");
		
		final CR_RegParser This = this;
		
		this.PTProvider = new PTypeProvider.Extensible();
		this.PTProvider.addType(
			PT_ToBeReplaced,
			RegParser.newRegParser(this.PTProvider, SubParserStr),
			new RPCompiler() {
				public Object compile(ParseResult $ThisResult, int $EIndex, String pParam, CompilationContext pContext,
						PTypeProvider $Provider) {
					ParseResult $Result = $ThisResult.getSubOf($EIndex);
					if($Result == null) {
						String T = $ThisResult.textOf($EIndex);
						((CContext)pContext).SB.append(T);
						return T;
					}
					
					String Kind = $Result.textOf("$Kind");
					if((Kind == null) || (Kind.length() < 1)) {
						String T = $ThisResult.textOf($EIndex);
						((CContext)pContext).SB.append(T);
						return T;
					}
					
					String  Name       =  $Result.textOf("$Name");             if(Name == null) Name = "";
					boolean IsLink     = ($Result.textOf("$Link") != null);
					boolean IsMultiple = Name.endsWith("*");                   if(IsMultiple) Name = Name.substring(0, Name.length() - 1);
					String  ResultName = IsLink?"":CodeReplacer.VNAME_Result;
					StringBuilder SB   = ((CContext)pContext).SB;
					
					boolean IsIndex = Name.startsWith("[");
					if(IsIndex) Name = Name.substring(1, Name.length() - 1);
					else        Name = This.getString(Name);
					
					char K = Kind.charAt(0);
					switch(K) {
						case '$': return This.replaceForText(      ResultName, IsMultiple, Name, SB);
						case '~': return This.replaceForName(      ResultName, IsMultiple, Name, SB);
						case '*': return This.replaceForTypeName(  ResultName, IsMultiple, Name, SB);
						case '&': return This.replaceForLocationCR(ResultName, IsMultiple, Name, SB);
						case '^': return This.replaceForLocation(  ResultName, IsMultiple, Name, SB);
						case ':': return This.replaceForPosition(  ResultName, IsMultiple, Name, SB);
						case '#': return This.replaceForSub(       ResultName, IsMultiple, Name, SB);
						case '%': {
							String TPName = ((CContext)pContext).TPName;
							return This.replaceForType(TPName, ResultName, IsMultiple, Name, SB);
						}
						case '@': {
							String TPName = ((CContext)pContext).TPName;
							String CCName = ((CContext)pContext).CCName;
							return This.replaceForValue(Kind.charAt(1) == '$', TPName, CCName, ResultName, IsMultiple, Name, SB);
						}
					}
					
					String T = $ThisResult.textOf($EIndex);
					((CContext)pContext).SB.append(T);
					return T;
				}
			}
		);
		String CodeBodyRP = 
			pIsForCodeBody_AndNotForOthers
				? pRefParser
				: String.format("(($Other[]:~%s~)|(#ToBeReplaced:!ToBeReplaced!))*" ,pRefParser);
		
		this.PTProvider.addType(
			PT_CodeBody,
			RegParser.newRegParser(this.PTProvider, CodeBodyRP),
			new RPCompiler() {
				public Object compile(ParseResult $ThisResult, int $EIndex, String pParam, CompilationContext pContext,
						PTypeProvider $Provider) {
					
					ParseResult $Result = $ThisResult.getSubOf($EIndex);
					if($Result == null) {
						String T = $ThisResult.textOf($EIndex);
						((CContext)pContext).SB.append(T);
						return T;
					}
					
					StringBuilder SB = new StringBuilder();
					int Count = $Result.count();
					for(int i = 0; i < Count; i++) {
						if(PT_ToBeReplaced.equals($Result.typeNameOf(i))) {
							Object O = $Result.valueOf(i, $Provider, pContext);
							SB.append((O == null)?"":O.toString());
						} else {
							String T = $Result.textOf(i);
							((CContext)pContext).SB.append(T);
							SB.append(T);
						}
					}
					return SB.toString();
				}
					
			}
		);
	}
	
	/** Compilation context for the code replace compilation */
	static class CContext extends CompilationContext.Simple {
		CContext(String pTypePackageName, String pCCName, StringBuilder pSB) {
			this.TPName     = pTypePackageName;
			this.CCName     = pCCName;
			this.SB         = pSB;
		}
		final String        TPName;
		final String        CCName;
		final StringBuilder SB;
	}
	
	/** Returns the name of this replace language */
	@Override public String getReplaceLanguageName() { return RegParserLanguageName; }
	
	/** Decodes the engine specifier for the target engine from the code's language parameter */
	@Override public EngineSpecifier decodeEngineSpecifier(String pLanguageParam) {
		if(pLanguageParam == null) return null;
		int Index = pLanguageParam.indexOf(' ');
		if(Index == -1) return new EngineSpecifier(pLanguageParam, "");
		else            return new EngineSpecifier(pLanguageParam.substring(0, Index), pLanguageParam.substring(Index + 1));
	} 

	/** Replace all the code result entry */
	@Override protected void replaceBody(ReplaceParseResult RPResult, String pTypeName, String ThisResultName,
			String TypePackageName, String pCCName, HashMap<String, String> pParams, StringBuilder SB) {
		String Body = RPResult.getBody();

		PType PT = this.PTProvider.getType(PT_CodeBody);
		CContext CC = new CContext(TypePackageName, pCCName, SB);
		PT.compile(Body, null, CC, this.PTProvider);
	}

}
