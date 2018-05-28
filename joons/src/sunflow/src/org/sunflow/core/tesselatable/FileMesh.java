package org.sunflow.core.tesselatable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sunflow.SunflowAPI;
import org.sunflow.core.ParameterList;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.Tesselatable;
import org.sunflow.core.ParameterList.InterpolationType;
import org.sunflow.core.primitive.TriangleMesh;
import org.sunflow.math.BoundingBox;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.Memory;
import org.sunflow.system.UI;
import org.sunflow.system.UI.Module;
import org.sunflow.util.FloatArray;
import org.sunflow.util.IntArray;

public class FileMesh implements Tesselatable {

    private String filename = null;
    private boolean smoothNormals = false;

    public BoundingBox getWorldBounds(Matrix4 o2w) {
        // world bounds can't be computed without reading file
        // return null so the mesh will be loaded right away
        return null;
    }

    public PrimitiveList tesselate() {
        if (filename.endsWith(".ra3")) {
            try {
                UI.printInfo(Module.GEOM, "RA3 - Reading geometry: \"%s\" ...", filename);
                File file = new File(filename);
                FileInputStream stream = new FileInputStream(filename);
                MappedByteBuffer map = stream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                map.order(ByteOrder.LITTLE_ENDIAN);
                IntBuffer ints = map.asIntBuffer();
                FloatBuffer buffer = map.asFloatBuffer();
                int numVerts = ints.get(0);
                int numTris = ints.get(1);
                UI.printInfo(Module.GEOM, "RA3 -   * Reading %d vertices ...", numVerts);
                float[] verts = new float[3 * numVerts];
                for (int i = 0; i < verts.length; i++) {
                    verts[i] = buffer.get(2 + i);
                }
                UI.printInfo(Module.GEOM, "RA3 -   * Reading %d triangles ...", numTris);
                int[] tris = new int[3 * numTris];
                for (int i = 0; i < tris.length; i++) {
                    tris[i] = ints.get(2 + verts.length + i);
                }
                stream.close();
                UI.printInfo(Module.GEOM, "RA3 -   * Creating mesh ...");
                return generate(tris, verts, smoothNormals);
            } catch (FileNotFoundException e) {
                Logger.getLogger(FileMesh.class.getName()).log(Level.SEVERE, null, e);
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - file not found", filename);
            } catch (IOException e) {
                Logger.getLogger(FileMesh.class.getName()).log(Level.SEVERE, null, e);
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - I/O error occured", filename);
            }
        } else if (filename.endsWith(".obj")) {
            int lineNumber = 1;
            try {
                UI.printInfo(Module.GEOM, "OBJ - Reading geometry: \"%s\" ...", filename);
                FloatArray verts = new FloatArray();
                IntArray tris = new IntArray();
                FileReader file = new FileReader(filename);
                BufferedReader bf = new BufferedReader(file);
                String line;
                while ((line = bf.readLine()) != null) {
                    if (line.startsWith("v")) {
                        String[] v = line.split("\\s+");
                        verts.add(Float.parseFloat(v[1]));
                        verts.add(Float.parseFloat(v[2]));
                        verts.add(Float.parseFloat(v[3]));
                    } else if (line.startsWith("f")) {
                        String[] f = line.split("\\s+");
                        if (f.length == 5) {
                            tris.add(Integer.parseInt(f[1]) - 1);
                            tris.add(Integer.parseInt(f[2]) - 1);
                            tris.add(Integer.parseInt(f[3]) - 1);
                            tris.add(Integer.parseInt(f[1]) - 1);
                            tris.add(Integer.parseInt(f[3]) - 1);
                            tris.add(Integer.parseInt(f[4]) - 1);
                        } else if (f.length == 4) {
                            tris.add(Integer.parseInt(f[1]) - 1);
                            tris.add(Integer.parseInt(f[2]) - 1);
                            tris.add(Integer.parseInt(f[3]) - 1);
                        }
                    }
                    if (lineNumber % 100000 == 0) {
                        UI.printInfo(Module.GEOM, "OBJ -   * Parsed %7d lines ...", lineNumber);
                    }
                    lineNumber++;
                }
                file.close();
                UI.printInfo(Module.GEOM, "OBJ -   * Creating mesh ...");
                return generate(tris.trim(), verts.trim(), smoothNormals);
            } catch (FileNotFoundException e) {
                Logger.getLogger(FileMesh.class.getName()).log(Level.SEVERE, null, e);
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - file not found", filename);
            } catch (NumberFormatException e) {
                Logger.getLogger(FileMesh.class.getName()).log(Level.SEVERE, null, e);
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - syntax error at line %d", lineNumber);
            } catch (IOException e) {
                Logger.getLogger(FileMesh.class.getName()).log(Level.SEVERE, null, e);
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - I/O error occured", filename);
            }
        } else if (filename.endsWith(".stl")) {
            try {
                UI.printInfo(Module.GEOM, "STL - Reading geometry: \"%s\" ...", filename);
                FileInputStream file = new FileInputStream(filename);
                DataInputStream stream = new DataInputStream(new BufferedInputStream(file));
                file.skip(80);
                int numTris = getLittleEndianInt(stream.readInt());
                UI.printInfo(Module.GEOM, "STL -   * Reading %d triangles ...", numTris);
                long filesize = new File(filename).length();
                if (filesize != (84 + 50 * numTris)) {
                    UI.printWarning(Module.GEOM, "STL - Size of file mismatch (expecting %s, found %s)", Memory.bytesToString(84 + 14 * numTris), Memory.bytesToString(filesize));
                    return null;
                }
                int[] tris = new int[3 * numTris];
                float[] verts = new float[9 * numTris];
                for (int i = 0, i3 = 0, index = 0; i < numTris; i++, i3 += 3) {
                    // skip normal
                    stream.readInt();
                    stream.readInt();
                    stream.readInt();
                    for (int j = 0; j < 3; j++, index += 3) {
                        tris[i3 + j] = i3 + j;
                        // get xyz
                        verts[index + 0] = getLittleEndianFloat(stream.readInt());
                        verts[index + 1] = getLittleEndianFloat(stream.readInt());
                        verts[index + 2] = getLittleEndianFloat(stream.readInt());
                    }
                    stream.readShort();
                    if ((i + 1) % 100000 == 0) {
                        UI.printInfo(Module.GEOM, "STL -   * Parsed %7d triangles ...", i + 1);
                    }
                }
                file.close();
                // create geometry
                UI.printInfo(Module.GEOM, "STL -   * Creating mesh ...");
                if (smoothNormals) {
                    UI.printWarning(Module.GEOM, "STL - format does not support shared vertices - normal smoothing disabled");
                }
                return generate(tris, verts, false);
            } catch (FileNotFoundException e) {
                Logger.getLogger(FileMesh.class.getName()).log(Level.SEVERE, null, e);
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - file not found", filename);
            } catch (IOException e) {
                Logger.getLogger(FileMesh.class.getName()).log(Level.SEVERE, null, e);
                UI.printError(Module.GEOM, "Unable to read mesh file \"%s\" - I/O error occured", filename);
            }
        } else {
            UI.printWarning(Module.GEOM, "Unable to read mesh file \"%s\" - unrecognized format", filename);
        }
        return null;
    }

