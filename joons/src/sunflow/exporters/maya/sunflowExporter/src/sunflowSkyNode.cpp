#include "sunflowSkyNode.h"
#include <iostream>
#include <cmath>

using namespace std;

float X = 0.525731112119133606;
float Z = 0.850650808352039932;

MTypeId sunflowSkyNode::id( 0x00114155 );

MObject sunflowSkyNode::size;
MObject sunflowSkyNode::resolution;

MObject sunflowSkyNode::sunVector;
MObject sunflowSkyNode::sunVectorX;
MObject sunflowSkyNode::sunVectorY;
MObject sunflowSkyNode::sunVectorZ;
MObject sunflowSkyNode::turbidity;
MObject sunflowSkyNode::exposure;
MObject sunflowSkyNode::sunLight;
MObject sunflowSkyNode::domeAlpha;
//MObject sunflowSkyNode::Gamma;
//MObject sunflowSkyNode::enableGround;
//MObject sunflowSkyNode::groundColor;
//MObject sunflowSkyNode::groundColorR;
//MObject sunflowSkyNode::groundColorG;
//MObject sunflowSkyNode::groundColorB;


sunflowSkyNode::sunflowSkyNode() {
	vData.clear();
	vData.append(MVector(-X, 0.0, Z));
	vData.append(MVector(X, 0.0, Z));
	vData.append(MVector(-X, 0.0, -Z));
	vData.append(MVector(X, 0.0, -Z));
	vData.append(MVector(0.0, Z, X));
	vData.append(MVector(0.0, Z, -X));
	vData.append(MVector(0.0, -Z, X));
	vData.append(MVector(0.0, -Z, -X));
	vData.append(MVector(Z, X, 0.0));
	vData.append(MVector(-Z, X, 0.0));
	vData.append(MVector(Z, -X, 0.0));
	vData.append(MVector(-Z, -X, 0.0));

	tData.clear();
	tData.append(MVector(0,4,1));
	tData.append(MVector(0,9,4));
	tData.append(MVector(9,5,4));
	tData.append(MVector(4,5,8));
	tData.append(MVector(4,8,1));
	tData.append(MVector(8,10,1));
	tData.append(MVector(8,3,10));
	tData.append(MVector(5,3,8));
	tData.append(MVector(5,2,3));
	tData.append(MVector(2,7,3));
	tData.append(MVector(7,10,3));
	tData.append(MVector(7,6,10));
	tData.append(MVector(7,11,6));
	tData.append(MVector(11,0,6));
	tData.append(MVector(0,1,6));
	tData.append(MVector(6,1,10));
	tData.append(MVector(9,0,11));
	tData.append(MVector(9,11,2));
	tData.append(MVector(9,2,5));
	tData.append(MVector(7,2,11));

}
sunflowSkyNode::~sunflowSkyNode() {}

MStatus sunflowSkyNode::compute( const MPlug& /*plug*/, MDataBlock& /*data*/ )
{ 
	return MS::kUnknownParameter;
}

