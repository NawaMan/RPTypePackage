/*----------------------------------------------------------------------------------------------------------------------
 * Copyright (C) 2008-2021 Nawapunth Manusitthipol. Implements with and for Java 11 JDK.
 *----------------------------------------------------------------------------------------------------------------------
 * LICENSE:
 * 
 * This file is part of Nawa's RPTypePackage.
 * 
 * The project is a free software; you can redistribute it and/or modify it under the SIMILAR terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2 of the License, or any later version.
 * You are only required to inform me about your modification and redistribution as or as part of commercial software
 * package. You can inform me via nawa<at>nawaman<dot>net.
 * 
 * The project is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the 
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 * ---------------------------------------------------------------------------------------------------------------------
 */

package net.nawaman.regparser.typepackage;

import java.io.Serializable;

import net.nawaman.regparser.ParserType;
import net.nawaman.regparser.RegParser;
import net.nawaman.script.Function;
import net.nawaman.script.ScriptManager;
import net.nawaman.script.Signature;

/**
 * Creates a PType from the spec
 *
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 **/
final public class PTKind implements Serializable {
	
    private static final long serialVersionUID = 2984929681138996368L;
    
    /** The default name of the parameter PTPackage (The first param of the type constructor) */
	static public final String DefaultParamName_PTPackage  = "$TPackage";
	/**
	 * The default name of the parameter SpecData
	 * 	- The second param of the type constructor
	 * 	- The first param of the spec constructor
	 **/
	static public final String DefaultParamName_PTSpecData = "$Spec";
	
	// Constructor -----------------------------------------------------------------------------------------------------
	
	/** Creates a type Spec from Data */
	public PTKind(Data pData) { this(pData.Name, pData.DataNames, pData.TypeCode); }
	/** Creates a type Spec */
	public PTKind(String pName, String[] pDataNames, String pTypeCode) {
		if(pName      == null) throw new NullPointerException();
		if(pDataNames == null) throw new NullPointerException();
		if(pTypeCode  == null) throw new NullPointerException();
		
		this.Data = new Data(pName, pDataNames, pTypeCode);
		this.updateRegParser();
		this.updateTypeConstructor();
	}
	
	Data     Data            = null;
	Function TypeConstructor = null;
	
	// Name ------------------------------------------------------------------------------------------------------------
	
	/** Returns the name of this type kind */
	public String getName() { return this.Data.getName(); }
	
	// Spec ------------------------------------------------------------------------------------------------------------

	/** Creates a new spec engine */
	public PTSpec newSpec(String pPTypeName) {		
		// Returns a new type spec
		return new PTSpec(pPTypeName, this.getName(), this.Data.DataNames);
	}
	
	// Spec Parser -----------------------------------------------------------------------------------------------------

	/** Prefix of the string parser */
	static final String StringParserPrefix = 
		"[:#:]def!Ignored!+%s!Ignored!+parser!Ignored!+($Name:~[:$:]?!Identifier!([:*:]|[:+:])?([:~:]|[:?:])?([:[:][:]:])?~)!Ignored!*[:::](^[:NewLine:])*\n"+
		"(#Body:~(^[:NewLine:][:#:]end!Ignored!+def!Ignored!+parser[:;:])*~:~\n";
	
	/** Suffix of the string parser */
	static final String StringParserSuffix =
		"~)+\n"+
		"[:NewLine:][:#:]end!Ignored!+def!Ignored!+parser[:;:]";


	/** Prefix of the string parser */
	static final String TypeToStringPrefix = 
		"#def %s parser %s:\n";
	
	/** Suffix of the string parser */
	static final String StringToStringSuffix =
		"\n" +
		"#end def parser;";
	
	
	private RegParser TypeParser = null;

	/** Returns the parser for parsing the type spec as a string - for display purposed */
	public String getSpecParserAsString() {

		String       KindName      = this.getName();
		String[]     PropertyNames = this.Data.DataNames;
		StringBuffer Properties    = new StringBuffer();
		
		for(int i = 0; i < PropertyNames.length; i++) {
			String PropertyName = PropertyNames[i];
			if(PropertyName == null) continue;
			
			StringBuffer PropertyNameAlternatives_ExceptThisProperty = new StringBuffer();
			for(int j = 0; j < PropertyNames.length; j++) {
				if(i == j) continue;	// Except this one
				String PName = PropertyNames[j];
				if(PName == null) continue;
				// Append the '|' sign
				if(PropertyNameAlternatives_ExceptThisProperty.length() != 0)
					PropertyNameAlternatives_ExceptThisProperty.append("|");
				// Append the property name
				PropertyNameAlternatives_ExceptThisProperty.append(PName);
			}
			// Change to '.' if empty or add prefix and suffix if not empty.
			if(PropertyNameAlternatives_ExceptThisProperty.length() == 0)
				 PropertyNameAlternatives_ExceptThisProperty = (new StringBuffer()).append(".");
			else PropertyNameAlternatives_ExceptThisProperty =
					(new StringBuffer()).append(
							String.format("(^[:NewLine:][:#:](%s)[:::])", PropertyNameAlternatives_ExceptThisProperty
					));
			
			String EachProperty = String.format(
					"(#%s:~\n"+
					"	%s*\n"+
					"	~:~\n"+
					"	((^[:NewLine:][:#:]%s[:::])*[:NewLine:])?\n"+
					"	[:#:]%s[:::][^[:NewLine:]]*[:NewLine:]\n"+
					"	($%s:~.*~)\n"+
					"~)\n",
					PropertyName,
					PropertyNameAlternatives_ExceptThisProperty.toString(),
					PropertyName,
					PropertyName,
					PropertyName
				);
			if(Properties.length() != 0) Properties.append("|\n");
			Properties.append(EachProperty);
		}
		
		return String.format(StringParserPrefix, KindName)+"(\n"+Properties.toString()+")*\n"+StringParserSuffix;
	}
	/** Returns the parser for parsing the type spec */
	public RegParser getSpecParser() {
		if(this.TypeParser == null) this.updateRegParser();
		return this.TypeParser;
	}
	
