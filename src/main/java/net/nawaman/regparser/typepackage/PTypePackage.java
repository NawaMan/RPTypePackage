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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import net.nawaman.javacompiler.JavaCompiler;
import net.nawaman.javacompiler.JavaCompilerObjectInputStream;
import net.nawaman.regparser.PType;
import net.nawaman.regparser.PTypeProvider;
import net.nawaman.regparser.PTypeProviderPocket;
import net.nawaman.regparser.RPGetChecker;
import net.nawaman.regparser.RegParser;
import net.nawaman.regparser.Util;
import net.nawaman.regparser.codereplace.CodeReplacer;
import net.nawaman.regparser.codereplace.RegParser2Java;
import net.nawaman.regparser.codereplace.RegParser2JavaScript;
import net.nawaman.regparser.result.ParseResult;
import net.nawaman.regparser.typepackage.PTKind.Data;
import net.nawaman.regparser.types.PTComposable;
import net.nawaman.regparser.types.PTSimple;
import net.nawaman.script.CompileOption;
import net.nawaman.script.Function;
import net.nawaman.script.ProblemContainer;
import net.nawaman.script.Scope;
import net.nawaman.script.ScriptEngine;
import net.nawaman.script.ScriptManager;
import net.nawaman.script.Signature;
import net.nawaman.script.Tools;
import net.nawaman.script.Tools.ExtractResult;
import net.nawaman.util.UObject;

/**
 * Parser Type Provider that holds holds the type definitions as string. The type definition is a string that can be
 *   handled like a program code.
 *
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 **/
public class PTypePackage implements PTypeProvider {

	/** Ensure that TPackageScriptEngine is registed. */
	static public void EnsureEngineRegisted() {
		ScriptManager.Instance.getDefaultEngineOf(TPackageScriptEngine.Name);
	}
	
	// Make it very sure that the engine is registed.
	static {
		EnsureEngineRegisted();
	}
	
	/** Returns the SimpleScriptEngine for TypePackage. */
	static public TPackageScriptEngine getScriptEngine() {
		return (TPackageScriptEngine)ScriptManager.Instance.getDefaultEngineOf(TPackageScriptEngine.Name);
	}
	
	/** Use the PTypePackage form the Usepath */
	static public PTypePackage Use(String Name) {
		Object O = ScriptManager.Use(Name);
		return GetPTypePackageFromUsable(O);
	}
	/** Use the PTypePackage form the Usepath */
	static public PTypePackage UseWithException(String Name) throws IOException, ClassNotFoundException {
		Object O = ScriptManager.UseWithException(Name);
		return GetPTypePackageFromUsable(O);
	}
	
	/** Create an empty parser-type package */
	public PTypePackage() {
		this(null, (PType[])null);
	}
	
	/** Creates a parser-type package with a scope */
	public PTypePackage(PType ... pTypes) {
		this(null, pTypes);
	}
	
	/** Creates a parser-type package with a scope */
	public PTypePackage(Scope.Simple pScope) {
		this(pScope, (PType[])null);
	}
	
	/** Creates a parser-type package with a scope and a set of native types */
	public PTypePackage(Scope.Simple pScope, PType ... pTypes) {
		this.setMainScope(pScope);
		
		// Add native types
		if(pTypes != null) { for(PType T : pTypes) { if(T == null) continue; this.addNativeType(T); } }

		// Common Type for parsing type kind and spec
		ensureKindAndSpecParserProvider();
	}
	
	static public final int CommonKindCount = 2;
	
	/** Use common four type kinds (Simple, Error) */
	public void useCommonKinds() {
		this.addKind(PTypePackage.DefKindSimpleStr);
		this.addKind(PTypePackage.DefKindErrorStr);
	}
	
	static private final long serialVersionUID = 8174304425217519646L;
	
	/** Name of the property called 'Checker' */
	static public final String PropCheckerName = "Checker";
	
	/** Definition of Simple parser kind */
	static public final String DefKindSimpleStr = String.format(
		"#def_kind Simple:\n" +
		"\n"+
		"#Variables:\n" +
		"	var Checker;\n" +
		"	var Verifier;\n" +
		"	var Compiler;\n" +
		"#Constructor:	// function (final %s $TPackage, final %s $Spec):%s\n"+
		"	// @Java: \n" +	
		"	import net.nawaman.regparser.*;\n" +
		"	import net.nawaman.regparser.typepackage.*;\n" +
		"	import net.nawaman.script.*;\n" +
		"	\n"+	
		"	String Name     = (String)$Spec.getValue(\"Name\");\n" +
		"	String Checker  = (String)$Spec.getValue(\"Checker\");\n" +
		"	String Verifier = (String)$Spec.getValue(\"Verifier\");\n" +
		"	String Compiler = (String)$Spec.getValue(\"Compiler\");\n" +
		"	\n" +
		"	if(Name    == null) throw new NullPointerException(\"Parser type name cannot be null.\");\n" +
		"	if(Checker == null) throw new NullPointerException(\"Checker of a simple parser type cannot be null. (\"+Name+\")\");\n" +
		"	\n"+
		"	%s ParserCK = null;\n"+
		"	%s ParserGC = null;\n" +
		"	Object CkEn = null;" +
		"	try { CkEn = %s.GetEngineFromCode(Checker); } catch(RuntimeException RTE) {} \n"+
		"	if(CkEn == null) ParserCK = %s.newRegParser($TPackage, Checker);\n" +
		"	else             ParserGC = new %s($TPackage, Name, Checker);\n" +
		"	\n" +
		"	if((ParserCK == null) && (ParserGC == null)) throw new NullPointerException(\"Checker is not a valid RegParser. (\"+Name+\")\");\n" +
		"	\n"+
		"	%s TheVerifier = (Verifier == null)?null:new %s($TPackage, Name, Verifier);\n" +
		"	%s TheCompiler = (Compiler == null)?null:new %s($TPackage, Name, Compiler);\n" +
		"	\n" +
		"	if(ParserCK != null) {\n"+
		"		if((TheVerifier == null) && (TheCompiler == null))\n" +
		"			 return new %s(Name, ParserCK);\n" +
		"		else return new %s(Name, ParserCK, TheVerifier, TheCompiler);\n" +
		"	} else {\n"+
		"		if((TheVerifier == null) && (TheCompiler == null))\n" +
		"			 return new %s(Name, ParserGC);\n" +
		"		else return new %s(Name, ParserGC, TheVerifier, TheCompiler);\n" +
		"	}\n"+
		"\n" +
		"#end def_kind;",
		PTypePackage.class.getCanonicalName(),
		Scope.class.getCanonicalName(),
		PType.class.getCanonicalName(),
		
		RegParser   .class.getCanonicalName(),
		RPGetChecker.class.getCanonicalName(),
		
		ScriptManager.class.getCanonicalName(),
		RegParser    .class.getCanonicalName(),
		TPGetChecker .class.getCanonicalName(),
		
		TPVerifier.class.getCanonicalName(),
		TPVerifier.class.getCanonicalName(),
		
		TPCompiler.class.getCanonicalName(),
		TPCompiler.class.getCanonicalName(),
		
		PTSimple    .class.getCanonicalName(),
		PTComposable.class.getCanonicalName(),
		
		PTSimple    .class.getCanonicalName(),
		PTComposable.class.getCanonicalName()
	);
	
	/** Cache of the simple type kind */
	static private PTKind Kind_Simple = null;

