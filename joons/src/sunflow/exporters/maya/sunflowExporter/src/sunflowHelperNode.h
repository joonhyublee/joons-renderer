#ifndef SUNFLOW_HELPER_NODE_H
#define SUNFLOW_HELPER_NODE_H

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




class sunflowHelperNode : public MPxLocatorNode
{
  public:   

    virtual MStatus         compute( const MPlug& plug, MDataBlock& data );

    virtual void            draw( M3dView & view, const MDagPath & path,
                                  M3dView::DisplayStyle displaystyle,
                                  M3dView::DisplayStatus displaystatus );

    virtual bool            isBounded() const;
    virtual MBoundingBox    boundingBox() const;

	virtual MStatus         connectionMade ( const MPlug & plug, const MPlug & otherPlug, bool asSrc );

    static  void *          creator();
    static  MStatus         initialize();

    static  MObject         aDummy;
    static  MObject         aHelperType;
    static  MObject         aHelperColor;
    static  MObject         aHelperOpacity;
	static	MObject			aMeshPath;
	static	MObject			aShaderNode;

  public:
    static	MTypeId		      id;

    int     m_helperType;
    MColor  m_helperColor;

};

#endif /* SUNFLOW_HELPER_NODE_H */
