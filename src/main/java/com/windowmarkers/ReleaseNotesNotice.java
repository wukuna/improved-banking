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
    static final String RELEASE = "2026-10-29-bank-controls";
    static final String SEEN_KEY = "releaseNotesShown";
    private static final String[] RECENT = {
        "Alt-drag bank buttons to reorder them, slide them along the bottom row, or dock them beside Bank Tags.",
        "Flexible banks no longer add blank rows for invisible tab drop areas; View presets and bank settings use clearer controls.",
        "View presets now lists 102 activities plus saved views, with clearer names and related methods grouped together.",
        "Wilderness budget gear, owned potion variants and charged supplies fill reviewed setups; suggested runes appear below the first gear setup.",
        "Presets share search setups, preserve phase gear and repeated items with Bank Tag Layouts, and stay selected when reopening the bank."
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
