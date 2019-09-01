package com.edison.ohgenius;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;


import org.gradle.api.Project;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * created by edison 2019-08-31
 */
public class GeniusTransform extends Transform {

    private Project mProject;

    public GeniusTransform(Project project){
        mProject = project;
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
        super.transform(transformInvocation);
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
