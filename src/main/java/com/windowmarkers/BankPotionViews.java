package com.windowmarkers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetDrag;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetConfig;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.PluginManager;

@Singleton
final class BankPotionViews
{
	private static final int[] TABS = { VarbitID.BANK_TAB_1, VarbitID.BANK_TAB_2, VarbitID.BANK_TAB_3,
			VarbitID.BANK_TAB_4, VarbitID.BANK_TAB_5, VarbitID.BANK_TAB_6, VarbitID.BANK_TAB_7, VarbitID.BANK_TAB_8,
			VarbitID.BANK_TAB_9 };

	@Inject
	Client client;
	@Inject
	ClientThread clientThread;
	@Inject
	EventBus events;
	@Inject
	ConfigManager configs;
	@Inject
	PluginManager plugins;
	@Inject
	BankPotionStock stock;
	@Inject
	BankPotionSettings settings;
	@Inject
	BankSearchView searchView;
	@Inject
	WindowMarkersConfig config;
	private final Map<Widget, BankPotionStock.Potion> drawn = new IdentityHashMap<>();
	private final BankPotionPlacement placement = new BankPotionPlacement();
	private final Map<MenuEntry, Click> clicks = new java.util.WeakHashMap<>();
	private Widget parent;
	private java.util.function.Function<Widget, Widget> menuSource = java.util.function.Function.identity();
	private Runnable changed;
	private volatile long generation;
	private volatile boolean active;
	private boolean externalBank;
	private boolean externalOrder;

	private static final class Click
	{
		final Widget parent;
		final Widget source;
		final BankPotionStock.Potion potion;
		final String context;

		Click(Widget parent, Widget source, BankPotionStock.Potion potion, String context)
		{
			this.parent = parent;
			this.source = source;
			this.potion = potion;
			this.context = context;
		}
	}

	private String context()
	{
		return generation + ":" + (configs.getProfile() == null ? "" : configs.getProfile().getId()) + ":"
				+ configs.getRSProfileKey();
	}

	void start(Runnable changed)
	{
		this.changed = changed;
		active = true;
		generation++;
		events.register(this);
		refreshProviders();
	}

	void stop()
	{
		active = false;
		generation++;
		events.unregister(this);
		long stopped = generation;
		clientThread.invokeLater(() ->
		{
			if (!active && generation == stopped)
				reset();
		});
	}

	boolean externalOrder()
	{
		return externalOrder;
	}

	boolean externalBank()
	{
		return externalBank;
	}

	private void refreshProviders()
	{
		externalBank = false;
		externalOrder = false;
		for (net.runelite.client.plugins.Plugin plugin : plugins.getPlugins())
		{
			if (!plugins.isPluginActive(plugin))
				continue;
			String name = plugin.getClass().getName();
			externalBank |= name.equals("com.potionstorage.PotionStoragePlugin");
			externalOrder |= name.equals("com.potionstoragecustomizer.PotionStorageCustomizerPlugin");
		}
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		defer(() ->
		{
			refreshProviders();
			changed.run();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("windowmarkers".equals(event.getGroup())
				&& (event.getKey().startsWith(BankPotionSettings.PREFIX) || event.getKey().equals("moveStoredPotions")))
		{
			defer(changed);
		}
	}

	private void defer(Runnable action)
	{
		long expected = generation;
		clientThread.invokeLater(() ->
		{
			if (active && expected == generation)
				action.run();
		});
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		Widget bank = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		if (active && client.getGameState() == GameState.LOGGED_IN && bank != null && !bank.isHidden()
				&& stock.refresh())
		{
			changed.run();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		stock.changed(event.getVarpId());
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
			reset();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
			reset();
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		generation++;
		defer(this::reset);
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		generation++;
		defer(this::reset);
	}

	private void reset()
	{
		generation++;
		clearWidgets();
		stock.clear();
	}

	@Subscribe(priority = -15)
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_BUILD)
			clearWidgets();
		if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING)
			append();
	}

