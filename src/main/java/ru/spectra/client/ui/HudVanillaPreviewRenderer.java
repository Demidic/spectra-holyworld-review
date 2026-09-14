package ru.spectra.client.ui;

import ru.spectra.client.Spectra;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import ru.spectra.mixin.accessors.BossBarHudAccessor;
import ru.spectra.mixin.accessors.InGameHudAccessor;

import java.util.UUID;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Uses Minecraft's own HUD renderers for the advanced editor preview. This
 * deliberately avoids imitating vanilla textures with custom rectangles.
 */
public final class HudVanillaPreviewRenderer {
    public static final float BOSS_BAR_WIDTH = 182.0f;
    public static final float BOSS_BAR_HEIGHT = 14.0f;
    public static final float PLAYER_LIST_WIDTH = 354.0f;
    public static final float PLAYER_LIST_HEIGHT = 173.0f;
    public static final float PLAYER_LIST_TOP = 10.0f;
    private static final int PLAYER_LIST_COLUMNS = 3;
    private static final int PLAYER_LIST_ROWS = 17;
    private static final List<String> PLAYER_LIST_PREVIEW_NAMES = createPreviewPlayerNames();

    private static final String[] SCORE_NAMES = {
            "Player", "Online", "Kills", "Coins", "play.example.net"
    };
    private static final int[] SCORE_VALUES = {24, 128, 12, 5280, 1};

    private static BossBarHud previewBossBarHud;
    private static ScoreboardObjective previewObjective;
    private static boolean renderingPreview;

    private HudVanillaPreviewRenderer() {
    }

    public static boolean isRenderingPreview() {
        return renderingPreview;
    }

    public static void render(DrawContext context) {
        if (!HudEditorScreen.isAdvancedOpen()) {
            return;
        }
        HudEditorOverlayWidget bossBar = HudEditorOverlays.bossBar();
        HudEditorOverlayWidget scoreboard = HudEditorOverlays.scoreboard();
        if (bossBar != null) {
            renderBossBar(context, bossBar);
        }
        if (scoreboard != null) {
            renderScoreboard(context, scoreboard);
        }
        HudEditorOverlayWidget playerList = HudEditorOverlays.playerList();
        if (playerList != null && HudEditorScreen.isPlayerListPreviewVisible()) {
            renderPlayerList(context, playerList);
        } else {
            hideActualPlayerList(MinecraftClient.getInstance());
        }
    }

    private static void renderBossBar(DrawContext context, HudEditorOverlayWidget overlay) {
        MinecraftClient client = MinecraftClient.getInstance();
        BossBarHud bossBarHud = currentBossBarHud(client);
        if (bossBarHud == null) {
            ensureBossBar(client);
            bossBarHud = previewBossBarHud;
        }
        float hudToGui = hudToGui(client);
        float desiredX = overlay.contentX() * hudToGui;
        float desiredY = overlay.contentY() * hudToGui;
        float defaultX = context.getScaledWindowWidth() / 2.0f - 91.0f;
        float defaultY = 3.0f;

        context.getMatrices().push();
        context.getMatrices().translate(desiredX, desiredY, 0.0f);
        context.getMatrices().scale(overlay.scale(), overlay.scale(), 1.0f);
        context.getMatrices().translate(-defaultX, -defaultY, 0.0f);
        float[] shaderColor = RenderSystem.getShaderColor();
        float red = shaderColor[0];
        float green = shaderColor[1];
        float blue = shaderColor[2];
        float alpha = shaderColor[3];
        RenderSystem.setShaderColor(red, green, blue,
                alpha * previewOpacity(overlay));
        renderingPreview = true;
        try {
            bossBarHud.render(context);
            // DrawContext batches GUI vertices. Flush while the alpha
            // multiplier is still active so the preview really fades.
            context.draw();
        } finally {
            renderingPreview = false;
            RenderSystem.setShaderColor(red, green, blue, alpha);
            context.getMatrices().pop();
        }
    }

    private static void renderScoreboard(DrawContext context, HudEditorOverlayWidget overlay) {
        MinecraftClient client = MinecraftClient.getInstance();
        ScoreboardObjective objective = currentScoreboardObjective(client);
        if (objective == null) {
            ensureScoreboard();
            objective = previewObjective;
        }
        float hudToGui = hudToGui(client);
        float desiredRight = (overlay.contentX() + overlay.contentWidth()) * hudToGui;
        float desiredCenterY = (overlay.contentY() + overlay.contentHeight() / 2.0f) * hudToGui;
        float defaultRight = context.getScaledWindowWidth() - 1.0f;
        float defaultCenterY = scoreboardDefaultCenterY(
                context.getScaledWindowHeight(), objective);

        context.getMatrices().push();
        context.getMatrices().translate(desiredRight, desiredCenterY, 0.0f);
        context.getMatrices().scale(overlay.scale(), overlay.scale(), 1.0f);
        context.getMatrices().translate(-defaultRight, -defaultCenterY, 0.0f);
        float[] shaderColor = RenderSystem.getShaderColor();
        float red = shaderColor[0];
        float green = shaderColor[1];
        float blue = shaderColor[2];
        float alpha = shaderColor[3];
        RenderSystem.setShaderColor(red, green, blue,
                alpha * previewOpacity(overlay));
        try {
            ((InGameHudAccessor) (Object) client.inGameHud)
                    .spectra$renderScoreboardSidebar(context, objective);
            context.draw();
        } finally {
            RenderSystem.setShaderColor(red, green, blue, alpha);
            context.getMatrices().pop();
        }
    }

