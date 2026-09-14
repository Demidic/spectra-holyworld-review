package ru.spectra.client.module;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.Spectra;
import ru.spectra.client.Lang;
import ru.spectra.client.type.Mc;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.type.NotificationType;
import ru.spectra.client.ui.setting.NumberSetting;
import ru.spectra.client.event.PacketReceiveEvent;
import ru.spectra.client.event.PlayerTickEvent;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.util.ClientLocalization;
import ru.spectra.client.type.SettingUnit;
import ru.spectra.client.ui.setting.TextFieldSetting;
import ru.spectra.client.model.Translation;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

@Aliases(aliases = {"Clan Invest", "Funtime", "Auto Invest"})
public class ClanInvestModule extends Module {
    public final TextFieldSetting thresholdSetting;
    public final NumberSetting percentageSetting;
    public final NumberSetting intervalMinutesSetting;
    public boolean investedThisCycle;
    public boolean awaitingConfirmation;
    public int pendingAmount;
    public long confirmationDeadline;
    public long nextInvestmentAt;
    static final long confirmationTimeoutMs = 4000;

    public ClanInvestModule() {
        super(ModuleTab.MISC, "Clan Invest");
        this.thresholdSetting = new TextFieldSetting(Lang.CLAN_INVEST_CURRENCY_THRESHOLD, Lang.CLAN_INVEST_CURRENCY_THRESHOLD_DESC).setOnlyDigits(true).setPlaceholder(Lang.CLAN_INVEST_CURRENCY_THRESHOLD_PLACEHOLDER).setText(String.valueOf(1000000));
        this.percentageSetting = new NumberSetting(Lang.CLAN_INVEST_INVEST_PERCENTAGE).currentValue(30.0f).range(1.0f, 100.0f).step(1.0f).unit(SettingUnit.PERCENTS);
        this.intervalMinutesSetting = new NumberSetting(Translation.clearText("Investment interval (minutes)"))
                .currentValue(15.0f)
                .range(15.0f, 60.0f)
                .step(1.0f);
        this.investedThisCycle = false;
        this.awaitingConfirmation = false;
        this.pendingAmount = 0;
        this.confirmationDeadline = 0L;
        this.nextInvestmentAt = 0L;
        addSettings(this.thresholdSetting, this.percentageSetting, this.intervalMinutesSetting);
        register(PacketReceiveEvent.class, class051Var -> {
            Mc class815Var = Mc.INSTANCE;
            if (isState() && class815Var.isWorldLoaded()) {
                if ((class051Var.getPacket()) instanceof GameMessageS2CPacket packet ) {
                    String lowerCase = packet.content().getString().toLowerCase(Locale.ROOT);
                    boolean zContains = lowerCase.contains("/clan create - создать клан ($10000)");
                    boolean zContains2 = lowerCase.contains("вы не можете пополнить баланс клана");
                    zContains |= lowerCase.contains("/clan create - \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u043a\u043b\u0430\u043d ($10000)");
                    zContains2 |= lowerCase.contains("\u0432\u044b \u043d\u0435 \u043c\u043e\u0436\u0435\u0442\u0435 \u043f\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u044c \u0431\u0430\u043b\u0430\u043d\u0441 \u043a\u043b\u0430\u043d\u0430");
                    if (zContains) {
                        Spectra.INSTANCE.notificationRepository().post(NotificationType.ERROR, (Text) Text.literal("Вы не состоите в клане. Инвестиции невозможны."), 6L, TimeUnit.SECONDS);
                        disableAndReset();
                        return;
                    }
                    if (zContains2) {
                        Spectra.INSTANCE.notificationRepository().post(NotificationType.INFO, (Text) Text.literal("Включите модуль, когда вы сможете пополнить баланс клана."), 6L, TimeUnit.SECONDS);
                        disableAndReset();
                        return;
                    }
                    boolean zContains3 = lowerCase.contains("пополнил баланс казны");
                    zContains3 |= lowerCase.contains("\u043f\u043e\u043f\u043e\u043b\u043d\u0438\u043b \u0431\u0430\u043b\u0430\u043d\u0441 \u043a\u0430\u0437\u043d\u044b");
                    if (this.awaitingConfirmation && zContains3) {
                        Spectra.INSTANCE.notificationRepository().post(NotificationType.SUCCESS, (Text) Text.literal("Инвестировано %s$ в клан.".formatted(Integer.valueOf(this.pendingAmount))), 6L, TimeUnit.SECONDS);
                        this.awaitingConfirmation = false;
                        this.pendingAmount = 0;
                        this.confirmationDeadline = 0L;
                        this.investedThisCycle = true;
                    }
                }
            }
        });
        register(PlayerTickEvent.class, class130Var -> {
            Mc class815Var = Mc.INSTANCE;
            if (isState() && class815Var.isWorldLoaded() && class130Var.isPre()) {
                if (!ServerUtil.isConnectedToAllFuntimeServers()) {
                    Spectra.INSTANCE.notificationRepository().post(NotificationType.ERROR, (Text) Text.literal("Данный модуль работает только на Funtime!"), 3L, TimeUnit.SECONDS);
                    disableAndReset();
                    return;
                }
                if (this.awaitingConfirmation && System.currentTimeMillis() > this.confirmationDeadline) {
                    this.awaitingConfirmation = false;
                    this.pendingAmount = 0;
                    this.confirmationDeadline = 0L;
                    Spectra.INSTANCE.notificationRepository().post(NotificationType.INFO, (Text) Text.literal("Не получено подтверждение инвестиции (таймаут)."), 4L, TimeUnit.SECONDS);
                }
                Optional<Integer> currentBalance = getCurrentBalance();
                if (currentBalance.isEmpty()) {
                    return;
                }
                int iIntValue = currentBalance.get().intValue();
                try {
                    int i = Integer.parseInt(this.thresholdSetting.getText());
                    if (i <= 0) {
                        Spectra.INSTANCE.notificationRepository().post(NotificationType.ERROR, (Text) Text.literal("Порог должен быть больше нуля."), 3L, TimeUnit.SECONDS);
                        disableAndReset();
                        return;
                    }
                    if (iIntValue < i) {
                        this.investedThisCycle = false;
                        return;
                    }
                    if (this.awaitingConfirmation || System.currentTimeMillis() < this.nextInvestmentAt) {
                        return;
                    }
                    int iMax = (int) Math.max(1L, Math.min(2147483647L, (((long) iIntValue) * ((long) ((int) this.percentageSetting.currentValue()))) / 100));
                    long now = System.currentTimeMillis();
                    try {
                        if (class815Var.getNetworkHandler() == null
                                || !class815Var.getNetworkHandler().getConnection().isOpen()) {
                            this.nextInvestmentAt = now + 5000L;
                            return;
                        }
                        class815Var.getPlayer().networkHandler.sendChatCommand("clan invest " + iMax);
                        this.awaitingConfirmation = true;
                        this.pendingAmount = iMax;
                        this.confirmationDeadline = now + confirmationTimeoutMs;
                        this.nextInvestmentAt = now
                                + TimeUnit.MINUTES.toMillis((long) this.intervalMinutesSetting.currentValue());
                    } catch (RuntimeException networkFailure) {
                        this.awaitingConfirmation = false;
                        this.pendingAmount = 0;
                        this.confirmationDeadline = 0L;
                        this.nextInvestmentAt = now + 5000L;
                        Spectra.LOGGER.warn("Clan Invest command was not sent because the connection is unavailable", networkFailure);
                        Spectra.INSTANCE.notificationRepository().post(
                                NotificationType.ERROR,
                                Text.literal(ClientLocalization.text(
                                        "Clan Invest: connection interrupted, retrying in 5 seconds.",
                                        "Clan Invest: соединение прервано, повтор через 5 секунд.")),
                                4L,
                                TimeUnit.SECONDS
                        );
                    }
                } catch (NumberFormatException e) {
                    Spectra.INSTANCE.notificationRepository().post(NotificationType.ERROR, (Text) Text.literal("Неверный формат порога валюты!"), 3L, TimeUnit.SECONDS);
                    disableAndReset();
                }
            }
        });
    }

