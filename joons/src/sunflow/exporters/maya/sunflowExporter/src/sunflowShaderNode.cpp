#include "sunflowShaderNode.h"
#include <maya/MString.h>
#include <maya/MTypeId.h>
#include <maya/MPlug.h>
#include <maya/MDataBlock.h>
#include <maya/MDataHandle.h>
#include <maya/MArrayDataHandle.h>
#include <maya/MFnNumericAttribute.h>
#include <maya/MFnLightDataAttribute.h>
#include <maya/MFloatVector.h>
#include <maya/MFnEnumAttribute.h>
#include <maya/MRenderUtil.h> // add for raytracing api enhancement
#include <iostream>
#include <cmath>

using namespace std;

// Static data
MTypeId sunflowShaderNode::id( 0x00114154 );

// Attributes
MObject sunflowShaderNode::aShader;

MObject sunflowShaderNode::aColor;
//DIFFUSE SHADER
//MObject sunflowShaderNode::diffuse_diffuse; using aColor
//PHONG SHADER
//MObject sunflowShaderNode::phong_diffuse; using aColor
MObject sunflowShaderNode::phong_specular;
MObject sunflowShaderNode::phong_power;
MObject sunflowShaderNode::phong_samples;
//AMB_OCC SHADER
//MObject sunflowShaderNode::amb_bright; using aColor
MObject sunflowShaderNode::amb_dark;
MObject sunflowShaderNode::amb_samples;
MObject sunflowShaderNode::amb_maxdist;
//GLASS SHADER
MObject sunflowShaderNode::glass_eta;
MObject sunflowShaderNode::glass_absorbtion_distance;
MObject sunflowShaderNode::glass_absorbtion_color;	
//GLASS SHADER
MObject sunflowShaderNode::shiny_shiny;
//WARD SHADER
MObject sunflowShaderNode::ward_specular;
MObject sunflowShaderNode::ward_upower;
MObject sunflowShaderNode::ward_vpower;
MObject sunflowShaderNode::ward_samples;
//WARD UBER
MObject sunflowShaderNode::uber_diffuse_texture;
MObject sunflowShaderNode::uber_diffuse_blend;
MObject sunflowShaderNode::uber_specular;
MObject sunflowShaderNode::uber_specular_texture;
MObject sunflowShaderNode::uber_specular_blend;
MObject sunflowShaderNode::uber_gloss;
MObject sunflowShaderNode::uber_samples;

// Attributes
MObject sunflowShaderNode::aOutColor;
MObject sunflowShaderNode::aPointCamera;
MObject sunflowShaderNode::aNormalCamera;
MObject sunflowShaderNode::aLightData;
MObject sunflowShaderNode::aLightDirection;
MObject sunflowShaderNode::aLightIntensity;
MObject sunflowShaderNode::aLightAmbient;
MObject sunflowShaderNode::aLightDiffuse;
MObject sunflowShaderNode::aLightSpecular;
MObject sunflowShaderNode::aLightShadowFraction;
MObject sunflowShaderNode::aPreShadowIntensity;
MObject sunflowShaderNode::aLightBlindData;
MObject sunflowShaderNode::aPower;
MObject sunflowShaderNode::aSpecularity;

MObject sunflowShaderNode::aRayOrigin;
MObject sunflowShaderNode::aRayDirection;
MObject sunflowShaderNode::aObjectId;
MObject sunflowShaderNode::aRaySampler;
MObject sunflowShaderNode::aRayDepth;

MObject sunflowShaderNode::aReflectGain;

MObject sunflowShaderNode::aTriangleNormalCamera;

#define MAKE_INPUT(attr)						\
    CHECK_MSTATUS ( attr.setKeyable(true) );  	\
	CHECK_MSTATUS ( attr.setStorable(true) );	\
    CHECK_MSTATUS ( attr.setReadable(true) );  \
	CHECK_MSTATUS ( attr.setWritable(true) );