void sunflowSkyNode::quadDome(M3dView & view, int drawMode=0){	

	int sides = (int) m_quadRes;
	float size = m_radius;
	MColor drawColor(0,1,0,0);
	MColor groundColor(0,0,0,0);

	if(drawMode){		
		sides = 16;
		size *= 1.04;
	}
	

	float theta1 = 0, theta2 = 0, theta3 = 0;
   	
	float ex = 0, px = 0, cx = 0;
	float ey = 0, py = 0, cy = 0;
	float ez = 0, pz = 0, cz = 0, r = size;

	for (int j = 0; j < sides / 2; j++)
	{
		theta1 =    j    * TWOPI / sides - PI/2;
		theta2 = (j + 1) * TWOPI / sides - PI/2;
		
		if(drawMode){
			glBegin(GL_LINE_STRIP);			
		}else{
			glBegin(GL_QUAD_STRIP);
		}
		{
			for (int i = 0; i <= sides; i++)
			{					
				theta3 = i * TWOPI / sides;
	
				ex = cosf(theta1) * cosf(theta3);
				ey = sinf(theta1);
				ez = cosf(theta1) * sinf(theta3);
				px = cx + r * ex;
				py = cy + r * ey;
				pz = cz + r * ez;
				
				drawColor.a = m_alpha;
				 
				if(ey<0){
					drawColor = groundColor;
				}else{
					if(ey<0){
						if(!drawMode)
							drawColor = skyDome.getSkyColor(MVector(ex,ey*-1,ez));
					}else{
						if(!drawMode)
							drawColor = skyDome.getSkyColor(MVector(ex,ey,ez));
					}
					drawColor *= m_exposure;
				}

				if(drawMode){
					drawColor = MColor(0,1,0);
				}else{
					view.setDrawColor( drawColor );
					glColor3f(drawColor.r,drawColor.g,drawColor.b);
				}
				glNormal3f(ex,ey,ez);
				glTexCoord2f(i/(float)sides,2*(j+1)/sides);
				glVertex3f(px, py, pz);

				ex = cosf(theta2) * cosf(theta3);
				ey = sinf(theta2);
				ez = cosf(theta2) * sinf(theta3);
				px = cx + r * ex;
				py = cy + r * ey;
				pz = cz + r * ez;	

				if(ey<0){
					drawColor = groundColor;
				}else{
					if(ey<0){
						if(!drawMode)
							drawColor = skyDome.getSkyColor(MVector(ex,ey*-1,ez));
					}else{
						if(!drawMode)
							drawColor = skyDome.getSkyColor(MVector(ex,ey,ez));
					}
					drawColor *= m_exposure;
				}				
				if(drawMode){
					drawColor = MColor(0,1,0);
				}else{
					view.setDrawColor( drawColor );
					glColor3f(drawColor.r,drawColor.g,drawColor.b);
				}
				glNormal3f(ex,ey,ez);
				glTexCoord2f(i/(float)sides,2*(j+1)/sides);
				glVertex3f(px, py, pz);
			}
		}
		glEnd();
	}
}

void sunflowSkyNode::drawTriangle(M3dView & view, MVector v1, MVector v2, MVector v3, int drawMode){
	float vertexRadius = m_radius;
	if(drawMode){
		glBegin(GL_LINE_STRIP);
		vertexRadius*=1.05;
	}else{
		glBegin(GL_POLYGON);
	}
		float groundMultiplier = 0;
		if(v1[1]<=0)
			groundMultiplier = 0;
		else
			groundMultiplier = 1;		
		MColor drawColor = skyDome.getSkyColor(MVector(v1[0],v1[1],v1[2]))*m_exposure*groundMultiplier;		
		if(drawMode)glColor3f(0,1,0);else glColor3f(drawColor.r,drawColor.g,drawColor.b);
		glNormal3f(v1[0],v1[1],v1[2]); glVertex3f(v1[0]*vertexRadius,v1[1]*vertexRadius,v1[2]*vertexRadius);

		if(v2[1]<=0)
			groundMultiplier = 0;
		else
			groundMultiplier = 1;
		drawColor = skyDome.getSkyColor(MVector(v2[0],v2[1],v2[2]))*m_exposure*groundMultiplier;		
		if(drawMode)glColor3f(0,1,0);else glColor3f(drawColor.r,drawColor.g,drawColor.b);
		glNormal3f(v2[0],v2[1],v2[2]); glVertex3f(v2[0]*vertexRadius,v2[1]*vertexRadius,v2[2]*vertexRadius);

		if(v3[1]<=0)
			groundMultiplier = 0;
		else
			groundMultiplier = 1;
		drawColor = skyDome.getSkyColor(MVector(v3[0],v3[1],v3[2]))*m_exposure*groundMultiplier;		
		if(drawMode)glColor3f(0,1,0);else glColor3f(drawColor.r,drawColor.g,drawColor.b);
		glNormal3f(v3[0],v3[1],v3[2]); glVertex3f(v3[0]*vertexRadius,v3[1]*vertexRadius,v3[2]*vertexRadius);
	glEnd();	
}

