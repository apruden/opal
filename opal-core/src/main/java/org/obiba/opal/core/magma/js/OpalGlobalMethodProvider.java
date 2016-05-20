package org.obiba.opal.core.magma.js;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.script.ScriptContext;
import javax.script.ScriptException;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.mozilla.javascript.Context;
import org.obiba.magma.js.MagmaContext;
import org.obiba.magma.js.MagmaContextFactory;
import org.obiba.magma.js.methods.AbstractGlobalMethodProvider;
import org.obiba.opal.core.runtime.OpalRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OpalGlobalMethodProvider extends AbstractGlobalMethodProvider {

  private static final Logger log = LoggerFactory.getLogger(OpalGlobalMethodProvider.class);

  /**
   * Set of methods to be exposed as top-level methods (ones that can be invoked anywhere)
   */
  private static final Set<String> GLOBAL_METHODS = ImmutableSet.of("source");

  @Override
  protected Set<String> getExposedMethods() {
    return GLOBAL_METHODS;
  }

  public static void source(ScriptContext ctx, Object[] args) {

    for(Object arg : args) {
      String fileName = Context.toString(arg);

      try {
        File file;

        if(fileName.startsWith("/")) {
          file = new File(System.getProperty("OPAL_HOME") + File.separator + "fs", fileName);
        } else {
          file = new File(OpalRuntime.MAGMA_JS_EXTENSION, fileName);
        }

        if(!file.getCanonicalPath().startsWith(System.getProperty("OPAL_HOME")))
          throw new RuntimeException("Unauthorized javascript library path: " + fileName);

        RequireCache cache;
        MagmaContext context = (MagmaContext) ctx;

        if(context.get(RequireCache.class) != null) {
          cache = (RequireCache) context.get(RequireCache.class);
          if(cache.hasLibrary(file.getAbsolutePath())) return;
        } else {
          cache = new RequireCache();
          context.push(RequireCache.class, cache);
        }

        log.debug("Loading file at path: {}", file.getAbsolutePath());

        try(FileReader reader = new FileReader(file)) {
          MagmaContextFactory.getEngine().eval(reader, context);
          cache.addLibrary(file.getAbsolutePath());
        } catch (ScriptException e) {
          throw Throwables.propagate(e);
        }
      } catch(FileNotFoundException e) {
        throw new RuntimeException("Javascript library not found: " + fileName, e);
      } catch(IOException e) {
        throw new RuntimeException("Javascript library cannot be read: " + fileName, e);
      }
    }
  }

  /**
   * Do not load the same library multiple times.
   */
  private static class RequireCache {

    private final List<String> libraries = Lists.newArrayList();

    public boolean hasLibrary(String path) {
      return libraries.contains(path);
    }

    public void addLibrary(String path) {
      if(!hasLibrary(path)) libraries.add(path);
    }

  }
}
