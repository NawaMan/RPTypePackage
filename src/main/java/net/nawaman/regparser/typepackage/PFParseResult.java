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
import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import javax.swing.JOptionPane;
import net.nawaman.regparser.PType;
import net.nawaman.regparser.PTypeRef;
import net.nawaman.regparser.Quantifier;
import net.nawaman.regparser.RegParser;
import net.nawaman.regparser.result.ParseResult;
import net.nawaman.regparser.swing.ParseResultTreeTableModel.JParseResultTreeTable;
import net.nawaman.swing.LineNumberedTextComponentPanel;
import net.nawaman.swing.text.HTMLOutputPane;

/**
 * ParseResult Panel
 *
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 */
public class PFParseResult extends javax.swing.JPanel {
	
    private static final long serialVersionUID = 6662165972907141490L;

    /** Creates new form PFParseResult */
	public PFParseResult(TypeFactory pTFactory) {
		this(pTFactory, null, null);
	}
	/** Creates new form PFParseResult */
	public PFParseResult(TypeFactory pTFactory, String pRegParserStr, RegParser pRegParser) {
		This = this;
		
		initComponents();
		
		// Default parser value
		if(pRegParserStr == null) pRegParserStr = ".*";
		if(pRegParser    == null) pRegParser    = RegParser.newRegParser(pRegParserStr);
		
		this.TFactory = pTFactory;
		
		if((pRegParser != null) && (pRegParser.getEntryCount() == 1) && (pRegParser.getEntryAt(0).typeRef() != null)
				&& (pRegParser.getEntryAt(0).getQuantifier() == Quantifier.One)) {
			PTypeRef TRef = pRegParser.getEntryAt(0).typeRef();
			this.TypeName  = TRef.name();
			this.TypeParam = TRef.parameter();
		}
		
		if(this.TypeName != null) {	// Given the type name - so it is a type parsing (can compile)
			// Set the parser and the flag
			this.Parser = RegParser.newRegParser(new PTypeRef.Simple(this.TypeName, this.TypeParam));
			this.IsTypeParsing = true;
			
			String T = "!" + this.TypeName + ((this.TypeParam == null)?"":"("+this.TypeParam+")")+"!";
			this.B_Process.setToolTipText(B_Parse_Tooltip_Prefix + " RegParser type " + T);
			this.B_Process.setText("Parse ~ " + T);
			
		} else {				// Given the no type name - so it is a parser parsing (can compile)
			
			// Set the parser and the flag
			this.Parser        = pRegParser;
			this.IsTypeParsing = false;
			if(pRegParserStr.indexOf("\n") == -1) {
				this.B_Process.setToolTipText(B_Parse_Tooltip_Prefix + " RegParser ~ "+pRegParserStr);
				this.B_Process.setText("Parse ~ "+pRegParserStr);
			} else {
				this.B_Process.setToolTipText(B_Parse_Tooltip_Prefix + " RegParser ~<br />"+pRegParserStr);
				this.B_Process.setText("Parse ~ "+pRegParserStr.substring(0, pRegParserStr.indexOf("\n")) + " ... ");
			}
		}
		
		// Remember the parse string
		this.ParserStr = pRegParserStr;
		
		this.LNP_OriginalText.getTextComponent().addKeyListener(new java.awt.event.KeyAdapter() {
			@Override public void keyReleased(KeyEvent evt) {
				if(evt.isControlDown() && (evt.getKeyCode() == KeyEvent.VK_ENTER)) B_Process.doClick();
			}
        });
	}
	
	// UI related fields -----------------------------------------------------------------------------------------------
	static private String B_Parse_Tooltip_Prefix = "<html>Parse the above text with ";
	
	static private PFParseResult This = null;
	
	private boolean        hasTreeResult    = false;
	private boolean        hasDebugResult   = false;
	private boolean        hasCompileResult = false;
	
	// Model related fields --------------------------------------------------------------------------------------------
	private TypeFactory  TFactory      = null;
	private String       TypeName      = null;
	private String       TypeParam     = null;
	private String       ParserStr     = null;
	private RegParser    Parser        = null;
	private ParseResult  PResult       = null;
	private boolean      IsTypeParsing = false;
	
