package net.environmentz.mixin;

import net.environmentz.EnvironmentzMain;
import net.environmentz.temperature.rooms.RoomManager;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 1001)
public class AbstractFurnanceBlockEntityMixin {
    //@Inject(method = "Lnet/minecraft/block/entity/AbstractFurnaceBlockEntity;tick(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/AbstractFurnaceBlockEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    @Inject(method = "tick", at=@At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z", shift = At.Shift.AFTER))
    private static void tick(World world, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity blockEntity, CallbackInfo info) {
        EnvironmentzMain.LOGGER.error("furnance state change: "+world.getBlockState(pos).get(AbstractFurnaceBlock.LIT));
        if (world.getBlockState(pos).get(AbstractFurnaceBlock.LIT)) {
            if (world instanceof ServerWorld) {
                RoomManager.getInstance(world.getServer()).addHeatSource(pos.getX(), pos.getY(), pos.getZ(), world, 2, 0);
            }
        }
    }
}
