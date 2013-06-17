package joons;

import java.util.ArrayList;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PMatrix3D;

public class JRStatics {
	//This class contains all the shared variables and constants,
	//and also the default values for them.
	
	//sys constants
	public static final String JR_VERSION = "v0.98b";
	public static final String UNRENDERED_FILE_NAME = "captured.png";
	public static final String RENDERED_FILE_NAME = "rendered_uninv.png";
	public static final String RENDERED_INV_FILE_NAME = "rendered_inv.png";
	
	//sys variables, default unless modified 
	public static PApplet P;
	public static float FOV, ASPECT;
	public static PImage IMG_RENDERED_INV;
	public static ArrayList<JRFiller> fillers;
	public static boolean FILLERS_ARE_VALID;
	public static boolean CORNELL_BOX_IS_CALLED = false;
	public static boolean GI_IS_CALLED = false;
	public static boolean GI_AMB_OCC_IS_CALLED = false;
	
	//user interface keys
	public static final String IPR = "ipr";
	public static final String BUCKET = "bucket";
	public static final String SRGB_NONLINEAR = "sRGB nonlinear";
	public static final String GI_AMB_OCC = "gi_ambient_occlusion"; // "ambocc" is the proper sunflow parameter.
	public static final String GI_INSTANT = "gi_instant"; //"igi" is the proper sunflow parameter.
	public static final String CORNELL_BOX = "cornell_box";	
	public static final String CONSTANT = "constant";
	public static final String DIFFUSE = "diffuse";
	public static final String SHINY = "shiny"; //"shiny_diffuse" is the proper sunflow parameter.
	public static final String MIRROR = "mirror";
	public static final String GLASS = "glass";
	public static final String PHONG = "phong";
	public static final String AMBIENT_OCCLUSION = "ambient_occlusion";
	public static final String LIGHT = "light";

	//sunflow image settings variables, default unless modified
	public static double SIZE_MULTIPLIER = 1;
	public static String SAMPLER = BUCKET;
	public static int AA_MIN = -2;
	public static int AA_MAX = 0;
	public static int AA_SAMPLES = 1;
	public static int CAUSTICS_EMIT = 1000000;
	public static int CAUSTICS_GATHER = 100;
	public static float CAUSTICS_RADIUS = 0.5f;
	public static int TRACE_DEPTH_DIFF = 1;
	public static int TRACE_DEPTH_REFL = 4;
	public static int TRACE_DEPTH_REFR = 4;
	public static float FOCUS_DISTANCE = -1; //uninitialized -1
	public static float LENS_RADIUS = 1f;
	public static int LENS_SIDES = -1; //uninitialized -1
	public static float LENS_ROTATION = -1; //uninitialized -1
	
	//sunflow GI instant variables
	public static int GI_INSTANT_SAMPLES = 16;
	public static int GI_INSTANT_SETS = 1;
	public static float GI_INSTANT_C = 0.00003f;
	public static int GI_INSTANT_BIAS_SAMPLES = 0;
	
	//sunflow GI ambient occlusion variables 
	public static float GI_AMB_OCC_BRIGHT_R = 0.5f;
	public static float GI_AMB_OCC_BRIGHT_G = 0.5f;
	public static float GI_AMB_OCC_BRIGHT_B = 0.5f;
	public static float GI_AMB_OCC_DARK_R = 0;
	public static float GI_AMB_OCC_DARK_G = 0;
	public static float GI_AMB_OCC_DARK_B = 0;
	public static float GI_AMB_OCC_MAX_DIST = 100;
	public static int GI_AMB_OCC_SAMPLES = 32;
	
	//background primitive variables
	public static float BG_R = 0.7f;
	public static float BG_G = 0.7f;
	public static float BG_B = 0.7f;
	
	//final default values
	public static final float DEF_RADIANCE = 5;
	public static final float DEF_RGB = 255;
	public static final float DEF_GLASS_ALPHA = 150;
	public static final int DEF_SAMPLES = 16;
	public static final float DEF_CORB_RADIANCE = 15;
	public static final float DEF_CORB_COLOR_1 = 220;
	public static final float DEF_CORB_COLOR_2 = 130;
	public static final float DEF_AMB_OCC_MAX_DIST = 50;
	
	
	//static methods
	public static JRFiller getCurrentFiller() {
		return fillers.get(fillers.size() - 1);
	}
	
	public static void initializeFillers() {
		fillers = new ArrayList<JRFiller>();
		//Default shader that kicks in when no shader has been declared.
		fillers.add(new JRFiller(DIFFUSE, DEF_RGB, DEF_RGB, DEF_RGB));
	}
	
	public static float[] applyTransform(float x, float y, float z){
		PMatrix3D tr = (PMatrix3D) P.getMatrix();
		float tx = x*tr.m00 + y*tr.m01 + z*tr.m02 + tr.m03;
		float ty = x*tr.m10 + y*tr.m11 + z*tr.m12 + tr.m13;
		float tz = x*tr.m20 + y*tr.m21 + z*tr.m22 + tr.m23;		
		return new float[] {tx, ty, tz};
	}
	
