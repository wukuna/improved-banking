package com.windowmarkers;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ProfileChanged;

/** Bundled release notes, shown once per release in each RuneLite profile. */
@Singleton
final class ReleaseNotesNotice
{
    static final String RELEASE = "2026-10-28-consolidated-presets";
    static final String SEEN_KEY = "releaseNotesShown";
    private static final String[] RECENT = {
        "Wilderness defaults pair owned Wilderness weapons with budget gear; explicit guide choices remain available.",
        "Owned divine and blighted supplies fill reviewed inventory slots; supported potions can use other owned doses of the same potion.",
        "Boss presets share search setups and guide choices, with separate phase gear and inventories and suitable owned slot completion.",
        "Search layouts hide native dividers; presets exclude unrelated items and preserve repeated supplies and gear with Bank Tag Layouts.",
        "Custom labels use nearby space and borders follow items; activity browsing is consolidated without deleting saved groups."
    };

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configManager;
    @Inject private ChatMessageManager chat;
    @Inject private net.runelite.client.eventbus.EventBus eventBus;
    private volatile boolean running;
    private volatile int generation;

    public synchronized void start() { if (running) return; running = true; generation++; eventBus.register(this); schedule(); }
    public synchronized void stop() { if (!running) return; running = false; generation++; eventBus.unregister(this); }

    private void schedule()
    {
        final int scheduledGeneration = generation;
        clientThread.invokeLater(() -> {
            if (running && generation == scheduledGeneration) showIfNeeded();
        });
    }

    private void showIfNeeded()
    {
        if (client.getGameState() != GameState.LOGGED_IN
            || RELEASE.equals(configManager.getConfiguration("windowmarkers", SEEN_KEY)))
            return;
        send("Recent updates (newest first):");
        for (int i = 0; i < Math.min(5, RECENT.length); i++) send((i + 1) + ". " + RECENT[i]);
        configManager.setConfiguration("windowmarkers", SEEN_KEY, RELEASE);
    }

    private void send(String text)
    {
        String message = new ChatMessageBuilder().append(ChatColorType.NORMAL).append("[")
            .append(ChatColorType.HIGHLIGHT).append("Improved Banking")
            .append(ChatColorType.NORMAL).append("] ").append(text).build();
        chat.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE)
            .runeLiteFormattedMessage(message).build());
    }

    @Subscribe public void onGameStateChanged(GameStateChanged event)
    {
        if (running && event.getGameState() == GameState.LOGGED_IN) schedule();
    }

    @Subscribe public void onProfileChanged(ProfileChanged event)
    {
        // An old queued callback must not announce against a replacement profile.
        generation++;
        if (running) schedule();
    }
}
