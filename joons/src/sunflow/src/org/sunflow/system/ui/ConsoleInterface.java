package org.sunflow.system.ui;

import org.sunflow.system.UI;
import org.sunflow.system.UserInterface;
import org.sunflow.system.UI.Module;
import org.sunflow.system.UI.PrintLevel;

/**
 * Basic console implementation of a user interface.
 */
public class ConsoleInterface implements UserInterface {

    private int min;
    private int max;
    private float invP;
    private String task;
    private int lastP;

    public ConsoleInterface() {
    }

    @Override
    public void print(Module m, PrintLevel level, String s) {
        System.err.println(UI.formatOutput(m, level, s));
    }

    @Override
    public void taskStart(String s, int min, int max) {
        task = s;
        this.min = min;
        this.max = max;
        lastP = -1;
        invP = 100.0f / (max - min);
    }

    @Override
    public void taskUpdate(int current) {
        int p = (min == max) ? 0 : (int) ((current - min) * invP);
        if (p != lastP) {
            System.err.print(task + " [" + (lastP = p) + "%]\r");
        }
    }

    @Override
    public void taskStop() {
        System.err.print("                                                                      \r");
    }
}