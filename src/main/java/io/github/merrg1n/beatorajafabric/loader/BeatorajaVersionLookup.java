package io.github.merrg1n.beatorajafabric.loader;

import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.util.ExceptionUtil;
import net.fabricmc.loader.impl.util.SimpleClassPath;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;

import static org.spongepowered.asm.util.Constants.STRING_DESC;

public final class BeatorajaVersionLookup {
    private static boolean isProbableVersion(String str) {
        return str.startsWith("beatoraja") || str.startsWith("LR2oraja");
    }

    public static String getVersion(Path gameJar) {
        try (SimpleClassPath cp = new SimpleClassPath(Collections.singletonList(gameJar))) {
            return fillVersionFromJar(cp);
        } catch (IOException e) {
            throw ExceptionUtil.wrap(e);
        }
    }

    public static String fillVersionFromJar(SimpleClassPath cp) throws IOException {
        String ver = Optional.ofNullable(cp.getInputStream("bms/player/beatoraja/MainController.class"))
                .flatMap(is -> analyze(is, new FieldStringConstantVisitor("VERSION")))
                .orElse("beatoraja unknown");
        return ver.split(" ")[1];
    }


    private static <T extends ClassVisitor & Analyzer> Optional<String> analyze(InputStream is, T analyzer) {
        try {
            ClassReader cr = new ClassReader(is);
            cr.accept(analyzer, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

            return Optional.ofNullable(analyzer.getResult());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // ignored
            }
        }

        return Optional.empty();
    }


    private interface Analyzer {
        String getResult();
    }

    private static final class FieldStringConstantVisitor extends ClassVisitor implements Analyzer {
        private final String fieldName;
        private String className;
        private String result;

        FieldStringConstantVisitor(String fieldName) {
            super(FabricLoaderImpl.ASM_VERSION);

            this.fieldName = fieldName;
        }

        @Override
        public String getResult() {
            return result;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.className = name;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (result == null && name.equals(fieldName) && descriptor.equals(STRING_DESC) && value instanceof String) {
                result = (String) value;
            }

            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (result != null || !name.equals("<clinit>")) return null;

            // capture LDC ".." followed by PUTSTATIC this.fieldName
            return new InsnFwdMethodVisitor() {
                @Override
                public void visitLdcInsn(Object value) {
                    String str;

                    if (value instanceof String && isProbableVersion(str = (String) value)) {
                        lastLdc = str;
                    } else {
                        lastLdc = null;
                    }
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    if (result == null && lastLdc != null && opcode == Opcodes.PUTSTATIC && owner.equals(className) && name.equals(fieldName) && descriptor.equals(STRING_DESC)) {
                        result = lastLdc;
                    }

                    lastLdc = null;
                }

                @Override
                protected void visitAnyInsn() {
                    lastLdc = null;
                }

                String lastLdc;
            };
        }
    }

    private abstract static class InsnFwdMethodVisitor extends MethodVisitor {
        InsnFwdMethodVisitor() {
            super(FabricLoaderImpl.ASM_VERSION);
        }

        protected abstract void visitAnyInsn();

        @Override
        public void visitLdcInsn(Object value) {
            visitAnyInsn();
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            visitAnyInsn();
        }

        @Override
        public void visitInsn(int opcode) {
            visitAnyInsn();
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            visitAnyInsn();
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            visitAnyInsn();
        }

        @Override
        public void visitTypeInsn(int opcode, java.lang.String type) {
            visitAnyInsn();
        }

        @Override
        public void visitMethodInsn(int opcode, java.lang.String owner, java.lang.String name, java.lang.String descriptor, boolean isInterface) {
            visitAnyInsn();
        }

        @Override
        public void visitInvokeDynamicInsn(java.lang.String name, java.lang.String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            visitAnyInsn();
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            visitAnyInsn();
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            visitAnyInsn();
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            visitAnyInsn();
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            visitAnyInsn();
        }

        @Override
        public void visitMultiANewArrayInsn(java.lang.String descriptor, int numDimensions) {
            visitAnyInsn();
        }
    }
}
