package org.sunflow;

import org.sunflow.core.AccelerationStructure;
import org.sunflow.core.BucketOrder;
import org.sunflow.core.CameraLens;
import org.sunflow.core.CausticPhotonMapInterface;
import org.sunflow.core.Filter;
import org.sunflow.core.GIEngine;
import org.sunflow.core.GlobalPhotonMapInterface;
import org.sunflow.core.ImageSampler;
import org.sunflow.core.LightSource;
import org.sunflow.core.Modifier;
import org.sunflow.core.PrimitiveList;
import org.sunflow.core.SceneParser;
import org.sunflow.core.Shader;
import org.sunflow.core.Tesselatable;
import org.sunflow.core.accel.BoundingIntervalHierarchy;
import org.sunflow.core.accel.KDTree;
import org.sunflow.core.accel.NullAccelerator;
import org.sunflow.core.accel.UniformGrid;
import org.sunflow.core.bucket.ColumnBucketOrder;
import org.sunflow.core.bucket.DiagonalBucketOrder;
import org.sunflow.core.bucket.HilbertBucketOrder;
import org.sunflow.core.bucket.RandomBucketOrder;
import org.sunflow.core.bucket.RowBucketOrder;
import org.sunflow.core.bucket.SpiralBucketOrder;
import org.sunflow.core.camera.FisheyeLens;
import org.sunflow.core.camera.PinholeLens;
import org.sunflow.core.camera.SphericalLens;
import org.sunflow.core.camera.ThinLens;
import org.sunflow.core.filter.BlackmanHarrisFilter;
import org.sunflow.core.filter.BoxFilter;
import org.sunflow.core.filter.CatmullRomFilter;
import org.sunflow.core.filter.CubicBSpline;
import org.sunflow.core.filter.GaussianFilter;
import org.sunflow.core.filter.LanczosFilter;
import org.sunflow.core.filter.MitchellFilter;
import org.sunflow.core.filter.SincFilter;
import org.sunflow.core.filter.TriangleFilter;
import org.sunflow.core.gi.AmbientOcclusionGIEngine;
import org.sunflow.core.gi.FakeGIEngine;
import org.sunflow.core.gi.InstantGI;
import org.sunflow.core.gi.IrradianceCacheGIEngine;
import org.sunflow.core.gi.PathTracingGIEngine;
import org.sunflow.core.light.DirectionalSpotlight;
import org.sunflow.core.light.ImageBasedLight;
import org.sunflow.core.light.PointLight;
import org.sunflow.core.light.SphereLight;
import org.sunflow.core.light.SunSkyLight;
import org.sunflow.core.light.TriangleMeshLight;
import org.sunflow.core.modifiers.BumpMappingModifier;
import org.sunflow.core.modifiers.NormalMapModifier;
import org.sunflow.core.modifiers.PerlinModifier;
import org.sunflow.core.parser.RA2Parser;
import org.sunflow.core.parser.RA3Parser;
import org.sunflow.core.parser.SCAsciiParser;
import org.sunflow.core.parser.SCBinaryParser;
import org.sunflow.core.parser.SCParser;
import org.sunflow.core.parser.ShaveRibParser;
import org.sunflow.core.photonmap.CausticPhotonMap;
import org.sunflow.core.photonmap.GlobalPhotonMap;
import org.sunflow.core.photonmap.GridPhotonMap;
import org.sunflow.core.primitive.Background;
import org.sunflow.core.primitive.BanchoffSurface;
import org.sunflow.core.primitive.Box;
import org.sunflow.core.primitive.CornellBox;
import org.sunflow.core.primitive.Cylinder;
import org.sunflow.core.primitive.Hair;
import org.sunflow.core.primitive.JuliaFractal;
import org.sunflow.core.primitive.ParticleSurface;
import org.sunflow.core.primitive.Plane;
import org.sunflow.core.primitive.QuadMesh;
import org.sunflow.core.primitive.Sphere;
import org.sunflow.core.primitive.SphereFlake;
import org.sunflow.core.primitive.Torus;
import org.sunflow.core.primitive.TriangleMesh;
import org.sunflow.core.renderer.BucketRenderer;
import org.sunflow.core.renderer.MultipassRenderer;
import org.sunflow.core.renderer.ProgressiveRenderer;
import org.sunflow.core.renderer.SimpleRenderer;
import org.sunflow.core.shader.AmbientOcclusionShader;
import org.sunflow.core.shader.AnisotropicWardShader;
import org.sunflow.core.shader.ConstantShader;
import org.sunflow.core.shader.DiffuseShader;
import org.sunflow.core.shader.GlassShader;
import org.sunflow.core.shader.IDShader;
import org.sunflow.core.shader.MirrorShader;
import org.sunflow.core.shader.NormalShader;
import org.sunflow.core.shader.PhongShader;
import org.sunflow.core.shader.PrimIDShader;
import org.sunflow.core.shader.QuickGrayShader;
import org.sunflow.core.shader.ShinyDiffuseShader;
import org.sunflow.core.shader.SimpleShader;
import org.sunflow.core.shader.TexturedAmbientOcclusionShader;
import org.sunflow.core.shader.TexturedDiffuseShader;
import org.sunflow.core.shader.TexturedPhongShader;
import org.sunflow.core.shader.TexturedShinyDiffuseShader;
import org.sunflow.core.shader.TexturedWardShader;
import org.sunflow.core.shader.UVShader;
import org.sunflow.core.shader.UberShader;
import org.sunflow.core.shader.ViewCausticsShader;
import org.sunflow.core.shader.ViewGlobalPhotonsShader;
import org.sunflow.core.shader.ViewIrradianceShader;
import org.sunflow.core.shader.WireframeShader;
import org.sunflow.core.tesselatable.BezierMesh;
import org.sunflow.core.tesselatable.FileMesh;
import org.sunflow.core.tesselatable.Gumbo;
import org.sunflow.core.tesselatable.Teapot;
import org.sunflow.image.BitmapReader;
import org.sunflow.image.BitmapWriter;
import org.sunflow.image.readers.BMPBitmapReader;
import org.sunflow.image.readers.HDRBitmapReader;
import org.sunflow.image.readers.IGIBitmapReader;
import org.sunflow.image.readers.JPGBitmapReader;
import org.sunflow.image.readers.PNGBitmapReader;
import org.sunflow.image.readers.TGABitmapReader;
import org.sunflow.image.writers.EXRBitmapWriter;
import org.sunflow.image.writers.HDRBitmapWriter;
import org.sunflow.image.writers.IGIBitmapWriter;
import org.sunflow.image.writers.PNGBitmapWriter;
import org.sunflow.image.writers.TGABitmapWriter;
import org.sunflow.system.Plugins;

