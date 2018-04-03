#ifndef SUNFLOW_SKY_NODE_H
#define SUNFLOW_SKY_NODE_H

#include "skylight.h"
#include <maya/MPxLocatorNode.h> 
#include <maya/MString.h> 
#include <maya/MTypeId.h> 
#include <maya/MPlug.h>
#include <maya/MVector.h>
#include <maya/MFloatVector.h>
#include <maya/MDataBlock.h>
#include <maya/MDataHandle.h>
#include <maya/MColor.h>
#include <maya/M3dView.h>
#include <maya/MDistance.h>
#include <maya/MFnUnitAttribute.h>
#include <maya/MFnNumericAttribute.h>
#include <maya/MFnMessageAttribute.h>
#include <maya/MFnLight.h>
#include <maya/MPlugArray.h>
#include <maya/MVectorArray.h>

#define PI              3.14159265358979323846
#define TWOPI           6.2831853071795864769

#define DRAWMODE 0

class sunflowSkyNode : public MPxLocatorNode
{
public:
	sunflowSkyNode();
	virtual ~sunflowSkyNode(); 

    virtual MStatus   		compute( const MPlug& plug, MDataBlock& data );

	virtual void            draw( M3dView & view, const MDagPath & path, 
								  M3dView::DisplayStyle style,
								  M3dView::DisplayStatus status );

	virtual bool            isBounded() const;
	virtual MBoundingBox    boundingBox() const; 

	static  void *          creator();
	static  MStatus         initialize();

	static  MObject         size;	// The size of the dome
	static  MObject         resolution;

	SunSky					skyDome;
	void					drawTriDome(M3dView & view, M3dView::DisplayStatus status);
	MStatus					subdivide(M3dView & view, MVector v1, MVector v2, MVector v3, int depth, int drawMode);
	void					drawTriangle(M3dView & view, MVector v1, MVector v2, MVector v3, int drawMode);

	void					drawQuadDome(M3dView & view, M3dView::DisplayStatus status);
	void					quadDome(M3dView & view, int drawMode);

public: 
	static	MTypeId		id;
protected:
	static MObject			sunVectorX;
	static MObject			sunVectorY;
	static MObject			sunVectorZ;
	static MObject			sunVector;
	static MObject			turbidity;
	static MObject			exposure;
	static MObject			sunLight;
	static MObject			domeAlpha;

	MVectorArray			vData;
	MVectorArray			tData;

	float					radius;
	float					domeOpacity;

	float					m_radius;
	float					m_multiplier;
	float					m_triRes;
	float					m_quadRes;
	float					m_exposure;
	float					m_alpha;

	//static MObject			Gamma;
	//static MObject			enableGround;
	//static MObject			groundColorR;
	//static MObject			groundColorG;
	//static MObject			groundColorB;
	//static MObject			groundColor;

};

#endif /* SUNFLOW_SKY_NODE_H */