MStatus sunflowSkyNode::subdivide(M3dView & view, MVector v1, MVector v2, MVector v3, int depth, int drawMode){
	MStatus stat;
	MVector v12;
	MVector v23;
	MVector v31;

	if(depth < 1){
		drawTriangle(view, v1, v2, v3, drawMode);
		return stat;
	}
	for(int i=0; i<3; i++){
		v12[i] = v1[i]+v2[i];
		v23[i] = v2[i]+v3[i];
		v31[i] = v3[i]+v1[i];
	}
	stat = v12.normalize();CHECK_MSTATUS(stat);
	stat = v23.normalize();CHECK_MSTATUS(stat);
	stat = v31.normalize();CHECK_MSTATUS(stat);
	stat = subdivide(view, v1, v12, v31, depth-1, drawMode);CHECK_MSTATUS(stat);
	stat = subdivide(view, v2, v23, v12, depth-1, drawMode);CHECK_MSTATUS(stat);
	stat = subdivide(view, v3, v31, v23, depth-1, drawMode);CHECK_MSTATUS(stat);
	stat = subdivide(view, v12, v23, v31, depth-1, drawMode);CHECK_MSTATUS(stat);
	return stat;
}

void sunflowSkyNode::drawQuadDome(M3dView & view, M3dView::DisplayStatus status){
	MStatus stat;

	view.beginGL();
	glPushAttrib( GL_CURRENT_BIT | GL_POINT_BIT );	
	#if		defined(SGI) || defined(MESA)
		glEnable( GL_POLYGON_OFFSET_EXT );
	#else
		glEnable( GL_POLYGON_OFFSET_FILL );
	#endif
	if((stat == M3dView::kActive) || (stat == M3dView::kLead)){
		view.setDrawColor( 18, M3dView::kActiveColors );
	} else {
		view.setDrawColor( 13, M3dView::kDormantColors );
	}
	glShadeModel(GL_SMOOTH);
	quadDome(view);

	// Make low res wireframe ontop if selected
	/*
	if((status == M3dView::kActive) || (status == M3dView::kLead)){		
		view.setDrawColor( 18, M3dView::kActiveColors );
		quadDome(view, 1);
	}
	*/
	view.endGL();
}

void sunflowSkyNode::drawTriDome(M3dView & view, M3dView::DisplayStatus status){
	MStatus stat;
	view.beginGL();
	glPushAttrib( GL_CURRENT_BIT | GL_POINT_BIT );
	#if		defined(SGI) || defined(MESA)
		glEnable( GL_POLYGON_OFFSET_EXT );
	#else
		glEnable( GL_POLYGON_OFFSET_FILL );
	#endif
	if((status == M3dView::kActive) || (status == M3dView::kLead))
		glDisable(GL_POLYGON_STIPPLE);
	else
		glEnable(GL_POLYGON_STIPPLE);

	glEnable (GL_BLEND);
	glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);	
	for(int i=0; i<20; i++){	
		stat = subdivide(view, vData[tData[i][0]], vData[tData[i][1]], vData[tData[i][2]],(int) m_triRes, 0); // Subdivide and draw the Sphere		
		CHECK_MSTATUS(stat);		
	}	
	glDisable (GL_BLEND); 
	if((status == M3dView::kActive) || (status == M3dView::kLead))
		glEnable(GL_POLYGON_STIPPLE);
	else
		glDisable(GL_POLYGON_STIPPLE);
	
	// Make low res wireframe ontop if selected
	if((status == M3dView::kActive) || (status == M3dView::kLead)){		
		glPushAttrib( GL_CURRENT_BIT | GL_POINT_BIT );
		#if		defined(SGI) || defined(MESA)
			glEnable( GL_POLYGON_OFFSET_EXT );
		#else
			glEnable( GL_POLYGON_OFFSET_FILL );
		#endif		
		view.setDrawColor( 18, M3dView::kActiveColors );		
		for(int i=0; i<20; i++){	
			stat = subdivide(view, vData[tData[i][0]], vData[tData[i][1]], vData[tData[i][2]],1,1); // Subdivide and draw the Sphere
			CHECK_MSTATUS(stat);			
		}		
	}
	view.endGL();

}