	/** Update the reg-parser (internal use only before PTKind should be immutable outside the package) */
	void updateRegParser() {
		this.TypeParser = RegParser.compile(this.getSpecParserAsString());
	}
	
	// Constructor -----------------------------------------------------------------------------------------------------
	
	/** Signature of the constructors */
	static public final Signature TypeConstructorSignature =
		new Signature.Simple(null, ParserType.class, false, PTypePackage.class, PTSpec.class);
	
	/** Returns a newly created PType from the spec */
	ParserType newPTypeFromSpec(PTypePackage pPTPackage, PTSpec pPTypeSpec) {
		String TSKind = (String)pPTypeSpec.getValue(PTSpec.FN_Kind);
		if(!this.getName().equals(TSKind))
			throw new IllegalArgumentException(this.getName() + " type kind cannot process parser type spec of type '"+TSKind+"'");
			
		ParserType     Result = null;
		Throwable Cause  = null;
		try { Result = (ParserType)this.TypeConstructor.run(pPTPackage, pPTypeSpec); }
		catch(Throwable T) { Cause = T; }
		// Invalid return type
		if(Result == null)
			throw new RuntimeException(String.format(
				"Invalid returned parser type from the type-constructor function of the ParserTypeKind `%s` with the " +
				"given spec '%s' (null).",
				this.getName(),
				pPTypeSpec.toString()
			), Cause);
		
		// Returns the type
		return Result;
	}

	/** Update the reg-parser (internal use only before PTKind should be immutable outside the package) */
	void updateTypeConstructor() { this.TypeConstructor = PTKind.newTypeConstructor(this.getName(), this.Data.getTypeCode()); }
	
	/** Create a new Type Constructor - function (PTypePackage, Scope):PType */
	static public Function newTypeConstructor(String pKindName, String pCode) {
		return ScriptManager.GetEngineFromCode(pCode).newFunction(
				PTKind.TypeConstructorSignature,
				new String[] { DefaultParamName_PTPackage, DefaultParamName_PTSpecData },
				pCode, null, null, null, null);
	}
	
	// Parsing and ToString --------------------------------------------------------------------------------------------
	
	static public final String PTKindParserString = 
		"[:#:]def[:_:]kind!Ignored!+($KindName:!Identifier!)[:::](^[:NewLine:])*\n"+
		"(#Body:~(^[:NewLine:][:#:]end!Ignored!+def_kind[:;:])*~:~\n"+
		"	(#Variables:~\n"+
		"		(^[:NewLine:][:#:]Constructor[:::])*\n"+
		"		~:~\n"+
		"		((^[:NewLine:][:#:]Variables[:::])*[:NewLine:])?\n"+
		"		[:#:]Variables[:::][^[:NewLine:]]*[:NewLine:]!Ignored!*\n"+
		"		(var!Ignored!+($VarName:!Identifier!)!Ignored!*[:;:]!Ignored!*)*\n"+
		"	~)\n"+
		"	(#Constructor:~\n"+
		"		.*\n"+
		"		~:~\n"+
		"		((^[:NewLine:][:#:]Constructor[:::])*[:NewLine:])?\n"+
		"		[:#:]Constructor[:::][^[:NewLine:]]*[:NewLine:]\n"+
		"		($ConstructorCode:~.*~)\n"+
		"	~)\n"+
		"~)+\n"+
		"[:NewLine:][:#:]end!Ignored!+def[:_:]kind[:;:]";
	
	static RegParser PTKindParser = null; 
	
	/** Returns the string representaton of this type kind */
	@Override public String toString() {
		StringBuffer Properties = new StringBuffer();
		
		for(int i = 0; i < this.Data.getDataCount(); i++) {
			String PropertyName = this.Data.getDataNames(i);
			if(PropertyName == null) continue;
			Properties.append(String.format("	var %s;\n", PropertyName));
		}
		
		return String.format(
					"#def_kind %s:\n" +
					"\n"+
					"#Variables:\n"+
					"%s"+
					"\n"+
					"#Constructor:\n"+
					"%s"+
					"\n"+
					"#end def_kind;",
					this.getName(),
					Properties.toString(),
					this.Data.getTypeCode()
				);
	}
	
	// Load & Save -----------------------------------------------------------------------------------------------------
	
	/** The data object for saving and loading PTypeKind */
	static final public class Data implements Serializable {
		
        private static final long serialVersionUID = -7182757944647951764L;

        Data(String pName, String[] pDataNames, String pTypeCode) {
			this.Name      = pName;
			this.DataNames = pDataNames.clone();
			this.TypeCode  = pTypeCode;
		}
		
		String   Name;
		String[] DataNames;
		String   TypeCode;
		
		/** Returns the name if the type kind */
		public String getName() {
			return this.Name;
		}
		/* Returns the number of data (exclude Name) */
		public int getDataCount() {
			return this.DataNames.length;
		}
		/** Returns the name of the data at the index (exclude Name) */
		public String getDataNames(int I) {
			if((I < 0) || (I >= this.DataNames.length)) return null;
			return this.DataNames[I];
		}
		/** Returns the code for type constructor */
		public String getTypeCode() {
			return this.TypeCode;
		}
	}
	
}