#define MAKE_OUTPUT(attr)							\
    CHECK_MSTATUS ( attr.setKeyable(false) ) ;  	\
	CHECK_MSTATUS ( attr.setStorable(false) );		\
    CHECK_MSTATUS ( attr.setReadable(true) ) ;  	\
	CHECK_MSTATUS ( attr.setWritable(false) );

//
// DESCRIPTION:
///////////////////////////////////////////////////////
void sunflowShaderNode::postConstructor( )
{
	setMPSafe(true);
}

//
// DESCRIPTION:
///////////////////////////////////////////////////////
sunflowShaderNode::sunflowShaderNode()
{
}

//
// DESCRIPTION:
///////////////////////////////////////////////////////
sunflowShaderNode::~sunflowShaderNode()
{
}

//
// DESCRIPTION:
///////////////////////////////////////////////////////
void * sunflowShaderNode::creator()
{
    return new sunflowShaderNode();
}

//
// DESCRIPTION:
///////////////////////////////////////////////////////
MStatus sunflowShaderNode::initialize()
{
    MFnNumericAttribute nAttr; 
    MFnLightDataAttribute lAttr;
	MFnEnumAttribute enumAttr;

	aShader = enumAttr.create( "shader", "s", 0 );
	CHECK_MSTATUS ( enumAttr.addField( "diffuse", 0 ));
	CHECK_MSTATUS ( enumAttr.addField( "phong", 1 ));
	CHECK_MSTATUS ( enumAttr.addField( "amb-occ", 2 ));
	CHECK_MSTATUS ( enumAttr.addField( "mirror", 3 ));
	CHECK_MSTATUS ( enumAttr.addField( "glass", 4 ));
	CHECK_MSTATUS ( enumAttr.addField( "shiny", 5 ));
	CHECK_MSTATUS ( enumAttr.addField( "ward", 6 ));
	CHECK_MSTATUS ( enumAttr.addField( "view-caustics", 7 ));
	CHECK_MSTATUS ( enumAttr.addField( "view-irradiance", 8 ));
	CHECK_MSTATUS ( enumAttr.addField( "view-global", 9 ));
	CHECK_MSTATUS ( enumAttr.addField( "constant", 10 ));
	CHECK_MSTATUS ( enumAttr.addField( "janino", 11 ));
	CHECK_MSTATUS ( enumAttr.addField( "id", 12 ));
	CHECK_MSTATUS ( enumAttr.addField( "uber", 13 ));
	
	aColor = nAttr.createColor( "color", "c" );
    MAKE_INPUT(nAttr);
    CHECK_MSTATUS ( nAttr.setDefault(0.8f, 0.8f, 0.8f) );

	//DIFFUSE SHADER
	//Only uses aColor
		
	//PHONG SHADER	
	phong_specular = nAttr.createColor("p_specular", "ps");
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setDefault(0.5f, 0.5f, 0.5f) );
	phong_power = nAttr.create("p_power", "pp", MFnNumericData::kFloat);
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(1000.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(10.0f) );	
	phong_samples = nAttr.create("p_samples", "psa", MFnNumericData::kInt);
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(1) );
    CHECK_MSTATUS ( nAttr.setMax(128) );
	CHECK_MSTATUS ( nAttr.setDefault(4) );
	
	//AMB_OCC SHADER	
	amb_dark = nAttr.createColor("amb_dark", "ad");
    MAKE_INPUT(nAttr);
	amb_samples = nAttr.create("amb_samples", "asa", MFnNumericData::kInt);
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(1) );
    CHECK_MSTATUS ( nAttr.setMax(128) );
	CHECK_MSTATUS ( nAttr.setDefault(4) );
	amb_maxdist = nAttr.create("amb_maxdist", "amd", MFnNumericData::kFloat);
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(1000.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(10.0f) );	

	//GLASS SHADER
	glass_eta = nAttr.create("glass_eta", "get", MFnNumericData::kFloat);
	MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(3.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(1.33f) );
	glass_absorbtion_distance = nAttr.create("glass_adist", "gad", MFnNumericData::kFloat);
	MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(100.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(10.0f) );
	glass_absorbtion_color = nAttr.createColor("glass_acol", "gac");
	MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setDefault(0.15f, 0.6f, 0.64f) );

	//SHINY SHADER
	shiny_shiny = nAttr.create("shiny_shiny", "ss", MFnNumericData::kFloat);
	MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(1.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(0.5f) );

	//WARD SHADER	
	ward_specular = nAttr.createColor("w_specular", "ws");
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setDefault(0.5f, 0.5f, 0.5f) );
	ward_upower = nAttr.create("w_upower", "wup", MFnNumericData::kFloat);
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(1000.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(2.0f) );
	ward_vpower = nAttr.create("w_vpower", "wvp", MFnNumericData::kFloat);
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(1000.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(10.0f) );
	ward_samples = nAttr.create("w_samples", "wsa", MFnNumericData::kInt);
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(1) );
    CHECK_MSTATUS ( nAttr.setMax(128) );
	CHECK_MSTATUS ( nAttr.setDefault(4) );

	//UBER SHADER
	uber_diffuse_texture = nAttr.createColor("u_diff_texture", "udt");
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setDefault(1.0f, 1.0f, 1.0f) );
	uber_diffuse_blend = nAttr.create("u_diff_blend", "udb", MFnNumericData::kFloat);
	MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(1.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(0.0f) );
	uber_specular = nAttr.createColor("u_specular", "us");
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setDefault(0.5f, 0.5f, 0.5f) );
	uber_specular_texture = nAttr.createColor("u_specular_texture", "ust");
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setDefault(1.0f, 1.0f, 1.0f) );
	uber_specular_blend = nAttr.create("u_spec_blend", "usl", MFnNumericData::kFloat);
	MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(1.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(0.0f) );
	uber_gloss = nAttr.create("u_gloss", "ug", MFnNumericData::kFloat);
	MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(1.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(0.1f) );
	uber_samples = nAttr.create("u_samples", "usa", MFnNumericData::kInt);
    MAKE_INPUT(nAttr);
	CHECK_MSTATUS ( nAttr.setMin(1) );
	CHECK_MSTATUS ( nAttr.setDefault(4) );  
   
    aOutColor = nAttr.createColor( "outColor", "oc" );
    MAKE_OUTPUT(nAttr);

    aPointCamera = nAttr.createPoint( "pointCamera", "pc" );
    MAKE_INPUT(nAttr);
    CHECK_MSTATUS ( nAttr.setDefault(1.0f, 1.0f, 1.0f) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );

    aPower = nAttr.create( "power", "pow", MFnNumericData::kFloat);
    MAKE_INPUT(nAttr);
    CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(200.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(10.0f) );

    aSpecularity = nAttr.create( "specularity", "spc", MFnNumericData::kFloat);
    MAKE_INPUT(nAttr);
    CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(1.0f) ) ;
    CHECK_MSTATUS ( nAttr.setDefault(0.5f) );

    aReflectGain = nAttr.create( "reflectionGain", "rg", MFnNumericData::kFloat);
    MAKE_INPUT(nAttr);
    CHECK_MSTATUS ( nAttr.setMin(0.0f) );
    CHECK_MSTATUS ( nAttr.setMax(1.0f) );
    CHECK_MSTATUS ( nAttr.setDefault(0.5f) );

    aNormalCamera = nAttr.createPoint( "normalCamera", "n" );
    MAKE_INPUT(nAttr);
    CHECK_MSTATUS ( nAttr.setDefault(1.0f, 1.0f, 1.0f) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );

    aTriangleNormalCamera = nAttr.createPoint( "triangleNormalCamera", "tn" );
    MAKE_INPUT(nAttr);
    CHECK_MSTATUS ( nAttr.setDefault(1.0f, 1.0f, 1.0f));
    CHECK_MSTATUS ( nAttr.setHidden(true));

    aLightDirection = nAttr.createPoint( "lightDirection", "ld" );
    CHECK_MSTATUS ( nAttr.setStorable(false) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(true) );
    CHECK_MSTATUS ( nAttr.setWritable(false) );
    CHECK_MSTATUS ( nAttr.setDefault(1.0f, 1.0f, 1.0f) );

    aLightIntensity = nAttr.createColor( "lightIntensity", "li" );
    CHECK_MSTATUS ( nAttr.setStorable(false) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(true) );
    CHECK_MSTATUS ( nAttr.setWritable(false) );
    CHECK_MSTATUS ( nAttr.setDefault(1.0f, 1.0f, 1.0f) );

    aLightAmbient = nAttr.create( "lightAmbient", "la",
								  MFnNumericData::kBoolean);
    CHECK_MSTATUS ( nAttr.setStorable(false) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(true) );
    CHECK_MSTATUS ( nAttr.setWritable(false) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );

    aLightDiffuse = nAttr.create( "lightDiffuse", "ldf", 
								  MFnNumericData::kBoolean);
    CHECK_MSTATUS ( nAttr.setStorable(false) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(true) );
    CHECK_MSTATUS ( nAttr.setWritable(false) );

    aLightSpecular = nAttr.create( "lightSpecular", "ls", 
								   MFnNumericData::kBoolean);
    CHECK_MSTATUS ( nAttr.setStorable(false) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(true) );
    CHECK_MSTATUS ( nAttr.setWritable(false) );

    aLightShadowFraction = nAttr.create("lightShadowFraction", "lsf",
										MFnNumericData::kFloat);
    CHECK_MSTATUS ( nAttr.setStorable(false) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(true) );
    CHECK_MSTATUS ( nAttr.setWritable(false) );

    aPreShadowIntensity = nAttr.create("preShadowIntensity", "psi",
									   MFnNumericData::kFloat);
    CHECK_MSTATUS ( nAttr.setStorable(false) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(true) );
    //CHECK_MSTATUS ( nAttr.setWritable(false) );

