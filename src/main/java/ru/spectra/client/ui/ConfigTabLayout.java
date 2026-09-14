package ru.spectra.client.ui;
import ru.spectra.client.util.ActionDialogBuilder;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.net.CloudConfigDto;
import ru.spectra.client.net.CloudConfigMetadata;
import ru.spectra.client.net.CloudConfigService;
import ru.spectra.client.render.ColorStack;
import ru.spectra.client.config.ConfigFile;
import ru.spectra.client.render.DrawCtx;
import ru.spectra.client.type.DropdownOption;
import ru.spectra.client.Spectra;
import ru.spectra.client.render.Fonts;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.model.InputEventContext;
import ru.spectra.client.Lang;
import ru.spectra.client.model.LayoutScaleContext;
import ru.spectra.client.model.Translation;
import ru.spectra.client.config.LoadedConfig;
import ru.spectra.client.render.MsdfFont;
import ru.spectra.client.type.SortOrder;
import ru.spectra.client.ui.setting.TextFieldSettingElement;
import ru.spectra.client.render.ThemePalette;
import ru.spectra.client.render.ToggleAnimator;
import ru.spectra.client.util.WeightedEngine;
import ru.spectra.client.model.WidgetBounds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfigTabLayout extends AbstractTabLayout {
    static final float an = 30.0f;
    static final float ao = 12.0f;
    static final float ap = 72.0f;
    static final float aq = 12.0f;
    static final float ar = 0.0f;
    public final Consumer<CloudConfigCard> openDetailCallback;
    public final Consumer<CloudConfigCard> afterLoadCallback;

    public ScrollbarWidget scrollbar;
    public final DropdownWidget<SortOrder> sortDropdown;

    public SortOrder currentSortType;
    public final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    public final GlTexture folderIcon = new GlTexture(new ClasspathResource("/icons/menu/new/folder_fill.png"));
    public final GlTexture sortIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final IconLabelBadge headerBadge =
            new IconLabelBadge(this.folderIcon, Lang.CONFIG_TAB_PLACEHOLDER).uppercase(false);
    public final ConfigHeaderButton createButton;
    public final ConfigHeaderButton autoLoadButton;
    public final WidgetBounds headerBounds = new WidgetBounds(ar, ar, ar, ar);
    public final ToggleAnimator detailAnimator = ToggleAnimator.times(3, 150);
    public ConfigCard selectedFrame = null;
    private ConfigCreateCard createCard;

    public final ConfigDetailPanel detailPanel = new ConfigDetailPanel();

    public ConfigTabLayout() {
        this.detailPanel.setRequestCloseAnimated(this::closeDetail);
        this.detailPanel.setRequestRefreshList(this::updateConfigList);
        this.detailPanel.setRequestApplyConfigById(this::applyConfigById);
        this.detailPanel.setRequestSaveCurrent(this::saveConfig);
        this.detailPanel.setRequestResetToDefaults(this::resetToDefaults);
        this.currentSortType = SortOrder.NEWEST_FIRST;
        this.openDetailCallback = class768Var -> {
            CloudConfigCard currentContext;
            if (this.detailPanel.isOpened() && (currentContext = this.detailPanel.getCurrentContext()) != null && currentContext.cloudId().equals(class768Var.cloudId())) {
                this.detailPanel.close();
            } else {
                this.detailPanel.open(class768Var);
            }
            this.detailAnimator.state(this.detailPanel.isOpened());
        };
        this.afterLoadCallback = class768Var2 -> {
            if (this.detailPanel.isOpened()) {
                this.openDetailCallback.accept(class768Var2);
            }
        };
        this.createButton = new ConfigHeaderButton(
                Translation.clearText("Create"),
                this.font,
                new GlTexture(new ClasspathResource("/icons/menu/new/click.png")),
                this::beginInlineCreate,
                true,
                () -> true
        );
        this.autoLoadButton = new ConfigHeaderButton(
                Translation.clearText("Enable auto-load"),
                this.font,
                new GlTexture(new ClasspathResource("/icons/menu/new/arrow_rotation.png")),
                this::toggleAutoLoad,
                false,
                this::autoLoadEnabled,
                () -> autoLoadEnabled() ? "Disable auto-load" : "Enable auto-load"
        );
        this.sortDropdown = new DropdownWidget<>(this.font, this.sortIcon, 8.0f, 8.0f, 5.5f, 12);
        this.sortDropdown.setOptions(List.of(new DropdownOption(SortOrder.NEWEST_FIRST, Lang.CONFIG_SORT_NEWEST), new DropdownOption(SortOrder.OLDEST_FIRST, Lang.CONFIG_SORT_OLDEST), new DropdownOption(SortOrder.ALPHABETICAL_AZ, Lang.CONFIG_SORT_ALPHABETICAL_AZ), new DropdownOption(SortOrder.ALPHABETICAL_ZA, Lang.CONFIG_SORT_ALPHABETICAL_ZA), new DropdownOption(SortOrder.AUTHORSHIP, Lang.CONFIG_SORT_AUTHOR)), SortOrder.NEWEST_FIRST);
        this.sortDropdown.label(Lang.CONFIG_SORT_NEWEST, 12);
        this.sortDropdown.onSelect(class798Var -> {
            this.currentSortType = class798Var;
            Spectra.INSTANCE.tabsController().current().markFramesDirty();
        });
    }

    @Override
    public void initialize(MenuTabElement class732Var) {
        super.initialize(class732Var);
        ScrollArea class789VarScrollingAreaComponent = class732Var.scrollingAreaComponent();
        Objects.requireNonNull(class789VarScrollingAreaComponent);
        Supplier supplier = class789VarScrollingAreaComponent::scrollY;
        Supplier supplier2 = this::getContentHeight;
        Supplier supplier3 = () -> {
            return Float.valueOf(((MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT) - this.headerBounds.height()) - 16.0f);
        };
        ScrollArea class789VarScrollingAreaComponent2 = class732Var.scrollingAreaComponent();
        Objects.requireNonNull(class789VarScrollingAreaComponent2);
        this.scrollbar = new ScrollbarWidget(supplier, supplier2, supplier3, (v1) -> {
            class789VarScrollingAreaComponent.scrollTo(v1);
        }, 10.0f, 3.0f, 24.0f);
    }

    @Override
    public void render(DrawCtx class699Var) {
        ThemePalette class764VarPalette = class699Var.theme().palette();
        ColorStack class115VarColorStack = class699Var.drawEngine().colorStack();
        syncActiveSelection(this.tab);
        class115VarColorStack.push();
        class115VarColorStack.alphaAnimation(this.tab.currentTabAnimation());
        this.tab.scrollingAreaComponent().beginArea(class699Var, this.tab.framesOriginX(), this.tab.framesOriginY() + ap, width(), height() - ap);
        float fMethod008 = framesAlpha();
        class115VarColorStack.push();
        class115VarColorStack.alpha(fMethod008);
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame class757Var = this.tab.frames().get(size);
            if (class757Var.visible() && isFrameVisible(class757Var, an, originY() + ap)) {
                class757Var.render(class699Var);
            }
        }
        this.tab.scrollingAreaComponent().endArea(class699Var, getContentHeight());
        if (needsScrollbar()) {
            this.scrollbar.render(class699Var);
        }
        this.headerBadge.render(class699Var);
        class699Var.text(this.font, Lang.CONFIG_AVAILABLE.effective(), 16, this.headerBounds.x() + 10.0f, this.headerBounds.y() + this.headerBadge.height() + 8.0f, class115VarColorStack.computeColor(class764VarPalette.text().tone(300).argb()));
        this.autoLoadButton.render(class699Var);
        this.createButton.render(class699Var);
        class115VarColorStack.pop();
        renderOverlays(class699Var);
        if (!this.detailAnimator.isZero()) {
            class115VarColorStack.push();
            class115VarColorStack.alpha(Math.max(this.detailAnimator.smoothAnimation() - 2.0f, ar));
            this.detailPanel.render(class699Var);
            class115VarColorStack.pop();
        }
        class115VarColorStack.pop();
    }

    @Override
    public void renderOverlays(DrawCtx class699Var) {
        float physical = class699Var.layoutContext().toPhysical(this.tab.scrollingAreaComponent().scrollY());
        float fMethod008 = framesAlpha();
        class699Var.matrixStack().push();
        class699Var.matrixStack().translate(ar, -physical, ar);
        class699Var.drawEngine().colorStack().push();
        class699Var.drawEngine().colorStack().alpha(fMethod008);
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame class757Var = this.tab.frames().get(size);
            if (class757Var.visible()) {
                class757Var.renderOverlays(class699Var);
            }
        }
        class699Var.drawEngine().colorStack().pop();
        class699Var.matrixStack().pop();
    }

    public float framesAlpha() {
        float fSmoothAnimation = this.detailAnimator.smoothAnimation();
        if (fSmoothAnimation < 1.0f || fSmoothAnimation > 2.0f) {
            return fSmoothAnimation < 1.0f ? 1.0f - fSmoothAnimation : fSmoothAnimation - 2.0f;
        }
        return ar;
    }

    @Override
    public void layout(LayoutScaleContext class698Var) {
        this.tab.frames().forEach(class757Var -> {
            class757Var.layout(class698Var);
        });
        this.headerBounds.withPosition(originX() + 12.0f, originY() + 16.0f).withSize(((width() - 12.0f) - (needsScrollbar() ? this.scrollbar.width() : ar)) - 24.0f, 46.0f);
        this.headerBadge.layout(class698Var);
        this.headerBadge.setPosition(this.headerBounds.x() + 10.0f, this.headerBounds.y());
        this.createButton.layout(class698Var);
        this.autoLoadButton.layout(class698Var);
        float controlsY = (this.headerBounds.y() + this.headerBounds.height()) - this.createButton.height();
        this.createButton.setPosition(
                this.headerBounds.x() + this.headerBounds.width() - 10.0f - this.createButton.width(),
                controlsY
        );
        this.autoLoadButton.setPosition(
                this.createButton.x() - 6.0f - this.autoLoadButton.width(),
                controlsY
        );
        this.detailPanel.layout(class698Var);
        this.detailPanel.setSize(310.0f, height());
        this.detailPanel.setPosition(this.tab.framesOriginX() + width(), originY());
        layoutScrollbar(class698Var);
    }

    @Override
    public void animation(WeightedEngine class141Var) {
        float fSmoothAnimation = this.detailAnimator.smoothAnimation();
        this.detailAnimator.animate(class141Var);
        if (fSmoothAnimation != this.detailAnimator.smoothAnimation()) {
            Spectra.INSTANCE.tabsController().current().markFramesDirty();
        }
        this.detailPanel.animation(class141Var);
        this.createButton.animation(class141Var);
        this.autoLoadButton.animation(class141Var);
        this.scrollbar.animation(class141Var);
    }

    @Override
    public boolean handleInput(InputEventContext class688Var, boolean z) {
        boolean zHandleInput = z | this.createButton.handleInput(class688Var, z);
        zHandleInput |= this.autoLoadButton.handleInput(class688Var, zHandleInput);
        if (needsScrollbar()) {
            zHandleInput |= this.scrollbar.handleInput(class688Var, z);
        }
        if (!this.detailAnimator.isZero()) {
            zHandleInput |= this.detailPanel.handleInput(class688Var, zHandleInput);
        }
        InputEventContext class688VarWithMouseOffset = class688Var.withMouseOffset(ar, this.tab.scrollingAreaComponent().scrollY());
        for (AbstractFrame class757Var : this.tab.frames()) {
            if (class757Var.visible() && isFrameVisible(class757Var, an, originY() + ap)) {
                zHandleInput |= class757Var.handleInput(class688VarWithMouseOffset, zHandleInput);
            }
        }
        return zHandleInput | this.tab.scrollingAreaComponent().handleInput(class688Var, zHandleInput);
    }

    public void resetToDefaults(CloudConfigCard class768Var) {
        if (class768Var == null) {
            return;
        }
        ConfigFile class145VarDefaultConfigFile = Spectra.INSTANCE.defaultConfigFile();
        if (class145VarDefaultConfigFile == null) {
            Spectra.LOGGER.warn("Default config is not available");
            return;
        }
        Spectra.INSTANCE.cloudConfigService().applyConfig(new LoadedConfig(CloudConfigDto.of(class768Var.cloudId(), class768Var.name(), class768Var.author().name()), class145VarDefaultConfigFile));
        Spectra.INSTANCE.cloudConfigService().saveConfig(class768Var.cloudId(), Spectra.INSTANCE.moduleRepository(), Spectra.INSTANCE.widgetStack()).thenRun(() -> {
            updateConfigList();
            Spectra.INSTANCE.configManager().saveLastConfigID();
        }).exceptionally(th -> {
            Spectra.LOGGER.error("Failed to overwrite config with defaults", th);
            return null;
        });
    }

    public void updateConfigList() {
        Spectra.INSTANCE.cloudConfigService().listConfigs().thenAccept(list -> {
            ArrayList<AbstractFrame> arrayList = new ArrayList<>();
            if (this.createCard != null) {
                arrayList.add(this.createCard);
            }
            Iterator it = list.iterator();
            while (it.hasNext()) {
                arrayList.add(new ConfigCard(
                        new CloudConfigCard(
                                (CloudConfigMetadata) it.next(),
                                Spectra.INSTANCE.userSession().texture()
                        ),
                        this.afterLoadCallback,
                        this::handleCardAction
                ));
            }
            this.tab.updateFramesSafe(arrayList);
            Spectra.LOGGER.info("Loaded {} cloud configs", Integer.valueOf(list.size()));
        }).exceptionally(th -> {
            Spectra.LOGGER.error("Failed to load cloud configs", th);
            return null;
        });
    }

    public void selectFrame(ConfigCard class818Var) {
        if (this.selectedFrame != null && this.selectedFrame != class818Var) {
            this.selectedFrame.selected(false);
        }
        this.selectedFrame = class818Var;
        if (this.selectedFrame == null || this.selectedFrame.selected()) {
            return;
        }
        this.selectedFrame.selected(true);
    }

    public void clearSelection() {
        if (this.selectedFrame != null) {
            this.selectedFrame.selected(false);
            this.selectedFrame = null;
        }
    }

    public void syncActiveSelection(MenuTabElement class732Var) {
        CloudConfigService class357VarCloudConfigService = Spectra.INSTANCE.cloudConfigService();
        ConfigCard class818Var = null;
        for (AbstractFrame class757Var : class732Var.frames()) {
            if (class757Var instanceof ConfigCard) {
                ConfigCard class818Var2 = (ConfigCard) class757Var;
                if (class357VarCloudConfigService.configActive(class818Var2.configContext().cloudId())) {
                    class818Var = class818Var2;
                    break;
                }
            }
        }
        if (class818Var == null) {
            clearSelection();
        } else {
            selectFrame(class818Var);
        }
    }

    @Override
    public float getContentHeight() {
        float f = 0.0f;
        for (AbstractFrame class757Var : this.tab.frames()) {
            if (class757Var.visible()) {
                float fY = ((class757Var.y() - this.tab.framesOriginY()) - ap) + class757Var.height() + class757Var.contentHeight();
                if (fY > f) {
                    f = fY;
                }
            }
        }
        return f + 10.0f;
    }

    @Override
    public void positionFrames() throws MatchException {
        List<AbstractFrame> listFrames = this.tab.frames();
        sortFrames(listFrames);
        float fFloatValue = ((Float) this.tab.frames().stream().filter((v0) -> {
            return v0.visible();
        }).map((v0) -> {
            return v0.width();
        }).max((v0, v1) -> {
            return Float.compare(v0, v1);
        }).orElse(Float.valueOf(ar))).floatValue();
        int iMax = fFloatValue > ar ? Math.max(1, (int) Math.floor((((((width() - 12.0f) - (needsScrollbar() ? this.scrollbar.width() : ar)) - 24.0f) + 3.0f) + 12.0f) / (fFloatValue + 12.0f))) : 1;
        float[] fArr = new float[iMax];
        float[] fArr2 = new float[iMax];
        for (int i = 0; i < iMax; i++) {
            fArr2[i] = i * (fFloatValue + 12.0f);
        }
        float fSmoothAnimation = this.detailAnimator.smoothAnimation();
        boolean z = fSmoothAnimation > ar && fSmoothAnimation < 3.0f;
        for (AbstractFrame class757Var : listFrames) {
            if (class757Var.visible()) {
                int iIndexOfMin = indexOfMin(fArr);
                float f = fArr2[iIndexOfMin];
                float f2 = fArr[iIndexOfMin];
                float fHeight = class757Var.height() + class757Var.contentHeight();
                if (class757Var instanceof ConfigCard) {
                    ((ConfigCard) class757Var).targetPosition(originX() + 12.0f + f, originY() + ap + f2, (z || this.tab.forceImmediateFramePositioning()) ? false : true);
                } else {
                    class757Var.setPosition(originX() + 12.0f + f, originY() + ap + f2);
                }
                fArr[iIndexOfMin] = fArr[iIndexOfMin] + fHeight + ar;
            }
        }
    }

    public void layoutScrollbar(LayoutScaleContext class698Var) {
        float f = MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT;
        this.scrollbar.setPosition(((originX() + width()) - 12.0f) - this.scrollbar.width(), this.headerBounds.y() + this.headerBounds.height());
        this.scrollbar.setSize(this.scrollbar.width(), f);
        this.scrollbar.layout(class698Var);
    }

    public void sortFrames(List<AbstractFrame> list) throws MatchException {
        Comparator<? super AbstractFrame> comparator;
        switch (this.currentSortType.ordinal()) {
            case 0:
                comparator = (class757Var, class757Var2) -> {
                    if (!(class757Var instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var = (ConfigCard) class757Var;
                    if (!(class757Var2 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var2 = (ConfigCard) class757Var2;
                    boolean zFavorite = class818Var.favorite();
                    if (zFavorite != class818Var2.favorite()) {
                        return zFavorite ? -1 : 1;
                    }
                    return Long.compare(class818Var2.configContext().lastModified(), class818Var.configContext().lastModified());
                };
                break;
            case 1:
                comparator = (class757Var3, class757Var4) -> {
                    if (!(class757Var3 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var = (ConfigCard) class757Var3;
                    if (!(class757Var4 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var2 = (ConfigCard) class757Var4;
                    boolean zFavorite = class818Var.favorite();
                    if (zFavorite != class818Var2.favorite()) {
                        return zFavorite ? -1 : 1;
                    }
                    int iCompare = Long.compare(class818Var.configContext().lastModified(), class818Var2.configContext().lastModified());
                    return iCompare != 0 ? iCompare : class818Var.configContext().cloudId().compareToIgnoreCase(class818Var2.configContext().cloudId());
                };
                break;
            case 2:
                comparator = (class757Var5, class757Var6) -> {
                    if (!(class757Var5 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var = (ConfigCard) class757Var5;
                    if (!(class757Var6 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var2 = (ConfigCard) class757Var6;
                    boolean zFavorite = class818Var.favorite();
                    if (zFavorite != class818Var2.favorite()) {
                        return zFavorite ? -1 : 1;
                    }
                    int iCompareToIgnoreCase = class818Var.configContext().name().compareToIgnoreCase(class818Var2.configContext().name());
                    return iCompareToIgnoreCase != 0 ? iCompareToIgnoreCase : class818Var.configContext().cloudId().compareToIgnoreCase(class818Var2.configContext().cloudId());
                };
                break;
            case 3:
                comparator = (class757Var7, class757Var8) -> {
                    if (!(class757Var7 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var = (ConfigCard) class757Var7;
                    if (!(class757Var8 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var2 = (ConfigCard) class757Var8;
                    boolean zFavorite = class818Var.favorite();
                    if (zFavorite != class818Var2.favorite()) {
                        return zFavorite ? -1 : 1;
                    }
                    int iCompareToIgnoreCase = class818Var2.configContext().name().compareToIgnoreCase(class818Var.configContext().name());
                    return iCompareToIgnoreCase != 0 ? iCompareToIgnoreCase : class818Var.configContext().cloudId().compareToIgnoreCase(class818Var2.configContext().cloudId());
                };
                break;
            case 4:
                comparator = (class757Var9, class757Var10) -> {
                    if (!(class757Var9 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var = (ConfigCard) class757Var9;
                    if (!(class757Var10 instanceof ConfigCard)) {
                        return 0;
                    }
                    ConfigCard class818Var2 = (ConfigCard) class757Var10;
                    boolean zFavorite = class818Var.favorite();
                    if (zFavorite != class818Var2.favorite()) {
                        return zFavorite ? -1 : 1;
                    }
                    int iCompareToIgnoreCase = class818Var.configContext().author().name().compareToIgnoreCase(class818Var2.configContext().author().name());
                    return iCompareToIgnoreCase != 0 ? iCompareToIgnoreCase : class818Var.configContext().name().compareToIgnoreCase(class818Var2.configContext().name());
                };
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
        list.sort((first, second) -> {
            if (first instanceof ConfigCreateCard) {
                return second instanceof ConfigCreateCard ? 0 : -1;
            }
            if (second instanceof ConfigCreateCard) {
                return 1;
            }
            return comparator.compare(first, second);
        });
    }

    public void openImportDialog() {
        String[] strArr = {""};
        ActionConfirmDialog class777VarActionConfirmationDialogContainer = Spectra.INSTANCE.menuWindow().actionConfirmationDialogContainer();
        class777VarActionConfirmationDialogContainer.open(new ActionDialogBuilder().title(Lang.CONFIG_IMPORT_TITLE).placeholderText(Lang.CONFIG_ACTION_PLACEHOLDER).icon(this.folderIcon).content(class816Var -> {
            TextFieldSettingElement class835Var = new TextFieldSettingElement(Lang.CONFIG_IMPORT_ID_LABEL, Lang.CONFIG_IMPORT_ID_DESCRIPTION, Lang.CONFIG_IMPORT_ID_PLACEHOLDER, false, 50, false, () -> {
                return "";
            });
            class835Var.onChange(str -> {
                strArr[0] = str.trim();
                class777VarActionConfirmationDialogContainer.hideError();
            });
            class835Var.focusAtEnd();
            class816Var.addFrameElement(class835Var);
        }).confirmLabel(Lang.CONFIG_IMPORT_CONFIRM).enableConfirmButton(() -> {
            return strArr[0].trim().length() == 16;
        }).onConfirm(() -> {
            String strTrim = strArr[0].trim();
            class777VarActionConfirmationDialogContainer.setLoading(true, Lang.CONFIG_IMPORT_LOADING.effective());
            Spectra.INSTANCE.cloudConfigService().importConfig(strTrim).thenCompose(r4 -> {
                return Spectra.INSTANCE.cloudConfigService().downloadConfig(strTrim);
            }).thenAccept(class089Var -> {
                Spectra.INSTANCE.cloudConfigService().applyConfig(class089Var);
                class777VarActionConfirmationDialogContainer.setLoading(false, "");
                updateConfigList();
                Spectra.LOGGER.info("Config imported & applied: {} by {}", class089Var.details().name(), class089Var.details().author());
                class777VarActionConfirmationDialogContainer.close();
                Spectra.INSTANCE.configManager().saveLastConfigID();
            }).exceptionally(th -> {
                class777VarActionConfirmationDialogContainer.setLoading(false, "");
                Spectra.LOGGER.error("Failed to import/apply config", th);
                class777VarActionConfirmationDialogContainer.showError(Spectra.INSTANCE.cloudConfigService().errorMessage(th).effective());
                return null;
            });
        }).build());
    }

    public void openCreateDialog() {
        beginInlineCreate();
    }

    private void beginInlineCreate() {
        if (this.createCard != null) {
            this.createCard.focus();
            return;
        }
        this.createCard = new ConfigCreateCard(this::submitInlineCreate, this::cancelInlineCreate);
        ArrayList<AbstractFrame> frames = new ArrayList<>(this.tab.frames());
        frames.removeIf(frame -> frame instanceof ConfigCreateCard);
        frames.addFirst(this.createCard);
        this.tab.updateFramesSafe(frames);
        this.tab.markFramesDirty();
    }

    private void cancelInlineCreate() {
        if (this.createCard == null) {
            return;
        }
        ConfigCreateCard card = this.createCard;
        this.createCard = null;
        ArrayList<AbstractFrame> frames = new ArrayList<>(this.tab.frames());
        frames.remove(card);
        frames.removeIf(frame -> frame instanceof ConfigCreateCard);
        this.tab.updateFramesSafe(frames);
        this.tab.markFramesDirty();
    }

    private void submitInlineCreate(String name) {
        ConfigCreateCard card = this.createCard;
        if (card == null) {
            return;
        }
        card.setLoading(true);
        Spectra.INSTANCE.cloudConfigService()
                .createConfig(
                        name,
                        Spectra.INSTANCE.moduleRepository(),
                        Spectra.INSTANCE.widgetStack()
                )
                .thenAccept(id -> {
                    this.createCard = null;
                    Spectra.LOGGER.info("Config created: {} (id: {})", name, id);
                    updateConfigList();
                })
                .exceptionally(error -> {
                    Spectra.LOGGER.error("Failed to create config", error);
                    card.setError(Spectra.INSTANCE.cloudConfigService().errorMessage(error).effective());
                    return null;
                });
    }

    private void handleCardAction(CloudConfigCard context, ConfigCard.Action action) {
        this.detailPanel.setCurrentContext(context);
        switch (action) {
            case SAVE -> saveConfig(context);
            case DELETE -> this.detailPanel.openDeleteDialog();
            case RENAME -> this.detailPanel.openRenameDialog();
            case RESET -> this.detailPanel.openResetDialog();
        }
    }

    private boolean autoLoadEnabled() {
        return Spectra.INSTANCE.configManager().menuStateConfig().isConfigAutoLoadEnabled();
    }

    private void toggleAutoLoad() {
        var preferences = Spectra.INSTANCE.configManager().menuStateConfig();
        preferences.setConfigAutoLoadEnabled(!preferences.isConfigAutoLoadEnabled());
        try {
            Spectra.INSTANCE.configManager().saveMenuState();
        } catch (Exception error) {
            Spectra.LOGGER.error("Failed to save config auto-load preference", error);
        }
    }

    public void closeDetail() {
        if (this.detailPanel.isOpened()) {
            this.detailPanel.close();
            this.detailAnimator.state(false);
            Spectra.INSTANCE.tabsController().current().markFramesDirty();
        }
    }

    public void applyConfigById(String str) {
        Spectra.INSTANCE.cloudConfigService().downloadConfig(str).thenAccept(class089Var -> {
            Spectra.INSTANCE.cloudConfigService().applyConfig(class089Var);
            Spectra.LOGGER.info("Config applied: {} by {}", class089Var.details().name(), class089Var.details().author());
            updateConfigList();
            Spectra.INSTANCE.configManager().saveLastConfigID();
        }).exceptionally(th -> {
            Spectra.LOGGER.error("Failed to load/apply config", th);
            return null;
        });
    }

    public void saveConfig(CloudConfigCard class768Var) {
        if (class768Var == null) {
            return;
        }
        Spectra.INSTANCE.cloudConfigService().saveConfig(class768Var.cloudId(), Spectra.INSTANCE.moduleRepository(), Spectra.INSTANCE.widgetStack()).thenRun(() -> {
            Spectra.LOGGER.info("Config saved: {}", class768Var.cloudId());
            updateConfigList();
        }).exceptionally(th -> {
            Spectra.LOGGER.error("Failed to save config", th);
            return null;
        });
    }

    public boolean needsScrollbar() {
        return getContentHeight() > (((MenuWindow.MENU_HEIGHT - MenuWindow.COLLAPSED_HEADER_HEIGHT) - this.headerBounds.height()) - 16.0f) + 0.5f;
    }

    @Override
    public float width() {
        return MenuWindow.CONTENT_WIDTH - (this.detailPanel.width() * ((Math.max(ar, Math.min(1.0f, this.detailAnimator.smoothAnimation() - 1.0f)) / 2.0f) * 2.0f));
    }

    public ConfigCard getSelectedFrame() {
        return this.selectedFrame;
    }

    public SortOrder getCurrentSortType() {
        return this.currentSortType;
    }
}
