package com.windowmarkers;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;

@Singleton
final class BankPotionPanel
{
	private static final String[] DESTINATIONS = { "Hidden", "Untabbed", "Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5",
			"Tab 6", "Tab 7", "Tab 8", "Tab 9" };

	@Inject
	Client client;
	@Inject
	ClientThread clientThread;
	@Inject
	ConfigManager configs;
	@Inject
	WindowMarkersConfig config;
	@Inject
	BankPotionStock stock;
	@Inject
	BankPotionSettings settings;
	@Inject
	BankPotionViews views;

	private static final class Row
	{
		final int id;
		final String name;
		final int count;
		int tab;
		boolean search;

		Row(int id, String name, int count, int tab, boolean search)
		{
			this.id = id;
			this.name = name;
			this.count = count;
			this.tab = tab;
			this.search = search;
		}
	}

	void open(Component parent)
	{
		clientThread.invokeLater(() ->
		{
			String context = context();
			List<Row> rows = new ArrayList<>();
			for (BankPotionStock.Potion potion : stock.potions())
			{
				rows.add(new Row(potion.enumId, client.getItemDefinition(potion.itemId).getName(), potion.quantity,
						settings.bankTab(potion.enumId), settings.search(potion.enumId)));
			}
			rows.sort(java.util.Comparator.comparing(row -> row.name));
			boolean external = views.externalBank();
			SwingUtilities.invokeLater(() -> show(parent, rows, context, external));
		});
	}

	private String context()
	{
		return (configs.getProfile() == null ? "" : configs.getProfile().getId()) + ":" + configs.getRSProfileKey();
	}

	private void save(String expected, Runnable change)
	{
		clientThread.invokeLater(() ->
		{
			if (Objects.equals(expected, context()))
				change.run();
		});
	}

	private void show(Component parent, List<Row> rows, String context, boolean external)
	{
		JPanel panel = new JPanel(new BorderLayout(0, 8));
		JLabel help = new JLabel(rows.isEmpty() ? "Open the bank to load stored potions."
				: external ? "Potion Storage currently manages normal bank tabs and searches."
						: "Right-click a stored potion to add it to bank and search.");
		help.setToolTipText(
				"Choices apply to each potion type, across its doses. Numbered tabs must already exist. Changes save immediately for this character and profile.");
		panel.add(help, BorderLayout.NORTH);
		AbstractTableModel model = new AbstractTableModel()
		{
			private final String[] columns = { "Potion", "Available", "Bank tab", "Searches" };

			public int getRowCount()
			{
				return rows.size();
			}

			public int getColumnCount()
			{
				return columns.length;
			}

			public String getColumnName(int column)
			{
				return columns[column];
			}

			public Class<?> getColumnClass(int column)
			{
				return column == 3 ? Boolean.class : column == 1 ? Integer.class : String.class;
			}

			public boolean isCellEditable(int row, int column)
			{
				return column >= 2 && !external && Objects.equals(context, context());
			}

			public Object getValueAt(int index, int column)
			{
				Row row = rows.get(index);
				switch (column)
				{
					case 0:
						return row.name;
					case 1:
						return row.count;
					case 2:
						return DESTINATIONS[row.tab + 1];
					default:
						return row.search;
				}
			}

			public void setValueAt(Object value, int index, int column)
			{
				Row row = rows.get(index);
				if (column == 2)
				{
					int tab = java.util.Arrays.asList(DESTINATIONS).indexOf(value) - 1;
					if (tab < -1 || tab > 9)
						return;
					row.tab = tab;
					save(context, () -> settings.setBankTab(row.id, tab));
				}
				else if (column == 3 && value instanceof Boolean)
				{
					boolean search = (Boolean) value;
					row.search = search;
					save(context, () -> settings.setSearch(row.id, search));
				}
				fireTableCellUpdated(index, column);
			}
		};
		JTable table = new JTable(model);
		table.setRowHeight(26);
		table.getColumnModel().getColumn(0).setPreferredWidth(260);
		table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(DESTINATIONS)));
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		JCheckBox move = new JCheckBox("Drag to rearrange potion-storage rows", config.moveStoredPotions());
		move.setToolTipText(
				"Uses the bank's swap or insert mode within each section. Disable Potion Storage Customizer to use this plugin's ordering.");
		move.addActionListener(event ->
		{
			boolean selected = move.isSelected();
			save(context, () -> configs.setConfiguration("windowmarkers", "moveStoredPotions", selected));
		});
		panel.add(move, BorderLayout.SOUTH);
		MarkerDialogs.show(parent, panel, "Stored potions", 620, 440);
	}
}
