package com.windowmarkers;

import java.awt.*;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;

final class BankSidebar extends PluginPanel
{
	private JComboBox<CustomPresets.Preset> customPicker;
	private JComboBox<String> layoutPicker;
	private boolean refreshingLayouts;
	private JLabel status, geContextLabel, viewLabel;
	private JButton addViewButton, clearViewButton, saveViewButton, reviewItemsButton;

	BankSidebar(WindowMarkersPlugin actions, MarkerLegend legend)
	{
		setLayout(new BorderLayout(0, 10));
		JPanel controls = new MarkerForm();
		JPanel toolsPanel = new MarkerForm();
		JPanel viewsPanel = new MarkerForm();
		JPanel guidePanel = new MarkerForm();
		guidePanel.setVisible(false);
		controls.add(new JLabel("Improved Banking"));
		controls.add(new JLabel("<html>Saved per character and RuneLite profile.</html>"));
		status = new JLabel("Open your bank to begin.");
		status.setPreferredSize(new Dimension(210, 48));
		controls.add(status);
		layoutPicker = new JComboBox<>();
		layoutPicker.setRenderer(PlainText.renderer());
		layoutPicker.addActionListener(e ->
		{
			if (!refreshingLayouts)
			{
				actions.switchNamedLayout(layoutPicker.getSelectedIndex());
			}
		});
		JPanel namedLayouts = new MarkerForm();
		namedLayouts.add(new JLabel(
				"<html>Switch the whole collection of Legend groups.<br>Search views are saved in Legend.</html>"));
		namedLayouts.add(layoutPicker);
		JPanel layoutActions = new JPanel(new GridLayout(1, 3, 3, 0));
		JButton newLayout = new JButton("New"), duplicate = new JButton("Copy"), rename = new JButton("Rename");
		newLayout.addActionListener(e -> actions.nameLayout(false, false));
		duplicate.addActionListener(e -> actions.nameLayout(true, false));
		rename.addActionListener(e -> actions.nameLayout(false, true));
		layoutActions.add(newLayout);
		layoutActions.add(duplicate);
		layoutActions.add(rename);
		namedLayouts.add(layoutActions);
		addDisclosure(toolsPanel, "Marker layouts…", namedLayouts);
		JPanel historyActions = new JPanel(new GridLayout(1, 2, 3, 0));
		JButton undo = new JButton("Undo"), redo = new JButton("Redo");
		undo.addActionListener(e -> actions.restoreHistory(false));
		redo.addActionListener(e -> actions.restoreHistory(true));
		historyActions.add(undo);
		historyActions.add(redo);
		controls.add(historyActions);
		JButton layoutControls = new JButton("Customize bank view");
		layoutControls.addActionListener(e -> BankLayoutSettings.open(this, actions.config, actions.configs,
				actions::isActive, actions::setStatus));
		toolsPanel.add(new JLabel("Bank tools"));
		toolsPanel.add(layoutControls);
		addButton(toolsPanel, "Stored potions…", () -> actions.potionPanel.open(this));
		toolsPanel.add(new JLabel("<html>Alt-drag bank buttons to arrange them.</html>"));
		JButton resetButtons = new JButton("Reset bank buttons");
		resetButtons.addActionListener(e ->
		{
			actions.configs.setConfiguration("windowmarkers", "bankControlLayout", "");
			actions.configs.unsetConfiguration("windowmarkers", "bankActionLayout");
			actions.configs.setConfiguration("windowmarkers", "customBankControls", false);
			actions.setStatus("Bank button arrangement reset.");
		});
		toolsPanel.add(resetButtons);
		JButton screenshot = new JButton("Save bank screenshot");
		screenshot.addActionListener(e -> actions.clientThread.invokeLater(actions::captureBank));
		toolsPanel.add(screenshot);
		JPanel customPanel = new MarkerForm();
		customPanel.add(new JLabel(
				"<html>Create named item groups or area markers.<br>Manage existing groups in Legend.</html>"));
		addButton(customPanel, "Create item group…", () -> actions.editGroupItems(null));
		addButton(customPanel, "Draw item group…", () -> actions.armGroup(null));
		addButton(customPanel, "Draw area marker", actions::armAreaMarker);
		addButton(customPanel, "Cancel drawing", actions::cancel);
		JCheckBox snap = new JCheckBox("Snap areas to item grid", actions.config.snapToGrid());
		snap.addActionListener(e -> actions.configs.setConfiguration("windowmarkers", "snapToGrid", snap.isSelected()));
		customPanel.add(snap);
		viewLabel = new JLabel("All bank items");
		viewsPanel.add(viewLabel);
		addViewButton = addButton(viewsPanel, "Add to Legend", actions::addViewToLegend);
		clearViewButton = addButton(viewsPanel, "Clear bank filter", actions::clearBankView);
		reviewItemsButton = addButton(viewsPanel, "Item uses and reasons…", actions::reviewItemUses);
		reviewItemsButton.setVisible(false);
		JPanel savedViews = new MarkerForm();
		saveViewButton = addButton(savedViews, "Save as custom view…", actions::saveCurrentBankView);
		addButton(savedViews, "Reset preset item positions",
				() -> actions.clientThread.invokeLater(() -> actions.bankLayout.resetPresetPositions()));
		addViewButton.setEnabled(false);
		clearViewButton.setEnabled(false);
		saveViewButton.setEnabled(false);
		customPicker = new JComboBox<>();
		customPicker.setRenderer(PlainText.renderer());
		savedViews.add(customPicker);
		addButton(savedViews, "Open saved view", () ->
		{
			CustomPresets.Preset saved = (CustomPresets.Preset) customPicker.getSelectedItem();
			if (saved != null)
				actions.chooseCatalogue(new BankCatalogue.Choice(BankCatalogue.Kind.CUSTOM_PRESET, saved.name, saved),
						true);
		});
		addButton(savedViews, "Edit saved view…", () ->
		{
			CustomPresets.Preset saved = (CustomPresets.Preset) customPicker.getSelectedItem();
			if (saved != null)
				actions.editSavedView(saved);
		});
		addButton(savedViews, "Delete saved view…", () ->
		{
			CustomPresets.Preset saved = (CustomPresets.Preset) customPicker.getSelectedItem();
			if (saved != null)
				actions.deleteCustomPreset(saved);
		});
		addButton(toolsPanel, "Add Bank Tags tag…", actions::addBankTag);
		JButton copyAccount = new JButton("Copy earlier account markers");
		copyAccount.addActionListener(e -> actions.copyAccountMarkers());
		toolsPanel.add(new JLabel("Copy older markers"));
		toolsPanel.add(copyAccount);
		JButton importShared = new JButton("Import shared markers");
		importShared.addActionListener(e -> actions.importShared());
		toolsPanel.add(importShared);
		JComboBox<WindowMarkersConfig.BorderStyle> styles = new JComboBox<>(WindowMarkersConfig.BorderStyle.values());
		styles.setSelectedItem(actions.config.style());
		styles.addActionListener(
				e -> actions.configs.setConfiguration("windowmarkers", "style", styles.getSelectedItem()));
		toolsPanel.add(new JLabel("Default border style"));
		toolsPanel.add(styles);
		geContextLabel = new JLabel();
		guidePanel.add(geContextLabel);
		JButton geGuide = new JButton("Choose bank / GE guide");
		geGuide.addActionListener(e ->
		{
			ActivitySetups.Setup setup = ActivitySetupPicker.choose(this, true);
			if (setup != null)
			{
				actions.configs.setConfiguration("windowmarkers", "geGuideContext", setup.id);
				actions.setStatus("Bank / GE guide selected.");
				actions.refreshOpenView();
			}
		});
		guidePanel.add(geGuide);
		JButton clearGeGuide = new JButton("Clear bank / GE guide");
		clearGeGuide.addActionListener(e ->
		{
			actions.configs.unsetConfiguration("windowmarkers", "geGuideContext");
			actions.setStatus("Using matching guides.");
			actions.refreshOpenView();
		});
		guidePanel.add(clearGeGuide);
		addButton(guidePanel, "Choose inventory example…", () ->
		{
			ActivitySetups.Setup setup = ActivitySetupPicker.chooseInventory(this);
			if (setup != null)
			{
				actions.configs.setConfiguration("windowmarkers", "bankInventoryContext", setup.id);
				actions.refreshOpenView();
			}
		});
		addButton(guidePanel, "Automatic inventory example", () ->
		{
			actions.configs.unsetConfiguration("windowmarkers", "bankInventoryContext");
			actions.refreshOpenView();
		});
		addDisclosure(savedViews, "Guide choices…", guidePanel);
		addDisclosure(viewsPanel, "Saved views…", savedViews);
		legend.viewControls(viewsPanel);
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Legend", legend);
		tabs.addTab("Custom", customPanel);
		tabs.addTab("Tools", toolsPanel);
		tabs.addChangeListener(e ->
		{
			actions.clearLegendHover();
		});
		add(controls, BorderLayout.NORTH);
		add(tabs, BorderLayout.CENTER);
	}

