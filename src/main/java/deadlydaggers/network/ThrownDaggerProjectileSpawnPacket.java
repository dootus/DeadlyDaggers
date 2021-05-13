package deadlydaggers.network;

import deadlydaggers.DeadlyDaggers;
import deadlydaggers.entity.ThrownDaggerEntity;
import deadlydaggers.item.DaggerItem;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;


import java.util.UUID;


//I could probably do something with serialising Ingredient.fromStack() to json and then just writing that on the buffer but whatever

public class ThrownDaggerProjectileSpawnPacket {

    public static final Identifier ID = new Identifier(DeadlyDaggers.MODID, "spawn_entity");





        public static Packet<?> createPacket(ThrownDaggerEntity entity) {
            PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
            buf.writeVarInt(Registry.ENTITY_TYPE.getRawId(entity.getType())); //1
            buf.writeUuid(entity.getUuid()); //2
            buf.writeVarInt(entity.getEntityId()); //3
            buf.writeDouble(entity.getX()); //4
            buf.writeDouble(entity.getY()); //5
            buf.writeDouble(entity.getZ()); //6
            buf.writeByte(MathHelper.floor(entity.pitch * 256.0F / 360.0F)); //7
            buf.writeByte(MathHelper.floor(entity.yaw * 256.0F / 360.0F)); //8

            ItemStack i = entity.asItemStack();
            buf.writeBoolean(i.hasEnchantments()); // 9

            //probably won't nullpointer :^)
            buf.writeString(i.getTag().getString("Potion")); //10
            if(i.getTag().contains("CustomPotionColor")) {
                buf.writeString(i.getTag().getString("CustomPotionColor"));//11
            }
            else{buf.writeInt(-1);}
            return ServerPlayNetworking.createS2CPacket(ID, buf);
        }

        @Environment(EnvType.CLIENT)
        public static void onPacket(MinecraftClient client, ClientPlayNetworkHandler networkHandler, PacketByteBuf buffer, PacketSender sender) {
            EntityType<?> type = Registry.ENTITY_TYPE.get(buffer.readVarInt()); //1
            UUID entityUUID = buffer.readUuid(); //2
            int entityID = buffer.readVarInt(); //3
            double x = buffer.readDouble(); //4
            double y = buffer.readDouble(); //5
            double z = buffer.readDouble(); //6
            float pitch = (buffer.readByte() * 360) / 256.0F; //7
            float yaw = (buffer.readByte() * 360) / 256.0F; //8

            //adding enchantment to client-side fake dagger stack so it glows if the real server-side dagger has enchantments
            boolean hasEnchantments = buffer.readBoolean(); //9
            ItemStack fakeDagger = new ItemStack(DeadlyDaggers.DAGGER_MAP.get(type),1);
            if(hasEnchantments){fakeDagger.addEnchantment(Enchantments.UNBREAKING,1);}

            String potion = buffer.readString(); //10
            fakeDagger.getOrCreateTag().putString("Potion",potion);
            int customPotionColor = buffer.readInt(); //11
            if(customPotionColor!=-1){
            fakeDagger.getTag().putInt("CustomPotionColor",customPotionColor);}

            //--------
            ClientWorld world = MinecraftClient.getInstance().world;
            final Entity entity = new ThrownDaggerEntity(world,x,y,z,fakeDagger);
//            final Entity entity = type.create(world);


            client.execute(() -> {
                if (world != null && entity != null) {
                    entity.updatePosition(x, y, z);
                    entity.updateTrackedPosition(x, y, z);
                    entity.pitch = pitch;
                    entity.yaw = yaw;
                    entity.setEntityId(entityID);
                    entity.setUuid(entityUUID);
                    world.addEntity(entityID, entity);
                }
            });
        }
    }


