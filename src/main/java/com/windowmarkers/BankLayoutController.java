/*
BSD 2-Clause License

Copyright (c) 2026, TheLeon0
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Adapted for Window Markers from TheLeon0/flexible-bank, commit 0ea1d3bf2c7bc9534323d4d6939599681c4d40fa.
*/
package com.windowmarkers;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.vars.InputType;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;

public final class BankLayoutController
{
	// {button, icon}
	private static final int[][] DEPOSIT_BUTTONS = {
		{InterfaceID.Bankmain.DEPOSITCONTAINERS, InterfaceID.Bankmain.DEPOSITCONTAINERS_GRAPHIC},
		{InterfaceID.Bankmain.DEPOSITINV, InterfaceID.Bankmain.DEPOSITINV_GRAPHIC},
		{InterfaceID.Bankmain.DEPOSITWORN, InterfaceID.Bankmain.DEPOSITWORN_GRAPHIC},
	};
	// the buttons in the strip left of the items
	private static final int[] LEFT_STRIP = {
		InterfaceID.Bankmain.POPUP_BUTTON_OUT,
		InterfaceID.Bankmain.STORAGE_POPUP_TAB,
		InterfaceID.Bankmain.INCINERATOR_TARGET,
	};

	// BANKMAIN_FINISHBUILDING is invoked at the end of every bank build, after the items have been
	// positioned, and takes the content height as the 9th int from the top of the stack.
	private static final int FINISHBUILDING_HEIGHT_ARG = 9;

	// geometry used by the bankmain_build script
	private static final int VANILLA_CONTAINER_WIDTH = 478;
	private static final int ITEMS_WIDTH_MARGIN = 18;
	private static final int LEFT_INSET = 51;
	private static final int RIGHT_INSET = 35;
	private static final int CELL_WIDTH = 36;
	private static final int CELL_HEIGHT = 32;
	private static final int ROW_HEIGHT = 36;
	private static final int VANILLA_COLUMNS = 8;
	private static final int VANILLA_GAP = 12;
	private static final int MIN_GAP = 2;
	private static final int SEPARATOR_MIN_WIDTH = 300;
    // bankmain_init creates the nine tab dividers as tiled GRAPHIC widgets.
    private static final int BANK_TAB_SEPARATOR_SPRITE = 897;

	private static final int MIN_BANK_HEIGHT = 200;
    private static final int SETTINGS_WIDTH=478,SETTINGS_HEIGHT=290,SETTINGS_TOP=35;
    private static final int[] SETTINGS_ROWS={InterfaceID.Bankmain.INCINERATOR_TOGGLE,InterfaceID.Bankmain.BANKTUT_TOGGLE,
        InterfaceID.Bankmain.SIDEOPS_TOGGLE,InterfaceID.Bankmain.BANKOPS_TOGGLE,
        InterfaceID.Bankmain.DEPOSITINV_TOGGLE,InterfaceID.Bankmain.DEPOSITWORN_TOGGLE,
        InterfaceID.Bankmain.DEPOSITCONTAINERS_TOGGLE,InterfaceID.Bankmain.POPUP_TAB_FORGETPOS,
        InterfaceID.Bankmain.DEPOSITPOTION_TOGGLE,InterfaceID.Bankmain.CHUGGINGBARREL_TOGGLE};
	// the game keeps this much of the modal's height free around the bank
	private static final int MODAL_HEIGHT_MARGIN = 3;
	private static final int REBUILD_COOLDOWN_TICKS = 5;
	private static final int BUTTON_PITCH = 37;
	// bottom bar children at or right of this x are the deposit buttons and their separator
	private static final int DEPOSIT_GROUP_X = 360;
	// the left strip's buttons sit at x=2, the lowest one 7px above the bottom bar
	private static final int LEFT_STRIP_X = 2;
	private static final int LEFT_STRIP_GAP = 7;

	@Inject
	Client client;

	@Inject
	ClientThread clientThread;

	@Inject
	EventBus eventBus;

	@Inject
	WindowMarkersConfig config;

    @Inject NativeBankSearch nativeSearch;
    @Inject BankSearchView searchView;
    @Inject net.runelite.client.game.ItemManager itemManager;
    private final SetupInventoryWidgets inventoryWidgets=new SetupInventoryWidgets();
    private final BankViewGeometry viewGeometry=new BankViewGeometry();
    @Inject BankPresetPositions presetPositions;
    final BankControlWidgets controls=new BankControlWidgets(this);
    final BankControlDrag controlDrag=new BankControlDrag(this);
    @Inject net.runelite.client.input.MouseManager mouseManager;
    @Inject net.runelite.client.ui.overlay.OverlayManager overlayManager;
    @Inject ConfigManager configs;
    private boolean customControls() {return config.customBankControls() || controlDrag.draft()!=null;}

	@com.google.inject.Inject(optional = true)
	BankTagsService bankTagsService;

	// base (game) and applied (ours) geometry of the static widgets we touch, keyed by component id
	private final Map<Integer, Track> positions = new HashMap<>();
	private final Map<Integer, Track> sizes = new HashMap<>();

	private EventBus.Subscriber frameSubscriber;
	private volatile boolean active;
	private int[] appliedSignature;
	private int rebuildCooldown;
	private int reflowedWidth;