	private static JButton addButton(JPanel panel, String title, Runnable action)
	{
		JButton button = new JButton(title);
		button.addActionListener(e -> action.run());
		panel.add(button);
		return button;
	}

	private static void addDisclosure(JPanel parent, String title, JPanel contents)
	{
		contents.setVisible(false);
		JToggleButton toggle = new JToggleButton(title);
		toggle.addActionListener(e ->
		{
			contents.setVisible(toggle.isSelected());
			parent.revalidate();
			parent.repaint();
		});
		parent.add(toggle);
		parent.add(contents);
	}

	void refreshLayouts(java.util.List<String> names, int index, java.util.List<CustomPresets.Preset> templates)
	{
		if (layoutPicker != null)
		{
			refreshingLayouts = true;
			layoutPicker.removeAllItems();
			for (String name : names)
				layoutPicker.addItem(name);
			layoutPicker.setSelectedIndex(index);
			refreshingLayouts = false;
		}
		if (customPicker != null)
		{
			Object selected = customPicker.getSelectedItem();
			customPicker.removeAllItems();
			for (CustomPresets.Preset template : templates)
				customPicker.addItem(template);
			if (templates.contains(selected))
				customPicker.setSelectedItem(selected);
		}
	}

	void setStatus(String text)
	{
		status.setText("<html>" + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") + "</html>");
	}

	void setGuideContext(String text)
	{
		geContextLabel.setText(text);
	}

	void updateView(String summary, boolean hasMatches, boolean canClear, boolean review)
	{
		viewLabel.setText(
				"<html>" + summary.split("\n", 2)[0].replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
						+ "</html>");
		viewLabel.setToolTipText(summary);
		addViewButton.setEnabled(hasMatches);
		saveViewButton.setEnabled(hasMatches);
		clearViewButton.setEnabled(canClear);
		reviewItemsButton.setVisible(review);
	}
}
