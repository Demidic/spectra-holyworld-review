package ru.spectra.mixin;

import ru.spectra.client.Spectra;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatInputSuggestor.class})
public abstract class ChatInputSuggestorMixin {

    @Shadow
    @Final
    MinecraftClient client;

    @Shadow
    @Final
    TextFieldWidget textField;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    private ChatInputSuggestor.SuggestionWindow window;

    @Shadow
    private ParseResults<CommandSource> parse;

    @Shadow
    boolean completingSuggestions;

    @Shadow
    private boolean windowActive;

    @Shadow
    public abstract void show(boolean z);

    @Inject(method = {"refresh"}, at = {@At("HEAD")}, cancellable = true)
    private void onRefresh(CallbackInfo callbackInfo) {
        String text = this.textField.getText();
        if (text.startsWith(Spectra.INSTANCE.commandDispatcher().getPrefix())) {
            callbackInfo.cancel();
            refreshCustomCommands(text);
        }
    }

    @Unique
    private void refreshCustomCommands(String str) {
        if (this.parse != null && !this.parse.getReader().getString().equals(str)) {
            this.parse = null;
        }
        if (!this.completingSuggestions) {
            this.textField.setSuggestion((String) null);
            this.window = null;
        }
        int cursor = this.textField.getCursor();
        String strSubstring = str.substring(Spectra.INSTANCE.commandDispatcher().getPrefix().length());
        List<String> suggestions = Spectra.INSTANCE.commandDispatcher().getSuggestions(strSubstring);
        if (suggestions.isEmpty() && cursor <= 0) {
            this.pendingSuggestions = Suggestions.empty();
            return;
        }
        new StringReader(str).skip();
        this.pendingSuggestions = buildSuggestions(str, strSubstring, suggestions, cursor);
        if (this.windowActive && ((Boolean) (Object) this.client.options.getAutoSuggestions().getValue()).booleanValue()) {
            this.pendingSuggestions.thenRun(() -> {
                if (this.pendingSuggestions.isDone()) {
                    show(false);
                }
            });
        }
    }

    @Unique
    private CompletableFuture<Suggestions> buildSuggestions(String str, String str2, List<String> list, int i) {
        int iLastIndexOf;
        String[] strArrSplit = str2.split("\\s+", -1);
        if (strArrSplit.length <= 1) {
            iLastIndexOf = str2.isEmpty() ? 1 : str.lastIndexOf(strArrSplit[0]);
        } else {
            String str3 = strArrSplit[strArrSplit.length - 1];
            iLastIndexOf = str3.isEmpty() ? i : str.lastIndexOf(str3);
        }
        SuggestionsBuilder suggestionsBuilder = new SuggestionsBuilder(str, iLastIndexOf);
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            suggestionsBuilder.suggest(it.next());
        }
        return CompletableFuture.completedFuture(suggestionsBuilder.build());
    }
}
