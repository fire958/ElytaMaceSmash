package net.elytramace;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ElytraMaceClient implements ClientModInitializer {

    public static KeyBinding keyLockAttack;
    public static KeyBinding keySpamMace;
    
    public static boolean isTargetLockActive = false;
    public static boolean isMaceSpamActive = false;
    public static List<String> friendList = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        keyLockAttack = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elytramace.lock",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.elytramace"
        ));

        keySpamMace = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.elytramace.spam",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_Z,
                "category.elytramace"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            while (keyLockAttack.wasPressed()) {
                isTargetLockActive = !isTargetLockActive;
                client.player.sendMessage(Text.literal("§b[ElytraMace] §fTarget Lock Attack: " + (isTargetLockActive ? "§aON" : "§cOFF")), true);
            }

            while (keySpamMace.wasPressed()) {
                isMaceSpamActive = !isMaceSpamActive;
                client.player.sendMessage(Text.literal("§b[ElytraMace] §fMace Auto-Spam: " + (isMaceSpamActive ? "§aON" : "§cOFF")), true);
            }

            if (isTargetLockActive && client.player.isFallFlying()) {
                handleElytraMaceTargetLock(client);
            }

            if (isMaceSpamActive) {
                handleSpamMaceAttack(client);
            }
        });
    }

    private void handleElytraMaceTargetLock(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player.getMainHandStack().getItem() != Items.MACE) return;

        LivingEntity closestTarget = findClosestEnemy(client, 12.0);
        if (closestTarget != null) {
            double dx = closestTarget.getX() - player.getX();
            double dy = closestTarget.getEyeY() - player.getEyeY();
            double dz = closestTarget.getZ() - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            
            player.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
            player.setPitch((float) Math.toDegrees(-Math.atan2(dy, dist)));

            client.interactionManager.attackEntity(player, closestTarget);
            player.swingHand(Hand.MAIN_HAND);
        }
    }

    private void handleSpamMaceAttack(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player.getMainHandStack().getItem() != Items.MACE) return;

        LivingEntity target = findClosestEnemy(client, 4.5);
        if (target != null && player.getAttackCooldownProgress(0.5f) >= 0.9f) {
            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
        }
    }

    private LivingEntity findClosestEnemy(MinecraftClient client, double range) {
        PlayerEntity player = client.player;
        LivingEntity closest = null;
        double minDistance = range;

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof LivingEntity target && entity != player) {
                if (target instanceof PlayerEntity && friendList.contains(target.getName().getString())) {
                    continue;
                }
                double dist = player.distanceTo(target);
                if (dist <= minDistance && target.isAlive()) {
                    minDistance = dist;
                    closest = target;
                }
            }
        }
        return closest;
    }
}