	/** Definition of Simple parser kind */
	static public final String DefKindErrorStr = 
		"#def_kind Error:\n" +
		"\n"+
		"#Variables:\n"+
		"	var "+PropCheckerName+";\n" +
		"	var ErrMessage;\n"+
		"	var IsFatal;\n"+
		"\n"+
		"#Constructor:\n"+
		"	// @Java:\n"+
		"	import net.nawaman.regparser.*;\n"+
		"	import net.nawaman.regparser.types.*;\n"+
        "   import net.nawaman.regparser.typepackage.*;\n"+
		"	\n"+
		"	String Name    = (String)$Spec.getValue(\"Name\");\n"+
		"	String Checker = (String)$Spec.getValue(\""+PropCheckerName+"\");\n" +
		"	String ErrMsg  = (String)$Spec.getValue(\"ErrMessage\");\n" +
		"	String IsFatal = (String)$Spec.getValue(\"IsFatal\");\n" +
		"	\n" +
		"	if(Name    == null) throw new NullPointerException(\"Parser type name cannot be null.\");\n" +
		"	if(Checker == null) throw new NullPointerException(\"Checker of an error parser type cannot be null. (\"+Name+\")\");\n" +
		"	if(ErrMsg  == null) throw new NullPointerException(\"Error message of an error parser type cannot be null. (\"+Name+\")\");\n" +
		"	if(IsFatal != null) IsFatal = IsFatal.trim();\n" +
		"	return new PTError(Name, RegParser.newRegParser($TPackage, Checker), ErrMsg.trim(), \"true\".equals(IsFatal));\n" +
		"\n"+
		"#end def_kind;";
	
	/** Cache of the error type kind */
	static private PTKind Kind_Error = null;
	
	static void ensureKindAndSpecParserProvider() {
		if(KindAndSpecParserProvider == null) {
			KindAndSpecParserProvider = new PTypeProvider.Extensible(
				// Identifier
				new PTSimple("Identifier", RegParser.newRegParser("[a-zA-Z_][a-zA-Z_0-9]*")),
				// TypeName
				new PTSimple("TypeName", RegParser.newRegParser("[:$:]?[a-zA-Z_][a-zA-Z_0-9]*([:+:]|[:*:])?([:?:]|[:~:])?([:[:][:]:])?")),
				// Ignored
				new PTSimple("Ignored",
					RegParser.newRegParser(
						"([:WhiteSpace:] | [:/:][:/:][^[:NewLine:]]*[:NewLine:] | [:/:][:*:](^[:*:][:/:])*[:*:][:/:] )*")),
				// Ignored without NewLine
				new PTSimple("IgnoredNoNewLine",
					RegParser.newRegParser(
						"([:Blank:] | [:/:][:/:][^[:NewLine:]]*[:NewLine:] | [:/:][:*:](^[:*:][:/:])*[:*:][:/:] )*"))
			);
		}
	}

	// Data ------------------------------------------------------------------------------------------------------------

	/** Type provider for internal use */
	static PTypeProvider.Extensible KindAndSpecParserProvider;
	
	transient Scope.Simple MainScope = null;
	
	boolean IsFrozen = false;

	Vector<String>               ClassPaths = null;
	TreeMap<String, PTKind.Data> KDatas     = null;
	TreeMap<String, PTSpec>      TSpecs     = null;

	/** Native types - that does not be create from spec */
	TreeMap<String, PType> PNTypes = null;
	
	TreeMap<String, String>       ErrorMsgs = null;
	HashMap<  String, Serializable> MoreDatas = null;
	
	/** Type kinds */
	transient TreeMap<String, PTKind> RPTKinds = null;
	/** Types that are created from specs */
	transient TreeMap<String, PType>  RPTypes  = null;
	
	/** Set the main scope of this type package */
	public boolean setMainScope(Scope.Simple pScope) {
		if(this.IsFrozen && (this.MainScope != null)) return false;
		this.MainScope = Scope.Simple.getDuplicateOf(pScope);
		return true;
	}
	/** Set the main scope of this type package */
	public Scope.Simple getMainScope() {
		return this.MainScope;
	}
	
	/** Freeze the type package */
	public boolean freeze() {
		if(this.IsFrozen) return false;
		this.IsFrozen = true;
		return true;
	}
	
	// Class Path ------------------------------------------------------------------------------------------------------
	
	/** Returns an array of the current set of class paths */
	public String[] getClassPaths() {
		if(this.ClassPaths == null) return Signature.Simple.EmptyStringArray;
		return this.ClassPaths.toArray(Signature.Simple.EmptyStringArray);
	} 
	
	/** Add a required class-path */
	public boolean addClassPath(String pURLString) {
		if(this.IsFrozen)      return false;
		if(pURLString == null) return false;
		try {
			URL Url = new URL(pURLString);
			if(this.ClassPaths == null) this.ClassPaths = new Vector<String>();
			this.ClassPaths.add(Url.toString());
			return true;
		} catch(MalformedURLException E) {
			try {
				URL Url = new URL("file://" + pURLString);
				if(this.ClassPaths == null) this.ClassPaths = new Vector<String>();
				this.ClassPaths.add(Url.toString().trim());
				return true;
			} catch(MalformedURLException E2) {}
		}
		return false;
	}
	
	/** Add the no longer-required ClassPath */
	public boolean removeClassPath(String pURLString) {
		if(this.IsFrozen)           return false;
		if(pURLString      == null) return false;
		if(this.ClassPaths == null) return false;
		try {
			URL Url = new URL(pURLString);
			if(this.ClassPaths == null) this.ClassPaths = new Vector<String>();
			if(this.ClassPaths.contains(Url.toString())) {
				this.ClassPaths.remove(Url.toString());
				return true;
			}
		} catch(MalformedURLException E) {
			try {
				URL Url = new URL("file://" + pURLString);
				if(this.ClassPaths == null) this.ClassPaths = new Vector<String>();
				if(this.ClassPaths.contains(Url.toString())) {
					this.ClassPaths.remove(Url.toString());
					return true;
				}
			} catch(MalformedURLException E2) {}
		}
		return false;
	}
	
	// TypeKinds -------------------------------------------------------------------------------------------------------

	/** Add type from type spec */
	public boolean addKind(PTKind.Data pTKData) {
		if(this.IsFrozen) return false;
		return this.addKind(pTKData.getName(), pTKData.DataNames, pTKData.getTypeCode());
	}
	/** Add type from type spec */
	public boolean addKind(String pName, String[] pDataNames, String pTypeCode) {
		if(this.IsFrozen) return false;
		if(pName      == null) return false;
		if(pDataNames == null) return false;
		if(pTypeCode  == null) return false;
		if((this.RPTKinds != null) && (this.RPTKinds.containsKey(pName))) return false;
		if(this.KDatas   == null) this.KDatas   = new TreeMap<String, Data>();
		if(this.RPTKinds == null) this.RPTKinds = new TreeMap<String, PTKind>();
		
		PTKind TK = new PTKind(pName, pDataNames, pTypeCode);
		this.KDatas.put(pName, TK.Data);
		this.RPTKinds.put(pName, TK);
		return false;
	}
	
	/** Add the kind by its definition */
	public String addKind(String pDefStr) {
		
		PTKind TK = null;
		
		if     ((pDefStr == DefKindSimpleStr) && (Kind_Simple != null)) TK = Kind_Simple; 
		else if((pDefStr == DefKindErrorStr)  && (Kind_Error  != null)) TK = Kind_Error;
		else {
			// Parse the definition
			if(PTKind.PTKindParser == null) PTKind.PTKindParser = RegParser.newRegParser(PTKind.PTKindParserString);
			ParseResult PR = PTKind.PTKindParser.parse(pDefStr, KindAndSpecParserProvider);
			if(PR == null) throw new RuntimeException("Invalid Kind Definition: \n" + pDefStr);
	
			// Get the name and checks if it already exist
			String KindName = PR.textOf("$KindName");
			if(KindName == null) throw new RuntimeException("Invalid Kind Definition: Kind Name is not found: \n" + pDefStr);
			if(this.getTypeKind(KindName) != null)
				throw new RuntimeException("Invalid Kind Definition: The kind is already exist in this package: \n" + pDefStr);
			
			// Get more info
			String[] VarNames        = PR.textsOf("$VarName");
			String   ConstructorCode = PR.textOf("$ConstructorCode");
			
			// Add the data
			TK = new PTKind(KindName, VarNames, ConstructorCode);
		}
		if(this.KDatas   == null) this.KDatas   = new TreeMap<String, Data>();
		if(this.RPTKinds == null) this.RPTKinds = new TreeMap<String, PTKind>();
			
		this.KDatas.put(TK.getName(), TK.Data);
		this.RPTKinds.put(TK.getName(), TK);

		if(pDefStr == DefKindSimpleStr) { Kind_Simple = TK; return Kind_Simple.getName(); }
		if(pDefStr == DefKindErrorStr)  { Kind_Error  = TK; return Kind_Error.getName();  }
		
		return TK.getName();
	}
	
