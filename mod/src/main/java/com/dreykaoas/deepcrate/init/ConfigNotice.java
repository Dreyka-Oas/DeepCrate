package com.dreykaoas.deepcrate.init;

import com.dreykaoas.deepcrate.config.io.ConfigIo;
import com.dreykaoas.deepcrate.config.io.diag.ConfigDrift;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/**
 * Telling an operator, on the way in, what the settings file needs from them.
 *
 * Only what they can act on: a value that is lost, or a key written twice. What the rewrite corrects
 * on its own stays in the log and out of the chat.
 */
public final class ConfigNotice {
    /** Past this the rest is counted rather than listed, so a broken file is not a wall of chat. */
    private static final int MAX_LINES = 6;

    private ConfigNotice() {}

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> notify(handler.player));
    }

    private static void notify(ServerPlayer serverPlayer) {
        ConfigDrift.Report report = ConfigIo.lastReport();
        if (report == null || report.clean() || !serverPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }

        List<Component> lines = lines(report);
        serverPlayer.sendSystemMessage(Component.translatable("deepcrate.notice.config_problems", report.problemCount()));
        for (Component line : lines.subList(0, Math.min(MAX_LINES, lines.size()))) {
            serverPlayer.sendSystemMessage(line);
        }

        if (lines.size() > MAX_LINES) {
            serverPlayer.sendSystemMessage(Component.translatable("deepcrate.notice.more_lines", lines.size() - MAX_LINES));
        }
    }

    private static List<Component> lines(ConfigDrift.Report report) {
        List<Component> lines = new ArrayList<>();
        for (ConfigDrift.Unknown unknown : report.unknown()) {
            lines.add(
                unknown.suggestion() == null
                    ? Component.translatable("deepcrate.notice.unknown_option", unknown.name())
                    : Component.translatable("deepcrate.notice.unknown_ambiguous", unknown.name(), unknown.suggestion())
            );
        }

        for (String name : report.duplicated()) {
            lines.add(Component.translatable("deepcrate.notice.duplicated", name));
        }

        return lines;
    }
}
