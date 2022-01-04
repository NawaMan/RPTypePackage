/*
 * TypeFactory.java
 *
 * Created on June 22, 2008-2019, 3:01 AM
 */

package net.nawaman.regparser.typepackage;

import java.awt.event.KeyEvent;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import net.nawaman.javacompiler.JavaCompiler;
import net.nawaman.regparser.CompilationContext;
import net.nawaman.regparser.RegParser;
import net.nawaman.script.Scope;
import net.nawaman.script.ScriptManager;
import net.nawaman.script.java.JavaEngine;
import net.nawaman.swing.LineNumberedTextComponentPanel;

/**
 *
 * @author  nawaman
 */
public class TypeFactory extends javax.swing.JFrame {

    private static final long serialVersionUID = -2770028697317853630L;

    static final String ProgramName = "TypeFactory 1.0";

	/** Creates new form TypeFactory */
	public TypeFactory() {
		this(null, null);
	}

	/** Creates new form TypeFactory */
	@SuppressWarnings({ "rawtypes", "unchecked" })
    TypeFactory(TypeFactory pTFactory, PTypePackage pTPackage) {
		// Is new type package or the loaded one.
		boolean IsNewOne = false;
		
		// Default package
		if (pTPackage == null) {
			pTPackage = new PTypePackage();
			IsNewOne = true;
		}
		this.TPackage = pTPackage;
		this.TPackage.useCommonKinds();
		this.TPackage.IsFrozen = true;

		initComponents();

		this.setPreferredSize(new Dimension(1200, 800));

		Dimension ScreenDimension = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation((ScreenDimension.width - this.getWidth()) / 2,
				(ScreenDimension.height - this.getHeight()) / 2);

		this.This = this;
		// Count the still running window
		TypeFactoryWindowCount++;

		// Add the type names into the list
		Set<String> TNames = this.TPackage.typeNames();
		if (TNames != null) {
			DefaultListModel LModel = (DefaultListModel) this.L_Types.getModel();
			for (String N : TNames) {
				boolean IsSet = false;
				for (int i = 0; i < LModel.getSize(); i++) {
					String TypeName = LModel.getElementAt(i).toString();
					if (TypeName.compareTo(N) > 0) {
						// Delete from the list
						LModel.insertElementAt(N, i);
						this.L_Types.setSelectedIndex(i);
						IsSet = true;
						break;
					}
				}
				if (!IsSet)
					LModel.addElement(N);
			}
		}

		// Add new menu item from the parent TypeFactory ---------------------------------------------------------------
		if(pTFactory != null) {
			int Count = pTFactory.MM_ClassPaths.getItemCount();
			for(int i = 0; i < Count; i++) {
				JMenuItem OMI = pTFactory.MM_ClassPaths.getItem(i);
				if((OMI == null) || !OMI.getText().startsWith("URL: ") && !OMI.getText().startsWith("JAR: ")) continue;
				this.addNewClassPathMenuItem(OMI.getText(), false);
			}
		}
		
		// Set the MainScope and CContext Code (CodeLab will be done by ensureUIAndModelSynchronized())
		Object Data = this.TPackage.getData("MainScope");
		if((Data == null) || (Data instanceof String)) {
			this.MainScopeCode = (String)Data;
			this.MICB_MainScopeSave.setSelected((Data != null) || IsNewOne);
		} else {
			try {
				this.TPackage.IsFrozen = false;
				this.TPackage.removeData("MainScope");
			} finally { this.TPackage.IsFrozen = true; }
		}
		
		Data = this.TPackage.getData("CContext");
		if((Data == null) || (Data instanceof String)) {
			this.CContextCode = (String)Data;
			this.MICB_CContextSave.setSelected((Data != null || IsNewOne));
		} else {
			try {
				this.TPackage.IsFrozen = false;
				this.TPackage.removeData("CContext");
			} finally { this.TPackage.IsFrozen = true; }
		}
		Data = this.TPackage.getData("ToFreeze");
		if(IsNewOne || (Data instanceof Boolean)) this.MICB_Freeze.setSelected(IsNewOne || (Boolean)Data);
		
		// Ensure all the class path of the given TypePackage is added
		this.ensureUIAndModelSynchronized();
		
		LineNumberedTextComponentPanel.Config Config =  new LineNumberedTextComponentPanel.Config();
		Config.setLineNumberDigit(0);
		Config.setRightLimit(0);
		this.LNP_Parser.setText("Reg(ular\\ )?Parser");
		this.LNP_Parser.getTextComponent().setSelectionStart(this.LNP_Parser.getText().length());
		this.LNP_Parser.getTextComponent().setSelectionEnd(this.LNP_Parser.getText().length());
		this.LNP_Parser.setConfig(Config);
		this.LNP_Parser.getTextComponent().addKeyListener(new java.awt.event.KeyAdapter() {
			@Override public void keyReleased(KeyEvent evt) {
				if(evt.isControlDown() && (evt.getKeyCode() == KeyEvent.VK_ENTER)) B_NewParser.doClick();
			}
        });
	}

	// UI related ------------------------------------------------------------------------------------------------------
	static int TypeFactoryWindowCount = 0;
	
	private int          ParserResultCount = 1;
	private TypeFactory  This           = null;
	private JFileChooser JarFileChooser = null;
	private JFileChooser TPFileChooser  = null;
	private JFileChooser TPKFileChooser = null;
	private JFileChooser TPTFileChooser = null;
	private JFileChooser TPPFileChooser = null;

	// Model related fields --------------------------------------------------------------------------------------------
	static final String[]           TypeFileExt              = new String[] { ".tpk", ".tpt", ".tpp" };
	static final String             TypeFileDesc             = "Type package file (*.tpk; *.tpt; *.tpp)";
	static final String[]           TypeSerializableFileExt  = new String[] { ".tpk" };
	static final String             TypeSerializableFileDesc = "Type package serializable file (*.tpk)";
	static final String[]           TypeTextFileExt          = new String[] { ".tpt" };
	static final String             TypeTextFileDesc         = "Type package definition file (*.tpt)";
	static final String[]           TypePocketFileExt        = new String[] { ".tpp" };
	static final String             TypePocketFileDesc       = "Type package pocket file (*.tpp)";
	static final CompilationContext DefaultCContext          = new CompilationContext.Simple();

	private File               WorkingDir     = new File(".");
	private PTypePackage       TPackage       = null;
	private String             MainScopeCode  = null;
	private String             CContextCode   = null;
	private CompilationContext CContext       = DefaultCContext;
	private int                CodeLabCount   = 0;
	private boolean            HaveBeenEdited = false;
	private File               ThisFile       = null;
	private char               ThisFileAs     = 'S';
	
	static private boolean IsUncheckClassPathWarningDisplayed = false;
	static private boolean IsImportAsTextDisplayed            = false;

	/** Returns the current Type Package */
	PTypePackage getTPackage() {
		return this.TPackage;
	}

	/** Returns the current Compilation Context */
	CompilationContext getCContext() {
		return this.CContext;
	}

	/** Set the text of the status bar */
	void setStatusBarText(String pStatusBarText) {
		this.L_StatusBar.setToolTipText(pStatusBarText);
		if((pStatusBarText != null) && (pStatusBarText.length() >= 100))
			pStatusBarText = pStatusBarText.substring(0, 100) + " ... ";
		this.L_StatusBar.setText(pStatusBarText);
	}

	/** Set the text of the status bar */
	void setStatusBarToolTipText(String pStatusBarToolTipText) {
		this.L_StatusBar.setToolTipText(pStatusBarToolTipText);
	}

	private void ensureTypeInfoDisplay(String pTypeName) {
		// First create a new tab
		for (int i = 0; i < this.PT_Tabs.getTabCount(); i++) {
			Object O2 = this.PT_Tabs.getTabComponentAt(i);
			if (!(O2 instanceof ClosableTabTitle))
				continue;
			ClosableTabTitle CTT = (ClosableTabTitle) O2;
			if (pTypeName.equals(((JLabel) CTT.getComponent(0)).getText())) {
				// Found the Tab
				this.PT_Tabs.setSelectedIndex(i);
				return;
			}
		}

		// Create a new one
		int TabIndex = this.PT_Tabs.getTabCount();
		ClosableTabTitle CTT = new ClosableTabTitle(pTypeName, this.PT_Tabs);
		this.PT_Tabs.addTab(pTypeName, new PFTypeInfo(this, pTypeName, CTT.Title));
		this.PT_Tabs.setSelectedIndex(TabIndex);
		this.PT_Tabs.setTabComponentAt(TabIndex, CTT);
	}
	
