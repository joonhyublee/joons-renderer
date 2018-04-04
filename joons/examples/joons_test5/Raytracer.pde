import joons.JoonsRenderer;
import java.io.File;

JoonsRenderer jr;

String fileName = "frame";
String fileType= "png";
int frameCounter = 0;
int frameCounterMax = 2;

//camera declarations
float eyeX = 0;
float eyeY = 0;
float eyeZ = 0;
float centerX = 0;
float centerY = 0;
float centerZ = -1;
float upX = 0;
float upY = 1;
float upZ = 0;
float fov = PI / 4; 
float aspect = 4/3f;  
float zNear = 5;
float zFar = 10000;

void joonsSetup() {
  jr = new JoonsRenderer(this);
  jr.setSampler("ipr"); //Rendering mode, either "ipr" or "bucket".
  jr.setSizeMultiplier(1); //Set size of the .PNG file as a multiple of the Processing sketch size.
  jr.setAA(-2, 0, 1); //Set anti-aliasing, (min, max, samples). -2 < min, max < 2, samples = 1,2,3,4..
  jr.setCaustics(1); //Set caustics. 1 ~ 100. affects quality of light scattered through glass.
  //jr.setTraceDepth(1,4,4); //Set trace depth, (diffraction, reflection, refraction). Affects glass. (1,4,4) is good.
  //jr.setDOF(170, 5); //Set depth of field of camera, (focus distance, lens radius). Larger radius => more blurry.
  background(255,0,0);
  textFont(createFont("Arial", 48));
  textAlign(CENTER);
  text("rendering...", width/2, height/2);
  jr.render();
}

void joonsBeginRender() {
  jr.beginRecord(); //Make sure to include methods you want rendered.
  camera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
  perspective(fov, aspect, zNear, zFar);

  jr.background(0, 255, 255); //background(gray), or (r, g, b), like Processing.
  //jr.background("gi_instant"); //Global illumination, normal mode.
  jr.background("gi_ambient_occlusion"); //Global illumination, ambient occlusion mode.
}

void joonsEndRender() {
  String inPath = "rendered.png";
  String outDir = "render";
  String outPath = outDir + "/" + fileName + "_" + zeroPadding(frameCounter, 1000) + "." + fileType;
  String tempPath = "captured.png";

  File f1 = new File(sketchPath(inPath));
  f1.delete();
  File f2 = new File(sketchPath(outDir));
  f2.mkdir();
  File f3 = new File(sketchPath(outPath));
  f3.delete();
  File f4 = new File(sketchPath(tempPath));
  f4.delete();
    
  jr.endRecord(); //Make sure to end record.
  jr.displayRendered(true); //Display rendered image if rendering completed, and the argument is true.  
  
  f1.renameTo(f3);
  
  if (frameCounter<frameCounterMax) {
    jr.render();
    frameCounter++;
  } else {
    f4.delete();
    f1.delete();
    exit();
  }
}

String zeroPadding(int _val, int _maxVal){
  String q = ""+_maxVal;
  return nf(_val,q.length());
}
