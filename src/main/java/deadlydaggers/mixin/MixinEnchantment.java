package deadlydaggers.mixin;

import deadlydaggers.item.DaggerItem;
import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class MixinEnchantment {
    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    private void isAcceptableItem(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
 if(stack.getItem() instanceof DaggerItem){
     Object o = (Object) this;
     if(o == Enchantments.LOYALTY
     || o == Enchantments.FIRE_ASPECT
     || o == Enchantments.LOOTING
     || o instanceof DamageEnchantment){info.setReturnValue(true);}
 }

    }
}