void sunflowSkyNode::draw( M3dView & view, const MDagPath & /*path*/, 
							 M3dView::DisplayStyle style,
							 M3dView::DisplayStatus status )
{ 
	MStatus stat;
	MObject thisNode = thisMObject();

	MPlug plug( thisNode, size );	
	MDistance sizeVal;
	plug.getValue( sizeVal );
	m_radius = (float) sizeVal.asCentimeters();

	MPlug resolutionPlug( thisNode, resolution );
	stat = resolutionPlug.getValue ( m_triRes );
	m_quadRes = m_triRes*8;

	MPlug sunLightPlug(thisNode, sunLight);
	MFloatVector lightVector(0.5,0.5,0.5);

	MPlugArray connectedPlugs;
	if(sunLightPlug.connectedTo(connectedPlugs,true,false,&stat) && connectedPlugs.length()){		
		if(connectedPlugs[0].node().apiType() == MFn::kDirectionalLight){
			MFnLight thisLight(connectedPlugs[0].node(), &stat);
			CHECK_MSTATUS( stat );
			lightVector = thisLight.lightDirection(0,MSpace::kWorld,&stat);
		}
	}

	
	lightVector.x *= -1;
	lightVector.y *= -1;
	lightVector.z *= -1;
	
	
	MPlug sunXPlug(thisNode, sunVectorX);
	MPlug sunYPlug(thisNode, sunVectorY);
	MPlug sunZPlug(thisNode, sunVectorZ);
	double sunX;
	double sunY;
	double sunZ;
	sunXPlug.getValue(sunX);
	sunYPlug.getValue(sunY);
	sunZPlug.getValue(sunZ);

	MPlug turbidityPlug( thisNode, turbidity );
	float turbidityVal; 
	stat = turbidityPlug.getValue ( turbidityVal );
	CHECK_MSTATUS( stat );
	
	MPlug exposurePlug( thisNode, exposure );	
	stat = exposurePlug.getValue ( m_exposure );
	CHECK_MSTATUS( stat );

	MPlug domeAlphaPlug( thisNode, domeAlpha );	
	stat = domeAlphaPlug.getValue (m_alpha );
	CHECK_MSTATUS( stat );	

	/*

	MPlug groundPlug(thisNode, enableGround);
	bool useGround;
	groundPlug.getValue(useGround);
	*/
/*
	MPlug groundRPlug(thisNode, groundColorR);
	MPlug groundGPlug(thisNode, groundColorG);
	MPlug groundBPlug(thisNode, groundColorB);
	double groundR;
	double groundG;
	double groundB;
	groundRPlug.getValue(groundR);
	groundGPlug.getValue(groundG);
	groundBPlug.getValue(groundB);
*/
	skyDome.setTurbidity(turbidityVal);
	skyDome.setPosition(MVector(lightVector.x,lightVector.y,lightVector.z));
	skyDome.initialize();

	if(DRAWMODE==0){
		drawQuadDome(view, status);
	}else{
		drawTriDome(view, status);
	}
	
		
}


bool sunflowSkyNode::isBounded() const
{ 
	return true;
}

MBoundingBox sunflowSkyNode::boundingBox() const
{   
	// Get the size
	//
	MObject thisNode = thisMObject();
	MPlug plug( thisNode, size );
	MDistance sizeVal;
	plug.getValue( sizeVal );

	double multiplier = sizeVal.asCentimeters();
 
	MPoint corner1( -1, -1, -1 );
	MPoint corner2( 1, 1, 1 );

	corner1 = corner1 * multiplier;
	corner2 = corner2 * multiplier;

	return MBoundingBox( corner1, corner2 );
}

void* sunflowSkyNode::creator()
{
	return new sunflowSkyNode();
}

