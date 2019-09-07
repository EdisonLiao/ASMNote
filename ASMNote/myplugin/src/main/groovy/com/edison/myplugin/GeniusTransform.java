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

import java.io.File;
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


    public GeniusTransform(Project project){
        mProject = project;
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
        long startTime = System.currentTimeMillis();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        boolean isIncremental = transformInvocation.isIncremental();
        if(!isIncremental) {
            outputProvider.deleteAll();
        }
        for(TransformInput input : inputs) {
            for(JarInput jarInput : input.getJarInputs()) {
                Status status = jarInput.getStatus();
                File dest = outputProvider.getContentLocation(
                        jarInput.getFile().getAbsolutePath(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR);
                if(isIncremental) {
                    switch(status) {
                        case NOTCHANGED:
                            break;
                        case ADDED:
                        case CHANGED:
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

            for(DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY);
                FileUtils.forceMkdir(dest);
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
                                } catch (IOException e) {
                                    //maybe mkdirs fail for some strange reason, try again.
                                    Files.createParentDirs(destFile);
                                }
                                break;
                        }
                    }
                } else {
                    transformDir(directoryInput.getFile(), dest);
                }

            }

        }

    }

    private void transformDir(final File inputDir, final File outputDir) throws IOException {
        FileUtils.copyDirectory(inputDir, outputDir);
    }

    private void transformJar(final File srcJar, final File destJar, Status status) {
        waitableExecutor.execute(() -> {
            FileUtils.copyFile(srcJar, destJar);
            return null;
        });
    }

    /**
     * 是否增量
     * @return
     */
    @Override
    public boolean isIncremental() {
        return true;
    }
}