/**
 * This class acts as the central repository for all user extensible types in
 * Sunflow, even built-in types are registered here. This class is static so
 * that new plugins may be reused by an application across several render
 * scenes.
 */
public final class PluginRegistry {
    // base types - needed by SunflowAPI

    public static final Plugins<PrimitiveList> primitivePlugins = new Plugins<PrimitiveList>(PrimitiveList.class);
    public static final Plugins<Tesselatable> tesselatablePlugins = new Plugins<Tesselatable>(Tesselatable.class);
    public static final Plugins<Shader> shaderPlugins = new Plugins<Shader>(Shader.class);
    public static final Plugins<Modifier> modifierPlugins = new Plugins<Modifier>(Modifier.class);
    public static final Plugins<LightSource> lightSourcePlugins = new Plugins<LightSource>(LightSource.class);
    public static final Plugins<CameraLens> cameraLensPlugins = new Plugins<CameraLens>(CameraLens.class);
    // advanced types - used inside the Sunflow core
    public static final Plugins<AccelerationStructure> accelPlugins = new Plugins<AccelerationStructure>(AccelerationStructure.class);
    public static final Plugins<BucketOrder> bucketOrderPlugins = new Plugins<BucketOrder>(BucketOrder.class);
    public static final Plugins<Filter> filterPlugins = new Plugins<Filter>(Filter.class);
    public static final Plugins<GIEngine> giEnginePlugins = new Plugins<GIEngine>(GIEngine.class);
    public static final Plugins<CausticPhotonMapInterface> causticPhotonMapPlugins = new Plugins<CausticPhotonMapInterface>(CausticPhotonMapInterface.class);
    public static final Plugins<GlobalPhotonMapInterface> globalPhotonMapPlugins = new Plugins<GlobalPhotonMapInterface>(GlobalPhotonMapInterface.class);
    public static final Plugins<ImageSampler> imageSamplerPlugins = new Plugins<ImageSampler>(ImageSampler.class);
    public static final Plugins<SceneParser> parserPlugins = new Plugins<SceneParser>(SceneParser.class);
    public static final Plugins<BitmapReader> bitmapReaderPlugins = new Plugins<BitmapReader>(BitmapReader.class);
    public static final Plugins<BitmapWriter> bitmapWriterPlugins = new Plugins<BitmapWriter>(BitmapWriter.class);

