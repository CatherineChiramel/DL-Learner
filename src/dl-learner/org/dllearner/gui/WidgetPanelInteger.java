package org.dllearner.gui;

/**
 * Copyright (C) 2007-2008, Jens Lehmann
 *
 * This file is part of DL-Learner.
 * 
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;

import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;

import org.dllearner.core.Component;
import org.dllearner.core.config.ConfigEntry;
import org.dllearner.core.config.ConfigOption;
import org.dllearner.core.config.IntegerConfigOption;
import org.dllearner.core.config.InvalidConfigOptionValueException;

/**
 * Panel for option Integer, defined in
 * org.dllearner.core.config.IntegerConfigOption.
 * 
 * @author Tilo Hielscher
 * 
 */
public class WidgetPanelInteger extends WidgetPanelAbstract implements
	ActionListener {

    private static final long serialVersionUID = -1802111225835164644L;

    private Config config;
    private ConfigOption<?> configOption;
    private JLabel nameLabel;
    private JPanel widgetPanel = new JPanel();
    private JButton setButton = new JButton("Set");
    private Component component;
    private Component oldComponent;
    private Class<? extends Component> componentOption;

    private Integer value;
    private JTextField integerField = new JTextField(3);

    public WidgetPanelInteger(Config config, Component component,
	    Component oldComponent, Class<? extends Component> componentOption,
	    ConfigOption<?> configOption) {
	this.config = config;
	this.configOption = configOption;
	this.component = component;
	this.oldComponent = oldComponent;
	this.componentOption = componentOption;

	showLabel(); // name of option and tooltip
	showThingToChange(); // textfield, setbutton
	add(widgetPanel, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == setButton) {
	    setEntry();
	}
    }

    @Override
    public void showLabel() {
	nameLabel = new JLabel(configOption.getName());
	nameLabel.setToolTipText(configOption.getDescription());
	widgetPanel.add(nameLabel);
    }

    @Override
    public void showThingToChange() {
	if (component != null) {
	    // IntegerConfigOption
	    if (configOption.getClass().toString().contains(
		    "IntegerConfigOption")) {
		// previous set value
		if (configOption != null) {
		    value = (Integer) config.getComponentManager()
			    .getConfigOptionValue(component,
				    configOption.getName());
		}
		// previous set value from old
		if (component != null && oldComponent != null) {
		    if (oldComponent.getClass().equals(component.getClass())) {
			value = (Integer) config.getComponentManager()
				.getConfigOptionValue(oldComponent,
					configOption.getName());
			if (value == null)
			    value = 0;
			else {
			    integerField.setText(value.toString());
			    setEntry();
			}
		    }
		}
		// default value
		else if (configOption.getDefaultValue() != null) {
		    value = (Integer) configOption.getDefaultValue();
		}
		// value == null
		if (value == null) {
		    value = 0;
		}
		integerField.setText(value.toString());
		integerField.setToolTipText(configOption
			.getAllowedValuesDescription());
		setButton.addActionListener(this);
		widgetPanel.add(integerField);
		widgetPanel.add(setButton);
	    }
	    // UNKNOWN
	    else {
		JLabel notImplementedLabel = new JLabel("not an integer");
		notImplementedLabel.setForeground(Color.RED);
		widgetPanel.add(notImplementedLabel);
	    }
	} else { // configOption == NULL
	    JLabel noConfigOptionLabel = new JLabel("no instance (Integer)");
	    noConfigOptionLabel.setForeground(Color.MAGENTA);
	    widgetPanel.add(noConfigOptionLabel);
	}
    }

    @Override
    public void setEntry() {
	IntegerConfigOption specialOption;
	value = Integer.parseInt(integerField.getText()); // get from input
	specialOption = (IntegerConfigOption) config.getComponentManager()
		.getConfigOption(componentOption, configOption.getName());
	try {
	    ConfigEntry<Integer> specialEntry = new ConfigEntry<Integer>(
		    specialOption, value);
	    config.getComponentManager().applyConfigEntry(component,
		    specialEntry);
	    System.out.println("set Integer: " + configOption.getName() + " = "
		    + value);
	} catch (InvalidConfigOptionValueException s) {
	    s.printStackTrace();
	}
    }
}
