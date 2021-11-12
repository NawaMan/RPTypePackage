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

import java.util.HashMap;

import net.nawaman.regparser.result.ParseResult;
import net.nawaman.script.Scope;
import net.nawaman.script.ScriptEngine;
import net.nawaman.script.ScriptManager;

/**
 * Code replacer that makes writing functions for handling ParseReslt easy (such as compiler or verifier).
 *
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 **/
abstract public class CodeReplacer {
	
	/** The name of the virtual variable for the original text */
	static public final String VNAME_OrgText   = "$OrgText";
	/** The name of the virtual variable for the parse type name */
	static public final String VNAME_TypeName  = "$TypeName";
	/** The name of the virtual variable for the parse result as a text */
	static public final String VNAME_Text      = "$Text";
	/** The name of the virtual variable for the type package main scope */
	static public final String VNAME_MainScope = "$MainScope";
	/** The name of the virtual variable for the parse result as parse result (The sub result not the result entry) */
	static public final String VNAME_Result    = "$Result";
	
	// Services --------------------------------------------------------------------------------------------------------
	
	static final HashMap<String, CodeReplacer> CReplacers = new HashMap<String, CodeReplacer>();

	/** Returns a code replacer by name */
	static public boolean registerCodeReplacer(CodeReplacer pCReplacer) {
		if(pCReplacer == null) return false;
		if(CodeReplacer.getCodeReplacer(pCReplacer.getName()) != null) return false;
		CodeReplacer.CReplacers.put(pCReplacer.getName(), pCReplacer);
		return true;
	}
	
	/** Returns a code replacer by name */
	static public CodeReplacer getCodeReplacer(String pCRName) {
		return CodeReplacer.CReplacers.get(pCRName);
	}
	
	/** Returns a code replacer that can replace the code */
	static public CodeReplacer getCodeReplacerFromCode(String pCode) {
		// Extract the language name and parameter
		String[] ENameParam = ScriptManager.GetEngineNameAndParamFromCode(pCode);
		if(ENameParam == null) return null;
		
		String LanguageName  = ENameParam[0]; if(LanguageName  == null) LanguageName  = "";
		String LanguageParam = ENameParam[1]; if(LanguageParam == null) LanguageParam = "";
		
		CodeReplacer CReplacer = null;
		
		for(String CRName : CodeReplacer.CReplacers.keySet()) {
			CReplacer = CodeReplacer.CReplacers.get(CRName);
			if(CReplacer == null) continue;
			// Ensure the replace language -------------------------------------------------------------
			if(!LanguageName.equals(CReplacer.getReplaceLanguageName())) continue;
			// Ensure target ---------------------------------------------------------------------------
			EngineSpecifier EngineSpecifier = CReplacer.decodeEngineSpecifier(LanguageParam);
			if((EngineSpecifier == null) || !CReplacer.isTargetEngine(EngineSpecifier)) continue;
			
			return CReplacer;
		}
		
		return null;
	}

	/** Perform the code replacing of the whole code */
	static public String replaceCode(String pCode, String pTypeName, String ThisResultName, String TypePackageName,
			String pCCName, HashMap<String, String> pParams) {
		
		// Get the replacer
		CodeReplacer CReplacer = CodeReplacer.getCodeReplacerFromCode(pCode);
		if(CReplacer == null) {
			ScriptEngine SE = null;
			try { SE = ScriptManager.GetEngineFromCode(pCode); }
			catch (Exception E) {}
			if(SE != null) return pCode;
			throw new IllegalArgumentException("Unable to extract the code replacer from the given code.");
		}
		
		// Extract the language name and parameter
		String          LanguageParam = ScriptManager.GetEngineNameAndParamFromCode(pCode)[1];
		EngineSpecifier TargetEngine  = CReplacer.decodeEngineSpecifier((LanguageParam == null)?"":LanguageParam);
		return CReplacer.replaceCode(pCode, TargetEngine, pTypeName, ThisResultName, TypePackageName, pCCName, pParams);
	}
	
	// Auxitary classes ------------------------------------------------------------------------------------------------
	
	/** Result of the replace code parsing */
	static public interface ReplaceParseResult {
		/** Return the prefix (after the engine identifier but before the replaced code) */
		public String getPrefix();
		/** Return the Body of the code (the one to be replaced) */
		public String getBody();
		/** Return the prefix (after the replaced code) */
		public String getSuffix();

		static public class Simple implements ReplaceParseResult {
			public Simple(String pPrefix, String pBody, String pSuffix) {
				this.Prefix = pPrefix;
				this.Body   = pBody;
				this.Suffix = pSuffix;
			}
			String Prefix = null;
			String Body   = null;
			String Suffix = null;
			/**{@inheritDoc}*/ @Override public String getPrefix() { return this.Prefix; }
			/**{@inheritDoc}*/ @Override public String getBody()   { return this.Body; }
			/**{@inheritDoc}*/ @Override public String getSuffix() { return this.Suffix; }
		} 
	}
	
