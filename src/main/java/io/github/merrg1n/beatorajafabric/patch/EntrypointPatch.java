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

public class EntrypointPatch extends GamePatch {
    @Override
    public void process(FabricLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
        String entrypoint = launcher.getEntrypoint();

        Log.debug(LogCategory.GAME_PATCH, "Entrypoint is " + entrypoint);
        ClassNode entrypointClazz = classSource.apply(entrypoint);
        if (entrypointClazz == null) {
            throw new LinkageError("Could not load entrypoint class " + entrypoint + "!");
        }
        Log.debug(LogCategory.GAME_PATCH, "Entrypoint class is " + entrypointClazz);

        MethodNode initMethod = findMethod(entrypointClazz, (method) -> method.name.equals("main"));

        if (initMethod == null) {
            // Do this if our method doesn't exist in the entrypoint class.
            throw new LinkageError("Could not find init method in " + entrypoint + "!");
        }
        // Debug log stating that we found our initializer method.
        Log.debug(LogCategory.GAME_PATCH, "Found init method: %s -> %s", entrypoint, entrypointClazz.name);
        // Debug log stating that the method is being patched with our hooks.
        Log.debug(LogCategory.GAME_PATCH, "Patching init method %s%s", initMethod.name, initMethod.desc);
        // Assign the variable `it` to the list of instructions for our initializer method.
        ListIterator<AbstractInsnNode> it = initMethod.instructions.iterator();
        // Add our hooks to the initializer method.
        it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BeatorajaHooks.INTERNAL_NAME, "init", "()V", false));
        classEmitter.accept(entrypointClazz);
    }
}
