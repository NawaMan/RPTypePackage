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

import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.HashMap;
import java.util.Set;

import net.nawaman.script.Scope;

/**
 * Specification of parser type
 *
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 **/
public class PTSpec implements Scope, Serializable {
	
    private static final long serialVersionUID = -8315783595715374142L;
    
    /** Field name of the field `Name` */
	static public final String FN_Name = "Name";
	/** Field name of the field `Kind` */
	static public final String FN_Kind = "Kind";
	
	/** Constructs a type spec */
	PTSpec(String pName, String pKind, String ... pOtherDataNames) {
		if(pName == null) throw new NullPointerException();
		if(pKind == null) throw new NullPointerException();
		
		if(pOtherDataNames == null) throw new NullPointerException();
		
		this.Datas.put(FN_Name, pName);
		this.Datas.put(FN_Kind, pKind);
		
		for(String N : pOtherDataNames) { if(N == null) continue; this.Datas.put(N, null); }
	}
	
	protected HashMap<String, String> Datas = new HashMap<String, String>();

	/** Returns the name of the type name of this spec */
	final public String getName() { return this.Datas.get(FN_Name); }
	/** Returns the name of the type kind of this spec */
	final public String getKind() { return this.Datas.get(FN_Kind); }
	
	// Implements scope ------------------------------------------------------------------------------------------------

	/** Returns a variable and constant names */
	final public Set<String> getVariableNames() {
		return this.Datas.keySet();
	}
	
	/** Returns the variable count */
	final public int getVarCount() { return this.Datas.size(); }
	
	/** Returns the variable value */
	final public Object  getValue(String pName) { return this.Datas.get(pName); }
	/** Change the variable value and return if success */
	final public Object setValue(String pName, Object pValue) {
		if(!this.isWritable(pName))                         throw new RuntimeException("The spec data `"+pName+"` is not writable.");
		if((pValue != null) && !(pValue instanceof String)) throw new RuntimeException("Invalid assign value '"+pValue+"' for the spec data '"+pName+"'.");
		this.Datas.put(pName, (pValue == null)?null:pValue.toString());
		return true;
	}
	
	/** Create a new variable and return if success */
	final public boolean newVariable(String pName, Class<?> pType, Object pValue)  { return false; }
	/** Create a new constant and return if success */
	final public boolean newConstant(String pName, Class<?> pType, Object pValue) { return false; }
	
	/** Removes a variable or a constant and return if success */
	final public boolean removeVariable(String pName) { return false; }
	
	/** Returns the variable value */
	final public Class<?> getTypeOf(String pName)  { return String.class;  }
	/** Checks if the variable of the given name is writable */
	final public boolean  isExist(String pName)    { return this.Datas.containsKey(pName); }
	/** Checks if the variable of the given name is writable */
	final public boolean  isWritable(String pName) {
		return this.Datas.containsKey(pName) && !(FN_Name.equals(pName) || FN_Kind.equals(pName));
	}
	/** Checks if this scope support constant declaration */
	final public boolean  isConstantSupport()     { return false; }

    /** Returns the <code>Writer</code> for scripts to use when displaying output. */
	final public Writer getWriter()      { return Scope.Simple.DOut; }
    /** Returns the <code>Writer</code> used to display error output. */
	final public Writer getErrorWriter() { return Scope.Simple.DErr; }
    /** Returns a <code>Reader</code> to be used by the script to read input. */
	final public Reader getReader()      { return Scope.Simple.DIn;  }
    
    /** Sets the <code>Writer</code> for scripts to use when displaying output. */
	final public void setWriter(Writer writer)      { return; }
    /** Sets the <code>Writer</code> used to display error output. */
	final public void setErrorWriter(Writer writer) { return; }
    /** Sets the <code>Reader</code> for scripts to read input */
	final public void setReader(Reader reader)      { return; }
	
}