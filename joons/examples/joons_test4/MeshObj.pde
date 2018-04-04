class MeshObj {

  ArrayList<MeshObjChild> obj;
 
  MeshObj(PShape _shape) {
    obj = new ArrayList<MeshObjChild>();
    
    for (int i=0; i<_shape.getChildCount(); i++) {
      obj.add(new MeshObjChild(_shape.getChild(i)));
    }
  }
  
  void draw() {
    for (int i=0; i<obj.size(); i++) {
      obj.get(i).draw();
    }
  }
  
  void drawVoxel() {
    for (int i=0; i<obj.size(); i++) {
      obj.get(i).drawVoxel();
    }
  }
  
}

class MeshObjChild {

  ArrayList<PVector> points;
  color strokeColor;
  color fillColor;
  float strokeWeightVal = 0.001;
  float voxelSize = 0.05;
  String material = "shiny";

  MeshObjChild(PShape _shape) {
    points = new ArrayList<PVector>();
    
    try {
      strokeColor = _shape.getStroke(0);
    } catch (Exception e) {
      strokeColor = color(0);
    }
    
    try {
      fillColor = _shape.getFill(0);
    } catch (Exception e) {
      fillColor = color(127);
    }
    
    for (int i=0; i<_shape.getVertexCount(); i++) {
      points.add(_shape.getVertex(i));
    }
  }
  
  void setMaterial(boolean doStroke, boolean doFill) {
    if (doStroke && strokeWeightVal > 0.00001) {
      strokeWeight(strokeWeightVal);
      stroke(strokeColor);
    } else {
      noStroke();
    }
    
    if (doFill) {
      fill(fillColor);
      jr.fill(material, red(fillColor), green(fillColor), blue(fillColor));
    } else {
      noFill();
    }
  }
  
  void draw() {
    setMaterial(true, true);
  
    beginShape();
    for (int i=0; i<points.size(); i++) {
      PVector p = points.get(i);
      vertex(p.x, p.y, p.z);
    }
    endShape();
  }
  
  void drawVoxel() {
    setMaterial(false, true);

    for (int i=0; i<points.size(); i++) {
      PVector p = points.get(i);
      pushMatrix();
      translate(p.x, p.y, p.z);
      box(voxelSize);
      popMatrix();
    }
  }
  
}
