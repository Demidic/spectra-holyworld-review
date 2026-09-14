package ru.spectra.mixin;

import ru.spectra.client.event.TabListEntryEvent;
import ru.spectra.client.util.ScoreboardHelper;
import ru.spectra.client.Spectra;
import ru.spectra.client.module.BetterChatModule;
import ru.spectra.client.event.RenderOverlayEvent;
import ru.spectra.client.type.RenderOverlayType;
import ru.spectra.client.event.ChunkLoadEvent;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayNetworkHandler.class})
public abstract class ClientPlayNetworkHandlerMixin {

    @ModifyVariable(method = "sendChatCommand", at = @At("HEAD"), argsOnly = true)
    private String spectra$fixRussianCommandLayout(String command) {
        if (Spectra.INSTANCE == null) {
            return command;
        }
        BetterChatModule betterChat =
                Spectra.INSTANCE.moduleRepository().get(BetterChatModule.class);
        return betterChat == null ? command : betterChat.fixCommandLayout(command);
    }

    @Inject(method = {"handlePlayerListAction"}, at = {@At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z")})
    private void playerListActionHook(PlayerListS2CPacket.Action action, PlayerListS2CPacket.Entry entry, PlayerListEntry playerListEntry, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new TabListEntryEvent(playerListEntry));
    }

    @Redirect(method = {"onEntityStatus"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;showFloatingItem(Lnet/minecraft/item/ItemStack;)V"))
    private void onEntityStatus(GameRenderer gameRenderer, ItemStack itemStack) {
        RenderOverlayEvent class252Var = new RenderOverlayEvent(RenderOverlayType.TOTEM_POP);
        Spectra.INSTANCE.eventDispatcher().dispatch(class252Var);
        if (class252Var.isCancelled()) {
            return;
        }
        gameRenderer.showFloatingItem(itemStack);
    }

    @Inject(method = {"onChunkData"}, at = {@At("RETURN")})
    private void onChunkDataHook(ChunkDataS2CPacket chunkDataS2CPacket, CallbackInfo callbackInfo) {
        Spectra.INSTANCE.eventDispatcher().dispatch(new ChunkLoadEvent(new ChunkPos(chunkDataS2CPacket.getChunkX(), chunkDataS2CPacket.getChunkZ())));
    }

    @Inject(method = {"onPlayerListHeader"}, at = {@At("HEAD")})
    public void onPlayerListHeader(PlayerListHeaderS2CPacket playerListHeaderS2CPacket, CallbackInfo callbackInfo) {
        Text textHeader = playerListHeaderS2CPacket.header();
        if (textHeader != null) {
            ScoreboardHelper.INSTANCE.setHeader(textHeader);
        }
    }
}
