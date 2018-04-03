package org.sunflow.core.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.SceneParser;
import org.sunflow.system.Parser;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class TriParser implements SceneParser {

    @Override
    public boolean parse(String filename, SunflowAPIInterface api) {
        try {
            UI.printInfo(Module.USER, "TRI - Reading geometry: \"%s\" ...", filename);
            Parser p = new Parser(filename);
            float[] verts = new float[3 * p.getNextInt()];
            for (int v = 0; v < verts.length; v += 3) {
                verts[v + 0] = p.getNextFloat();
                verts[v + 1] = p.getNextFloat();
                verts[v + 2] = p.getNextFloat();
                p.getNextToken();
                p.getNextToken();
            }
            int[] triangles = new int[p.getNextInt() * 3];
            for (int t = 0; t < triangles.length; t += 3) {
                triangles[t + 0] = p.getNextInt();
                triangles[t + 1] = p.getNextInt();
                triangles[t + 2] = p.getNextInt();
            }

            // create geometry
            api.parameter("triangles", triangles);
            api.parameter("points", "point", "vertex", verts);
            api.geometry(filename, "triangle_mesh");

            // create shader
            api.shader(filename + ".shader", "simple");
            api.parameter("shaders", filename + ".shader");

            // create instance
            api.instance(filename + ".instance", filename);

            p.close();
            // output to ra3 format
            RandomAccessFile stream = new RandomAccessFile(filename.replace(".tri", ".ra3"), "rw");
            MappedByteBuffer map = stream.getChannel().map(MapMode.READ_WRITE, 0, 8 + 4 * (verts.length + triangles.length));
            map.order(ByteOrder.LITTLE_ENDIAN);
            IntBuffer ints = map.asIntBuffer();
            FloatBuffer floats = map.asFloatBuffer();
            ints.put(0, verts.length / 3);
            ints.put(1, triangles.length / 3);
            for (int i = 0; i < verts.length; i++) {
                floats.put(2 + i, verts[i]);
            }
            for (int i = 0; i < triangles.length; i++) {
                ints.put(2 + verts.length + i, triangles[i]);
            }
            stream.close();
        } catch (FileNotFoundException e) {
            Logger.getLogger(TriParser.class.getName()).log(Level.SEVERE, null, e);
            return false;
        } catch (IOException e) {
            Logger.getLogger(TriParser.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }
        return true;
    }
}