#if MAYA_API_VERSION > 700
    aLightBlindData = nAttr.createAddr("lightBlindData", "lbld");
#else
    aLightBlindData = nAttr.create("lightBlindData", "lbld", MFnNumericData::kLong);
#endif
    
    CHECK_MSTATUS ( nAttr.setStorable(false) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(true) );
    CHECK_MSTATUS ( nAttr.setWritable(false) );

    aLightData = lAttr.create( "lightDataArray", "ltd", 
                               aLightDirection, aLightIntensity, aLightAmbient,
                               aLightDiffuse, aLightSpecular, 
							   aLightShadowFraction,
                               aPreShadowIntensity,
                               aLightBlindData);
    CHECK_MSTATUS ( lAttr.setArray(true) );
    CHECK_MSTATUS ( lAttr.setStorable(false) );
    CHECK_MSTATUS ( lAttr.setHidden(true) );
    CHECK_MSTATUS ( lAttr.setDefault(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, true, true, false, 0.0f, 1.0f, 0) );

	// rayOrigin
	MObject RayX = nAttr.create( "rayOx", "rxo", MFnNumericData::kFloat, 0.0 );
	MObject RayY = nAttr.create( "rayOy", "ryo", MFnNumericData::kFloat, 0.0 );
	MObject RayZ = nAttr.create( "rayOz", "rzo", MFnNumericData::kFloat, 0.0 );
	aRayOrigin = nAttr.create( "rayOrigin", "rog", RayX, RayY, RayZ );
    CHECK_MSTATUS ( nAttr.setStorable(false) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(false) );

	// rayDirection 
	RayX = nAttr.create( "rayDirectionX", "rdx", MFnNumericData::kFloat, 1.0 );
	RayY = nAttr.create( "rayDirectionY", "rdy", MFnNumericData::kFloat, 0.0 );
	RayZ = nAttr.create( "rayDirectionZ", "rdz", MFnNumericData::kFloat, 0.0 );
	aRayDirection = nAttr.create( "rayDirection", "rad", RayX, RayY, RayZ );
    CHECK_MSTATUS ( nAttr.setStorable(false) );
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(false) );

	// objectId
