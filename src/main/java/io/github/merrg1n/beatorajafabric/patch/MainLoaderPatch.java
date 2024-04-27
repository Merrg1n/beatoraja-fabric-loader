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

public class MainLoaderPatch extends GamePatch {
    @Override
    public void process(FabricLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
        String entrypoint = launcher.getEntrypoint();

        Log.debug(LogCategory.GAME_PATCH, "Entrypoint is " + entrypoint);
        ClassNode targetClazz = classSource.apply(entrypoint);
        if (targetClazz == null) {
            throw new LinkageError("Could not load entrypoint class " + entrypoint + "!");
        }
        Log.debug(LogCategory.GAME_PATCH, "Entrypoint class is " + targetClazz);

        if (processInitMethod(targetClazz) && processBranding(targetClazz)) {
            classEmitter.accept(targetClazz);
        } else {
            throw new LinkageError("Failed to apply MainLoaderPatch, some patch failed.");
        }
    }

    private boolean processInitMethod(ClassNode targetClazz) {
        MethodNode initMethod = findMethod(targetClazz, (method) -> method.name.equals("main"));

        if (initMethod == null) {
            // Do this if our method doesn't exist in the entrypoint class.
            throw new LinkageError("Could not find init method in " + targetClazz.name + "!");
        }
        // Debug log stating that we found our initializer method.
        Log.debug(LogCategory.GAME_PATCH, "Patching method %s %s%s", targetClazz.name, initMethod.name, initMethod.desc);
        // Assign the variable `it` to the list of instructions for our initializer method.
        ListIterator<AbstractInsnNode> it = initMethod.instructions.iterator();
        // Add our hooks to the initializer method.
        it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BeatorajaHooks.INTERNAL_NAME, "init", "()V", false));
        return true;
    }

    private boolean processBranding(ClassNode targetClazz) {
        String methodName = "start";
        boolean applied = false;
        MethodNode targetMethod = findMethod(targetClazz, (method) -> method.name.equals(methodName));
        if (targetMethod == null) {
            throw new LinkageError("Could not find " + methodName + " method in " + targetClazz.name + "!");
        }

        Log.debug(LogCategory.GAME_PATCH, "Patching method %s %s%s", targetClazz.name, targetMethod.name, targetMethod.desc);

        ListIterator<AbstractInsnNode> it = targetMethod.instructions.iterator();
        while (it.hasNext()) {
            AbstractInsnNode insn = it.next();

            // find invokevirtual setTitle
            if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
            MethodInsnNode isInsn = (MethodInsnNode) insn;
            if (!isInsn.owner.equals("javafx/stage/Stage") || !isInsn.name.equals("setTitle")) continue;

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
        return applied;
    }
}