	private static final class Track
	{
		private final int[] base;
		private int[] applied;

		private Track(int[] base)
		{
			this.base = base;
		}
	}

	void startUp()
	{
        inventoryWidgets.setClient(client);
		active = true;controlDrag.resetInput();
        if(mouseManager!=null)mouseManager.registerMouseListener(0,controlDrag);
        if(overlayManager!=null)overlayManager.add(controlDrag.overlay);
		eventBus.register(this);
		// The frame has to be in place before the bank tags plugin sizes its tab column on this event,
		// the item grid can only be reflowed after it has laid out its tag tabs.
		frameSubscriber = eventBus.register(ScriptPreFired.class, this::layoutFrame, 10);
		clientThread.invokeLater(this::rebuildBank);
	}

	void shutDown()
	{
		active = false;
        controlDrag.resetInput();
        if(mouseManager!=null)mouseManager.unregisterMouseListener(controlDrag);
        if(overlayManager!=null)overlayManager.remove(controlDrag.overlay);
		eventBus.unregister(this);
		eventBus.unregister(frameSubscriber);
		clientThread.invokeLater(() ->
		{
			restore();
			rebuildBank();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("windowmarkers".equals(event.getGroup()) && Arrays.asList("bankLayoutEnabled","customWidth","width","customHeight","height","columns","depositButtons","alignDepositRight","tabAlignment","tabGap","setupSearchLayout","customBankControls","bankControlLayout","bankButtonDragging").contains(event.getKey()))
		{
            controlDrag.clear();
			clientThread.invokeLater(() -> { restoreTabs(); restore(); rebuildBank(); });
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
            controlDrag.clear();controls.clear();
            inventoryWidgets.clear();
            hiddenSeparators.clear();
            viewGeometry.clear(); searchHeadings.clear(); inventoryNotes.clear();
            // Restore surviving widgets as well as the modal. An unfinished Alt-drag
            // must not leave changed quantity children behind on a soft close.
            restore();
		}
	}

    @Subscribe public void onProfileChanged(net.runelite.client.events.ProfileChanged event)
    { controlDrag.clear();clientThread.invokeLater(() -> { restore(); rebuildBank(); }); }

	private void layoutFrame(ScriptPreFired event)
	{
		if(event.getScriptId()==ScriptID.BANKMAIN_BUILD) {
            viewGeometry.clear(); searchHeadings.clear(); inventoryNotes.clear();
            inventoryWidgets.clear();
            // Restore before the native builder establishes visibility for this build.
            restoreSeparators();
        }
		if (active && config.bankLayoutEnabled() && event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING)
		{
			applyFrame();
		}
	}

	private void applyFrame()
	{
		if (!config.bankLayoutEnabled()) { return; }
		Widget universe = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		Widget container = client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (universe == null || universe.isHidden() || container == null || items == null || items.isHidden() && !settingsVisible())
		{
			return;
		}

		applyUniverse(universe);
        layoutSettings();
        if(items.isHidden()) {
            revalidateTree(universe);
            appliedSignature=signature(universe);
            return;
        }
		fillWidth(client.getWidget(InterfaceID.Bankmain.TABS));
		fillWidth(client.getWidget(InterfaceID.Bankmain.TABS_LINE0));
		universe.revalidate();
		container.revalidate();
        controls.previewWidth=container.getWidth();controls.previewHeight=container.getHeight();

		// the build script resets the item grid to its vanilla width every time
		int[] s = baseSize(items);
		int gap=Math.max(0,Math.min(40,config.tabGap()));
        BankControlLayout.Plan controlPlan=customControls()?controls.plan(container):null;
        int inset=controlPlan==null?0:controlPlan.inset;
        int[] itemPosition=basePos(items);
        setPos(items,itemPosition[0]+inset,itemPosition[1]+gap,itemPosition[2],itemPosition[3]);
        setSize(items, ITEMS_WIDTH_MARGIN+inset, s[3]==WidgetSizeMode.MINUS ? s[1]+gap : Math.max(1,s[1]-gap), WidgetSizeMode.MINUS, s[3]);
        Widget scrollbar=client.getWidget(InterfaceID.Bankmain.SCROLLBAR);
        if(scrollbar!=null)
        {
            int[] scrollPosition=basePos(scrollbar),scrollSize=baseSize(scrollbar);
            setPos(scrollbar,scrollPosition[0],scrollPosition[1]+gap,scrollPosition[2],scrollPosition[3]);
            setSize(scrollbar,scrollSize[0],scrollSize[3]==WidgetSizeMode.MINUS ? scrollSize[1]+gap : Math.max(1,scrollSize[1]-gap),scrollSize[2],scrollSize[3]);
        }

        if(controlPlan==null) layoutBottom(container);
        else controls.apply(container,controlPlan);
		revalidateTree(universe);
		appliedSignature = signature(universe);
        if(controlPlan!=null)controls.capture();
	}

	@Subscribe(priority = -10)
	public void onScriptPreFired(ScriptPreFired event)
	{
		if (!active || event.getScriptId() != ScriptID.BANKMAIN_FINISHBUILDING)
		{
			return;
		}

		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (items == null)
		{
			return;
		}

        boolean grouped=groupedSearch();
        inventoryWidgets.clear();
        if(!config.bankLayoutEnabled() && !grouped && !ownsPreset()) return;
		if(grouped) hideSeparators(items);
		int[] ends = grouped ? layoutSearch(items) : reflowItems(items);
        if(ownsPreset() && ends!=null) {
            String identity=presetIdentity(),account=configs==null?null:configs.getRSProfileKey();
            String key=presetPositions==null?null:presetPositions.key(identity,items.getWidth());
            BankPresetPositions.Layout saved=presetPositions==null?new BankPresetPositions.Layout():presetPositions.read(account,key);
            ends[1]=viewGeometry.capture(items,saved,() -> {
                if(presetPositions!=null && java.util.Objects.equals(identity,presetIdentity())
                    && java.util.Objects.equals(key,presetPositions.key(presetIdentity(),items.getWidth())))
                    presetPositions.save(account,key,saved);
            },inventoryNotes,ends[1]);
            inventoryWidgets.allowPresetDragging();
        }
		reflowedWidth = items.getWidth();
		if (ends == null)
		{
			return;
		}

		int[] stack = client.getIntStack();
		int idx = client.getIntStackSize() - FINISHBUILDING_HEIGHT_ARG;
		// The temporary view owns the whole displayed layout. A tag-layout
        // subscriber may already have replaced the native height argument.
        if (idx>=0 && (searchView!=null && searchView.isActive() || grouped && ends[0]==0))
        {
            stack[idx]=Math.max(1,ends[1]+8);
            return;
        }
		// only touch the argument if it looks like the height of the layout we just replaced
		if (idx >= 0 && stack[idx] > 0 && Math.abs(stack[idx] - ends[0]) <= 2 * ROW_HEIGHT)
		{
			stack[idx] = Math.max(1, stack[idx] + ends[1] - ends[0]);
		}
	}

	@Subscribe(priority=-20)
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (active && config.bankLayoutEnabled() && event.getScriptId() == ScriptID.BANKMAIN_SIZE_CHECK)
		{
			reapplyFrame();
		}
        if(active && config.bankLayoutEnabled() && event.getScriptId()==ScriptID.BANKMAIN_BUILD) alignTabs();
        // Bank Tag Layouts can auto-layout a temporary tag after finishbuilding.
        // Restore our current view's visible cells and positions, including
        // repeated supplies/phase gear; never touch another tag's layout.
        if(active && event.getScriptId()==ScriptID.BANKMAIN_BUILD && ownsPreset())
            viewGeometry.restore(client.getWidget(InterfaceID.Bankmain.ITEMS));
	}

    // Search refresh rebuilds the native tabs after ClientTick. Apply their final
    // positions after scripts, before any frame can display the intermediate layout.
    @Subscribe public void onFocusChanged(net.runelite.api.events.FocusChanged event) {if(!event.isFocused()){controlDrag.cancel();viewGeometry.cancelDrag();}}
    @Subscribe public void onGameStateChanged(net.runelite.api.events.GameStateChanged event) {
        if(event.getGameState()!=net.runelite.api.GameState.LOGGED_IN){controlDrag.cancel();viewGeometry.cancelDrag();}
    }
    @Subscribe(priority=-20)
    public void onBeforeRender(BeforeRender event)
    {
        if(active && config.bankLayoutEnabled()) {
            int update=controlDrag.update();if(update==2)restore();if(update>0)applyFrame();
            reapplyFrame();alignTabs();controlDrag.capture();
        }
        else controlDrag.clear();
        if(active && ownsPreset()) viewGeometry.restore(client.getWidget(InterfaceID.Bankmain.ITEMS));
    }

    // Run before Bank Tags snapshots the native drop target. Only display
    // positions in our current preset change; real bank order stays untouched.
    @Subscribe(priority=30)
    public void onWidgetDrag(net.runelite.api.events.WidgetDrag event)
    {
        if(!active || !ownsPreset()) return;
        Widget items=client.getWidget(InterfaceID.Bankmain.ITEMS);
        if(items==null || items.isHidden()) return;
        Widget dragged=client.getDraggedWidget();
        if(dragged!=null && dragged.getId()==InterfaceID.Bankmain.ITEMS) client.setDraggedOnWidget(null);
        String identity=presetIdentity();
        viewGeometry.drag(client,items,() -> active && ownsPreset()
            && java.util.Objects.equals(identity,presetIdentity()) && client.getWidget(InterfaceID.Bankmain.ITEMS)==items && !items.isHidden());
    }

    private boolean ownsPreset() { return searchView!=null && searchView.isActive() || groupedSearch(); }
    private String presetIdentity() {
        return searchView!=null && searchView.isActive()?searchView.layoutIdentity()+":"+searchView.bySlot():
            nativeSearch==null?null:nativeSearch.layoutIdentity()+":true";
    }

    void resetPresetPositions()
    {
        if(!active || !ownsPreset() || presetPositions==null || configs==null) return;
        Widget items=client.getWidget(InterfaceID.Bankmain.ITEMS);if(items==null || items.isHidden())return;
        presetPositions.save(configs.getRSProfileKey(),presetPositions.key(presetIdentity(),items.getWidth()),new BankPresetPositions.Layout());
        viewGeometry.clear();rebuildBank();
    }

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!active || !config.bankLayoutEnabled()) { return; }
		if (rebuildCooldown > 0)
		{
			rebuildCooldown--;
		}

