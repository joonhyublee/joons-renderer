////////////////////////////////////////////////////////////////////////////
//	copyright	: (C) 2006 by Mad Crew 
//	Email		: fredrik (at) madcrew (dot) se
//	Website 	: http://www.madcrew.se
//	Created		:2006-11-03
////////////////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////////
//                                                                        	//
//   This program is free software; you can redistribute it and/or modify	//
//   it under the terms of the GNU General Public License as published by 	//
//   the Free Software Foundation; either version 2 of the License, or    	//
//   (at your option) any later version.                                  	//
//                                                                       	//
////////////////////////////////////////////////////////////////////////////

#include "skylight.h"
#include "sunskyConstants.h"

SunSky::SunSky() {}

SunSky::SunSky( MVector pos, double size, double  turb )
{
  position = pos;
  turbidity = turb;
  initialize();
}

void SunSky::initialize()
{

  position.normalize();
  phi = atan2( position.x, position.z );
  theta = acos( position.y );

  double theta2 = theta * theta;
  double theta3 = theta * theta2;
  double T = turbidity;
  double T2 = T * T;
  double chi = (4.0/9.0 - T / 120.0) * (M_PI - 2 * theta);

  zenith_Y = (4.0453 * T - 4.9710) * tan( chi ) - 0.2155 * T + 2.4192;
  zenith_Y *= 1000;
  zenith_x = ( 0.00165*theta3 - 0.00374*theta2 + 0.00208*theta + 0      ) * T2 +
             (-0.02902*theta3 + 0.06377*theta2 - 0.03202*theta + 0.00394) * T  +
             ( 0.11693*theta3 - 0.21196*theta2 + 0.06052*theta + 0.25885);

  zenith_y = ( 0.00275*theta3 - 0.00610*theta2 + 0.00316*theta  + 0      ) * T2 +
             (-0.04214*theta3 + 0.08970*theta2 - 0.04153*theta  + 0.00515) * T  +
             ( 0.15346*theta3 - 0.26756*theta2 + 0.06669*theta  + 0.26688);

  perez_Y[0] =  0.17872 * T  - 1.46303;
  perez_Y[1] = -0.35540 * T  + 0.42749;
  perez_Y[2] = -0.02266 * T  + 5.32505;
  perez_Y[3] =  0.12064 * T  - 2.57705;
  perez_Y[4] = -0.06696 * T  + 0.37027;

  perez_x[0] = -0.01925 * T  - 0.25922;
  perez_x[1] = -0.06651 * T  + 0.00081;
  perez_x[2] = -0.00041 * T  + 0.21247;
  perez_x[3] = -0.06409 * T  - 0.89887;
  perez_x[4] = -0.00325 * T  + 0.04517;

  perez_y[0] = -0.01669 * T  - 0.26078;
  perez_y[1] = -0.09495 * T  + 0.00921;
  perez_y[2] = -0.00792 * T  + 0.21023;
  perez_y[3] = -0.04405 * T  - 1.65369;
  perez_y[4] = -0.01092 * T  + 0.05291;
}

//-----------------------------------------------------------------------------


MColor SunSky::gammaCorrect(MColor& color, float gamma )
{
	color.r = pow(color.r, (float ) 1.0/gamma);
	color.g = pow(color.g, (float ) 1.0/gamma);
	color.b = pow(color.b, (float ) 1.0/gamma);
	return color;
}


//-----------------------------------------------------------------------------
double SunSky::perezFunction(const double *coeff, double thetaV, double gamma, double Yz ) const
{
  double den, num;
  double cosTheta = cos( theta );

  den = (1 + coeff[0] * exp( coeff[1] )) *
        (1 + coeff[2] * exp( coeff[3] * theta ) + coeff[4] * cosTheta * cosTheta);

  num = (1 + coeff[0] * exp( coeff[1] / cos(thetaV) )) *
        (1 + coeff[2] * exp( coeff[3] * gamma ) + coeff[4] * cos(gamma) * cos(gamma));

  return Yz * num / den;
}



//-----------------------------------------------------------------------------
MColor SunSky::getSkyColor(const MVector& direction ) const
{
  double thetaV;        // the angle about surface normal
  double gamma;         // the angle between direction and sun
  double x,y,Y;

  //gamma = mi_vector_dot( &direction, &position );
  gamma =	((direction.x*position.x) +	(direction.y*position.y) + (direction.z*position.z));
  //if( gamma < 0 )
  //  return Colors.black;

  gamma = acos( gamma );
  thetaV = acos( direction.y );
  x = perezFunction( perez_x, thetaV, gamma, zenith_x );
  y = perezFunction( perez_y, thetaV, gamma, zenith_y );
  Y = perezFunction( perez_Y, thetaV, gamma, zenith_Y );

  return toRGB( x, y, Y );
}


//-----------------------------------------------------------------------------
MColor SunSky::toRGB(double x, double y, double Ys ) const
{
  MColor result;
  double X, Y, Z;
  double R, G, B;
  double Yarea;
  double amp;
  double M1 = (-1.3515 - 1.7703*x +  5.9114*y)/(0.0241 + 0.2562*x - 0.7341*y);
  double M2 = ( 0.03   -31.4424*x + 30.0717*y)/(0.0241 + 0.2562*x - 0.7341*y);
  int Sidx, CIEidx;

  Sidx = (380 - 300) / 10;    // match wavelengths - S starts at 300, CIE at 380
  CIEidx = 0;
  X = Y = Z = 0;
  Yarea = 0;
  for( int i=380; i < 780; i+=10 )
  {
    amp = S0[Sidx] + M1 * S1[Sidx] + M2 * S2[Sidx];
    X += amp * CIE_X[CIEidx];
    Y += amp * CIE_Y[CIEidx];
    Z += amp * CIE_Z[CIEidx];
    Yarea += CIE_Y[CIEidx];
    Sidx++;
    CIEidx += 2;
  }

  // scale luminance
  Ys /= 4000;
  X *= Ys / Y;
  Z *= Ys / Y;
  Y = Ys;

/*  X = x / y * Ys;
  Y = Ys;
  Z = ( 1 - x - y ) / y * Ys;
*/

  // convert to RGB
  R =  1.967 * X - 0.548 * Y - 0.297 * Z;
  G = -0.955 * X + 1.938 * Y - 0.027 * Z;
  B =  0.064 * X - 0.130 * Y + 0.982 * Z;

//  if( R < 0 || G < 0 || B < 0 )
//    return Colors.black;
//  else
  if( R < 0 ) R = 0;
  if( G < 0 ) G = 0;
  if( B < 0 ) B = 0;

  result.r = R;
  result.g = G;
  result.b = B;
    return result;
}
