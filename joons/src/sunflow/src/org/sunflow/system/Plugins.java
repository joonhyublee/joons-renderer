package org.sunflow.system;

import org.codehaus.janino.ClassBodyEvaluator;
import org.codehaus.janino.CompileException;
import org.codehaus.janino.Parser.ParseException;
import org.codehaus.janino.Scanner.ScanException;
import org.sunflow.system.UI.Module;
import org.sunflow.util.FastHashMap;

/**
 * This class represents a list of plugins which implement a certain interface
 * or extend a certain class. Many plugins may be registered and created at a
 * later time by recalling their unique name only.
 *
 * @param <T> Default constructible type or interface all plugins will derive
 * from or implement
 */
public final class Plugins<T> {

    private final FastHashMap<String, Class<? extends T>> pluginClasses;
    private final Class<T> baseClass;

    /**
     * Create an empty plugin list. You must specify
     * <code>T.class</code> as an argument.
     *
     * @param baseClass
     */
    public Plugins(Class<T> baseClass) {
        pluginClasses = new FastHashMap<String, Class<? extends T>>();
        this.baseClass = baseClass;
    }

    /**
     * Create an object from the specified type name. If this type name is
     * unknown or invalid,
     * <code>null</code> is returned.
     *
     * @param name plugin type name
     * @return an instance of the specified plugin type, or <code>null</code> if
     * not found or invalid
     */
    public T createObject(String name) {
        if (name == null || name.equals("none")) {
            return null;
        }
        Class<? extends T> c = pluginClasses.get(name);
        if (c == null) {
            // don't print an error, this will be handled by the caller
            return null;
        }
        try {
            return c.newInstance();
        } catch (InstantiationException e) {
            UI.printError(Module.API, "Cannot create object of type \"%s\" - %s", name, e.getLocalizedMessage());
            return null;
        } catch (IllegalAccessException e) {
            UI.printError(Module.API, "Cannot create object of type \"%s\" - %s", name, e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Check this plugin list for the presence of the specified type name
     *
     * @param name plugin type name
     * @return <code>true</code> if this name has been registered,
     * <code>false</code> otherwise
     */
    public boolean hasType(String name) {
        return pluginClasses.get(name) != null;
    }

    /**
     * Generate a unique plugin type name which has not yet been registered.
     * This is meant to be used when the actual type name is not crucial, but
     * succesfully registration is.
     *
     * @param prefix a prefix to be used in generating the unique name
     * @return a unique plugin type name not yet in use
     */
    public String generateUniqueName(String prefix) {
        String type;
        for (int i = 1; hasType(type = String.format("%s_%d", prefix, i)); i++) {
        }
        return type;
    }

    /**
     * Define a new plugin type from java source code. The code string contains
     * import declarations and a class body only. The implemented type is
     * implicitly the one of the plugin list being registered against.If the
     * plugin type name was previously associated with a different class, it
     * will be overriden. This allows the behavior core classes to be modified
     * at runtime.
     *
     * @param name plugin type name
     * @param sourceCode Java source code definition for the plugin
     * @return <code>true</code> if the code compiled and registered
     * successfully, <code>false</code> otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean registerPlugin(String name, String sourceCode) {
        try {
            ClassBodyEvaluator cbe = new ClassBodyEvaluator();
            cbe.setClassName(name);
            if (baseClass.isInterface()) {
                cbe.setImplementedTypes(new Class[]{baseClass});
            } else {
                cbe.setExtendedType(baseClass);
            }
            cbe.cook(sourceCode);
            return registerPlugin(name, cbe.getClazz());
        } catch (CompileException e) {
            UI.printError(Module.API, "Plugin \"%s\" could not be declared - %s", name, e.getLocalizedMessage());
            return false;
        } catch (ParseException e) {
            UI.printError(Module.API, "Plugin \"%s\" could not be declared - %s", name, e.getLocalizedMessage());
            return false;
        } catch (ScanException e) {
            UI.printError(Module.API, "Plugin \"%s\" could not be declared - %s", name, e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * Define a new plugin type from an existing class. This checks to make sure
     * the provided class is default constructible (ie: has a constructor with
     * no parameters). If the plugin type name was previously associated with a
     * different class, it will be overriden. This allows the behavior core
     * classes to be modified at runtime.
     *
     * @param name plugin type name
     * @param pluginClass class object for the plugin class
     * @return <code>true</code> if the plugin registered successfully,
     * <code>false</code> otherwise
     */
    public boolean registerPlugin(String name, Class<? extends T> pluginClass) {
        // check that the given class is compatible with the base class
        try {
            if (pluginClass.getConstructor() == null) {
                UI.printError(Module.API, "Plugin \"%s\" could not be declared - default constructor was not found", name);
                return false;
            }
        } catch (SecurityException e) {
            UI.printError(Module.API, "Plugin \"%s\" could not be declared - default constructor is not visible (%s)", name, e.getLocalizedMessage());
            return false;
        } catch (NoSuchMethodException e) {
            UI.printError(Module.API, "Plugin \"%s\" could not be declared - default constructor was not found (%s)", name, e.getLocalizedMessage());
            return false;
        }
        if (pluginClasses.get(name) != null) {
            UI.printWarning(Module.API, "Plugin \"%s\" was already defined - overwriting previous definition", name);
        }
        pluginClasses.put(name, pluginClass);
        return true;
    }
}