	void append()
	{
		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		ItemContainer bank = client.getItemContainer(InventoryID.BANK);
		if (items == null || items.isHidden() || bank == null)
			return;
		boolean preset = searchView.isActive();
		if (!preset && (externalBank || searchView.bankTags.getActiveBankTag() != null))
			return;
		boolean searching = client.getVarcIntValue(VarClientID.MESLAYERMODE) == net.runelite.api.vars.InputType.SEARCH
				.getType();
		String query = Objects.toString(client.getVarcStrValue(VarClientID.MESLAYERINPUT), "")
				.toLowerCase(java.util.Locale.ROOT);
		int tab = client.getVarbitValue(VarbitID.BANK_CURRENTTAB);
		if (tab < 0 || tab > 9)
			return;
		parent = items;
		placement.clear();
		Map<Integer, List<BankPotionStock.Potion>> groups = new LinkedHashMap<>();
		for (BankPotionStock.Potion potion : stock.potions())
		{
			int target = settings.bankTab(potion.enumId);
			if (preset)
			{
				if (potion.variants.stream().noneMatch(searchView.bankTags.getActiveBankTag()::contains))
					continue;
				if (alreadyShown(items, potion.itemId))
					continue;
				target = 0;
			}
			else if (searching)
			{
				if (!settings.search(potion.enumId) || !client.getItemDefinition(potion.itemId).getName()
						.toLowerCase(java.util.Locale.ROOT).contains(query))
					continue;
				target = 0;
			}
			else if (target < 0 || tab != 0 && target != tab
					|| target > 0 && client.getVarbitValue(TABS[target - 1]) == 0)
			{
				continue;
			}
			groups.computeIfAbsent(target, ignored -> new ArrayList<>()).add(potion);
		}
		for (int selected = 0; selected < 10; selected++)
		{
			List<BankPotionStock.Potion> potions = groups.get(selected);
			List<Widget> cells = sectionItems(items, selected, bank.size());
			if (potions == null)
			{
				if (!preset && !searching)
					placement.capture(client, selected, cells, drawn, settings);
				continue;
			}
			if (!preset && !searching && selected > 0 && cells.isEmpty())
				continue;
			int bottom = preset || searching ? sectionBottom(items, -1)
					: cells.stream().mapToInt(cell -> cell.getOriginalY() + 36).max().orElse(0);
			int filled = preset || searching ? 0 : cells.size() % 8;
			int firstRow = bottom - (filled > 0 ? 36 : 0);
			int freeSlots = filled == 0 ? 0 : 8 - filled;
			int extraRows = (Math.max(0, potions.size() - freeSlots) + 7) / 8;
			int height = extraRows * 36;
			for (Widget child : items.getDynamicChildren())
			{
				if (height > 0 && child != null && !child.isSelfHidden() && child.getOriginalY() >= bottom)
				{
					child.setOriginalY(child.getOriginalY() + height);
					child.revalidate();
				}
			}
			for (int i = 0; i < potions.size(); i++)
			{
				Widget widget = create(items, potions.get(i));
				widget.setOriginalX(51 + (filled + i) % 8 * 48);
				widget.setOriginalY(firstRow + (filled + i) / 8 * 36);
				widget.revalidate();
				cells.add(widget);
			}
			if (!preset && !searching)
				placement.capture(client, selected, cells, drawn, settings);
		}
		if (!groups.isEmpty())
		{
			int height = 0;
			for (Widget child : items.getDynamicChildren())
			{
				if (child != null && !child.isSelfHidden())
					height = Math.max(height, child.getOriginalY() + child.getHeight());
			}
			int argument = client.getIntStackSize() - 9;
			if (argument >= 0)
				client.getIntStack()[argument] = Math.max(client.getIntStack()[argument], height + 4);
		}
	}

	private List<Widget> sectionItems(Widget items, int tab, int bankSize)
	{
		int start = 0;
		int end = bankSize;
		for (int i = 1; i <= 9; i++)
		{
			int size = client.getVarbitValue(TABS[i - 1]);
			if (tab == i)
			{
				end = start + size;
				break;
			}
			start += size;
		}
		List<Widget> result = new ArrayList<>();
		for (int i = start; i < end; i++)
		{
			Widget cell = items.getChild(i);
			if (cell != null && !cell.isSelfHidden() && cell.getItemId() >= 0)
				result.add(cell);
		}
		return result;
	}

	private boolean alreadyShown(Widget items, int id)
	{
		for (Widget child : items.getDynamicChildren())
		{
			if (child != null && !child.isSelfHidden() && child.getItemId() == id && child.getItemQuantity() > 0)
				return true;
		}
		return false;
	}

