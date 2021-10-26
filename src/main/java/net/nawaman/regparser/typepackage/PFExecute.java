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

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.PrintStream;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Caret;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

import net.nawaman.regparser.CompilationContext;
import net.nawaman.regparser.PType;
import net.nawaman.script.Function;
import net.nawaman.script.ProblemContainer;
import net.nawaman.script.ScriptEngine;
import net.nawaman.script.ScriptManager;
import net.nawaman.script.Signature;
import net.nawaman.swing.LineNumberedTextComponentPanel;
import net.nawaman.swing.text.HTMLOutputPane;

/**
 * A call back class
 *
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 **/
interface ExecCallBack {
	
	public void setResult(String pCode, Object pResult);
	
}

/**
 * Execution lab
 * 
 * NOTE: Use out:PrintStream as the way to print out something to the result console.
 *
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 */
public class PFExecute extends javax.swing.JPanel {

    private static final long serialVersionUID = 6346684434877160060L;
    
    final ExecCallBack DoNothing = new ExecCallBack() { public void setResult(String pCode, Object pResult) {} };
	
	/** Creates new form PFMainScope */
	public PFExecute() {
		this(null, null, null, int.class, null, false, null);
	}
	/** Creates new form PFMainScope */
	PFExecute(TypeFactory pTFactory, ExecCallBack pCallBack, String pReturnName, Class<?> pReturnClass, String pInitCode,
			boolean pIsNoCompile, JLabel pTitle) {
		
		
		this.TFactory  = pTFactory;
		this.Title     = pTitle;
		this.TitleText = (pTitle == null)?null:pTitle.getText();
		
		// Flag indicating that the code can be saved without the need for compiling
		this.IsCompile = pIsNoCompile;
		
		initComponents();
        ((javax.swing.JTextPane)this.TP_ResultConsole.getTextComponent()).setContentType("text/html");
		
		if(pCallBack == null) pCallBack = DoNothing;
		
		if(pReturnName  == null) pReturnName  = "Result";
		if(pReturnClass == null) pReturnClass = Object.class;
		
		if(pReturnClass.isPrimitive()) {
			if(pReturnClass == int.class)     pReturnClass = Integer.class;
			if(pReturnClass == boolean.class) pReturnClass = Boolean.class;
			if(pReturnClass == double.class)  pReturnClass = Double.class;
			if(pReturnClass == char.class)    pReturnClass = Character.class;
			if(pReturnClass == byte.class)    pReturnClass = Byte.class;
			if(pReturnClass == long.class)    pReturnClass = Long.class;
			if(pReturnClass == short.class)   pReturnClass = Short.class;
			if(pReturnClass == float.class)   pReturnClass = Float.class;
		}
		
		if(pInitCode == null) {
			String Import = pReturnClass.getPackage().getName();
			Import    = Import.startsWith("java.")?"":"import "+Import + ".*;\n\n";
			pInitCode = "// @Java:\n"+Import+"return null;";
		}
		
		this.CallBack = pCallBack;
		
		// Display the short name
		String CName = pReturnClass.getCanonicalName();
		if(CName.startsWith("java.lang.") && (CName.indexOf(",", "java.lang.".length()) == -1))
			CName = CName.substring("java.lang.".length());
		
		this.L_ResultClass.setText(
				"<html><b>"+
				(this.IsCompile?pReturnName:pReturnName.substring(TypeFactory.TextDataNamePrefix.length()))+
				(this.IsCompile?": </b>"+CName:"")
			);
		
		this.LNP_Execute.setToolTipText("Execute code for " + pReturnName + ": " + pReturnClass.getCanonicalName());
		this.LNP_Execute.getTextComponent().setText(this.LNP_Execute.getToolTipText());
		this.LNP_Execute.getTextComponent().setText(pInitCode);
		
		String SignatureName = pReturnName;
		StringBuffer SB = new StringBuffer();
		for(int i = 0; i < SignatureName.length(); i++) {
			char Char = SignatureName.charAt(i);
			if(Character.isJavaIdentifierPart(Char)) SB.append(Char);
		}
		SignatureName = SB.toString();
		
		this.Signature = new net.nawaman.script.Signature.Simple(
							SignatureName, pReturnClass, false,
							new Class<?>[] { PTypePackage.class, CompilationContext.class });
		
		if(!this.IsCompile) {
			this.SP_CodeAndResult.setResizeWeight(1);
			this.SP_CodeAndResult.setEnabled(false);
			this.TP_ResultConsole.setVisible(false);
			
			this.B_Execute.setText("Save");
			this.B_Execute.setMnemonic('S');
		}
		
		this.LNP_Execute.save();
		this.LNP_Execute.useChangeHighlight();
		this.LNP_Execute.getTextComponent().addKeyListener(new java.awt.event.KeyAdapter() {
			@Override public void keyReleased(KeyEvent evt) {
				if(evt.getKeyCode() == KeyEvent.VK_ENTER) {
					if(evt.isControlDown() && !evt.isAltDown() && !evt.isShiftDown() && !evt.isMetaDown()) {
						B_Execute.doClick();
						
					} else {
						JTextComponent JTC = LNP_Execute.getTextComponent();
						
						String Text = JTC.getText();
						int    Pos  = JTC.getCaretPosition() - 2;
						int    BeginLine = Text.lastIndexOf("\n", Pos) + 1;
						if(BeginLine < 0) BeginLine = 0;
	
						Caret pos = JTC.getCaret();
						String Tabs = "";
						for(int i = BeginLine; true; i++) {
							if(     Text.charAt(i) == '\t') Tabs += '\t';
							else if(Text.charAt(i) ==  ' ') Tabs += ' ';
							else break;
						}
						try{ JTC.getDocument().insertString(pos.getDot(), Tabs, null); }
						catch(Exception E) {}
					}
				}
			}
        });
		this.LNP_Execute.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) { updateTabTitle(); }
		});

	}
	
	// UI related fields -----------------------------------------------------------------------------------------------
	
	String TitleText = null;
	JLabel Title     = null;

	static LineBorder BORDER_RED = new LineBorder(Color.RED,  3);
		
	Border Border_Normal = null;
	
	// Model related fields --------------------------------------------------------------------------------------------
	static final String[] ParamNames = new String[] { "$TPackage", "$CContext" };
	
	private TypeFactory  TFactory;
	private ExecCallBack CallBack;
	private Signature    Signature;
	private Function     Funct = null;
	private boolean      IsCompile;
	
	public String   getExecuteCode() { return this.LNP_Execute.getTextComponent().getText(); }
	public Class<?> getResultClass() { return this.Signature.getReturnType();                }
	
	private void updateTabTitle() {
		if(this.Title == null) return;
		if(this.LNP_Execute.isChanged()) this.Title.setText("<html><b>" + this.TitleText + "</b></html>");
		else                             this.Title.setText(this.TitleText);
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        B_Execute        = new javax.swing.JButton();
        L_ResultClass    = new javax.swing.JLabel();
        SP_CodeAndResult = new javax.swing.JSplitPane();
        SP_Execute       = new javax.swing.JScrollPane();
        
        LNP_Execute      = new LineNumberedTextComponentPanel();
        TP_ResultConsole = new HTMLOutputPane();

        B_Execute.setMnemonic('E');
        B_Execute.setText("Execute");
        B_Execute.setToolTipText("Ctrl+E");
        B_Execute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                B_ExecuteActionPerformed(evt);
            }
        });

        //L_ResultClass.setFont(new java.awt.Font("DejaVu Sans", 0, 18));
        L_ResultClass.setFont(new java.awt.Font("Monospaced", 0, 18));
        L_ResultClass.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        L_ResultClass.setText("<html><center><b>Result: </b>java.lang.Object</center></html>");

        SP_CodeAndResult.setDividerSize(4);
        SP_CodeAndResult.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        SP_CodeAndResult.setResizeWeight(0.5);

        TP_ResultConsole.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        TP_ResultConsole.getTextComponent().setBackground(Color.WHITE);
        TP_ResultConsole.getTextComponent().setToolTipText("Result Console");
        ((JTextPane)TP_ResultConsole.getTextComponent()).addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                TP_ResultConsoleHyperlinkUpdate(evt);
            }
        });

        SP_CodeAndResult.setRightComponent(TP_ResultConsole);

        SP_Execute.setViewportView(LNP_Execute);
        LNP_Execute.useChangeHighlight();

        SP_CodeAndResult.setLeftComponent(SP_Execute);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(SP_CodeAndResult, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 713, Short.MAX_VALUE)
                    .addComponent(B_Execute, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 713, Short.MAX_VALUE)
                    .addComponent(L_ResultClass, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 713, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(L_ResultClass)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SP_CodeAndResult, javax.swing.GroupLayout.DEFAULT_SIZE, 459, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(B_Execute)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
	
	static String LocationFinderSpec =
				"#def Simple parser LocationFinder:\n" +
				"\n" +
				"#Checker:\n" +
				"	("+
				"		($Line:~"+
				"			[:NewLine:]Found.at.[:(:]($Col:~[0-9]+~)[:,:].($Row:~[0-9]+~)[:):][:NewLine:]"+
				"			Line.[:#:]($Row;)[:::]"+
				"			~:~"+
				"			($NewLine:~[:NewLine:]~)(#Line:~Found.at.[:(:]($Col:~[0-9]+~)[:,:].($Row:~[0-9]+~)[:):]~)" +
				"			(($NewLine:~[:NewLine:]~)|($Tab:~[:Tab:]~)|($Space:~[: :]~)||.)*"+
				"		~)" +
				"		||" +
				"		(($NewLine:~[:NewLine:]~)|($Tab:~[:Tab:]~)|($Space:~[: :]~)||.)" +
				"	)*"+
				"\n" +
				"#Compiler:\n" +
				"	// @RegParser(Java):\n" +
				"	StringBuffer SB = new StringBuffer();\n" +
				"	for(int i = 0; i < $Result.count(); i++) {\n" +
				"		String Name = ~~[i]~~;\n" +
				"		if(\"$NewLine\".equals(Name))    SB.append(\"<br>\\n\");\n" +
				"		else if(\"$Tab\".equals(Name))   SB.append(\"&nbsp;&nbsp;&nbsp;&nbsp;\");\n" +
				"		else if(\"$Space\".equals(Name)) SB.append(\"&nbsp;\");\n" +
				"		else if(\"#Line\".equals(Name)) {\n" +
				"			String Row = ##[i]##.textOf(\"$Row\");\n" +
				"			String Col = ##[i]##.textOf(\"$Col\");\n" +
				"			SB.append(\"<font color='0000FF'><a href='\").append(Col).append(\"_\").append(Row).append(\"'>\");\n" +
				"			SB.append($$[i]$$).append(\"</a></font>\");\n" +
				"		} else SB.append($$[i]$$);\n" +
				"	}\n" +
				"	return \"<html><font color='FF0000'>\" + SB.toString() + \"</font>\";\n" +
				"\n"+
				"#end def parser;";
	
	static PType LocationFinder = null;
	
	static PType getLocationFinder() {
		if(LocationFinder == null) {
			PTypePackage PTP = new PTypePackage();
			PTP.useCommonKinds();
			LocationFinder = PTP.getType(PTP.addType(LocationFinderSpec));
		}
		return LocationFinder;
	}
	
	private void B_ExecuteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_B_ExecuteActionPerformed
		String Code = this.LNP_Execute.getTextComponent().getText();
				
		if(!this.IsCompile) {
			this.CallBack.setResult(Code, null);
			this.LNP_Execute.save();
			this.updateTabTitle();
			return;
		}
		
		PrintStream OUT = System.out;
		PrintStream ERR = System.err;
		PrintStream Out;
		PrintStream Err;
		
		try {
			this.TP_ResultConsole.clearText();
			Out = this.TP_ResultConsole.newSimpleFormatterPrintStream();
			Err = this.TP_ResultConsole.newSimpleFormatterPrintStream().changeColor(Color.RED);
			
			Out.print("<html><b>--------------------------------------------------------------------------------------------------</b>"); Out.println();
			Out.print("<html><b>--------------------------------------------- Console --------------------------------------------</b>"); Out.println();
			Out.print("<html><b>--------------------------------------------------------------------------------------------------</b>"); Out.println();
			
			// Execute
			Object Result = null;
			try {
				System.setOut(Out);
				System.setErr(Err);
				
				if(this.LNP_Execute.isChanged() || (this.Funct == null)) {
					String  Error    = null;
					boolean IsToStop = false;
					
					ScriptEngine Engine = null;
					try { Engine = ScriptManager.GetEngineFromCode(Code); } catch(Exception E) {}
					if(Engine == null) {
						Error = 
							"Unknown script engine for the code.\n" +
							"The first line of a code should starts with the engine declaration.\n" +
							"For example:\n" +
							"	Ex1: // @Java:\\n\n"+
							"	Ex2: // @JavaScript:\\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; &lt;----- for Java 6.0 and up\n"+
							"	Ex3: // @RegParser(Java):\\n&nbsp;&nbsp;&nbsp;&nbsp;                          &lt;----- for Compiler code only\n";
						IsToStop = true;
					} else {
					
						// Can get the engine so compiler it.
						ProblemContainer PContainer = Engine.newCompileProblemContainer();

						// Compile
						this.Funct = ScriptManager.Instance.newFunction(this.Signature, ParamNames, Code, null, null,
								null, PContainer);
						this.LNP_Execute.save();
						this.updateTabTitle();

						// There are problems with the compilation
						if(PContainer.hasProblem()) {
							Error = PContainer.toString();
							if(PContainer.hasError()) IsToStop = true;
						}
					}

					if(Error != null) {
						// Convert the error message to detect problems and create hyper-link
						Err.println(getLocationFinder().compile(Error));
						if(IsToStop) {
							this.LNP_Execute.setChangeHighlightToRed();
							JOptionPane.showMessageDialog(this.getParent(), "There is a problem with the code.");
							return;
						}
					}
				}
				
				Result = this.Funct.run(
								(this.TFactory == null)?null:this.TFactory.getTPackage(),
								(this.TFactory == null)?null:this.TFactory.getCContext()
							);
				this.CallBack.setResult(Code, Result);
				this.TP_ResultConsole.setBorder(this.Border_Normal);
				
			} catch(Throwable T) {
				T.printStackTrace(System.err);
				this.TP_ResultConsole.setBorder(BORDER_RED);
				
			}
			
			Out.print("<html><b>--------------------------------------------------------------------------------------------------</b>");       Out.println();
			Out.print("<html><b>---------------------------------------------- Result --------------------------------------------</b>");       Out.println();
			Out.print("<html><b>--------------------------------------------------------------------------------------------------</b>");       Out.println();
			Out.print("<html><b>Result Value:</b> " + ((Result == null)?"null"                         :Result.toString()));                    Out.println();
			Out.print("<html><b>Result Class:</b> " + ((Result == null)?Object.class.getCanonicalName():Result.getClass().getCanonicalName())); Out.println();
			Out.print("<html><b>--------------------------------------------------------------------------------------------------</b>");       Out.println();
			
		} catch(Throwable E) {
			this.TP_ResultConsole.setBorder(BORDER_RED);
			JOptionPane.showMessageDialog(this.getParent(), "There is a problem compiling the code: " + E.toString());
			
		} finally {
			System.setOut(OUT);
			System.setErr(ERR);
		}
	}//GEN-LAST:event_B_ExecuteActionPerformed

	private void TP_ResultConsoleHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_TP_ResultConsoleHyperlinkUpdate
		String Location = evt.getDescription();
		String[] Ls = Location.split("_");
		int Col = Integer.parseInt(Ls[0]) - 1;
		int Row = Integer.parseInt(Ls[1]) - 1;
		
		if(Col < 0) Col = 0;
			
		// Find the position in the text
		Element Section = this.LNP_Execute.getTextComponent().getDocument().getDefaultRootElement();
		int ParaCount = Section.getElementCount();
		if(Row > ParaCount) return;
			
		try {
			Element E = Section.getElement(Row);
			int RangeStart = E.getStartOffset();
			int RangeEnd   = E.getEndOffset();
			if((RangeEnd - RangeStart) < Col) return;
			this.LNP_Execute.getTextComponent().setSelectionStart(RangeStart + Col);
			this.LNP_Execute.getTextComponent().setSelectionEnd(RangeStart + Col + 1);
			
			this.TFactory.setStatusBarText(String.format("Row: %d, Col: %d, Start: %d, End: %d", Row, Col, RangeStart, RangeEnd));
		} catch (Exception ex) { return;}
	}//GEN-LAST:event_TP_ResultConsoleHyperlinkUpdate
	
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton     B_Execute;
    private javax.swing.JLabel      L_ResultClass;
    private javax.swing.JSplitPane  SP_CodeAndResult;
    private javax.swing.JScrollPane SP_Execute;

    private LineNumberedTextComponentPanel LNP_Execute;
    
    private HTMLOutputPane TP_ResultConsole;
    // End of variables declaration//GEN-END:variables
	
}
