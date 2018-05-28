package org.sunflow.core.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.SceneParser;
import org.sunflow.system.Parser;
import org.sunflow.system.UI;
import org.sunflow.system.Parser.ParserException;
import org.sunflow.system.UI.Module;
import org.sunflow.util.FloatArray;
import org.sunflow.util.IntArray;

public class ShaveRibParser implements SceneParser {

    @Override
    public boolean parse(String filename, SunflowAPIInterface api) {
        try {
            Parser p = new Parser(filename);
            p.checkNextToken("version");
            p.checkNextToken("3.04");
            p.checkNextToken("TransformBegin");

            if (p.peekNextToken("Procedural")) {
                // read procedural shave rib
                boolean done = false;
                while (!done) {
                    p.checkNextToken("DelayedReadArchive");
                    p.checkNextToken("[");
                    String f = p.getNextToken();
                    UI.printInfo(Module.USER, "RIB - Reading voxel: \"%s\" ...", f);
                    api.include(f);
                    p.checkNextToken("]");
                    while (true) {
                        String t = p.getNextToken();
                        if (t == null || t.equals("TransformEnd")) {
                            done = true;
                            break;
                        } else if (t.equals("Procedural")) {
                            break;
                        }
                    }
                }
                return true;
            }

            boolean cubic = false;
            if (p.peekNextToken("Basis")) {
                cubic = true;
                // u basis
                p.checkNextToken("catmull-rom");
                p.checkNextToken("1");
                // v basis
                p.checkNextToken("catmull-rom");
                p.checkNextToken("1");
            }
            while (p.peekNextToken("Declare")) {
                p.getNextToken(); // name
                p.getNextToken(); // interpolation & type
            }
            int index = 0;
            boolean done = false;
            p.checkNextToken("Curves");
            do {
                if (cubic) {
                    p.checkNextToken("cubic");
                } else {
                    p.checkNextToken("linear");
                }
                int[] nverts = parseIntArray(p);
                for (int i = 1; i < nverts.length; i++) {
                    if (nverts[0] != nverts[i]) {
                        UI.printError(Module.USER, "RIB - Found variable number of hair segments");
                        return false;
                    }
                }
                int nhairs = nverts.length;

                UI.printInfo(Module.USER, "RIB - Parsed %d hair curves", nhairs);

                api.parameter("segments", nverts[0] - 1);

                p.checkNextToken("nonperiodic");
                p.checkNextToken("P");
                float[] points = parseFloatArray(p);
                if (points.length != 3 * nhairs * nverts[0]) {
                    UI.printError(Module.USER, "RIB - Invalid number of points - expecting %d - found %d", nhairs * nverts[0], points.length / 3);
                    return false;
                }
                api.parameter("points", "point", "vertex", points);

                UI.printInfo(Module.USER, "RIB - Parsed %d hair vertices", points.length / 3);

                p.checkNextToken("width");
                float[] w = parseFloatArray(p);
                if (w.length != nhairs * nverts[0]) {
                    UI.printError(Module.USER, "RIB - Invalid number of hair widths - expecting %d - found %d", nhairs * nverts[0], w.length);
                    return false;
                }
                api.parameter("widths", "float", "vertex", w);

                UI.printInfo(Module.USER, "RIB - Parsed %d hair widths", w.length);

                String name = String.format("%s[%d]", filename, index);
                UI.printInfo(Module.USER, "RIB - Creating hair object \"%s\"", name);
                api.geometry(name, "hair");
                api.instance(name + ".instance", name);

                UI.printInfo(Module.USER, "RIB - Searching for next curve group ...");
                while (true) {
                    String t = p.getNextToken();
                    if (t == null || t.equals("TransformEnd")) {
                        done = true;
                        break;
                    } else if (t.equals("Curves")) {
                        break;
                    }
                }
                index++;
            } while (!done);
            UI.printInfo(Module.USER, "RIB - Finished reading rib file");
        } catch (FileNotFoundException exp) {
            UI.printError(Module.USER, "RIB - File not found: %s", filename);
            Logger.getLogger(ShaveRibParser.class.getName()).log(Level.SEVERE, null, exp);
            return false;
        } catch (ParserException exp) {
            UI.printError(Module.USER, "RIB - Parser exception: %s", exp);
            Logger.getLogger(ShaveRibParser.class.getName()).log(Level.SEVERE, null, exp);
            return false;
        } catch (IOException exp) {
            UI.printError(Module.USER, "RIB - I/O exception: %s", exp);
            Logger.getLogger(ShaveRibParser.class.getName()).log(Level.SEVERE, null, exp);
            return false;
        }
        return true;
    }

    private int[] parseIntArray(Parser p) throws IOException {
        IntArray array = new IntArray();
        boolean done = false;
        do {
            String s = p.getNextToken();
            if (s.startsWith("[")) {
                s = s.substring(1);
            }
            if (s.endsWith("]")) {
                s = s.substring(0, s.length() - 1);
                done = true;
            }
            array.add(Integer.parseInt(s));
        } while (!done);
        return array.trim();
    }

    private float[] parseFloatArray(Parser p) throws IOException {
        FloatArray array = new FloatArray();
        boolean done = false;
        do {
            String s = p.getNextToken();
            if (s.startsWith("[")) {
                s = s.substring(1);
            }
            if (s.endsWith("]")) {
                s = s.substring(0, s.length() - 1);
                done = true;
            }
            array.add(Float.parseFloat(s));
        } while (!done);
        return array.trim();
    }
}