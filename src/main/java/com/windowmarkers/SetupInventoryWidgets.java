package com.windowmarkers;

import java.util.*;
import net.runelite.api.*;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.*;
import net.runelite.client.game.ItemManager;

/** Repeated bank displays reference the existing stack and its existing actions. */
final class SetupInventoryWidgets
{
    private static final class Copy
    {
        final Widget widget,source,parent;
        final int itemId;
        Copy(Widget widget,Widget source,Widget parent,int itemId)
        { this.widget=widget;this.source=source;this.parent=parent;this.itemId=itemId; }
    }
    private final Map<Widget,Copy> copies=new IdentityHashMap<>();
    private final Map<MenuEntry,Copy> menus=new WeakHashMap<>();
    private Client client;
    void setClient(Client client) { this.client=client; }

    Widget add(Widget parent,Widget source,int exampleId,ItemManager manager)
    {
        int id=source==null?exampleId:source.getItemId();
        Widget widget=parent.createChild(-1,WidgetType.GRAPHIC);
        widget.setOriginalWidth(36);widget.setOriginalHeight(32);
        widget.setItemId(id);widget.setBorderType(1);
        if(source!=null)
        {
            widget.setItemQuantity(source.getItemQuantity());
            widget.setItemQuantityMode(source.getItemQuantityMode());
            widget.setOpacity(source.getOpacity());widget.setName(source.getName());
            String[] actions=source.getActions();
            if(actions!=null) for(int i=0;i<actions.length;i++) if(actions[i]!=null) widget.setAction(i,actions[i]);
            WidgetConfigNode config=client==null?null:client.getWidgetConfig(source);
            int mask=config==null?source.getClickMask():config.getClickMask();
            widget.setClickMask(mask & ~(WidgetConfig.DRAG|WidgetConfig.DRAG_ON|WidgetConfig.WIDGET_USE_TARGET));
            widget.setOnOpListener(source.getOnOpListener());
            widget.setHasListener(source.getOnOpListener()!=null);
        }
        else
        {
            var definition=manager.getItemComposition(id);
            widget.setName(definition==null?"Guide item":definition.getName());
            widget.setItemQuantity(1);widget.setItemQuantityMode(ItemQuantityMode.NEVER);
            widget.setOpacity(150);widget.setClickMask(0);
        }
        copies.put(widget,new Copy(widget,source,parent,id));
        return widget;
    }

    boolean preview(Widget widget) { Copy copy=copies.get(widget);return copy!=null && copy.source==null; }

    void allowPresetDragging()
    {
        for(Copy copy:copies.values()) if(copy.source!=null) {
            copy.widget.setClickMask(copy.widget.getClickMask() | WidgetConfig.DRAG);
            copy.widget.setDragParent(copy.source.getDragParent());
            copy.widget.setDragDeadZone(copy.source.getDragDeadZone());
            copy.widget.setDragDeadTime(copy.source.getDragDeadTime());
            copy.widget.setHasListener(true);
        }
    }

    void menuAdded(MenuEntry menu)
    {
        menus.remove(menu);
        Copy copy=copies.get(menu.getWidget());
        if(copy==null || copy.source==null || menu.getParam1()!=InterfaceID.Bankmain.ITEMS
            || menu.getType()!=MenuAction.CC_OP && menu.getType()!=MenuAction.CC_OP_LOW_PRIORITY) return;
        // Rebind the already-existing action, before other plugins inspect it.
        // No action is invoked, and withdrawal quantity remains the user's choice.
        menu.setParam0(copy.source.getIndex());
        menus.put(menu,copy);
    }

    void clicked(MenuOptionClicked event,Client client)
    {
        Copy copy=menus.get(event.getMenuEntry());
        if(copy==null) return;
        ItemContainer bank=client.getItemContainer(InventoryID.BANK);
        int index=copy.source.getIndex();
        Item item=bank==null?null:bank.getItem(index);
        if(client.getWidget(InterfaceID.Bankmain.ITEMS)!=copy.parent || copy.parent.getChild(index)!=copy.source
            || copy.source.isSelfHidden() || copy.source.getItemId()!=copy.itemId
            || item==null || item.getId()!=copy.itemId || item.getQuantity()<=0)
            event.consume();
    }

    void clear()
    {
        Set<Widget> parents=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Copy copy:copies.values()) parents.add(copy.parent);
        for(Widget parent:parents)
        {
            Widget[] children=parent.getChildren();if(children==null) continue;
            Widget[] remaining=children.clone();int length=remaining.length;
            // Remove only our exact widgets, preserving children another plugin replaced or appended.
            while(length>0 && copies.containsKey(remaining[length-1])) length--;
            for(int i=0;i<remaining.length;i++) if(copies.containsKey(remaining[i])) { remaining[i].setHidden(true);remaining[i]=null; }
            parent.setChildren(Arrays.copyOf(remaining,length));
        }
        copies.clear();
    }
}
