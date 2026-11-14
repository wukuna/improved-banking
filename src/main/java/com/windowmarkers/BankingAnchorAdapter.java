package com.windowmarkers;

import com.windowmarkers.anchors.AnchorParticipant;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;

@Singleton
final class BankingAnchorAdapter implements AnchorParticipant.Target,AnchorParticipant.Provider
{
    @Inject private javax.inject.Provider<BankLayoutController> owner;
    @Inject private ClientThread thread;
    @Inject private EventBus events;
    private AnchorParticipant participant;
    private boolean dirty;
    private Point last;
    void start() {participant=new AnchorParticipant("Improved Banking",events,thread,this);participant.start();}
    void stop() {if(participant!=null)participant.stop();participant=null;}
    public java.util.Collection<AnchorParticipant.Target> targets() {return List.of(this);}
    public String id() {return "banking.window";}
    public String name() {return "Bank window";}
    public Rectangle bounds() {return owner.get().windowDrag.anchorBounds();}
    public Rectangle movementBounds() {return owner.get().windowDrag.anchorLimits();}
    public String state() {return available()?"VISIBLE":"UNAVAILABLE";}
    public boolean available() {return owner.get().windowDrag.anchorAvailable();}
    public boolean claim() {last=null;return available();}
    public void move(Point point) {if(!point.equals(last)){owner.get().windowDrag.anchorMove(point);last=new Point(point);dirty=true;}}
    public void release(boolean keep) {owner.get().windowDrag.anchorRelease(keep);last=null;dirty=true;}
    public void commit() {if(dirty){dirty=false;owner.get().applyAnchorFrame();}}
}
