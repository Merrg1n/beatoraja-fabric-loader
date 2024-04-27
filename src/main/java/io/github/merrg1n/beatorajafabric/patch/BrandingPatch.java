package io.github.merrg1n.beatorajafabric.patch;

import io.github.merrg1n.beatorajafabric.BeatorajaHooks;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class BrandingPatch extends GamePatch {

    @Override
    public void process(FabricLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
        String target = "bms.player.beatoraja.MainLoader";
        String methodName = "start";
        boolean applied = false;
        ClassNode targetClazz = classSource.apply(target);
        if (targetClazz == null) {
            throw new LinkageError("Could not load class " + target + "!");
        }

        MethodNode targetMethod = findMethod(targetClazz, (method) -> method.name.equals(methodName));
        if (targetMethod == null) {
            throw new LinkageError("Could not find " + methodName + " method in " + target + "!");
        }

        Log.debug(LogCategory.GAME_PATCH, "Patching method %s %s%s", targetClazz.name, targetMethod.name, targetMethod.desc);

        ListIterator<AbstractInsnNode> it = targetMethod.instructions.iterator();
        while (it.hasNext()) {
            AbstractInsnNode insn = it.next();
            if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode isInsn = (MethodInsnNode) insn;
                if (!isInsn.owner.equals("javafx/stage/Stage") || !isInsn.name.equals("setTitle"))
                    continue;
                it.previous(); // before call setTitle
                it.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        BeatorajaHooks.INTERNAL_NAME,
                        "insertBranding",
                        "(Ljava/lang/String;)Ljava/lang/String;",
                        false));
                it.next();
                applied = true;
                break;
            }
        }
        if (applied) {
            classEmitter.accept(targetClazz);
        } else {
            Log.warn(LogCategory.GAME_PATCH, "Failed to apply brand name. Instruction not found.");
        }
    }
}
