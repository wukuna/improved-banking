package com.windowmarkers;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.ImageCapture;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankLayoutTest
{
    @Test public void temporaryViewKeepsLayoutAndScrollRangeAfterAnotherLayoutSubscriber() {
        BankLayoutController c=controller();when(c.config.setupSearchLayout()).thenReturn(true);
        c.searchView=new BankSearchView();c.itemManager=mock(ItemManager.class);
        c.searchView.bankTags=mock(net.runelite.client.plugins.banktags.BankTagsService.class);
        c.searchView.tags=mock(net.runelite.client.plugins.banktags.TagManager.class);c.searchView.items=c.itemManager;
        doAnswer(call -> {when(c.searchView.bankTags.getActiveTag()).thenReturn(call.getArgument(0));return null;})
            .when(c.searchView.bankTags).openBankTag(anyString(),anyInt());
        Marker group=new Marker(0,0,48,36);group.linkedItemIds=new int[]{385};
        c.searchView.show("Food",Collections.singletonList(group),true);
        Widget items=mock(Widget.class),item=cell(51,360);
        when(item.getIndex()).thenReturn(0);when(item.getItemId()).thenReturn(385);
        when(c.itemManager.canonicalize(385)).thenReturn(385);
        when(items.getWidth()).thenReturn(478);when(items.getDynamicChildren()).thenReturn(new Widget[]{item});
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items);
        int[] stack=new int[9];stack[0]=9000; // Height already changed by the competing layout.
        when(c.client.getIntStack()).thenReturn(stack);when(c.client.getIntStackSize()).thenReturn(9);
        c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));
        int x=item.getOriginalX(),y=item.getOriginalY();assertEquals(48,stack[0]);
        item.setOriginalX(300);item.setOriginalY(800);
        c.onScriptPostFired(new ScriptPostFired(ScriptID.BANKMAIN_BUILD));
        assertEquals(x,item.getOriginalX());assertEquals(y,item.getOriginalY());
        c.searchView.clear();item.setOriginalY(500);
        c.onScriptPostFired(new ScriptPostFired(ScriptID.BANKMAIN_BUILD));
        assertEquals("Switching away releases ownership",500,item.getOriginalY());
        verify(c.client,never()).runScript(any(Object[].class));
    }
    @Test public void typingAnActivityFilterMustNotSelectTheInputClosingSingleTabMode() throws Exception {
        BankLayoutController c=controller();
        c.nativeSearch=new NativeBankSearch();c.nativeSearch.client=c.client;c.nativeSearch.config=c.config;
        c.nativeSearch.bankTags=mock(net.runelite.client.plugins.banktags.BankTagsService.class);
        when(c.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(c.client.getWidget(InterfaceID.Bankmain.UNIVERSE)).thenReturn(mock(Widget.class));
        when(c.config.improvedBankSearch()).thenReturn(true);
        when(c.config.setupSearchLayout()).thenReturn(true);
        for(String field:new String[]{"query","slots","matches"}) {
            java.lang.reflect.Field f=NativeBankSearch.class.getDeclaredField(field);f.setAccessible(true);
            f.set(c.nativeSearch,field.equals("query")?"cox":field.equals("slots")?true:Collections.singleton(1));
        }
        c.nativeSearch.start();
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(mock(Widget.class));
        when(c.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE))
            .thenReturn(net.runelite.api.vars.InputType.SEARCH.getType());
        when(c.client.getVarcStrValue(net.runelite.api.gameval.VarClientID.MESLAYERINPUT)).thenReturn("cox");
        int[] stack={0};when(c.client.getIntStack()).thenReturn(stack);when(c.client.getIntStackSize()).thenReturn(1);
        ScriptCallbackEvent event=new ScriptCallbackEvent();event.setEventName("bankBuildTab");
        c.onScriptCallbackEvent(event);
        // BankMainBuild's single-tab branch calls meslayer_close when search is active.
        assertEquals("Keep the native search builder so its input remains open",0,stack[0]);
        verify(c.client,never()).runScript(any(Object[].class));
    }

    @Test public void viewportHeightChangesDoNotRepeatedlyRebuildTheBankButWidthChangesDo() throws Exception {
        BankLayoutController c=controller();Widget items=mock(Widget.class);
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items);
        when(items.getWidth()).thenReturn(478);when(items.getHeight()).thenReturn(250);
        java.lang.reflect.Field signature=BankLayoutController.class.getDeclaredField("appliedSignature");signature.setAccessible(true);signature.set(c,new int[12]);
        java.lang.reflect.Field width=BankLayoutController.class.getDeclaredField("reflowedWidth");width.setAccessible(true);width.set(c,478);
        clearInvocations(c.clientThread);
        for(int i=0;i<30;i++) { when(items.getHeight()).thenReturn(250+i);c.onClientTick(new ClientTick()); }
        verifyNoInteractions(c.clientThread);
        when(items.getWidth()).thenReturn(600);c.onClientTick(new ClientTick());
        verify(c.clientThread,times(1)).invokeLater(any(Runnable.class));
    }
    @Test public void aCustomViewUsesNativeSingleSectionBuildAndRestoresOrdinaryTabs() {
        BankLayoutController c=controller();c.searchView=new BankSearchView();
        c.searchView.bankTags=mock(net.runelite.client.plugins.banktags.BankTagsService.class);
        c.searchView.tags=mock(net.runelite.client.plugins.banktags.TagManager.class);c.searchView.items=mock(ItemManager.class);
        doAnswer(call -> { when(c.searchView.bankTags.getActiveTag()).thenReturn(call.getArgument(0));return null; })
            .when(c.searchView.bankTags).openBankTag(anyString(),anyInt());
        Marker group=new Marker(0,0,48,36);group.linkedItemIds=new int[]{385};
        c.searchView.show("Food",Collections.singletonList(group),false);
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(mock(Widget.class));
        int[] stack={9};when(c.client.getIntStack()).thenReturn(stack);when(c.client.getIntStackSize()).thenReturn(1);
        ScriptCallbackEvent event=new ScriptCallbackEvent();event.setEventName("bankBuildTab");
        c.onScriptCallbackEvent(event);assertEquals(1,stack[0]);
        when(c.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE))
            .thenReturn(net.runelite.api.vars.InputType.SEARCH.getType());
        stack[0]=0;c.onScriptCallbackEvent(event);assertEquals(0,stack[0]);
        when(c.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE)).thenReturn(0);
        stack[0]=9;c.searchView.clear();c.onScriptCallbackEvent(event);assertEquals(9,stack[0]);
        c.searchView.show("Food",Collections.singletonList(group),false);
        event.setEventName("differentCallback");c.onScriptCallbackEvent(event);assertEquals(9,stack[0]);
    }
    @Test public void groupedResultsHideOnlyTabDecorationsAndRestoreBeforeNativeBuild() throws Exception {
        BankLayoutController c=controller();when(c.config.setupSearchLayout()).thenReturn(true);
        c.searchView=new BankSearchView();
        c.searchView.bankTags=mock(net.runelite.client.plugins.banktags.BankTagsService.class);
        c.searchView.tags=mock(net.runelite.client.plugins.banktags.TagManager.class);c.searchView.items=mock(ItemManager.class);
        doAnswer(call -> {when(c.searchView.bankTags.getActiveTag()).thenReturn(call.getArgument(0));return null;})
            .when(c.searchView.bankTags).openBankTag(anyString(),anyInt());
        Marker group=new Marker(0,0,48,36);group.linkedItemIds=new int[]{385};
        c.searchView.show("cox",Collections.singletonList(group),true);
        Widget items=mock(Widget.class);
        Widget title=decoration(net.runelite.api.widgets.WidgetType.TEXT,"Tab 5",false);
        Widget line=decoration(net.runelite.api.widgets.WidgetType.GRAPHIC,"",false);
        when(line.getSpriteId()).thenReturn(897); // Native bankmain_init separator sprite.
        when(line.getOriginalHeight()).thenReturn(2);
        Widget unrelatedSprite=decoration(net.runelite.api.widgets.WidgetType.GRAPHIC,"",false);
        when(unrelatedSprite.getOriginalHeight()).thenReturn(2);when(unrelatedSprite.getSpriteId()).thenReturn(898);
        Widget hidden=decoration(net.runelite.api.widgets.WidgetType.TEXT,"Tab 2",true);
        Widget other=decoration(net.runelite.api.widgets.WidgetType.TEXT,"Another plugin's label",false);
        Widget item=decoration(net.runelite.api.widgets.WidgetType.GRAPHIC,"",false);when(item.getItemId()).thenReturn(385);
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items);
        when(items.getDynamicChildren()).thenReturn(new Widget[]{title,line,hidden,other,item,unrelatedSprite});
        c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));
        assertTrue(title.isSelfHidden());assertTrue(line.isSelfHidden());assertTrue(hidden.isSelfHidden());
        assertFalse(other.isSelfHidden());assertFalse(item.isSelfHidden());assertFalse(unrelatedSprite.isSelfHidden());
        // Exercise the real registered pre-build hook before native scripts set visibility.
        ArgumentCaptor<java.util.function.Consumer<ScriptPreFired>> hook=ArgumentCaptor.forClass(java.util.function.Consumer.class);
        verify(c.eventBus).register(eq(ScriptPreFired.class),hook.capture(),eq(10f));
        hook.getValue().accept(new ScriptPreFired(ScriptID.BANKMAIN_BUILD));
        assertFalse(title.isSelfHidden());assertFalse(line.isSelfHidden());
        verify(hidden,never()).setHidden(false);
        title.setHidden(true); // The new native build hides this tab itself.
        c.searchView.clear();
        c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));
        assertTrue(title.isSelfHidden());assertFalse(line.isSelfHidden());
        verify(c.client,never()).runScript(any(Object[].class));
    }
    @Test public void temporaryViewRejectsItemsAdmittedByAnotherFilter() {
        BankLayoutController c=controller();c.searchView=new BankSearchView();
        c.searchView.client=c.client;c.searchView.bankTags=mock(net.runelite.client.plugins.banktags.BankTagsService.class);
        c.searchView.tags=mock(net.runelite.client.plugins.banktags.TagManager.class);c.searchView.items=mock(ItemManager.class);
        when(c.searchView.items.canonicalize(anyInt())).thenAnswer(call -> call.getArgument(0));
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(mock(Widget.class));
        doAnswer(call -> {when(c.searchView.bankTags.getActiveTag()).thenReturn(call.getArgument(0));return null;})
            .when(c.searchView.bankTags).openBankTag(anyString(),anyInt());
        Marker group=new Marker(0,0,48,36);group.linkedItemIds=new int[]{25731};
        c.searchView.show("Boss",Collections.singletonList(group),true);
        int[] stack={1,2347};when(c.client.getIntStack()).thenReturn(stack);when(c.client.getIntStackSize()).thenReturn(2);
        ScriptCallbackEvent event=new ScriptCallbackEvent();event.setEventName("bankSearchFilter");
        EventBus bus=new EventBus();bus.register(c);
        bus.register(ScriptCallbackEvent.class,e -> stack[0]=1,0);
        bus.post(event);assertEquals("An unrelated hammer must not leak into the preset",0,stack[0]);
        stack[1]=25731;bus.post(event);assertEquals(1,stack[0]);
        stack[1]=25733;bus.post(event);assertEquals("The uncharged state was not selected",0,stack[0]);
        when(c.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE))
            .thenReturn(net.runelite.api.vars.InputType.SEARCH.getType());
        bus.post(event);assertEquals("Typing still uses native search and tag intersection",1,stack[0]);
        when(c.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE)).thenReturn(0);
        c.searchView.clear();bus.post(event);assertEquals("Other bank views retain their own filtering",1,stack[0]);
    }
    private Widget decoration(int type,String text,boolean hidden) {
        Widget widget=mock(Widget.class);java.util.concurrent.atomic.AtomicBoolean state=new java.util.concurrent.atomic.AtomicBoolean(hidden);
        when(widget.getType()).thenReturn(type);when(widget.getText()).thenReturn(text);when(widget.getItemId()).thenReturn(-1);
        when(widget.getOriginalWidth()).thenReturn(400);when(widget.getOriginalHeight()).thenReturn(type==net.runelite.api.widgets.WidgetType.TEXT?16:1);
        when(widget.isSelfHidden()).thenAnswer(call -> state.get());
        doAnswer(call -> {state.set(call.getArgument(0));return widget;}).when(widget).setHidden(anyBoolean());
        return widget;
    }
    @Test public void nativeSearchHidesSpriteDividersWithoutClosingItsInput() throws Exception {
        BankLayoutController c=controller();when(c.config.setupSearchLayout()).thenReturn(true);
        when(c.config.improvedBankSearch()).thenReturn(true);
        c.nativeSearch=new NativeBankSearch();c.nativeSearch.client=c.client;c.nativeSearch.config=c.config;
        c.nativeSearch.bankTags=mock(net.runelite.client.plugins.banktags.BankTagsService.class);
        when(c.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(c.client.getWidget(InterfaceID.Bankmain.UNIVERSE)).thenReturn(mock(Widget.class));
        for(String field:new String[]{"query","slots","matches"}) {
            java.lang.reflect.Field f=NativeBankSearch.class.getDeclaredField(field);f.setAccessible(true);
            f.set(c.nativeSearch,field.equals("query")?"hydra":field.equals("slots")?true:Collections.singleton(1));
        }
        c.nativeSearch.start();
        when(c.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE))
            .thenReturn(net.runelite.api.vars.InputType.SEARCH.getType());
        when(c.client.getVarcStrValue(net.runelite.api.gameval.VarClientID.MESLAYERINPUT)).thenReturn("hydra");
        Widget items=mock(Widget.class),line=decoration(net.runelite.api.widgets.WidgetType.GRAPHIC,"",false);
        when(line.getSpriteId()).thenReturn(897);when(line.getOriginalHeight()).thenReturn(2);
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items);
        when(items.getDynamicChildren()).thenReturn(new Widget[]{line});
        c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));
        assertTrue(line.isSelfHidden());verify(c.client,never()).runScript(any(Object[].class));
    }
    private Widget cell(int x,int y)
    {
        Widget widget=mock(Widget.class);AtomicInteger px=new AtomicInteger(x),py=new AtomicInteger(y);
        when(widget.getType()).thenReturn(net.runelite.api.widgets.WidgetType.GRAPHIC);when(widget.getItemId()).thenReturn(100);
        when(widget.getOriginalX()).thenAnswer(i -> px.get());when(widget.getOriginalY()).thenAnswer(i -> py.get());
        doAnswer(i -> {px.set(i.getArgument(0));return widget;}).when(widget).setOriginalX(anyInt());
        doAnswer(i -> {py.set(i.getArgument(0));return widget;}).when(widget).setOriginalY(anyInt());
        when(widget.getOriginalWidth()).thenReturn(36);when(widget.getWidth()).thenReturn(36);
        when(widget.getOriginalHeight()).thenReturn(32);when(widget.getHeight()).thenReturn(32);return widget;
    }
    private BankLayoutController controller()
    {
        BankLayoutController c=new BankLayoutController();c.client=mock(Client.class);c.config=mock(WindowMarkersConfig.class);
        c.clientThread=mock(ClientThread.class);c.eventBus=mock(EventBus.class);
        when(c.config.bankLayoutEnabled()).thenReturn(true);c.startUp();return c;
    }
    @Test public void tabAlignmentDoesNotDriftAndRestoresOnClose()
    {
        BankLayoutController c=controller();Widget tabs=mock(Widget.class),a=cell(51,0),b=cell(91,0);
        when(c.client.getWidget(InterfaceID.Bankmain.TABS)).thenReturn(tabs);when(tabs.getWidth()).thenReturn(600);
        when(tabs.getDynamicChildren()).thenReturn(new Widget[]{a,b});when(c.config.tabAlignment()).thenReturn(WindowMarkersConfig.TabAlignment.RIGHT);
        c.onBeforeRender(new BeforeRender());int shifted=a.getOriginalX();assertTrue(shifted>51);
        c.onBeforeRender(new BeforeRender());assertEquals(shifted,a.getOriginalX());assertEquals(40,b.getOriginalX()-a.getOriginalX());
        // A native search rebuild lands after ClientTick, before the next frame.
        for(int i=0;i<20;i++) {
            c.onClientTick(new ClientTick()); a.setOriginalX(51); b.setOriginalX(91);
            c.onScriptPostFired(new ScriptPostFired(ScriptID.BANKMAIN_BUILD));
            assertEquals(shifted,a.getOriginalX());
            c.onBeforeRender(new BeforeRender()); assertEquals(shifted,a.getOriginalX());
        }
        c.onWidgetClosed(new WidgetClosed(InterfaceID.BANKMAIN,0,true));assertEquals(51,a.getOriginalX());assertEquals(91,b.getOriginalX());
    }
    @Test public void reflowPreservesIconSizeAndIncludesTopPaddingInScrollHeight()
    {
        BankLayoutController c=controller();Widget items=mock(Widget.class);Widget[] cells=new Widget[10];
        for(int i=0;i<10;i++) { cells[i]=cell(51+(i%8)*48,(i/8)*36); }
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items);when(items.getDynamicChildren()).thenReturn(cells);
        when(items.getWidth()).thenReturn(478);when(c.config.columns()).thenReturn(4);
        int[] stack=new int[9];stack[0]=72;when(c.client.getIntStack()).thenReturn(stack);when(c.client.getIntStackSize()).thenReturn(9);
        c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));
        assertEquals(4,cells[0].getOriginalY());assertEquals(76,cells[8].getOriginalY());assertEquals(112,stack[0]);
        for(Widget w:cells) { verify(w,never()).setOriginalWidth(anyInt());verify(w,never()).setOriginalHeight(anyInt()); }
    }
    @Test public void nativeOneSlotTabDropAreasDoNotCreateBlankRowsInWideBanks() {
        BankLayoutController c=controller();Widget items=mock(Widget.class);
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items);when(items.getWidth()).thenReturn(698);
        List<Widget> children=new ArrayList<>();List<Widget> first=new ArrayList<>(),targets=new ArrayList<>();
        int nativeY=0;
        for(int count:new int[]{23,15,1}) {
            for(int i=0;i<count;i++) {
                Widget item=cell(51+(i%8)*48,nativeY+(i/8)*36);if(i==0)first.add(item);children.add(item);
            }
            // Script 509 creates a transparent rectangle across the unused end
            // of each tab's last row. Seven items leave a 36x32 rectangle.
            int used=count%8,free=8-used;
            Widget target=cell(51+used*48,nativeY+((count-1)/8)*36+4);
            when(target.getType()).thenReturn(net.runelite.api.widgets.WidgetType.RECTANGLE);
            when(target.getItemId()).thenReturn(-1);when(target.getOpacity()).thenReturn(255);when(target.isFilled()).thenReturn(true);
            AtomicInteger width=new AtomicInteger(free*36+(free-1)*12);
            when(target.getOriginalWidth()).thenAnswer(call -> width.get());
            doAnswer(call -> {width.set(call.getArgument(0));return target;}).when(target).setOriginalWidth(anyInt());
            children.add(target);targets.add(target);nativeY+=((count+7)/8)*36+12;
        }
        when(items.getDynamicChildren()).thenReturn(children.toArray(new Widget[0]));
        int[] stack=new int[9];stack[0]=nativeY-12;
        when(c.client.getIntStack()).thenReturn(stack);when(c.client.getIntStackSize()).thenReturn(9);
        c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));
        assertEquals(4,first.get(0).getOriginalY());
        assertEquals("The gear tab's invisible drop area must not consume an extra row",88,first.get(1).getOriginalY());
        assertEquals("The potion tab's invisible drop area must not consume an extra row",172,first.get(2).getOriginalY());
        assertEquals(208,stack[0]);
        assertEquals(531,targets.get(0).getOriginalX());assertEquals(44,targets.get(0).getOriginalY());assertEquals(132,targets.get(0).getOriginalWidth());
        assertEquals(147,targets.get(1).getOriginalX());assertEquals(128,targets.get(1).getOriginalY());assertEquals(516,targets.get(1).getOriginalWidth());
        assertEquals(99,targets.get(2).getOriginalX());assertEquals(176,targets.get(2).getOriginalY());assertEquals(564,targets.get(2).getOriginalWidth());
        for(Widget target:targets) {verify(target,never()).setHidden(anyBoolean());verify(target,never()).clearActions();}
    }
    @Test public void rewrappedFullRowsDisableTheDropAreaWithoutAddingAnEmptyRow() {
        BankLayoutController c=controller();Widget items=mock(Widget.class);List<Widget> children=new ArrayList<>();
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items);when(items.getWidth()).thenReturn(698);
        for(int i=0;i<13;i++)children.add(cell(51+(i%8)*48,(i/8)*36));
        Widget target=cell(291,40);when(target.getType()).thenReturn(net.runelite.api.widgets.WidgetType.RECTANGLE);
        when(target.getItemId()).thenReturn(-1);when(target.getOpacity()).thenReturn(255);when(target.isFilled()).thenReturn(true);
        when(target.getOriginalWidth()).thenReturn(132);children.add(target);
        when(items.getDynamicChildren()).thenReturn(children.toArray(new Widget[0]));
        int[] stack=new int[9];stack[0]=72;when(c.client.getIntStack()).thenReturn(stack);when(c.client.getIntStackSize()).thenReturn(9);
        c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));
        assertEquals(40,stack[0]);verify(target).setOriginalWidth(0);verify(target,never()).setHidden(anyBoolean());
    }
    @Test public void intentionalTagLayoutGapsArePreservedWithNativeDropAreas() {
        BankLayoutController c=controller();c.bankTagsService=mock(net.runelite.client.plugins.banktags.BankTagsService.class);
        when(c.bankTagsService.getActiveLayout()).thenReturn(new net.runelite.client.plugins.banktags.tabs.Layout("Own layout"));
        Widget items=mock(Widget.class),a=cell(51,0),b=cell(387,360),target=cell(51,400);
        when(target.getType()).thenReturn(net.runelite.api.widgets.WidgetType.RECTANGLE);when(target.getItemId()).thenReturn(-1);
        when(target.getOpacity()).thenReturn(255);when(target.isFilled()).thenReturn(true);
        when(items.getDynamicChildren()).thenReturn(new Widget[]{a,b,target});when(items.getWidth()).thenReturn(698);
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items);
        int[] stack=new int[9];stack[0]=396;when(c.client.getIntStack()).thenReturn(stack);when(c.client.getIntStackSize()).thenReturn(9);
        c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING));
        assertEquals(4,a.getOriginalY());assertEquals(364,b.getOriginalY());assertEquals(387,b.getOriginalX());
        assertEquals(400,stack[0]);verify(target,never()).setOriginalWidth(anyInt());
    }
    @Test public void captureIncludesOffscreenItemsWithoutChangingGameWidgets()
    {
        BankCapture c=new BankCapture();c.client=mock(Client.class);c.items=mock(ItemManager.class);c.capture=mock(ImageCapture.class);c.config=mock(WindowMarkersConfig.class);c.borders=new BorderRenderer();
        Widget bank=mock(Widget.class),grid=mock(Widget.class),a=cell(50,0),b=cell(50,1000);
        when(c.client.getWidget(InterfaceID.Bankmain.UNIVERSE)).thenReturn(bank);when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(grid);
        when(grid.getDynamicChildren()).thenReturn(new Widget[]{a,b});when(grid.getWidth()).thenReturn(478);
        when(a.getItemId()).thenReturn(100);when(b.getItemId()).thenReturn(101);when(b.getRelativeY()).thenReturn(1000);
        when(c.items.getItemComposition(anyInt())).thenReturn(mock(ItemComposition.class));
        c.clientThread=mock(ClientThread.class);
        net.runelite.client.util.AsyncBufferedImage icon=new net.runelite.client.util.AsyncBufferedImage(c.clientThread,36,32,BufferedImage.TYPE_INT_ARGB);
        when(c.items.getImage(anyInt(),anyInt(),eq(true))).thenReturn(icon);
        assertTrue(c.save(Collections.emptyList(),() -> true,saved -> assertTrue(saved)));
        icon.loaded();ArgumentCaptor<java.util.function.BooleanSupplier> task=ArgumentCaptor.forClass(java.util.function.BooleanSupplier.class);
        verify(c.clientThread).invokeLater(task.capture());assertTrue(task.getValue().getAsBoolean());
        ArgumentCaptor<BufferedImage> image=ArgumentCaptor.forClass(BufferedImage.class);
        verify(c.capture).saveScreenshot(image.capture(),eq("bank"),eq("bank-markers"),eq(true),eq(false));assertTrue(image.getValue().getHeight()>1000);
        verify(grid,never()).setScrollY(anyInt());verify(b,never()).setOriginalY(anyInt());
    }
    @Test public void oversizedCaptureIsRejectedBeforeAllocatingImage()
    { assertFalse(BankCapture.reasonableSize(4096,30000));assertFalse(BankCapture.reasonableSize(0,200));assertTrue(BankCapture.reasonableSize(1000,6000)); }
    @Test public void slotSearchDoesNotOverrideDisabledOptionOrAnotherTagsLayout() throws Exception {
        BankLayoutController c=controller(); c.bankTagsService=mock(net.runelite.client.plugins.banktags.BankTagsService.class);
        c.nativeSearch=new NativeBankSearch(); c.nativeSearch.client=c.client; c.nativeSearch.config=c.config;c.nativeSearch.bankTags=c.bankTagsService;
        when(c.client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(c.client.getWidget(InterfaceID.Bankmain.UNIVERSE)).thenReturn(mock(Widget.class));
        when(c.client.getVarcIntValue(net.runelite.api.gameval.VarClientID.MESLAYERMODE)).thenReturn(11);
        when(c.client.getVarcStrValue(net.runelite.api.gameval.VarClientID.MESLAYERINPUT)).thenReturn("toa");
        when(c.config.improvedBankSearch()).thenReturn(true);
        for(String field:new String[]{"query","slots","matches"}) {
            java.lang.reflect.Field f=NativeBankSearch.class.getDeclaredField(field); f.setAccessible(true);
            f.set(c.nativeSearch,field.equals("query")?"toa":field.equals("slots")?true:Collections.singleton(1));
        }
        c.nativeSearch.start(); assertTrue(c.nativeSearch.groupBySlot());
        when(c.config.bankLayoutEnabled()).thenReturn(false); when(c.config.setupSearchLayout()).thenReturn(false);
        Widget items=mock(Widget.class),item=cell(51,0); when(items.getDynamicChildren()).thenReturn(new Widget[]{item});
        when(c.client.getWidget(InterfaceID.Bankmain.ITEMS)).thenReturn(items);
        c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING)); verify(item,never()).setOriginalY(anyInt());
        when(c.config.setupSearchLayout()).thenReturn(true);
        when(c.bankTagsService.getActiveLayout()).thenReturn(new net.runelite.client.plugins.banktags.tabs.Layout("Own layout"));
        c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING)); verify(item,never()).setOriginalY(anyInt());
        when(c.bankTagsService.getActiveLayout()).thenReturn(null);
        when(c.bankTagsService.getActiveBankTag()).thenReturn(id -> true);
        c.onScriptPreFired(new ScriptPreFired(ScriptID.BANKMAIN_FINISHBUILDING)); verify(item,never()).setOriginalY(anyInt());
    }
}

