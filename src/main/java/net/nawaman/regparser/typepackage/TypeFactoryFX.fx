package net.nawaman.regparser.typepackage;

import java.lang.*;
import javafx.ui.*;
import javax.swing.*;

var TFs:Frame*;

var TF:Frame = null;
	
TF = Frame {

	title: "Type Factory 1.0"
	width:  1200
	height:  800
	centerOnScreen: true
	lookAndFeel: UIManager.getSystemLookAndFeelClassName()
	content: RootPane {
		menubar: MenuBar {
			menus: Menu {
				text: "File"
				mnemonic: F
				items: MenuItem {
					text: "Quit"
					mnemonic: X
					accelerator: { modifier: CTRL, keyStroke: Q }
					action: operation() {
						TF.hide();
					}
				}
			}
		}
		content: Label {
			text: "Hello World"
		}
	}
	visible: true
};
