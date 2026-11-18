package com.windowmarkers;

import java.awt.Color;
import java.awt.Component;
import javax.swing.*;

final class MarkerAppearanceEditor
{
	private final JTextField label;
	private final JComboBox<Marker.LabelPlacement> placement;
	private final JCheckBox labelOnce, linked, customBorder, fill;
	private final JComboBox<String> font;
	private final JSpinner fontSize, opacity;
	private final Color[] labelColor, colors;
	private final JComboBox<WindowMarkersConfig.BorderStyle> style;
	private final JPanel form;

	MarkerAppearanceEditor(Marker marker, WindowMarkersConfig config)
	{
		label = new JTextField(marker.labelText());
		placement = new JComboBox<>(Marker.LabelPlacement.values());
		placement.setSelectedItem(marker.labelPlacement == null ? Marker.LabelPlacement.INSIDE : marker.labelPlacement);
		labelOnce = new JCheckBox("Show label once across linked regions", Boolean.TRUE.equals(marker.labelOnce));
		linked = new JCheckBox("Link to enclosed items", marker.linked());
		linked.setToolTipText("Follows selected item identities in every tab. Uncheck to restore the original area.");
		font = new JComboBox<>(
				new String[] { "RuneScape", "RuneScape Bold", "Arial", "Verdana", "Serif", "Monospaced" });
		font.setSelectedItem(marker.labelFont == null ? "RuneScape" : marker.labelFont);
		fontSize = new JSpinner(new SpinnerNumberModel(marker.fontSize(), 8, 48, 1));
		labelColor = new Color[] { marker.labelArgb == null ? Color.WHITE : new Color(marker.labelArgb, true) };
		JButton labelColorButton = new JButton("Choose label color");
		labelColorButton.setForeground(labelColor[0]);
		labelColorButton.addActionListener(e ->
		{
			Color chosen = MarkerDialogs.chooseColor(labelColorButton, "Label color", labelColor[0]);
			if (chosen != null)
			{
				labelColor[0] = chosen;
				labelColorButton.setForeground(chosen);
			}
		});
		style = new JComboBox<>(WindowMarkersConfig.BorderStyle.values());
		style.setSelectedItem(marker.style == null ? config.style() : marker.style);
		customBorder = new JCheckBox("Use custom border color", marker.borderArgb != null);
		fill = new JCheckBox("Show background", marker.backgroundArgb != null);
		colors = new Color[] { marker.borderArgb == null ? config.color() : new Color(marker.borderArgb, true),
				marker.backgroundArgb == null ? new Color(60, 45, 30) : new Color(marker.backgroundArgb, true) };
		JButton border = new JButton("Choose border color"), background = new JButton("Choose background color");
		border.setForeground(colors[0]);
		background.setForeground(new Color(colors[1].getRGB()));
		border.addActionListener(e ->
		{
			Color chosen = MarkerDialogs.chooseColor(border, "Border color", colors[0]);
			if (chosen != null)
			{
				colors[0] = chosen;
				customBorder.setSelected(true);
				border.setForeground(chosen);
			}
		});
		background.addActionListener(e ->
		{
			Color chosen = MarkerDialogs.chooseColor(background, "Background color", colors[1]);
			if (chosen != null)
			{
				colors[1] = chosen;
				fill.setSelected(true);
				background.setForeground(chosen);
			}
		});
		opacity = new JSpinner(new SpinnerNumberModel(
				marker.backgroundArgb == null ? 50 : colors[1].getAlpha() * 100 / 255, 0, 100, 5));
		form = new MarkerForm();
		form.add(new JLabel("Label (up to 60 characters)"));
		form.add(label);
		form.add(placement);
		form.add(labelOnce);
		form.add(linked);
		form.add(font);
		form.add(new JLabel("Font size (8–48)"));
		form.add(fontSize);
		form.add(labelColorButton);
		form.add(new JLabel("Border style"));
		form.add(style);
		form.add(customBorder);
		form.add(border);
		form.add(fill);
		form.add(background);
		form.add(new JLabel("Background opacity (%)"));
		form.add(opacity);

	}

	boolean show(Component parent)
	{
		return MarkerDialogs.confirm(parent, form, "Marker appearance", 440, 640);
	}

	boolean linksItems()
	{
		return linked.isSelected();
	}

	void apply(Marker marker, int[] enclosedIds)
	{
		marker.linkedItemIds = linked.isSelected() ? (marker.linked() ? marker.linkedItemIds : enclosedIds) : null;
		marker.label = label.getText().substring(0, Math.min(60, label.getText().length()));
		marker.labelPlacement = (Marker.LabelPlacement) placement.getSelectedItem();
		marker.labelOnce = labelOnce.isSelected();
		marker.labelFont = (String) font.getSelectedItem();
		marker.labelSize = ((Number) fontSize.getValue()).intValue();
		marker.labelArgb = labelColor[0].getRGB();
		marker.style = (WindowMarkersConfig.BorderStyle) style.getSelectedItem();
		marker.borderArgb = customBorder.isSelected() ? colors[0].getRGB() : null;
		int alpha = ((Number) opacity.getValue()).intValue() * 255 / 100;
		marker.backgroundArgb = fill.isSelected()
				? new Color(colors[1].getRed(), colors[1].getGreen(), colors[1].getBlue(), alpha).getRGB()
				: null;
	}
}
