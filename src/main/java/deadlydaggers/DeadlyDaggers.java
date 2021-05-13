package deadlydaggers;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import deadlydaggers.entity.ThrownDaggerEntity;
import deadlydaggers.item.DaggerItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.loot.v1.FabricLootPoolBuilder;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterials;
import net.minecraft.loot.ConstantLootTableRange;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Position;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

//todo: if Fabric enchanting API gets merged, enable enchanting on table (currently most enchantments can only be obtained by books). There isn't an easy way to mess with EnchantmenTargets without something like that.
//todo: if combat test ever gets merged (even less likely), can switch to that from reach-attributes and messing with invulnerability timer in postHit
//dagger entities stuck in the world don't make a client instance on world load, so they're imperceptible (but can still be picked up properly). The same thing happens with Campanion spears, so if they ever figure it out I'll copy their fix.

public class DeadlyDaggers implements ModInitializer {

    public static final String MODID = "deadlydaggers";

    public static final DaggerItem WOODEN_DAGGER = new DaggerItem(ToolMaterials.WOOD, 1, -0.5F, new FabricItemSettings().group(ItemGroup.COMBAT));
    public static final DaggerItem STONE_DAGGER = new DaggerItem(ToolMaterials.STONE,1,-0.5f, new FabricItemSettings().group(ItemGroup.COMBAT));
    public static final DaggerItem IRON_DAGGER = new DaggerItem(ToolMaterials.IRON,1,-0.5f, new FabricItemSettings().group(ItemGroup.COMBAT));
    public static final DaggerItem GOLD_DAGGER = new DaggerItem(ToolMaterials.GOLD,1,-0.5f, new FabricItemSettings().group(ItemGroup.COMBAT));
    public static final DaggerItem DIAMOND_DAGGER = new DaggerItem(ToolMaterials.DIAMOND,1,-0.5f, new FabricItemSettings().group(ItemGroup.COMBAT));
    public static final DaggerItem NETHERITE_DAGGER = new DaggerItem(ToolMaterials.NETHERITE,1,-0.5f, new FabricItemSettings().group(ItemGroup.COMBAT));



    public static final EntityType<ThrownDaggerEntity> THROWN_WOODEN_DAGGER = registerDagger("thrown_wooden_dagger");
    public static final EntityType<ThrownDaggerEntity> THROWN_STONE_DAGGER = registerDagger("thrown_stone_dagger");
    public static final EntityType<ThrownDaggerEntity> THROWN_IRON_DAGGER = registerDagger("thrown_iron_dagger");
    public static final EntityType<ThrownDaggerEntity> THROWN_GOLD_DAGGER = registerDagger("thrown_gold_dagger");
    public static final EntityType<ThrownDaggerEntity> THROWN_DIAMOND_DAGGER = registerDagger("thrown_diamond_dagger");
    public static final EntityType<ThrownDaggerEntity> THROWN_NETHERITE_DAGGER = registerDagger("thrown_netherite_dagger");



    public static final BiMap<EntityType<ThrownDaggerEntity>,DaggerItem> DAGGER_MAP = ImmutableBiMap.<EntityType<ThrownDaggerEntity>,DaggerItem>builder()
            .put(THROWN_WOODEN_DAGGER,WOODEN_DAGGER)
            .put(THROWN_STONE_DAGGER,STONE_DAGGER)
            .put(THROWN_IRON_DAGGER,IRON_DAGGER)
            .put(THROWN_GOLD_DAGGER,GOLD_DAGGER)
            .put(THROWN_DIAMOND_DAGGER,DIAMOND_DAGGER)
            .put(THROWN_NETHERITE_DAGGER,NETHERITE_DAGGER).build();

//should probably move this stuff to its own class but whatever
    private static final List<Identifier> CHESTS_YOU_CAN_FIND_DAGGERS_IN = Arrays.asList(
            new Identifier("minecraft","chests/ruined_portal"),
            new Identifier("minecraft","chests/spawn_bonus_chest"),
            new Identifier("minecraft","chests/bastion_treasure"),
            new Identifier("minecraft","chests/nether_bridge"),
            new Identifier("minecraft","chests/stronghold_corridor"),
            new Identifier("minecraft","chests/village/village_weaponsmith")
    );

    private static EntityType<ThrownDaggerEntity> registerDagger(String id){
       return Registry.register(Registry.ENTITY_TYPE,new Identifier(DeadlyDaggers.MODID,id),
            FabricEntityTypeBuilder.<ThrownDaggerEntity>create(SpawnGroup.MISC,ThrownDaggerEntity::new)
            .dimensions(EntityDimensions.fixed(0.3f,0.3f))
            .trackRangeBlocks(4)
            .trackedUpdateRate(10).disableSummon()
            .build());

    }


    @Override
    public void onInitialize() {
        Registry.register(Registry.ITEM, new Identifier(MODID, "wooden_dagger"), WOODEN_DAGGER);
        Registry.register(Registry.ITEM, new Identifier(MODID, "stone_dagger"), STONE_DAGGER);
        Registry.register(Registry.ITEM, new Identifier(MODID, "iron_dagger"), IRON_DAGGER);
        Registry.register(Registry.ITEM, new Identifier(MODID, "gold_dagger"), GOLD_DAGGER);
        Registry.register(Registry.ITEM, new Identifier(MODID, "diamond_dagger"), DIAMOND_DAGGER);
        Registry.register(Registry.ITEM, new Identifier(MODID, "netherite_dagger"), NETHERITE_DAGGER);




        for(Item dagger : DAGGER_MAP.inverse().keySet()) {
            DispenserBlock.registerBehavior(dagger, new ProjectileDispenserBehavior() {
                @Override
                protected ProjectileEntity createProjectile(World world, Position position, ItemStack stack) {
                    return new ThrownDaggerEntity(world, position.getX(), position.getY(), position.getZ(), stack.copy());
                }
            });
        }

    for(Identifier chest : CHESTS_YOU_CAN_FIND_DAGGERS_IN) {
        LootTableLoadingCallback.EVENT.register((resourceManager, lootManager, id, supplier, setter) -> {
            if (chest.equals(id)) {
                supplier.pool(FabricLootPoolBuilder.builder().with(getInjectEntry(id.getPath())));
            }
    });
}

    }

private static LootPoolEntry.Builder<?> getInjectEntry(String name){
        Identifier table = new Identifier(MODID,"inject/"+name);
        return LootTableEntry.builder(table).weight(1);
}



public static class BackstabDamageSource extends EntityDamageSource{

        public BackstabDamageSource(Entity source){
            super("backstab",source);
            setBypassesArmor();
        }

    public Text getDeathMessage(LivingEntity entity) {
            //source can be null in this since daggers can be shit out by dispensers
        String string = "deadlydaggers.death.attack.backstab";
        if(source != null){string += ".creature";
        ItemStack itemStack = this.source instanceof LivingEntity ? ((LivingEntity)this.source).getMainHandStack() : ItemStack.EMPTY;
                if(!itemStack.isEmpty() && itemStack.hasCustomName()){string += ".item";
                    return new TranslatableText(string, entity.getDisplayName(),source.getDisplayName(), itemStack.toHoverableText());
                }
        return new TranslatableText(string, entity.getDisplayName(),source.getDisplayName()); }
        return new TranslatableText(string, entity.getDisplayName());
    }
}
}
