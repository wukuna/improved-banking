package com.windowmarkers;

import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BankTabTimingTest
{
    public static final class Names
    {
        final BankControlsTest.Cell tab;
        int x;
        Names(BankControlsTest.Cell tab){this.tab=tab;}
        @Subscribe public void onScriptPostFired(ScriptPostFired event){x=tab.widget.getOriginalX();}
    }

    @Test public void thirdPartyNamesReadFinalTabPositionsDuringEveryBankRebuild()
    {
        Client client=mock(Client.class);EventBus bus=new EventBus();
        BankLayoutController layout=new BankLayoutController();layout.client=client;layout.eventBus=bus;
        layout.clientThread=mock(ClientThread.class);layout.config=mock(WindowMarkersConfig.class);
        when(layout.config.bankLayoutEnabled()).thenReturn(true);
        when(layout.config.tabAlignment()).thenReturn(WindowMarkersConfig.TabAlignment.LEFT);
        BankControlsTest.Cell tabs=new BankControlsTest.Cell(InterfaceID.Bankmain.TABS,null,20,40,720,40,0,0,0,0);
        BankControlsTest.Cell tab=new BankControlsTest.Cell(1,tabs,150,0,40,40,0,0,0,0);
        tabs.children.add(tab.widget);when(client.getWidget(tabs.id)).thenReturn(tabs.widget);
        Names names=new Names(tab);bus.register(names);layout.startUp();
        for(int script:new int[]{ScriptID.BANKMAIN_BUILD,ScriptID.BANKMAIN_INIT,ScriptID.BANKMAIN_FINISHBUILDING,
            ScriptID.BANKMAIN_SEARCH_REFRESH,ScriptID.BANKMAIN_SEARCH_TOGGLE,ScriptID.BANKMAIN_SIZE_CHECK}) {
            tab.widget.setOriginalX(150);bus.post(new ScriptPostFired(script));
            assertTrue(names.x<150);assertEquals(tab.widget.getOriginalX(),names.x);
        }
        verify(client,never()).runScript(anyInt(),any());
    }
}
