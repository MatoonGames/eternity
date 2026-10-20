package com.matoon.eternity.dev;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Repairs respawning from any dimension other than the overworld in the dev client.
 *
 * Forge's {@code EntityPlayerMP.copyFrom} copies the inherited {@code EntityPlayer}
 * fields {@code spawnPos} and {@code spawnForced}. Those share their obfuscated names
 * with {@code EntityPlayerMP}'s own {@code managedPosX}/{@code managedPosZ}, and the
 * dev workspace's remapping picked the wrong ones, leaving references to a
 * {@code BlockPos managedPosX} and a {@code boolean managedPosZ} that do not exist.
 * Respawning then throws {@code NoSuchFieldError}, the player is stuck on the death
 * screen, and their death is never saved. Production Forge is unaffected.
 *
 * The fix is matched on the wrong descriptors, so it does nothing if the workspace
 * is ever regenerated with correct mappings.
 */
public class RespawnFieldFix implements IClassTransformer {

    private static final Logger LOG = LogManager.getLogger("EternityDevFixes");

    private static final String TARGET = "net.minecraft.entity.player.EntityPlayerMP";
    private static final String BLOCK_POS = "Lnet/minecraft/util/math/BlockPos;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET.equals(transformedName)) {
            return basicClass;
        }
        ClassNode node = new ClassNode();
        new ClassReader(basicClass).accept(node, 0);
        int fixed = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (!(insn instanceof FieldInsnNode)) {
                    continue;
                }
                FieldInsnNode field = (FieldInsnNode) insn;
                if ("managedPosX".equals(field.name) && BLOCK_POS.equals(field.desc)) {
                    field.name = "spawnPos";
                    fixed++;
                } else if ("managedPosZ".equals(field.name) && "Z".equals(field.desc)) {
                    field.name = "spawnForced";
                    fixed++;
                }
            }
        }
        if (fixed == 0) {
            return basicClass;
        }
        LOG.info("Repaired {} mis-mapped spawn field references in EntityPlayerMP", fixed);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }
}
