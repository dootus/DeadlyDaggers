package deadlydaggers.client;

import deadlydaggers.DeadlyDaggers;
import deadlydaggers.client.renderer.ThrownDaggerEntityRenderer;
import deadlydaggers.network.ThrownDaggerProjectileSpawnPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.potion.PotionUtil;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DeadlyDaggersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {

        EntityRendererRegistry.INSTANCE.register(DeadlyDaggers.THROWN_WOODEN_DAGGER, (dispatcher, context) -> new ThrownDaggerEntityRenderer(dispatcher));
        EntityRendererRegistry.INSTANCE.register(DeadlyDaggers.THROWN_STONE_DAGGER, (dispatcher, context) -> new ThrownDaggerEntityRenderer(dispatcher));
        EntityRendererRegistry.INSTANCE.register(DeadlyDaggers.THROWN_IRON_DAGGER, (dispatcher, context) -> new ThrownDaggerEntityRenderer(dispatcher));
        EntityRendererRegistry.INSTANCE.register(DeadlyDaggers.THROWN_GOLD_DAGGER, (dispatcher, context) -> new ThrownDaggerEntityRenderer(dispatcher));
        EntityRendererRegistry.INSTANCE.register(DeadlyDaggers.THROWN_DIAMOND_DAGGER, (dispatcher, context) -> new ThrownDaggerEntityRenderer(dispatcher));
        EntityRendererRegistry.INSTANCE.register(DeadlyDaggers.THROWN_NETHERITE_DAGGER, (dispatcher, context) -> new ThrownDaggerEntityRenderer(dispatcher));

        ClientPlayNetworking.registerGlobalReceiver(ThrownDaggerProjectileSpawnPacket.ID, ThrownDaggerProjectileSpawnPacket::onPacket);


        for (Item dagger : DeadlyDaggers.DAGGER_MAP.inverse().keySet()) {
            FabricModelPredicateProviderRegistry.register(dagger, new Identifier("poisoned"), (itemStack, clientWorld, livingEntity) -> {
                if (PotionUtil.getPotionEffects(itemStack).isEmpty()) {
                    return 0.0F;
                } else return 1.0F;
            });

            ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
                if(tintIndex ==1){
                    return PotionUtil.getColor(stack) == 16253176 ? -1:PotionUtil.getColor(stack);

                }else{return -1;}
            }, dagger);

        }
    }

}