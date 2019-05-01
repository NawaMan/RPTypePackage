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

package net.nawaman.regparser.typepackage;

import net.nawaman.javacompiler.JavaCompilerObjectInputStream;
import net.nawaman.javacompiler.JavaCompilerObjectOutputStream;
import net.nawaman.regparser.*;
import net.nawaman.util.UString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Pocket holding PTypePackage.
 * 
 * The pocket ensures that the error involving serialization is partly tolerated.  
 *
 * @author Nawapunth Manusitthipol
 */
public class PTypePackagePocket implements PTypeProviderPocket {
		
	public PTypePackagePocket(PTypePackage pTPackage) {
		this.TPackage = pTPackage;
	}
		
	transient boolean      isRepackagingNeeded = false;
	transient PTypePackage TPackage            = null;

	public PTypeProvider getTProvider() {
		return this.TPackage;
	}
		
	public boolean isRepackagingNeeded() {
		return this.isRepackagingNeeded;
	}

	/** Custom deserialization is needed. */
	private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
		// Save the rest
		aStream.defaultReadObject();

		ObjectInputStream IOS = null;
		try {
			IOS = new ObjectInputStream(new ByteArrayInputStream((byte[])aStream.readObject()));
			
			// Get the byte array that holds the Package as serialized object
			ObjectInputStream IOS_TP = JavaCompilerObjectInputStream.NewJavaCompilerObjectInputStream(new ByteArrayInputStream((byte[])IOS.readObject()));
			
			// Load the body
			this.TPackage = (PTypePackage)IOS_TP.readObject();
		} catch(Exception E) {
			if(IOS == null) return;

			// Get the byte array that holds the type definition
			ObjectInputStream IOS_TD = new ObjectInputStream(new ByteArrayInputStream((byte[])IOS.readObject()));
			
			String Defs = (String)IOS_TD.readObject();
			if(Defs == null) return;
			try {
				this.TPackage = PTypePackage.newPackageFromDefs(Defs);
				this.isRepackagingNeeded();
			} catch(Throwable EE) {}
		} finally { if(IOS != null) { try { IOS.close(); } catch(Throwable E) {} } }
	}

	/** Custom serialization is needed. */
	private void writeObject(ObjectOutputStream aStream) throws IOException {
		// Save the rest
		aStream.defaultWriteObject();

		// Save the Kinds and Types
		ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
		ObjectOutputStream    OOS  = new ObjectOutputStream(BAOS);

		// Separate the compiled package and the compiled package
		// This is to protect against serialization problem like SerializableID changed or field added.
		try {
			// Save the Package as serialized object
			ByteArrayOutputStream BAOS_TP = new ByteArrayOutputStream();
			ObjectOutputStream    OOS_TP  = JavaCompilerObjectOutputStream.NewJavaCompilerObjectOutputStream(BAOS_TP);
			OOS_TP.writeObject(this.TPackage);
			OOS_TP.close();

			// Save the type definition
			ByteArrayOutputStream BAOS_TD = new ByteArrayOutputStream();
			ObjectOutputStream    OOS_TD  = new ObjectOutputStream(BAOS_TD);
			OOS_TD.writeObject((this.TPackage == null) ? null : this.TPackage.getDefsAsSrting());
			OOS_TD.close();
			
			OOS.writeObject(BAOS_TP.toByteArray());
			OOS.writeObject(BAOS_TD.toByteArray());
			OOS.close();
			aStream.writeObject(BAOS.toByteArray());
		} catch(Exception E) {
		} finally { OOS.close(); }
	}
	
	// Utility methods -------------------------------------------------------------------------------------------------
	
	static private JFileChooser TPPFileChooser = null;
	
	/** Gets the Type definition and save it to a file */
	static public boolean saveTypeDeinitionTo() {
		String TypeDefs = extractTypeDeinition();
		if(TypeDefs == null) return false;
		
		if(TPPFileChooser == null) TPPFileChooser = new JFileChooser();
		File TheFile = TPPFileChooser.getSelectedFile();
		if((TheFile != null) && TheFile.exists()) {
			// Ask for overwrite
			int O = JOptionPane.showConfirmDialog(null, "The file '" + TheFile.toString()+ "' is already " +
					"exist.\n Are you sure you want to save over it?");
			if(O != JOptionPane.YES_OPTION) return false;
		}
		
		try { UString.saveTextToFile(TheFile, TypeDefs); }
		catch(Exception E) { return false; }
		return true;
	}
	
	/** Gets the Type definition and save it to a file */
	static public boolean saveTypeDeinitionTo(String FNameToSave) {
		if(FNameToSave == null) return false;
		return saveTypeDeinitionTo(new File(FNameToSave));
	}
	
	/** Gets the Type definition and save it to a file */
	static public boolean saveTypeDeinitionTo(File FileToSave) {
		if((FileToSave == null) || !FileToSave.exists()) return false;
		
		String TypeDefs = extractTypeDeinition();
		if(TypeDefs == null) return false;
		
		try { UString.saveTextToFile(FileToSave, TypeDefs); }
		catch(Exception E) { return false; }
		return true;
	}
	
	/** Gets the Type definition out of the input stream that holds PTypePackagePocket */
	static public String extractTypeDeinition() {
		if(TPPFileChooser == null) TPPFileChooser = new JFileChooser();
		File TheFile = TPPFileChooser.getSelectedFile();
		if((TheFile == null) || !TheFile.exists()) return null;
		
		return extractTypeDeinition(TheFile);
	}
	
	/** Gets the Type definition out of the input stream that holds PTypePackagePocket */
	static public String extractTypeDeinition(String FName) {
		return extractTypeDeinition(new File(FName));
	}
	
	/** Gets the Type definition out of the input stream that holds PTypePackagePocket */
	static public String extractTypeDeinition(File F) {
		try { return extractTypeDeinition(new FileInputStream(F)); }
		catch(Exception E) { return null; }
	}
	
	/** Gets the Type definition out of the input stream that holds PTypePackagePocket */
	static public String extractTypeDeinition(InputStream IS) {
		try {
			ObjectInputStream IOS    = new ObjectInputStream(IS);
			
			// Read the bytes but not used.
			IOS.readObject();
			
			ObjectInputStream IOS_TD = new ObjectInputStream(new ByteArrayInputStream((byte[])IOS.readObject()));
			return (String)IOS_TD.readObject();
		}
		catch(Exception E) {}
		finally {
			try { IS.close(); }
			catch(Exception E) {}
		}
		return null;
	}
}