package net.nawaman.regparser.typepackage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Random;

import net.nawaman.script.CompileOption;
import net.nawaman.script.Executable;
import net.nawaman.script.ExecutableInfo;
import net.nawaman.script.FrozenVariableInfos;
import net.nawaman.script.Function;
import net.nawaman.script.ProblemContainer;
import net.nawaman.script.Scope;
import net.nawaman.script.ScriptEngine;
import net.nawaman.script.ScriptEngineOption;
import net.nawaman.script.ScriptManager;
import net.nawaman.script.Signature;
import net.nawaman.script.Tools;
import net.nawaman.script.Utils;
import net.nawaman.usepath.FileExtFilter;
import net.nawaman.usepath.FileExtUsableFilter;
import net.nawaman.usepath.UsableFilter;

/**
 * An engine compatible with SimpleScript Engine to be used so that TypeDefinition can be compiled the sameway Script
 *    in the SimpleScript Environment.
 **/
public class TPackageScriptEngine extends ScriptEngine.Simple {

	/** The name of the Engine */
	static public final String Name      = TPackageScriptEngine.class.getCanonicalName();
	/** The name of the Engine */
	static public final String ShortName = "RegParserTypePackage";
	
	/** The short Signature Spec */
	static public final String         SIMPLE_KIND_NAME  = "TypePackage";
	/** The short Signature Spec */
	static public final String         SIMPLE_KIND_SPEC  = "{ "+SIMPLE_KIND_NAME+" }";
	/** The full Signature Spec */
	static public final String         SIGNATURE_SPEC    = "{ function ():"+PTypePackage.class.getCanonicalName()+" }";
	/** The default Signature for TypeDefinition */
	static public final Signature      TYPEDEF_SIGNATURE = new Signature.Simple("getTypePackage", PTypePackage.class);
	/** The default ExecutableInfo for TypeDefinition */
	static public final ExecutableInfo TYPEDEF_EXECINFO  = new ExecutableInfo(null, "function", TYPEDEF_SIGNATURE, SIGNATURE_SPEC, null);
	
	/** Filter for TPTFile only (tpt = TypePackage as Text) */
	static class TPTFileFilter extends FileExtUsableFilter {
		public TPTFileFilter() {
			super(new FileExtFilter.ExtListFileFilter("tpt"));
		}
	}
	
	static TPTFileFilter TPTFileFilter = new TPTFileFilter(); 
	
	// Services as SimpleScriptEngine ----------------------------------------------------------------------------------
	
	static private TPackageScriptEngine Instance = null;
	
	/** Get the only instance of SSEngine */
	static public TPackageScriptEngine newInstance(ScriptEngineOption pOption) {
		if(Instance == null) Instance = new TPackageScriptEngine();
		return Instance;
	}
	
	/**{@inheritDoc}*/ @Override
	public String getName() {
		return Name;
	}

	/**{@inheritDoc}*/ @Override
	public String getShortName() {
		return ShortName;
	}

	/**{@inheritDoc}*/ @Override
	public UsableFilter[] getUsableFilters() {
		return new UsableFilter[] { TPTFileFilter };
	}
	
	/**{@inheritDoc}*/ @Override
	public ProblemContainer newCompileProblemContainer() {
		// TODO - Make this
		return null;
	}

	/**{@inheritDoc}*/ @Override
	public ExecutableInfo getReplaceExecutableInfo(ExecutableInfo EInfo) {
		if((EInfo == null) || (EInfo.Kind == null)) return TYPEDEF_EXECINFO;
		return EInfo;
	}
	
	/**{@inheritDoc}*/ @Override
	public boolean isCompilable() {
		return true;
	}

	/**{@inheritDoc}*/ @Override
	public boolean isCompiledCodeSerializable() {
		return true;
	}

	// The functions that are waited to be saved
	HashMap<Integer, Function> FunctionToBeSaved = new HashMap<Integer, Function>();
	Random                     Random            = new Random();
	
	// NOTE: It is a constrains (for the integrety of the saved data) that the save of the text and its compiled
	//     executable must be comming out of one of the newXXX function of a ScriptEngine in order to be saved together.
	//     However, TypeDef in TypePackage are separatedly compiled in order to reduce compilation time as only the one
	//     that has been changed are compiled. This results in PTypePackage of the code that is not comming out of
	//     newFunction. 
	
