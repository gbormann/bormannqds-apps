package com.bormannqds.apps.wjh.ptatool.gui;

import com.bormannqds.lib.bricks.gui.AbstractPanel;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class ButtonPanel extends AbstractPanel {

	/**
	 * Create the panel.
	 */
	public ButtonPanel(final Action closeAction) {
		super(new FlowLayout(FlowLayout.CENTER));
		initialise(closeAction);
	}

	public JButton getDefaultButton() {
		return defaultButton;
	}
	
	// -------- Private ----------

	private void setDefaultButton(final JButton defaultButton) {
		this.defaultButton = defaultButton;
	}

	private void initialise(final Action closeAction) {
		{
			JButton closeButton = new JButton(closeAction);
			add(closeButton);
			setDefaultButton(closeButton);
		}
	}

	private JButton defaultButton;
}
