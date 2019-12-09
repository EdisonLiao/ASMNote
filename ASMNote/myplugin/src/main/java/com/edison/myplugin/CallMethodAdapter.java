package com.edison.myplugin;

import org.gradle.api.logging.Logger;
import org.objectweb.asm.Label;
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

    /**
     * 访问每一行代码
     * @param opcode
     * @param owner
     * @param name
     * @param desc
     * @param itf
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        logger.error("GeniusTransform--MethodInsn");
//        if (opcode == Opcodes.INVOKEVIRTUAL && "setContentView".equals(name)){
//            logger.error("GeniusTransform--in_MethodInsn11");
//
//            mv.visitLdcInsn("hook_it");
//            mv.visitLdcInsn("beforeSetContentView......");
//            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
//            mv.visitInsn(Opcodes.POP);
//        }
//
//        if (opcode == Opcodes.RETURN){
//            logger.error("GeniusTransform--in_MethodInsn22");
//            mv.visitLdcInsn("hook_it");
//            mv.visitLdcInsn("afterSetContentView......");
//            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
//            mv.visitInsn(Opcodes.POP);
//        }

        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    /**
     * 方法开始时
     */
    @Override
    public void visitCode() {
        logger.error("GeniusTransform--visitCode");
//        mv.visitLdcInsn("hook_it");
//        mv.visitLdcInsn("beforeSetContentView......");
//        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
//        mv.visitInsn(Opcodes.POP);

        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(19, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn(new Integer(2131165325));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/edison/asmnote/MainActivity", "findViewById", "(I)Landroid/view/View;", false);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "android/widget/TextView");
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("Hello Kitty");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "android/widget/TextView", "setText", "(Ljava/lang/CharSequence;)V", false);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(21, l2);
        mv.visitInsn(Opcodes.RETURN);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLocalVariable("this", "Lcom/edison/asmnote/MainActivity;", null, l0, l3, 0);
        mv.visitLocalVariable("tv", "Landroid/widget/TextView;", null, l1, l3, 1);
        mv.visitMaxs(2, 2);


        super.visitCode();
    }

    /**
     * 方法结束时
     */
    @Override
    public void visitEnd() {
        super.visitEnd();
    }



}