	/** Engine specifier for specifying the target code */
	static public class EngineSpecifier {
		public EngineSpecifier(String pName, String pParam) {
			this.Name  = pName;
			this.Param = pParam;
		}
		
		String Name; String Param;
		
		/** Returns the name of the target engine */
		public String getName()  { return this.Name;  }
		/** Returns the parameter of the target engine */
		public String getParam() { return this.Param; }
		
		@Override public String toString() {
			return String.format("// @%s(%s):", this.Name, this.Param);
		}
	}
	
	// Implementation --------------------------------------------------------------------------------------------------
	
	// Information ---------------------------------------------------------------------------------
	
	/** Returns the replacer name */
	abstract public String getName();
	
	/** Returns the name of this replace language */
	abstract public String getReplaceLanguageName();
	
	/** Checks if the given target language is supported by this replacer */
	abstract public boolean isTargetEngine(EngineSpecifier pESpecifier);
		
	// Language specific ---------------------------------------------------------------------------
	
	/** Returns the engine identification line of the target language */
	abstract public String getEngineIdentifierLine(EngineSpecifier pESpecifier);
	
	/** Returns the string representation for a self contain comment (the one that need no new line) */
	abstract public String getCommentOf(String pComment);

	/** Returns the string representation for a null value */
	abstract public String getNull();

	/** Returns the string representation for a string literal */
	abstract public String getString(String pStrLiteral);
	
	/** Returns the string representation for a local variable */
	abstract public String getVar(String pVariableName);

	/** Returns the string representation for an object's method invocation */
	abstract public String getCall(String pObject, String pMethod, String ... pParams);

	/** Returns the string representation for declaring a new constant */
	abstract public String getNew(Class<?> pType, String pName, String pValue);
	
	// Parsing ---------------------------------------------------------------------------------------------------------
	
	/** Parse the code */
	abstract public ReplaceParseResult parseCode(String pCode);
	
	/** Decodes the engine specifier for the target engine from the code's language parameter */
	abstract public EngineSpecifier decodeEngineSpecifier(String pLanguageParam);

	// Replacing -------------------------------------------------------------------------------------------------------
	
	/** Replace all the code result entry */
	abstract protected void replaceBody(ReplaceParseResult RPResult, String pTypeName, String ThisResultName,
			String TypePackageName, String pCCName, HashMap<String, String> pParams, StringBuilder SB);
	
	/** Add more predefined parameters */
	public void addExtraPredefinedParameters(EngineSpecifier pTargetEngine, String pTypeName, String ThisResultName,
			String TypePackageName, String pCCName, HashMap<String, String> pParams) {}
	
	/**
	 * Perform the code replacing of the whole code.
	 * 
	 * @param pCode	           the original code
	 * @param pTargetEngine    the target engine specifier
	 * @param pTypeName        the string of the parser type this code will be used for
	 * @param pThisResultName  the name of the variable visible to this code that holds the result name
	 * @param pTypePackageName the name of the variable visible to this code that holds the type package name
	 **/
	final public String replaceCode(String pCode, EngineSpecifier pTargetEngine, String pTypeName, String pThisResultName,
			String pTypePackageName, String pCCName, HashMap<String, String> pParams) {
		// Extract the language name and parameter
		String[] ENameParam = ScriptManager.GetEngineNameAndParamFromCode(pCode);
		if(ENameParam == null)
			throw new IllegalArgumentException("Unable to extract the script engine name from the given code.");
		
		String LanguageName  = ENameParam[0]; if(LanguageName  == null) LanguageName  = "";
		String LanguageParam = ENameParam[1]; if(LanguageParam == null) LanguageParam = "";
		
		if(!LanguageName.equals(this.getReplaceLanguageName()) || !this.isTargetEngine(pTargetEngine))
			throw new IllegalArgumentException("The given code cannot be replaced by this replacer.");
		
		// Eliminate the Engine name line (the first line) - Which is // @RegParser(...):\n
		// It will be replaced with //@...:\n
		pCode = pCode.substring(pCode.indexOf('\n') + 1);
		
		ReplaceParseResult RPResult = this.parseCode(pCode);
		if(RPResult == null) throw new IllegalArgumentException("Unknown error parsing the given code.");
		
		// Formulate the replaced code
		StringBuilder Result = new StringBuilder();
		// Add the target engine identifier line
		Result.append(this.getEngineIdentifierLine(pTargetEngine));

		String Prefix = RPResult.getPrefix();
		Result.append((Prefix == null)?"":Prefix);
		
		// Declare predefine variables -------------------------------------------------------------
		
		Result.append(this.getCommentOf(" Predefined variables "));

		// Add Parser Type Name
		if(pTypeName != null)
			Result.append(this.getNew(String.class, VNAME_TypeName, this.getString(pTypeName)));
		
		if(pThisResultName != null) {
			// Add the original text
			Result.append(this.getNew(String.class, VNAME_OrgText, this.getCall(this.getVar(pThisResultName), "originalText")));

			String EIndexName = (pParams == null)?null:pParams.get("EntryIndex");
			if(EIndexName == null) {
				// Add Text Parse result and Sub Parse result
				Result.append(this.getNew(String     .class, VNAME_Text,   this.getCall(this.getVar(pThisResultName), "text")));
				Result.append(this.getNew(ParseResult.class, VNAME_Result, this.getNull()));
			} else {
				// Add Text Parse result and Sub Parse result
				Result.append(this.getNew(String     .class, VNAME_Text,   this.getCall(this.getVar(pThisResultName), "textOf",      this.getVar(EIndexName))));
				Result.append(this.getNew(ParseResult.class, VNAME_Result, this.getCall(this.getVar(pThisResultName), "subResultOf", this.getVar(EIndexName))));
			}
		}

		// Add MainScope
		if(pTypePackageName != null)
			Result.append(this.getNew(Scope.Simple.class, VNAME_MainScope, this.getCall(this.getVar(pTypePackageName), "getMainScope")));
		
		// Add more if needed
		this.addExtraPredefinedParameters(pTargetEngine, pTypeName, pThisResultName, pTypePackageName, pCCName, pParams);
		
		Result.append(this.getCommentOf(" The code "));
		
		// The replace the code body ---------------------------------------------------------------
		StringBuilder SB = new StringBuilder();
		this.replaceBody(RPResult, pTypeName, pThisResultName, pTypePackageName, pCCName, pParams, SB);
		Result.append(SB.toString());
		
		// The rest --------------------------------------------------------------------------------
		Result.append(this.getCommentOf(" End of the code "));
		String Suffix = RPResult.getSuffix();
		Result.append((Suffix == null)?"":Suffix);
		
		return Result.toString();
	}
	