	/** Returns the RegParser String */
	String getParserStr() {
		return this.ParserStr;
	}
	
	/** Parse the result */
	private void parse() {
		// Parse the original text
		try { this.PResult = this.Parser.parse(this.LNP_OriginalText.getTextComponent().getText(), 0, this.TFactory.getTPackage()); }
		catch(Exception E) { throw new RuntimeException(E); }
		
		// Display as a text
		if(this.PResult == null) this.TA_ParseResultAsText.setText("<< Unmatch >>");
		else {
			this.TA_ParseResultAsText.setText(this.PResult.toString());
			this.LNP_OriginalText.getTextComponent().setSelectionStart(0);
			this.LNP_OriginalText.getTextComponent().setSelectionEnd(this.PResult.endPosition());
		}
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        PM_ParseButton       = new javax.swing.JPopupMenu();
        MI_Edit              = new javax.swing.JMenuItem();
        P_ParseBorder        = new javax.swing.JPanel();
        SP_Parse             = new javax.swing.JSplitPane();
        P_OriginalText       = new javax.swing.JPanel();
        B_Process            = new javax.swing.JButton();
        SP_OriginalText      = new javax.swing.JScrollPane();
        LNP_OriginalText     = new LineNumberedTextComponentPanel();
        TP_Results           = new javax.swing.JTabbedPane();
        SP_ParseResultAsTree = new javax.swing.JScrollPane();
        
        TA_ParseResultAsText = new HTMLOutputPane();
        TA_DebugResult       = new HTMLOutputPane();
        TP_CompileResult     = new HTMLOutputPane();

        MI_Edit.setText("Edit");
        MI_Edit.setToolTipText("Edit this parser");
        MI_Edit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MI_EditActionPerformed(evt);
            }
        });
        PM_ParseButton.add(MI_Edit);

        P_ParseBorder.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));

        SP_Parse.setDividerLocation(120);
        SP_Parse.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        B_Process.setMnemonic('P');
        B_Process.setText("Parse");
        B_Process.setToolTipText("");
        B_Process.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent evt) {
                B_ProcessMousePressed(evt);
            }
        });
        B_Process.addActionListener(new java.awt.event.ActionListener() {
        	@Override public void actionPerformed(java.awt.event.ActionEvent evt) {
                B_ProcessActionPerformed(evt);
            }
        });

        SP_OriginalText.setViewportView(LNP_OriginalText);

        javax.swing.GroupLayout P_OriginalTextLayout = new javax.swing.GroupLayout(P_OriginalText);
        P_OriginalText.setLayout(P_OriginalTextLayout);
        P_OriginalTextLayout.setHorizontalGroup(
            P_OriginalTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(B_Process, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 759, Short.MAX_VALUE)
            .addComponent(SP_OriginalText, javax.swing.GroupLayout.DEFAULT_SIZE, 759, Short.MAX_VALUE)
        );
        P_OriginalTextLayout.setVerticalGroup(
            P_OriginalTextLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, P_OriginalTextLayout.createSequentialGroup()
                .addComponent(SP_OriginalText, javax.swing.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(B_Process))
        );

        SP_Parse.setLeftComponent(P_OriginalText);

        TP_Results.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                TP_ResultsStateChanged(evt);
            }
        });

        TA_ParseResultAsText.getTextComponent().setBackground(Color.WHITE);

        TP_Results.addTab("Parse Result as Text", TA_ParseResultAsText);
        TP_Results.addTab("Parse Result as Tree", SP_ParseResultAsTree);

        //TA_DebugResult.setFont(new java.awt.Font("Bitstream Vera Sans Mono", 0, 15));
        TA_DebugResult.setFont(new java.awt.Font("Monospaced", 0, 15));
        TA_DebugResult.getTextComponent().setBackground(Color.WHITE);
        TA_DebugResult.setTabSize(4);

        TP_Results.addTab("Debug", TA_DebugResult);

        TP_CompileResult.getTextComponent().setBackground(Color.WHITE);
        //TP_CompileResult.setFont(new java.awt.Font("Bitstream Vera Sans Mono", 0, 15));
        TP_CompileResult.setFont(new java.awt.Font("Monospaced", 0, 15));

        TP_Results.addTab("Compile Result", TP_CompileResult);

        SP_Parse.setRightComponent(TP_Results);

        javax.swing.GroupLayout P_ParseBorderLayout = new javax.swing.GroupLayout(P_ParseBorder);
        P_ParseBorder.setLayout(P_ParseBorderLayout);
        P_ParseBorderLayout.setHorizontalGroup(
            P_ParseBorderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(SP_Parse, javax.swing.GroupLayout.DEFAULT_SIZE, 759, Short.MAX_VALUE)
        );
        P_ParseBorderLayout.setVerticalGroup(
            P_ParseBorderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(SP_Parse, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(P_ParseBorder, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(P_ParseBorder, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

	private void B_ProcessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_B_ProcessActionPerformed
		this.PResult          = null;
		this.hasTreeResult    = false;
		this.hasDebugResult   = false;
		this.hasCompileResult = false;
		int Selected = this.TP_Results.getSelectedIndex();
		this.TP_Results.setSelectedIndex(0);
		
		// Do the re-parsing
		EventQueue.invokeLater(new Runnable() { public void run() {
			try { parse(); }
			catch(Exception E) {
				ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
				PrintStream PS = new PrintStream(BAOS);
				E.printStackTrace(PS);
				PS.flush();
				PS.close();
				
				TFactory.setStatusBarText("ERROR: " + E.toString());
				JOptionPane.showMessageDialog(This, "ERROR: " + E.toString() + "\n" + BAOS.toString());
			}
		} });
		
		this.TP_Results.setSelectedIndex(Selected);
	}//GEN-LAST:event_B_ProcessActionPerformed

	private void TP_ResultsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_TP_ResultsStateChanged
		int ResultIndex = this.TP_Results.getSelectedIndex();
		
		// Parse Result as Text - Do nothing because the result should be obtained since B_Process was clicked.
		if(ResultIndex == 0) return;
		
		// Parse Result as Tree
		if((ResultIndex == 1) && !this.hasTreeResult) {
			// Set the current parse result to the parse result tree
    		SP_ParseResultAsTree.setViewportView(new JParseResultTreeTable(PResult));
			this.hasTreeResult = true;
		}
		
		// Debug Result
		if((ResultIndex == 2) && !this.hasDebugResult) {
			// Parse again with Debug flag on.
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
					PrintStream           PS   = new PrintStream(BAOS); 
        			
					try {
						// Parse and save the debug value
                    	RegParser.DebugMode        = true;
                    	RegParser.DebugPrintStream = PS;
						parse();
						TFactory.setStatusBarText("Parse success.");
					} catch(Exception E) {
						TA_DebugResult.setText("<< ERROR!!! - "+E.toString()+" >>");
						TFactory.setStatusBarText("ERROR: " + E.toString());
					} finally {
                    	RegParser.DebugMode        = false;
                    	RegParser.DebugPrintStream = null;
					}
					
                    // Highlight the match and show the parse result
                    PS.println("--------------------------------- Result ---------------------------------");
                    if(PResult != null) PS.println(PResult.toString());
                    else                PS.println("<< !!! UN MATCH !!! >>");
                    
					TA_DebugResult.setText(BAOS.toString());
        		}
        	});
			
			// Set debug result flag so that we do not need to do this again
			this.hasDebugResult = true;
		}
		
		// Compile Result
		if((ResultIndex == 3) && !this.hasCompileResult) {
			if(this.IsTypeParsing) {
				// Do the compilation
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						if(PResult == null) {
							TFactory.setStatusBarText("<< !!! UN MATCH !!! >>");
							TP_CompileResult.setText("&lt;&lt; !!! UN MATCH !!! &gt;&gt;");
							return;
						}
						
						PTypePackage TPackage = TFactory.getTPackage();
						PType Type = TPackage.getType(TypeName);
						if(Type == null) {
							TFactory.setStatusBarText("<< ERROR!! - The RefParser Type named `" + TypeName + "` does not exist.>>");
							TP_CompileResult.setText("&lt;&lt; !!! ERROR !!! &gt;&gt;");
							return;
						}
						
						Object      Result = null;
						PrintStream OUT    = System.out;
						PrintStream ERR    = System.err;
						PrintStream Out;
						PrintStream Err;

						try {
							TP_CompileResult.clearText();
							Out = TP_CompileResult.newSimpleFormatterPrintStream();
							Err = TP_CompileResult.newSimpleFormatterPrintStream().changeColor(Color.RED);
							
							try {
								Out.println(
									"----------------------------------------------------------------------------\n"+
									"---------------------------------- Console ---------------------------------\n"+
									"----------------------------------------------------------------------------"
								);
								
								System.setOut(Out);
								System.setErr(Err);
								
								Result = Type.compile(PResult, 0, TypeParam, TFactory.getCContext(), TPackage);
								TFactory.setStatusBarText("Parse success.");
							} catch(Exception E) {							
								TFactory.setStatusBarText("<< ERROR!! - "+E.toString()+">>");
	
								Out.print(
									"\n" +
									"----------------------------------------------------------------------------\n"+
									"----------------------------------- Error! ---------------------------------\n"+
									"----------------------------------------------------------------------------\n"
								);
								E.printStackTrace(System.err);
								Out.print( 
									"\n----------------------------------------------------------------------------"
								);
								TFactory.setStatusBarText("ERROR: " + E.toString());
								return;
							}

							Out.print(
								"\n" +
								"----------------------------------------------------------------------------\n" +
								"----------------------------------- Result ---------------------------------\n" +
								"----------------------------------------------------------------------------\n" +
								"Type: !" + TypeName + ((TypeParam == null)?"":"("+TypeName+")") + "!\n" +
								"Result Class: " + ((Result == null)?"Object":Result.getClass().getCanonicalName()) + "\n" +
								"Result Value: --------------------------------------------------------------\n");
							Out.print((Result == null)?"null":Result.toString());
							Out.print(
								"\n----------------------------------------------------------------------------"+
								"\n"+
								"\n"+
								((TFactory.getCContext() == null)?"NO Compile Context":TFactory.getCContext().toString())
							);
						} finally {
							System.setOut(OUT);
							System.setErr(ERR);
						}
					}
				});
				
			} else {	// Cannot compile
				this.TP_CompileResult.setText("<< No Compile Result - The current RegParser is not a type>>");
			}
			
			this.hasCompileResult = true;
		}
	}//GEN-LAST:event_TP_ResultsStateChanged

	private void MI_EditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MI_EditActionPerformed
		this.TFactory.setParserText(this.ParserStr);
	}//GEN-LAST:event_MI_EditActionPerformed

	private void B_ProcessMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_B_ProcessMousePressed
		if(evt.isPopupTrigger()) this.PM_ParseButton.show(evt.getComponent(), evt.getX(), evt.getY());
	}//GEN-LAST:event_B_ProcessMousePressed
		
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton     B_Process;
    private javax.swing.JMenuItem   MI_Edit;
    private javax.swing.JPopupMenu  PM_ParseButton;
    private javax.swing.JPanel      P_OriginalText;
    private javax.swing.JPanel      P_ParseBorder;
    private javax.swing.JScrollPane SP_OriginalText;
    private javax.swing.JSplitPane  SP_Parse;
    private javax.swing.JScrollPane SP_ParseResultAsTree;
    private javax.swing.JTabbedPane TP_Results;
    
    private LineNumberedTextComponentPanel LNP_OriginalText;

    private HTMLOutputPane TA_DebugResult;
    private HTMLOutputPane TP_CompileResult;
    private HTMLOutputPane TA_ParseResultAsText;
    // End of variables declaration//GEN-END:variables
	
}
