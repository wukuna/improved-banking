package com.windowmarkers;

import java.awt.Component;
import java.util.*;
import java.util.List;
import javax.swing.*;

final class BankPresetReview
{
	private final JPanel panel = new MarkerForm();
	private final JCheckBox includeGrouped, showView;
	private final Map<JCheckBox, Marker> replacements = new LinkedHashMap<>();
	private final Map<JCheckBox, List<AutoCategories.Item>> choices = new LinkedHashMap<>();
	private final Map<String, List<AutoCategories.Item>> groups;
	private final AutoCategories.Preset preset;
	private final ActivitySetups.Setup setup;

	BankPresetReview(Map<String, List<AutoCategories.Item>> groups, AutoCategories.Preset preset,
			Map<Integer, String> exclusions, ActivitySetups.Setup setup, boolean explicitCategory, boolean slotView,
			boolean requestedView)
	{
		this.groups = groups;
		this.preset = preset;
		this.setup = setup;
		panel.add(new JLabel(
				"<html>Wiki classifications describe uses, not one combined loadout.<br>Outfit groups include owned pieces; partial ownership does<br>not activate a full-set bonus. Tools are grouped separately.<br>Raid lists depend on role, spellbook, charges and invocations.<br>Unresolved items stay in an unchecked review group.</html>"));
		if (setup != null && !setup.notes.isEmpty())
		{
			JTextArea notes = new JTextArea(setup.notes);
			notes.setEditable(false);
			notes.setLineWrap(true);
			notes.setWrapStyleWord(true);
			notes.setColumns(42);
			panel.add(notes);
		}
		if (!exclusions.isEmpty())
		{
			JTextArea excluded = new JTextArea("Excluded lower-tier items:\n" + String.join("\n", exclusions.values()));
			excluded.setEditable(false);
			excluded.setLineWrap(true);
			excluded.setWrapStyleWord(true);
			excluded.setColumns(42);
			panel.add(excluded);
		}
		JButton source = new JButton("Open Wiki source");
		source.addActionListener(e -> net.runelite.client.util.LinkBrowser
				.browse(setup == null ? WikiCatalogue.source(preset) : setup.source));
		panel.add(source);
		includeGrouped = new JCheckBox("Include items already in other groups", explicitCategory);
		panel.add(includeGrouped);
		showView = new JCheckBox("Show selected groups as a temporary bank view", requestedView || slotView);
		panel.add(showView);
		if (slotView)
			panel.add(new JLabel(
					"<html>View groups equipment by slot, then tools and supplies.<br>Save it from the bank dropdown. Your bank order stays unchanged.</html>"));
		panel.add(new JLabel(
				"<html>Including grouped items keeps their existing groups too.<br>To replace old groups instead, check their names below.<br>Replacing removes the entire checked marker. Undo restores it.</html>"));
	}

	void addExistingMarkers(List<Marker> markers)
	{
		for (Marker marker : markers)
		{
			if (!marker.linked())
			{
				continue;
			}
			JCheckBox replace = new JCheckBox("Replace: " + marker.labelText(), false);
			replacements.put(replace, marker);
			panel.add(replace);
		}
	}

	boolean show(Component parent)
	{
		for (java.util.Map.Entry<String, List<AutoCategories.Item>> entry : groups.entrySet())
		{
			JCheckBox choose = new JCheckBox(entry.getKey(), RaidRecommendations.defaultSelected(entry.getKey()));
			choices.put(choose, entry.getValue());
			panel.add(choose);
			JTextArea items = new JTextArea(
					entry.getValue().stream().map(i -> i.name).collect(java.util.stream.Collectors.joining(", ")));
			items.setLineWrap(true);
			items.setWrapStyleWord(true);
			items.setEditable(false);
			items.setColumns(42);
			items.setBackground(panel.getBackground());
			panel.add(items);
		}

		return MarkerDialogs.confirm(parent, panel,
				"Review " + (setup == null ? preset : setup.activity + " — " + setup.label), 620, 620);
	}

	Map<String, List<AutoCategories.Item>> selectedGroups()
	{
		Map<String, List<AutoCategories.Item>> selected = new LinkedHashMap<>();
		choices.forEach((box, items) ->
		{
			if (box.isSelected())
				selected.put(box.getText(), items);
		});
		return selected;
	}

	List<Marker> replacedMarkers()
	{
		List<Marker> selected = new ArrayList<>();
		replacements.forEach((box, marker) ->
		{
			if (box.isSelected())
				selected.add(marker);
		});
		return selected;
	}

	boolean includeGrouped()
	{
		return includeGrouped.isSelected();
	}

	boolean showView()
	{
		return showView.isSelected();
	}
}
