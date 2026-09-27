package com.example.factions.mixin;

import com.example.factions.Faction;
import com.example.factions.FactionManager;
import com.example.factions.FactionConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Перехватывает клик по слоту крафта ДО того, как AbstractContainerMenu#clicked
 * успевает списать ингредиенты и выдать результат. Это единственный надёжный способ
 * не допустить дюп-баг с шифт-крафтом через Mouse Tweaks/Inventory Profiles —
 * в отличие от ItemEvents.crafted (KubeJS), который срабатывает уже ПОСЛЕ списания.
 *
 * Работает и для обычного клика, и для quick-move (шифт), потому что оба пути
 * идут через один и тот же метод clicked(...).
 *
 * Примечание: сигнатура clicked(int, int, ClickType, Player) актуальна для
 * Minecraft 1.20.2+/1.21.x. Если при сборке сигнатура не совпадёт — глянь
 * маппинги AbstractContainerMenu в твоей версии и поправь @Inject.
 */
@Mixin(AbstractContainerMenu.class)
public class CraftingGuardMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void factions$blockForeignCraft(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
        if (slotId < 0 || slotId >= self.slots.size()) return;

        Slot slot = self.slots.get(slotId);
        // Нас интересует только слот РЕЗУЛЬТАТА крафта (верстак, печь и т.д.) —
        // именно там лежит итоговый предмет ДО того, как ингредиенты будут списаны.
        if (!(slot instanceof ResultSlot)) return;

        ItemStack result = slot.getItem();
        if (result.isEmpty()) return;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (id == null) return;

        Faction owner = FactionConfig.ownerOf(id.toString());
        if (owner == null) return; // общий предмет — можно всем

        if (FactionManager.get(serverPlayer) != owner) {
            ci.cancel(); // клик полностью отменяется: ни списания ресурсов, ни выдачи предмета
            serverPlayer.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    "Это относится к фракции «" + owner.displayName + "». Тебе недоступно.")
            );
        }
    }
}
