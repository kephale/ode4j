/*************************************************************************
 *                                                                       *
 * Open Dynamics Engine, Copyright (C) 2001,2002 Russell L. Smith.       *
 * All rights reserved.  Email: russ@q12.org   Web: www.q12.org          *
 *                                                                       *
 * This library is free software; you can redistribute it and/or         *
 * modify it under the terms of EITHER:                                  *
 *   (1) The GNU Lesser General Public License as published by the Free  *
 *       Software Foundation; either version 2.1 of the License, or (at  *
 *       your option) any later version. The text of the GNU Lesser      *
 *       General Public License is included with this library in the     *
 *       file LICENSE.TXT.                                               *
 *   (2) The BSD-style license that is included with this library in     *
 *       the file LICENSE-BSD.TXT.                                       *
 *                                                                       *
 * This library is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files    *
 * LICENSE.TXT and LICENSE-BSD.TXT for more details.                     *
 *                                                                       *
 *************************************************************************/
package org.ode4j.ode;

import org.cpp4j.java.RefDouble;

public interface DCylinder extends DGeom {

	public void setParams (double radius, double length);
	public void getParams (RefDouble radius, RefDouble length);
	public double getRadius ();
	public double getLength ();
	

//	  // intentionally undefined, don't use these
//	  dCylinder (dCylinder &);
//	  void operator= (dCylinder &);
//
//	public:
//	  dCylinder() { }
//	  dCylinder (dSpaceID space, dReal radius, dReal length)
//	    { _id = dCreateCylinder (space,radius,length); }
//
//	  void create (dSpaceID space, dReal radius, dReal length) {
//	    if (_id) dGeomDestroy (_id);
//	    _id = dCreateCylinder (space,radius,length);
//	  }
//
//	  void setParams (dReal radius, dReal length)
//	    { dGeomCylinderSetParams (_id, radius, length); }
//	  void getParams (dReal *radius, dReal *length) const
//	    { dGeomCylinderGetParams (_id,radius,length); }
}
