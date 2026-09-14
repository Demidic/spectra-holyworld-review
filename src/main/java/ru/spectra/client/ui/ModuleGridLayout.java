package ru.spectra.client.ui;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.Spectra;
import ru.spectra.client.model.CursorMoveInput;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.math.Easings;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.model.MouseButtonInput;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.util.WeightedEngine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class ModuleGridLayout extends AbstractTabLayout {
    public static final float as = 50.0f;
    public static final float at = 10.0f;
    public static final float au = 6.0f;
    public static final float av = 8.0f;
    public static final float aw = 14.0f;
    ScrollbarWidget scrollbar;
    private final MsdfFont emptyIconFont = Fonts.MENU_ICON.get();
    private final MsdfFont emptyTitleFont = Fonts.INTER_SEMIBOLD.get();
    private final Map<ModuleCard, ToggleAnimator> suggestionHoverAnimations =
            new IdentityHashMap<>();

    @Override
    public void initialize(MenuTabElement class732Var) {
        super.initialize(class732Var);
        ScrollArea class789VarScrollingAreaComponent = class732Var.scrollingAreaComponent();
        Objects.requireNonNull(class789VarScrollingAreaComponent);
        Supplier supplier = class789VarScrollingAreaComponent::scrollY;
        Supplier supplier2 = this::getContentHeight;
        Supplier supplier3 = () -> {
            return Float.valueOf(MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT);
        };
        ScrollArea class789VarScrollingAreaComponent2 = class732Var.scrollingAreaComponent();
        Objects.requireNonNull(class789VarScrollingAreaComponent2);
        this.scrollbar = new ScrollbarWidget(supplier, supplier2, supplier3, (v1) -> {
            class789VarScrollingAreaComponent.scrollTo(v1);
        }, 16.0f, 3.0f, 24.0f);
    }

    @Override
    public void render(DrawCtx class699Var) {
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        class115VarColorStack.push();
        MenuWindow menu = Spectra.INSTANCE.menuWindow();
        if (menu == null || menu.openAnimation.isOne()) {
            class115VarColorStack.alphaAnimation(this.tab.currentTabAnimation());
        }
        this.scrollbar.render(class699Var);
        this.tab.scrollingAreaComponent().beginArea(class699Var, originX(), originY(), width(), height());
        renderFrames(class699Var);
        this.tab.scrollingAreaComponent().endArea(class699Var, getContentHeight());
        renderEmptySearch(class699Var);
        renderOverlays(class699Var);
        class115VarColorStack.pop();
    }

    private void renderEmptySearch(DrawCtx ctx) {
        if (!isEmptySearch()) {
            return;
        }
        ColorStack colors = ctx.colorStack();
        float centerX = originX() + width() / 2.0f;
        float top = originY() + 113.0f;
        int muted = colors.computeColor(0x62626C, 175);
        String icon = "Ф";
        float iconSize = 27.0f;
        ctx.text(this.emptyIconFont, icon, Math.round(iconSize),
                centerX - this.emptyIconFont.getWidth(icon, iconSize) / 2.0f,
                top, muted);
        String title = "Функции по запросу не найдены";
        float titleSize = 13.0f;
        ctx.text(this.emptyTitleFont, title, Math.round(titleSize),
                centerX - this.emptyTitleFont.getWidth(title, titleSize) / 2.0f,
                top + 39.0f, muted);

        List<ModuleCard> suggestions = suggestions();
        if (suggestions.isEmpty()) {
            return;
        }
        this.suggestionHoverAnimations.values()
                .forEach(animation -> animation.state(false));
        float gap = 7.0f;
        float totalWidth = suggestions.stream()
                .map(card -> suggestionWidth(card.name()))
                .reduce(0.0f, Float::sum)
                + gap * (suggestions.size() - 1);
        float x = centerX - totalWidth / 2.0f;
        float y = top + 68.0f;
        for (ModuleCard card : suggestions) {
            float suggestionWidth = suggestionWidth(card.name());
            boolean hovered = isSuggestionHovered(ctx, x, y, suggestionWidth);
            ToggleAnimator hoverAnimation = this.suggestionHoverAnimations.computeIfAbsent(
                    card,
                    ignored -> new ToggleAnimator(170, Easings.EASE_IN_OUT_CUBIC)
            );
            hoverAnimation.state(hovered);
            float hover = hoverAnimation.smoothAnimation();
            int border = colors.computeColor(0x777784, Math.round(44.0f + 34.0f * hover));
            int fill = colors.computeColor(0xFFFFFF, Math.round(5.0f + 10.0f * hover));
            ctx.fillOutlinedRoundedRect(x, y, suggestionWidth, 25.0f, 7.0f, 1.0f, border, fill);
            ctx.text(this.emptyTitleFont, card.name(), 11, x + 10.0f, y + 6.0f,
                    colors.computeColor(0xA9A9B3, Math.round(170.0f + 60.0f * hover)));
            x += suggestionWidth + gap;
        }
    }

    private boolean isSuggestionHovered(DrawCtx ctx, float x, float y, float width) {
        var mouse = ctx.logicalMousePosition();
        return mouse.x() >= x && mouse.x() <= x + width
                && mouse.y() >= y && mouse.y() <= y + 25.0f;
    }

    public void renderFrames(DrawCtx class699Var) {
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame class757Var = this.tab.frames().get(size);
            if (class757Var.visible() && isFrameVisible(class757Var, as, originY())) {
                float dimmingForFrame = this.tab.dimmingManager().getDimmingForFrame(class757Var);
                if (dimmingForFrame > 0.01f) {
                    class115VarColorStack.push();
                    class115VarColorStack.alpha(1.0f - (dimmingForFrame * 0.5f));
                }
                class757Var.render(class699Var);
                if (dimmingForFrame > 0.01f) {
                    class115VarColorStack.pop();
                }
            }
        }
    }

    @Override
    public void renderOverlays(DrawCtx class699Var) {
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        this.tab.scrollingAreaComponent().scrollY();
        class115VarColorStack.push();
        class699Var.matrixStack().push();
        class699Var.matrixStack().translate(0.0f, -class699Var.layoutContext().toPhysical(this.tab.scrollingAreaComponent().scrollY()), 0.0f);
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame class757Var = this.tab.frames().get(size);
            if (class757Var.visible() && isFrameVisible(class757Var, as, originY())) {
                float dimmingForFrame = this.tab.dimmingManager().getDimmingForFrame(class757Var);
                if (dimmingForFrame > 0.01f) {
                    class115VarColorStack.push();
                    class115VarColorStack.alpha(1.0f - (dimmingForFrame * 0.5f));
                }
                class757Var.renderOverlays(class699Var);
                if (dimmingForFrame > 0.01f) {
                    class115VarColorStack.pop();
                }
            }
        }
        class699Var.matrixStack().pop();
        class115VarColorStack.pop();
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        this.tab.frames().forEach(class757Var -> {
            class757Var.layout(class698Var);
        });
        layoutScrollbar(class698Var);
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        this.scrollbar.animation(class141Var);
        if (!isEmptySearch()) {
            this.suggestionHoverAnimations.values()
                    .forEach(animation -> animation.state(false));
        }
        this.suggestionHoverAnimations.values()
                .forEach(animation -> animation.animate(class141Var));
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        if (!z && isEmptySearch() && class688Var.inputEvent() instanceof MouseButtonInput mouse
                && mouse.button() == 0 && mouse.action().press()
                && handleSuggestionClick(class688Var)) {
            return true;
        }
        boolean zHandleInput = z | this.scrollbar.handleInput(class688Var, z);
        boolean z2 = ((((float) class688Var.logicalMousePosition().y()) > this.tab.framesOriginY() ? 1 : (((float) class688Var.logicalMousePosition().y()) == this.tab.framesOriginY() ? 0 : -1)) < 0) && ((class688Var.inputEvent() instanceof MouseButtonInput) || (class688Var.inputEvent() instanceof CursorMoveInput));
        InputEventContext class688VarWithMouseOffset = class688Var.withMouseOffset(0.0f, this.tab.scrollingAreaComponent().scrollY());
        for (AbstractFrame class757Var : this.tab.frames()) {
            if (class757Var.visible() && isFrameVisible(class757Var, as, originY())) {
                if (class757Var.handleInput(class688VarWithMouseOffset, z2 || zHandleInput) && !z2) {
                    zHandleInput = true;
                }
            }
        }
        return zHandleInput | this.tab.scrollingAreaComponent().handleInput(class688Var, zHandleInput);
    }

    private boolean handleSuggestionClick(InputEventContext context) {
        List<ModuleCard> suggestions = suggestions();
        if (suggestions.isEmpty()) {
            return false;
        }
        float gap = 7.0f;
        float totalWidth = suggestions.stream()
                .map(card -> suggestionWidth(card.name()))
                .reduce(0.0f, Float::sum)
                + gap * (suggestions.size() - 1);
        float x = originX() + width() / 2.0f - totalWidth / 2.0f;
        float y = originY() + 181.0f;
        for (ModuleCard card : suggestions) {
            float suggestionWidth = suggestionWidth(card.name());
            if (context.inArea(x, y, suggestionWidth, 25.0f)) {
                Spectra.INSTANCE.menuWindow().headerContainer.setSearchText(card.name());
                this.tab.focusFrame(card);
                return true;
            }
            x += suggestionWidth + gap;
        }
        return false;
    }

    private boolean isEmptySearch() {
        return !this.tab.searchQuery().isEmpty()
                && this.tab.frames().stream().noneMatch(frame -> frame instanceof ModuleCard && frame.visible());
    }

    private float suggestionWidth(String value) {
        return this.emptyTitleFont.getWidth(value, 11.0f) + 20.0f;
    }

    private List<ModuleCard> suggestions() {
        if (this.tab.searchQuery().isEmpty()) {
            return List.of();
        }
        String query = normalize(this.tab.searchQuery());
        ArrayList<ModuleCard> cards = new ArrayList<>();
        for (AbstractFrame frame : this.tab.frames()) {
            if (frame instanceof ModuleCard card
                    && card.module().isVisibleInMenu()
                    && (this.tab.activeCategories.isEmpty()
                    || (card.module().getCategory() != null
                    && this.tab.activeCategories.contains(card.module().getCategory())))) {
                cards.add(card);
            }
        }
        cards.sort(Comparator
                .comparingInt((ModuleCard card) -> editDistance(query, normalize(card.name())))
                .thenComparing(ModuleCard::name, String.CASE_INSENSITIVE_ORDER));
        return cards.stream().limit(4).toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private static int editDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int column = 0; column <= right.length(); column++) {
            previous[column] = column;
        }
        for (int row = 1; row <= left.length(); row++) {
            current[0] = row;
            for (int column = 1; column <= right.length(); column++) {
                int cost = left.charAt(row - 1) == right.charAt(column - 1) ? 0 : 1;
                current[column] = Math.min(
                        Math.min(current[column - 1] + 1, previous[column] + 1),
                        previous[column - 1] + cost
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    @Override
    public float getContentHeight() {
        float f = 0.0f;
        for (AbstractFrame class757Var : this.tab.frames()) {
            if (class757Var.visible()) {
                float fY = (class757Var.y() - this.tab.framesOriginY()) + class757Var.height() + class757Var.contentHeight();
                if (fY > f) {
                    f = fY;
                }
            }
        }
        return f + 10.0f;
    }

    @Override
    public void positionFrames() {
        List<AbstractFrame> listFrames = this.tab.frames();
        listFrames.sort(Comparator.comparing(class757Var -> {
            if (class757Var instanceof ModuleCard) {
                return Boolean.valueOf(!((ModuleCard) class757Var).favorite());
            }
            return false;
        }).thenComparing(class757Var2 -> {
            return class757Var2 instanceof ModuleCard ? ((ModuleCard) class757Var2).name() : "";
        }, String.CASE_INSENSITIVE_ORDER));
        float fFloatValue = ((Float) listFrames.stream().filter((v0) -> {
            return v0.visible();
        }).map((v0) -> {
            return v0.width();
        }).max((v0, v1) -> {
            return Float.compare(v0, v1);
        }).orElse(Float.valueOf(0.0f))).floatValue();
        int iMax = fFloatValue > 0.0f
                ? Math.max(1, (int) Math.floor(((width() - at - au - this.scrollbar.width() + av) / (fFloatValue + av))))
                : 1;
        float[] fArr = new float[iMax];
        float[] fArr2 = new float[iMax];
        for (int i = 0; i < iMax; i++) {
            fArr2[i] = i * (fFloatValue + av);
        }
        boolean expansionInProgress = listFrames.stream()
                .filter(ModuleCard.class::isInstance)
                .map(ModuleCard.class::cast)
                .anyMatch(card -> !card.expansionAnimation.finished());
        int visibleIndex = 0;
        for (AbstractFrame class757Var3 : listFrames) {
            if (class757Var3.visible()) {
                int iIndexOfMin = visibleIndex % iMax;
                float f = fArr2[iIndexOfMin];
                float f2 = fArr[iIndexOfMin];
                if (class757Var3 instanceof ModuleCard) {
                    ((ModuleCard) class757Var3).targetPosition(
                            originX() + at + f,
                            originY() + at + f2,
                            !this.tab.forceImmediateFramePositioning() && !expansionInProgress
                    );
                } else {
                    class757Var3.setPosition(originX() + at + f, originY() + at + f2);
                }
                fArr[iIndexOfMin] = fArr[iIndexOfMin] + class757Var3.height() + class757Var3.contentHeight() + aw;
                visibleIndex++;
            }
        }
    }

    @Override
    public float width() {
        return MenuWindow.CONTENT_WIDTH;
    }

    public void layoutScrollbar(LayoutScaleContext class698Var) {
        float f = MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT;
        this.scrollbar.setPosition(((originX() + width()) - au) - this.scrollbar.width(), originY());
        this.scrollbar.setSize(this.scrollbar.width(), f);
        this.scrollbar.layout(class698Var);
    }
}
