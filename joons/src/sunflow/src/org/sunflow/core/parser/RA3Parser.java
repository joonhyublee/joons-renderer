package org.sunflow.core.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sunflow.SunflowAPIInterface;
import org.sunflow.core.SceneParser;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;

public class RA3Parser implements SceneParser {

    @Override
    public boolean parse(String filename, SunflowAPIInterface api) {
        try {
            UI.printInfo(Module.USER, "RA3 - Reading geometry: \"%s\" ...", filename);
            File file = new File(filename);
            FileInputStream stream = new FileInputStream(filename);
            MappedByteBuffer map = stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            map.order(ByteOrder.LITTLE_ENDIAN);
            IntBuffer ints = map.asIntBuffer();
            FloatBuffer buffer = map.asFloatBuffer();
            int numVerts = ints.get(0);
            int numTris = ints.get(1);
            UI.printInfo(Module.USER, "RA3 -   * Reading %d vertices ...", numVerts);
            float[] verts = new float[3 * numVerts];
            for (int i = 0; i < verts.length; i++) {
                verts[i] = buffer.get(2 + i);
            }
            UI.printInfo(Module.USER, "RA3 -   * Reading %d triangles ...", numTris);
            int[] tris = new int[3 * numTris];
            for (int i = 0; i < tris.length; i++) {
                tris[i] = ints.get(2 + verts.length + i);
            }
            stream.close();
            UI.printInfo(Module.USER, "RA3 -   * Creating mesh ...");

            // create geometry
            api.parameter("triangles", tris);
            api.parameter("points", "point", "vertex", verts);
            api.geometry(filename, "triangle_mesh");

            // create default shader (this will simply error out if the shader
            // already exists)
            api.shader("ra3shader", "simple");
            // create instance
            api.parameter("shaders", "ra3shader");
            api.instance(filename + ".instance", filename);
        } catch (FileNotFoundException e) {
            Logger.getLogger(RA3Parser.class.getName()).log(Level.SEVERE, null, e);
            return false;
        } catch (IOException e) {
            Logger.getLogger(RA3Parser.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }
        return true;
    }
}