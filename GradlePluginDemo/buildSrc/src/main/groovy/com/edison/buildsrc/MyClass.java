package com.edison.buildsrc;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;

public class MyClass implements Plugin {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public void init(JavacTask javacTask, String... strings) {

    }
}
