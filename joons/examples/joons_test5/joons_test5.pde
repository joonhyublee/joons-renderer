MeshObj battlePod;
float posX = 0;

void setup() {
  size(800, 600, P3D);
  
  joonsSetup();
  battlePod = new MeshObj(loadShape("battle_pod_tri.obj"));
}

void draw() {
  joonsBeginRender();

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
  fill(127);
  jr.fill("glass", 150, 255, 255);
  translate(posX, -10, -100);
  posX++;
  scale(60, 60, 60);
  rotateX(radians(180));
  rotateY(radians(90));
  battlePod.drawVoxel();
  popMatrix();

  joonsEndRender();
}