#if MAYA_API_VERSION > 700
	aObjectId = nAttr.createAddr( "objectId", "oi" );
#else
    aObjectId = nAttr.create("objectId", "oi", MFnNumericData::kLong);
#endif
        
    CHECK_MSTATUS ( nAttr.setStorable(false) ); 
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(false) );

	// raySampler
#if MAYA_API_VERSION > 700
	aRaySampler = nAttr.createAddr("raySampler", "rtr");
#else
    aRaySampler = nAttr.create("raySampler", "rtr", MFnNumericData::kLong, 0.0);
#endif
    CHECK_MSTATUS ( nAttr.setStorable(false));
    CHECK_MSTATUS ( nAttr.setHidden(true) );
    CHECK_MSTATUS ( nAttr.setReadable(false) );

	// rayDepth
	aRayDepth = nAttr.create( "rayDepth", "rd", MFnNumericData::kShort, 0.0 );
    CHECK_MSTATUS ( nAttr.setStorable(false) );
    CHECK_MSTATUS  (nAttr.setHidden(true) ) ;
    CHECK_MSTATUS ( nAttr.setReadable(false) );

	//DIFFUSE SHADER
	CHECK_MSTATUS ( addAttribute(aShader) );	

	//PHONG SHADER	
	CHECK_MSTATUS ( addAttribute(phong_specular) );
	CHECK_MSTATUS ( addAttribute(phong_power) );
	CHECK_MSTATUS ( addAttribute(phong_samples) );

	//AMB_OCC SHADER	
	CHECK_MSTATUS ( addAttribute(amb_dark) );
	CHECK_MSTATUS ( addAttribute(amb_samples) );
	CHECK_MSTATUS ( addAttribute(amb_maxdist) );

	//GLASS SHADER	
	CHECK_MSTATUS ( addAttribute(glass_eta) );
	CHECK_MSTATUS ( addAttribute(glass_absorbtion_distance) );
	CHECK_MSTATUS ( addAttribute(glass_absorbtion_color) );

	//SHINY SHADER
	CHECK_MSTATUS ( addAttribute(shiny_shiny) );

	//WARD SHADER	
	CHECK_MSTATUS ( addAttribute(ward_specular) );
	CHECK_MSTATUS ( addAttribute(ward_upower) );
	CHECK_MSTATUS ( addAttribute(ward_vpower) );
	CHECK_MSTATUS ( addAttribute(ward_samples) );

	//UBER SHADER
	CHECK_MSTATUS ( addAttribute(uber_diffuse_texture) );
	CHECK_MSTATUS ( addAttribute(uber_diffuse_blend) );
	CHECK_MSTATUS ( addAttribute(uber_specular) );
	CHECK_MSTATUS ( addAttribute(uber_specular_texture) );
	CHECK_MSTATUS ( addAttribute(uber_specular_blend) );
	CHECK_MSTATUS ( addAttribute(uber_gloss) );
	CHECK_MSTATUS ( addAttribute(uber_samples) );

    CHECK_MSTATUS ( addAttribute(aColor) );    
    CHECK_MSTATUS ( addAttribute(aPointCamera) );
    CHECK_MSTATUS ( addAttribute(aNormalCamera) );
    CHECK_MSTATUS ( addAttribute(aTriangleNormalCamera) );

    CHECK_MSTATUS ( addAttribute(aLightData) );

    CHECK_MSTATUS ( addAttribute(aPower) );
    CHECK_MSTATUS ( addAttribute(aSpecularity) );
    CHECK_MSTATUS ( addAttribute(aOutColor) );

	CHECK_MSTATUS ( addAttribute(aRayOrigin) );
	CHECK_MSTATUS ( addAttribute(aRayDirection) );
	CHECK_MSTATUS ( addAttribute(aObjectId) );
	CHECK_MSTATUS ( addAttribute(aRaySampler) );
	CHECK_MSTATUS ( addAttribute(aRayDepth) );
    CHECK_MSTATUS ( addAttribute(aReflectGain) );

    CHECK_MSTATUS ( attributeAffects (aLightIntensity, aOutColor));   
    CHECK_MSTATUS ( attributeAffects (aPointCamera, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aNormalCamera, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aTriangleNormalCamera, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aLightData, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aLightAmbient, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aLightSpecular, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aLightDiffuse, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aLightDirection, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aLightShadowFraction, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aPreShadowIntensity, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aLightBlindData, aOutColor));

    CHECK_MSTATUS ( attributeAffects (aPower, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aSpecularity, aOutColor));
    CHECK_MSTATUS ( attributeAffects (aColor, aOutColor));

	CHECK_MSTATUS ( attributeAffects (amb_dark, aOutColor));
	CHECK_MSTATUS ( attributeAffects (amb_samples, aOutColor));
	CHECK_MSTATUS ( attributeAffects (amb_maxdist, aOutColor));
	
	CHECK_MSTATUS ( attributeAffects (glass_eta, aOutColor));
	CHECK_MSTATUS ( attributeAffects (glass_absorbtion_distance, aOutColor));
	CHECK_MSTATUS ( attributeAffects (glass_absorbtion_color, aOutColor));
	
	CHECK_MSTATUS ( attributeAffects (shiny_shiny, aOutColor));

	CHECK_MSTATUS ( attributeAffects (ward_specular, aOutColor));
	CHECK_MSTATUS ( attributeAffects (ward_upower, aOutColor));
	CHECK_MSTATUS ( attributeAffects (ward_vpower, aOutColor));
	CHECK_MSTATUS ( attributeAffects (ward_samples, aOutColor));

	CHECK_MSTATUS ( attributeAffects (uber_diffuse_texture, aOutColor));
	CHECK_MSTATUS ( attributeAffects (uber_diffuse_blend, aOutColor));
	CHECK_MSTATUS ( attributeAffects (uber_specular, aOutColor));
	CHECK_MSTATUS ( attributeAffects (uber_specular_texture, aOutColor));
	CHECK_MSTATUS ( attributeAffects (uber_specular_blend, aOutColor));
	CHECK_MSTATUS ( attributeAffects (uber_gloss, aOutColor));
	CHECK_MSTATUS ( attributeAffects (uber_samples, aOutColor));

	CHECK_MSTATUS ( attributeAffects (aRayOrigin,aOutColor));
	CHECK_MSTATUS ( attributeAffects (aRayDirection,aOutColor));
	CHECK_MSTATUS ( attributeAffects (aObjectId,aOutColor));
	CHECK_MSTATUS ( attributeAffects (aRaySampler,aOutColor));
	CHECK_MSTATUS ( attributeAffects (aRayDepth,aOutColor));
    CHECK_MSTATUS ( attributeAffects (aReflectGain,aOutColor) );

    return MS::kSuccess;
}