	private void addNewClassPathMenuItem(String pText, boolean pIsCheck) {
		JCheckBoxMenuItem MI = new JCheckBoxMenuItem();
		MI.setText(pText.trim());
		MI.setSelected(pIsCheck);
		MI.setToolTipText("If checked, the class path will be saved with this TypePackage.");
		this.MM_ClassPaths.add(MI);
		
		MI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	JCheckBoxMenuItem The_MI = (JCheckBoxMenuItem)evt.getSource();
            	if(The_MI.isSelected()) {
            		String Name = The_MI.getText().substring(5);
					try {
						JavaEngine JE = (JavaEngine) ScriptManager.Instance.getDefaultEngineOf(JavaEngine.class.getCanonicalName());
						JE.addClasspathURL(Name);
						
						This.TPackage.IsFrozen = false;
						This.TPackage.addClassPath(Name);
					} finally { This.TPackage.IsFrozen = true; }
            	} else {
            		String Name = The_MI.getText().substring(5);
					try {
						JavaEngine JE = (JavaEngine) ScriptManager.Instance.getDefaultEngineOf(JavaEngine.class.getCanonicalName());
						JE.removeClasspathURL(Name);
				
						This.TPackage.IsFrozen = false;
						This.TPackage.removeClassPath(Name);
					} finally { This.TPackage.IsFrozen = true; }
            		if(!TypeFactory.IsUncheckClassPathWarningDisplayed) {
	            		JOptionPane.showMessageDialog(This,
	            			"BE ADVICED: Be careful when unchecking a classpath.\n\n" +
	            			"Unchecking a classpath will preventing it from being used in a new compilation and disable \n" +
							"it from being saved with the TypePackage. However, any code compiled with the unchecked\n" +
	            			"classpath STILL HAVE ACCESS to the classpath. If such code (including the type compiler,\n" +
							"main scope, compilation context, named codelab) are saved, they WILL NOT work after they\n" +
	            			"are loaded unless the classpath is accessible by the time those codes are loaded.");
	            		TypeFactory.IsUncheckClassPathWarningDisplayed = true;
            		}
            	}
            }
        });
	}
	
	private void ensureUIAndModelSynchronized() {
		// Ensure all the class path of the given TypePackage is added -------------------------------------------------
		if(this.TPackage.ClassPaths != null) {
			int Count = this.TPackage.ClassPaths.size();
			CP: for(int cp = 0; cp < Count; cp++) {
				String URLStr = this.TPackage.ClassPaths.get(cp);
				if(URLStr == null) continue;
				URLStr = URLStr.trim();
				if(URLStr.endsWith(".jar")) URLStr += "!/";
				
				String Start = "URL: ";
				if(URLStr.startsWith("jar:") || URLStr.endsWith(".jar!/")) Start = "JAR: ";
				URLStr = Start + URLStr;
				
				int MCount = this.MM_ClassPaths.getItemCount();
				for(int i = 0; i < MCount; i++) {
					JMenuItem OMI = this.MM_ClassPaths.getItem(i);
					if(OMI == null) continue;
					if(OMI.getText().equals(URLStr)) { OMI.setSelected(true); continue CP; }
					if(OMI.getText().equals(URLStr)) { OMI.setSelected(true); continue CP; }
				}

				this.addNewClassPathMenuItem(URLStr, true);
			}
		}
		// Ensure all the error message of the given TypePackage is added ----------------------------------------------
		if(this.TPackage.ErrorMsgs != null) {
			for(String EN : this.TPackage.ErrorMsgs.keySet()) {
				String M    = this.TPackage.ErrorMsgs.get(EN);
				String Kind = null;
				if(EN.startsWith("WARNING_"))          Kind = "Warning";
				else if(EN.startsWith("ERROR_"))       Kind = "Error";
				else if(EN.startsWith("FATAL_ERROR_")) Kind = "Fatal Error";
				this.createErrMessageMenu(EN, M, Kind);
			}
		}
		
		// Sybchronize the codelabs
		this.ensureCodeLabSynchronizeWithMenu();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void initComponents() {

        PM_TypeList = new javax.swing.JPopupMenu();
        MI_EditType = new javax.swing.JMenuItem();
        MI_TestType = new javax.swing.JMenuItem();
        S_PM_TypeList = new javax.swing.JSeparator();
        MI_PM_NewType = new javax.swing.JMenuItem();
        MI_PM_DeleteType = new javax.swing.JMenuItem();
        P_StatusBar = new javax.swing.JPanel();
        L_StatusBar = new javax.swing.JLabel();
        P_Whole = new javax.swing.JPanel();
        SP_Whole = new javax.swing.JSplitPane();
        P_NewParser = new javax.swing.JPanel();
        B_NewParser = new javax.swing.JButton();
        SP_Parser = new javax.swing.JScrollPane();
        LNP_Parser = new net.nawaman.swing.LineNumberedTextComponentPanel();
        SP_Bottom = new javax.swing.JSplitPane();
        PT_Tabs = new javax.swing.JTabbedPane();
        P_TypeOutter = new javax.swing.JPanel();
        P_TypesInner = new javax.swing.JPanel();
        SP_Types = new javax.swing.JScrollPane();
        L_Types = new javax.swing.JList();
        B_NewType = new javax.swing.JButton();
        B_DeleteType = new javax.swing.JButton();
        MB_MainMenu = new javax.swing.JMenuBar();
        MM_File = new javax.swing.JMenu();
        MI_New = new javax.swing.JMenuItem();
        S_MMFile = new javax.swing.JSeparator();
        MI_Open = new javax.swing.JMenuItem();
        MI_OpenAsText = new javax.swing.JMenuItem();
        MI_OpenAsSerializable = new javax.swing.JMenuItem();
        MI_OpenAsPocket = new javax.swing.JMenuItem();
        S_MMFile2 = new javax.swing.JSeparator();
        MI_ImportAsText = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        Save = new javax.swing.JMenuItem();
        MI_SaveAsText = new javax.swing.JMenuItem();
        MI_SaveAsPocket = new javax.swing.JMenuItem();
        SaveAsSerializable = new javax.swing.JMenuItem();
        S_MM_File3 = new javax.swing.JSeparator();
        MI_Quit = new javax.swing.JMenuItem();
        MM_Edit = new javax.swing.JMenu();
        MI_NewType = new javax.swing.JMenuItem();
        MI_DeleteType = new javax.swing.JMenuItem();
        MM_CodeLab = new javax.swing.JMenu();
        MI_CodeLab = new javax.swing.JMenuItem();
        MI_NewNamedCodeLab = new javax.swing.JMenuItem();
        MI_NewNamedText = new javax.swing.JMenuItem();
        S_CodeLab = new javax.swing.JSeparator();
        MM_ClassPaths = new javax.swing.JMenu();
        MI_AddURL = new javax.swing.JMenuItem();
        MI_AddJarFile = new javax.swing.JMenuItem();
        S_ClassPaths = new javax.swing.JSeparator();
        MM_ErrorMessage = new javax.swing.JMenu();
        MI_NewWarning = new javax.swing.JMenuItem();
        MI_NewErrorMessage = new javax.swing.JMenuItem();
        MI_NewFatalError = new javax.swing.JMenuItem();
        S_ErrorMessage = new javax.swing.JSeparator();
        MM_Setting = new javax.swing.JMenu();
        MM_MainScope = new javax.swing.JMenu();
        MI_MainScope = new javax.swing.JMenuItem();
        MI_ClearMainScope = new javax.swing.JMenuItem();
        MICB_MainScopeSave = new javax.swing.JCheckBoxMenuItem();
        MM_CContext = new javax.swing.JMenu();
        MI_CContextEdit = new javax.swing.JMenuItem();
        MI_CContextClear = new javax.swing.JMenuItem();
        MICB_CContextSave = new javax.swing.JCheckBoxMenuItem();
        S_Setting = new javax.swing.JSeparator();
        MICB_Freeze = new javax.swing.JCheckBoxMenuItem();
        MM_Help = new javax.swing.JMenu();
        MI_Content = new javax.swing.JMenuItem();
        S_MM_Help = new javax.swing.JSeparator();
        MI_About = new javax.swing.JMenuItem();

        MI_EditType.setText("Edit type");
        MI_EditType.setToolTipText("Edit this type");
        MI_EditType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MI_EditTypeActionPerformed(evt);
            }
        });
        PM_TypeList.add(MI_EditType);

        MI_TestType.setMnemonic('T');
        MI_TestType.setText("Test type");
        MI_TestType.setToolTipText("Test this type");
        MI_TestType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MI_TestTypeActionPerformed(evt);
            }
        });
        PM_TypeList.add(MI_TestType);
        PM_TypeList.add(S_PM_TypeList);

        MI_PM_NewType.setMnemonic('N');
        MI_PM_NewType.setText("New type");
        MI_PM_NewType.setToolTipText("Create a new type");
        MI_PM_NewType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MI_PM_NewTypeActionPerformed(evt);
            }
        });
        PM_TypeList.add(MI_PM_NewType);

        MI_PM_DeleteType.setText("Delete type");
        MI_PM_DeleteType.setToolTipText("Delete this type");
        MI_PM_DeleteType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MI_PM_DeleteTypeActionPerformed(evt);
            }
        });
        PM_TypeList.add(MI_PM_DeleteType);

        setTitle("Type Factory 1.0");

        P_StatusBar.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        L_StatusBar.setText("Welcome to RegParser Type Factory");

        javax.swing.GroupLayout P_StatusBarLayout = new javax.swing.GroupLayout(P_StatusBar);
        P_StatusBar.setLayout(P_StatusBarLayout);
        P_StatusBarLayout.setHorizontalGroup(
            P_StatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(L_StatusBar, javax.swing.GroupLayout.DEFAULT_SIZE, 952, Short.MAX_VALUE)
        );
        P_StatusBarLayout.setVerticalGroup(
            P_StatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(L_StatusBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        P_Whole.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));

        SP_Whole.setDividerLocation(60);
        SP_Whole.setDividerSize(3);
        SP_Whole.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        P_NewParser.setMinimumSize(new java.awt.Dimension(100, 32));

        B_NewParser.setMnemonic('P');
        B_NewParser.setText("<html><center>New<br />Parser</center>");
        B_NewParser.setMaximumSize(new java.awt.Dimension(100, 30));
        B_NewParser.setMinimumSize(new java.awt.Dimension(100, 30));
        B_NewParser.setPreferredSize(new java.awt.Dimension(100, 30));
        B_NewParser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                B_NewParserActionPerformed(evt);
            }
        });

        SP_Parser.setViewportView(LNP_Parser);

        javax.swing.GroupLayout P_NewParserLayout = new javax.swing.GroupLayout(P_NewParser);
        P_NewParser.setLayout(P_NewParserLayout);
        P_NewParserLayout.setHorizontalGroup(
            P_NewParserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, P_NewParserLayout.createSequentialGroup()
                .addComponent(SP_Parser, javax.swing.GroupLayout.DEFAULT_SIZE, 862, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(B_NewParser, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        P_NewParserLayout.setVerticalGroup(
            P_NewParserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(SP_Parser, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
            .addComponent(B_NewParser, javax.swing.GroupLayout.DEFAULT_SIZE, 60, Short.MAX_VALUE)
        );

        SP_Whole.setLeftComponent(P_NewParser);

        SP_Bottom.setDividerLocation(200);
        SP_Bottom.setDividerSize(3);
        SP_Bottom.setRightComponent(PT_Tabs);

        P_TypeOutter.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Parser Types", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        P_TypesInner.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 5, 5));

        L_Types.setModel(new DefaultListModel());//  });
		L_Types.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		L_Types.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override public void mouseClicked(java.awt.event.MouseEvent evt) {
		        L_TypesMouseClicked(evt);
		    }
			@Override public void mousePressed(java.awt.event.MouseEvent evt) {
		        L_TypesMousePressed(evt);
		    }
	    });
	    L_Types.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
	        public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
	            L_TypesValueChanged(evt);
	        }
	    });
	    L_Types.addKeyListener(new java.awt.event.KeyAdapter() {
	        @Override public void keyReleased(java.awt.event.KeyEvent evt) {
	            L_TypesKeyReleased(evt);
	        }
	    });
	    SP_Types.setViewportView(L_Types);
	
	    B_NewType.setMnemonic('N');
	    B_NewType.setText("New Type");
	    B_NewType.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            B_NewTypeActionPerformed(evt);
	        }
	    });
	
	    B_DeleteType.setMnemonic('D');
	    B_DeleteType.setText("Delete Type");
	    B_DeleteType.setEnabled(false);
	    B_DeleteType.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            B_DeleteTypeActionPerformed(evt);
	        }
	    });
	
	    javax.swing.GroupLayout P_TypesInnerLayout = new javax.swing.GroupLayout(P_TypesInner);
	    P_TypesInner.setLayout(P_TypesInnerLayout);
	    P_TypesInnerLayout.setHorizontalGroup(
	        P_TypesInnerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	        .addComponent(B_DeleteType, javax.swing.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE)
	        .addComponent(B_NewType, javax.swing.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE)
	        .addComponent(SP_Types, javax.swing.GroupLayout.DEFAULT_SIZE, 178, Short.MAX_VALUE)
	    );
	    P_TypesInnerLayout.setVerticalGroup(
	        P_TypesInnerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, P_TypesInnerLayout.createSequentialGroup()
	            .addComponent(SP_Types, javax.swing.GroupLayout.DEFAULT_SIZE, 450, Short.MAX_VALUE)
	            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
	            .addComponent(B_NewType)
	            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
	            .addComponent(B_DeleteType))
	    );
	
	    javax.swing.GroupLayout P_TypeOutterLayout = new javax.swing.GroupLayout(P_TypeOutter);
	    P_TypeOutter.setLayout(P_TypeOutterLayout);
	    P_TypeOutterLayout.setHorizontalGroup(
	        P_TypeOutterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	        .addComponent(P_TypesInner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
	    );
	    P_TypeOutterLayout.setVerticalGroup(
	        P_TypeOutterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	        .addComponent(P_TypesInner, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
	    );
	
	    SP_Bottom.setLeftComponent(P_TypeOutter);
	
	    SP_Whole.setRightComponent(SP_Bottom);
	
	    javax.swing.GroupLayout P_WholeLayout = new javax.swing.GroupLayout(P_Whole);
	    P_Whole.setLayout(P_WholeLayout);
	    P_WholeLayout.setHorizontalGroup(
	        P_WholeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	        .addComponent(SP_Whole, javax.swing.GroupLayout.DEFAULT_SIZE, 946, Short.MAX_VALUE)
	    );
	    P_WholeLayout.setVerticalGroup(
	        P_WholeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	        .addComponent(SP_Whole, javax.swing.GroupLayout.DEFAULT_SIZE, 621, Short.MAX_VALUE)
	    );
	
	    MM_File.setMnemonic('F');
	    MM_File.setText("File");
	
	    MI_New.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_New.setMnemonic('N');
	    MI_New.setText("New");
	    MI_New.setToolTipText("Create a new TypePackage");
	    MI_New.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_NewActionPerformed(evt);
	        }
	    });
	    MM_File.add(MI_New);
	    MM_File.add(S_MMFile);
	
	    MI_Open.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_Open.setText("Open ...");
	    MI_Open.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_LoadActionPerformed(evt);
	        }
	    });
	    MM_File.add(MI_Open);
	
	    MI_OpenAsText.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_OpenAsText.setMnemonic('T');
	    MI_OpenAsText.setText("Open as Text ...");
	    MI_OpenAsText.setToolTipText("Open a type package from a parser type definition file");
	    MI_OpenAsText.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_LoadActionPerformed(evt);
	        }
	    });
	    MM_File.add(MI_OpenAsText);
	
	    MI_OpenAsSerializable.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_OpenAsSerializable.setMnemonic('O');
	    MI_OpenAsSerializable.setText("Open as Serializable ...        ");
	    MI_OpenAsSerializable.setToolTipText("Open a type package as Serializable object");
	    MI_OpenAsSerializable.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_LoadActionPerformed(evt);
	        }
	    });
	    MM_File.add(MI_OpenAsSerializable);
	
	    MI_OpenAsPocket.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_OpenAsPocket.setText("Open as Pocket ...");
	    MI_OpenAsPocket.setToolTipText("Pocket hold TypePackage");
	    MI_OpenAsPocket.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_LoadActionPerformed(evt);
	        }
	    });
	    MM_File.add(MI_OpenAsPocket);
	    MM_File.add(S_MMFile2);
	
	    MI_ImportAsText.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_ImportAsText.setMnemonic('I');
	    MI_ImportAsText.setText("Import as Text ...");
	    MI_ImportAsText.setToolTipText("Import parser types from a parser definition file");
	    MI_ImportAsText.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_LoadActionPerformed(evt);
	        }
	    });
	    MM_File.add(MI_ImportAsText);
	    MM_File.add(jSeparator1);
	
	    Save.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    Save.setMnemonic('S');
	    Save.setText("Save");
	    Save.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            SaveActionPerformed(evt);
	        }
	    });
	    MM_File.add(Save);
	
	    MI_SaveAsText.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_SaveAsText.setMnemonic('T');
	    MI_SaveAsText.setText("Save as Text ...");
	    MI_SaveAsText.setToolTipText("Save this type package to a parser type definition file");
	    MI_SaveAsText.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            SaveAsTextActionPerformed(evt);
	        }
	    });
	    MM_File.add(MI_SaveAsText);
	
	    MI_SaveAsPocket.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_SaveAsPocket.setText("Save as Pocket ...");
	    MI_SaveAsPocket.setToolTipText("");
	    MI_SaveAsPocket.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            SaveAsPocketActionPerformed(evt);
	        }
	    });
	    MM_File.add(MI_SaveAsPocket);
	
	    SaveAsSerializable.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    SaveAsSerializable.setMnemonic('a');
	    SaveAsSerializable.setText("Save as Serializable ...        ");
	    SaveAsSerializable.setToolTipText("Save this type package");
	    SaveAsSerializable.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            SaveAsSerializableActionPerformed(evt);
	        }
	    });
	    MM_File.add(SaveAsSerializable);
	    MM_File.add(S_MM_File3);
	
	    MI_Quit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_Quit.setMnemonic('Q');
	    MI_Quit.setText("Quit");
	    MI_Quit.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_QuitActionPerformed(evt);
	        }
	    });
	    MM_File.add(MI_Quit);
	
	    MB_MainMenu.add(MM_File);
	
	    MM_Edit.setMnemonic('E');
	    MM_Edit.setText("Edit");
	
	    MI_NewType.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_NewType.setMnemonic('N');
	    MI_NewType.setText("New Type       ");
	    MI_NewType.setToolTipText("Create a new parser type");
	    MI_NewType.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_NewTypeActionPerformed(evt);
	        }
	    });
	    MM_Edit.add(MI_NewType);
	
	    MI_DeleteType.setMnemonic('D');
	    MI_DeleteType.setText("Delete Type    ");
	    MI_DeleteType.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_DeleteTypeActionPerformed(evt);
	        }
	    });
	    MM_Edit.add(MI_DeleteType);
	
	    MB_MainMenu.add(MM_Edit);
	
	    MM_CodeLab.setMnemonic('L');
	    MM_CodeLab.setText("CodeLab");
	    MM_CodeLab.setToolTipText("An experimenting code lab. You can use it to test any code before using it.");
	
	    MI_CodeLab.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_CodeLab.setMnemonic('L');
	    MI_CodeLab.setText("New CodeLab    ");
	    MI_CodeLab.setToolTipText("Unnamed CodeLab will not be saved with TypePackage");
	    MI_CodeLab.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_CodeLabActionPerformed(evt);
	        }
	    });
	    MM_CodeLab.add(MI_CodeLab);
	
	    MI_NewNamedCodeLab.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_NewNamedCodeLab.setMnemonic('N');
	    MI_NewNamedCodeLab.setText("New Named CodeLab    ");
	    MI_NewNamedCodeLab.setToolTipText("Named CodeLab can be saved with TypePackage.\\n However, only TypeFactory can access and use codelabs.");
	    MI_NewNamedCodeLab.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_NewNamedCodeLabActionPerformed(evt);
	        }
	    });
	    MM_CodeLab.add(MI_NewNamedCodeLab);
	
	    MI_NewNamedText.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_NewNamedText.setText("New Text Data");
	    MI_NewNamedText.setToolTipText("Arbitary text data (may be used for testing)");
	    MI_NewNamedText.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_NewNamedCodeLabActionPerformed(evt);
	        }
	    });
	    MM_CodeLab.add(MI_NewNamedText);
	    MM_CodeLab.add(S_CodeLab);
	
	    MB_MainMenu.add(MM_CodeLab);
	
	    MM_ClassPaths.setMnemonic('C');
	    MM_ClassPaths.setText("ClassPaths");
	    MM_ClassPaths.setToolTipText("Add class path for main scope, type verification and compiler code.");
	
	    MI_AddURL.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_AddURL.setMnemonic('U');
	    MI_AddURL.setText("Add URL    ");
	    MI_AddURL.setToolTipText("Add a URL of class path");
	    MI_AddURL.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_AddURLActionPerformed(evt);
	        }
	    });
	    MM_ClassPaths.add(MI_AddURL);
	
	    MI_AddJarFile.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_AddJarFile.setMnemonic('J');
	    MI_AddJarFile.setText("Add Jar file    ");
	    MI_AddJarFile.setToolTipText("Add a jar file");
	    MI_AddJarFile.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_AddJarFileActionPerformed(evt);
	        }
	    });
	    MM_ClassPaths.add(MI_AddJarFile);
	    MM_ClassPaths.add(S_ClassPaths);
	
	    MB_MainMenu.add(MM_ClassPaths);
	
	    MM_ErrorMessage.setMnemonic('E');
	    MM_ErrorMessage.setText("ErrorMessage");
	    MM_ErrorMessage.setToolTipText("Error message used with RegParser");
	
	    MI_NewWarning.setMnemonic('W');
	    MI_NewWarning.setText("New Warning Message");
	    MI_NewWarning.setToolTipText("Warnings are non-serious problems");
	    MI_NewWarning.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_NewErrorActionPerformed(evt);
	        }
	    });
	    MM_ErrorMessage.add(MI_NewWarning);
	
	    MI_NewErrorMessage.setMnemonic('E');
	    MI_NewErrorMessage.setText("New Error Message");
	    MI_NewErrorMessage.setToolTipText("Errors are serious problem but the compilation can still continue.");
	    MI_NewErrorMessage.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_NewErrorActionPerformed(evt);
	        }
	    });
	    MM_ErrorMessage.add(MI_NewErrorMessage);
	
	    MI_NewFatalError.setMnemonic('F');
	    MI_NewFatalError.setText("New Fatal Error Message");
	    MI_NewFatalError.setToolTipText("Fatal Errors are error that will cause the compiling to stop immediately");
	    MI_NewFatalError.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_NewErrorActionPerformed(evt);
	        }
	    });
	    MM_ErrorMessage.add(MI_NewFatalError);
	    MM_ErrorMessage.add(S_ErrorMessage);
	
	    MB_MainMenu.add(MM_ErrorMessage);
	
	    MM_Setting.setMnemonic('S');
	    MM_Setting.setText("Setting");
	
	    MM_MainScope.setMnemonic('M');
	    MM_MainScope.setText("Main Scope");
	    MM_MainScope.setToolTipText("Abitary data given to this TypePackage and can be used by parser types of this type package");
	
	    MI_MainScope.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MI_MainScope.setMnemonic('E');
	    MI_MainScope.setText("Edit    ");
	    MI_MainScope.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_MainScopeActionPerformed(evt);
	        }
	    });
	    MM_MainScope.add(MI_MainScope);
	
	    MI_ClearMainScope.setMnemonic('C');
	    MI_ClearMainScope.setText("Clear    ");
	    MI_ClearMainScope.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_ClearMainScopeActionPerformed(evt);
	        }
	    });
	    MM_MainScope.add(MI_ClearMainScope);
	
	    MICB_MainScopeSave.setMnemonic('S');
	    MICB_MainScopeSave.setSelected(true);
	    MICB_MainScopeSave.setText("Save with TypePackage");
	    MM_MainScope.add(MICB_MainScopeSave);
	
	    MM_Setting.add(MM_MainScope);
	
	    MM_CContext.setMnemonic('C');
	    MM_CContext.setText("Compilation Context");
	
	    MI_CContextEdit.setMnemonic('E');
	    MI_CContextEdit.setText("Edit");
	    MI_CContextEdit.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_CContextEditActionPerformed(evt);
	        }
	    });
	    MM_CContext.add(MI_CContextEdit);
	
	    MI_CContextClear.setMnemonic('C');
	    MI_CContextClear.setText("Clear    ");
	    MI_CContextClear.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_CContextClearActionPerformed(evt);
	        }
	    });
	    MM_CContext.add(MI_CContextClear);
	
	    MICB_CContextSave.setMnemonic('S');
	    MICB_CContextSave.setSelected(true);
	    MICB_CContextSave.setText("Save with TypePackage");
	    MM_CContext.add(MICB_CContextSave);
	
	    MM_Setting.add(MM_CContext);
	    MM_Setting.add(S_Setting);
	
	    MICB_Freeze.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK));
	    MICB_Freeze.setMnemonic('F');
	    MICB_Freeze.setSelected(true);
	    MICB_Freeze.setText("Freeze the Saved TypePackage    ");
	    MICB_Freeze.setToolTipText("Freeze the current type package so it cannot be modified.");
	    MM_Setting.add(MICB_Freeze);
	
	    MB_MainMenu.add(MM_Setting);
	
	    MM_Help.setMnemonic('H');
	    MM_Help.setText("Help");
	
	    MI_Content.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
	    MI_Content.setMnemonic('C');
	    MI_Content.setText("Help Contents          ");
	    MI_Content.setToolTipText("Help (Under unstruction)");
	    MI_Content.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_ContentActionPerformed(evt);
	        }
	    });
	    MM_Help.add(MI_Content);
	    MM_Help.add(S_MM_Help);
	
	    MI_About.setMnemonic('A');
	    MI_About.setText("About");
	    MI_About.setToolTipText("Information about this program");
	    MI_About.addActionListener(new java.awt.event.ActionListener() {
	        public void actionPerformed(java.awt.event.ActionEvent evt) {
	            MI_AboutActionPerformed(evt);
	        }
	    });
	    MM_Help.add(MI_About);
	
	    MB_MainMenu.add(MM_Help);
	
	    setJMenuBar(MB_MainMenu);
	
	    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
	    getContentPane().setLayout(layout);
	    layout.setHorizontalGroup(
	        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	        .addComponent(P_StatusBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
	        .addComponent(P_Whole, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
	    );
	    layout.setVerticalGroup(
	        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
	        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
	            .addComponent(P_Whole, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
	            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
	            .addComponent(P_StatusBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
	    );
	
	    pack();
    }// </editor-fold>//GEN-END:initComponents

	private void B_NewParserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_B_NewParserActionPerformed
		// Create the RegParser
		try {
			String RPStr = this.LNP_Parser.getTextComponent().getText();
			RegParser RP = RegParser.compile(RPStr);

			// Create a new parse tab
			// Search for a repeat
			for (int i = this.PT_Tabs.getTabCount(); --i >= 0;) {
				Object O = this.PT_Tabs.getComponentAt(i);
				if (!(O instanceof PFParseResult))
					continue;
				PFParseResult PRP = (PFParseResult) O;
				if (!PRP.getParserStr().equals(RPStr))
					continue;

				// Focus the already exist Tab
				this.PT_Tabs.setSelectedIndex(i);
				return;
			}

			// No repeat
			RegParser Parser = null;
			if (RPStr.equals("")) {
				this.setStatusBarText("Please enter a valid RegParser!!!");
				return;
			} else {
				// Parse the RegParser
				try {
					Parser = RegParser.compile(RPStr);
					if (Parser == null) {
						this.setStatusBarText("Please enter a valid RegParser!!!");
						return;
					}
				} catch (Exception E) {
					this.setStatusBarText("There is an error parsing a RegParser: " + E.toString());
					return;
				}
			}

			int TabIndex = this.PT_Tabs.getTabCount();
			String TabTitle = "Parse Result " + this.ParserResultCount++;
			this.PT_Tabs.addTab(TabTitle, new PFParseResult(this, RPStr, RP));
			this.PT_Tabs.setSelectedIndex(TabIndex);
			this.PT_Tabs.setTabComponentAt(TabIndex, new ClosableTabTitle(TabTitle, PT_Tabs));
			this.setStatusBarText("Enter the input and press \"Parse\"");

		} catch (Exception E) {
			this.setStatusBarText("ERROR!!! Invalide RegParser! "
					+ E.toString());
		}
	}//GEN-LAST:event_B_NewParserActionPerformed

	private void L_TypesValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_L_TypesValueChanged
		// Disable or enable delete type button.
		this.B_DeleteType.setEnabled((this.L_Types.getSelectedValuesList() != null));
	}//GEN-LAST:event_L_TypesValueChanged

	@SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean addNewType(String pTName, String pTypeDef) {
		if (this.TPackage.type(pTName) != null)
			return false;

		// Add an entry on the list
		DefaultListModel LModel = ((DefaultListModel) this.L_Types.getModel());
		// Add the new one
		boolean IsSet = false;
		for (int i = 0; i < LModel.getSize(); i++) {
			String TypeName = LModel.getElementAt(i).toString();
			if (TypeName.compareTo(pTName) > 0) {
				// Delete from the list
				LModel.insertElementAt(pTName, i);
				this.L_Types.setSelectedIndex(i);
				IsSet = true;
				break;
			}
		}
		if (!IsSet)
			LModel.addElement(pTName);

		// Create and add the type.
		try {
			String TName = null;
			try{
				this.TPackage.IsFrozen = false;
				TName = this.TPackage.addType(pTypeDef);
			} finally {
				this.TPackage.IsFrozen = true;
			}
			if(TName != null) return true;
		} catch (Exception E) {
			this.setStatusBarText("ERROR: " + E.toString());
		}
		return false;
	}

	private void updateTitle() {
		String FileName = (this.ThisFile == null)?null:this.ThisFile.getName();
		this.setTitle(ProgramName
				+ ((FileName == null) ? "" : " - " + FileName)
				+ (this.HaveBeenEdited ? "*" : ""));
	}

	/** Rename type in the type list - This one will be called after the type has renamed in TPackage so no need to update there */
	@SuppressWarnings({ "unchecked", "rawtypes" })
    void updateType(String pOldName, String pNewName) {
		this.HaveBeenEdited = true;
		this.updateTitle();

		if (pOldName.equals(pNewName))
			return;
		DefaultListModel LModel = ((DefaultListModel) this.L_Types.getModel());

		// Remove the old one
		for (int i = 0; i < LModel.getSize(); i++) {
			String TypeName = LModel.getElementAt(i).toString();
			if (TypeName.equals(pOldName)) {
				// Delete from the list
				LModel.remove(i);
				break;
			}
		}

		// Add the new one
		boolean IsSet = false;
		for (int i = 0; i < LModel.getSize(); i++) {
			String TypeName = LModel.getElementAt(i).toString();
			if (TypeName.compareTo(pNewName) > 0) {
				// Delete from the list
				LModel.insertElementAt(pNewName, i);
				IsSet = true;
				break;
			}
		}
		if (!IsSet)
			LModel.addElement(pNewName);

		// Update Type Tab name
		for (int i = 0; i < this.PT_Tabs.getTabCount(); i++) {
			Object O2 = this.PT_Tabs.getTabComponentAt(i);
			if (!(O2 instanceof ClosableTabTitle))
				continue;
			ClosableTabTitle CTT = (ClosableTabTitle) O2;
			if (pOldName.equals(((JLabel) CTT.getComponent(0)).getText())) {
				// Found the Tab
				((JLabel) CTT.getComponent(0)).setText(pNewName);
				return;
			}
		}
	}

	@SuppressWarnings("rawtypes")
    private void B_NewTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_B_NewTypeActionPerformed
		// Create a new type
		this.setStatusBarText("Adding a new type ...");

		String TName = JOptionPane.showInputDialog("Enter a new type name");
		if ((TName == null) || (TName.length() == 0))
			return;

		String DefaultTypeDef = "#def Simple parser " + TName + ":\n"
				+ "#Checker:\n" + "	.*\n" + "#end def parser;";

		if (!this.addNewType(TName, DefaultTypeDef)) {
			this.HaveBeenEdited = true;
			this.updateTitle();

			DefaultListModel DLM = (DefaultListModel) this.L_Types.getModel();
			for (int i = DLM.getSize(); --i >= 0;) {
				String S = (String) DLM.getElementAt(i);
				if (!TName.equals(S))
					continue;

				// Found it		
				this.L_Types.setSelectedIndex(i);
				this.ensureTypeInfoDisplay(TName);
			}
			JOptionPane.showMessageDialog(this, "The type named '" + TName
					+ "' is already exist.");
			return;
		}

		// Add success
		this.ensureTypeInfoDisplay(TName);
		this.setStatusBarText("The type '" + TName + "' is added.");
	}//GEN-LAST:event_B_NewTypeActionPerformed

	@SuppressWarnings("rawtypes")
    private void B_DeleteTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_B_DeleteTypeActionPerformed
		if (this.L_Types.getSelectedValuesList() != null) {
			int[] Is = this.L_Types.getSelectedIndices();
			for (int I : Is) {
				String TypeName = this.L_Types.getModel().getElementAt(I).toString();
				// Delete from the provider
				this.HaveBeenEdited = true;
				this.updateTitle();

				try{
					this.TPackage.IsFrozen = false;
					this.TPackage.removeType(TypeName);
					JOptionPane.showConfirmDialog(this,
						"Are you sure, you want to delete the type `"+TypeName+"`?",
						"Type deleting confirmation",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE
					);
				} finally {
					this.TPackage.IsFrozen = true;
				}
				

				// Delete from the list
				((DefaultListModel) this.L_Types.getModel()).remove(I);

				// Delete form the tab
				for (int i = 0; i < this.PT_Tabs.getTabCount(); i++) {
					Object O2 = this.PT_Tabs.getTabComponentAt(i);
					if (!(O2 instanceof ClosableTabTitle))
						continue;
					ClosableTabTitle CTT = (ClosableTabTitle) O2;
					if (TypeName.equals(((JLabel) CTT.getComponent(0)).getText())) {
						// Found the Tab
						this.PT_Tabs.remove(i);
						this.B_DeleteType.setEnabled(false);
						return;
					}
				}
			}
		}

		this.setStatusBarText("The type(s) is/are deleted.");
	}//GEN-LAST:event_B_DeleteTypeActionPerformed

	private void L_TypesKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_L_TypesKeyReleased
		if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER)
			TypesListMouseClicked();
	}//GEN-LAST:event_L_TypesKeyReleased

	private void MI_NewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_NewActionPerformed
		NewTypeFactory(this, null);
	}//GEN-LAST:event_MI_NewActionPerformed

	private void L_TypesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_L_TypesMouseClicked
		// Look for double click
		if (evt.getClickCount() >= 2)
			TypesListMouseClicked();
	}//GEN-LAST:event_L_TypesMouseClicked

	private void MI_QuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_QuitActionPerformed
		this.processWindowEvent(new WindowEvent(this,
				WindowEvent.WINDOW_CLOSING));
	}//GEN-LAST:event_MI_QuitActionPerformed

	static class TypeFileFilter extends javax.swing.filechooser.FileFilter {

		TypeFileFilter(String[] pExts, String pDesc) {
			this.Exts  = pExts;
			this.Desc = pDesc;
		}

		private String[] Exts = null;
		private String   Desc = null;

		@Override public boolean accept(File f) {
			if (f == null)
				return false;
			if (f.isDirectory())
				return true;
			if(this.Exts == null) return true;
			for(int i = this.Exts.length; --i >= 0; ) {
				String Ext = this.Exts[i];
				if(Ext == null) continue;
				if(f.toString().endsWith(Ext)) return true;
			}
			return false;
		}

		@Override public String getDescription() {
			return this.Desc;
		}

		public String getExtension(File f) {
			String ext = null;
			String s = f.getName();
			int i = s.lastIndexOf('.');

			if (i > 0 && i < s.length() - 1) {
				ext = s.substring(i + 1).toLowerCase();
			}
			return ext;
		}
	}

	private void SaveAsTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveAsTextActionPerformed
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if(This.TPTFileChooser == null) This.TPTFileChooser = new JFileChooser(This.WorkingDir);
				else                            This.TPTFileChooser.setCurrentDirectory(This.WorkingDir);

				// Show open dialog; this method does not return until the dialog is closed
				This.TPTFileChooser.addChoosableFileFilter(new TypeFileFilter(TypeTextFileExt, TypeTextFileDesc));
				if (This.TPTFileChooser.showSaveDialog(This) == JFileChooser.CANCEL_OPTION) return;
				File TheFile = This.TPTFileChooser.getSelectedFile();
				if (TheFile == null) return;

				if(((This.ThisFile == null) || !TheFile.toString().equals(This.ThisFile.toString())) && TheFile.exists()) {
					// Ask for overwrite
					int O = JOptionPane.showConfirmDialog(This, "The file '" + TheFile.toString()+ "' is already " +
							"exist.\n Are you sure you want to save over it?");
					if(O != JOptionPane.YES_OPTION) return;
				}
				
				if (!TheFile.toString().endsWith(TypeTextFileExt[0])) TheFile = new File(TheFile.toString() + TypeTextFileExt[0]);

				if (!TheFile.isFile() || !TheFile.exists() || !TheFile.canWrite()) {
					boolean IsFail = false;
					if (!TheFile.exists()) {
						try { TheFile.createNewFile(); }
						catch (Exception E) { IsFail = true; }
					} else IsFail = true;
					if (IsFail) {
						JOptionPane.showMessageDialog(This, "The selected file `" + TheFile.toString() + "` is not writable.");
						return;
					}
				}
				
				This.WorkingDir = TheFile.getParentFile().getAbsoluteFile();
				This.saveAsText(TheFile);
			}
		});
	}//GEN-LAST:event_SaveAsTextActionPerformed

	private void SaveAsSerializableActionPerformed(
			java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveAsSerializableActionPerformed
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (This.TPKFileChooser == null) This.TPKFileChooser = new JFileChooser(This.WorkingDir);
				else                             This.TPKFileChooser.setCurrentDirectory(This.WorkingDir);

				// Show open dialog; this method does not return until the dialog is closed
				This.TPKFileChooser.addChoosableFileFilter(new TypeFileFilter(TypeSerializableFileExt, TypeSerializableFileDesc));
				if (This.TPKFileChooser.showSaveDialog(This) == JFileChooser.CANCEL_OPTION) return;
				File TheFile = This.TPKFileChooser.getSelectedFile();
				if (TheFile == null) return;
				
				if(((This.ThisFile == null) || !TheFile.toString().equals(This.ThisFile.toString())) && TheFile.exists()) {
					// Ask for overwrite
					int O = JOptionPane.showConfirmDialog(This, "The file '" + TheFile.toString()+ "' is already " +
							"exist.\n Are you sure you want to save over it?");
					if(O != JOptionPane.YES_OPTION) return;
				}

				if (!TheFile.toString().endsWith(TypeSerializableFileExt[0])) TheFile = new File(TheFile.toString() + TypeSerializableFileExt[0]);

				if (!TheFile.isDirectory() || !TheFile.exists() || !TheFile.canWrite()) {
					boolean IsFail = false;
					if (!TheFile.exists()) { try { TheFile.createNewFile(); } catch (Exception E) { IsFail = true; } }
					else IsFail = true;
					if (IsFail) {
						JOptionPane.showMessageDialog(This,
								"The selected file `" + TheFile.toString() + "` is not writable.");
						return;
					}
				}

				This.WorkingDir = TheFile.getParentFile();
				This.saveAsSerializable(TheFile);
			}
		});
	}//GEN-LAST:event_SaveAsSerializableActionPerformed

	private void MI_NewTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_NewTypeActionPerformed
		this.B_NewType.doClick();
	}//GEN-LAST:event_MI_NewTypeActionPerformed

	private void MI_ContentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_ContentActionPerformed
		JOptionPane.showMessageDialog(this,
				"<html>This should contains helps on RegParser and TypePackage<br />"
						+ "but it is under construction at the moment.");
	}//GEN-LAST:event_MI_ContentActionPerformed

	private void MI_AboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_AboutActionPerformed
		JOptionPane.showMessageDialog(
						this,
						"<html><center>"
								+ "TypeFactory 1.0 (June 2008-2019)<br/>"
								+ "<br/>"
								+ "RegParser is a parser mechanism based on<br/>"
								+ "Regular Expression implemented in Java.<br/>"
								+ "<br/>"
								+ "TypePackage is an extension of RegParser<br/>"
								+ "taking care of the parser type management.<br />"
								+ "<br/>"
								+ "TypeFactory is a tool for creating <br/>"
								+ "and manipulating TypePackages.<br />"
								+ "<br/>"
								+ "<hr width='80%'>"
								+ "Copyright (c) 2008-2019: Nawapunth Manusitthipol&nbsp;&nbsp;&nbsp;&nbsp;<br/>"
								+ "<br/>"
								+ "<a href='http://nawa.pn-np.net/'>http://nawa.pn-np.net/</a><br/>"
								+ "<br/>" + "</center>",
						"About - TypeFactory 1.00",
						JOptionPane.INFORMATION_MESSAGE);
	}//GEN-LAST:event_MI_AboutActionPerformed

	private void MI_LoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_LoadActionPerformed

		char C = '0';
		if(      evt.getSource() == this.MI_Open)               C = 'O';
		else if( evt.getSource() == this.MI_OpenAsSerializable) C = 'S';
		else if (evt.getSource() == this.MI_OpenAsText)         C = 'T';
		else if (evt.getSource() == this.MI_OpenAsPocket)       C = 'P';
		else if (evt.getSource() == this.MI_ImportAsText) {
			C = 'I';
			if(!TypeFactory.IsImportAsTextDisplayed) {
				JOptionPane.showMessageDialog(this,
					"NOTE: Be careful importing as text.\n" +
					"\n" +
					"Importing as text means that a type-package definition text file will be open and all its elements \n" +
					"will be imported (appended) INTO this type-package. If an elements (e.g. Type) has already exist in \n" +
					"this type-package, the element will be rejected (not imported). This can results in the situation \n" +
					"when some types are added but some are rejected and the added type required the rejected ones in order to \n" +
					"work properly."
				);
				TypeFactory.IsImportAsTextDisplayed = true;
			}
		}
		if (C == '0') return;

		final char LoadKind = C;

		EventQueue.invokeLater(new Runnable() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
            public void run() {
				
				JFileChooser FC   = null;
				String[]     Ext  = null;
				String       Desc = null;
				
				switch(LoadKind) {
					case 'O': FC = This.TPFileChooser;  Ext = TypeFileExt;             Desc = TypeFileDesc; break;
					case 'S': FC = This.TPKFileChooser; Ext = TypeSerializableFileExt; Desc = TypeSerializableFileDesc; break;
					case 'P': FC = This.TPPFileChooser; Ext = TypePocketFileExt;       Desc = TypePocketFileDesc;       break;
					default:  FC = This.TPTFileChooser; Ext = TypeTextFileExt;         Desc = TypeTextFileDesc;
				}
				
				if(FC == null) {
					if     (LoadKind == 'O') FC = (This.TPFileChooser  = new JFileChooser(This.WorkingDir));
					else if(LoadKind == 'S') FC = (This.TPKFileChooser = new JFileChooser(This.WorkingDir));
					else if(LoadKind == 'P') FC = (This.TPPFileChooser = new JFileChooser(This.WorkingDir));
					else                     FC = (This.TPTFileChooser = new JFileChooser(This.WorkingDir));
				} else FC.setCurrentDirectory(This.WorkingDir);

				// Show open dialog; this method does not return until the dialog is closed
				FC.addChoosableFileFilter(new TypeFileFilter(Ext, Desc));
				if (FC.showOpenDialog(This) == JFileChooser.CANCEL_OPTION) return;
				File TheFile = FC.getSelectedFile();
				if (TheFile == null) return;

				if (!TheFile.isFile() || !TheFile.exists() || !TheFile.canRead()) {
					JOptionPane.showMessageDialog(This, "The selected file `" + TheFile.toString()
							+ "` does not exist or it is not readable.");
					return;
				}
				
				This.WorkingDir = TheFile.getParentFile().getAbsoluteFile();

				try {
					PTypePackage PTP = null;

					// Load the file
					if(LoadKind == 'O') {
						This.ThisFile = TheFile;
						if(     TheFile.toString().endsWith(TypeSerializableFileExt[0])) This.ThisFileAs = 'S';
						else if(TheFile.toString().endsWith(TypePocketFileExt[0]))       This.ThisFileAs = 'P';
						else if(TheFile.toString().endsWith(TypeTextFileExt[0]))         This.ThisFileAs = 'T';
						else JOptionPane.showMessageDialog(This, "Unknown file type `" + TheFile.toString() + "` .");
						
						switch (This.ThisFileAs) {
							case 'S': PTP = PTypePackage.loadAsSerializableFromFile(This.ThisFile); break;
							case 'P': PTP = PTypePackage.loadAsPocketFromFile(      This.ThisFile); break;
							case 'T': PTP = PTypePackage.loadAsTextFromFile(        This.ThisFile); break;
						}
					}
					else if (LoadKind == 'P') PTP = PTypePackage.loadAsPocketFromFile(TheFile);
					else if (LoadKind == 'S') PTP = PTypePackage.loadAsSerializableFromFile(TheFile);
					// Text (open and import)
					else PTP = PTypePackage.loadAsTextFromFile(TheFile);

					// In case of import, add PTP in to the current package
					if (LoadKind == 'I') {
						PTypePackage Old = (LoadKind == 'I') ? This.TPackage :new PTypePackage();

						HashSet<String> Success = new HashSet<String>();
						HashSet<String> Fail    = new HashSet<String>();

						// Append classpaths
						if(PTP.ClassPaths != null) {
							JavaEngine JE = (JavaEngine)ScriptManager.Instance.getDefaultEngineOf(JavaEngine.class.getCanonicalName());
							for(String CP : PTP.ClassPaths) {
								CP = CP.trim();
								if((Old.ClassPaths != null) && (Old.ClassPaths.contains(CP))) continue;
								JE.addClasspathURL(CP);
								try {
									Old.IsFrozen = false;
									Old.addClassPath(CP);
								} finally { Old.IsFrozen = true; }
							}
						}
						
						// Append Type Kinds
						if (PTP.KDatas != null) {
							// Add each TypeKind
							for (String KName : PTP.KDatas.keySet()) {
								if (Old.getTypeKind(KName) != null) {
									Fail.add("TypeKind: " + KName + " (already exist)");
									continue;
								}

								// If it does not exist, add
								try {
									PTKind TK = PTP.getTypeKind(KName);
									String KN = null;
									try {
										This.setStatusBarText("Importing " + KN + " ...");
										Old.IsFrozen = false;
										KN = Old.addKind(TK.toString());
									} finally { Old.IsFrozen = true; }
									if (KN != null) Success.add("TypeKind: " + KName);
									else            Fail.add("TypeKind: " + KName + " (Unknown error)");
								} catch (Exception E) { Fail.add("TypeKind: " + KName + " (" + E.toString() + ")"); }
							}
						}

						Set<String> TNames = PTP.typeNames();
						if(TNames != null) {
							// Add Types
							for (String TName : TNames) {
								if (Old.type(TName) != null) {
									Fail.add("Type: " + TName + " (already exist)");
									continue;
								}

								// If it does not exist, add
								try {
									String Def = PTP.getTypeToString(TName);
									String TN = null;
									try {
										This.setStatusBarText("Importing " + TN + " ...");
										Old.IsFrozen = false;
										TN = Old.addType(Def);
									} finally { Old.IsFrozen = true; }
									if (TN != null) {
										boolean IsSet = false;
										DefaultListModel LModel = (DefaultListModel) This.L_Types.getModel();
										for (int i = 0; i < LModel.getSize(); i++) {
											String TypeName = LModel.getElementAt(i).toString();
											if (TypeName.compareTo(TName) > 0) {
												// Delete from the list
												LModel.insertElementAt(TName, i);
												This.L_Types.setSelectedIndex(i);
												IsSet = true;
												break;
											}
										}
										if (!IsSet) LModel.addElement(TName);
										Success.add("Type: " + TName);
									} else
										Fail.add("Type: " + TName
												+ " (Unknown error)");
								} catch (Exception E) { Fail.add("Type: " + TName + " (" + E.toString() + ")"); }
							}
						}

						// Appends error message
						if(PTP.ErrorMsgs != null) {
							try {
								Old.IsFrozen = false;
								for(String E : PTP.ErrorMsgs.keySet()) {
									if((Old.ErrorMsgs != null) && (Old.ErrorMsgs.containsKey(E))) {
										Fail.add("Error Message: " + E);
										continue;
									}
									Old.addErrorMessage(E, PTP.ErrorMsgs.get(E));
									Success.add("Error Message: " + E);
								}
							} finally { Old.IsFrozen = true; }
						}

						// Appends moredata
						if(PTP.MoreDatas != null) {
							try {
								Old.IsFrozen = false;
								for(String D : PTP.MoreDatas.keySet()) {
									if((Old.MoreDatas != null) && (Old.MoreDatas.containsKey(D))) {
										if(D.startsWith(CodeLabNamePrefix))       Fail.add("CodeLab: "  + D);
										else if(D.startsWith(TextDataNamePrefix)) Fail.add("TextData: " + D);
										else if(D.equals("MainScope")) continue; // MainScope will be done later
										else if(D.equals("CContext"))  continue; // CContext will be done later
										else Fail.add("Data: " + D);
										continue;
									}
									Old.addData(D, PTP.MoreDatas.get(D));									
									
									if(D.startsWith(CodeLabNamePrefix))  Success.add("CodeLab: " + D);
									if(D.startsWith(TextDataNamePrefix)) Success.add("TextData: " + D);
								}
							} finally { Old.IsFrozen = true; }
							
							Object Data = This.TPackage.getData("MainScope");
							if(Data instanceof String) {
								String MSCode = (String)Data;
								if(This.MainScopeCode != null) Fail.add("MainScope Code");
								else {
									This.MainScopeCode = MSCode;
									This.MICB_MainScopeSave.setSelected(true);
									Success.add("MainScope Code");
								}
							} else if(Data != null) Fail.add("MainScope Code");
							
							Data = This.TPackage.getData("CContext");
							if(Data instanceof String) {
								String CCCode = (String)Data;
								if(This.CContextCode != null) Fail.add("Compilation Context Code");
								else {
									This.CContextCode = CCCode;
									This.MICB_CContextSave.setSelected(true);
									Success.add("Compilation Context Code");
								}
							} else if(Data != null) Fail.add("Compilation Context Code");
						}

						StringBuffer SB = new StringBuffer();
						if ((Success.size() == 0) && (Fail.size() != 0))
							SB.append("No types or kinds have been successfully added.");
						else if ((Success.size() == 0) && (Fail.size() == 0))
							SB.append("No types or kinds definition found in the file.");
						else {
							SB.append("The following kinds/types has successfully been added.\n");
							for (String S : Success) SB.append("    ").append(S).append("\n");
						}

						if (Fail.size() != 0) {
							SB.append("The following kinds/types were fail to add.\n");
							for (String F : Fail) SB.append("    ").append(F).append("\n");
						}

						This.HaveBeenEdited = true;
						This.updateTitle();
						This.ensureUIAndModelSynchronized();
						
						JOptionPane.showMessageDialog(This, SB.toString());
					} else {
						// Create a new Windows that use the newly loaded package
						TypeFactory TF = NewTypeFactory(This, PTP);
						TF.HaveBeenEdited = false;
						TF.ThisFile       = TheFile;
						TF.ThisFileAs     = LoadKind;
						TF.updateTitle();

						Set<String> TNames = This.TPackage.typeNames();
						if (!This.HaveBeenEdited
								&& ((TNames == null) || (TNames.size() == 0))
								&& ((This.TPackage.KDatas == null)
										|| (This.TPackage.KDatas.size() == PTypePackage.CommonKindCount))) {
							// Close this one if the current Frame is completely empty
							This.processWindowEvent(new WindowEvent(This,
									WindowEvent.WINDOW_CLOSING));
						}
					}

					This.setStatusBarText("The kind(s) and/or type(s) have been loaded.");
				} catch (Exception E) {
					ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
					PrintStream           PS   = new PrintStream(BAOS);
					E.printStackTrace(PS);
					JOptionPane.showMessageDialog(This,
							"There is a problem reading the selected file `" + TheFile.toString() + "`" + ":\n"
									+ BAOS.toString() + ".");
				}
			}
		});
	}//GEN-LAST:event_MI_LoadActionPerformed

	private void MI_MainScopeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_MainScopeActionPerformed
		// Check if there is already a tab with "Main Scope" as name
		// First create a new tab
		for (int i = 0; i < this.PT_Tabs.getTabCount(); i++) {
			Object O2 = this.PT_Tabs.getTabComponentAt(i);
			if (!(O2 instanceof ClosableTabTitle)) continue;
			ClosableTabTitle CTT = (ClosableTabTitle) O2;
			if ("Main Scope".equals(((JLabel) CTT.getComponent(0)).getText())) {
				// Found the Tab
				this.PT_Tabs.setSelectedIndex(i);
				return;
			}
		}

		final TypeFactory TF = this;

		ExecCallBack CB = new ExecCallBack() {
			public void setResult(String pCode, Object pResult) {
				TF.MainScopeCode      = pCode;
				TF.TPackage.MainScope = Scope.Simple.getDuplicateOf((Scope) pResult);
			}
		};

		if(this.MainScopeCode == null)
			this.MainScopeCode = "// @Java:\n//import net.nawaman.script.*;\n//return new Scope.Simple();\nreturn null;";
			
		// Otherwise, create one
		// Create a new one
		int TabIndex = this.PT_Tabs.getTabCount();
		ClosableTabTitle CTT = new ClosableTabTitle("Main Scope", this.PT_Tabs);
		this.PT_Tabs.addTab("Main Scope", new PFExecute(null, CB, "MainScope", Scope.Simple.class, this.MainScopeCode, true, CTT.Title));
		this.PT_Tabs.setSelectedIndex(TabIndex);
		this.PT_Tabs.setTabComponentAt(TabIndex, CTT);
	}//GEN-LAST:event_MI_MainScopeActionPerformed

	private void MI_ClearMainScopeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_ClearMainScopeActionPerformed
		this.MainScopeCode      = null;
		this.TPackage.MainScope = null;

		// Check if there is already a tab with "Main Scope" as name
		// First create a new tab
		for (int i = 0; i < this.PT_Tabs.getTabCount(); i++) {
			Object O2 = this.PT_Tabs.getTabComponentAt(i);
			if (!(O2 instanceof ClosableTabTitle))
				continue;
			ClosableTabTitle CTT = (ClosableTabTitle) O2;
			if ("Main Scope".equals(((JLabel) CTT.getComponent(0)).getText())) {
				// Found the Tab
				this.PT_Tabs.remove(i);
				this.MI_MainScopeActionPerformed(evt);
				break;
			}
		}
	}//GEN-LAST:event_MI_ClearMainScopeActionPerformed

	private void MI_AddURLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_AddURLActionPerformed
		String Result = JOptionPane.showInputDialog(this,
				"Enter a URL to be imported", "URL to import",
				JOptionPane.OK_CANCEL_OPTION);
		if ((Result == null) || (Result.length() == 0))
			return;
		try {
			URL Url = new URL(Result);
			String UrlStr = "URL: " + Url.toString();

			// Check if the URL is already added.
			for (int i = MM_ClassPaths.getItemCount(); --i >= 0;) {
				JMenuItem MI = MM_ClassPaths.getItem(i);
				if (MI == null)
					continue;
				if (!UrlStr.equals(MI.getText()))
					continue;
				return;
			}

			JavaEngine JE = (JavaEngine) ScriptManager.Instance.getDefaultEngineOf(JavaEngine.class.getCanonicalName());
			JE.addClasspathURL(Url.toString());
			try {
				This.TPackage.IsFrozen = false;
				This.TPackage.addClassPath(Url.toString());
			} finally { This.TPackage.IsFrozen = true; }
			this.addNewClassPathMenuItem(UrlStr, true);
		} catch (MalformedURLException E) {
			JOptionPane.showMessageDialog(this, "The given URL is mal-formed ("+ Result + ").");
		}
	}//GEN-LAST:event_MI_AddURLActionPerformed

	static class JarFileFilter extends javax.swing.filechooser.FileFilter {

		@Override
		public boolean accept(File f) {
			if (f == null)
				return false;
			if (f.isDirectory())
				return true;
			return f.toString().toLowerCase().endsWith(".jar");
		}

		@Override
		public String getDescription() {
			return "Jar file (*.jar)";
		}

		public String getExtension(File f) {
			String ext = null;
			String s = f.getName();
			int i = s.lastIndexOf('.');

			if (i > 0 && i < s.length() - 1) {
				ext = s.substring(i + 1).toLowerCase();
			}
			return ext;
		}
	}

	private void MI_AddJarFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_AddJarFileActionPerformed
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (This.JarFileChooser == null) This.JarFileChooser = new JFileChooser(This.WorkingDir);
				else                             This.JarFileChooser.setCurrentDirectory(This.WorkingDir);

				// Show open dialog; this method does not return until the dialog is closed
				This.JarFileChooser.addChoosableFileFilter(new JarFileFilter());
				if (This.JarFileChooser.showOpenDialog(This) == JFileChooser.CANCEL_OPTION) return;
				File TheFile = This.JarFileChooser.getSelectedFile();
				if (TheFile == null) return;

				if (!TheFile.isFile() || !TheFile.exists() || !TheFile.canRead()) {
					JOptionPane.showMessageDialog(This, "The selected jar file `"
									+ TheFile.toString()
									+ "` does not exist or it is not readable.");
					return;
				}

				This.WorkingDir = TheFile.getParentFile().getAbsoluteFile();
				String PathStr  = "JAR: " + TheFile.toString();

				// Check if the URL is already added.
				for (int i = MM_ClassPaths.getItemCount(); --i >= 0;) {
					JMenuItem MI = MM_ClassPaths.getItem(i);
					if (MI == null)
						continue;
					if (!PathStr.equals(MI.getText()))
						continue;
					return;
				}

				JavaEngine JE = (JavaEngine) ScriptManager.Instance.getDefaultEngineOf(JavaEngine.class.getCanonicalName());
				JE.addJarFile(TheFile.toString());
				try {
					This.TPackage.IsFrozen = false;
					This.TPackage.addClassPath(TheFile.toString());
				} finally { This.TPackage.IsFrozen = true; }
				This.addNewClassPathMenuItem(PathStr, true);
			}
		});
	}//GEN-LAST:event_MI_AddJarFileActionPerformed

	private void SaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveActionPerformed
		// Check if this type package has been associated with a file
		if ((this.ThisFile == null) || this.ThisFile.isDirectory() || !this.ThisFile.canRead()) {
			this.SaveAsPocketActionPerformed(evt);
			return;
		}

		if(this.ThisFileAs == 'O') {
			if(     ThisFile.toString().endsWith(TypeSerializableFileExt[0])) This.ThisFileAs = 'S';
			else if(ThisFile.toString().endsWith(TypePocketFileExt[0]))       This.ThisFileAs = 'P';
			else if(ThisFile.toString().endsWith(TypeTextFileExt[0]))         This.ThisFileAs = 'T';
		}
		// Save the file as it is been saving
		switch(this.ThisFileAs) {
			case 'S': this.saveAsSerializable(this.ThisFile); break;
			case 'T': this.saveAsText(this.ThisFile); break;
			case 'P': this.saveAsPocket(this.ThisFile); break;
		}
	}//GEN-LAST:event_SaveActionPerformed

	private void MI_CodeLabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_CodeLabActionPerformed
		// Create a new one
		int TabIndex = this.PT_Tabs.getTabCount();
		String TabName = "Lab" + (++this.CodeLabCount);
		ClosableTabTitle CTT = new ClosableTabTitle(TabName, this.PT_Tabs);
		this.PT_Tabs.addTab(TabName,
				new PFExecute(this, null, TabName, null,
					"// @Java:\nSystem.out.println(\"Hello World!!!\");\nSystem.err.println(\"Hello World!!!\");\nreturn null;",
					true, null));
		this.PT_Tabs.setSelectedIndex(TabIndex);
		this.PT_Tabs.setTabComponentAt(TabIndex, CTT);
	}//GEN-LAST:event_MI_CodeLabActionPerformed

	private void MI_CContextEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_CContextEditActionPerformed
		// Check if there is already a tab with "C-Context" as name
		// First create a new tab
		for (int i = 0; i < this.PT_Tabs.getTabCount(); i++) {
			Object O2 = this.PT_Tabs.getTabComponentAt(i);
			if (!(O2 instanceof ClosableTabTitle))
				continue;
			ClosableTabTitle CTT = (ClosableTabTitle) O2;
			if ("C-Context".equals(((JLabel) CTT.getComponent(0)).getText())) {
				// Found the Tab
				this.PT_Tabs.setSelectedIndex(i);
				return;
			}
		}

		final TypeFactory TF = this;

		ExecCallBack CB = new ExecCallBack() {
			public void setResult(String pCode, Object pResult) {
				TF.CContextCode = pCode;
				TF.CContext     = ((CompilationContext) pResult);
			}
		};

		if(this.CContextCode == null)
			this.CContextCode = "// @Java:\nimport net.nawaman.regparser.*;\nreturn new CompilationContext.Simple();";
			
		// Otherwise, create one
		// Create a new one
		int TabIndex = this.PT_Tabs.getTabCount();
		ClosableTabTitle CTT = new ClosableTabTitle("C-Context", this.PT_Tabs);
		this.PT_Tabs.addTab("C-Context", new PFExecute(this, CB, "C-Context", CompilationContext.class, this.CContextCode, true, CTT.Title));
		this.PT_Tabs.setSelectedIndex(TabIndex);
		this.PT_Tabs.setTabComponentAt(TabIndex, CTT);
	}//GEN-LAST:event_MI_CContextEditActionPerformed

	private void MI_CContextClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_CContextClearActionPerformed
		this.CContext     = null;
		this.CContextCode = null;

		// Check if there is already a tab with "Main Scope" as name
		// First create a new tab
		for (int i = 0; i < this.PT_Tabs.getTabCount(); i++) {
			Object O2 = this.PT_Tabs.getTabComponentAt(i);
			if (!(O2 instanceof ClosableTabTitle)) continue;
			ClosableTabTitle CTT = (ClosableTabTitle) O2;
			if ("C-Context".equals(((JLabel) CTT.getComponent(0)).getText())) {
				// Found the Tab
				this.PT_Tabs.remove(i);
				this.MI_MainScopeActionPerformed(evt);
				break;
			}
		}
	}//GEN-LAST:event_MI_CContextClearActionPerformed

	private void MI_DeleteTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_DeleteTypeActionPerformed
		this.B_DeleteType.doClick();
	}//GEN-LAST:event_MI_DeleteTypeActionPerformed

	private void MI_NewNamedCodeLabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_NewNamedCodeLabActionPerformed

		boolean IsCodeLab = (evt.getSource() == this.MI_NewNamedCodeLab);
		
		String Name = null;
		GetName: while(Name == null) {
			// Ask for the name
			Name = JOptionPane.showInputDialog(
					this,
					"What is the name of the "+(IsCodeLab?"CodeLab":"TextData")+"?",
					"Creating a "+(IsCodeLab?"named CodeLab":"TextData"),
					JOptionPane.QUESTION_MESSAGE);

			if(Name == null) return;
			
			if(this.getCodeLabFromTypePackage(Name, IsCodeLab) != null) {
				int Choice = JOptionPane.showConfirmDialog(
						this,
						"The "+(IsCodeLab?"CodeLab":"TextData")+" is already exist. Do you want to try another name?",
						"Creating "+(IsCodeLab?"named CodeLab":"TextData"),
						JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
				
				if(Choice == JOptionPane.YES_OPTION) { Name = null; continue GetName; }
				if(Choice == JOptionPane.NO_OPTION)  this.showCodeLab(Name, IsCodeLab);
				return;
			}
		}
		
		Class<?> Class      = null;
		int      ArrayCount =    0;
		GetClass: while(IsCodeLab && (Class == null)) {
			// Ask for the class
			String CName = JOptionPane.showInputDialog(this,
					"What is the class of the result of this CodeLab?\n" +
					"Enter a fully qualify name of the class e.g., java.io.File\n" +
					"NOTE 1: If the desired class is in java.lang package, you may 'java.lang' can be ommitted.\n" +
					"NOTE 2: Parameterized class cannot be used, e.g., 'java.util.List<String>' is not valid.\n" +
					"        Use 'java.util.List' in stread.\n"+
					"NOTE 3: If the desired class is an array, put '[]' at the end, e.g., 'Integer[]'.\n"+
					"NOTE 4: Primitive types will name automatically converted to its wrapper type, \n" +
					"        e.g., 'int' means 'java.lang.Integer'.\n"+
					"Creating a named CodeLab", "Object");
			
			if(CName == null) return;
			
			CName = CName.trim();
			while(CName.endsWith("[]")) { ArrayCount++; CName = CName.substring(0, CName.length() - "[]".length()); }
			
			if(     CName.equals("int"))     CName = "java.lang.Integer";
			else if(CName.equals("boolean")) CName = "java.lang.Boolean";
			else if(CName.equals("void"))    CName = "java.lang.Object";
			else if(CName.equals("char"))    CName = "java.lang.Character";
			else if(CName.equals("double"))  CName = "java.lang.Double";
			else if(CName.equals("Long"))    CName = "java.lang.Long";
			else if(CName.equals("Byte"))    CName = "java.lang.Byte";
			else if(CName.equals("Float"))   CName = "java.lang.Float";
			else if(CName.equals("Short"))   CName = "java.lang.Short";
			
			try {
				Class = JavaCompiler.Instance.forName(CName);
				
			} catch(ClassNotFoundException E) {
				
				try { Class =  java.lang.Class.forName("java.lang."+CName); }
				catch(ClassNotFoundException EE) {
					int Choice = JOptionPane.showConfirmDialog(this,
							"The given class name does not exist.\n" +
							"If you are sure it does, check the classpath.\n" +
							"Do you want to try again?", "Creating a named CodeLab",
							JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

					if(Choice == JOptionPane.YES_OPTION) continue GetClass;
					if(Choice == JOptionPane.NO_OPTION)  return;
				}
			}
		}
		
		// Create array
		for(int i = ArrayCount; --i >= 0; ) Class = Array.newInstance(Class, 0).getClass();
		
		this.newNamedCodeLab(Name, Class, IsCodeLab);
	}//GEN-LAST:event_MI_NewNamedCodeLabActionPerformed

	private void MI_NewErrorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_NewErrorActionPerformed
		String Prefix = null;
		String Kind   = null;
		
		if(     evt.getSource() == this.MI_NewWarning)      { Kind = "Warning";     Prefix = "WARNING_";     }
		else if(evt.getSource() == this.MI_NewErrorMessage) { Kind = "Error";       Prefix = "ERROR_";       }
		else if(evt.getSource() == this.MI_NewFatalError)   { Kind = "Fatal Error"; Prefix = "FATAL_ERROR_"; }
		else return;
		
		String Name = null;
		
		MessageName: while(Name == null) {
			Name = (String)JOptionPane.showInputDialog(this,
					"Enter the name of the " + Kind + ".\n The name MUST starts with `"+Prefix+"`.",
					"Create new " + Kind, JOptionPane.QUESTION_MESSAGE, null, null, Prefix);
			if(Name == null) return;
			boolean IsWrong = false;
			if(Name.startsWith(Prefix)) {
				for(int i = Name.length(); --i >= 0; ) {
					char c = Name.charAt(i);
					if((i == 0) && ((c >= '0') && (c <= '9'))) {
						IsWrong = true;
						break;
					}
					if(!(((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')) || (c == '_') || ((c >= '0') && (c <= '9')))) {
						IsWrong = true;
						break;
					} 
				}
				if(!IsWrong) {
					// Check for repeat
					// Remove from Menu
					for(int i = 0; i < this.MM_ErrorMessage.getItemCount(); i++) {
						JMenuItem MI = this.MM_ErrorMessage.getItem(i);
						if(MI           == null) continue;
						if(MI.getText() == null) continue;
						if(!MI.getText().startsWith(Name)) continue;
						if(JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(this,
								"The given message already exist.\n Do you want to try again?", "Create new " + Kind,
								JOptionPane.YES_NO_OPTION)) return;
						Name = null;
						continue MessageName;
					}
					break;
				}
			}
			if(JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(this,
				"The given name is mal-formed.\n Do you want to try again?", "Create new " + Kind,
				JOptionPane.YES_NO_OPTION)) return;
			Name = null;
		}


		String Message = JOptionPane.showInputDialog(this,"Enter the " + Kind + " message for '"+Name+"'.",
					"Create new " + Kind, JOptionPane.QUESTION_MESSAGE);
		if(Message == null) return;
		
		this.createErrMessageMenu(Name, Message, Kind);
}//GEN-LAST:event_MI_NewErrorActionPerformed

	private void SaveAsPocketActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SaveAsPocketActionPerformed
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if(This.TPPFileChooser == null) This.TPPFileChooser = new JFileChooser(This.WorkingDir);
				else                            This.TPPFileChooser.setCurrentDirectory(This.WorkingDir);

				// Show open dialog; this method does not return until the dialog is closed
				This.TPPFileChooser.addChoosableFileFilter(new TypeFileFilter(TypePocketFileExt, TypePocketFileDesc));
				if (This.TPPFileChooser.showSaveDialog(This) == JFileChooser.CANCEL_OPTION) return;
				File TheFile = This.TPPFileChooser.getSelectedFile();
				if (TheFile == null) return;

				if(((This.ThisFile == null) || !TheFile.toString().equals(This.ThisFile.toString())) && TheFile.exists()) {
					// Ask for overwrite
					int O = JOptionPane.showConfirmDialog(This, "The file '" + TheFile.toString()+ "' is already " +
							"exist.\n Are you sure you want to save over it?");
					if(O != JOptionPane.YES_OPTION) return;
				}
				
				if (!TheFile.toString().endsWith(TypePocketFileExt[0])) TheFile = new File(TheFile.toString() + TypePocketFileExt[0]);

				if (!TheFile.isFile() || !TheFile.exists() || !TheFile.canWrite()) {
					boolean IsFail = false;
					if (!TheFile.exists()) {
						try { TheFile.createNewFile(); }
						catch (Exception E) { IsFail = true; }
					} else IsFail = true;
					if (IsFail) {
						JOptionPane.showMessageDialog(This, "The selected file `" + TheFile.toString() + "` is not writable.");
						return;
					}
				}
				
				This.WorkingDir = TheFile.getParentFile().getAbsoluteFile();
				This.saveAsPocket(TheFile);
			}
		});
}//GEN-LAST:event_SaveAsPocketActionPerformed

	private void MI_TestTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_TestTypeActionPerformed
		int I = this.L_Types.getSelectedIndex();
		if(I == -1) return;
		Object O = this.L_Types.getModel().getElementAt(I);
		if(O == null) return;
		this.LNP_Parser.getTextComponent().setText("!"+ O.toString() +"!");
		this.B_NewParser.doClick();
	}//GEN-LAST:event_MI_TestTypeActionPerformed

	private void L_TypesMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_L_TypesMousePressed
		if(evt.isPopupTrigger()) {
			boolean IsTestTypeEnable = true;
			int I = this.L_Types.getSelectedIndex();
			if(I == -1) IsTestTypeEnable = false;
			else {
				Object O = this.L_Types.getModel().getElementAt(I);
				if(O == null) IsTestTypeEnable = false;
			}
			
			this.MI_TestType.setEnabled(IsTestTypeEnable);
			this.MI_EditType.setEnabled(IsTestTypeEnable);
			this.MI_PM_DeleteType.setEnabled(IsTestTypeEnable);
			this.PM_TypeList.show(evt.getComponent(), evt.getX(), evt.getY());
		}
	}//GEN-LAST:event_L_TypesMousePressed

	private void MI_EditTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_EditTypeActionPerformed
		TypesListMouseClicked();
	}//GEN-LAST:event_MI_EditTypeActionPerformed

	private void MI_PM_NewTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_PM_NewTypeActionPerformed
		this.B_NewType.doClick();
	}//GEN-LAST:event_MI_PM_NewTypeActionPerformed

	private void MI_PM_DeleteTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_PM_DeleteTypeActionPerformed
		this.B_DeleteType.doClick();
	}//GEN-LAST:event_MI_PM_DeleteTypeActionPerformed

	private void createErrMessageMenu(String Name, String Message, String Kind) {
		// Add a new one
		try {
			this.TPackage.IsFrozen = false;
			this.TPackage.addErrorMessage(Name, Message);
			
			String ShortMessage = Message;
			if(ShortMessage.contains("\n")) ShortMessage = ShortMessage.substring(0, ShortMessage.indexOf("\n"));
			if(ShortMessage.length() >= 50) ShortMessage = ShortMessage.substring(0, 50) + "...";
			
			JMenu MM = new JMenu();
			MM.setText(       Name + " - " + ShortMessage);
			MM.setToolTipText(Name + " - " + Message);
			this.MM_ErrorMessage.add(MM);

			JMenuItem MI = new JMenuItem();
			MI.setText("Edit");
			MI.setMnemonic('E');
			
			final String FName = Name;
			final String FKind = Kind;
			
			MI.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String Msg = (String)JOptionPane.showInputDialog(This,"Enter the " + FKind + " message for '"+FName+"'.",
							"Create new " + FKind, JOptionPane.QUESTION_MESSAGE, null, null,
							This.TPackage.errorMessage(FName));
					if(Msg == null) return;
					
					try {
						This.TPackage.IsFrozen = false;
						This.TPackage.removeErrorMessage(FName);
						This.TPackage.addErrorMessage(FName, Msg);
						
						JMenu TheMM = ((JMenu)((JPopupMenu)((JMenuItem)e.getSource()).getParent()).getInvoker());
			
						String TheShortMessage = Msg;
						if(TheShortMessage.contains("\n")) TheShortMessage = TheShortMessage.substring(0, TheShortMessage.indexOf("\n"));
						if(TheShortMessage.length() >= 50) TheShortMessage = TheShortMessage.substring(0, 50) + "...";

						TheMM.setText(       FName + " - " + TheShortMessage);
						TheMM.setToolTipText(FName + " - " + Msg);
						
					} finally { This.TPackage.IsFrozen = true; }
				}
			});
			MM.add(MI);

			MI = new JMenuItem();
			MI.setText("Delete");
			MI.setMnemonic('D');
			MI.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						This.TPackage.IsFrozen = false;
						This.TPackage.removeErrorMessage(FName);
						This.MM_ErrorMessage.remove(((JPopupMenu)((JMenuItem)e.getSource()).getParent()).getInvoker());
					} finally { This.TPackage.IsFrozen = true; }
				}
			});
			MM.add(MI);
		} finally { this.TPackage.IsFrozen = true; }
	}
	
	static final String CodeLabNamePrefix  = "C-Lab: ";
	static final String TextDataNamePrefix = "TextData: ";
	
	final String getDefaultCodeLabCode(Class<?> pClass) {
		if(pClass == null) pClass = Object.class;
		Class<?> Cs = pClass;
		while(Cs.isArray()) Cs = Cs.getComponentType();
		
		String Import = (Cs.getPackage().getName().startsWith("java.lang"))?"":"\nimport "+Cs.getPackage().getName()+".*;\n";
		return "// @Java:\n"+Import+"System.out.println(\"Hello World!!!\");\nSystem.err.println(\"Hello World!!!\");\nreturn null;";
	}
	
	/** Create a new named codelab */
	private void newNamedCodeLab(String pName, Class<?> pClass, boolean IsCodeLab) {
		if(IsCodeLab) pName = (pName.startsWith(CodeLabNamePrefix))?pName:CodeLabNamePrefix + pName;
		else          pName = (pName.startsWith(TextDataNamePrefix))?pName:TextDataNamePrefix + pName;
			
		this.setCodeLabToTypePackage(pName, pClass, IsCodeLab?this.getDefaultCodeLabCode(pClass):"", IsCodeLab);
		this.showCodeLab(pName, IsCodeLab);
		this.ensureCodeLabSynchronizeWithMenu();
	}
	
	private Serializable[] getCodeLabFromTypePackage(String pName, boolean IsCodeLab) {
		if(pName == null) return null;
		if(IsCodeLab) pName = (pName.startsWith(CodeLabNamePrefix))?pName:CodeLabNamePrefix + pName;
		else          pName = (pName.startsWith(TextDataNamePrefix))?pName:TextDataNamePrefix + pName;
		
		Serializable S = this.TPackage.getData(pName);
		if((S instanceof Serializable[]) && (((Serializable[])S).length == 2)) {
			if(((Serializable[])S)[0] == null) ((Serializable[])S)[0] = Object.class;
			if(((Serializable[])S)[1] == null) ((Serializable[])S)[1] = this.getDefaultCodeLabCode((Class<?>)((Serializable[])S)[0]);
			return (Serializable[])S;
		}
		return null;
	}
	
	/** Get a text data associated with the name */
	static public String getTextData(PTypePackage pTPackage,String pName) {
		pName = (pName.startsWith(TextDataNamePrefix))?pName:TextDataNamePrefix + pName;
		Serializable S = pTPackage.getData(pName);
		if((S instanceof Serializable[]) && (((Serializable[])S).length == 2)) {
			Object O = ((Serializable[])S)[1];
			return (O == null)?null:O.toString();
		}
		return null;
	}
	
	private void setCodeLabToTypePackage(String pName, Class<?> pClass, String pCode, boolean IsCodeLab) {
		if(IsCodeLab) pName = (pName.startsWith(CodeLabNamePrefix))?pName:CodeLabNamePrefix + pName;
		else          pName = (pName.startsWith(TextDataNamePrefix))?pName:TextDataNamePrefix + pName;
		
		try {
			this.TPackage.IsFrozen = false;
			this.TPackage.addData(pName, new Serializable[] { pClass, pCode });
		} finally{
			this.TPackage.IsFrozen = true;
		}
	}
	
	/** Show the already existing codelab */
	private void showCodeLab(String pName, boolean IsCodeLab) {
		// Check if there is already a tab with the codelab named as the tab name
		if(IsCodeLab) pName = (pName.startsWith(CodeLabNamePrefix))?pName:CodeLabNamePrefix + pName;
		else          pName = (pName.startsWith(TextDataNamePrefix))?pName:TextDataNamePrefix + pName;
		String TheTabName = pName;
		
		for (int i = 0; i < this.PT_Tabs.getTabCount(); i++) {
			Object O2 = this.PT_Tabs.getTabComponentAt(i);
			if (!(O2 instanceof ClosableTabTitle)) continue;
			ClosableTabTitle CTT = (ClosableTabTitle) O2;
			if (TheTabName.equals(((JLabel) CTT.getComponent(0)).getText())) {
				// Found the Tab
				this.PT_Tabs.setSelectedIndex(i);
				break;
			}
		}
		
		// Create a new Tab
		int TabIndex = this.PT_Tabs.getTabCount();
		final String TabName = TheTabName;
		Serializable[] S = this.getCodeLabFromTypePackage(pName, IsCodeLab);
		if(S == null) return;
		
		final Class<?> Class     = (Class<?>)S[0];
		final String   Code      = (String)S[1];
		final boolean  isCodeLab = IsCodeLab;
		
		ClosableTabTitle CTT = new ClosableTabTitle(TabName, this.PT_Tabs);
		this.PT_Tabs.addTab(TabName, new PFExecute(this,
				new ExecCallBack() {
					public void setResult(String pCode, Object pResult) {
						This.setCodeLabToTypePackage(TabName, Class, pCode, isCodeLab);
					}
				},
				TabName, Class, Code, IsCodeLab, CTT.Title));
		this.PT_Tabs.setSelectedIndex(TabIndex);
		this.PT_Tabs.setTabComponentAt(TabIndex, CTT);
	}
	
	private void removeCodeLab(String pName, boolean IsCodeLab) {
		// Check if there is already a tab with the codelab named as the tab name
		if(IsCodeLab) pName = (pName.startsWith(CodeLabNamePrefix))?pName:CodeLabNamePrefix + pName;
		else          pName = (pName.startsWith(TextDataNamePrefix))?pName:TextDataNamePrefix + pName;
		
		final String Name = pName;
		// Remove from Tab
		for (int i = 0; i < this.PT_Tabs.getTabCount(); i++) {
			Object O2 = this.PT_Tabs.getTabComponentAt(i);
			if (!(O2 instanceof ClosableTabTitle)) continue;
			ClosableTabTitle CTT = (ClosableTabTitle) O2;
			if (Name.equals(((JLabel) CTT.getComponent(0)).getText())) {
				// Found the Tab
				this.PT_Tabs.remove(i);
				break;
			}
		}
		
		// Remove from Menu
		for(int i = 0; i < this.MM_CodeLab.getItemCount(); i++) {
			JMenuItem MI = this.MM_CodeLab.getItem(i);
			if(MI == null) continue;
			if(!Name.equals(MI.getText())) continue;
			this.MM_CodeLab.remove(MI);
		}
		
		// Remove from TypePackage
		if(this.TPackage.MoreDatas != null) {
			try {
				this.TPackage.IsFrozen = false;
				this.TPackage.MoreDatas.remove(Name);
			} finally {
				this.TPackage.IsFrozen = true;
			}
		}
	}
	
	private void ensureCodeLabSynchronizeWithMenu() {
		if(TPackage.MoreDatas != null) {
			// Make sure all CodeLab in TypePackage is on the menu
			InTPackage: for(String DN : this.TPackage.MoreDatas.keySet()) {
				if((DN == null) || (!DN.startsWith(CodeLabNamePrefix) && !DN.startsWith(TextDataNamePrefix))) continue;
				// See in the menu if it already exist
				for(int i = 0; i < this.MM_CodeLab.getItemCount(); i++) {
					JMenuItem MI = this.MM_CodeLab.getItem(i);
					if(MI == null) continue;
					if(DN.equals(MI.getText())) continue InTPackage;
				}
				
				final boolean IsCodeLab = DN.startsWith(CodeLabNamePrefix);
					
				JMenu MM = new JMenu();
				MM.setText(DN);
				MM.setToolTipText(DN + ": " + this.getCodeLabFromTypePackage(DN, IsCodeLab)[0]);
				this.MM_CodeLab.add(MM);

				final String CLName = DN;
				JMenuItem MI = new JMenuItem();
				MI.setText("Edit");
				MI.setMnemonic('E');
				MI.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						This.showCodeLab(CLName, IsCodeLab);
					}
				});
				MM.add(MI);

				MI = new JMenuItem();
				MI.setText("Delete");
				MI.setMnemonic('D');
				MI.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						This.removeCodeLab(CLName, IsCodeLab);
					}
				});
				MM.add(MI);
			}
		}
		
		// Ensure what's in the menu is in the TPackage
		for(int i = 0; i < this.MM_CodeLab.getItemCount(); i++) {
			JMenuItem MI = this.MM_CodeLab.getItem(i);
			if(MI           == null) continue;
			if(MI.getText() == null) continue;
			if(!MI.getText().startsWith(CodeLabNamePrefix) && !MI.getText().startsWith(TextDataNamePrefix)) continue;
			
			final boolean IsCodeLab = MI.getText().startsWith(CodeLabNamePrefix);
			// Already exist
			if(this.getCodeLabFromTypePackage(MI.getText(), IsCodeLab) != null) continue;
			
			// Not exist.
			// See first if the code still there
			Class<?> Class = null;
			String   Code  = null;
			for (int t = 0; t < this.PT_Tabs.getTabCount(); t++) {
				Object O2 = this.PT_Tabs.getTabComponentAt(t);
				if (!(O2 instanceof ClosableTabTitle)) continue;
				ClosableTabTitle CTT = (ClosableTabTitle) O2;
				if (MI.getText().equals(((JLabel) CTT.getComponent(0)).getText())) {
					// Found the Tab
					Code  = ((PFExecute)this.PT_Tabs.getComponentAt(t)).getExecuteCode();
					Class = ((PFExecute)this.PT_Tabs.getComponentAt(t)).getResultClass();
					break;
				}
			}
			
			if(Class == null) Class = Object.class;
			if(Code  == null) Code  = this.getDefaultCodeLabCode(Class);
			
			this.setCodeLabToTypePackage(MI.getText(), Class, Code, IsCodeLab);
		}
	}
	
	private void TypesListMouseClicked() {
		Object O = this.L_Types.getSelectedValue();
		if(O == null) return;
		this.ensureTypeInfoDisplay(O.toString());

		this.setStatusBarText("Selecting the type '" + O.toString() + "'");
	}

	// Save the type package as a serializable object
	private void saveAsSerializable(File pTheFile) {
		final File TheFile = pTheFile;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					This.TPackage.IsFrozen = false;
					
					if(This.MICB_MainScopeSave.isSelected() && (This.MainScopeCode != null)) 
						 This.TPackage.addData("MainScope", This.MainScopeCode);
					else This.TPackage.removeData("MainScope");
					
					if(This.MICB_CContextSave.isSelected() && (This.CContextCode != null))
						 This.TPackage.addData("CContext", This.CContextCode);
					else This.TPackage.removeData("CContext");

					This.TPackage.addData("ToFreeze", This.MICB_Freeze.isSelected());
					
					This.TPackage.IsFrozen = This.MICB_Freeze.isSelected();
					This.TPackage.saveToFile(TheFile);
					This.setStatusBarText("The type(s) is/are saved.");

					// Update file information
					This.ThisFile = TheFile;
					This.ThisFileAs = 'S';

					This.HaveBeenEdited = false;
					This.updateTitle();
				} catch (Exception E) {
					JOptionPane.showMessageDialog(This,
							"There is a problem writing the selected file `"
									+ TheFile.toString() + "`" + ": "
									+ E.toString() + ".");
				} finally {
					This.TPackage.IsFrozen = false;
				}
			}
		});
	}

	private void saveAsText(File pTheFile) {
		final File TheFile = pTheFile;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					This.TPackage.IsFrozen = false;
					
					if(This.MICB_MainScopeSave.isSelected() && (This.MainScopeCode != null)) 
						 This.TPackage.addData("MainScope", This.MainScopeCode);
					else This.TPackage.removeData("MainScope");
					
					if(This.MICB_CContextSave.isSelected() && (This.CContextCode != null))
						 This.TPackage.addData("CContext", This.CContextCode);
					else This.TPackage.removeData("CContext");

					This.TPackage.addData("ToFreeze", This.MICB_Freeze.isSelected());
					
					This.TPackage.saveAsTextToFile(TheFile);
					This.setStatusBarText("The type(s) is/are saved.");

					// Update file information
					This.ThisFile = TheFile;
					This.ThisFileAs     = 'T';
					This.HaveBeenEdited = false;
					This.updateTitle();
				} catch (Exception E) {
					JOptionPane.showMessageDialog(This,
							"There is a problem writing the selected file `"
									+ TheFile.toString() + "`" + ": "
									+ E.toString() + ".");
				} finally {
					This.TPackage.IsFrozen = false;
				}
			}
		});
	}

	private void saveAsPocket(File pTheFile) {
		final File TheFile = pTheFile;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					This.TPackage.IsFrozen = false;
					
					if(This.MICB_MainScopeSave.isSelected() && (This.MainScopeCode != null)) 
						 This.TPackage.addData("MainScope", This.MainScopeCode);
					else This.TPackage.removeData("MainScope");
					
					if(This.MICB_CContextSave.isSelected() && (This.CContextCode != null))
						 This.TPackage.addData("CContext", This.CContextCode);
					else This.TPackage.removeData("CContext");

					This.TPackage.addData("ToFreeze", This.MICB_Freeze.isSelected());					
					
					This.TPackage.saveAsPocketToFile(TheFile);
					This.setStatusBarText("The type(s) is/are saved.");

					// Update file information
					This.ThisFile = TheFile;
					This.ThisFileAs     = 'P';
					This.HaveBeenEdited = false;
					This.updateTitle();
				} catch (Exception E) {
					JOptionPane.showMessageDialog(This,
							"There is a problem writing the selected file `"
									+ TheFile.toString() + "`" + ": "
									+ E.toString() + ".");
				} finally {
					This.TPackage.IsFrozen = false;
				}
			}
		});
	}

	/** Confirm before close */
	@Override
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			int exit = !this.HaveBeenEdited ? JOptionPane.YES_OPTION
					: JOptionPane.showConfirmDialog(this, "Are you sure?",
							"Closing type factory?", JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE);

			if (exit == JOptionPane.YES_OPTION) {
				TypeFactoryWindowCount--;
				if (TypeFactoryWindowCount == 0)
					System.exit(0);
				else
					this.setVisible(false);
			}
		} else
			super.processWindowEvent(e);
	}

	static public TypeFactory NewTypeFactory(TypeFactory TFactory, PTypePackage PTP) {
		try {
			//Set cross-platform Java L&F (also called "Metal")
			//UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
			// handle exception
			/*
			try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
			catch (Exception E) {}*/
		}
		final PTypePackage TP = PTP;

		TypeFactory TF = new TypeFactory(TFactory, TP);
		TF.setVisible(true);
		return TF;
	}
	
	void setParserText(String pText) {
		this.LNP_Parser.getTextComponent().setText(pText);
		this.LNP_Parser.requestFocusInWindow();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		NewTypeFactory(null, null);
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton B_DeleteType;
    private javax.swing.JButton B_NewParser;
    private javax.swing.JButton B_NewType;
    private net.nawaman.swing.LineNumberedTextComponentPanel LNP_Parser;
    private javax.swing.JLabel L_StatusBar;
    @SuppressWarnings("rawtypes")
    private javax.swing.JList L_Types;
    private javax.swing.JMenuBar MB_MainMenu;
    private javax.swing.JCheckBoxMenuItem MICB_CContextSave;
    private javax.swing.JCheckBoxMenuItem MICB_Freeze;
    private javax.swing.JCheckBoxMenuItem MICB_MainScopeSave;
    private javax.swing.JMenuItem MI_About;
    private javax.swing.JMenuItem MI_AddJarFile;
    private javax.swing.JMenuItem MI_AddURL;
    private javax.swing.JMenuItem MI_CContextClear;
    private javax.swing.JMenuItem MI_CContextEdit;
    private javax.swing.JMenuItem MI_ClearMainScope;
    private javax.swing.JMenuItem MI_CodeLab;
    private javax.swing.JMenuItem MI_Content;
    private javax.swing.JMenuItem MI_DeleteType;
    private javax.swing.JMenuItem MI_EditType;
    private javax.swing.JMenuItem MI_ImportAsText;
    private javax.swing.JMenuItem MI_MainScope;
    private javax.swing.JMenuItem MI_New;
    private javax.swing.JMenuItem MI_NewErrorMessage;
    private javax.swing.JMenuItem MI_NewFatalError;
    private javax.swing.JMenuItem MI_NewNamedCodeLab;
    private javax.swing.JMenuItem MI_NewNamedText;
    private javax.swing.JMenuItem MI_NewType;
    private javax.swing.JMenuItem MI_NewWarning;
    private javax.swing.JMenuItem MI_Open;
    private javax.swing.JMenuItem MI_OpenAsPocket;
    private javax.swing.JMenuItem MI_OpenAsSerializable;
    private javax.swing.JMenuItem MI_OpenAsText;
    private javax.swing.JMenuItem MI_PM_DeleteType;
    private javax.swing.JMenuItem MI_PM_NewType;
    private javax.swing.JMenuItem MI_Quit;
    private javax.swing.JMenuItem MI_SaveAsPocket;
    private javax.swing.JMenuItem MI_SaveAsText;
    private javax.swing.JMenuItem MI_TestType;
    private javax.swing.JMenu MM_CContext;
    private javax.swing.JMenu MM_ClassPaths;
    private javax.swing.JMenu MM_CodeLab;
    private javax.swing.JMenu MM_Edit;
    private javax.swing.JMenu MM_ErrorMessage;
    private javax.swing.JMenu MM_File;
    private javax.swing.JMenu MM_Help;
    private javax.swing.JMenu MM_MainScope;
    private javax.swing.JMenu MM_Setting;
    private javax.swing.JPopupMenu PM_TypeList;
    private javax.swing.JTabbedPane PT_Tabs;
    private javax.swing.JPanel P_NewParser;
    private javax.swing.JPanel P_StatusBar;
    private javax.swing.JPanel P_TypeOutter;
    private javax.swing.JPanel P_TypesInner;
    private javax.swing.JPanel P_Whole;
    private javax.swing.JSplitPane SP_Bottom;
    private javax.swing.JScrollPane SP_Parser;
    private javax.swing.JScrollPane SP_Types;
    private javax.swing.JSplitPane SP_Whole;
    private javax.swing.JSeparator S_ClassPaths;
    private javax.swing.JSeparator S_CodeLab;
    private javax.swing.JSeparator S_ErrorMessage;
    private javax.swing.JSeparator S_MMFile;
    private javax.swing.JSeparator S_MMFile2;
    private javax.swing.JSeparator S_MM_File3;
    private javax.swing.JSeparator S_MM_Help;
    private javax.swing.JSeparator S_PM_TypeList;
    private javax.swing.JSeparator S_Setting;
    private javax.swing.JMenuItem Save;
    private javax.swing.JMenuItem SaveAsSerializable;
    private javax.swing.JSeparator jSeparator1;
    // End of variables declaration//GEN-END:variables

}

