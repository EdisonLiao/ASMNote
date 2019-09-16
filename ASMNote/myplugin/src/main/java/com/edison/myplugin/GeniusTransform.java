package com.edison.myplugin;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.ide.common.internal.WaitableExecutor;
import com.google.common.io.Files;

import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * created by edison 2019-08-31
 */
public class GeniusTransform extends Transform {

    private Project mProject;
    private WaitableExecutor waitableExecutor;
    private final Logger logger;


    public GeniusTransform(Project project) {
        mProject = project;
        this.logger = project.getLogger();
        this.waitableExecutor = WaitableExecutor.useGlobalSharedThreadPool();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    /**
     * 限定当前transform处理的范围
     *
     * @return
     */
    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        Set<QualifiedContent.Scope> scopes = new HashSet<>();
        scopes.add(QualifiedContent.Scope.PROJECT);
        scopes.add(QualifiedContent.Scope.SUB_PROJECTS);
        scopes.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES);
        return scopes;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        boolean isIncremental = transformInvocation.isIncremental();
        logger.error("GeniusTransform---intotransform");
        if (!isIncremental) {
            outputProvider.deleteAll();
        }
        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                Status status = jarInput.getStatus();
                File dest = outputProvider.getContentLocation(
                        jarInput.getFile().getAbsolutePath(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR);
                if (isIncremental) {
                    switch (status) {
                        case NOTCHANGED:
                            break;
                        case ADDED:
                        case CHANGED:
                            logger.error("GeniusTransform---changeJar:" + jarInput.getFile().getAbsolutePath());
                            transformJar(jarInput.getFile(), dest, status);
                            break;
                        case REMOVED:
                            if (dest.exists()) {
                                FileUtils.forceDelete(dest);
                            }
                            break;
                    }
                } else {

                    transformJar(jarInput.getFile(), dest, status);
                }
            }

            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY);
//
                if(isIncremental) {
                    String srcDirPath = directoryInput.getFile().getAbsolutePath();
                    String destDirPath = dest.getAbsolutePath();
                    Map<File, Status> fileStatusMap = directoryInput.getChangedFiles();
                    for (Map.Entry<File, Status> changedFile : fileStatusMap.entrySet()) {
                        Status status = changedFile.getValue();
                        File inputFile = changedFile.getKey();
                        String destFilePath = inputFile.getAbsolutePath().replace(srcDirPath, destDirPath);
                        File destFile = new File(destFilePath);
                        switch (status) {
                            case NOTCHANGED:
                                break;
                            case REMOVED:
                                if(destFile.exists()) {
                                    //noinspection ResultOfMethodCallIgnored
                                    destFile.delete();
                                }
                                break;
                            case ADDED:
                            case CHANGED:
                                try {
                                    FileUtils.touch(destFile);
                                    logger.error("GeniusTransform--before_contain");
                                    if (destFilePath.contains("MainActivity")){
                                        logger.error("GeniusTransform--is_contain");
                                        weave(inputFile);
                                    }else {
                                        FileUtils.copyFile(directoryInput.getFile(), dest);
                                    }
                                } catch (IOException e) {
                                    //maybe mkdirs fail for some strange reason, try again.
                                    Files.createParentDirs(destFile);
                                }
                                break;
                        }
                    }
                } else {
                    logger.error("GeniusTransform--transformDir");
                    transformDir(directoryInput.getFile(), dest);
                }

            }

        }

    }

    private void transformDir(final File inputDir, final File outputDir) throws IOException {
        final String inputDirPath = inputDir.getAbsolutePath();
        final String outputDirPath = outputDir.getAbsolutePath();

        if (inputDir.isDirectory()) {
            for (final File file : com.android.utils.FileUtils.getAllFiles(inputDir)) {
                String filePath = file.getAbsolutePath();
                if (filePath.contains("MainActivity")) {
                    logger.error("GeniusTransform--MainActivity:" + filePath);
                    File outputFile = new File(filePath.replace(inputDirPath, outputDirPath));
                    logger.error("GeniusTransform--MainActivityOUT:" + outputFile.getAbsolutePath());
                    FileUtils.touch(outputFile);
                    weave(file);
                } else {
                    logger.error("GeniusTransform--else:" + filePath);
                    FileUtils.copyDirectory(inputDir, outputDir);
                }
            }
        }
    }

    private void transformJar(final File srcJar, final File destJar, Status status) {
        waitableExecutor.execute(() -> {
            FileUtils.copyFile(srcJar, destJar);
            return null;
        });
    }

    private void weave(File inputFile) {
        try {
            logger.error("GeniusTransform--wInputFile:" + inputFile.getAbsolutePath());
            FileInputStream is = new FileInputStream(inputFile);
            ClassReader cr = new ClassReader(is);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            ClassVisitor adapter = new CallClassAdapter(cw, logger);
            cr.accept(adapter, ClassReader.EXPAND_FRAMES);
            String outPutPath = inputFile.getParentFile().getAbsoluteFile() + File.separator + inputFile.getName();
            logger.error("GeniusTransform--wOutPutFile:" + outPutPath);
            FileOutputStream fos = new FileOutputStream(outPutPath);
            fos.write(cw.toByteArray());
            fos.flush();
            fos.close();
            is.close();
            logger.error("GeniusTransform--weave_ing,,");
        } catch (Exception e) {
            logger.error("GeniusTransform--weave_exception");
            e.printStackTrace();
        }
    }

    /**
     * 是否增量
     *
     * @return
     */
    @Override
    public boolean isIncremental() {
        return true;
    }
}