	/** Get type kind from type kind name */
	public PTKind getTypeKind(String pKName) {
		if(this.RPTKinds == null) return null;
		return this.RPTKinds.get(pKName);
	}
	/** Recreate the kind - Internal use only */
	boolean updateKind(String pName) {
		if((this.KDatas == null) || !this.KDatas.containsKey(pName)) return false;
		
		PTKind TK = this.RPTKinds.get(pName);
		TK = new PTKind(pName, TK.Data.DataNames, TK.Data.TypeCode);
		if(this.RPTKinds == null) this.RPTKinds = new TreeMap<String, PTKind>();
		this.RPTKinds.put(pName, TK);
		return true;
	}
	
	// Error Message ---------------------------------------------------------------------------------------------------

	public boolean addErrorMessage(String pErrName, String pErrMsg) {
		if(this.IsFrozen)    return false;
		if(pErrName == null) return false;
		if(pErrMsg  == null) return false;
		if((this.ErrorMsgs != null) && (this.ErrorMsgs.containsKey(pErrName))) return false;
		
		if(this.ErrorMsgs == null) this.ErrorMsgs = new TreeMap<String, String>();
		this.ErrorMsgs.put(pErrName, pErrMsg);
		return true;
	}
	protected boolean removeErrorMessage(String pErrName) {
		if(this.IsFrozen)                                                      return false;
		if(pErrName == null)                                                   return false;
		if((this.ErrorMsgs != null) && (this.ErrorMsgs.containsKey(pErrName))) return false;
		
		if(this.ErrorMsgs == null) this.ErrorMsgs = new TreeMap<String, String>();
		this.ErrorMsgs.remove(pErrName);
		return true;
	}
	
	// TypeProvider ----------------------------------------------------------------------------------------------------
	
	/** Returns type from name */
	public PType getType(String pName) {
		if(pName == null) return null;
		if(this.RPTypes != null) {
			PType PT = this.RPTypes.get(pName);
			if(PT != null) return PT;
		}
		if(this.PNTypes != null) {
			PType PT = this.PNTypes.get(pName);
			if(PT != null) return PT;
		}
		return null;
	}
	
	/** Returns the names of all types in this provider */
	public Set<String> getAllTypeNames() {
		if(this.RPTypes == null) return null;
		return this.RPTypes.keySet();
	}
	
	/** Remove a type */
	void removeType(String pTName) {
		if(this.RPTypes != null) this.RPTypes.remove(pTName);
		if(this.TSpecs  != null) this.TSpecs.remove(pTName);
	}
	
	/** Returns the names of all types in this provider */
	public Set<String> getAllErrorMessageNames() {
		if(this.ErrorMsgs == null) return null;
		return this.ErrorMsgs.keySet();
	}
	
	/** Get an error message  */
	public String getErrorMessage(String pErrName) {
		return (this.ErrorMsgs != null)?this.ErrorMsgs.get(pErrName):null;
	}
	
	// Types -----------------------------------------------------------------------------------------------------------

	/** Add type from type spec */
	public boolean addNativeType(PType pType) {
		if(this.IsFrozen) return false;
		if(pType == null) return false;
		
		// Checks in Type Spec
		if((this.TSpecs   != null) && this.TSpecs.containsKey(pType.name()))
			throw new RuntimeException("Add Type Error: The given type `"+pType.name()+"` is already exist in this package.");
		// Checks in Native type
		if((this.PNTypes != null) && this.PNTypes.containsKey(pType.name()))
			throw new RuntimeException("Add Type Error: The given type `"+pType.name()+"` is already exist in this package.");
		
		if(this.PNTypes == null) this.PNTypes = new TreeMap<String, PType>();
		this.PNTypes.put(pType.name(), pType);
		return true;
	}
	/** Add type from type spec */
	public boolean addType(PTSpec pSpec) {
		if(this.IsFrozen) return false;
		if(pSpec == null) return false;
		
		// Checks in Type Spec
		if((this.TSpecs  != null) && this.TSpecs.containsKey(pSpec.getName()))
			throw new RuntimeException("Add Type Error: The given type `"+pSpec.getName()+"` is already exist in this package.");
		// Checks in Native type
		if((this.PNTypes != null) && this.PNTypes.containsKey(pSpec.getName()))
			throw new RuntimeException("Add Type Error: The given type `"+pSpec.getName()+"` is already exist in this package.");
		
		// Add and update
		if(this.TSpecs == null) this.TSpecs = new TreeMap<String, PTSpec>();
		this.TSpecs.put(pSpec.getName(), pSpec);
		this.updateType(pSpec.getName());
		return true;
	}
	
	static final String    KindNameExtractorStr = "!Ignored!*[:#:]def!Ignored!+($KindName:~[:$:]?!Identifier![:~:]?([:[:][:]:])?~)!Ignored!+parser!Ignored!+";
	static       RegParser KindNameExtractor    = null;
	
	/** Add a type form the definition */
	public String addType(String pTypeDefStr) {

		// Extract the Kind name
		if(KindNameExtractor == null) KindNameExtractor = RegParser.newRegParser(KindNameExtractorStr);
		
		ParseResult KindNamePR = KindNameExtractor.parse(pTypeDefStr, KindAndSpecParserProvider);
		if(KindNamePR == null) throw new RuntimeException("Invalid Kind Definition: " + pTypeDefStr);
		String KindName = KindNamePR.textOf("$KindName");
		if(KindName == null) throw new RuntimeException("Invalid Type Definition: Kind Name is not found: \n" + pTypeDefStr);
		
		PTKind Kind = this.getTypeKind(KindName);
		if(Kind == null) throw new RuntimeException("Invalid Type Definition: Unknown Type Kind: \n" + pTypeDefStr);
		
		// Parse the definition
		ParseResult PR = Kind.getSpecParser().parse(pTypeDefStr, KindAndSpecParserProvider);
		if(PR == null) throw new RuntimeException("Invalid Type Definition: \n" + pTypeDefStr);

		// Get the name and checks if it already exist
		String TypeName = PR.textOf("$Name");
		if(TypeName == null) throw new RuntimeException("Invalid Type Definition: Type Name is not found: " + pTypeDefStr);
		if(this.getType(TypeName) != null)
			throw new RuntimeException("Invalid Type Definition: The type is already exist in this package: " + pTypeDefStr);
		
		PTSpec Spec = Kind.newSpec(TypeName);
		for(int i = 0; i < Kind.Data.getDataCount(); i++) {
			String PName  = Kind.Data.getDataNames(i);
			String PValue = PR.textOf("$" + PName);
			Spec.setValue(PName, (PValue == null)?null:PValue.trim());
		}
		
		// Add the data
		if(!this.addType(Spec)) return null;
		return TypeName;
	}
	
	/** Recreate the type - Internal use only */
	boolean updateType(String pName) {
		if(pName       == null)             return false;    
		if(this.TSpecs == null)             return false;
		if(!this.TSpecs.containsKey(pName)) return false;
		PTKind K = this.getTypeKind(this.TSpecs.get(pName).getKind());
		if(K == null) return false;
		if(this.RPTypes == null) this.RPTypes = new TreeMap<String, PType>();
		PType PT = K.newPTypeFromSpec(this, this.TSpecs.get(pName));
		PTypeProvider.Simple.exclusivelyInclude(this, PT);
		this.RPTypes.put(pName, PT);
		return true;
	}
	
	/** Returns s spec of the type (the duplicated one) */
	public PTSpec getTypeSpec(String pName) {
		PTSpec S = this.getTheTypeSpec(pName);
		if(S == null) return null;
		PTKind TK = this.getTypeKind(S.getKind());
		PTSpec NewS = TK.newSpec(pName);
		Scope.Simple.duplicate(S, NewS);
		return NewS;
	}
	
	/** Returns s spec of the type (the actual one) */
	PTSpec getTheTypeSpec(String pName) {
		if(pName       == null) return null;    
		if(this.TSpecs == null) return null;
		return this.TSpecs.get(pName);
	}
	