		reapplyFrame();

		// Only width changes affect item wrapping. Finishbuilding may adjust the
		// viewport height after our hook; using that height here causes a rebuild loop.
		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (appliedSignature != null && items != null && !items.isHidden() && rebuildCooldown == 0
			&& items.getWidth() != reflowedWidth)
		{
			rebuildCooldown = REBUILD_COOLDOWN_TICKS;
			clientThread.invokeLater(this::rebuildBank);
		}
	}

    @Subscribe(priority=-20)
    public void onScriptCallbackEvent(net.runelite.api.events.ScriptCallbackEvent event)
    {
        if(active && searchView!=null) searchView.filter(event);
        // Single-tab mode explicitly closes a native search input in BankMainBuild.
        // Use it only for saved views outside chatbox search, never while typing.
        if(active && "bankBuildTab".equals(event.getEventName())
            && client.getWidget(InterfaceID.Bankmain.ITEMS)!=null
            && client.getVarcIntValue(VarClientID.MESLAYERMODE)!=InputType.SEARCH.getType()
            && searchView!=null && searchView.isActive()) {
            int size=client.getIntStackSize();
            if(size>0) client.getIntStack()[size-1]=1;
        }
    }

	/**
	 * The game puts the bank and the modal back to their own sizes whenever the size of either changes,
	 * which includes us resizing them. Putting ours back right away, before a frame is drawn, keeps that
	 * from showing.
	 */
	private void reapplyFrame()
	{
		Widget universe = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		if (universe != null && (appliedSignature != null && !Arrays.equals(signature(universe), appliedSignature)
            || appliedSignature == null && settingsVisible()
            || customControls() && !settingsVisible() && controls.changed()))
		{
			applyFrame();
		}
	}

	private void rebuildBank()
	{
		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (items == null || items.isHidden())
		{
			return;
		}

		Object[] build = items.getOnInvTransmitListener();
		if (build != null)
		{
			client.runScript(build);
		}
	}

	private int[] signature(Widget universe)
	{
		Widget modal = modal();
		Widget area = modal == null ? null : modal.getParent();
		return new int[]{
			universe.getOriginalX(), universe.getOriginalY(), universe.getXPositionMode(), universe.getYPositionMode(),
			universe.getOriginalWidth(), universe.getOriginalHeight(), universe.getWidthMode(), universe.getHeightMode(),
			modal == null ? 0 : modal.getOriginalWidth(), modal == null ? 0 : modal.getOriginalHeight(),
			area == null ? 0 : area.getWidth(), area == null ? 0 : area.getHeight(),settingsVisible()?1:0
		};
	}

	/**
	 * The top level interface's container the bank is opened in. It is 512px wide and clips the bank.
	 */
	private Widget modal()
	{
		Widget infinite = client.getWidget(InterfaceID.Bankmain.INFINITE);
		return infinite == null ? null : infinite.getParent();
	}

	private void applyModal(Widget modal)
	{
		Widget area = modal.getParent();
		int[] s = baseSize(modal);
		int width = s[0];
		int height = s[1];
		// in resizable mode the game already stretches the modal over the area's full height for the bank
		if (area != null && s[2] == WidgetSizeMode.ABSOLUTE && config.customWidth())
		{
			width = Math.max(width, Math.min(config.width(), area.getWidth()));
		}
		if (area != null && s[3] == WidgetSizeMode.ABSOLUTE && config.customHeight())
		{
			height = Math.max(height, Math.min(config.height() + MODAL_HEIGHT_MARGIN, area.getHeight()));
		}

		setSize(modal, width, height, s[2], s[3]);
		// The bank's root is sized from the modal by the engine. It has no parent component of its own,
		// so revalidating it directly would size it to the whole canvas.
		modal.revalidateScroll();
	}

	private void applyUniverse(Widget universe)
	{
		Widget parent = client.getWidget(InterfaceID.Bankmain.INFINITE);
		Widget modal = modal();
		if (modal != null)
		{
			applyModal(modal);
		}

		int[] p = basePos(universe);
		int[] s = baseSize(universe);
		int width = s[0];
		int height = s[1];
		if (config.customWidth() && s[2] == WidgetSizeMode.ABSOLUTE)
		{
			int max = parent == null ? s[0] : parent.getWidth();
			width = Math.max(s[0], Math.min(config.width(), max));
		}
		if (config.customHeight() && s[3] == WidgetSizeMode.ABSOLUTE)
		{
			int max = parent == null ? s[1] : parent.getHeight() - MODAL_HEIGHT_MARGIN;
			height = Math.max(Math.min(s[1], MIN_BANK_HEIGHT), Math.min(config.height(), Math.max(s[1], max)));
		}

        // The native settings form needs its full height even when the item view is short.
        if(settingsVisible() && s[3]==WidgetSizeMode.ABSOLUTE) height=Math.max(height,SETTINGS_TOP+SETTINGS_HEIGHT+5);
		setSize(universe, width, height, s[2], s[3]);
		if (height != s[1])
		{
			setPos(universe, p[0], 0, p[2], WidgetPositionMode.ABSOLUTE_CENTER);
		}
		else
		{
			setPos(universe, p[0], p[1], p[2], p[3]);
		}
	}

    private boolean settingsVisible() {
        Widget settings=client.getWidget(InterfaceID.Bankmain.MENU_CONTAINER);
        return settings!=null && !settings.isHidden();
    }

    private void layoutSettings() {
        Widget settings=client.getWidget(InterfaceID.Bankmain.MENU_CONTAINER);
        if(settings==null) return;
        // Native checkbox scripts anchor the two columns 251px from opposite
        // edges. Stretching their parent makes them overlap. Keep the whole
        // native form centred at its designed size, including its click targets.
        setSize(settings,SETTINGS_WIDTH,SETTINGS_HEIGHT,WidgetSizeMode.ABSOLUTE,WidgetSizeMode.ABSOLUTE);
        setPos(settings,0,SETTINGS_TOP,WidgetPositionMode.ABSOLUTE_CENTER,WidgetPositionMode.ABSOLUTE_TOP);
        for(int i=0;i<SETTINGS_ROWS.length;i++) {
            Widget row=client.getWidget(SETTINGS_ROWS[i]);if(row==null) continue;
            int[] p=basePos(row);
            // The first three rows start centred, then native init freezes their
            // Y in pixels. Normalize that Y too when opening a tall bank.
            setPos(row,p[0],23+(i/2)*28,p[2],WidgetPositionMode.ABSOLUTE_TOP);
        }
    }

	private void fillWidth(Widget widget)
	{
		if (widget != null)
		{
			int[] s = baseSize(widget);
			setSize(widget, 0, s[1], WidgetSizeMode.MINUS, s[3]);
		}
	}

	/**
	 * Lays out the bottom bar. The deposit buttons either stay in it, following the right edge of a widened
	 * bank, or are stacked at the bottom of the strip left of the items, pushing that strip's own buttons up.
	 */
	private void layoutBottom(Widget container)
	{
		Widget bottom = client.getWidget(InterfaceID.Bankmain.BOTTOM);
		if (bottom == null)
		{
			return;
		}

		int[] bp = basePos(bottom);
		int[] bs = baseSize(bottom);

		List<Widget[]> deposits = new ArrayList<>();
		if (config.depositButtons() == DepositButtons.LEFT && bp[3] == WidgetPositionMode.ABSOLUTE_BOTTOM && bs[3] == WidgetSizeMode.ABSOLUTE)
		{
			for (int[] ids : DEPOSIT_BUTTONS)
			{
				Widget button = client.getWidget(ids[0]);
				Widget icon = client.getWidget(ids[1]);
				if (button != null && icon != null && !button.isSelfHidden())
				{
					deposits.add(new Widget[]{button, icon});
				}
			}
		}
		boolean left = !deposits.isEmpty();

		// The deposit buttons can't leave the bar's layer, so the layer grows upwards over the strip
		// instead and everything else in it moves down by the same amount. Kept even so that vertically
		// centred children stay where they were.
		int grow = left ? LEFT_STRIP_GAP + deposits.size() * BUTTON_PITCH : 0;
		grow += grow & 1;
		setPos(bottom, bp[0], bp[1], bp[2], bp[3]);
		setSize(bottom, bs[0], bs[1] + grow, bs[2], bs[3]);

		int alignRight = !left && config.alignDepositRight()
			? Math.max(0, container.getWidth() - VANILLA_CONTAINER_WIDTH) : 0;
		for (Widget child : bottom.getStaticChildren())
		{
			if (left && isDepositButton(child.getId()))
			{
				continue;
			}

			int[] p = basePos(child);
			int[] s = baseSize(child);
			int x = p[0];
			int y = p[1];
			if (p[2] == WidgetPositionMode.ABSOLUTE_LEFT && x >= DEPOSIT_GROUP_X)
			{
				x += alignRight;
			}
			if (left && child.getId() == InterfaceID.Bankmain.DEPOSIT_LINE)
			{
				// nothing left to divide; park it out of view
				x = -1000;
			}
			if (p[3] == WidgetPositionMode.ABSOLUTE_TOP)
			{
				y += grow;
			}
			else if (p[3] == WidgetPositionMode.ABSOLUTE_CENTER)
			{
				y += grow / 2;
			}

			setPos(child, x, y, p[2], p[3]);
			setSize(child, s[0], s[3] == WidgetSizeMode.MINUS ? s[1] + grow : s[1], s[2], s[3]);
		}

		for (int i = 0; i < deposits.size(); i++)
		{
			Widget button = deposits.get(i)[0];
			Widget icon = deposits.get(i)[1];
			int[] p = basePos(button);
			int[] ip = basePos(icon);
			int y = i * BUTTON_PITCH;
			setPos(button, LEFT_STRIP_X, y, WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_TOP);
			setPos(icon, LEFT_STRIP_X + ip[0] - p[0], y + ip[1] - p[1], WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_TOP);
		}

		int lift = deposits.size() * BUTTON_PITCH;
		for (int id : LEFT_STRIP)
		{
			Widget widget = client.getWidget(id);
			if (widget != null)
			{
				int[] p = basePos(widget);
				setPos(widget, p[0], p[3] == WidgetPositionMode.ABSOLUTE_BOTTOM ? p[1] + lift : p[1], p[2], p[3]);
			}
		}
	}

	private static boolean isDepositButton(int id)
	{
		for (int[] ids : DEPOSIT_BUTTONS)
		{
			if (id == ids[0] || id == ids[1])
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Re-wraps the item grid the build script just laid out in 8 columns to the columns that fit now.
	 *
	 * @return {old content end, new content end}, or null if nothing was reflowed
	 */
	private int[] reflowItems(Widget items)
	{
		Widget[] children = items.getDynamicChildren();
		if (children == null)
		{
			return null;
		}

		int usable = items.getWidth() - LEFT_INSET - RIGHT_INSET;
		List<Widget> cells = new ArrayList<>();
		List<Widget> separators = new ArrayList<>();
        Map<Widget,Integer> tabDropAreas=new java.util.IdentityHashMap<>();
		for (Widget child : children)
		{
			if (child == null)
			{
				continue;
			}

            // A native tab's unused trailing slot is also 36x32 when the last
            // row contains seven items. It is a transparent RECTANGLE, not an
            // item; counting it creates an entire blank row after rewrapping.
			boolean cell = child.getType()==WidgetType.GRAPHIC && child.getItemId()>=0
                && child.getOriginalWidth() == CELL_WIDTH && child.getOriginalHeight() == CELL_HEIGHT;
            boolean tabDropArea=child.getType()==WidgetType.RECTANGLE && child.getOriginalHeight()==CELL_HEIGHT
                && child.getOpacity()==255 && child.isFilled() && child.getItemId()<0;
			if (!cell && !tabDropArea && child.getOriginalWidth() >= SEPARATOR_MIN_WIDTH && child.getOriginalWidth() != usable)
			{
				// tab separator lines and labels are created once, at the vanilla width
				child.setOriginalWidth(usable);
				child.revalidate();
			}

			if (!child.isSelfHidden())
			{
                if(tabDropArea) tabDropAreas.put(child,child.getOriginalY());
                else (cell ? cells : separators).add(child);
			}
		}

		// tag layouts are designed around the 8 column grid
		if (cells.isEmpty())
		{
			return null;
		}
        if (bankTagsService != null && bankTagsService.getActiveLayout() != null && !(searchView!=null && searchView.isActive()))
        {
            int end=0;
            for(Widget cell:cells) { end=Math.max(end,cell.getOriginalY()+ROW_HEIGHT); cell.setOriginalY(cell.getOriginalY()+4); cell.revalidate(); }
            for(Widget separator:separators) { separator.setOriginalY(separator.getOriginalY()+4); separator.revalidate(); }
            for(Widget area:tabDropAreas.keySet()) { area.setOriginalY(area.getOriginalY()+4);area.revalidate(); }
            return new int[]{end,end+4};
        }

		int columns = columns(usable);
		int pitch = columns > 1 ? CELL_WIDTH + (usable - columns * CELL_WIDTH) / (columns - 1) : CELL_WIDTH;

		cells.sort(Comparator.comparingInt(Widget::getOriginalY).thenComparingInt(Widget::getOriginalX));
		int count = cells.size();
		int[] oldY = new int[count];
		for (int i = 0; i < count; i++)
		{
			oldY[i] = cells.get(i).getOriginalY();
		}

		// {old start y, shift} of each section; in the "all items" view each tab is its own section
		List<int[]> shifts = new ArrayList<>();
		int oldEnd = 0;
		int newEnd = 4;
		int start = 0;
		while (start < count)
		{
			// a section is a run of full rows, directly below each other, closed by its last row
			int end = start + 1;
			int rowStart = start;
			while (end < count)
			{
				if (oldY[end] != oldY[end - 1])
				{
					if (oldY[end] - oldY[end - 1] != ROW_HEIGHT || end - rowStart != VANILLA_COLUMNS)
					{
						break;
					}
					rowStart = end;
				}
				end++;
			}

			// Native tab gaps can include a spare eight-column row. It is no longer
            // needed after wrapping to a wider bank; retain room for the tab heading.
            int gap=Math.max(0,oldY[start]-oldEnd);
			int newStart = newEnd + (start==0?gap:Math.min(24,gap));
			shifts.add(new int[]{oldY[start], newStart - oldY[start]});
			for (int i = start; i < end; i++)
			{
				Widget cell = cells.get(i);
				cell.setOriginalX(LEFT_INSET + ((i - start) % columns) * pitch);
				cell.setOriginalY(newStart + ((i - start) / columns) * ROW_HEIGHT);
				cell.revalidate();
			}

			newEnd = newStart + ((end - start + columns - 1) / columns) * ROW_HEIGHT;
            for(Map.Entry<Widget,Integer> entry:tabDropAreas.entrySet()) {
                if(entry.getValue()!=oldY[end-1]+ROW_HEIGHT-CELL_HEIGHT) continue;
                // Keep the native drag target on this tab's unused row tail,
                // never over real items or on a new row of its own.
                int used=(end-start)%columns,free=used==0?0:columns-used;
                Widget area=entry.getKey();
                area.setOriginalX(LEFT_INSET+used*pitch);
                area.setOriginalY(newEnd-CELL_HEIGHT);
                area.setOriginalWidth(free==0?0:free*CELL_WIDTH+(free-1)*(pitch-CELL_WIDTH));
                area.revalidate();
            }
			oldEnd = oldY[end - 1] + ROW_HEIGHT;
			start = end;
		}

		for (Widget separator : separators)
		{
			// a separator belongs to the section it sits above
			int y = separator.getOriginalY();
			int shift = newEnd - oldEnd;
			for (int[] section : shifts)
			{
				if (section[0] >= y)
				{
					shift = section[1];
					break;
				}
			}

			if (shift != 0)
			{
				separator.setOriginalY(y + shift);
				separator.revalidate();
			}
		}

		return new int[]{oldEnd, newEnd};
	}

	private int columns(int usable)
	{
		int columns = config.columns() > 0 ? config.columns() : (usable + VANILLA_GAP) / (CELL_WIDTH + VANILLA_GAP);
		return Math.max(1, Math.min(columns, (usable + MIN_GAP) / (CELL_WIDTH + MIN_GAP)));
	}

    private boolean groupedSearch()
    {
        if(!config.setupSearchLayout()) return false;
        if(searchView!=null && searchView.isActive()) return searchView.bySlot();
        if(bankTagsService!=null && bankTagsService.getActiveLayout()!=null) return false;
        return (bankTagsService==null || bankTagsService.getActiveBankTag()==null)
            && nativeSearch!=null && nativeSearch.groupBySlot();
    }

    private final java.util.List<SetupItemLayout.Heading> searchHeadings=new java.util.ArrayList<>();
    private final java.util.List<SetupItemLayout.InventoryNote> inventoryNotes=new java.util.ArrayList<>();
    java.util.List<SetupItemLayout.InventoryNote> inventoryNotes() { return groupedSearch()?viewGeometry.visibleNotes(inventoryNotes):java.util.Collections.emptyList(); }
    boolean inventoryPreview(Widget widget) { return inventoryWidgets.preview(widget); }
    @Subscribe(priority=20) public void onMenuEntryAdded(net.runelite.api.events.MenuEntryAdded event) { inventoryWidgets.menuAdded(event.getMenuEntry()); }
    @Subscribe(priority=20) public void onMenuOptionClicked(net.runelite.api.events.MenuOptionClicked event) { inventoryWidgets.clicked(event,client); }
    java.util.List<SetupItemLayout.Heading> searchHeadings() { return groupedSearch()?searchHeadings.stream()
        .filter(h -> !h.emptySlot || !viewGeometry.occupied(h.x,h.y)).collect(java.util.stream.Collectors.toList()):java.util.Collections.emptyList(); }
    private int[] layoutSearch(Widget items)
    {
        int usable=items.getWidth()-LEFT_INSET-RIGHT_INSET;
        int count=Math.max(3,Math.min(8,(usable+VANILLA_GAP)/(CELL_WIDTH+VANILLA_GAP)));
        int pitch=CELL_WIDTH+VANILLA_GAP;
        boolean view=searchView!=null && searchView.isActive();
        return SetupItemLayout.apply(items,itemManager,count,pitch,4,view?searchView.loadout():nativeSearch.loadout(),
            view?searchView.ranks():nativeSearch.ranks(),searchHeadings,inventoryWidgets,inventoryNotes);
    }

    private final java.util.Set<Widget> hiddenSeparators=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private void hideSeparators(Widget items)
    {
        Widget[] children=items.getDynamicChildren();
        if(children==null) return;
        for(Widget child:children) {
            if(child==null || child.isSelfHidden() || child.getItemId()>=0) continue;
            int type=child.getType();
            boolean heading=type==WidgetType.TEXT && child.getText()!=null
                && net.runelite.client.util.Text.removeTags(child.getText()).matches("Tab (?:[1-9]|[IVX]+)");
            boolean line=(type==WidgetType.LINE || type==WidgetType.RECTANGLE)
                && child.getOriginalWidth()>=SEPARATOR_MIN_WIDTH && child.getOriginalHeight()<=2;
            boolean spriteLine=type==WidgetType.GRAPHIC && child.getSpriteId()==BANK_TAB_SEPARATOR_SPRITE
                && child.getOriginalWidth()>0 && child.getOriginalHeight()==2;
            if(heading || line || spriteLine) { hiddenSeparators.add(child);child.setHidden(true); }
        }
    }

    private void restoreSeparators()
    {
        for(Widget separator:hiddenSeparators) separator.setHidden(false);
        hiddenSeparators.clear();
    }

	private void restore()
	{
        viewGeometry.clear(); searchHeadings.clear(); inventoryNotes.clear();
        inventoryWidgets.clear();
        restoreSeparators();
		restoreTabs();
        controls.clear();
		Widget modal = modal();
		restore(positions, true, true);
		restore(sizes, false, true);
		positions.clear();
		sizes.clear();
		appliedSignature = null;

		Widget universe = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
		Widget items = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (modal == null || universe == null || items == null)
		{
			return;
		}

		modal.revalidateScroll();
		revalidateTree(universe);
		Widget[] children = items.getDynamicChildren();
		if (children == null)
		{
			return;
		}

		int usable = items.getWidth() - LEFT_INSET - RIGHT_INSET;
		for (Widget child : children)
		{
			if (child != null && child.getType()!=WidgetType.RECTANGLE && child.getOriginalWidth() >= SEPARATOR_MIN_WIDTH)
			{
				child.setOriginalWidth(usable);
				child.revalidate();
			}
		}
	}

	private void restore(Map<Integer, Track> tracks, boolean position, boolean bank)
	{
		for (Map.Entry<Integer, Track> entry : tracks.entrySet())
		{
			if (!bank && WidgetUtil.componentToInterface(entry.getKey()) == InterfaceID.BANKMAIN)
			{
				continue;
			}

			Widget widget = client.getWidget(entry.getKey());
			if (widget == null || entry.getValue().applied == null)
			{
				continue;
			}

			if (position)
			{
				int[] p = basePos(widget);
				widget.setPos(p[0], p[1], p[2], p[3]);
			}
			else
			{
				int[] s = baseSize(widget);
				widget.setSize(s[0], s[1], s[2], s[3]);
			}

			if (!bank)
			{
				widget.revalidateScroll();
			}
		}
	}

	private void revalidateTree(Widget widget)
	{
		widget.revalidate();
		for (Widget child : widget.getStaticChildren())
		{
			revalidateTree(child);
		}

		Widget[] children = widget.getDynamicChildren();
		if (children != null)
		{
			for (Widget child : children)
			{
				if (child != null)
				{
					child.revalidate();
				}
			}
		}
	}

	private int[] basePos(Widget widget)
	{
		return base(positions, widget.getId(), new int[]{
			widget.getOriginalX(), widget.getOriginalY(), widget.getXPositionMode(), widget.getYPositionMode()
		});
	}

	private int[] baseSize(Widget widget)
	{
		return base(sizes, widget.getId(), new int[]{
			widget.getOriginalWidth(), widget.getOriginalHeight(), widget.getWidthMode(), widget.getHeightMode()
		});
	}

	/**
	 * The geometry the game gave a widget. Scripts reposition some widgets on every bank build, sometimes
	 * only on one axis, so any field that no longer holds what we last wrote is taken as the game's again.
	 */
	private static int[] base(Map<Integer, Track> tracks, int id, int[] current)
	{
		Track track = tracks.get(id);
		if (track == null)
		{
			track = new Track(current);
			tracks.put(id, track);
		}
		else
		{
			for (int i = 0; i < current.length; i++)
			{
				if (track.applied == null || current[i] != track.applied[i])
				{
					track.base[i] = current[i];
				}
			}
		}
		return track.base.clone();
	}

	void setPos(Widget widget, int x, int y, int xMode, int yMode)
	{
		basePos(widget);
		widget.setPos(x, y, xMode, yMode);
		positions.get(widget.getId()).applied = new int[]{x, y, xMode, yMode};
	}

	void setSize(Widget widget, int width, int height, int widthMode, int heightMode)
	{
		baseSize(widget);
		widget.setSize(width, height, widthMode, heightMode);
		sizes.get(widget.getId()).applied = new int[]{width, height, widthMode, heightMode};
	}

    private final Map<Widget,int[]> tabPositions=new java.util.IdentityHashMap<>();
    private void restoreTabs()
    {
        for(Map.Entry<Widget,int[]> e:tabPositions.entrySet())
        {
            Widget tab=e.getKey(); int[] track=e.getValue();
            if(tab.getOriginalX()==track[1]) { tab.setOriginalX(track[0]); tab.revalidate(); }
        }
        tabPositions.clear();
    }
    private void alignTabs()
    {
        Widget tabs=client.getWidget(InterfaceID.Bankmain.TABS);
        if(tabs==null || tabs.isHidden()) { tabPositions.clear(); return; }
        List<Widget> children=new ArrayList<>();
        if(tabs.getDynamicChildren()!=null) { children.addAll(Arrays.asList(tabs.getDynamicChildren())); }
        if(tabs.getStaticChildren()!=null) { children.addAll(Arrays.asList(tabs.getStaticChildren())); }
        children.removeIf(w -> w==null || w.isSelfHidden() || w.getWidth()>80 || w.getHeight()>60 || w.getWidth()<=0);
        tabPositions.keySet().retainAll(children);
        if(children.isEmpty()) { return; }
        int left=Integer.MAX_VALUE,right=0;
        for(Widget tab:children)
        {
            int[] track=tabPositions.computeIfAbsent(tab,w -> new int[]{w.getOriginalX(),w.getOriginalX()});
            if(tab.getOriginalX()!=track[1]) { track[0]=tab.getOriginalX(); }
            left=Math.min(left,track[0]); right=Math.max(right,track[0]+tab.getWidth());
        }
        int offset=LEFT_INSET+tabOffset(Math.max(0,tabs.getWidth()-LEFT_INSET-RIGHT_INSET),right-left,config.tabAlignment())-left;
        for(Widget tab:children)
        {
            int[] track=tabPositions.get(tab); int x=track[0]+offset;
            if(tab.getOriginalX()!=x) { tab.setOriginalX(x); tab.revalidate(); }
            track[1]=x;
        }
    }
    static int tabOffset(int available,int span,WindowMarkersConfig.TabAlignment alignment)
    {
        int spare=Math.max(0,available-span);
        return alignment==WindowMarkersConfig.TabAlignment.RIGHT ? spare : alignment==WindowMarkersConfig.TabAlignment.CENTER ? spare/2 : 0;
    }
}