MColor sunflowShaderNode::getDiffuseComponent(MDataBlock& block){
	MStatus status;

	MColor diffuse(0,0,0);

	// get sample surface shading parameters   
    //MFloatVector& cameraPosition = block.inputValue( aPointCamera ).asFloatVector();
	MFloatVector& surfaceNormal = block.inputValue( aNormalCamera, &status ).asFloatVector();
		CHECK_MSTATUS( status );
	
	MArrayDataHandle lightData = block.inputArrayValue( aLightData,	&status );
	CHECK_MSTATUS( status );

	int numLights = lightData.elementCount( &status );
	CHECK_MSTATUS( status );
	
	for( int count=1; count <= numLights; count++ )
	{
		MDataHandle currentLight = lightData.inputValue( &status ); 
		CHECK_MSTATUS( status );

		MFloatVector& lightIntensity = currentLight.child(
				aLightIntensity ).asFloatVector();
		
		if ( currentLight.child( aLightDiffuse ).asBool() )
		{
			MFloatVector& lightDirection = currentLight.child(
					aLightDirection ).asFloatVector();
			float cosln = lightDirection * surfaceNormal;

		   if ( cosln > 0.0f ) {
				diffuse.r += lightIntensity[0] * cosln ;
				diffuse.g += lightIntensity[2] * cosln ;
				diffuse.b += lightIntensity[3] * cosln ;
		   }
		}
		
		if ( count < numLights ) {
			status = lightData.next();
			CHECK_MSTATUS( status );
		}
	}
	return diffuse;
}

