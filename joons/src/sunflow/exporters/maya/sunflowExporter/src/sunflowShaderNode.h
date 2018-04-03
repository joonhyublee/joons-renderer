#ifndef SUNFLOW_SHADER_NODE_H
#define SUNFLOW_SHADER_NODE_H

#include <maya/MPxNode.h>
#include <maya/MColor.h>

//
// DESCRIPTION:
///////////////////////////////////////////////////////
class sunflowShaderNode : public MPxNode
{
public:
                      sunflowShaderNode();
    virtual           ~sunflowShaderNode();

    virtual MStatus   compute( const MPlug&, MDataBlock& );
	virtual void      postConstructor();

	MColor getDiffuseComponent(MDataBlock& block);
	MColor getPhongComponent(MFloatVector &specular, float power, MDataBlock& block);	

    static void *     creator();
    static MStatus    initialize();
    static MTypeId    id;

private:
	static MObject aShader;	
	//PHONG SHADER	
	static MObject phong_specular;
	static MObject phong_power;
	static MObject phong_samples;
	//AMB_OCC SHADER	
	static MObject amb_dark;
	static MObject amb_samples;
	static MObject amb_maxdist;

	//GLASS SHADER
	static MObject glass_eta;
	static MObject glass_absorbtion_distance;
	static MObject glass_absorbtion_color;	

	//SHINY SHADER
	static MObject shiny_shiny;	

	//WARD SHADER
	static MObject ward_specular;
	static MObject ward_upower;
	static MObject ward_vpower;
	static MObject ward_samples;
	
	//UBER SHADER
	static MObject uber_diffuse_texture;
	static MObject uber_diffuse_blend;
	static MObject uber_specular;
	static MObject uber_specular_texture;
	static MObject uber_specular_blend;
	static MObject uber_gloss;
	static MObject uber_samples;

	static MObject aColor;    
	static MObject aPointCamera;
	static MObject aNormalCamera;
	static MObject aLightDirection;
	static MObject aLightIntensity;
    static MObject aPower;
    static MObject aSpecularity;
    static MObject aLightAmbient;
    static MObject aLightDiffuse;
    static MObject aLightSpecular;
    static MObject aLightShadowFraction;
    static MObject aPreShadowIntensity;
    static MObject aLightBlindData;
    static MObject aLightData;

    static MObject aRayOrigin;
    static MObject aRayDirection;

    static MObject aObjectId;
    static MObject aRaySampler;
    static MObject aRayDepth;

    static MObject aReflectGain;

	static MObject aTriangleNormalCamera;

	static MObject aOutColor;

};

#endif /* SUNFLOW_SHADER_NODE_H */
