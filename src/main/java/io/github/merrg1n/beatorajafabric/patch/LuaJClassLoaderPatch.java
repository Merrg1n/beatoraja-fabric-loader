package io.github.merrg1n.beatorajafabric.patch;

import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class LuaJClassLoaderPatch extends GamePatch {

    @Override
    public void process(FabricLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {
        String target = "org.luaj.vm2.lib.jse.LuajavaLib";
        String methodName = "classForName";
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
            AbstractInsnNode inst = it.next();

            // find invokestatic getSystemClassLoader
            if (inst.getOpcode() != Opcodes.INVOKESTATIC) continue;
            MethodInsnNode isInst = (MethodInsnNode) inst;
            if (!isInst.owner.equals("java/lang/ClassLoader") || !isInst.name.equals("getSystemClassLoader"))
                continue;

            it.remove(); // remove this call
            it.add(new VarInsnNode(Opcodes.ALOAD, 0)); // load this
            it.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Object",
                    "getClass",
                    "()Ljava/lang/Class;",
                    false
            ));
            it.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Class",
                    "getClassLoader",
                    "()Ljava/lang/ClassLoader;",
                    false
            )); // call this.getClass().getClassLoader()
            applied = true;
            break;
        }
        if (applied) {
            classEmitter.accept(targetClazz);
        } else {
            Log.warn(LogCategory.GAME_PATCH, "Failed to apply brand name. Instruction not found.");
        }
    }
}
