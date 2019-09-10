package com.edison.myplugin;

import org.gradle.api.logging.Logger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * created by edison 2019-09-07
 */
public class CallMethodAdapter extends MethodVisitor implements Opcodes {

    private Logger logger;

    public CallMethodAdapter(MethodVisitor methodVisitor,Logger logger) {
        super(Opcodes.ASM4, methodVisitor);
        this.logger = logger;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        logger.error("GeniusTransform--MethodInsn");
        if (opcode == Opcodes.INVOKEVIRTUAL && "setContentView".equals(name)){
//            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "androidx/appcompat/app/AppCompatActivity", "onCreate", "(Landroid/os/Bundle;)V", false);
            logger.error("GeniusTransform--in_MethodInsn11");

            mv.visitLdcInsn("hook_it");
            mv.visitLdcInsn("beforeSetContentView......");
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
            mv.visitInsn(Opcodes.POP);
        }

        if (opcode == Opcodes.RETURN){
            logger.error("GeniusTransform--in_MethodInsn22");
            mv.visitLdcInsn("hook_it");
            mv.visitLdcInsn("afterSetContentView......");
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
            mv.visitInsn(Opcodes.POP);
        }

        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitCode() {
        logger.error("GeniusTransform--visitCode");
        mv.visitLdcInsn("hook_it");
        mv.visitLdcInsn("beforeSetContentView......");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
        mv.visitInsn(Opcodes.POP);
        super.visitCode();
    }
}