	// ToString of Type (It have to be here becuase TypePackage is the only place we can get the spec of a type) -------

	/** Returns the string representation of a spec type */
	public String getTypeToString(String pName) {
		if(pName == null) return null;
		return this.getTypeToString(this.getType(pName));
	}

	/** Returns the string representation of a spec type */
	public String getTypeToString(PType pType) {
		if(pType == null) return null;
		PTSpec Spec = this.getTheTypeSpec(pType.name());
		if(Spec == null) return pType.toString();
		
		StringBuffer ToString = new StringBuffer();
		
		// Prefix and name
		ToString.append(String.format(PTKind.TypeToStringPrefix, Spec.getKind(), Spec.getName()));
		
		// Each properties
		String[] PNames = Spec.getVariableNames().toArray(new String[0]);
		if(PNames != null) {
			for(int i = PNames.length; --i >= 0; ) {
				String PName = PNames[i];
				if(PName == null) continue;
				if(PName.equals(PTSpec.FN_Name) || PName.equals(PTSpec.FN_Kind)) continue;
				String Value = (String)Spec.getValue(PName);
				if(Value == null) continue; 
				ToString.append("\n#").append(PName).append(":").append("\n");
				ToString.append("\t").append(Value).append("\n");
			}
		}
		
		// Suffix
		ToString.append(PTKind.StringToStringSuffix);
		return ToString.toString();
	}
	
	// Scope -----------------------------------------------------------------------------------------------------------

	/** Returns a variable and constant names */
	public Set<String> getVariableNames() {
		if(this.MainScope == null) return Scope.Empty.getEmptyNames();
		return this.MainScope.getVariableNames();
	}
	
	/** Returns the variable count */
	public int getVarCount() {
		if(this.MainScope == null) return 0;
		return this.MainScope.getVarCount();
	}
	
	/** Returns the variable value */
	public Object getValue(String pName) {
		if(this.MainScope == null) return null;
		return this.MainScope.getValue(pName);
	}
	
	/** Change the variable value and return if success */
	public Object setValue(String pName, Object pValue) {
		if(this.MainScope == null) throw new NullPointerException();
		return this.MainScope.setValue(pName, pValue);
	}
	
	/** Create a new variable and return if success */
	public boolean newVariable(String pName, Class<?> pType, Object pValue) {
		if(this.IsFrozen)          return false;
		if(this.MainScope == null) return false;
		return this.MainScope.newVariable(pName, pType, pValue);
	}
	
	/** Create a new constant and return if success */
	public boolean newConstant(String pName, Class<?> pType, Object pValue) {
		if(this.IsFrozen)          return false;
		if(this.MainScope == null) return false;
		return this.MainScope.newVariable(pName, pType, pValue);
	}
	
	/** Removes a variable or a constant and return if success */
	public boolean removeVariable(String pName) {
		if(this.IsFrozen)          return false;
		if(this.MainScope == null) return false;
		return this.MainScope.removeVariable(pName);
	}
	
	/** Returns the variable value */
	public Class<?> getTypeOf(String pName) {
		if(this.MainScope == null) return null;
		return this.MainScope.getTypeOf(pName);
	}
	
	/** Checks if the variable of the given name is writable */
	public boolean isExist(String pName) {
		if(this.MainScope == null) return false;
		return this.MainScope.isExist(pName);
	}
	
	/** Checks if the variable of the given name is writable */
	public boolean isWritable(String pName) {
		if(this.MainScope == null) return false;
		return this.MainScope.isWritable(pName);
	}
	
	/** Checks if this scope support constant declaration */
	public boolean isConstantSupport() {
		if(this.MainScope == null) return false;
		return this.MainScope.isConstantSupport();
	}

    /** Returns the <code>Writer</code> for scripts to use when displaying output. */
    public Writer getWriter() {
		if(this.MainScope == null) return Scope.Simple.DOut;
		return this.MainScope.getWriter();
	}
    
    /** Returns the <code>Writer</code> used to display error output. */
    public Writer getErrorWriter() {
		if(this.MainScope == null) return Scope.Simple.DErr;
		return this.MainScope.getErrorWriter();
	}
    
    /** Sets the <code>Writer</code> for scripts to use when displaying output. */
    public void setWriter(Writer writer) {
		if(this.MainScope == null) return;
		this.MainScope.setWriter(writer);
	}
    
    /** Sets the <code>Writer</code> used to display error output. */
    public void setErrorWriter(Writer writer) {
		if(this.MainScope == null) return;
		this.MainScope.setErrorWriter(writer);
	}
    
    /** Returns a <code>Reader</code> to be used by the script to read input. */
    public Reader getReader() {
		if(this.MainScope == null) return Scope.Simple.DIn;
		return this.MainScope.getReader();
	}
    
    /** Sets the <code>Reader</code> for scripts to read input */
    public void setReader(Reader reader) {
		if(this.MainScope == null) return;
		this.MainScope.setReader(reader);
	}
    
    // More Data -------------------------------------------------------------------------------------------------------

    /** Add a data */
    void addData(String pName, Serializable pValue) {
		if(this.IsFrozen) return;
    	if(this.MoreDatas == null) this.MoreDatas = new HashMap<String, Serializable>();
    	this.MoreDatas.put(pName, pValue);
    }

    /** Get remove a data */
    void removeData(String pName) {
		if(this.IsFrozen)          return;
    	if(this.MoreDatas == null) return;
    	this.MoreDatas.remove(pName);
    }
    
    /** Get a data */
    public Serializable getData(String pName) {
    	if(this.MoreDatas == null) return null;
    	return this.MoreDatas.get(pName);
    }
	
	// Serializable ----------------------------------------------------------------------------------------------------
	
	byte[] BufferToSave = null;
	
	/** Custom deserialization is needed. */
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
		// Save the rest
		aStream.defaultReadObject();
		
		JavaCompiler JC = JavaCompiler.Instance;
		if(aStream instanceof JavaCompilerObjectInputStream)
			JC = ((JavaCompilerObjectInputStream)aStream).getJavaCompiler();

		// Recreate ClassPaths - There was a bug that will prevent TreeMap from being read property, so we rebuilt them
		if(this.ClassPaths != null) {
			Vector<String> TempClassPaths = this.ClassPaths;
			this.ClassPaths = new Vector<String>();
			for(int i = 0; i < TempClassPaths.size(); i++) {
				String ClassPath = TempClassPaths.get(i);
				if(this.ClassPaths.add(ClassPath)) {
					JC.addClasspathURL(ClassPath);
				}
			}
		}

		// Recreate KDatas - There was a bug that will prevent TreeMap from being read propery, so we rebuilt them
		if(this.KDatas != null) {
			TreeMap<String, Data> TempKDatas = this.KDatas;
			this.KDatas = new TreeMap<String, Data>();
			for(String Name : TempKDatas.keySet()) this.KDatas.put(Name, TempKDatas.get(Name));
		}

		// Recreate TSpecs - There was a bug that will prevent TreeMap from being read propery, so we rebuilt them
		if(this.TSpecs != null) {
			TreeMap<String, PTSpec> TempTSpecs = this.TSpecs;
			this.TSpecs = new TreeMap<String, PTSpec>();
			for(String Name : TempTSpecs.keySet()) this.TSpecs.put(Name, TempTSpecs.get(Name));
		}

		// Recreate ErrorMsgs - There was a bug that will prevent TreeMap from being read propery, so we rebuilt them
		if(this.ErrorMsgs != null) {
			TreeMap<String, String> TempErrorMsgs = this.ErrorMsgs;
			this.ErrorMsgs = new TreeMap<String, String>();
			for(String Name : TempErrorMsgs.keySet()) this.ErrorMsgs.put(Name, TempErrorMsgs.get(Name));
		}

		// Recreate MoreDatas - There was a bug that will prevent TreeMap from being read propery, so we rebuilt them
		if(this.MoreDatas != null) {
			HashMap<String, Serializable> TempMoreDatas = this.MoreDatas;
			this.MoreDatas = new HashMap<String, Serializable>();
			for(String Name : TempMoreDatas.keySet()) this.MoreDatas.put(Name, TempMoreDatas.get(Name));
		}
		
