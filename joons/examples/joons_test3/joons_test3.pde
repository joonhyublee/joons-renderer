import joons.JoonsRenderer;

JoonsRenderer jr;
MeshObj battlePod;

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

void setup() {
  size(800, 600, P3D);
  
  battlePod = new MeshObj(loadShape("battle_pod_tri.obj"));
  
  jr = new JoonsRenderer(this);
  jr.setSampler("ipr"); //Rendering mode, either "ipr" or "bucket".
  jr.setSizeMultiplier(1); //Set size of the .PNG file as a multiple of the Processing sketch size.
  jr.setAA(-2, 0, 1); //Set anti-aliasing, (min, max, samples). -2 < min, max < 2, samples = 1,2,3,4..
  jr.setCaustics(1); //Set caustics. 1 ~ 100. affects quality of light scattered through glass.
  //jr.setTraceDepth(1,4,4); //Set trace depth, (diffraction, reflection, refraction). Affects glass. (1,4,4) is good.
  //jr.setDOF(170, 5); //Set depth of field of camera, (focus distance, lens radius). Larger radius => more blurry.
}

void draw() {
  jr.beginRecord(); //Make sure to include methods you want rendered.
  camera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
  perspective(fov, aspect, zNear, zFar);

  jr.background(0, 255, 255); //background(gray), or (r, g, b), like Processing.
  //jr.background("gi_instant"); //Global illumination, normal mode.
  jr.background("gi_ambient_occlusion"); //Global illumination, ambient occlusion mode.

  pushMatrix();
  translate(0, 0, -120);
  strokeWeight(1);
  stroke(0);
  jr.background("cornell_box", 100, 100, 100); //cornellBox(width, height, depth);
  popMatrix();
  
  pushMatrix();
  translate(-40, 20, -140);
  jr.fill("light", 5, 5, 5);
  sphere(20);
  popMatrix();

  pushMatrix();
  translate(40, -20, -140);
  jr.fill("light", 5, 5, 5);
  sphere(20);
  popMatrix();
  
  pushMatrix();
  fill(127);
  jr.fill("glass", 150, 255, 255);
  translate(0, -10, -100);
  scale(60, 60, 60);
  rotateX(radians(180));
  rotateY(radians(90));
  battlePod.draw();
  popMatrix();

  jr.endRecord(); //Make sure to end record.
  jr.displayRendered(true); //Display rendered image if rendering completed, and the argument is true.
}

void keyPressed() {
  if (key=='r'||key=='R') jr.render(); //Press 'r' key to start rendering.
}
