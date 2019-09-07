package com.edison.myplugin;


import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.Collections;

/**
 * created by edison 2019-08-31
 */
public class GeniusPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        AppExtension appExtension = (AppExtension)project.getProperties().get("android");
        appExtension.registerTransform(new GeniusTransform(project), Collections.EMPTY_LIST);
    }
}