    private TriangleMesh generate(int[] tris, float[] verts, boolean smoothNormals) {
        ParameterList pl = new ParameterList();
        pl.addIntegerArray("triangles", tris);
        pl.addPoints("points", InterpolationType.VERTEX, verts);
        if (smoothNormals) {
            float[] normals = new float[verts.length]; // filled with 0's
            Point3 p0 = new Point3();
            Point3 p1 = new Point3();
            Point3 p2 = new Point3();
            Vector3 n = new Vector3();
            for (int i3 = 0; i3 < tris.length; i3 += 3) {
                int v0 = tris[i3 + 0];
                int v1 = tris[i3 + 1];
                int v2 = tris[i3 + 2];
                p0.set(verts[3 * v0 + 0], verts[3 * v0 + 1], verts[3 * v0 + 2]);
                p1.set(verts[3 * v1 + 0], verts[3 * v1 + 1], verts[3 * v1 + 2]);
                p2.set(verts[3 * v2 + 0], verts[3 * v2 + 1], verts[3 * v2 + 2]);
                Point3.normal(p0, p1, p2, n); // compute normal
                // add face normal to each vertex
                // note that these are not normalized so this in fact weights
                // each normal by the area of the triangle
                normals[3 * v0 + 0] += n.x;
                normals[3 * v0 + 1] += n.y;
                normals[3 * v0 + 2] += n.z;
                normals[3 * v1 + 0] += n.x;
                normals[3 * v1 + 1] += n.y;
                normals[3 * v1 + 2] += n.z;
                normals[3 * v2 + 0] += n.x;
                normals[3 * v2 + 1] += n.y;
                normals[3 * v2 + 2] += n.z;
            }
            // normalize all the vectors
            for (int i3 = 0; i3 < normals.length; i3 += 3) {
                n.set(normals[i3 + 0], normals[i3 + 1], normals[i3 + 2]);
                n.normalize();
                normals[i3 + 0] = n.x;
                normals[i3 + 1] = n.y;
                normals[i3 + 2] = n.z;
            }
            pl.addVectors("normals", InterpolationType.VERTEX, normals);
        }
        TriangleMesh m = new TriangleMesh();
        if (m.update(pl, null)) {
            return m;
        }
        // something failed in creating the mesh, the error message will be
        // printed by the mesh itself - no need to repeat it here
        return null;
    }

    public boolean update(ParameterList pl, SunflowAPI api) {
        String file = pl.getString("filename", null);
        if (file != null) {
            filename = api.resolveIncludeFilename(file);
        }
        smoothNormals = pl.getBoolean("smooth_normals", smoothNormals);
        return filename != null;
    }

    private int getLittleEndianInt(int i) {
        // input integer has its bytes in big endian byte order
        // swap them around
        return (i >>> 24) | ((i >>> 8) & 0xFF00) | ((i << 8) & 0xFF0000) | (i << 24);
    }

    private float getLittleEndianFloat(int i) {
        // input integer has its bytes in big endian byte order
        // swap them around and interpret data as floating point
        return Float.intBitsToFloat(getLittleEndianInt(i));
    }
}