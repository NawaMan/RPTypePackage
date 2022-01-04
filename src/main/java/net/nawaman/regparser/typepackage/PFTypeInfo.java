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

import javax.swing.event.ChangeEvent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import javax.swing.text.Element;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeListener;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

import net.nawaman.regparser.ParserType;
import net.nawaman.swing.LineNumberedTextComponentPanel;

/**
 * Panel for TypeInfo editor
 *
 * @author Nawapunth Manusitthipol (https://github.com/NawaMan)
 */
@SuppressWarnings("rawtypes")
public class PFTypeInfo extends javax.swing.JPanel {
	
    private static final long serialVersionUID = -8504654167125633097L;

    /** Creates new form PanelTypeInfo */
	public PFTypeInfo() {
		this(null, "", null);
	}
	
	/** Creates new form PanelTypeInfo */
	@SuppressWarnings("unchecked")
    public PFTypeInfo(TypeFactory pTFactory, String pTypeName, JLabel pTitle) {
		initComponents();
		
		this.Title = pTitle;
		
		this.NameBorder    = this.TF_Name.getBorder();
		this.KindBorder    = this.CB_Kind.getBorder();
		this.TextBorder    = this.CB_Text.getBorder();
		this.FlattenBorder = this.CB_Flatten.getBorder();
		this.VerifyBorder  = this.CB_Verify.getBorder();
		this.TF_Name.setMargin(this.TF_Insets);
				
		// Set type factory and type name
		this.TFactory = pTFactory;
		this.TypeName = pTypeName.trim();
		
		// Get kind
		PTypePackage TPackage = this.TFactory.getTPackage();
		if(TPackage.KDatas != null) {
			KDATA: for(String S : TPackage.KDatas.keySet()) {
				for(int i = this.CB_Kind.getItemCount(); --i >= 0;) {
					if(!this.CB_Kind.getItemAt(i).toString().equals(S)) continue;
					continue KDATA;
				}
				this.CB_Kind.addItem(S);
			}
		}
		
		// Prepare properties
		if(this.TypeName.contains("$")) {
			if(this.TypeName.contains("[]")) this.CB_Text.setSelectedIndex(2);
			else                             this.CB_Text.setSelectedIndex(1);
		} else                               this.CB_Text.setSelectedIndex(0);
		
		if(     this.TypeName.contains("+")) this.CB_Flatten.setSelectedIndex(1);
		else if(this.TypeName.contains("*")) this.CB_Flatten.setSelectedIndex(2);
		else                                 this.CB_Flatten.setSelectedIndex(0);
		
		if(     this.TypeName.contains("~")) this.CB_Verify.setSelectedIndex(2);
		else if(this.TypeName.contains("?")) this.CB_Verify.setSelectedIndex(1);
		else                                 this.CB_Verify.setSelectedIndex(0);
		
		// Extract type
		this.PrevTypeName = this.TypeName.substring(
				this.TypeName.contains("$")?1:0,
				this.TypeName.length() -
				(
					(this.TypeName.contains("[]")?2:0) +
					(this.TypeName.contains("?") ?1:0) +
					(this.TypeName.contains("~") ?1:0) +
					(this.TypeName.contains("+") ?1:0) +
					(this.TypeName.contains("*") ?1:0)
				)
			);
		this.TF_Name.setText(this.PrevTypeName);
		
		// Get the type
		ParserType PT = ((this.TFactory == null)||(this.TFactory.getTPackage() == null))
						?null:this.TFactory.getTPackage().type(this.TypeName);
		if(PT == null) {
			JOptionPane.showMessageDialog(this, "The type named '"+this.TypeName+"' does not exist.");
			return;
		}
		
		// Set definition
		this.PrevText = this.TFactory.getTPackage().getTypeToString(this.TypeName);
		int Start = this.PrevText.indexOf("\n");
		int End   = this.PrevText.lastIndexOf("\n");
		this.PrevText = this.PrevText.substring(Start + 1, End).trim();
		this.LNP_TypeDef.getTextComponent().setText(this.PrevText);
		this.LNP_TypeDef.save();
		
		// Set Kind
		PTSpec Spec = this.TFactory.getTPackage().getTypeSpec(this.TypeName);
		this.PrevKindName = Spec.getKind();
		for(int i = this.CB_Kind.getItemCount(); --i >= 0; ) {
			Object Item = this.CB_Kind.getItemAt(i);
			if(Item == null) continue;
			if(Item.equals(this.PrevKindName)) {
				this.CB_Kind.setSelectedIndex(i);
				break;
			}
		}
		
		// Set the tool tip
		this.setKindToolTip();
		
		this.LNP_TypeDef.getTextComponent().requestFocus();
		this.LNP_TypeDef.useChangeHighlight();
		this.LNP_TypeDef.getTextComponent().addKeyListener(new java.awt.event.KeyAdapter() {
			@Override public void keyReleased(KeyEvent evt) {
				if(evt.getKeyCode() == KeyEvent.VK_ENTER) {
					if(evt.isControlDown() && !evt.isAltDown() && !evt.isShiftDown() && !evt.isMetaDown()) {
						LNP_TypeDef.save();
						B_TypeChange.doClick();
					} else {
						JTextComponent JTC = LNP_TypeDef.getTextComponent();
						
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
		
		// Make it sktip the line with the start of the new data
		this.LNP_TypeDef.setRestartLineConfig(new LineNumberedTextComponentPanel.RestartLineConfig() {
			
			Pattern P = null;
			
			@Override public boolean isSkipFirstLine() { return true; }

			@Override public boolean isRestart(int I, LineNumberedTextComponentPanel pSource) {
				if(I < 0) return false;
				String TextAtLine = null;
				
				Element Section = pSource.getTextComponent().getDocument().getDefaultRootElement();

				// Get number of paragraphs.
				// In a text pane, a span of characters terminated by single
				// newline is typically called a paragraph.
				int paraCount = Section.getElementCount();
				if(I >= paraCount) return false;
					
				try {
					Element e = Section.getElement(I);
					int rangeStart = e.getStartOffset();
					int rangeEnd = e.getEndOffset();
					TextAtLine = pSource.getTextComponent().getText(rangeStart, rangeEnd-rangeStart);
				} catch (Exception ex) { return false; }
				
				if((TextAtLine.length() < 2) && (TextAtLine.charAt(0) != '#')) return false;
				
				// Admit it, Java RegExt is faster.
				if(P == null) P = Pattern.compile("^#[a-zA-Z_][a-zA-Z_0-9]*:");
					
				Matcher M = P.matcher(TextAtLine);
				return M.find();
			}
		});
		this.LNP_TypeDef.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) { updateTabTitle(); }
		});
	}
	
	// UI related fields -----------------------------------------------------------------------------------------------
	private Border      NameBorder    = null;
	private Border      KindBorder    = null;
	private Border      TextBorder    = null;
	private Border      FlattenBorder = null;
	private Border      VerifyBorder  = null;
	private LineBorder  Border_Red    = new LineBorder(Color.RED,   3);
    private LineBorder  Border_Blue   = new LineBorder(Color.BLUE,  3);
    private LineBorder  Border_Black  = new LineBorder(Color.BLACK, 1);
	
	private Insets TF_Insets = new Insets(0, 3, 0, 3);
	
	JLabel Title = null;
	
	// Model related fields --------------------------------------------------------------------------------------------
	private TypeFactory TFactory;
	
	private String TypeName = "";
	
    private String PrevTypeName = "";
    private String PrevKindName = "";
    private String PrevText     = "";
	
	private void setKindToolTip() {
		switch(this.CB_Kind.getSelectedIndex()) {
			case 0:
				this.CB_Kind.setToolTipText("<html>#Checker: (as RegParser)<br />#Verifier: (as Code)<br />#Compiler: (as Code)");
				break;
			case 1:
				this.CB_Kind.setToolTipText("<html>#Checker: (as RegParser)<br />#ErrMessage: (as String (trimed))<br />#IsFatal: (as true or false (trimed))");
				break;
		}
		this.LNP_TypeDef.setToolTipText(this.CB_Kind.getToolTipText());
		this.LNP_TypeDef.getTextComponent().setToolTipText(this.LNP_TypeDef.getToolTipText());
	}
	
	boolean IsTabTitleChanged = false;
	
	private void updateTabTitleToChanged() {
		if(this.Title == null) return;
		this.Title.setText("<html><b>" + this.TypeName + "</b></html>");
		this.IsTabTitleChanged = true;
	}
	private void updateTabTitleToNoChanged() {
		if(this.Title == null) return;
		this.Title.setText(this.TypeName);
		this.IsTabTitleChanged = false;
	}
	
	private void updateTabTitle() {
		if(this.Title == null) return;
		
		if(this.IsTabTitleChanged != (
				(this.CB_Flatten.getBorder() == this.Border_Blue) ||
				(this.CB_Verify.getBorder()  == this.Border_Blue) ||
				(this.CB_Text.getBorder()    == this.Border_Blue) ||
				(this.CB_Kind.getBorder()    == this.Border_Blue) ||
				(this.TF_Name.getBorder()    == this.Border_Blue) ||
				(this.LNP_TypeDef.isChanged())
			)) {
			if(this.IsTabTitleChanged)
				 this.updateTabTitleToNoChanged();
			else this.updateTabTitleToChanged();
		}
	}
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    @SuppressWarnings("unchecked")
    private void initComponents() {

        P_TypeInfo = new javax.swing.JPanel();
        B_TypeChange = new javax.swing.JButton();
        CB_Kind = new javax.swing.JComboBox();
        L_Name = new javax.swing.JLabel();
        TF_Name = new javax.swing.JTextField();
        L_Kind = new javax.swing.JLabel();
        SP_TypeDef = new javax.swing.JScrollPane();
        LNP_TypeDef = new net.nawaman.swing.LineNumberedTextComponentPanel();
        CB_Flatten = new javax.swing.JComboBox();
        CB_Verify = new javax.swing.JComboBox();
        CB_Text = new javax.swing.JComboBox();

        P_TypeInfo.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));

        B_TypeChange.setMnemonic('A');
        B_TypeChange.setText("Apply Change");
        B_TypeChange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                B_TypeChangeActionPerformed(evt);
            }
        });

        CB_Kind.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Simple", "Error" }));
        CB_Kind.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                CB_KindItemStateChanged(evt);
            }
        });

        L_Name.setText("Name");

        TF_Name.setToolTipText("Name of the type (alphabets or number only)");
        TF_Name.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_NameKeyReleased(evt);
            }
        });

        L_Kind.setText("Kind");

        SP_TypeDef.setViewportView(LNP_TypeDef);

        CB_Flatten.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Flatten", "Single", "Always" }));
        CB_Flatten.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CB_FlattenActionPerformed(evt);
            }
        });

        CB_Verify.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "No Verification", "Self-Contain", "Boundary" }));
        CB_Verify.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CB_VerifyActionPerformed(evt);
            }
        });

        CB_Text.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Tree", "Text", "Collective Text" }));
        CB_Text.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CB_TextActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout P_TypeInfoLayout = new javax.swing.GroupLayout(P_TypeInfo);
        P_TypeInfo.setLayout(P_TypeInfoLayout);
        P_TypeInfoLayout.setHorizontalGroup(
            P_TypeInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(B_TypeChange, javax.swing.GroupLayout.DEFAULT_SIZE, 850, Short.MAX_VALUE)
            .addGroup(P_TypeInfoLayout.createSequentialGroup()
                .addComponent(L_Name)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(TF_Name, javax.swing.GroupLayout.DEFAULT_SIZE, 232, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(L_Kind)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CB_Kind, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CB_Text, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CB_Flatten, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CB_Verify, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(SP_TypeDef, javax.swing.GroupLayout.DEFAULT_SIZE, 850, Short.MAX_VALUE)
        );
        P_TypeInfoLayout.setVerticalGroup(
            P_TypeInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, P_TypeInfoLayout.createSequentialGroup()
                .addGroup(P_TypeInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(L_Name)
                    .addComponent(CB_Verify, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CB_Flatten, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CB_Text, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CB_Kind, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(L_Kind)
                    .addComponent(TF_Name, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SP_TypeDef, javax.swing.GroupLayout.DEFAULT_SIZE, 394, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(B_TypeChange))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(P_TypeInfo, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(P_TypeInfo, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

	private void CB_KindItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_CB_KindItemStateChanged
		if(evt.getStateChange() != java.awt.event.ItemEvent.SELECTED) return;
		this.CB_Kind.setBorder(
			this.CB_Kind.getSelectedItem().equals(this.PrevKindName)
			?this.Border_Black:this.Border_Blue
		);
		this.setKindToolTip();
		this.updateTabTitle();
	}//GEN-LAST:event_CB_KindItemStateChanged

	private void TF_NameKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_NameKeyReleased
		this.TF_Name.setBorder(
			this.TF_Name.getText().trim().equals(this.PrevTypeName)
			?this.NameBorder:this.Border_Blue
		);
		this.TF_Name.setMargin(this.TF_Insets);
		
		if(evt.isControlDown() && (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER)) this.B_TypeChange.doClick();
		this.updateTabTitle();
	}//GEN-LAST:event_TF_NameKeyReleased

	JScrollPane JSP = null;
	JTextArea   JTA = null;
	
	static Font MonoFont = null;
	
	private void B_TypeChangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_B_TypeChangeActionPerformed
		// Ensure type name is in the correct format
		boolean IsNameWellFormed = true;
		String  TName            = this.TF_Name.getText().trim();
		if(TName.length() == 0) IsNameWellFormed = false;
		else {
			char C = TName.charAt(0);
			if(((C >= 'a') && (C <= 'z'))||((C >= 'A') && (C <= 'Z'))||(C == '_')) {
				for(int i = TName.length(); --i >= 0; ) {
					C = TName.charAt(i);
					if(((C >= 'a') && (C <= 'z'))||((C >= 'A') && (C <= 'Z'))||((C >= '0') && (C <= '9'))||(C == '_')) {
						continue;
					} else {
						IsNameWellFormed = false;
						break;
					}
				}
			} else IsNameWellFormed = false;
		}
		
		if(!IsNameWellFormed) {
			String Err = "The type named '"+TName+"' is mal-formed.";
			JOptionPane.showMessageDialog(this.TFactory, Err);
			this.TFactory.setStatusBarText(Err);
			this.TF_Name.setBorder(this.Border_Red);
			return;
		}
		
		// Save the old def
		String OldDef = this.TFactory.getTPackage().getTypeToString(this.TypeName);
		try {
			this.TFactory.getTPackage().IsFrozen = false;
			this.TFactory.getTPackage().removeType(this.TypeName);
		} finally { this.TFactory.getTPackage().IsFrozen = true; }
		
		// Create the new type def
		String NewDef = null;
		
		int Index = this.CB_Text.getSelectedIndex();
		switch(Index) {
			case 2: TName += "[]";
			case 1: TName  = "$" + TName; break;
		}
		Index = this.CB_Flatten.getSelectedIndex();
		switch(Index) {
			case 1: TName += "+"; break;
			case 2: TName += "*"; break;
		}
		Index = this.CB_Verify.getSelectedIndex();
		switch(Index) {
			case 1: TName += "?"; break;
			case 2: TName += "~"; break;
		}
		
		if(this.TFactory.getTPackage().type(TName) != null) {
			JOptionPane.showMessageDialog(this, "The type named '"+TName+"' is already exist.");
			// Revert
			try {
				this.TFactory.getTPackage().IsFrozen = false;
				this.TFactory.getTPackage().addType(OldDef);
			} finally { this.TFactory.getTPackage().IsFrozen = true; }
			return;
		}
		
		NewDef =  "#def " + this.CB_Kind.getSelectedItem().toString() + " parser " + TName + ":\n\n";
		NewDef += this.LNP_TypeDef.getTextComponent().getText();
		NewDef += "\n\n#end def parser;";
		
		// Try to add in to TPackage
		try {
			// If success, notify
			// If fail, add the old one and notify
			String N = null;
			try {
				this.TFactory.getTPackage().IsFrozen = false;
				N = this.TFactory.getTPackage().addType(NewDef);
			} finally { this.TFactory.getTPackage().IsFrozen = true; }
			if(N != null) this.TFactory.setStatusBarText("Type " + TName + " is updated.");
			else {
				this.TFactory.setStatusBarText("Fail to update the type " + TName + ".");
				this.LNP_TypeDef.setChangeHighlightToRed();
				// Revert
				try {
					this.TFactory.getTPackage().IsFrozen = false;
					this.TFactory.getTPackage().addType(OldDef);
				} finally { this.TFactory.getTPackage().IsFrozen = true; }
				return;
			}
			
			// Clear the Border
			this.TF_Name   .setBorder(this.NameBorder);
			this.CB_Kind   .setBorder(this.KindBorder);
			this.CB_Text   .setBorder(this.TextBorder);
			this.CB_Flatten.setBorder(this.FlattenBorder);
			this.CB_Verify .setBorder(this.VerifyBorder);
			this.LNP_TypeDef.save();
			
			this.TFactory.updateType(this.TypeName, TName);

			// Now the new values are the prev values
			this.TypeName     = TName;
			this.PrevTypeName = this.TF_Name.getText();
			this.PrevKindName = this.CB_Kind.getSelectedItem().toString();
			
			if(this.Title != null) {
				// Change this so that the title can be update properly
				this.Title.setText(this.TypeName);
				this.updateTabTitleToNoChanged();
			}
		} catch(Exception E) {			
			
			// TODO - Make change the error message to make it easier to read. Especially, the first line error
			String Err = E.toString();
			
			ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
			PrintWriter PW = new PrintWriter(BAOS, true);
			E.printStackTrace(PW);
			PW.flush();
			
			Err = "The type named '"+TName+"' fail: " + Err + "\n" + BAOS.toString();
			
			if(MonoFont == null)
			    //MonoFont = new Font("Bitstream Vera Sans Mono", 0, 15);
                MonoFont = new Font("Monospaced", 0, 15);
			
			if(this.JTA == null) {
				this.JTA = new JTextArea();
				this.JTA.setTabSize(4);
				this.JTA.setFont(MonoFont);
			}
			this.JTA.setText(Err);
			
			if(this.JSP == null) {
				this.JSP = new JScrollPane(this.JTA);
				this.JSP.setMaximumSize(new Dimension(700, 500));
				this.JSP.setPreferredSize(new Dimension(700, 500));
			}
			
			JOptionPane.showMessageDialog(this.TFactory, this.JSP);
			
			this.TFactory.setStatusBarText(Err);
			// Revert
			try {
				this.TFactory.getTPackage().IsFrozen = false;
				this.TFactory.getTPackage().removeType(TName);
				this.TFactory.getTPackage().addType(OldDef);
			} finally { this.TFactory.getTPackage().IsFrozen = true; }

			TFactory.setStatusBarText("ERROR: " + E.toString());
			
			if(this.TF_Name   .getBorder() == this.Border_Blue) this.TF_Name   .setBorder(this.Border_Red);
			if(this.CB_Kind   .getBorder() == this.Border_Blue) this.CB_Kind   .setBorder(this.Border_Red);
			if(this.CB_Text   .getBorder() == this.Border_Blue) this.CB_Text   .setBorder(this.Border_Red);
			if(this.CB_Flatten.getBorder() == this.Border_Blue) this.CB_Flatten.setBorder(this.Border_Red);
			if(this.CB_Verify .getBorder() == this.Border_Blue) this.CB_Verify .setBorder(this.Border_Red);
			this.LNP_TypeDef.setChangeHighlightToRed();
			
		}
	}//GEN-LAST:event_B_TypeChangeActionPerformed

	private void CB_FlattenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CB_FlattenActionPerformed
		int Index = this.CB_Flatten.getSelectedIndex();
		if(     this.TypeName.contains("+")) this.CB_Flatten.setBorder((Index != 1)?this.Border_Blue:this.FlattenBorder);
		else if(this.TypeName.contains("*")) this.CB_Flatten.setBorder((Index != 2)?this.Border_Blue:this.FlattenBorder);
		else                                 this.CB_Flatten.setBorder((Index != 0)?this.Border_Blue:this.FlattenBorder);
		this.updateTabTitle();
}//GEN-LAST:event_CB_FlattenActionPerformed

	private void CB_VerifyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CB_VerifyActionPerformed
		int Index = this.CB_Verify.getSelectedIndex();
		if(     this.TypeName.contains("?")) this.CB_Verify.setBorder((Index != 1)?this.Border_Blue:this.VerifyBorder);
		else if(this.TypeName.contains("~")) this.CB_Verify.setBorder((Index != 2)?this.Border_Blue:this.VerifyBorder);
		else                                 this.CB_Verify.setBorder((Index != 0)?this.Border_Blue:this.VerifyBorder);
		this.updateTabTitle();
	}//GEN-LAST:event_CB_VerifyActionPerformed

	private void CB_TextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CB_TextActionPerformed
		int Index = this.CB_Text.getSelectedIndex();
		if(     this.TypeName.contains("[]")) this.CB_Text.setBorder((Index != 2)?this.Border_Blue:this.TextBorder);
		else if(this.TypeName.contains("$"))  this.CB_Text.setBorder((Index != 1)?this.Border_Blue:this.TextBorder);
		else                                  this.CB_Text.setBorder((Index != 0)?this.Border_Blue:this.TextBorder);
		this.updateTabTitle();
	}//GEN-LAST:event_CB_TextActionPerformed
		
	
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton B_TypeChange;
    private javax.swing.JComboBox CB_Flatten;
    private javax.swing.JComboBox CB_Kind;
    private javax.swing.JComboBox CB_Text;
    private javax.swing.JComboBox CB_Verify;
    private net.nawaman.swing.LineNumberedTextComponentPanel LNP_TypeDef;
    private javax.swing.JLabel L_Kind;
    private javax.swing.JLabel L_Name;
    private javax.swing.JPanel P_TypeInfo;
    private javax.swing.JScrollPane SP_TypeDef;
    private javax.swing.JTextField TF_Name;
    // End of variables declaration//GEN-END:variables
	
}