    private static void renderPlayerList(DrawContext context, HudEditorOverlayWidget overlay) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.getNetworkHandler() != null) {
            Scoreboard scoreboard = client.world.getScoreboard();
            ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);
            var playerListHud = client.inGameHud.getPlayerListHud();
            playerListHud.setVisible(true);
            playerListHud.render(
                    context, context.getScaledWindowWidth(), scoreboard, objective
            );
            return;
        }
        float hudToGui = hudToGui(client);
        float desiredX = overlay.contentX() * hudToGui;
        float desiredY = overlay.contentY() * hudToGui;
        int baseWidth = Math.round(PLAYER_LIST_WIDTH);
        int columnWidth = baseWidth / PLAYER_LIST_COLUMNS;
        int rowHeight = 9;

        context.getMatrices().push();
        context.getMatrices().translate(desiredX, desiredY, 0.0f);
        context.getMatrices().scale(overlay.scale(), overlay.scale(), 1.0f);
        if (overlay.showBackground()) {
            context.fill(0, 0, baseWidth, Math.round(PLAYER_LIST_HEIGHT), 0xB0000000);
        }
        String header = "Online: " + PLAYER_LIST_PREVIEW_NAMES.size();
        int headerX = (baseWidth - client.textRenderer.getWidth(header)) / 2;
        context.drawTextWithShadow(client.textRenderer, header, headerX, 3, 0xFFFFFFFF);
        for (int index = 0; index < PLAYER_LIST_PREVIEW_NAMES.size(); index++) {
            int column = index / PLAYER_LIST_ROWS;
            int row = index % PLAYER_LIST_ROWS;
            int x = column * columnWidth + 5;
            int y = 17 + row * rowHeight;
            if (overlay.showBackground()) {
                context.fill(
                        column * columnWidth + 2, y,
                        (column + 1) * columnWidth - 2, y + 8,
                        (row & 1) == 0 ? 0x28000000 : 0x18000000
                );
            }
            int color = switch (index % 7) {
                case 0 -> 0xFFFFD45A;
                case 1 -> 0xFF8FE388;
                default -> 0xFFE7E7EA;
            };
            context.drawTextWithShadow(
                    client.textRenderer, PLAYER_LIST_PREVIEW_NAMES.get(index), x, y, color
            );
        }
        context.getMatrices().pop();
    }

    private static void hideActualPlayerList(MinecraftClient client) {
        if (client.world != null && client.inGameHud != null) {
            client.inGameHud.getPlayerListHud().setVisible(false);
        }
    }

    private static List<String> createPreviewPlayerNames() {
        String[] first = {
                "Shadow", "Nova", "Pixel", "Frost", "Lunar", "Crimson", "Silent",
                "Rapid", "Neon", "Void", "Sky", "Iron", "Aqua", "Wild", "Mystic"
        };
        String[] second = {
                "Fox", "Wolf", "Byte", "Storm", "Knight", "Craft", "Ghost", "Spark",
                "Raven", "Blade", "Dream", "Panda", "Flame", "Orbit", "Core"
        };
        Random random = new Random(0x5EEDBEEFL);
        ArrayList<String> names = new ArrayList<>(50);
        while (names.size() < 50) {
            String name = first[random.nextInt(first.length)]
                    + second[random.nextInt(second.length)]
                    + (10 + random.nextInt(90));
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return List.copyOf(names);
    }

    private static void ensureBossBar(MinecraftClient client) {
        if (previewBossBarHud != null) {
            return;
        }
        previewBossBarHud = new BossBarHud(client);
        ClientBossBar bar = new ClientBossBar(
                UUID.fromString("7d541a6e-cdc5-4d89-a1b2-8cb362355749"),
                Text.literal("Spectra Preview"),
                0.72f,
                BossBar.Color.PURPLE,
                BossBar.Style.PROGRESS,
                false,
                false,
                false
        );
        ((BossBarHudAccessor) (Object) previewBossBarHud)
                .spectra$getBossBars()
                .put(bar.getUuid(), bar);
    }

    private static void ensureScoreboard() {
        if (previewObjective != null) {
            return;
        }
        Scoreboard scoreboard = new Scoreboard();
        previewObjective = scoreboard.addObjective(
                "spectra_preview",
                ScoreboardCriterion.DUMMY,
                Text.literal("SPECTRA"),
                ScoreboardCriterion.RenderType.INTEGER,
                false,
                null
        );
        for (int index = 0; index < SCORE_NAMES.length; index++) {
            ScoreAccess score = scoreboard.getOrCreateScore(
                    ScoreHolder.fromName(SCORE_NAMES[index]),
                    previewObjective
            );
            score.setScore(SCORE_VALUES[index]);
            score.setDisplayText(Text.literal(SCORE_NAMES[index]));
        }
    }

    public static float scoreboardWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        ScoreboardObjective current = currentScoreboardObjective(client);
        if (current != null) {
            Scoreboard scoreboard = current.getScoreboard();
            NumberFormat numberFormat =
                    current.getNumberFormatOr(StyledNumberFormat.RED);
            int separatorWidth = client.textRenderer.getWidth(":");
            int widest = client.textRenderer.getWidth(current.getDisplayName());
            for (ScoreboardEntry entry : visibleEntries(current)) {
                Text name = Team.decorateName(
                        scoreboard.getScoreHolderTeam(entry.owner()),
                        entry.name()
                );
                int scoreWidth = client.textRenderer.getWidth(
                        entry.formatted(numberFormat));
                int row = client.textRenderer.getWidth(name)
                        + (scoreWidth > 0 ? separatorWidth + scoreWidth : 0);
                widest = Math.max(widest, row);
            }
            return widest + 4.0f;
        }
        int widest = client.textRenderer.getWidth("SPECTRA");
        for (int index = 0; index < SCORE_NAMES.length; index++) {
            int row = client.textRenderer.getWidth(SCORE_NAMES[index])
                    + 2
                    + client.textRenderer.getWidth(String.valueOf(SCORE_VALUES[index]));
            widest = Math.max(widest, row);
        }
        return widest + 4.0f;
    }

    public static float scoreboardHeight() {
        ScoreboardObjective current =
                currentScoreboardObjective(MinecraftClient.getInstance());
        if (current != null) {
            return visibleEntries(current).length * 9.0f + 10.0f;
        }
        return SCORE_NAMES.length * 9.0f + 10.0f;
    }

    public static float scoreboardDefaultCenterY(
            float scaledWindowHeight, ScoreboardObjective objective) {
        int rows = objective == null
                ? SCORE_NAMES.length
                : visibleEntries(objective).length;
        return scaledWindowHeight / 2.0f - rows * 1.5f - 5.0f;
    }

    private static ScoreboardEntry[] visibleEntries(
            ScoreboardObjective objective) {
        return objective.getScoreboard().getScoreboardEntries(objective)
                .stream()
                .filter(entry -> !entry.hidden())
                .sorted(Comparator.comparingInt(ScoreboardEntry::value)
                        .reversed()
                        .thenComparing(
                                ScoreboardEntry::owner,
                                String.CASE_INSENSITIVE_ORDER))
                .limit(15L)
                .toArray(ScoreboardEntry[]::new);
    }

    public static float bossBarHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        BossBarHud current = currentBossBarHud(client);
        if (current == null) {
            return BOSS_BAR_HEIGHT;
        }
        int count = ((BossBarHudAccessor) (Object) current)
                .spectra$getBossBars().size();
        return BOSS_BAR_HEIGHT + Math.max(0, count - 1) * 19.0f;
    }

    private static BossBarHud currentBossBarHud(MinecraftClient client) {
        if (client.world == null || client.inGameHud == null) {
            return null;
        }
        BossBarHud hud = client.inGameHud.getBossBarHud();
        return ((BossBarHudAccessor) (Object) hud)
                .spectra$getBossBars().isEmpty() ? null : hud;
    }

    private static ScoreboardObjective currentScoreboardObjective(
            MinecraftClient client) {
        if (client.world == null) {
            return null;
        }
        Scoreboard scoreboard = client.world.getScoreboard();
        if (client.player != null) {
            Team team = scoreboard.getScoreHolderTeam(
                    client.player.getNameForScoreboard());
            if (team != null) {
                ScoreboardDisplaySlot teamSlot =
                        ScoreboardDisplaySlot.fromFormatting(team.getColor());
                if (teamSlot != null) {
                    ScoreboardObjective teamObjective =
                            scoreboard.getObjectiveForSlot(teamSlot);
                    if (teamObjective != null) {
                        return teamObjective;
                    }
                }
            }
        }
        return scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
    }

    private static float hudToGui(MinecraftClient client) {
        return (float) (WidgetStack.HUD_SCALE
                / Math.max(1.0d, client.getWindow().getScaleFactor()));
    }

    private static float previewOpacity(HudEditorOverlayWidget overlay) {
        return Spectra.INSTANCE == null ? 1.0f
                : Spectra.INSTANCE.widgetStack().playerListOcclusionOpacity(overlay);
    }
}