	/** Save a pair of typedef and PTypePackage */
	void saveTypeDefAsText(String pName, String pCode, PTypePackage pTPackage, OutputStream OS)
	         throws IOException, ClassNotFoundException {
		
		final int RInt = Random.nextInt();
		this.FunctionToBeSaved.put(RInt + pCode.hashCode(), new GetTypePackageFunction(pTPackage));
		CompileOption COption = new CompileOption() { public @Override int hashCode() { return RInt; } };
		Tools.CompileAndSave(pCode, pName, OS, COption, null);
	}
	
	/**{@inheritDoc}*/ @Override
	public Function newFunction(Signature pSignature, String[] pParamNames, String pCode, Scope pFrozen,
			String[] pFrozenVNames, CompileOption pOption, ProblemContainer pResult) {
		
		// The functions that are waited to be saved
		if((pCode != null) && (pOption != null)) {
			int Hash = pCode.hashCode() + pOption.hashCode();
			Function F = this.FunctionToBeSaved.get(Hash);
			if(F != null) {
				this.FunctionToBeSaved.remove(Hash);
				return F;
			}
		}
		
		if((pSignature.getParamCount() != 0) || pSignature.isVarArgs() ||
		   (!PTypePackage.class.isAssignableFrom(pSignature.getReturnType())))
			throw new IllegalArgumentException("Unsupport function signature: " + pSignature);
		
		return new GetTypePackageFunction(PTypePackage.newPackageFromDefs(pCode));
	}

	/**{@inheritDoc}*/ @Override
	public Executable compileExecutable(ExecutableInfo pExecInfo, String pCode, CompileOption pOption,
			ProblemContainer pResult) {
		
		// TypePackage
		if(SIMPLE_KIND_NAME.equals(pExecInfo.Kind) && (pExecInfo.Signature == null))
			pExecInfo = TYPEDEF_EXECINFO;
		
		return Utils.compileExecutable(this, pExecInfo, pCode, pOption, pResult);
	}

	/**{@inheritDoc}*/ @Override
	public String getLongComments(String Comment, int Width) {
		int WidthMinusOne = Width - 1;
		StringBuilder SB = new StringBuilder();
		while(SB.length() <  WidthMinusOne) SB.append("*");
		return String.format("/%1$s\n%2$s\n%1$s/", SB.toString(), Comment);
	}
	
	// Helper class ----------------------------------------------------------------------------------------------------
	
	/** A simulation function to hold the PTypePackage */
	static protected class GetTypePackageFunction implements Function {
		
        private static final long serialVersionUID = -6766703293542983777L;

        protected GetTypePackageFunction(PTypePackage pTPackage) {
			this.TPackage = pTPackage;
		}
		
		transient private PTypePackage TPackage;
		
		/** Execute the function */
		public Object run(Object ... pParams) {
			return this.TPackage;
		}
		
		// Serialization -----------------------------------------------------------------------------------------------
		
		// TPackage needed to be saved by a regulat OutputStream

		/** Custom deserialization is needed. */
		private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
			// Save the rest
			aStream.defaultReadObject();

			ObjectInputStream OIS = new ObjectInputStream(aStream);
			this.TPackage = (PTypePackage)OIS.readObject();
			
		}
		/** Custom serialization is needed. */
		private void writeObject(ObjectOutputStream aStream) throws IOException {
			// Save the rest
			aStream.defaultWriteObject();

			ObjectOutputStream OOS = new ObjectOutputStream(aStream);
			OOS.writeObject(this.TPackage);			
		}
		
		// To Satisfy Executable ---------------------------------------------------------------------------------------
		
		/**{@inheritDoc}*/ @Override
		public String getEngineName() {
			return TPackageScriptEngine.Name;
		}
		
		/**{@inheritDoc}*/ @Override
		public ScriptEngine getEngine() {
			return ScriptManager.Instance.getDefaultEngineOf(TPackageScriptEngine.Name);
		}
		
		/**{@inheritDoc}*/ @Override
		public FrozenVariableInfos getFVInfos() {
			return null;
		}
		
		/**{@inheritDoc}*/ @Override
		public Executable reCreate(Scope pNewFrozenScope) {
			return this;
		}
		
		// To Satisfy Function -----------------------------------------------------------------------------------------
		
		/**{@inheritDoc}*/ @Override
		public String getCode() {
			return null;
		}
		
		/**{@inheritDoc}*/ @Override
		public Signature getSignature() {
			return TYPEDEF_SIGNATURE;
		}
	}
}