    // Register all plugins on startup:
    static {
        // primitives
        primitivePlugins.registerPlugin("triangle_mesh", TriangleMesh.class);
        primitivePlugins.registerPlugin("sphere", Sphere.class);
        primitivePlugins.registerPlugin("cylinder", Cylinder.class);
        primitivePlugins.registerPlugin("box", Box.class);
        primitivePlugins.registerPlugin("banchoff", BanchoffSurface.class);
        primitivePlugins.registerPlugin("hair", Hair.class);
        primitivePlugins.registerPlugin("julia", JuliaFractal.class);
        primitivePlugins.registerPlugin("particles", ParticleSurface.class);
        primitivePlugins.registerPlugin("plane", Plane.class);
        primitivePlugins.registerPlugin("quad_mesh", QuadMesh.class);
        primitivePlugins.registerPlugin("torus", Torus.class);
        primitivePlugins.registerPlugin("background", Background.class);
        primitivePlugins.registerPlugin("sphereflake", SphereFlake.class);
    }

    static {
        // tesslatable
        tesselatablePlugins.registerPlugin("bezier_mesh", BezierMesh.class);
        tesselatablePlugins.registerPlugin("file_mesh", FileMesh.class);
        tesselatablePlugins.registerPlugin("gumbo", Gumbo.class);
        tesselatablePlugins.registerPlugin("teapot", Teapot.class);
    }

    static {
        // shaders
        shaderPlugins.registerPlugin("ambient_occlusion", AmbientOcclusionShader.class);
        shaderPlugins.registerPlugin("constant", ConstantShader.class);
        shaderPlugins.registerPlugin("diffuse", DiffuseShader.class);
        shaderPlugins.registerPlugin("glass", GlassShader.class);
        shaderPlugins.registerPlugin("mirror", MirrorShader.class);
        shaderPlugins.registerPlugin("phong", PhongShader.class);
        shaderPlugins.registerPlugin("shiny_diffuse", ShinyDiffuseShader.class);
        shaderPlugins.registerPlugin("uber", UberShader.class);
        shaderPlugins.registerPlugin("ward", AnisotropicWardShader.class);
        shaderPlugins.registerPlugin("wireframe", WireframeShader.class);

        // textured shaders
        shaderPlugins.registerPlugin("textured_ambient_occlusion", TexturedAmbientOcclusionShader.class);
        shaderPlugins.registerPlugin("textured_diffuse", TexturedDiffuseShader.class);
        shaderPlugins.registerPlugin("textured_phong", TexturedPhongShader.class);
        shaderPlugins.registerPlugin("textured_shiny_diffuse", TexturedShinyDiffuseShader.class);
        shaderPlugins.registerPlugin("textured_ward", TexturedWardShader.class);

        // preview shaders
        shaderPlugins.registerPlugin("quick_gray", QuickGrayShader.class);
        shaderPlugins.registerPlugin("simple", SimpleShader.class);
        shaderPlugins.registerPlugin("show_normals", NormalShader.class);
        shaderPlugins.registerPlugin("show_uvs", UVShader.class);
        shaderPlugins.registerPlugin("show_instance_id", IDShader.class);
        shaderPlugins.registerPlugin("show_primitive_id", PrimIDShader.class);
        shaderPlugins.registerPlugin("view_caustics", ViewCausticsShader.class);
        shaderPlugins.registerPlugin("view_global", ViewGlobalPhotonsShader.class);
        shaderPlugins.registerPlugin("view_irradiance", ViewIrradianceShader.class);
    }

    static {
        // modifiers
        modifierPlugins.registerPlugin("bump_map", BumpMappingModifier.class);
        modifierPlugins.registerPlugin("normal_map", NormalMapModifier.class);
        modifierPlugins.registerPlugin("perlin", PerlinModifier.class);
    }

    static {
        // light sources
        lightSourcePlugins.registerPlugin("directional", DirectionalSpotlight.class);
        lightSourcePlugins.registerPlugin("ibl", ImageBasedLight.class);
        lightSourcePlugins.registerPlugin("point", PointLight.class);
        lightSourcePlugins.registerPlugin("sphere", SphereLight.class);
        lightSourcePlugins.registerPlugin("sunsky", SunSkyLight.class);
        lightSourcePlugins.registerPlugin("triangle_mesh", TriangleMeshLight.class);
        lightSourcePlugins.registerPlugin("cornell_box", CornellBox.class);
    }

    static {
        // camera lenses
        cameraLensPlugins.registerPlugin("pinhole", PinholeLens.class);
        cameraLensPlugins.registerPlugin("thinlens", ThinLens.class);
        cameraLensPlugins.registerPlugin("fisheye", FisheyeLens.class);
        cameraLensPlugins.registerPlugin("spherical", SphericalLens.class);
    }

