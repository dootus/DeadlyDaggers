package deadlydaggers.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import deadlydaggers.DeadlyDaggers;
import deadlydaggers.entity.ThrownDaggerEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.PotionUtil;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DaggerItem extends ToolItem implements Vanishable {
    private EntityType<ThrownDaggerEntity> cachedType = null;
    private final Multimap<EntityAttribute, EntityAttributeModifier> attributeModifiers;


    public DaggerItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Item.Settings settings) {
        super(toolMaterial, settings);
        float attackDamage1 = (float) attackDamage + toolMaterial.getAttackDamage();
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier", attackDamage1, EntityAttributeModifier.Operation.ADDITION));
        builder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Weapon modifier", attackSpeed, EntityAttributeModifier.Operation.ADDITION));
        builder.put(ReachEntityAttributes.ATTACK_RANGE, new EntityAttributeModifier("Attack range", -1.0, EntityAttributeModifier.Operation.ADDITION));
        this.attributeModifiers = builder.build();

    }


    @Environment(EnvType.CLIENT)
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        PotionUtil.buildTooltip(stack, tooltip, 0.125F);
    }

    public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        return !miner.isCreative();
    }

    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        for(StatusEffectInstance effect:PotionUtil.getPotionEffects(stack)){target.addStatusEffect(new StatusEffectInstance(effect));}
        NbtCompound t = stack.getNbt();
        if(t!=null){t.remove("Potion");
       t.remove("CustomPotionEffects");
        t.remove("CustomPotionColor");}


        //cancelling knockback
        Vec3d vec3d = attacker.getRotationVector();
        target.velocityModified = true;
        target.addVelocity(-vec3d.x * 0.5, -0.25, -vec3d.z * 0.5);


//backstab bonus damage
        //it's behind if the unit vectors are codirectional, dot product = 1 (since they're unit vectors)
        boolean isBehind = attacker.getRotationVector().dotProduct(target.getRotationVector()) > 0.8;

        if(isBehind) {
          //  System.out.println("BACKSTAB");
            target.timeUntilRegen=0;
            target.damage(new DeadlyDaggers.BackstabDamageSource(attacker),(float)attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
            target.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 0.5F);
        }

//only has half the invulnerability time of a sword
        target.timeUntilRegen = 10;


        stack.damage(1, attacker, (playerx) -> playerx.sendToolBreakStatus(Hand.MAIN_HAND));
        return true;
    }

    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (state.getHardness(world, pos) != 0.0F) {
            stack.damage(2, miner, (playerx) -> playerx.sendToolBreakStatus(Hand.MAIN_HAND));
        }

        return true;
    }

    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        if (state.isOf(Blocks.COBWEB) || state.isIn(BlockTags.WOOL)) {
            return 15.0F;
        } else {
            Material material = state.getMaterial();
            return material != Material.PLANT && material != Material.REPLACEABLE_PLANT && !state.isIn(BlockTags.LEAVES) && material != Material.GOURD ? 1.0F : 1.5F;
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (!world.isClient) {

                    stack.damage(1,user, player ->user.sendToolBreakStatus(user.getActiveHand()));



                    ThrownDaggerEntity daggerEntity = new ThrownDaggerEntity(world, user, stack.copy());
                    daggerEntity.setProperties(user, user.getPitch(), user.getYaw(), 0.0F, 2.5F, 1.0F);
                    daggerEntity.setPos(daggerEntity.getX(),daggerEntity.getY()-0.1,daggerEntity.getZ());
                    daggerEntity.setVelocity(daggerEntity.getVelocity().multiply(0.5D));
                    if (user.getAbilities().creativeMode) {
                        daggerEntity.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
                    }

                    world.spawnEntity(daggerEntity);
                    world.playSoundFromEntity(null, daggerEntity, SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    if (!user.getAbilities().creativeMode) {
                        stack.decrement(1);
                    }
            return TypedActionResult.success(stack,world.isClient());


    }
            return super.use(world, user, hand);
        }


    public EntityType<ThrownDaggerEntity> getType() {
        if (cachedType == null) {
            cachedType = DeadlyDaggers.DAGGER_MAP.inverse().get(this);
        }
        return cachedType;
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot equipmentSlot) {
        return equipmentSlot == EquipmentSlot.MAINHAND ? attributeModifiers : super.getAttributeModifiers(equipmentSlot);
    }



    }

