package com.edison.myplugin;

import org.gradle.api.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * created by edison 2019-09-07
 */
public class CallClassAdapter extends ClassVisitor{

    private Logger logger;

    public CallClassAdapter(ClassVisitor classVisitor,Logger logger) {
        super(Opcodes.ASM4, classVisitor);
        this.logger = logger;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        logger.error("GeniusTransform--before_onCreate");
        if ("hookLoggg".equals(name) && cv != null){
            logger.error("GeniusTransform--is_onCreate");
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            return new CallMethodAdapter(mv,logger);
        }

        if (cv != null){
            return cv.visitMethod(access, name, desc, signature, exceptions);
        }

        return null;
    }
}
