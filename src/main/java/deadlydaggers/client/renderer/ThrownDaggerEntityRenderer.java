package deadlydaggers.client.renderer;

import deadlydaggers.DeadlyDaggers;
import deadlydaggers.entity.ThrownDaggerEntity;
import deadlydaggers.item.DaggerItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.*;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;

public class ThrownDaggerEntityRenderer extends EntityRenderer<ThrownDaggerEntity> {
   // public ThrownDaggerEntityRenderer(EntityRenderDispatcher entityRenderDispatcher) {
   //     super(entityRenderDispatcher);
   // }
    public ThrownDaggerEntityRenderer(EntityRendererFactory.Context context){super(context);}

//rendering the item directly so we don't use this
    @Override
    public Identifier getTexture(ThrownDaggerEntity entity) {
        return null;
    }


    public void render(ThrownDaggerEntity daggerEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i) {
        matrixStack.push();
        matrixStack.multiply((Vec3f.POSITIVE_Y.getDegreesQuaternion(daggerEntity.getYaw()-90)));
        matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(daggerEntity.getPitch() - 30));
//spinning daggers in flight
       if(!daggerEntity.isInGround()){
        matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(daggerEntity.age*-20));
       }
        MinecraftClient.getInstance().getItemRenderer()
                .renderItem(daggerEntity.asItemStack(), ModelTransformation.Mode.THIRD_PERSON_RIGHT_HAND, i, 700000, matrixStack, vertexConsumerProvider,1);
        matrixStack.pop();
        super.render(daggerEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }
}