	// Replace the properties ------------------------------------------------------------------------------------------
	
	/** Replace a simple property (only one parameter which is the entry name) */
	private String rS(String ResultName, boolean IsMultiple, String EntryName, String Kind, StringBuilder SB) {
		String Result = this.getCall(this.getVar(ResultName), String.format("%s%sOf", Kind, (IsMultiple?"s":"")), EntryName);
		if(SB != null) SB.append(Result);
		return Result;
	}
	
	/** Replace a simple property (only one parameter which is the entry name) */
	final protected void replaceSimple(String ResultName, boolean IsMultiple, String EntryName, String Kind, StringBuilder SB) {
		this.rS(ResultName, IsMultiple, EntryName, Kind, SB);
	}
	
	// NOTE:
	//	RN = ResultName
	//	IM = IsMultiple
	//	EN = EntryName
	//	SB = StringBuilder
	
	/** Replace text */
	final public String replaceForText      (String RN, boolean IM, String EN, StringBuilder SB) { return this.rS(RN, IM, EN, "text",          SB); }
	/** Replace name */
	final public String replaceForName      (String RN, boolean IM, String EN, StringBuilder SB) { return this.rS(RN, IM, EN, "name",          SB); }
	/** Replace sub */
	final public String replaceForSub       (String RN, boolean IM, String EN, StringBuilder SB) { return this.rS(RN, IM, EN,  "subResult",    SB); }
	/** Replace type name */
	final public String replaceForTypeName  (String RN, boolean IM, String EN, StringBuilder SB) { return this.rS(RN, IM, EN, "typeName",      SB); }
	/** Replace (col,row) */
	final public String replaceForCoordinate(String RN, boolean IM, String EN, StringBuilder SB) { return this.rS(RN, IM, EN, "coordinate",    SB); }
	/** Replace location */
	final public String replaceForLocation  (String RN, boolean IM, String EN, StringBuilder SB) { return this.rS(RN, IM, EN, "location",      SB); }
	/** Replace position */
	final public String replaceForPosition  (String RN, boolean IM, String EN, StringBuilder SB) { return this.rS(RN, IM, EN, "startPosition", SB); }

	/** Replace type */
	final public String replaceForType(String TypePackageName, String ResultName, boolean IsMultiple,
			String EntryName, StringBuilder SB) {
		String Result = this.getCall(
				this.getVar(ResultName),
				String.format("type%sOf", (IsMultiple?"s":"")),
				EntryName,
				TypePackageName
			);
			if(SB != null) SB.append(Result);
			return Result;
	}
	
	/** Replace compiled value */
	final public String replaceForValue(boolean IsAsText, String TypePackageName, String pCContextName,
			String ResultName, boolean IsMultiple, String EntryName, StringBuilder SB) {
		String Result = this.getCall(
			this.getVar(ResultName),
			String.format("value%s%sOf", IsAsText?"AsText":"", (IsMultiple?"s":"")),
			EntryName,
			TypePackageName,
			((pCContextName == null)?this.getNull():pCContextName)
		);
		if(SB != null) SB.append(Result);
		return Result;
	}}