	private int sectionBottom(Widget items, int section)
	{
		int start = 0;
		int end = items.getDynamicChildren().length;
		if (section > 0)
		{
			for (int i = 1; i <= 9; i++)
			{
				int size = client.getVarbitValue(TABS[i - 1]);
				if (i == section)
				{
					end = start + size;
					break;
				}
				start += size;
			}
		}
		else
			start = 0;
		int bottom = 0;
		Widget[] children = items.getDynamicChildren();
		for (int i = start; i < Math.min(end, children.length); i++)
		{
			Widget widget = children[i];
			if (widget != null && !widget.isSelfHidden() && widget.getItemId() >= 0)
				bottom = Math.max(bottom, widget.getOriginalY() + 36);
		}
		return bottom;
	}

	private Widget create(Widget items, BankPotionStock.Potion potion)
	{
		Widget widget = items.createChild(-1, WidgetType.GRAPHIC);
		widget.setOriginalWidth(36);
		widget.setOriginalHeight(32);
		widget.setItemId(potion.itemId);
		widget.setItemQuantity(potion.quantity);
		widget.setItemQuantityMode(ItemQuantityMode.STACKABLE);
		widget.setBorderType(1);
		widget.setName("<col=ff9040>" + client.getItemDefinition(potion.itemId).getName() + "</col> (storage)");
		int requested = client.getVarbitValue(VarbitID.BANK_REQUESTEDQUANTITY);
		String[] quantities = { "1", "5", "10", Integer.toString(Math.max(1, requested)), "All" };
		int selected = Math.max(0, Math.min(4, client.getVarbitValue(VarbitID.BANK_QUANTITY_TYPE)));
		String[] actions = { quantities[selected], "1", "5", "10", Integer.toString(requested), "X", "All",
				"All-but-1" };
		int mask = 0;
		for (int i = 0; i < actions.length; i++)
		{
			if (i == 1 && selected == 0 || i == 4 && requested <= 0)
				continue;
			widget.setAction(i, "Withdraw-" + actions[i]);
			mask |= WidgetConfig.transmitAction(i);
		}
		widget.setClickMask(mask | WidgetConfig.DRAG | WidgetConfig.DRAG_ON);
		widget.setDragParent(items);
		widget.setDragDeadTime(5);
		widget.setDragDeadZone(5);
		widget.setHasListener(true);
		drawn.put(widget, potion);
		return widget;
	}

	boolean validSource(Widget source)
	{
		BankPotionStock.Potion original = drawn.get(source);
		if (original == null)
		{
			if (!externalBank || source == null || source.isSelfHidden() || source.getItemQuantity() <= 0)
				return false;
			return stock.potions().stream().anyMatch(p -> p.itemId == source.getItemId() && p.quantity > 0);
		}
		return stock.potions().stream()
				.anyMatch(p -> p.enumId == original.enumId && p.itemId == original.itemId && p.quantity > 0);
	}

	void setMenuSource(java.util.function.Function<Widget, Widget> source)
	{
		menuSource = source;
	}