		// Load native type
		try {
			TreeMap<String, PType> Types = (TreeMap<String, PType>)aStream.readObject();
			if(Types != null) {
				this.PNTypes = new TreeMap<String, PType>();
				for(String Name : Types.keySet()) this.PNTypes.put(Name, Types.get(Name));
			}
		} catch(Exception E) {
			System.out.println("Fail to load: this.RNTypes: " + E);
		}
		
		// Load spec type
		try {
			TreeMap<String, PType> Types = (TreeMap<String, PType>)aStream.readObject();
			if(Types != null) {
				this.RPTypes = new TreeMap<String, PType>();
				for(String Name : Types.keySet()) {
					this.RPTypes.put(Name, Types.get(Name));
					PTypeProvider.Simple.exclusivelyInclude(this, Types.get(Name));
				}
			}
		} catch(Exception E) {
			System.out.println("Fail to load: this.RPTypes: " + E);
		}
		
		// Load type kinds
		try{
			TreeMap<String, PTKind> Kinds = (TreeMap<String, PTKind>)aStream.readObject();
			if(Kinds != null) {
				this.RPTKinds = new TreeMap<String, PTKind>();
				for(String Name : Kinds.keySet()) this.RPTKinds.put(Name, Kinds.get(Name));
			}
		} catch(Exception E) {
			System.out.println("Fail to load: this.RPTKind: " + E);
		}
		