/** Action listener for Close Tab */
class CloseTabAction implements ActionListener {
	public CloseTabAction(String pTitle, JTabbedPane pParent) {
		this.Title = pTitle;
		this.Parent = pParent;
	}

	JTabbedPane Parent;

	String Title;

	public void actionPerformed(java.awt.event.ActionEvent evt) {
		Parent.remove(Parent.indexOfTab(this.Title));
	}
}

/** Closable Tab Title */
class ClosableTabTitle extends JPanel {
    
    private static final long serialVersionUID = -2226442646833624149L;

    ClosableTabTitle(String pTitle, JTabbedPane pTabbedPanel) {
		JButton TapTitleClose = new JButton(XImage);
		TapTitleClose.setBorder(new EmptyBorder(3, 3, 3, 3));
		TapTitleClose.addActionListener(new CloseTabAction(pTitle, pTabbedPanel));
		TapTitleClose.setOpaque(true);
		TapTitleClose.setBorderPainted(false);

		this.Title = new JLabel(pTitle);
		Title.setBorder(new EmptyBorder(0, 0, 0, 0));

		this.setBorder(new EmptyBorder(0, 0, 0, 0));
		this.setOpaque(true);
		this.add(Title);
		this.add(TapTitleClose);
	}
	
	JLabel Title = null;

