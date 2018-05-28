////////////////////////////////////////////////////////////////////////////
//	copyright	: (C) 2006 by Mad Crew 
//	Email		: fredrik (at) madcrew (dot) se
//	Website 	: http://www.madcrew.se
//	Created		: 2006-11-03
////////////////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////////
//                                                                        	//
//   This program is free software; you can redistribute it and/or modify	//
//   it under the terms of the GNU General Public License as published by 	//
//   the Free Software Foundation; either version 2 of the License, or    	//
//   (at your option) any later version.                                  	//
//                                                                       	//
////////////////////////////////////////////////////////////////////////////


#ifndef SUNSKY_H
#define SUNSKY_H

#include <maya/MColor.h>
#include <maya/MVector.h>
#include <cmath>
//=============================================================================
// SunSky
class SunSky
{
public:
  SunSky();
  SunSky( MVector position, double size, double turbidity );
  // [in]  position - the location of the sun.
  // [in]  angle    - the number of degrees of arc occupied by the sun.
  // [in]  turbidity- is the ratio of the optical thickness of the haze and
  //       molecules to haze. Turbidity = (Tm + Th) / Tm. It is the fraction of
  //       scattering due to haze as opposed to molecules. 2-6 are good for clear
  //       days

  MColor getSkyColor(const MVector& direction ) const;
    // returns the color of the sky in a particular direction

  void setTurbidity( double t ) { turbidity = t; }
  void setPosition( MVector pos ) { position = pos; }
  void initialize(); 
  MColor gammaCorrect( MColor& color, float gamma );

private:
  double    perez_x[5], // coefficients for the perez functions
            perez_y[5],
            perez_Y[5];
  double    zenith_x,   // chromaticity at the zenith
            zenith_y,
            zenith_Y;   // in cd/m^2
  MVector  position;   // normalized MVector pointing to the sun
  double    theta, phi; // spherical coordinates of the sun position
  double    turbidity;  // turbitity of the atmosphere


  SunSky( const SunSky& );
    // copy constructor - unimplemented

  SunSky& operator=( const SunSky& );
    // assigment - unimplemented

  double perezFunction(const double *coeff, double thetaV, double gamma, double Yz ) const;
  MColor toRGB(double x, double y, double Ys ) const;
};

#endif /* SUNSKY_H */