	//error message strings
	public static final String JR_VERSION_PRINT =
			"Joons-Renderer : "+JR_VERSION+".";
	
	public static final String IMAGE_SAMPLER_ERROR =
			"Joons-Renderer : ERROR, Unknown sampler type. Use either \"ipr\" or \"bucket\" with setSampler() in setup().";
	
	public static final String IMAGE_AA_ERROR = 
			"Joons-Renderer : ERROR, aaMax must be equal to or greater than aaMin. Both must be within -2 ~ +2.\n" +
			"Joons-Renderer : Use setAA(int aaMin, int aaMax), or\n" +
			"Joons-Renderer :     setAA(int aaMin, int aaMax, int aaSamples).";
	
	public static final String GI_INSTANT_ERROR = 
			"Joons-Renderer : ERROR, background type \"gi_instant\" must have 0 or 4 parameters.\n" +
			"Joons-Renderer : int samples, int sets, float b, float biasSamples.";
	
	public static final String GI_AMB_OCC_ERROR = 
			"Joons-Renderer : ERROR, background type \"gi_ambient_occlusion\" must have 0 or 8 parameters.\n" +
			"Joons-Renderer : float brightR, float brightG, float brightB,\n" +
			"Joons-Renderer : float darkR,   float darkG,   float darkB, float maximumDistance, int samples.";
	
	public static final String FILLER_UNKOWN_ERROR = 
			"Joons-Renderer : ERROR, Unknown fill type.\n"+
			"Joons-Renderer : Choose from \"constant\", \"diffuse\", \"shiny\", \"mirror\", \"glass\", \"phong\",\n" +
			"Joons-Renderer : \"ambient_occlusion\" and \"light\".";
	
	public static final String FILLER_LIGHT_ERROR = 
			"Joons-Renderer : ERROR, fill type \"light\" must have 0, 3 or 4 parameters.\n" + 
			"Joons-Renderer : float radianceR, float radianceG, float radianceB, or\n"+
			"Joons-Renderer : float radianceR, float radianceG, floar radianceB, samples.";
	
	public static final String FILLER_CONSTANT_ERROR = 
			"Joons-Renderer : ERROR, fill type \"constant\" must have 0 or 3 parameters.\n" + 
			"Joons-Renderer : float R, float G, float B.";
	
	public static final String FILLER_DIFFUSE_ERROR = 
			"Joons-Renderer : ERROR, fill type \"diffuse\" must have 0 or 3 parameters.\n" + 
			"Joons-Renderer : float R, float G, float B.";
	
	public static final String FILLER_SHINY_ERROR = 
			"Joons-Renderer : ERROR, fill type \"shiny\" must have 0, 3 or 4 parameters.\n" + 
			"Joons-Renderer : float R, float G, float B, or\n" +
			"Joons-Renderer : float R, float G, float B, float shininess.";
	
	public static final String FILLER_MIRROR_ERROR = 
			"Joons-Renderer : ERROR, fill type \"mirror\" must have 0 or 3 parameters.\n" + 
			"Joons-Renderer : float R, float G, float B.";
	
	public static final String FILLER_GLASS_ERROR =
			"Joons-Renderer : ERROR, fill type \"glass\" must have 0, 3, 4 or 8 parameters.\n" +
			"Joons-Renderer : float R, float G, float B, or\n" +
			"Joons-Renderer : float R, float G, float B, float indexOfRefraction, or\n" +
			"Joons-Renderer : float R, float G, float B, float indexOfRefraction, float absorptionDistance,\n" +
			"Joons-Renderer : float absorptionR, float absorptionG, float absorptionB.";
	
	public static final String FILLER_PHONG_ERROR =
			"Joons-Renderer : ERROR, fill type \"phong\" must have 0, 3, 6 or 8 parameters.\n" +
			"Joons-Renderer : float diffuseR,  float diffuseG,  float diffuseB, or\n" +
			"Joons-Renderer : float diffuseR,  float diffuseG,  float diffuseB,\n" +
			"Joons-Renderer : float specularR, float specularG, float specularB, or\n" +
			"Joons-Renderer : float diffuseR,  float diffuseG,  float diffuseB,\n" + 
			"Joons-Renderer : float specularR, float specularG, float specularB,\n" +
			"Joons-Renderer : float specularityHardness, float reflectionBluriness.";
	
	public static final String FILLER_AMB_OCC_ERROR = 
			"Joons-Renderer : ERROR, fill type \"ambient_occlusion\" must have 0, 3 or 8 parameters.\n" +
			"Joons-Renderer : float brightR, float brightG, float brightB, or\n" +
			"Joons-Renderer : float brightR, float brightG, float brightB,\n" +
			"Joons-Renderer : float darkR,   float darkG,   float darkB, float maximumDistance, float samples.";
}
