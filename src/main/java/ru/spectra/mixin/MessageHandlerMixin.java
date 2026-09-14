package ru.spectra.mixin;

import ru.spectra.client.event.ChatReceiveEvent;
import ru.spectra.client.type.ChatMessageType;
import ru.spectra.client.Spectra;
import com.mojang.authlib.GameProfile;
import java.time.Instant;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MessageHandler.class})
public class MessageHandlerMixin {

    @Shadow
    private long lastProcessTime;

    @Inject(method = {"method_45745"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/network/message/MessageType$Parameters;applyChatDecoration(Lnet/minecraft/text/Text;)Lnet/minecraft/text/Text;", shift = At.Shift.BEFORE)}, cancellable = true)
    private void injectDisguisedChatLambda(MessageType.Parameters parameters, Text text, Instant instant, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (spectra_ru$emitChatEvent(parameters, text, ChatMessageType.DISGUISED_CHAT_MESSAGE)) {
            this.lastProcessTime = Util.getMeasuringTimeMs();
            callbackInfoReturnable.cancel();
        }
    }

    @Inject(method = {"processChatMessageInternal"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", ordinal = 0, shift = At.Shift.BEFORE)}, cancellable = true)
    private void injectChatMessage1(MessageType.Parameters parameters, SignedMessage signedMessage, Text text, GameProfile gameProfile, boolean z, Instant instant, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (spectra_ru$emitChatEvent(null, text, ChatMessageType.CHAT_MESSAGE)) {
            this.lastProcessTime = Util.getMeasuringTimeMs();
            callbackInfoReturnable.cancel();
        }
    }

    @Inject(method = {"processChatMessageInternal"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", ordinal = 1, shift = At.Shift.BEFORE)}, cancellable = true)
    private void injectChatMessage2(MessageType.Parameters parameters, SignedMessage signedMessage, Text text, GameProfile gameProfile, boolean z, Instant instant, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (spectra_ru$emitChatEvent(parameters, text, ChatMessageType.CHAT_MESSAGE)) {
            this.lastProcessTime = Util.getMeasuringTimeMs();
            callbackInfoReturnable.cancel();
        }
    }

    @Inject(method = {"onGameMessage"}, at = {@At("HEAD")}, cancellable = true)
    private void injectGameMessage(Text text, boolean z, CallbackInfo callbackInfo) {
        if (spectra_ru$emitChatEvent(null, text, z ? ChatMessageType.DISGUISED_CHAT_MESSAGE : ChatMessageType.GAME_MESSAGE)) {
            this.lastProcessTime = Util.getMeasuringTimeMs();
            callbackInfo.cancel();
        }
    }

    @Unique
    private boolean spectra_ru$emitChatEvent(MessageType.Parameters parameters, Text text, ChatMessageType class068Var) {
        ChatReceiveEvent class066Var = new ChatReceiveEvent(text.getString(), text, class068Var, text2 -> {
            return parameters != null ? parameters.applyChatDecoration(text) : text;
        });
        Spectra.INSTANCE.eventDispatcher().dispatch(class066Var);
        return class066Var.isCancelled();
    }
}