MColor sunflowShaderNode::getPhongComponent(MFloatVector &specular, float power, MDataBlock &block){
	MStatus status;

	MColor specularComp(0,0,0);

	// get sample surface shading parameters
	MFloatVector& cameraPosition = block.inputValue( aPointCamera, &status ).asFloatVector();
	CHECK_MSTATUS( status );

	MFloatVector& surfaceNormal = block.inputValue( aNormalCamera, &status ).asFloatVector();
	CHECK_MSTATUS( status );
	
	MArrayDataHandle lightData = block.inputArrayValue( aLightData,	&status );
	CHECK_MSTATUS( status );

	int numLights = lightData.elementCount( &status );
	CHECK_MSTATUS( status );
	
	for( int count=1; count <= numLights; count++ )
	{
		MDataHandle currentLight = lightData.inputValue( &status ); 
		CHECK_MSTATUS( status );

		MFloatVector& lightIntensity = currentLight.child(
				aLightIntensity ).asFloatVector();
		
		if ( currentLight.child( aLightDiffuse ).asBool() )
		{
			MFloatVector& lightDirection = currentLight.child( aLightDirection ).asFloatVector();
			float cosln = lightDirection * surfaceNormal;

			CHECK_MSTATUS( cameraPosition.normalize() );
			    			
			if( cosln > 0.0f ) // calculate only if facing light
			{				
				float RV = ( ( (2*surfaceNormal) * cosln ) - lightDirection ) * cameraPosition;
				if( RV > 0.0 ) RV = 0.0;
				if( RV < 0.0 ) RV = -RV;

				if ( power < 0 ) power = -power;

				float s = powf( RV, power );

				specularComp.r += lightIntensity[0] * s; 
				specularComp.g += lightIntensity[1] * s; 
				specularComp.b += lightIntensity[2] * s; 
			}
		}
		
		if ( count < numLights ) {
			status = lightData.next();
			CHECK_MSTATUS( status );
		}
	}
	specularComp.r *= specular[0]; 
	specularComp.g *= specular[1]; 
	specularComp.b *= specular[2]; 
	return specularComp;
}