    static {
        // accels
        accelPlugins.registerPlugin("bih", BoundingIntervalHierarchy.class);
        accelPlugins.registerPlugin("kdtree", KDTree.class);
        accelPlugins.registerPlugin("null", NullAccelerator.class);
        accelPlugins.registerPlugin("uniformgrid", UniformGrid.class);
    }

    static {
        // bucket orders
        bucketOrderPlugins.registerPlugin("column", ColumnBucketOrder.class);
        bucketOrderPlugins.registerPlugin("diagonal", DiagonalBucketOrder.class);
        bucketOrderPlugins.registerPlugin("hilbert", HilbertBucketOrder.class);
        bucketOrderPlugins.registerPlugin("random", RandomBucketOrder.class);
        bucketOrderPlugins.registerPlugin("row", RowBucketOrder.class);
        bucketOrderPlugins.registerPlugin("spiral", SpiralBucketOrder.class);
    }

    static {
        // filters
        filterPlugins.registerPlugin("blackman-harris", BlackmanHarrisFilter.class);
        filterPlugins.registerPlugin("box", BoxFilter.class);
        filterPlugins.registerPlugin("catmull-rom", CatmullRomFilter.class);
        filterPlugins.registerPlugin("gaussian", GaussianFilter.class);
        filterPlugins.registerPlugin("lanczos", LanczosFilter.class);
        filterPlugins.registerPlugin("mitchell", MitchellFilter.class);
        filterPlugins.registerPlugin("sinc", SincFilter.class);
        filterPlugins.registerPlugin("triangle", TriangleFilter.class);
        filterPlugins.registerPlugin("bspline", CubicBSpline.class);
    }

    static {
        // gi engines
        giEnginePlugins.registerPlugin("ambocc", AmbientOcclusionGIEngine.class);
        giEnginePlugins.registerPlugin("fake", FakeGIEngine.class);
        giEnginePlugins.registerPlugin("igi", InstantGI.class);
        giEnginePlugins.registerPlugin("irr-cache", IrradianceCacheGIEngine.class);
        giEnginePlugins.registerPlugin("path", PathTracingGIEngine.class);
    }

    static {
        // caustic photon maps
        causticPhotonMapPlugins.registerPlugin("kd", CausticPhotonMap.class);
    }

    static {
        // global photon maps
        globalPhotonMapPlugins.registerPlugin("grid", GridPhotonMap.class);
        globalPhotonMapPlugins.registerPlugin("kd", GlobalPhotonMap.class);
    }

    static {
        // image samplers
        imageSamplerPlugins.registerPlugin("bucket", BucketRenderer.class);
        imageSamplerPlugins.registerPlugin("ipr", ProgressiveRenderer.class);
        imageSamplerPlugins.registerPlugin("fast", SimpleRenderer.class);
        imageSamplerPlugins.registerPlugin("multipass", MultipassRenderer.class);
    }

    static {
        // parsers
        parserPlugins.registerPlugin("sc", SCParser.class);
        parserPlugins.registerPlugin("sca", SCAsciiParser.class);
        parserPlugins.registerPlugin("scb", SCBinaryParser.class);
        parserPlugins.registerPlugin("rib", ShaveRibParser.class);
        parserPlugins.registerPlugin("ra2", RA2Parser.class);
        parserPlugins.registerPlugin("ra3", RA3Parser.class);
    }

    static {
        // bitmap readers
        bitmapReaderPlugins.registerPlugin("hdr", HDRBitmapReader.class);
        bitmapReaderPlugins.registerPlugin("tga", TGABitmapReader.class);
        bitmapReaderPlugins.registerPlugin("png", PNGBitmapReader.class);
        bitmapReaderPlugins.registerPlugin("jpg", JPGBitmapReader.class);
        bitmapReaderPlugins.registerPlugin("bmp", BMPBitmapReader.class);
        bitmapReaderPlugins.registerPlugin("igi", IGIBitmapReader.class);
    }

    static {
        // bitmap writers
        bitmapWriterPlugins.registerPlugin("png", PNGBitmapWriter.class);
        bitmapWriterPlugins.registerPlugin("hdr", HDRBitmapWriter.class);
        bitmapWriterPlugins.registerPlugin("tga", TGABitmapWriter.class);
        bitmapWriterPlugins.registerPlugin("exr", EXRBitmapWriter.class);
        bitmapWriterPlugins.registerPlugin("igi", IGIBitmapWriter.class);
    }
}