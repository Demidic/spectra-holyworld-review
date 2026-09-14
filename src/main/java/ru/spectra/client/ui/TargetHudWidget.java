package ru.spectra.client.ui;
import ru.spectra.client.render.AnimatedFloat;
import ru.spectra.client.ui.setting.BooleanSetting;
import ru.spectra.client.ui.setting.ModeSetting;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.model.DragRenderContext;
import ru.spectra.client.model.GlStateSnapshot;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.math.Easings;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.render.SvgTexture;
import ru.spectra.client.type.ItemSpriteManager;
import ru.spectra.client.util.IteratorUtil;
import ru.spectra.client.util.FriendManager;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;
import ru.spectra.client.util.MathUtil;
import ru.spectra.client.util.PlayerVisualFilter;
import ru.spectra.client.type.Mc;
import ru.spectra.client.model.MouseButtonInput2;
import ru.spectra.client.model.MouseMoveInput;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.module.NameProtectModule;
import ru.spectra.client.util.ScoreboardHelper;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.util.StencilBufferUtil;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.model.WidgetBounds;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.nio.charset.StandardCharsets;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class TargetHudWidget extends Draggable {
    static final String PLAYER_TYPE_GLYPH = "\u0438";
    private static boolean renderingModelPortrait;
    private static float modelPortraitOpacity = 1.0f;
    public float width;
    public float height;
    public final AnimatedFloat fadeAnimation;
    public final ToggleAnimator toggleAnimatorA;
    public final ToggleAnimator toggleAnimatorB;
    public final AnimatedFloat healthAnimation;
    public final AnimatedFloat absorptionAnimation;
    public final AnimatedFloat healthDigitAnimation;
    public final GlTexture backgroundTexture;
    public final GlTexture heartTexture;
    public final GlTexture gappleTexture;
    private final SvgTexture mobTypeIcon;
    private final SvgTexture animalTypeIcon;
    private final SvgTexture entityTypeIcon;
    private final SvgTexture friendTypeIcon;
    public final MsdfFont nameFont;
    public final MsdfFont valueFont;
    private final MsdfFont menuIconFont;
    public LivingEntity target;
    private int previousHealthNumber;
    private int currentHealthNumber;
    private boolean healthNumberInitialized;
    private GameProfile previewProfile;

    public final WidgetBounds avatarBounds;
    public final WidgetBounds contentBounds;
    public final WidgetBounds itemsBounds;
    public final WidgetBounds nameBounds;
    static final float padding = 10.0f;
    static final float cornerRadius = 8.0f;
    static final float itemSize = 12.0f;
    static final float itemSpacing = 3.0f;
    static final int iconSize = 10;
    static final float halfDivisor = 2.0f;
    static final float sectionGap = 4.0f;
    static final float minWidth = 180.0f;
    public final ModeSetting<PortraitMode> portraitMode;
    public final BooleanSetting includeOtherEntities;
    static final int spacingTwo = 2;

    static final double roundIncrement = 0.5d;

    static final double raycastExpand = 0.7d;

    public TargetHudWidget(BooleanSupplier booleanSupplier) {
        super("TargetHud", booleanSupplier);
        this.width = 204.0f;
        this.height = 64.0f;
        this.fadeAnimation = new AnimatedFloat(200, Easings.LINEAR);
        this.toggleAnimatorA = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
        this.toggleAnimatorB = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
        this.healthAnimation = new AnimatedFloat(160, Easings.LINEAR);
        this.absorptionAnimation = new AnimatedFloat(180, Easings.LINEAR);
        this.healthDigitAnimation = new AnimatedFloat(220, Easings.EASE_IN_OUT_CUBIC);
        this.healthDigitAnimation.set(1.0f);
        this.backgroundTexture = new GlTexture(new ClasspathResource("/textures/hud_background.png"));
        this.heartTexture = new GlTexture(new ClasspathResource("/icons/menu/new/heartpulse.png"));
        this.gappleTexture = new GlTexture(new ClasspathResource("/icons/menu/new/gapple.png"));
        this.mobTypeIcon = svg("/icons/hud/vector/swords.svg");
        this.animalTypeIcon = svg("/icons/hud/vector/leaf.svg");
        this.entityTypeIcon = svg("/icons/hud/vector/cube.svg");
        this.friendTypeIcon = svg("/icons/hud/vector/star.svg");
        this.nameFont = Fonts.INTER_SEMIBOLD.get();
        this.valueFont = Fonts.INTER_BOLD.get();
        this.menuIconFont = Fonts.MENU_ICON.get();
        this.avatarBounds = new WidgetBounds(0.0f, 0.0f, 44.0f, 44.0f);
        this.contentBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 44.0f);
        this.itemsBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.nameBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.portraitMode = new ModeSetting<PortraitMode>(
                Translation.clearText("Left portrait"),
                Translation.clearText("Chooses a head or an upper-body model")
        ).values(PortraitMode.class).currentValue(PortraitMode.HEAD);
        this.includeOtherEntities = new BooleanSetting(
                Translation.clearText("Other entities"),
                Translation.clearText("Shows Target HUD for mobs and animals as well as players")
        ).setValue(true);
        addSettings(this.portraitMode, this.includeOtherEntities);
        this.x = 1258.5f;
        this.y = 1050.0f;
    }

    @Override
    public void layout(DragRenderContext class809Var) {
        if (isVisible()) {
            this.width = 204.0f;
            this.height = 64.0f;
            this.avatarBounds.withSize(44.0f, 44.0f).withPosition(this.x + 8.0f, this.y + 10.0f);
            this.contentBounds.withPosition(this.x + 60.0f, this.y + 9.0f).withSize(91.0f, 46.0f);
        }
    }

    @Override
    public void render(DragRenderContext class809Var) {
        boolean menuPreview = isMainMenuPreview();
        if (menuPreview) {
            this.fadeAnimation.set(1.0f);
        }
        if (!isVisible()
                || (!menuPreview && (this.fadeAnimation.isZero()
                || this.target == null
                || !Mc.INSTANCE.isWorldLoaded()
                || Mc.INSTANCE.getNetworkHandler() == null))) {
            return;
        }
        float opacity = this.fadeAnimation.animatedValue();
        DrawEngine draw = class809Var.drawEngine();
        MatrixStack matrices = class809Var.matrixStack();
        ColorStack colors = draw.colorStack();
        int clientAccent = class809Var.theme().palette().accent().argb();
        colors.push();
        colors.alpha(opacity);
        SpectraHudStyle.panel(draw, matrices, this.x, this.y, this.width, this.height, 8.0f);
        // Vanilla's live entity renderer owns framebuffer/depth state. Nesting
        // it inside the visibility capture target is driver-sensitive and can
        // terminate the JVM natively. During the short blur transition use the
        // target's texture portrait; the live model returns at full visibility.
        boolean visibilityCapture = Spectra.INSTANCE.widgetStack()
                .isVisibilityCaptureActive();
        boolean renderModel = this.portraitMode.isSelected(PortraitMode.MODEL)
                && (!visibilityCapture || menuPreview);
        boolean genericEntityPortrait =
                !menuPreview && !(this.target instanceof PlayerEntity);
        if (genericEntityPortrait) {
            renderEntityTypePortrait(
                    draw, matrices.peek().getPositionMatrix(), colors,
                    this.avatarBounds.x(), this.avatarBounds.y(), 44.0f,
                    clientAccent
            );
        } else if (renderModel) {
            if (menuPreview) {
                renderPreviewBust(draw, matrices.peek().getPositionMatrix(), colors,
                        this.avatarBounds.x(), this.avatarBounds.y(), 44.0f);
            } else {
                draw.draw();
                GlStateSnapshot state = GlStateSnapshot.create();
                try {
                    renderEntityBust(this.target, this.avatarBounds.x(), this.avatarBounds.y(),
                            44.0f, opacity);
                } finally {
                    state.revert();
                }
            }
        } else {
            renderAvatar(draw, matrices.peek().getPositionMatrix(), colors,
                    this.avatarBounds.x(), this.avatarBounds.y(), 44.0f, 44.0f, 8.0f);
        }

        float health = menuPreview ? 20.0f : getDisplayHealth(this.target);
        float maxHealth = menuPreview ? 20.0f : Math.max(1.0f, getEffectiveMaxHealth(this.target));
        float healthFraction = MathUtil.clamp(health / maxHealth, 0.0f, 1.0f);
        this.healthAnimation.destination(healthFraction);

        TargetTypeDisplay targetType = menuPreview
                ? new TargetTypeDisplay("Player", null, PLAYER_TYPE_GLYPH)
                : targetType(this.target);
        float typeTextY = this.contentBounds.y() + 2.0f;
        if (targetType.menuGlyph() != null) {
            draw.msdfFontVerticalCHorizontalC(
                    matrices.peek().getPositionMatrix(),
                    this.menuIconFont,
                    targetType.menuGlyph(),
                    this.contentBounds.x() + 4.5f,
                    typeTextY + this.valueFont.getHeight(9.0f) / 2.0f,
                    9.0f,
                    0.05f,
                    colors.computeColor(clientAccent)
            );
        } else {
            draw.textureVerticalC(
                    matrices.peek().getPositionMatrix(),
                    targetType.icon(),
                    this.contentBounds.x(),
                    this.contentBounds.y() + 6.5f,
                    9,
                    9,
                    colors.computeColor(clientAccent)
            );
        }
        draw.msdfFont(
                matrices.peek().getPositionMatrix(),
                this.valueFont,
                targetType.label().toUpperCase(),
                this.contentBounds.x() + 13.0f,
                typeTextY,
                9.0f,
                0.05f,
                colors.computeColor(clientAccent)
        );
        String previewName = Mc.INSTANCE.getSession().getUsername();
        String name = truncateToWidth(
                this.nameFont,
                menuPreview ? previewName : getDisplayName(this.target),
                13,
                90.0f
        );
        draw.msdfFont(matrices.peek().getPositionMatrix(), this.nameFont, name, this.contentBounds.x(), this.contentBounds.y() + 15.0f, 13.0f, 0.05f, colors.computeColor(SpectraHudStyle.TEXT));

        float itemX = this.contentBounds.x();
        float itemY = this.contentBounds.y() + 34.0f;
        int itemCount = 0;
        for (ItemStack stack : menuPreview ? Collections.<ItemStack>emptyList() : getEquipment(this.target)) {
            if (!stack.isEmpty() && itemCount < 6) {
                draw.itemStack(matrices.peek().getPositionMatrix(), stack, itemX, itemY, 0.34375f, opacity);
                itemX += 12.5f;
                itemCount++;
            }
        }

        float circleX = this.x + this.width - 27.0f;
        float circleY = this.y + this.height / 2.0f;
        draw.hollowCircle(matrices.peek().getPositionMatrix(), circleX, circleY, 18.0f, 2.5f, colors.computeColor(0xFF2A2A2C));
        float animatedHealth = MathUtil.clamp(this.healthAnimation.animatedValue(), 0.0f, 1.0f);
        if (animatedHealth >= 0.999f) {
            draw.hollowCircle(matrices.peek().getPositionMatrix(), circleX, circleY, 18.0f, 2.5f, colors.computeColor(clientAccent));
        } else if (animatedHealth > 0.0f) {
            float sweep = 360.0f * animatedHealth;
            float firstSweep = Math.min(90.0f, sweep);
            if (firstSweep > 0.01f) {
                draw.arc(matrices.peek().getPositionMatrix(), circleX, circleY, 18.0f,
                        270.0f, 270.0f + firstSweep, 2.5f, colors.computeColor(clientAccent));
            }
            float remaining = sweep - firstSweep;
            if (remaining > 0.01f) {
                draw.arc(matrices.peek().getPositionMatrix(), circleX, circleY, 18.0f,
                        0.0f, remaining, 2.5f, colors.computeColor(clientAccent));
            }
        }
        updateHealthNumber(Math.max(0, Math.round(health)));
        renderRollingHealth(draw, matrices, colors, circleX, circleY);
        colors.pop();
    }

    @Override
    public boolean click(MouseButtonInput2 class807Var, boolean z) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput class808Var, boolean z) {
        return z && !class808Var.intercepted();
    }

    @Override
    public void animate(WeightedEngine class141Var) {
        this.healthAnimation.animate(class141Var);
        this.absorptionAnimation.animate(class141Var);
        this.healthDigitAnimation.animate(class141Var);
        this.fadeAnimation.animate(class141Var);
        this.toggleAnimatorA.animate(class141Var);
        this.toggleAnimatorB.animate(class141Var);
    }

    @Override
    protected boolean isContentVisible() {
        return isMainMenuPreview() || this.fadeAnimation.destination() > 0.5f;
    }

    public void updateTarget() {
        if (!Mc.INSTANCE.isWorldLoaded() || Mc.INSTANCE.getNetworkHandler() == null) {
            this.target = null;
            this.fadeAnimation.set(0.0f);
            this.healthAnimation.set(0.0f);
            this.absorptionAnimation.set(0.0f);
            this.healthNumberInitialized = false;
            return;
        }
        LivingEntity livingEntityMethod001 = findTarget();
        if (this.target != null && !PlayerVisualFilter.shouldRender(this.target)) {
            this.target = null;
            this.fadeAnimation.set(0.0f);
        }
        if (livingEntityMethod001 == null) {
            if (this.target != null) {
                this.fadeAnimation.destination(0.0f);
            }
            if (this.fadeAnimation.isZero()) {
                this.target = null;
                this.healthAnimation.set(0.0f);
                this.absorptionAnimation.set(0.0f);
                return;
            }
            return;
        }
        if (!Objects.equals(this.target, livingEntityMethod001)) {
            this.target = livingEntityMethod001;
            float healthBelowName = getDisplayHealth(livingEntityMethod001);
            float fMethod007 = getEffectiveMaxHealth(livingEntityMethod001);
            this.healthAnimation.set(MathUtil.clamp(fMethod007 > 0.0f ? healthBelowName / fMethod007 : 0.0f, 0.0f, 1.0f));
            this.absorptionAnimation.set(MathUtil.clamp(livingEntityMethod001.getAbsorptionAmount() / 20.0f, 0.0f, 1.0f));
            int healthNumber = Math.max(0, Math.round(healthBelowName));
            this.previousHealthNumber = healthNumber;
            this.currentHealthNumber = healthNumber;
            this.healthNumberInitialized = true;
            this.healthDigitAnimation.set(1.0f);
        }
        this.fadeAnimation.destination(1.0f);
    }

    public void renderAvatar(DrawEngine class154Var, Matrix4f matrix4f, ColorStack class115Var, float f, float f2, float f3, float f4, float f5) {
        if (isMainMenuPreview()) {
            GlTexture skin = previewSkinTexture();
            if (skin != null) {
                int color = class115Var.computeColor(0xFFFFFFFF);
                renderPlayerHead(class154Var, matrix4f, skin, f, f2, f3, f4, f5, color);
            }
            return;
        }
        Mc class815Var = Mc.INSTANCE;
        if (this.target instanceof AbstractClientPlayerEntity player) {
            AbstractTexture texture = class815Var.getTextureManager()
                    .getTexture(player.getSkinTextures().texture());
            ItemSpriteManager class219Var = ItemSpriteManager.INSTANCE;
            GlTexture orCreateTexture = class219Var.getOrCreateTexture(texture);
            int i = this.target.hurtTime;
            float tickDelta = renderTickDelta(class815Var);
            float f6 = 1.0f - ((i - (i != 0 ? tickDelta : 0.0f)) / padding);
            int iComputeColor = class115Var.computeColor(this.fadeAnimation.animatedValue(), StencilBufferUtil.STENCIL_MASK, (int) (100.0f + (155.0f * f6)), (int) (100.0f + (155.0f * f6)));
            if (orCreateTexture != null) {
                renderPlayerHead(class154Var, matrix4f, orCreateTexture,
                        f, f2, f3, f4, f5, iComputeColor);
            }
        }
    }

    // Keep this interface invocation in Java: JNI GetMethodID would also
    // initialize RenderTickCounter, which invokeinterface does not require.
    private static float renderTickDelta(Mc client) {
        return client.getRenderTickCounter().getTickDelta(false);
    }

    private void renderPlayerHead(DrawEngine draw, Matrix4f matrix, GlTexture skin,
                                  float x, float y, float width, float height,
                                  float radius, int color) {
        int texture = draw.bindTexture(skin.textureWithSTB());
        // Base face: 8..16 / 64. Hat/hair layer: 40..48 / 64.
        draw.roundedTexture(
                matrix, x, y, width, height, radius, radius, radius, radius,
                0.125f, 0.125f, 0.25f, 0.25f,
                texture, color, color, color, color
        );
        draw.roundedTexture(
                matrix, x, y, width, height, radius, radius, radius, radius,
                0.625f, 0.125f, 0.75f, 0.25f,
                texture, color, color, color, color
        );
    }

    private void renderEntityTypePortrait(DrawEngine draw, Matrix4f matrix,
                                          ColorStack colors, float x, float y,
                                          float size, int accent) {
        draw.textureVerticalC(
                matrix,
                this.entityTypeIcon,
                x + (size - 22.0f) / 2.0f,
                y + size / 2.0f,
                22,
                22,
                colors.computeColor(accent)
        );
    }

    private void renderPreviewBust(DrawEngine draw, Matrix4f matrix, ColorStack colors,
                                   float x, float y, float size) {
        GlTexture skin = previewSkinTexture();
        if (skin == null) {
            return;
        }
        int texture = draw.bindTexture(skin.textureWithSTB());
        int color = colors.computeColor(0xFFFFFFFF);
        float torsoX = x + size * 0.27f;
        float torsoY = y + size * 0.43f;
        float torsoWidth = size * 0.46f;
        float torsoHeight = size * 0.55f;
        draw.roundedTexture(matrix, torsoX, torsoY, torsoWidth, torsoHeight,
                2.5f, 2.5f, 2.5f, 2.5f,
                0.3125f, 0.3125f, 0.4375f, 0.5f,
                texture, color, color, color, color);
        draw.roundedTexture(matrix, torsoX, torsoY, torsoWidth, torsoHeight,
                2.5f, 2.5f, 2.5f, 2.5f,
                0.3125f, 0.5625f, 0.4375f, 0.75f,
                texture, color, color, color, color);
        float headSize = size * 0.55f;
        float headX = x + (size - headSize) / 2.0f;
        float headY = y + size * 0.08f;
        draw.roundedTexture(matrix, headX, headY, headSize, headSize,
                4.0f, 4.0f, 4.0f, 4.0f,
                0.125f, 0.125f, 0.25f, 0.25f,
                texture, color, color, color, color);
        draw.roundedTexture(matrix, headX - 0.5f, headY - 0.5f,
                headSize + 1.0f, headSize + 1.0f,
                4.0f, 4.0f, 4.0f, 4.0f,
                0.625f, 0.125f, 0.75f, 0.25f,
                texture, color, color, color, color);
    }

    private void renderEntityBust(LivingEntity entity, float x, float y,
                                  float size, float opacity) {
        if (entity == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        double guiScale = Math.max(1.0d, client.getWindow().getScaleFactor());
        float hudToGui = (float) (WidgetStack.HUD_SCALE / guiScale);
        int left = Math.round(x * hudToGui);
        int top = Math.round(y * hudToGui);
        int right = Math.round((x + size) * hudToGui);
        int bottom = Math.round((y + size) * hudToGui);
        int modelScale = Math.max(14, Math.round(38.0f * hudToGui));
        float centerX = (left + right) / 2.0f;
        float centerY = (top + bottom) / 2.0f;
        DrawContext context = new DrawContext(client, client.getBufferBuilders().getEntityVertexConsumers());
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, MathUtil.clamp(opacity, 0.0f, 1.0f));
        beginModelPortrait(opacity);
        try {
            InventoryScreen.drawEntity(
                    context,
                    left, top, right, bottom,
                    modelScale,
                    0.42f,
                    centerX + 9.0f * hudToGui,
                    centerY - 3.0f * hudToGui,
                    entity
            );
        } finally {
            try {
                context.draw();
            } finally {
                endModelPortrait();
                RenderSystem.setShaderColor(
                        1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    public static boolean isRenderingModelPortrait() {
        return renderingModelPortrait;
    }

    public static float modelPortraitOpacity() {
        return modelPortraitOpacity;
    }

    static void beginModelPortrait(float opacity) {
        modelPortraitOpacity = MathUtil.clamp(opacity, 0.0f, 1.0f);
        renderingModelPortrait = true;
    }

    static void endModelPortrait() {
        renderingModelPortrait = false;
        modelPortraitOpacity = 1.0f;
    }

    public LivingEntity findTarget() {
        LivingEntity livingEntityMethod005;
        Mc class815Var = Mc.INSTANCE;
        if ((livingEntityMethod005 = raycastTarget(class815Var)) != null
                && !livingEntityMethod005.getName().getString().isEmpty()) {
            return livingEntityMethod005;
        }
        if (!HudEditorScreen.isEditing() || getDisplayName(class815Var.getPlayer()).isEmpty()) {
            return null;
        }
        return class815Var.getPlayer();
    }

    @Override
    public void update() {
        if (isVisible()) {
            updateTarget();
        }
    }

    private void updateHealthNumber(int value) {
        if (!this.healthNumberInitialized) {
            this.previousHealthNumber = value;
            this.currentHealthNumber = value;
            this.healthNumberInitialized = true;
            this.healthDigitAnimation.set(1.0f);
            return;
        }
        if (value != this.currentHealthNumber) {
            this.previousHealthNumber = this.currentHealthNumber;
            this.currentHealthNumber = value;
            this.healthDigitAnimation.set(0.0f);
            this.healthDigitAnimation.destination(1.0f);
        }
    }

    private boolean isMainMenuPreview() {
        return HudEditorScreen.isAdvancedOpen() && !Mc.INSTANCE.isWorldLoaded();
    }

    @Override
    public boolean isEnabled() {
        // The main-menu editor must always expose Target HUD so it can be
        // positioned before joining a world, even when its module is disabled.
        return super.isEnabled() || isMainMenuPreview();
    }

    private GlTexture previewSkinTexture() {
        String username = Mc.INSTANCE.getSession().getUsername();
        UUID uuid = Mc.INSTANCE.getSession().getUuidOrNull();
        if (uuid == null) {
            uuid = UUID.nameUUIDFromBytes(
                    ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)
            );
        }
        if (this.previewProfile == null
                || !this.previewProfile.getId().equals(uuid)
                || !this.previewProfile.getName().equals(username)) {
            this.previewProfile = new GameProfile(uuid, username);
        }
        SkinTextures textures = Mc.INSTANCE.getSkinProvider().getSkinTextures(this.previewProfile);
        AbstractTexture texture = Mc.INSTANCE.getTextureManager().getTexture(textures.texture());
        return ItemSpriteManager.INSTANCE.getOrCreateTexture(texture);
    }

    private void renderRollingHealth(DrawEngine draw, MatrixStack matrices, ColorStack colors, float centerX, float centerY) {
        float progress = this.healthDigitAnimation.animatedValue();
        if (this.healthDigitAnimation.isAtDestination()) {
            draw.msdfFontVerticalCHorizontalC(
                    matrices.peek().getPositionMatrix(),
                    this.valueFont,
                    String.valueOf(this.currentHealthNumber),
                    centerX,
                    centerY,
                    10.0f,
                    0.05f,
                    colors.computeColor(SpectraHudStyle.TEXT)
            );
            return;
        }
        float travel = 12.0f;
        float direction = this.currentHealthNumber >= this.previousHealthNumber ? -1.0f : 1.0f;
        draw.beginScissor(matrices.peek().getPositionMatrix(), centerX - 11.0f, centerY - 6.0f, 22.0f, 12.0f);
        if (progress < 0.999f && this.previousHealthNumber != this.currentHealthNumber) {
            draw.msdfFontVerticalCHorizontalC(
                    matrices.peek().getPositionMatrix(),
                    this.valueFont,
                    String.valueOf(this.previousHealthNumber),
                    centerX,
                    centerY + direction * progress * travel,
                    10.0f,
                    0.05f,
                    colors.computeColor(SpectraHudStyle.TEXT)
            );
        }
        draw.msdfFontVerticalCHorizontalC(
                matrices.peek().getPositionMatrix(),
                this.valueFont,
                String.valueOf(this.currentHealthNumber),
                centerX,
                centerY - direction * (1.0f - progress) * travel,
                10.0f,
                0.05f,
                colors.computeColor(SpectraHudStyle.TEXT)
        );
        draw.endScissor();
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }

    public static float getEffectiveMaxHealth(LivingEntity livingEntity) {
        float maxHealth = livingEntity.getMaxHealth();
        if (!(livingEntity instanceof PlayerEntity)) {
            return maxHealth;
        }
        float healthBelowName = ScoreboardHelper.INSTANCE.getHealthBelowName(livingEntity);
        return Math.max(maxHealth, healthBelowName);
    }

    private static float getDisplayHealth(LivingEntity livingEntity) {
        return livingEntity instanceof PlayerEntity
                ? ScoreboardHelper.INSTANCE.getHealthBelowName(livingEntity)
                : livingEntity.getHealth();
    }

    public static boolean shouldShowAbsorption(LivingEntity livingEntity) {
        return livingEntity != null && !ServerUtil.isConnectedToServer("funtime") && livingEntity.getAbsorptionAmount() > 0.0f && livingEntity.getMaxAbsorption() <= 20.0f;
    }

    public static String formatValue(float f, boolean z) {
        return z ? "Unknown" : String.valueOf(MathUtil.round(f, roundIncrement));
    }

    public static List<ItemStack> getEquipment(LivingEntity livingEntity) {
        if (livingEntity == null) {
            return Collections.emptyList();
        }
        List<ItemStack> list = IteratorUtil.toList(livingEntity.getAllArmorItems().iterator());
        Collections.reverse(list);
        list.removeIf((v0) -> {
            return v0.isEmpty();
        });
        ItemStack mainHandStack = livingEntity.getMainHandStack();
        ItemStack offHandStack = livingEntity.getOffHandStack();
        if (!mainHandStack.isEmpty()) {
            list.add(mainHandStack);
        }
        if (!offHandStack.isEmpty()) {
            list.add(offHandStack);
        }
        return list;
    }

    public static String truncateToWidth(MsdfFont class161Var, String str, int i, float f) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        if (class161Var.getWidth(str, i) <= f) {
            return str;
        }
        int i2 = 0;
        int length = str.length();
        int i3 = 0;
        while (i2 <= length) {
            int i4 = (i2 + length) >>> 1;
            if (class161Var.getWidth(str.substring(0, i4), i) <= f) {
                i3 = i4;
                i2 = i4 + 1;
            } else {
                length = i4 - 1;
            }
        }
        return i3 <= 0 ? "" : str.substring(0, i3);
    }

    public String getDisplayName(LivingEntity livingEntity) {
        if (livingEntity == null) {
            return "";
        }
        String strStripTextFormat = StringHelper.stripTextFormat(livingEntity.getName().getString());
        NameProtectModule class512Var = (NameProtectModule) Spectra.INSTANCE.moduleRepository().get(NameProtectModule.class);
        return class512Var.isState() ? class512Var.replace(strStripTextFormat) : strStripTextFormat;
    }

    public LivingEntity raycastTarget(Mc class815Var) {
        ClientPlayerEntity player = class815Var.getPlayer();
        Entity cameraEntity = class815Var.getCameraEntity();
        if (player == null || cameraEntity == null || class815Var.getWorld() == null) {
            return null;
        }
        double entityInteractionRange = player.getEntityInteractionRange();
        Vec3d cameraPosVec = cameraEntity.getCameraPosVec(1.0f);
        Vec3d rotationVec = cameraEntity.getRotationVec(1.0f);
        Vec3d vec3dAdd = cameraPosVec.add(rotationVec.multiply(entityInteractionRange));
        List<LivingEntity> entitiesByClass = class815Var.getWorld().getEntitiesByClass(LivingEntity.class, cameraEntity.getBoundingBox().stretch(rotationVec.multiply(entityInteractionRange)).expand(1.0d), livingEntity -> {
            return livingEntity != player
                    && livingEntity.isAlive()
                    && !livingEntity.isSpectator()
                    && PlayerVisualFilter.shouldRender(livingEntity)
                    && (this.includeOtherEntities.isValue()
                    || livingEntity instanceof PlayerEntity);
        });
        LivingEntity retainedTarget = this.target;
        if (retainedTarget != null && !PlayerVisualFilter.shouldRender(retainedTarget)) {
            retainedTarget = null;
        }
        if (!this.includeOtherEntities.isValue()
                && !(retainedTarget instanceof PlayerEntity)) {
            retainedTarget = null;
        }
        LivingEntity livingEntityMethod010 = raycastSingle(
                retainedTarget, raycastExpand, cameraPosVec, vec3dAdd);
        if (livingEntityMethod010 != null) {
            return livingEntityMethod010;
        }
        if (entitiesByClass.size() <= 2) {
            return findClosestRaycast(entitiesByClass, roundIncrement, cameraPosVec, vec3dAdd);
        }
        return null;
    }

    public LivingEntity findClosestRaycast(List<LivingEntity> list, double d, Vec3d vec3d, Vec3d vec3d2) {
        LivingEntity livingEntity = null;
        double d2 = Double.MAX_VALUE;
        for (LivingEntity livingEntity2 : list) {
            Optional optionalRaycast = livingEntity2.getBoundingBox().expand(d).raycast(vec3d, vec3d2);
            if (!optionalRaycast.isEmpty()) {
                double dSquaredDistanceTo = vec3d.squaredDistanceTo((Vec3d) optionalRaycast.get());
                if (dSquaredDistanceTo < d2) {
                    d2 = dSquaredDistanceTo;
                    livingEntity = livingEntity2;
                }
            }
        }
        return livingEntity;
    }

    public LivingEntity raycastSingle(LivingEntity livingEntity, double d, Vec3d vec3d, Vec3d vec3d2) {
        if (livingEntity != null && livingEntity.isAlive()
                && PlayerVisualFilter.shouldRender(livingEntity)
                && livingEntity.getBoundingBox().expand(d).raycast(vec3d, vec3d2).isPresent()) {
            return livingEntity;
        }
        return null;
    }

    private TargetTypeDisplay targetType(LivingEntity entity) {
        if (entity instanceof PlayerEntity && FriendManager.isFriend(entity.getName().getString())) {
            return new TargetTypeDisplay("Friend", this.friendTypeIcon, null);
        }
        if (entity instanceof PlayerEntity) {
            return new TargetTypeDisplay("Player", null, PLAYER_TYPE_GLYPH);
        }
        if (entity instanceof AnimalEntity) {
            return new TargetTypeDisplay("Animal", this.animalTypeIcon, null);
        }
        if (entity instanceof MobEntity) {
            return new TargetTypeDisplay("Mob", this.mobTypeIcon, null);
        }
        return new TargetTypeDisplay("Entity", this.entityTypeIcon, null);
    }

    public enum PortraitMode implements DisplayNamed {
        HEAD("Head"),
        MODEL("Model");

        private final Translation displayName;

        PortraitMode(String displayName) {
            this.displayName = Translation.clearText(displayName);
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }

    private static SvgTexture svg(String path) {
        return new SvgTexture(new ClasspathResource(path), 128, 128);
    }

    private record TargetTypeDisplay(String label, SvgTexture icon, String menuGlyph) {
    }
}
