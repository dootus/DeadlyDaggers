package deadlydaggers.entity;

import deadlydaggers.DeadlyDaggers;
import deadlydaggers.item.DaggerItem;
import deadlydaggers.network.ThrownDaggerProjectileSpawnPacket;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.potion.PotionUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ThrownDaggerEntity extends PersistentProjectileEntity {
    private static final TrackedData<Byte> LOYALTY;
    private static final TrackedData<Boolean> ENCHANTED;
    private ItemStack daggerStack;
    private boolean dealtDamage;
    public int returnTimer;

    public ThrownDaggerEntity(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
        this.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        this.daggerStack = new ItemStack(DeadlyDaggers.WOODEN_DAGGER);
    }

    public ThrownDaggerEntity(World world, LivingEntity owner, ItemStack stack){
        super(((DaggerItem) stack.getItem()).getType(), owner, world);
       daggerStack = stack;
        this.dataTracker.set(LOYALTY, (byte) EnchantmentHelper.getLoyalty(stack));
        this.dataTracker.set(ENCHANTED, stack.hasGlint());
    }


    @Override
    public Packet<?> createSpawnPacket() {
        return ThrownDaggerProjectileSpawnPacket.createPacket(this);
    }

    //for dispensers
    public ThrownDaggerEntity(World world, double x, double y, double z, ItemStack stack){
        super(((DaggerItem) stack.getItem()).getType(),x,y,z,world);
        daggerStack = stack;
        this.dataTracker.set(LOYALTY, (byte) EnchantmentHelper.getLoyalty(stack));
        this.dataTracker.set(ENCHANTED, stack.hasGlint());
        pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
        this.setOwner(null);
    }


    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(LOYALTY, (byte)0);
        this.dataTracker.startTracking(ENCHANTED, false);
    }


    @Override
    public ItemStack asItemStack() {
        return daggerStack == null?new ItemStack(DeadlyDaggers.WOODEN_DAGGER):daggerStack;
       //return new ItemStack(DeadlyDaggers.WOODEN_DAGGER);
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }

        Entity entity = this.getOwner();
        if ((this.dealtDamage || this.isNoClip()) && entity != null) {
            int i = this.dataTracker.get(LOYALTY);
            if (i > 0 && !this.isOwnerAlive()) {
                if (!this.world.isClient && this.pickupType == PersistentProjectileEntity.PickupPermission.ALLOWED) {
                    this.dropStack(this.asItemStack(), 0.1F);
                }

                this.remove(RemovalReason.DISCARDED);
            } else if (i > 0) {
                this.setNoClip(true);
                Vec3d vec3d = new Vec3d(entity.getX() - this.getX(), entity.getEyeY() - this.getY(), entity.getZ() - this.getZ());
                this.setPos(this.getX(), this.getY() + vec3d.y * 0.015D * (double)i, this.getZ());
                if (this.world.isClient) {
                    this.lastRenderY = this.getY();
                }

                double d = 0.05D * (double)i;
                this.setVelocity(this.getVelocity().multiply(0.95D).add(vec3d.normalize().multiply(d)));
                if (this.returnTimer == 0) {
                    this.playSound(SoundEvents.ITEM_TRIDENT_RETURN, 10.0F, 1.0F);
                }

                ++this.returnTimer;
            }
        }

        super.tick();
    }

    private boolean isOwnerAlive() {
        Entity entity = this.getOwner();
        if (entity != null && entity.isAlive()) {
            return !(entity instanceof ServerPlayerEntity) || !entity.isSpectator();
        } else {
            return false;
        }
    }


    @Nullable
    protected EntityHitResult getEntityCollision(Vec3d currentPosition, Vec3d nextPosition) {
        return this.dealtDamage ? null : super.getEntityCollision(currentPosition, nextPosition);
    }

    public void onPlayerCollision(PlayerEntity player) {
        Entity entity = this.getOwner();
        if (entity == null || entity.getUuid() == player.getUuid()) {
            super.onPlayerCollision(player);
        }
    }

    public void age() {
        int i = this.dataTracker.get(LOYALTY);
        if (this.pickupType != PersistentProjectileEntity.PickupPermission.ALLOWED || i <= 0) {
            super.age();
        }
    }


    public boolean isInGround(){
        return this.inGround;
    }


    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        DaggerItem d = (DaggerItem)daggerStack.getItem();
        float damage = d.getMaterial().getAttackDamage() + 1;
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            damage += EnchantmentHelper.getAttackDamage(this.daggerStack, livingEntity.getGroup());
        }

        Entity entity2 = this.getOwner();
        DamageSource damageSource = DamageSource.thrownProjectile(this, entity2 == null ? this : entity2);
        this.dealtDamage = true;
        SoundEvent soundEvent = SoundEvents.ITEM_TRIDENT_HIT;
        if (entity.damage(damageSource, damage)) {
            if (entity.getType() == EntityType.ENDERMAN) {
                return;
            }

            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity2 = (LivingEntity)entity;
                if (entity2 instanceof LivingEntity) {
                    EnchantmentHelper.onUserDamaged(livingEntity2, entity2);
                    EnchantmentHelper.onTargetDamaged((LivingEntity)entity2, livingEntity2);
                }


                //backstab bonus damage


                //it's behind if the unit vectors are codirectional, dot product = 1 (since they're unit vectors)
                boolean isBehind = this.getVelocity().dotProduct(livingEntity2.getRotationVector()) > 0.8;

                if(isBehind) {
                //    System.out.println("RANGED BACKSTAB");
                    livingEntity2.timeUntilRegen=0;
                    livingEntity2.damage(new DeadlyDaggers.BackstabDamageSource(this.getOwner()),damage);
                    playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 0.5F);
                }


                for(StatusEffectInstance effect:PotionUtil.getPotionEffects(daggerStack)){livingEntity2.addStatusEffect(new StatusEffectInstance(effect));}
                NbtCompound t = daggerStack.getNbt();
                if(t!=null){t.remove("Potion");
                    t.remove("CustomPotionEffects");
                    t.remove("CustomPotionColor");}
                this.onHit(livingEntity2);
            }
        }

        this.setVelocity(this.getVelocity().multiply(-0.01D, -0.1D, -0.01D));
        float g = 1.0F;

        this.playSound(soundEvent, g, 1.0F);

    }

    @Override
    public void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        tag.put("daggerStack", daggerStack.writeNbt(new NbtCompound()));
        tag.putBoolean("DealtDamage", this.dealtDamage);

    }

    @Override
    public void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        if(tag.contains("daggerStack")){
            daggerStack = ItemStack.fromNbt(tag.getCompound("daggerStack"));
            this.dataTracker.set(LOYALTY, (byte) EnchantmentHelper.getLoyalty(daggerStack));
            this.dataTracker.set(ENCHANTED, daggerStack.hasGlint());
        }
        this.dealtDamage = tag.getBoolean("DealtDamage");
    }

    static {
        LOYALTY = DataTracker.registerData(ThrownDaggerEntity.class, TrackedDataHandlerRegistry.BYTE);
        ENCHANTED = DataTracker.registerData(ThrownDaggerEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    }

}