MStatus sunflowSkyNode::initialize()
{ 
	MFnUnitAttribute unitFn;
	MFnNumericAttribute	nAttr;
	MFnMessageAttribute lightAttr;
	MStatus			 status;

	size = unitFn.create( "size", "sz", MFnUnitAttribute::kDistance );
	unitFn.setDefault( 200.0 );

	resolution = nAttr.create( "resolution", "res", MFnNumericData::kInt, 3, &status );
	CHECK_MSTATUS ( status );
	CHECK_MSTATUS ( nAttr.setMin(1) );
    CHECK_MSTATUS ( nAttr.setMax(6) );
    CHECK_MSTATUS ( nAttr.setDefault(3) );

	turbidity = nAttr.create( "turbidity", "trb",MFnNumericData::kFloat, 2.0, &status );
	CHECK_MSTATUS ( status );
	CHECK_MSTATUS ( nAttr.setMin(2.0f) );
    CHECK_MSTATUS ( nAttr.setMax(15.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(5.0f) );

	exposure = nAttr.create( "exposure", "exp", MFnNumericData::kFloat, 0.2, &status );
	CHECK_MSTATUS ( status );
	CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(1.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(0.15f) );

	sunLight = lightAttr.create( "sunLight", "sl", &status );
	if (!status) {
		status.perror("create sunLight");
		return status;
	}
	CHECK_MSTATUS ( nAttr.setHidden(false) );

	domeAlpha = nAttr.create( "domeAlpha", "da",
									MFnNumericData::kFloat, 0.5, &status );
	if (!status) {
		status.perror("create domeAlpha attribute");
		return status;
	}
	/*
	Gamma = nAttr.create( "Gamma", "g",
									MFnNumericData::kFloat, 1, &status );
	if (!status) {
		status.perror("create Gamma attribute");
		return status;
	}

	enableGround = nAttr.create( "enableGround", "eg",
									MFnNumericData::kBoolean, 1, &status );
	if (!status) {
		status.perror("create enableGround attribute");
		return status;
	}
	*/
/*
	groundColorR = nAttr.create( "groundColorR", "gcr",MFnNumericData::kFloat, 0,
			&status );
	CHECK_MSTATUS( status );
	CHECK_MSTATUS( nAttr.setKeyable( true ) );
	CHECK_MSTATUS( nAttr.setStorable( true ) );
	CHECK_MSTATUS( nAttr.setDefault( 0.2f ) );

	groundColorG = nAttr.create( "groundColorG", "gcg", MFnNumericData::kFloat, 0,
			&status );
	CHECK_MSTATUS( status );
	CHECK_MSTATUS( nAttr.setKeyable( true ) );
	CHECK_MSTATUS( nAttr.setStorable( true ) );
	CHECK_MSTATUS( nAttr.setDefault( 0.15f ) );

	groundColorB = nAttr.create( "groundColorB", "gcb",MFnNumericData::kFloat, 0,
			&status );
	CHECK_MSTATUS( status );
	CHECK_MSTATUS( nAttr.setKeyable( true ) );
	CHECK_MSTATUS( nAttr.setStorable( true ) );
	CHECK_MSTATUS( nAttr.setDefault( 0.12f ) );

	groundColor = nAttr.create( "gColor", "gc", groundColorR, groundColorG, groundColorB,
			&status );
	CHECK_MSTATUS( status );
	CHECK_MSTATUS( nAttr.setKeyable( true ) );
	CHECK_MSTATUS( nAttr.setStorable( true ) );
	CHECK_MSTATUS( nAttr.setDefault( 0.2f, 0.15f, 0.12f ) );
	CHECK_MSTATUS( nAttr.setUsedAsColor( true ) );
*/
	status = addAttribute (size);
		if (!status) { status.perror("addAttribute size"); return status;}	
	status = addAttribute (resolution);
		if (!status) { status.perror("addAttribute resolution"); return status;}
	//CHECK_MSTATUS ( addAttribute(sunVector) );
	status = addAttribute (turbidity);
		if (!status) { status.perror("addAttribute Turbidity"); return status;}
	status = addAttribute (exposure);
		if (!status) { status.perror("addAttribute Exposure"); return status;}
	CHECK_MSTATUS ( addAttribute(sunLight) );
	CHECK_MSTATUS ( addAttribute(domeAlpha) );
	//status = addAttribute (Gamma);
		//if (!status) { status.perror("addAttribute Gamma"); return status;}
	//status = addAttribute (enableGround);
		//if (!status) { status.perror("addAttribute enableGround"); return status;}
	//status = addAttribute (groundColor);
		//if (!status) { status.perror("addAttribute groundColor"); return status;}
	
	return MS::kSuccess;
}