	@Subscribe(priority = 10)
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		MenuEntry menu = event.getMenuEntry();
		addStorageMenuEntry(menu);
		clicks.remove(menu);
		if (parent == null || menu.getParam1() != InterfaceID.Bankmain.ITEMS)
			return;
		Widget source = menuSource.apply(parent.getChild(menu.getParam0()));
		BankPotionStock.Potion potion = drawn.get(source);
		if (potion != null)
			clicks.put(menu, new Click(parent, source, potion, context()));
	}

	private void addStorageMenuEntry(MenuEntry entry)
	{
		if (!active || externalBank || entry.getParam1() != InterfaceID.Bankmain.POTIONSTORE_ITEMS
				|| entry.getParam0() < 0 || entry.getParam0() % 5 != 0 || entry.getIdentifier() != 1
				|| entry.getType() != MenuAction.CC_OP && entry.getType() != MenuAction.CC_OP_LOW_PRIORITY)
			return;
		Widget store = client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
		BankPotionStock.Potion potion = stock.potions().stream()
				.filter(candidate -> candidate.index * 5 == entry.getParam0()).findFirst().orElse(null);
		if (potion == null || !storageRowCurrent(store, potion))
			return;
		boolean shown = settings.bankTab(potion.enumId) >= 0 && settings.search(potion.enumId);
		String option = shown ? "Remove potion from bank window" : "Add potion to bank window";
		for (MenuEntry existing : client.getMenuEntries())
			if (option.equals(existing.getOption()) && existing.getParam1() == entry.getParam1()
					&& existing.getParam0() == entry.getParam0())
				return;
		String expected = context();
		Widget row = store.getChild(entry.getParam0());
		client.createMenuEntry(0).setOption(option).setTarget(entry.getTarget())
				.setType(MenuAction.RUNELITE_LOW_PRIORITY).setParam0(entry.getParam0())
				.setParam1(entry.getParam1()).onClick(clicked ->
				{
					if (!active || externalBank || !expected.equals(context())
							|| row != store.getChild(potion.index * 5) || !storageRowCurrent(store, potion))
						return;
					if (shown)
					{
						settings.setBankTab(potion.enumId, -1);
						settings.setSearch(potion.enumId, false);
					}
					else
					{
						if (settings.bankTab(potion.enumId) < 0)
						{
							settings.setBankPosition(potion.enumId, -1);
							settings.setBankTab(potion.enumId, 0);
						}
						settings.setSearch(potion.enumId, true);
					}
				});
	}

	private boolean storageRowCurrent(Widget store, BankPotionStock.Potion potion)
	{
		if (store == null || store.isHidden() || client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS) != store)
			return false;
		Widget row = store.getChild(potion.index * 5);
		Widget icon = store.getChild(potion.index * 5 + 1);
		return row != null && !row.isSelfHidden() && icon != null && potion.variants.contains(icon.getItemId())
				&& stock.potions().stream().anyMatch(current -> current.enumId == potion.enumId
						&& current.index == potion.index && current.quantity > 0);
	}

	@Subscribe(priority = 10)
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.isConsumed() || event.getParam1() != InterfaceID.Bankmain.ITEMS
				|| event.getMenuAction() != MenuAction.CC_OP && event.getMenuAction() != MenuAction.CC_OP_LOW_PRIORITY)
			return;
		Click click = clicks.get(event.getMenuEntry());
		if (click == null)
			return;
		Widget source = click.source;
		BankPotionStock.Potion potion = click.potion;
		if (!Objects.equals(click.context, context()) || parent != click.parent
				|| client.getWidget(InterfaceID.Bankmain.ITEMS) != parent
				|| parent.getChild(event.getParam0()) != source || source.isSelfHidden() || !validSource(source)
				|| event.getItemId() > 0 && event.getItemId() != potion.itemId || event.getId() < 1
				|| event.getId() > 8)
		{
			event.consume();
			return;
		}
		Widget store = client.getWidget(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
		if (store == null)
		{
			event.consume();
			return;
		}
		int index = potion.index * 5;
		if (store.getChild(index) == null)
		{
			int count = store.getChildren() == null ? 0 : store.getChildren().length;
			for (int i = count; i <= index; i++)
				store.createChild(i, WidgetType.GRAPHIC);
		}
		MenuEntry menu = event.getMenuEntry();
		menu.setParam1(InterfaceID.Bankmain.POTIONSTORE_ITEMS);
		menu.setParam0(index);
	}

	@Subscribe(priority = -25)
	public void onWidgetDrag(WidgetDrag event)
	{
		Widget items = parent;
		Widget source = client.getDraggedWidget();
		Widget target = client.getDraggedOnWidget();
		String expected = context();
		if (items != null && !searchView.isActive())
			placement.drag(client, items, drawn, settings,
					() -> active && expected.equals(context()) && parent == items
							&& client.getWidget(InterfaceID.Bankmain.ITEMS) == items && !externalBank
							&& !searchView.isActive() && searchView.bankTags.getActiveBankTag() == null
							&& client.getGameState() == GameState.LOGGED_IN
							&& (!drawn.containsKey(source) || validSource(source))
							&& (!drawn.containsKey(target) || validSource(target)));
		else if (drawn.containsKey(source) || drawn.containsKey(client.getDraggedOnWidget()))
			client.setDraggedOnWidget(null);
	}

	private void clearWidgets()
	{
		placement.clear();
		if (parent != null && parent.getChildren() != null)
		{
			Widget[] children = parent.getChildren().clone();
			int size = children.length;
			while (size > 0 && drawn.containsKey(children[size - 1]))
				size--;
			for (int i = 0; i < children.length; i++)
			{
				if (drawn.containsKey(children[i]))
				{
					children[i].setHidden(true);
					children[i] = null;
				}
			}
			parent.setChildren(Arrays.copyOf(children, size));
		}
		drawn.clear();
		parent = null;
	}
}