    private void disableAndReset() {
        this.awaitingConfirmation = false;
        this.pendingAmount = 0;
        this.confirmationDeadline = 0L;
        this.investedThisCycle = false;
        this.nextInvestmentAt = 0L;
        setState(false);
    }

    @Override
    public void deactivate() {
        this.investedThisCycle = false;
        this.awaitingConfirmation = false;
        this.pendingAmount = 0;
        this.confirmationDeadline = 0L;
        this.nextInvestmentAt = 0L;
        super.deactivate();
    }

    public Optional<Integer> getCurrentBalance() {
        Scoreboard scoreboard;
        ScoreboardObjective objectiveForSlot;
        ClientPlayerEntity player = Mc.INSTANCE.getPlayer();
        if (player != null) {
            Scoreboard localizedScoreboard = player.getScoreboard();
            ScoreboardObjective localizedObjective = localizedScoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (localizedObjective != null) {
                Optional<Integer> localizedBalance = localizedScoreboard.getScoreboardEntries(localizedObjective).stream()
                        .filter(entry -> !entry.hidden())
                        .map(entry -> Team.decorateName(localizedScoreboard.getScoreHolderTeam(entry.owner()), entry.name()).getString())
                        .map(text -> text.toLowerCase(Locale.ROOT)
                                .replace('o', '\u043e').replace('e', '\u0435')
                                .replace('a', '\u0430').replace('c', '\u0441'))
                        .filter(text -> text.contains("\u043c\u043e\u043d\u0435\u0442")
                                || text.contains("\u0431\u0430\u043b\u0430\u043d\u0441"))
                        .map(this::parseDigits)
                        .flatMap(Optional::stream)
                        .findFirst();
                if (localizedBalance.isPresent()) {
                    return localizedBalance;
                }
            }
        }
        if (player != null && (objectiveForSlot = (scoreboard = player.getScoreboard()).getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR)) != null) {
            return scoreboard.getScoreboardEntries(objectiveForSlot).stream().filter(scoreboardEntry -> {
                return !scoreboardEntry.hidden();
            }).map(scoreboardEntry2 -> {
                return (Integer) Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry2.owner()), scoreboardEntry2.name()).getSiblings().stream().map((v0) -> {
                    return v0.getString();
                }).filter(str -> {
                    return str.toLowerCase(Locale.ROOT).replace('o', (char) 1086).replace('e', (char) 1077).contains("монет") || str.toLowerCase(Locale.ROOT).replace('a', (char) 1072).replace('c', (char) 1089).contains("баланс");
                }).map(this::parseDigits).flatMap((v0) -> {
                    return v0.stream();
                }).findFirst().orElse(null);
            }).filter((v0) -> {
                return Objects.nonNull(v0);
            }).findFirst();
        }
        return Optional.empty();
    }

    public Optional<Integer> parseDigits(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        if (sb.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.valueOf(Integer.parseInt(sb.toString())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