	static ImageIcon XImage = new ImageIcon(new byte[] { (byte) 0x89,
			(byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A,
			(byte) 0x1A, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x0D, (byte) 0x49, (byte) 0x48, (byte) 0x44, (byte) 0x52,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x08, (byte) 0x06,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1F, (byte) 0xF3,
			(byte) 0xFF, (byte) 0x61, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x01, (byte) 0x73, (byte) 0x52, (byte) 0x47, (byte) 0x42,
			(byte) 0x00, (byte) 0xAE, (byte) 0xCE, (byte) 0x1C, (byte) 0xE9,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x62,
			(byte) 0x4B, (byte) 0x47, (byte) 0x44, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xF9,
			(byte) 0x43, (byte) 0xBB, (byte) 0x7F, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x09, (byte) 0x70, (byte) 0x48, (byte) 0x59,
			(byte) 0x73, (byte) 0x00, (byte) 0x00, (byte) 0x0B, (byte) 0x13,
			(byte) 0x00, (byte) 0x00, (byte) 0x0B, (byte) 0x13, (byte) 0x01,
			(byte) 0x00, (byte) 0x9A, (byte) 0x9C, (byte) 0x18, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x74, (byte) 0x49,
			(byte) 0x4D, (byte) 0x45, (byte) 0x07, (byte) 0xD8, (byte) 0x05,
			(byte) 0x16, (byte) 0x09, (byte) 0x13, (byte) 0x24, (byte) 0xDD,
			(byte) 0x16, (byte) 0x2D, (byte) 0x1D, (byte) 0x00, (byte) 0x00,
			(byte) 0x03, (byte) 0x43, (byte) 0x49, (byte) 0x44, (byte) 0x41,
			(byte) 0x54, (byte) 0x38, (byte) 0xCB, (byte) 0xA5, (byte) 0xD3,
			(byte) 0x7F, (byte) 0x4C, (byte) 0x94, (byte) 0x05, (byte) 0x1C,
			(byte) 0xC7, (byte) 0xF1, (byte) 0xCF, (byte) 0xF3, (byte) 0xD0,
			(byte) 0x0E, (byte) 0x1E, (byte) 0x9E, (byte) 0xE7, (byte) 0xB8,
			(byte) 0xBB, (byte) 0x26, (byte) 0x21, (byte) 0x91, (byte) 0x03,
			(byte) 0xD7, (byte) 0x01, (byte) 0x8A, (byte) 0x8B, (byte) 0x74,
			(byte) 0xB8, (byte) 0x45, (byte) 0xCD, (byte) 0x65, (byte) 0x5B,
			(byte) 0xE4, (byte) 0x06, (byte) 0xCD, (byte) 0x74, (byte) 0x92,
			(byte) 0x4A, (byte) 0x46, (byte) 0x4E, (byte) 0x5D, (byte) 0xB1,
			(byte) 0x7E, (byte) 0xD1, (byte) 0x4A, (byte) 0x68, (byte) 0x72,
			(byte) 0x58, (byte) 0xD7, (byte) 0xA0, (byte) 0xE6, (byte) 0x56,
			(byte) 0x6A, (byte) 0xE1, (byte) 0x8F, (byte) 0x6B, (byte) 0xC5,
			(byte) 0x65, (byte) 0x40, (byte) 0x5A, (byte) 0x66, (byte) 0x0C,
			(byte) 0x29, (byte) 0x31, (byte) 0x4D, (byte) 0x18, (byte) 0x41,
			(byte) 0xDB, (byte) 0x29, (byte) 0x63, (byte) 0xE3, (byte) 0x37,
			(byte) 0xE2, (byte) 0xDD, (byte) 0x71, (byte) 0xDD, (byte) 0x0F,
			(byte) 0x90, (byte) 0x1F, (byte) 0x16, (byte) 0x2B, (byte) 0x3A,
			(byte) 0x38, (byte) 0x22, (byte) 0xE0, (byte) 0x9E, (byte) 0xE7,
			(byte) 0xD3, (byte) 0x1F, (byte) 0x79, (byte) 0x9B, (byte) 0x6B,
			(byte) 0xFD, (byte) 0xD7, (byte) 0x77, (byte) 0x7B, (byte) 0xFF,
			(byte) 0xF9, (byte) 0xFA, (byte) 0x7E, (byte) 0xFF, (byte) 0xFA,
			(byte) 0x02, (byte) 0xFF, (byte) 0x77, (byte) 0xC6, (byte) 0x03,
			(byte) 0x1E, (byte) 0x8C, (byte) 0xF9, (byte) 0x87, (byte) 0x71,
			(byte) 0xAC, (byte) 0xFC, (byte) 0x40, (byte) 0xD7, (byte) 0x16,
			(byte) 0x59, (byte) 0x9A, (byte) 0xDD, (byte) 0xFF, (byte) 0xF4,
			(byte) 0x36, (byte) 0xFF, (byte) 0x4D, (byte) 0xAF, (byte) 0xCB,
			(byte) 0x3C, (byte) 0x1E, (byte) 0xF0, (byte) 0xE0, (byte) 0xDF,
			(byte) 0x5D, (byte) 0x6B, (byte) 0xB9, (byte) 0x9C, (byte) 0xB7,
			(byte) 0x73, (byte) 0x45, (byte) 0x12, (byte) 0x5F, (byte) 0xDE,
			(byte) 0xF4, (byte) 0x04, (byte) 0x07, (byte) 0xBB, (byte) 0x3A,
			(byte) 0x8A, (byte) 0xC7, (byte) 0x03, (byte) 0x1E, (byte) 0x60,
			(byte) 0xCC, (byte) 0xE7, (byte) 0xC6, (byte) 0x47, (byte) 0x65,
			(byte) 0x6F, (byte) 0x16, (byte) 0xDA, (byte) 0x65, (byte) 0x50,
			(byte) 0x2B, (byte) 0xC8, (byte) 0xE5, (byte) 0x77, (byte) 0xE6,
			(byte) 0x44, (byte) 0x96, (byte) 0x6C, (byte) 0xCF, (byte) 0xE7,
			(byte) 0x4D, (byte) 0xAF, (byte) 0x2B, (byte) 0x73, (byte) 0xCC,
			(byte) 0xE7, (byte) 0x46, (byte) 0xA4, (byte) 0xF6, (byte) 0xA6,
			(byte) 0xC6, (byte) 0x92, (byte) 0xE2, (byte) 0xBB, (byte) 0xF5,
			(byte) 0x5C, (byte) 0xC8, (byte) 0xCF, (byte) 0xA1, (byte) 0x33,
			(byte) 0x51, (byte) 0xE2, (byte) 0xCE, (byte) 0x54, (byte) 0x33,
			(byte) 0x03, (byte) 0xEE, (byte) 0xEB, (byte) 0x5B, (byte) 0x31,
			(byte) 0xEA, (byte) 0x75, (byte) 0xE2, (byte) 0x49, (byte) 0x45,
			(byte) 0xA6, (byte) 0x16, (byte) 0x0F, (byte) 0xB2, (byte) 0xEA,
			(byte) 0x10, (byte) 0xE9, (byte) 0xBA, (byte) 0xCE, (byte) 0xFA,
			(byte) 0x94, (byte) 0x78, (byte) 0x96, (byte) 0x14, (byte) 0xEC,
			(byte) 0xE0, (byte) 0xC8, (byte) 0xCF, (byte) 0x37, (byte) 0xCC,
			(byte) 0xA3, (byte) 0x5E, (byte) 0x67, (byte) 0x7A, (byte) 0x73,
			(byte) 0xC3, (byte) 0xB9, (byte) 0xCD, (byte) 0xAF, (byte) 0x1B,
			(byte) 0x62, (byte) 0x19, (byte) 0x3E, (byte) 0x5C, (byte) 0x41,
			(byte) 0xDE, (byte) 0x9A, (byte) 0x20, (byte) 0x1F, (byte) 0xB8,
			(byte) 0x97, (byte) 0x87, (byte) 0x25, (byte) 0xB0, (byte) 0xE6,
			(byte) 0xE8, (byte) 0xFB, (byte) 0x1F, (byte) 0x60, (byte) 0xC4,
			(byte) 0x33, (byte) 0x84, (byte) 0xB7, (byte) 0x9F, (byte) 0xDF,
			(byte) 0x5B, (byte) 0x68, (byte) 0x97, (byte) 0x41, (byte) 0xDE,
			(byte) 0x23, (byte) 0x90, (byte) 0x35, (byte) 0x1F, (byte) 0x93,
			(byte) 0x03, (byte) 0x3D, (byte) 0x3C, (byte) 0x93, (byte) 0x68,
			(byte) 0x60, (byte) 0xF9, (byte) 0xEE, (byte) 0xC2, (byte) 0xA1,
			(byte) 0x8B, (byte) 0x5F, (byte) 0xD5, (byte) 0xED, (byte) 0x2D,
			(byte) 0x35, (byte) 0xC4, (byte) 0x50, (byte) 0xAD, (byte) 0x3A,
			(byte) 0x44, (byte) 0x8E, (byte) 0x8D, (byte) 0x92, (byte) 0xD9,
			(byte) 0x69, (byte) 0x74, (byte) 0x9A, (byte) 0xC0, (byte) 0x5C,
			(byte) 0x63, (byte) 0x1C, (byte) 0xAF, (byte) 0x5E, (byte) 0xB9,
			(byte) 0xF8, (byte) 0x0E, (byte) 0x02, (byte) 0xEE, (byte) 0x41,
			(byte) 0xF8, (byte) 0x5D, (byte) 0x03, (byte) 0xB0, (byte) 0x3C,
			(byte) 0xB7, (byte) 0xAB, (byte) 0xBC, (byte) 0x4E, (byte) 0xB9,
			(byte) 0xBD, (byte) 0xE4, (byte) 0xEB, (byte) 0x5A, (byte) 0xB2,
			(byte) 0xAB, (byte) 0x83, (byte) 0xA7, (byte) 0x12, (byte) 0xF4,
			(byte) 0x2C, (byte) 0x8D, (byte) 0x11, (byte) 0xA8, (byte) 0xDA,
			(byte) 0x8E, (byte) 0x90, (byte) 0xA3, (byte) 0x7E, (byte) 0x32,
			(byte) 0x6B, (byte) 0x25, (byte) 0x87, (byte) 0x4C, (byte) 0x60,
			(byte) 0xAE, (byte) 0xC9, (byte) 0x34, (byte) 0xD1, (byte) 0x76,
			(byte) 0xA1, (byte) 0x61, (byte) 0x5F, (byte) 0xC0, (byte) 0x3D,
			(byte) 0x08, (byte) 0xF8, (byte) 0x5C, (byte) 0xFD, (byte) 0xF0,
			(byte) 0xB9, (byte) 0xFA, (byte) 0xE1, (byte) 0x75, (byte) 0xF6,
			(byte) 0x61, (byte) 0xFF, (byte) 0xF6, (byte) 0x6D, (byte) 0xD5,
			(byte) 0x5F, (byte) 0x2A, (byte) 0x20, (byte) 0x97, (byte) 0x47,
			(byte) 0x91, (byte) 0x17, (byte) 0xEA, (byte) 0x49, (byte) 0x47,
			(byte) 0x1B, (byte) 0x79, (byte) 0xEA, (byte) 0x24, (byte) 0x19,
			(byte) 0xF0, (byte) 0x92, (byte) 0xEB, (byte) 0x92, (byte) 0xD9,
			(byte) 0x67, (byte) 0x04, (byte) 0xF3, (byte) 0x96, (byte) 0x2D,
			(byte) 0xE3, (byte) 0x4F, (byte) 0xDF, (byte) 0x37, (byte) 0xEE,
			(byte) 0x89, (byte) 0x38, (byte) 0x51, (byte) 0x53, (byte) 0x55,
			(byte) 0x68, (byte) 0xAA, (byte) 0x0A, (byte) 0x6A, (byte) 0x1A,
			(byte) 0x5E, (byte) 0xB4, (byte) 0x96, (byte) 0x17, (byte) 0x75,
			(byte) 0x3E, (byte) 0xBE, (byte) 0xB9, (byte) 0xBB, (byte) 0x61,
			(byte) 0x4E, (byte) 0x05, (byte) 0x5E, (byte) 0x7A, (byte) 0x16,
			(byte) 0xF8, (byte) 0xF5, (byte) 0x16, (byte) 0xB0, (byte) 0x76,
			(byte) 0x3D, (byte) 0xF0, (byte) 0xD4, (byte) 0xA3, (byte) 0xE8,
			(byte) 0xF1, (byte) 0x8F, (byte) 0xC0, (byte) 0x12, (byte) 0x9D,
			(byte) 0x80, (byte) 0xB2, (byte) 0x3A, (byte) 0x7B, (byte) 0x51,
			(byte) 0x52, (byte) 0x4A, (byte) 0x72, (byte) 0x6D, (byte) 0xC4,
			(byte) 0x89, (byte) 0xAA, (byte) 0x1A, (byte) 0x46, (byte) 0x24,
			(byte) 0x52, (byte) 0x43, (byte) 0xD6, (byte) 0xC6, (byte) 0x0D,
			(byte) 0x47, (byte) 0x9C, (byte) 0x61, (byte) 0x00, (byte) 0x8B,
			(byte) 0x8B, (byte) 0x58, (byte) 0x7A, (byte) 0xE1, (byte) 0x19,
			(byte) 0xFC, (byte) 0x99, (byte) 0x93, (byte) 0x0D, (byte) 0x4C,
			(byte) 0x8E, (byte) 0xC3, (byte) 0xB1, (byte) 0x08, (byte) 0x64,
			(byte) 0xE6, (byte) 0x3C, (byte) 0xD6, (byte) 0x9D, (byte) 0xB0,
			(byte) 0x22, (byte) 0xC9, (byte) 0x7E, (byte) 0xA7, (byte) 0x11,
			(byte) 0x49, (byte) 0x22, (byte) 0xD2, (byte) 0xB5, (byte) 0xE6,
			(byte) 0xD6, (byte) 0x8D, (byte) 0x13, (byte) 0x6F, (byte) 0x59,
			(byte) 0xCE, (byte) 0x1E, (byte) 0x54, (byte) 0x80, (byte) 0xF9,
			(byte) 0x30, (byte) 0x70, (byte) 0x63, (byte) 0x1A, (byte) 0x70,
			(byte) 0xFF, (byte) 0x01, (byte) 0x4C, (byte) 0x2F, (byte) 0x00,
			(byte) 0xAF, (byte) 0xC9, (byte) 0x80, (byte) 0xE1, (byte) 0xFC,
			(byte) 0xB9, (byte) 0xF4, (byte) 0xF3, (byte) 0x9F, (byte) 0x7F,
			(byte) 0x71, (byte) 0xFC, (byte) 0x4E, (byte) 0x23, (byte) 0x0A,
			(byte) 0x82, (byte) 0x00, (byte) 0x41, (byte) 0x10, (byte) 0xE0,
			(byte) 0xB8, (byte) 0xD2, (byte) 0x92, (byte) 0xF9, (byte) 0x9B,
			(byte) 0xF5, (byte) 0x60, (byte) 0xFB, (byte) 0x01, (byte) 0x59,
			(byte) 0xC3, (byte) 0x7C, (byte) 0x18, (byte) 0x70, (byte) 0x07,
			(byte) 0x81, (byte) 0x19, (byte) 0x1D, (byte) 0x30, (byte) 0x1D,
			(byte) 0x0B, (byte) 0x04, (byte) 0x66, (byte) 0x81, (byte) 0xE0,
			(byte) 0x22, (byte) 0x50, (byte) 0xA6, (byte) 0x40, (byte) 0xF9,
			(byte) 0xEB, (byte) 0x93, (byte) 0x93, (byte) 0xC5, (byte) 0x97,
			(byte) 0xCE, (byte) 0xD6, (byte) 0x7F, (byte) 0x1A, (byte) 0x71,
			(byte) 0xA2, (byte) 0x20, (byte) 0x08, (byte) 0x68, (byte) 0x6F,
			(byte) 0xBA, (byte) 0x94, (byte) 0x37, (byte) 0x5B, (byte) 0x61,
			(byte) 0xED, (byte) 0x2F, (byte) 0xB9, (byte) 0x8D, (byte) 0x87,
			(byte) 0x83, (byte) 0x40, (byte) 0x30, (byte) 0x1A, (byte) 0xF8,
			(byte) 0x30, (byte) 0x39, (byte) 0x15, (byte) 0xA7, (byte) 0xD7,
			(byte) 0x3D, (byte) 0x82, (byte) 0x19, (byte) 0x09, (byte) 0xF0,
			(byte) 0xCD, (byte) 0x00, (byte) 0xA1, (byte) 0x25, (byte) 0xC0,
			(byte) 0xAA, (byte) 0x07, (byte) 0xA6, (byte) 0xAA, (byte) 0x8E,
			(byte) 0x16, (byte) 0xB5, (byte) 0x7E, (byte) 0xDB, (byte) 0xF4,
			(byte) 0x9E, (byte) 0x20, (byte) 0x08, (byte) 0x40, (byte) 0x4B,
			(byte) 0xE3, (byte) 0x37, (byte) 0x05, (byte) 0x15, (byte) 0x31,
			(byte) 0xC2, (byte) 0x2C, (byte) 0xE3, (byte) 0xC1, (byte) 0x39,
			(byte) 0x13, (byte) 0xD8, (byte) 0x2B, (byte) 0x82, (byte) 0x3F,
			(byte) 0x4A, (byte) 0xE0, (byte) 0x96, (byte) 0xD5, (byte) 0xAB,
			(byte) 0xD8, (byte) 0x7D, (byte) 0xB5, (byte) 0xAD, (byte) 0xD8,
			(byte) 0xD9, (byte) 0xD7, (byte) 0x99, (byte) 0xB0, (byte) 0xFB,
			(byte) 0xE1, (byte) 0x87, (byte) 0xE8, (byte) 0x88, (byte) 0x05,
			(byte) 0x7B, (byte) 0x05, (byte) 0x30, (byte) 0x64, (byte) 0x04,
			(byte) 0xD5, (byte) 0x78, (byte) 0x70, (byte) 0x8F, (byte) 0x4E,
			(byte) 0x64, (byte) 0x47, (byte) 0xEB, (byte) 0xE5, (byte) 0xAD,
			(byte) 0xE2, (byte) 0xF2, (byte) 0xFB, (byte) 0x92, (byte) 0xA4,
			(byte) 0x25, (byte) 0xA3, (byte) 0x41, (byte) 0x59, (byte) 0xD2,
			(byte) 0xFE, (byte) 0xB9, (byte) 0x3C, (byte) 0xA5, (byte) 0x03,
			(byte) 0x6C, (byte) 0xF7, (byte) 0x67, (byte) 0xA0, (byte) 0xB2,
			(byte) 0xA6, (byte) 0xFA, (byte) 0x5D, (byte) 0xA3, (byte) 0xC9,
			(byte) 0x74, (byte) 0x42, (byte) 0xA7, (byte) 0x8B, (byte) 0xFE,
			(byte) 0xA5, (byte) 0xCC, (byte) 0x76, (byte) 0x6C, (byte) 0x65,
			(byte) 0x75, (byte) 0x46, (byte) 0x16, (byte) 0x16, (byte) 0x24,
			(byte) 0xC0, (byte) 0x13, (byte) 0x04, (byte) 0x16, (byte) 0x54,
			(byte) 0x60, (byte) 0x3E, (byte) 0xEA, (byte) 0x2E, (byte) 0x28,
			(byte) 0x8A, (byte) 0x5E, (byte) 0x16, (byte) 0x95, (byte) 0x38,
			(byte) 0x7D, (byte) 0xCD, (byte) 0x86, (byte) 0xE3, (byte) 0xB6,
			(byte) 0x37, (byte) 0x2A, (byte) 0x35, (byte) 0x19, (byte) 0xBD,
			(byte) 0x31, (byte) 0x80, (byte) 0x7D, (byte) 0xF5, (byte) 0x83,
			(byte) 0x93, (byte) 0x95, (byte) 0xB5, (byte) 0x9F, (byte) 0xA5,
			(byte) 0xC7, (byte) 0x99, (byte) 0x8C, (byte) 0x56, (byte) 0x31,
			(byte) 0x4A, (byte) 0x84, (byte) 0x18, (byte) 0x25, (byte) 0x42,
			(byte) 0x92, (byte) 0x63, (byte) 0x03, (byte) 0x16, (byte) 0xBB,
			(byte) 0x2D, (byte) 0xFB, (byte) 0x44, (byte) 0x5A, (byte) 0x26,
			(byte) 0x86, (byte) 0x25, (byte) 0xA0, (byte) 0x34, (byte) 0x24,
			(byte) 0x60, (byte) 0xFD, (byte) 0xAB, (byte) 0xAF, (byte) 0x58,
			(byte) 0x14, (byte) 0x63, (byte) 0x5C, (byte) 0x8F, (byte) 0x30,
			(byte) 0xE2, (byte) 0x19, (byte) 0x02, (byte) 0x00, (byte) 0x4C,
			(byte) 0x4D, (byte) 0x4C, (byte) 0x16, (byte) 0x3A, (byte) 0x7E,
			(byte) 0x68, (byte) 0xC6, (byte) 0xA6, (byte) 0x1D, (byte) 0xF9,
			(byte) 0x1E, (byte) 0x59, (byte) 0x51, (byte) 0x3A, (byte) 0xFF,
			(byte) 0xEB, (byte) 0x73, (byte) 0xE7, (byte) 0x42, (byte) 0xA1,
			(byte) 0xB5, (byte) 0x1D, (byte) 0x2D, (byte) 0xAD, (byte) 0xD1,
			(byte) 0xE6, (byte) 0x35, (byte) 0x19, (byte) 0x52, (byte) 0x4A,
			(byte) 0x5A, (byte) 0xEA, (byte) 0xEF, (byte) 0x00, (byte) 0x06,
			(byte) 0xFE, (byte) 0x06, (byte) 0x41, (byte) 0xEC, (byte) 0xD0,
			(byte) 0x82, (byte) 0x85, (byte) 0x5C, (byte) 0x3D, (byte) 0x0C,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x49,
			(byte) 0x45, (byte) 0x4E, (byte) 0x44, (byte) 0xAE, (byte) 0x42,
			(byte) 0x60, (byte) 0x82 });
}
