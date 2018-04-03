
#include <maya/MPxLocatorNode.h>
#include <maya/MString.h>
#include <maya/MTypeId.h>
#include <maya/MPlug.h>
#include <maya/MVector.h>
#include <maya/MDataBlock.h>
#include <maya/MDataHandle.h>
#include <maya/MColor.h>
#include <maya/M3dView.h>
#include <maya/MFnEnumAttribute.h>
#include <maya/MFnNumericAttribute.h>
#include <maya/MFnTypedAttribute.h>
#include <maya/MFnMessageAttribute.h>

#include "sunflowHelperNode.h"

#if defined(OSMac_MachO_)
#include <OpenGL/gl.h>
#include <OpenGL/glu.h>
#else
#include <GL/gl.h>
#include <GL/glu.h>
#endif

MTypeId sunflowHelperNode::id( 0x00114156 );
MObject sunflowHelperNode::aHelperType;
MObject sunflowHelperNode::aHelperColor;
MObject sunflowHelperNode::aHelperOpacity;
MObject sunflowHelperNode::aMeshPath;
MObject sunflowHelperNode::aShaderNode;

void* sunflowHelperNode::creator()
{
  return new sunflowHelperNode;
}


MStatus sunflowHelperNode::initialize()
{
	MFnEnumAttribute		eAttr;
	MFnNumericAttribute		numAttr;
	MFnTypedAttribute		tAttr;
	MFnMessageAttribute		shaderAttr;
	MStatus					status;

	aHelperType = eAttr.create( "type", "t", 0, &status );
	eAttr.addField( "File Mesh", 0 );
	eAttr.addField( "Infinite Plane", 1 );
	CHECK_MSTATUS(eAttr.setKeyable(true));
	CHECK_MSTATUS(eAttr.setStorable(true));
	CHECK_MSTATUS(eAttr.setReadable(true));
	CHECK_MSTATUS(eAttr.setWritable(true));
	CHECK_MSTATUS(eAttr.setConnectable(false));
	CHECK_MSTATUS( addAttribute( aHelperType ) );

	aHelperColor = numAttr.createColor( "helperColor", "hc", &status );
	CHECK_MSTATUS(numAttr.setKeyable(true));
	CHECK_MSTATUS(numAttr.setStorable(true));
	CHECK_MSTATUS(numAttr.setReadable(true));
	CHECK_MSTATUS(numAttr.setWritable(true));
	CHECK_MSTATUS( numAttr.setMin( 0.0, 0.0, 0.0 ) );
	CHECK_MSTATUS( numAttr.setMax( 1.0, 1.0, 1.0 ) );
	CHECK_MSTATUS( numAttr.setDefault( 0.0, 0.0, 0.5) );
	CHECK_MSTATUS( addAttribute( aHelperColor ) );

	aHelperOpacity = numAttr.create( "helperOpacity", "ho", MFnNumericData::kFloat, 0.0, &status );
	CHECK_MSTATUS(numAttr.setKeyable(true));
	CHECK_MSTATUS(numAttr.setStorable(true));
	CHECK_MSTATUS(numAttr.setReadable(true));
	CHECK_MSTATUS(numAttr.setWritable(true));
	CHECK_MSTATUS( numAttr.setMin( 0.0 ) );
	CHECK_MSTATUS( numAttr.setMax( 1.0 ) );
	CHECK_MSTATUS( addAttribute( aHelperOpacity ) );

	aMeshPath = tAttr.create(MString("meshPath"), MString("mp"), MFnData::kString, aMeshPath, &status );
	CHECK_MSTATUS( addAttribute (aMeshPath) );

	aShaderNode = shaderAttr.create( "shaderNode", "sn", &status );
	CHECK_MSTATUS( addAttribute (aShaderNode) );

	return MS::kSuccess;
}



MStatus sunflowHelperNode::compute( const MPlug& /* plug */, MDataBlock& /* data */ )
{
	return MS::kUnknownParameter;
}