		// If the load fail, recreate it
		if((this.RPTKinds == null) && (this.KDatas != null)) {
			for(String N : this.KDatas.keySet())
				this.updateKind(N);
		}
		// If the load fail, recreate it
		if((this.RPTypes  == null) && (this.TSpecs != null)) {
			for(String N : this.TSpecs.keySet())
				this.updateType(N);
		}
	}

	/** Custom serialization is needed. */
	private void writeObject(ObjectOutputStream aStream) throws IOException {
		// Save the rest
		aStream.defaultWriteObject();
		
		try { aStream.writeObject(this.PNTypes);  } catch(Exception E) { System.out.println("Fail to save: this.PNTypes: "  + E); }
		try { aStream.writeObject(this.RPTypes);  } catch(Exception E) { System.out.println("Fail to save: this.RPTypes: "  + E); }
		try { aStream.writeObject(this.RPTKinds); } catch(Exception E) { System.out.println("Fail to save: this.RPTKinds: " + E); }
	}
	
	// Load ---------------------------------------------------------------------------------------

	/** Load a type package from a file named FN */
	static public PTypePackage loadAsSerializableFromFile(String FN) throws IOException {
		return loadAsSerializableFromFile(new File(FN));
	}

	/** Load a type package from a file F */
	static public PTypePackage loadAsSerializableFromFile(File F) throws IOException {
		FileInputStream FIS = null;
		try {
			FIS = new FileInputStream(F);
			return loadAsSerializableFromStream(FIS);
		} catch(IllegalArgumentException E) {
			throw new IllegalArgumentException("The file `"+F+"` does not contains the type-package object.");
		} catch(RuntimeException E) {
			throw E;
		} finally {
			if(FIS != null) FIS.close();
		}
	}

	/** Load a type package as text from a stream IS */
	static public PTypePackage loadAsSerializableFromStream(InputStream IS) throws IOException {
		ObjectInputStream OIS = (IS instanceof ObjectInputStream)?(ObjectInputStream)IS:new ObjectInputStream(IS);
		Object O = null;
		try {
			O = OIS.readObject();
		} catch(Exception E) {
			System.out.println(E.toString());
			E.printStackTrace(System.out);
			if(E instanceof IOException) throw (IOException)E;
			
			if(E.getCause() != null) {
				Throwable T = E.getCause();
				System.out.println(T.toString());
				T.printStackTrace(System.out);
			}
			
			throw new RuntimeException("An error occurs while trying to load a parser-type package.", E);
		}
		
		if(O instanceof PTypePackage) return (PTypePackage)O;
		throw new IllegalArgumentException("The input stream does not contains the type-package object.");
	}
	
	// Save ---------------------------------------------------------------------------------------

	/** Load a type package from a file named FN */
	public void saveToFile(String FN) throws IOException {
		saveToFile(new File(FN));
	}

	/** Load a type package from a file F */
	public void saveToFile(File F) throws IOException {
		FileOutputStream FOS = null;
		try {
			FOS = new FileOutputStream(F);
			saveToStream(FOS);
		} finally {
			if(FOS != null) FOS.close();
		}
	}

	/** Load a type package as text from a stream IS */
	public void saveToStream(OutputStream OS) throws IOException {
		ObjectOutputStream OOS = (OS instanceof ObjectOutputStream)?(ObjectOutputStream)OS:new ObjectOutputStream(OS);
		try {
			OOS.writeObject(this);
		} catch(Exception E) {
			if(E instanceof IOException) throw (IOException)E;
			throw new RuntimeException("An error occurs while trying to load a parser-type package.", E);
		}
	}
	
	// Load/Save as Text -----------------------------------------------------------------------------------------------
	
	static final String DefsExtractorStr =
		"(" +
		"	!Ignored!*" +
		"	($Def:~[:#:]def([:_:](classpaths|kind|error|data)|!Ignored!+!Identifier!!Ignored!*)" +
		"		(^[:NewLine:][:#:]end!Ignored!+def([:_:](classpaths|kind|error|data)[:;:]|!Ignored!+parser[:;:]|[:Blank:]native[:;:]))*" +
		"		[:NewLine:][:#:]end!Ignored!+def([:_:](classpaths|kind|error|data)[:;:]|!Ignored!+parser[:;:]|[:Blank:]native[:;:])" +
		"	~)" +
		"	!Ignored!*"+
		")*";
	static private RegParser DefsExtractor = null;
	static RegParser getDefsExtractor() {
		if(DefsExtractor == null) DefsExtractor = RegParser.newRegParser(DefsExtractorStr);
		return DefsExtractor;
	}
	
	static final String ClassPathExtractorStr = 
		"[:#:]def_classpaths[:::]" +
		"	(!Ignored!*[:#:]ClassPath[:::]($ClassPath:~[^[:NewLine:]]*[:NewLine:]~)!Ignored!*)*"+
		"[:#:]end[:Blank:]def_classpaths[:;:]";
	static private RegParser ClassPathExtractor = null;
	
	static final String NativeExtractorStr = 
		"[:#:]def[:Blank:]Native[:Blank:]parsers[:::]" +
		"	(^[:NewLine:][:#:]Bytes[:::])*" +
		"	[:NewLine:][:#:]Bytes[:::]" +
		"	(!Ignored!*($Number:~[0-9A-F]+~:~($Number:~[0-9A-F]{2}~)*~))*"+
		"	!Ignored!*" +
		"[:#:]end[:Blank:]def[:Blank:]native[:;:]";
	static private RegParser NativeTypeExtractor = null;
	
	static final String ErrorMessageExtractorStr = 
		"[:#:]def_errors[:::]"+
		"	($Context:~(^[:#:]end[:Blank:]def_error[:;:])*~:~"+
		"		(!Ignored!*[:#:]Error[:::]($Error:~(^!Ignored!*[:#:]Error[:::])*~)!Ignored!*)*"+
		"	~)"+
		"[:#:]end[:Blank:]def_error[:;:]";
	static private RegParser ErrorMessageExtractor = null;
	
	static final String DataExtractorStr = 
		"[:#:]def_data[:::]" +
		"	(^[:NewLine:][:#:]Bytes[:::])*" +
		"	[:NewLine:][:#:]Bytes[:::]" +
		"	(!Ignored!*($Number:~[0-9A-F]+~:~($Number:~[0-9A-F]{2}~)*~))*"+
		"	!Ignored!*" +
		"[:#:]end[:Blank:]def_data[:;:]";
	static private RegParser DataExtractor = null;

	/** Returns the PTypePackage form the object loaded from the Usepaths */
	static PTypePackage GetPTypePackageFromUsable(Object O) {
		Signature Signature = null;
		if((O instanceof Function) && ((Signature = ((Function)O).getSignature()) != null) &&
		   (Signature.getParamCount() == 0) && !Signature.isVarArgs() &&
		   (PTypePackage.class.isAssignableFrom(Signature.getReturnType()))) {
			
			Function     Function = (Function)O;
			PTypePackage TPackage = (PTypePackage)Function.run();
			if(TPackage != null) return TPackage;
		}
		
		throw new IllegalArgumentException("The input stream does not contains the type package definition.");
	}

	/** Load a type package as text from a file named FN */
	static public PTypePackage loadAsTextFromFile(String FN) throws IOException {
		PTypePackage PTP = newPackageFromDefs(Util.loadTextFile(FN));
		if(PTP == null) throw new IllegalArgumentException("The input stream does not contains the type package definition.");
		return PTP;
	}

	/** Load a type package as text from a file F */
	static public PTypePackage loadAsTextFromFile(File F) throws IOException, ClassNotFoundException {
		Object O = Tools.Use(F);
		return GetPTypePackageFromUsable(O);
	}

	/** Load a type package as text from a stream IS */
	static public PTypePackage loadAsTextFromStream(InputStream IS) throws IOException, ClassNotFoundException {
		String Def = Util.loadTextFromStream(IS);
		
		if(Def != null) {
			ExtractResult ExRe = Tools.ExtractExecutableFromCompiledText("", Def);

			Object    O         = null;
			Signature Signature = null;
			if((ExRe != null) && ((Signature = ((Function)(O = ExRe.Executable)).getSignature()) != null) &&
			   (Signature.getParamCount() == 0) && !Signature.isVarArgs() &&
			   (PTypePackage.class.isAssignableFrom(Signature.getReturnType()))) {
						
				Function     Function = (Function)O;
				PTypePackage TPackage = (PTypePackage)Function.run();
				if(TPackage != null) return TPackage;
			}
		}
		
		throw new IllegalArgumentException("The input stream does not contains the type package definition.");
	}
	
	/** Creates a new package from definitions string */
	static public PTypePackage newPackageFromDefs(String pDefs) {
		if(pDefs == null) return null;
		// Common Type for parsing type kind and spec
		ensureKindAndSpecParserProvider();
		
		PTypePackage PTP = new PTypePackage();
		PTP.addAllFromDefs(pDefs);
		return PTP;
	}
	
	@SuppressWarnings("unchecked")
	public void addAllFromDefs(String pDefs) {
		if(pDefs == null)
			throw new IllegalArgumentException("Invalid Definitions:\n" + pDefs);
		
		int IgnorePosition = ScriptManager.GetEndOfIgnored(pDefs);
		if(IgnorePosition == -1) IgnorePosition = 0;
		
		int EndOfFirstLine = pDefs.indexOf('\n', IgnorePosition);
		if(EndOfFirstLine == -1) EndOfFirstLine = IgnorePosition;
		
		String FirstLine = pDefs.substring(IgnorePosition, EndOfFirstLine);

		// Later the parameter will be the version number
		String[] EAndP = ScriptManager.GetEngineNameAndParamFromCode(FirstLine);
		if((EAndP == null) || (EAndP.length < 2) || !TPackageScriptEngine.ShortName.equals(EAndP[0]) || (EAndP[1] != null))
			throw new IllegalArgumentException("Invalid Definitions:\n" + pDefs);
		
		//pDefs = pDefs.substring(EndOfFirstLine);
		
		RegParser   RP = getDefsExtractor();
		ParseResult PR = RP.parse(pDefs, KindAndSpecParserProvider);
		if(PR == null) throw new IllegalArgumentException("Invalid Definitions:\n" + pDefs);
	
		String[] Defs = PR.textsOf("$Def");
		
		if(Defs != null) {

			// Extract all native types
			if(ClassPathExtractor == null) ClassPathExtractor = RegParser.newRegParser(ClassPathExtractorStr);
			
			// Extract all ClassPaths
			for(String Def : Defs) {
				if((Def == null) || (Def.length() == 0)) continue;
				if(!Def.startsWith("#def_classpaths"))   continue;
				
				ParseResult PRCP = ClassPathExtractor.match(Def, KindAndSpecParserProvider); 
				if(PRCP == null) throw new RuntimeException("Invalid Definitions: Unable to deserialize classpath: \n" + Def);
	
				String[] TheClassPaths = PRCP.textsOf("$ClassPath");
				if(TheClassPaths != null) {
					for(int i = 0; i < TheClassPaths.length; i++) {
						String ClassPath = TheClassPaths[i];
						if(this.ClassPaths == null) this.ClassPaths = new Vector<String>();
						ClassPath = ClassPath.trim();
						
						if(ClassPath.startsWith("file:/") && !ClassPath.startsWith("file://"))
							ClassPath = ClassPath.substring("file:/".length() - 1);
						if(ClassPath.startsWith("file://")) ClassPath = ClassPath.substring("file://".length());
						
						if(this.ClassPaths.add(ClassPath)) {
							if(ClassPath.endsWith(".jar")) JavaCompiler.Instance.addJarFile(ClassPath); 
							else                           JavaCompiler.Instance.addClasspathURL(ClassPath);
						}
					}
				}
			}
			
			// Extract all kinds
			for(String Def : Defs) {
				if((Def == null) || (Def.length() == 0)) continue;
				if(!Def.startsWith("#def_")) continue;
				
				if(Def.startsWith("#def_kind")) {
					this.addKind(Def);
				} else if(Def.startsWith("#def_data")) {
					
				}
			}
			
			// Extract all native types
			if(NativeTypeExtractor == null) NativeTypeExtractor = RegParser.newRegParser(NativeExtractorStr);
			// Add Native type
			for(String Def : Defs) {
				if((Def == null) || (Def.length() == 0)) continue;
				if(!Def.endsWith("#end def native;"))    continue;
				
				ParseResult PRNType = NativeTypeExtractor.match(Def, KindAndSpecParserProvider); 
				if(PRNType == null) throw new RuntimeException("Invalid Definitions: Unable to deserialize native types: \n" + Def);
	
				String[] Datas = PRNType.textsOf("$Number");
				if((Datas == null) || (Datas.length == 0))
					throw new RuntimeException("Invalid Definitions: Unable to deserialize native types: Empty datas: \n" + Def);
				
				byte[] Bytes = new byte[Datas.length];
				for(int i = 0; i < Datas.length; i++) Bytes[i] = (byte)Integer.parseInt(Datas[i], 16);
				
				try {
					ByteArrayInputStream BAIS = new ByteArrayInputStream(Bytes);
					Serializable[] S = Util.loadObjectsFromStream(BAIS);
					for(int i = S.length; --i >= 0; ) this.addNativeType((PType)S[i]);
				} catch(Exception E) {
					throw new RuntimeException("Invalid Definitions: Unable to deserialize native types: \n" + Def, E);
				}
			}
			
			// Extract all types
			for(String Def : Defs) {
				if((Def == null) || (Def.length() == 0)) continue;
				if(!Def.endsWith("parser;")) continue;
				this.addType(Def);
			}

			// Extract all error messages
			if(ErrorMessageExtractor == null) ErrorMessageExtractor = RegParser.newRegParser(ErrorMessageExtractorStr);
			// Extract all errors
			for(String Def : Defs) {
				if((Def == null) || (Def.length() == 0)) continue;
				if(!Def.startsWith("#def_errors"))   continue;
				
				ParseResult PRErr = ErrorMessageExtractor.match(Def, KindAndSpecParserProvider); 
				if(PRErr == null) throw new RuntimeException("Invalid Definitions: Unable to deserialize error message: \n" + Def);
	
				String[] TheErrors = PRErr.textsOf("$Error");
				if(TheErrors != null) {
					for(int i = 0; i < TheErrors.length; i++) {
						String ErrName = TheErrors[i];
						int I = ErrName.indexOf(" - ");
						String ErrMsg = null;
						if(I != -1) {
							ErrMsg  = ErrName.substring(I + 3).trim();
							ErrName = ErrName.substring(0, I).trim();
						}
						this.addErrorMessage(ErrName, ErrMsg);
					}
				}
			}

			// Extract all native types
			if(DataExtractor == null) DataExtractor = RegParser.newRegParser(DataExtractorStr);
			// Extract all data
			for(String Def : Defs) {
				if((Def == null) || (Def.length() == 0)) continue;
				if(!Def.startsWith("#def_data"))         continue;
				
				ParseResult PRData = DataExtractor.match(Def, KindAndSpecParserProvider); 
				if(PRData == null) throw new RuntimeException("Invalid Definitions: Unable to deserialize extra datas: \n" + Def);
	
				String[] Datas = PRData.textsOf("$Number");
				if((Datas == null) || (Datas.length == 0))
					throw new RuntimeException("Invalid Definitions: Unable to deserialize extra datas: Empty datas: \n" + Def);
				
				byte[] Bytes = new byte[Datas.length];
				for(int i = 0; i < Datas.length; i++) Bytes[i] = (byte)Integer.parseInt(Datas[i], 16);
				
				try {
					ByteArrayInputStream BAIS = new ByteArrayInputStream(Bytes);
					Serializable[] S = Util.loadObjectsFromStream(BAIS);
					if((S.length == 1) && ((S[0] == null)||(S[0] instanceof HashMap))) {
						if(S[0] != null) {
							if(this.MoreDatas == null) this.MoreDatas = new HashMap<String, Serializable>();
							HashMap<String, Serializable> TheDatas = (HashMap<String, Serializable>)S[0];
							for(String N : TheDatas.keySet()) this.MoreDatas.put(N, TheDatas.get(N));
						}
					} else throw new RuntimeException("Invalid Definitions: Unable to deserialize extra datas: Wrong data type: \n" + Def);
				} catch(Exception E) {
					throw new RuntimeException("Invalid Definitions: Unable to deserialize extra datas: \n" + Def, E);
				}
			}
		}
	}
	
	// Save as Text --------------------------------------------------------------------------------
	
	/** Save this type package as text to a file named FN */
	public void saveAsTextToFile(String FN) throws IOException {
		this.saveAsTextToFile(new File(FN));
	}

	/** Save this type package as text to a file F */
	public void saveAsTextToFile(File F) throws IOException {
		FileOutputStream FOS = null;
		try {
			FOS = new FileOutputStream(F);
			this.saveAsTextToStream(F.getName(), FOS);
		} finally {
			if(FOS != null) FOS.close();
		}
	}

	/** Save this type package as text to a stream OS */
	public void saveAsTextToStream(String pName, OutputStream OS) throws IOException {
		try { TPackageScriptEngine.newInstance(null).saveTypeDefAsText(pName, this.getDefsAsSrting(), this, OS); }
		catch (ClassNotFoundException  e) {
			// Should not happend since all class are already loaded.
		}
	}
	
	String ToString(Object O) {
		if(O == null) return "null";
		if(O instanceof String)
			return "STRING START ------------------------------------------------------------\n"+
				   ((String)O)+
				 "\n-------------------------------------------------------------------------\n";
		if(O instanceof Character) return "'"+Util.escapeText((String)O)+"'";
		if(O instanceof Number) {
			if(O instanceof Integer) return "(int)"    + O.toString();
			if(O instanceof Double)  return "(double)" + O.toString();
			if(O instanceof Byte)    return "(byte)"   + O.toString();
			if(O instanceof Short)   return "(short)"  + O.toString();
			if(O instanceof Long)    return "(long)"   + O.toString();
			if(O instanceof Float)   return "(float)"  + O.toString();
		}
		if(O.getClass().isArray()) {
			StringBuffer SB = new StringBuffer();
			String CName = O.getClass().getComponentType().getCanonicalName();
			if(CName.startsWith("java.lang.") && (CName.substring("java.lang.".length()).indexOf(".") == -1))
				CName = CName.substring("java.lang.".length());
			SB.append(CName).append("[] {");
			for(int i = 0; i < java.lang.reflect.Array.getLength(O); i++) {
				if(i != 0) SB.append(",");
				String EToString = ToString(java.lang.reflect.Array.get(O, i));
				if(EToString.contains("\n")) {
					String[] Lines = EToString.split("\n");
					StringBuffer LinesSB = new StringBuffer();
					for(int l = 0; l < Lines.length; l++) {
						if(l != 0) LinesSB.append("\n\t");
						LinesSB.append(Lines[l]);
					}
					EToString = LinesSB.toString();
				}
				SB.append("\n\t").append(EToString);
			}
			SB.append("\n}");
			return SB.toString();
		}
		return O.toString() + ":" + O.getClass().getCanonicalName();
	}
	
	// There will later version number here
	/** The TypeDefinition prefix for the text format */
	static public String TextFilePrefix = "// @" + TPackageScriptEngine.ShortName + ": " + TPackageScriptEngine.SIGNATURE_SPEC;
	
	/** Returns all definition as a string so that it can be used in other means */
	@SuppressWarnings("unchecked")
	public String getDefsAsSrting() {
		StringBuffer DefToString = new StringBuffer();
		String Name = (this.MainScope != null)?(String)this.MainScope.getValue("Name"):null;
		if(Name != null)
			 DefToString.append(TextFilePrefix + "\n\n// TypePackage ("+Name+") **************************************************************************");
		else DefToString.append(TextFilePrefix + "\n\n// TypePackage *************************************************************************************");
		
		// ClassPaths
		if((this.ClassPaths != null) && (this.ClassPaths.size() != 0)) {
			
			DefToString.append("\n\n// ClassPaths *****************************************************************************\n\n");
			DefToString.append("#def_classpaths:\n");
			for(int i = 0; i < this.ClassPaths.size(); i++)
				DefToString.append("#ClassPath: ").append(this.ClassPaths.get(i)).append("\n");
			DefToString.append("#end def_classpaths;\n");
		}
		
		// Type kinds
		if(this.RPTKinds != null) {
			
			DefToString.append("\n\n// TypeKinds *****************************************************************************\n\n");
			
			Vector<String> KNames = new Vector<String>(this.RPTKinds.keySet());
			Collections.sort(KNames, UObject.getTheComparator());
			for(String KName : KNames) {
				
				String Title = "// " + KName + " ";
				while(Title.length() < 80) Title += "+";
				
				DefToString.append(Title).append("\n");
				DefToString.append(this.RPTKinds.get(KName).toString());
				DefToString.append("\n\n");
			}
		}
		
		try {
			// Native types
			if(this.PNTypes != null) {
				StringBuffer SB = new StringBuffer();
				Vector<Serializable> NTypes = new Vector<Serializable>();
				
				Vector<String> NTNames = new Vector<String>(this.PNTypes.keySet());
				Collections.sort(NTNames, UObject.getTheComparator());
				for(String NTName : NTNames) {
					SB.append("\ttype: ").append(NTName).append(";\n");
					NTypes.add(this.PNTypes.get(NTName));
				}
				
				ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
				Util.saveObjectsToStream(BAOS, NTypes.toArray(new Serializable[0]));

				byte[] Bytes = BAOS.toByteArray();
				StringBuffer NTDatas = new StringBuffer();
				for(int i = 0; i < Bytes.length; i++) {
					if((i % 40) == 0) NTDatas.append("\n\t");
					int B = Bytes[i];
					if(B < 0) B = (0xFF & B);
					String H = String.format("%X", B);
					if(H.length() < 2) H = "0" + H;
					NTDatas.append(H);
				}
				NTDatas.append("\n");/* There will later version number here */

				DefToString.append("\n\n// Native Types **************************************************************************\n\n");
				DefToString.append( 
					"#def Native parsers:\n" +
					"\n"+
					"#Names:\n" +
					SB.toString() +
					"\n"+
					"#Bytes:"+
					NTDatas.toString() + 
					"\n"+
					"#end def native;");
			}
		} catch(Exception E) {
			throw new RuntimeException("Unable to export the type package: There is an exception thrown while trying to serialize native types.", E);
		}
		
		// Types with spec
		if(this.TSpecs != null) {
			
			DefToString.append("\n\n// Types *********************************************************************************\n\n");

			Vector<String> TTSpecs = new Vector<String>(this.TSpecs.keySet());
			Collections.sort(TTSpecs, UObject.getTheComparator());
			for(String SName : TTSpecs) {
				
				String Title = "// " + SName + " ";
				while(Title.length() < 80) Title += "-";
				
				DefToString.append(Title).append("\n");
				DefToString.append(this.getTypeToString(SName));
				DefToString.append("\n\n");
			}
		}
			
		// Error Messages
		if((this.ErrorMsgs != null) && (this.ErrorMsgs.size() != 0)) {
			
			DefToString.append("\n\n// ErrorMsgs *****************************************************************************\n\n");
			DefToString.append("#def_errors:\n");
			
			Vector<String> ErrorTMsgs = new Vector<String>(this.ErrorMsgs.keySet());
			Collections.sort(ErrorTMsgs, UObject.getTheComparator());
			for(String N : ErrorTMsgs) {
				String EMsg = this.ErrorMsgs.get(N);
				DefToString.append("#Error: ").append(N).append(" - ").append(EMsg).append("\n");
			}
			DefToString.append("#end def_error;\n");
		}
		
		try {
			// MoreDatas
			if(this.MoreDatas != null) {
				StringBuffer SB = new StringBuffer();

				Vector<String> TMoreDatas = new Vector<String>(this.MoreDatas.keySet());
				Collections.sort(TMoreDatas, UObject.getTheComparator());
				for(String NDataName : TMoreDatas) {
					Serializable D = this.MoreDatas.get(NDataName);
					String DToString = ToString(D);
					
					if(DToString.indexOf('\n') != -1) {
						String[] Lines = DToString.split("\n");
						StringBuffer LinesSB = new StringBuffer();
						for(int i = 0; i < Lines.length; i++) LinesSB.append("\n\t").append(Lines[i]);
						DToString = LinesSB.toString();
					} else {
						DToString = " = " + DToString + ";";
					}
					
					SB.append("\tdata: ").append(NDataName).append(DToString).append("\n\n");
				}
				
				ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
				Util.saveObjectsToStream(BAOS, new Serializable[] { this.MoreDatas } );

				byte[] Bytes = BAOS.toByteArray();
				StringBuffer DataBytes = new StringBuffer();
				for(int i = 0; i < Bytes.length; i++) {
					if((i % 40) == 0) DataBytes.append("\n\t");
					int B = Bytes[i];
					if(B < 0) B = (0xFF & B);
					String H = String.format("%X", B);
					if(H.length() < 2) H = "0" + H;
					DataBytes.append(H);
				}
				DataBytes.append("\n");

				DefToString.append("\n\n// More Datas **************************************************************************\n\n");
				DefToString.append( 
					"#def_data:\n" +
					"\n"+
					"#Names:\n" +
					"	//These data list are for reference only\n" +
					SB.toString() +
					"\n"+
					"#Bytes:"+
					DataBytes.toString() + 
					"\n"+
					"#end def_data;");
			}
		} catch(Exception E) {
			throw new RuntimeException("Unable to export the type package: There is an exception thrown while trying to serialize extra datas.", E);
		}
		
		return DefToString.toString();
	}
	
	// AsPockget -------------------------------------------------------------------------------------------------------

	/** Load a type package as text from a file named FN */
	static public PTypePackage loadAsPocketFromFile(String FN) throws IOException {
		PTypeProvider PTP = PTypeProviderPocket.Simple.loadAsPocketFromFile(FN);
		if(!(PTP instanceof PTypePackage)) throw new IllegalArgumentException("The given file '"+FN+"' does not contain a PTypePackage.");
		return (PTypePackage)PTP;
	}

	/** Load a type package as text from a file F */
	static public PTypePackage loadAsPocketFromFile(File F) throws IOException {
		PTypeProvider PTP = PTypeProviderPocket.Simple.loadAsPocketFromFile(F);
		if(!(PTP instanceof PTypePackage)) throw new IllegalArgumentException("The given file '"+F.toString()+"' does not contain a PTypePackage.");
		return (PTypePackage)PTP;
	}

	/** Load a type package as text from a stream IS */
	static public PTypePackage loadAsPocketFromStream(InputStream IS) throws IOException {
		PTypeProvider PTP = PTypeProviderPocket.Simple.loadAsPocketFromStream(IS);
		if(!(PTP instanceof PTypePackage)) if(PTP instanceof PTypePackage) throw new IllegalArgumentException("The given stream '"+IS.toString()+"' does not contain a PTypePackage.");
		return (PTypePackage)PTP;
	}
	
	/** Save this type package as text to a file named FN */
	public void saveAsPocketToFile(String FN) throws IOException {
		PTypeProviderPocket.Simple.savePocketToFile(FN, new PTypePackagePocket((this)));
	}

	/** Save this type package as text to a file F */
	public void saveAsPocketToFile(File F) throws IOException {
		PTypeProviderPocket.Simple.savePocketToFile(F, new PTypePackagePocket((this)));
	}

	/** Save this type package as text to a stream OS */
	public void saveAsPocketToStream(OutputStream OS) throws IOException {
		PTypeProviderPocket.Simple.savePocketToStream(OS, new PTypePackagePocket((this)));
	}
	
	// Simplifying the code --------------------------------------------------------------------------------------------
	
	/** Replace a simplified compiled code with the actual code (Support Java and JavaScript only) */
	static public String replaceCode(String pTypeName, String ThisResultName, String TypePackageName, String EIndexName,
			String pCompilationContextName,  String pCode) {
		// Two Default Replacer
		if(CodeReplacer.getCodeReplacer(RegParser2Java      .Name) == null) CodeReplacer.registerCodeReplacer(RegParser2Java      .Instance);
		if(CodeReplacer.getCodeReplacer(RegParser2JavaScript.Name) == null) CodeReplacer.registerCodeReplacer(RegParser2JavaScript.Instance);
		
		HashMap<String, String> Params = (EIndexName == null)?null:new HashMap<String, String>(1);
		if(EIndexName != null) Params.put("EntryIndex", EIndexName);
		return CodeReplacer.replaceCode(pCode, pTypeName, ThisResultName, TypePackageName, pCompilationContextName, Params);
	}
	
	/** Create a new replaced function */
	static Function newReplacedFunction(String pTypeName, Signature pSignature, String[] pParamNames, String pCode,
			CompileOption pOption, ProblemContainer pResult, String PNParseResult, String PNTypePackage, String PNEntryIndex,
			String PNCompilationContext) {
		if(pCode == null) return null;
		String Code = PTypePackage.replaceCode(pTypeName, PNParseResult, PNTypePackage, PNEntryIndex, PNCompilationContext, pCode);
		ScriptEngine SE = ScriptManager.GetEngineFromCode(Code);
		if(SE == null) throw new RuntimeException(
				"Unknown script engine for the code.\n" +
				"The first line of a code should starts with the engine declaration.\n" +
				"For example:\n" +
				"	Ex1: // @Java:\\n\n"+
				"	Ex2: // @JavaScript:\\n           <----- for Java 6.0 and up\n"+
				"	Ex3: // @RegParser(Java):\\n      <----- for Compiler code only\n"
				);
		return SE.newFunction(pSignature, pParamNames, Code, null, null, pOption, pResult);
	}
	
	// Load all type ---------------------------------------------------------------------------------------------------
	
	/** Load a type package as text from a file named FN */
	static public PTypePackage loadFromFile(String FN) throws IOException {
		if(FN != null) {
			File F = new File(FN);
			if(!F.exists())  throw new RuntimeException("The given file '"+FN+"' does not exist.");
			if(!F.canRead()) throw new RuntimeException("The given file '"+FN+"' is not readable.");
			PTypePackage PTP = null;
			try { if((PTP = PTypePackage.loadAsTextFromFile(FN))         != null) return PTP; } catch (IllegalArgumentException E) {}
			try { if((PTP = PTypePackage.loadAsSerializableFromFile(FN)) != null) return PTP; } catch (IllegalArgumentException E) {}
			try { if((PTP = PTypePackage.loadAsPocketFromFile(FN))       != null) return PTP; } catch (IllegalArgumentException E) {}
		}
		throw new RuntimeException("The given file '"+FN+"' does not contain a PTypePackage.");
	}

}