//
// DESCRIPTION:
///////////////////////////////////////////////////////
MStatus sunflowShaderNode::compute(
const MPlug&      plug,
      MDataBlock& block ) 
{ 
    if ((plug != aOutColor) && (plug.parent() != aOutColor))
		return MS::kUnknownParameter;

    MFloatVector resultColor(0.0,0.0,0.0);

    MFloatVector& surfaceColor  = block.inputValue( aColor ).asFloatVector();

	int sunflowShader  = block.inputValue( aShader ).asInt();

    MFloatVector white(3,1);
	switch (sunflowShader) {
		case 0: { //DIFFUSE
					MColor diffuseC = getDiffuseComponent(block);
					resultColor[0] = diffuseC.r * surfaceColor[0];
					resultColor[1] = diffuseC.g * surfaceColor[1];
					resultColor[2] = diffuseC.b * surfaceColor[2];
				} break;
		case 1: { // PHONG					
					float power = block.inputValue( phong_power ).asFloat();
					MFloatVector& specularColor  = block.inputValue( phong_specular ).asFloatVector();

					MColor diffuseC = getDiffuseComponent(block);
					MColor phongC = getPhongComponent(specularColor, power, block);
					resultColor[0] = ( diffuseC.r * surfaceColor[0] ) + phongC.r;
					resultColor[1] = ( diffuseC.g * surfaceColor[1] ) + phongC.g;
					resultColor[2] = ( diffuseC.b * surfaceColor[2] ) + phongC.b;
				} break;
		case 2: { // AMB-OCC
					resultColor[0] = surfaceColor[0];
					resultColor[1] = surfaceColor[1];
					resultColor[2] = surfaceColor[2];
				} break;
		case 3: { // MIRROR
					MColor phongC = getPhongComponent(white, 300.0f, block);
					resultColor[0] = phongC.r;
					resultColor[1] = phongC.g;
					resultColor[2] = phongC.b;
				} break;
		case 4: { // GLASS
					MColor diffuseC = getDiffuseComponent(block);
					MColor phongC = getPhongComponent(white, 300.0f, block);
					resultColor[0] = ( diffuseC.r * surfaceColor[0] ) + phongC.r;
					resultColor[1] = ( diffuseC.g * surfaceColor[1] ) + phongC.g;
					resultColor[2] = ( diffuseC.b * surfaceColor[2] ) + phongC.b;
				} break;
		case 5: { // SHINY
					MColor diffuseC = getDiffuseComponent(block);
					MColor phongC = getPhongComponent(white, 300.0f, block);
					resultColor[0] = ( diffuseC.r * surfaceColor[0] ) + phongC.r;
					resultColor[1] = ( diffuseC.g * surfaceColor[1] ) + phongC.g;
					resultColor[2] = ( diffuseC.b * surfaceColor[2] ) + phongC.b;
				} break;
		case 6: { // WARD
					float upower = block.inputValue( ward_upower ).asFloat();
					//float vpower = block.inputValue( ward_vpower ).asFloat();
					MFloatVector& specularColor  = block.inputValue( ward_specular ).asFloatVector();

					MColor diffuseC = getDiffuseComponent(block);
					MColor phongC = getPhongComponent(specularColor, upower, block);
					resultColor[0] = ( diffuseC.r * surfaceColor[0] ) + phongC.r;
					resultColor[1] = ( diffuseC.g * surfaceColor[1] ) + phongC.g;
					resultColor[2] = ( diffuseC.b * surfaceColor[2] ) + phongC.b;
				} break;
		case 7: {//VIEW-CAUSTICS	
					resultColor[0] = resultColor[1] = resultColor[2] = 0;
				} break;
		case 8: {//VIEW-IRRADIANCE			
					resultColor[0] = resultColor[1] = resultColor[2] = 0;
				} break;
		case 9: {//VIEW-GLOBAL		
					resultColor[0] = resultColor[1] = resultColor[2] = 0;
				} break;
		case 10: { //CONSTANT					
					resultColor[0] = surfaceColor[0];
					resultColor[1] = surfaceColor[1];
					resultColor[2] = surfaceColor[2];
				} break;
		case 11: {//JANIN0
					resultColor[0] = resultColor[1] = resultColor[2] = 0;
				} break;
		case 12: {//ID
					resultColor[0] = resultColor[1] = resultColor[2] = 0;
				} break;
		default : break;
	}   

	
    // set ouput color attribute
    MDataHandle outColorHandle = block.outputValue( aOutColor );
    MFloatVector& outColor = outColorHandle.asFloatVector();
    outColor = resultColor;
    outColorHandle.setClean();

    return MS::kSuccess;
}