void sunflowHelperNode::draw(  M3dView & view, const MDagPath & /*path*/,
                             M3dView::DisplayStyle /*displayStyle*/,
                             M3dView::DisplayStatus displaystatus )
{
  // Get the type
  //
  MObject thisNode = thisMObject();
  MPlug typePlug( thisNode, aHelperType );
  CHECK_MSTATUS(typePlug.getValue( m_helperType ));
  MPlug colorPlug( thisNode, aHelperColor );
  CHECK_MSTATUS(colorPlug.child(0).getValue( m_helperColor.r ));
  CHECK_MSTATUS(colorPlug.child(1).getValue( m_helperColor.g ));
  CHECK_MSTATUS(colorPlug.child(2).getValue( m_helperColor.b ));
  MPlug opacityPlug( thisNode, aHelperOpacity );
  CHECK_MSTATUS(opacityPlug.getValue( m_helperColor.a ));

  view.beginGL();

  // Draw the arrows
  //
/*
  glPushAttrib( GL_ALL_ATTRIB_BITS );

  glBegin( GL_LINES );

    glColor4f( 1.0f, 0.0f, 0.0f, 1.0f );
    glVertex3f(  0.00f,  0.00f,  0.00f );
    glVertex3f(  0.50f,  0.00f,  0.00f );
    glVertex3f(  0.50f,  0.00f,  0.00f );
    glVertex3f(  0.40f, -0.05f,  0.00f );
    glVertex3f(  0.40f, -0.05f,  0.00f );
    glVertex3f(  0.40f,  0.00f,  0.00f );

    glColor4f( 0.0f, 1.0f, 0.0f, 1.0f );
    glVertex3f(  0.00f,  0.00f,  0.00f  );
    glVertex3f(  0.00f,  0.50f,  0.00f  );
    glVertex3f(  0.00f,  0.50f,  0.00f  );
    glVertex3f(  0.05f,  0.40f,  0.00f  );
    glVertex3f(  0.05f,  0.40f,  0.00f  );
    glVertex3f(  0.00f,  0.40f,  0.00f  );

    glColor4f( 0.0f, 0.0f, 1.0f, 1.0f );
    glVertex3f(  0.00f,  0.00f,  0.00f   );
    glVertex3f(  0.00f,  0.00f,  0.50f   );
    glVertex3f(  0.00f,  0.00f,  0.50f   );
    glVertex3f(  0.00f, -0.05f,  0.40f   );
    glVertex3f(  0.00f, -0.05f,  0.40f   );
    glVertex3f(  0.00f,  0.00f,  0.40f   );

  glEnd();

  glPopAttrib();
  */


  // draw the warious primitives
  //

  switch( m_helperType ) {

    case 0:
      // CUBE
      glPushAttrib( GL_ALL_ATTRIB_BITS );
      if ( displaystatus == M3dView::kDormant ) glColor4f( m_helperColor.r, m_helperColor.g, m_helperColor.b, 1.0f );
      glBegin( GL_LINES );

        glVertex3f( -0.50f,  0.50f, -0.50f );
        glVertex3f(  0.50f,  0.50f, -0.50f );
        glVertex3f(  0.50f,  0.50f, -0.50f );
        glVertex3f(  0.50f,  0.50f,  0.50f );
        glVertex3f(  0.50f,  0.50f,  0.50f );
        glVertex3f( -0.50f,  0.50f,  0.50f );
        glVertex3f( -0.50f,  0.50f,  0.50f );
        glVertex3f( -0.50f,  0.50f, -0.50f );

        glVertex3f( -0.50f, -0.50f, -0.50f );
        glVertex3f(  0.50f, -0.50f, -0.50f );
        glVertex3f(  0.50f, -0.50f, -0.50f );
        glVertex3f(  0.50f, -0.50f,  0.50f );
        glVertex3f(  0.50f, -0.50f,  0.50f );
        glVertex3f( -0.50f, -0.50f,  0.50f );
        glVertex3f( -0.50f, -0.50f,  0.50f );
        glVertex3f( -0.50f, -0.50f, -0.50f );

        glVertex3f( -0.50f,  0.50f, -0.50f );
        glVertex3f( -0.50f, -0.50f, -0.50f );
        glVertex3f(  0.50f,  0.50f, -0.50f );
        glVertex3f(  0.50f, -0.50f, -0.50f );
        glVertex3f(  0.50f,  0.50f,  0.50f );
        glVertex3f(  0.50f, -0.50f,  0.50f );
        glVertex3f( -0.50f,  0.50f,  0.50f );
        glVertex3f( -0.50f, -0.50f,  0.50f );

      glEnd();
      glPopAttrib();
      break;

    case 1:
       // CLIPPING PLANE
       
       // Extra OpenGL stuff disabled for now - there seems to be a problem on Linux with Nvidia drivers (glPushAttrib/glPopAttrib not respected?)
       
//      glPushAttrib( GL_ALL_ATTRIB_BITS );
//
//      glEnable( GL_LINE_STIPPLE);
//      glLineStipple(1,0xff);
//      if ( displaystatus == M3dView::kDormant ) glColor4f( m_helperColor.r, m_helperColor.g, m_helperColor.b, 1.0f );


      glBegin( GL_LINES );

        glVertex3f( -1.0f,  0.0f,  1.0f );
        glVertex3f(  1.0f,  0.0f,  1.0f );
        glVertex3f(  1.0f,  0.0f,  1.0f );
        glVertex3f(  1.0f,  0.0f,  -1.0f );
        glVertex3f(  1.0f,  0.0f,  -1.0f );
        glVertex3f( -1.0f,  0.0f,  -1.0f );
        glVertex3f( -1.0f,  0.0f,  -1.0f );
        glVertex3f( -1.0f,  0.0f,  1.0f );

        glVertex3f( -1.0f,  0.0f,  1.0f );
        glVertex3f(  1.0f,  0.0f,  -1.0f );
        glVertex3f(  1.0f,  0.0f,  1.0f );
        glVertex3f( -1.0f,  0.0f,  -1.0f );

      glEnd();

//      glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
//      glEnable(GL_ALPHA_TEST);
//      glAlphaFunc(GL_ALWAYS ,0.1);
//      glEnable(GL_BLEND);
//      glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//      glEnable(GL_DEPTH_TEST);
//      glDepthFunc(GL_LEQUAL);
//      glDepthMask( GL_FALSE );
//      glColor4f( m_helperColor.r, m_helperColor.g, m_helperColor.b, m_helperColor.a );
//      glBegin( GL_QUADS );
//        glVertex3f( -1.0f , 0.0f, 1.0f );
//        glVertex3f(  1.0f , 0.0f, 1.0f );
//        glVertex3f(  1.0f , 0.0f, -1.0f );
//        glVertex3f( -1.0f , 0.0f, -1.0f );
//      glEnd();
//
//      glPopAttrib();
      break;   
  }



  view.endGL();
}


bool sunflowHelperNode::isBounded() const
{
  return true;
}


MBoundingBox sunflowHelperNode::boundingBox() const
{
  MPoint corner1;
  MPoint corner2;

  switch( m_helperType ) {

    case 0:
		corner1 = MPoint( -0.5, -0.5, -0.5 );
		corner2 = MPoint(  0.5,  0.5,  0.5 );
		break;
    case 1:
		corner1 = MPoint( -1.0, -1.0,  0.0 );
		corner2 = MPoint(  1.0,  1.0,  0.5 );
		break;
    default:
		corner1 = MPoint( -0.5, -0.5, -0.5 );
		corner2 = MPoint(  0.5,  0.5,  0.5 );
		break;
  }

  return MBoundingBox( corner1, corner2 );
}

MStatus sunflowHelperNode::connectionMade ( const MPlug & plug, const MPlug & otherPlug, bool asSrc )
{
	/*
	We should do some testing to se that the connected node is a material.
	For now i do it at export time.
	*/

	return MS::kUnknownParameter;
}



