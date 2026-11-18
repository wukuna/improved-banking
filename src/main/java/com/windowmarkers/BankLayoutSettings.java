package com.windowmarkers;

import java.awt.Component;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import net.runelite.client.config.ConfigManager;

final class BankLayoutSettings
{
	static void open(Component parent, WindowMarkersConfig config, ConfigManager configs, BooleanSupplier active,
			Consumer<String> status)
	{
		String profile = configs.getProfile().getId() + "";
		JPanel form = new MarkerForm();
		JCheckBox actionRail = new JCheckBox("Customize bank view", config.bankActionRail());
		JCheckBox resetPosition = new JCheckBox("Reset bank window position");
		JCheckBox tagSpace = new JCheckBox("Share Bank Tags strip: tags above, controls below",
				config.bankActionTagSpace());
		JCheckBox tagDivider = new JCheckBox("Adjustable Bank Tags divider", config.bankTagDividerResizable());
		tagDivider.setToolTipText("Turn off to remove the divider bar and keep the saved split.");
		tagDivider.setEnabled(tagSpace.isSelected());
		tagSpace.addActionListener(e -> tagDivider.setEnabled(tagSpace.isSelected()));
		JCheckBox equipmentPreview = new JCheckBox("Worn equipment hover preview", config.bankEquipmentPreview());
		JCheckBox compactSearch = new JCheckBox("Compact search button", config.compactBankSearch());
		compactSearch.setToolTipText("Use the smaller button outside customized strips too.");
		JCheckBox nativeEquipment = new JCheckBox("Restore original worn equipment button",
				config.restoreOriginalWornEquipmentButton());
		nativeEquipment.setToolTipText("Turn off to resume the saved strip position.");
		JSpinner columnSpacing = new JSpinner(
				new SpinnerNumberModel(Math.max(4, Math.min(24, config.bankColumnSpacing())), 4, 24, 1));
		columnSpacing
				.setToolTipText("Minimum gap between native item slots. Saved equipment layouts keep their spacing.");
		JSpinner gap = new JSpinner(new SpinnerNumberModel(Math.max(0, Math.min(40, config.tabGap())), 0, 40, 2));
		JComboBox<WindowMarkersConfig.TabAlignment> alignment = new JComboBox<>(
				WindowMarkersConfig.TabAlignment.values());
		alignment.setSelectedItem(config.tabAlignment());
		form.add(actionRail);
		form.add(resetPosition);
		form.add(tagSpace);
		form.add(tagDivider);
		form.add(equipmentPreview);
		form.add(compactSearch);
		form.add(nativeEquipment);
		actionRail.setToolTipText(
				"Drag the title or resize grip. Alt-drag buttons along the strips. Turning off keeps the saved window position and size.");
		form.add(new JLabel("Column spacing (pixels)"));
		form.add(columnSpacing);
		form.add(new JLabel("Top tab alignment"));
		form.add(alignment);
		form.add(new JLabel("Space below tabs (pixels)"));
		form.add(gap);
		form.add(new JLabel(
				"<html>Bank Tag layouts retain their designed item grid.<br>Saved marker layouts remain per account and profile.</html>"));
		if (!MarkerDialogs.confirm(parent, form, "Customize bank view", 480, 490))
		{
			return;
		}
		if (!active.getAsBoolean() || !profile.equals(configs.getProfile().getId() + ""))
		{
			status.accept("Profile changed. Reopen bank settings.");
			return;
		}
		configs.setConfiguration("windowmarkers", "bankActionRail", actionRail.isSelected());
		configs.setConfiguration("windowmarkers", "bankActionTagSpace", tagSpace.isSelected());
		configs.setConfiguration("windowmarkers", "bankTagDividerResizable", tagDivider.isSelected());
		configs.setConfiguration("windowmarkers", "bankEquipmentPreview", equipmentPreview.isSelected());
		configs.setConfiguration("windowmarkers", "compactBankSearch", compactSearch.isSelected());
		configs.setConfiguration("windowmarkers", "restoreOriginalWornEquipmentButton", nativeEquipment.isSelected());
		configs.setConfiguration("windowmarkers", "bankColumnSpacing", columnSpacing.getValue());
		if (resetPosition.isSelected())
			configs.unsetConfiguration("windowmarkers", "bankWindowPosition");
		configs.setConfiguration("windowmarkers", "tabGap", gap.getValue());
		configs.setConfiguration("windowmarkers", "tabAlignment", alignment.getSelectedItem());
		status.accept("Bank layout settings saved.");
	}

}
