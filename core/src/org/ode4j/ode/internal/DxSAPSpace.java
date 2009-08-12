/*************************************************************************
 *                                                                       *
 * Open Dynamics Engine, Copyright (C) 2001-2003 Russell L. Smith.       *
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
package org.ode4j.ode.internal;

import static org.ode4j.ode.internal.Common.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.ode4j.ode.DAABB;
import org.ode4j.ode.DSapSpace;


/**
 *  Sweep and Prune adaptation/tweaks for ODE by Aras Pranckevicius.
 *  Additional work by David Walters
 *  Original code:
 *		OPCODE - Optimized Collision Detection
 *		Copyright (C) 2001 Pierre Terdiman
 *		Homepage: http://www.codercorner.com/Opcode.htm
 *
 *	This version does complete radix sort, not "classical" SAP. So, we
 *	have no temporal coherence, but are able to handle any movement
 *	velocities equally well.
 *
 *  2009-Apr-29
 *  Implementation has been modified for Java port. It now used merge-sort
 *  provided by Java. See doc of <tt>BoxPruning</tt>.
 *  Tilmann Zaeschke 
 */
public class DxSAPSpace extends DxSpace implements DSapSpace {

	// --------------------------------------------------------------------------
	//  SAP space code
	// --------------------------------------------------------------------------

	//--------------------------------------------------------------------------
	// Local Declarations
	//--------------------------------------------------------------------------

	//! A generic couple structure
	private static class Pair
	{
		//		uint32 id0;	//!< First index of the pair
		//		uint32 id1;	//!< Second index of the pair
//		int id0;	//!< First index of the pair
//		int id1;	//!< Second index of the pair
		DxGeom g0;
		DxGeom g1;

		// Default and Value Constructor
		//Pair() {}
		//Pair( uint32 i0, uint32 i1 ) : id0( i0 ), id1( i1 ) {}
//		Pair(int i0, int i1) {
//			id0 = i0;
//			id1 = i1;
//		}
		Pair(DxGeom geom0, DxGeom geom1) {
			g0 = geom0;
			g1 = geom1;
		}
	};

	//--------------------------------------------------------------------------
	// Helpers
	//--------------------------------------------------------------------------

	//	/**
	//	 *	Complete box pruning.
	//	 *  Returns a list of overlapping pairs of boxes, each box of the pair
	//	 *  belongs to the same set.
	//	 *
	//	 *	@param	count	[in] number of boxes.
	//	 *	@param	geoms	[in] geoms of boxes.
	//	 *	@param	pairs	[out] array of overlapping pairs.
	//	 */
	//	void BoxPruning( int count, const dxGeom** geoms, dArray< Pair >& pairs );
	//TODO remove, doc was moved further down.

	//--------------------------------------------------------------------------
	// Implementation Data
	//--------------------------------------------------------------------------

	// We have two lists (arrays of pointers) to dirty and clean
	// geoms. Each geom knows it's index into the corresponding list
	// (see macros above).
	private DArray<DxGeom> DirtyList = new DArray<DxGeom>(); // dirty geoms
	private DArray<DxGeom> GeomList = new DArray<DxGeom>();	// clean geoms

	// For SAP, we ultimately separate "normal" geoms and the ones that have
	// infinite AABBs. No point doing SAP on infinite ones (and it doesn't handle
	// infinite geoms anyway).
	//private dArray<dxGeom> TmpGeomList;	// temporary for normal geoms
	private ArrayList<DxGeom> TmpGeomList = new ArrayList<DxGeom>();	// temporary for normal geoms
	private DArray<DxGeom> TmpInfGeomList = new DArray<DxGeom>();	// temporary for geoms with infinite AABBs

	// Our sorting axes. (X,Z,Y is often best). Stored *2 for minor speedup
	// Axis indices into geom's aabb are: min=idx, max=idx+1
	//	uint32 ax0idx;
	//	uint32 ax1idx;
	//	uint32 ax2idx;
	private int ax0id;
	private int ax1id;
	private int ax2id;

	// pruning position array scratch pad
	// NOTE: this is float not dReal because of the OPCODE radix sorter
	//private dArray< Float > poslist;
	//TZ: TODO remove, not required in this implementation, see BoxPruning(..).
//	private float[] poslist;
//	private RaixSortContext sortContext = new RaixSortContext();
	//};

	// Creation
	public static DxSAPSpace dSweepAndPruneSpaceCreate( DxSpace space, int axisorder ) {
		return new DxSAPSpace( space, axisorder );
	}


	//==============================================================================

	//#define GEOM_ENABLED(g) (((g)->gflags & GEOM_ENABLE_TEST_MASK) == GEOM_ENABLE_TEST_VALUE)
//	private boolean GEOM_ENABLED(dxGeom g) { 
//		return (((g)->gflags & GEOM_ENABLE_TEST_MASK) == GEOM_ENABLE_TEST_VALUE); }
	//
	//// HACK: We abuse 'next' and 'tome' members of dxGeom to store indice into dirty/geom lists.
	//#define GEOM_SET_DIRTY_IDX(g,idx) { (g)->next = (dxGeom*)(size_t)(idx); }
	//#define GEOM_SET_GEOM_IDX(g,idx) { (g)->tome = (dxGeom**)(size_t)(idx); }
	//#define GEOM_GET_DIRTY_IDX(g) ((int)(size_t)(g)->next)
	//#define GEOM_GET_GEOM_IDX(g) ((int)(size_t)(g)->tome)
	//#define GEOM_INVALID_IDX (-1)
	//TODO implement
	/**
	 * As an alternative to the above, be implement separate fields in dxGeom.
	 * This should be faster than having a generic class for SAP and QT-Space info.
	 */
	private void GEOM_SET_DIRTY_IDX(DxGeom g,int idx) { g._sapIdxDirty = idx; }
	private void GEOM_SET_GEOM_IDX(DxGeom g,int idx) { g._sapIdxGeom = idx; }
	private int GEOM_GET_DIRTY_IDX(DxGeom g) { return g._sapIdxDirty; }
	private int GEOM_GET_GEOM_IDX(DxGeom g) { return g._sapIdxGeom; }
	private static final int GEOM_INVALID_IDX = -1;
	
	/**
	 *  A bit of repetitive work - similar to collideAABBs, but doesn't check
	 *  if AABBs intersect (because SAP returns pairs with overlapping AABBs).
	 */
	//static void collideGeomsNoAABBs( dxGeom *g1, dxGeom *g2, void *data, dNearCallback *callback )
	static void collideGeomsNoAABBs( DxGeom g1, DxGeom g2, Object data, DNearCallback callback )
	{
		dIASSERT( (g1._gflags & GEOM_AABB_BAD)==0 );
		dIASSERT( (g2._gflags & GEOM_AABB_BAD)==0 );

		// no contacts if both geoms on the same body, and the body is not 0
		if (g1.body == g2.body && g1.body!=null) return;

		// test if the category and collide bitfields match
		if ( ((g1.category_bits & g2.collide_bits)!=0 ||
				(g2.category_bits & g1.collide_bits)!=0) == false) {
			return;
		}

		DAABB bounds1 = g1._aabb;
		DAABB bounds2 = g2._aabb;

		// check if either object is able to prove that it doesn't intersect the
		// AABB of the other
		if (g1.AABBTest (g2,bounds2) == false) return;
		if (g2.AABBTest (g1,bounds1) == false) return;

		// the objects might actually intersect - call the space callback function
		callback.call (data,g1,g2);
	}


	//dxSAPSpace::dxSAPSpace( dSpaceID _space, int axisorder ) : dxSpace( _space )
	private DxSAPSpace( DxSpace space, int axisorder )// : dxSpace( _space )
	{
		super(space);
		type = dSweepAndPruneSpaceClass;

		// Init AABB to infinity
		//	_aabb[0] = -dInfinity;
		//	_aabb[1] = dInfinity;
		//	_aabb[2] = -dInfinity;
		//	_aabb[3] = dInfinity;
		//	_aabb[4] = -dInfinity;
		//	_aabb[5] = dInfinity;
		_aabb.set(-dInfinity, dInfinity, -dInfinity, dInfinity, -dInfinity, dInfinity);

//		ax0idx = ( ( axisorder ) & 3 ) << 1;
//		ax1idx = ( ( axisorder >> 2 ) & 3 ) << 1;
//		ax2idx = ( ( axisorder >> 4 ) & 3 ) << 1;
		//TZ the new AABB class does not need '<< 1'.
		ax0id = ( ( axisorder ) & 3 );
		ax1id = ( ( axisorder >> 2 ) & 3 );
		ax2id = ( ( axisorder >> 4 ) & 3 );
	}

	//TODO check super class?!?
	//dxSAPSpace::~dxSAPSpace()
	@Override
	public void DESTRUCTOR()
	{
		CHECK_NOT_LOCKED(this);
		if ( cleanup ) {
			// note that destroying each geom will call remove()
			//		for ( ; DirtyList.size()!=0; dGeomDestroy( DirtyList.get( 0 ) ) ) {}
			//		for ( ; GeomList.size()!=0; dGeomDestroy( GeomList.get( 0 ) ) ) {}
			while ( DirtyList.size()!=0 ) DirtyList.get( 0 ).dGeomDestroy( ); 
			while ( GeomList.size()!=0) GeomList.get( 0 ).dGeomDestroy( );

		}
		else {
			// just unhook them
			for ( ; DirtyList.size()!=0; remove( DirtyList.get( 0 ) ) ) {}
			for ( ; GeomList.size()!=0; remove( GeomList.get( 0 ) ) ) {}
		}
		super.DESTRUCTOR();
	}

	//dxGeom* dxSAPSpace::getGeom( int i )
	@Override
	public DxGeom getGeom( int i )
	{
		dUASSERT( i >= 0 && i < count, "index out of range" );
		int dirtySize = DirtyList.size();
		if( i < dirtySize )
			return DirtyList.get(i);
		else
			return GeomList.get(i-dirtySize);
	}

	//void dxSAPSpace::add( dxGeom* g )
	@Override
	void add( DxGeom g )
	{
		CHECK_NOT_LOCKED (this);
		dAASSERT(g);
		dUASSERT(g.parent_space == null && g.getNext() == null, "geom is already in a space");

		g._gflags |= GEOM_DIRTY | GEOM_AABB_BAD;

		// add to dirty list
		GEOM_SET_DIRTY_IDX( g, DirtyList.size() );
		GEOM_SET_GEOM_IDX( g, GEOM_INVALID_IDX );
		DirtyList.push( g );

		g.parent_space = this;
		this.count++;

		dGeomMoved();
	}

	//void dxSAPSpace::remove( dxGeom* g )
	@Override
	void remove( DxGeom g )
	{
		CHECK_NOT_LOCKED(this);
		dAASSERT(g);
		dUASSERT(g.parent_space == this,"object is not in this space");

		// remove
		int dirtyIdx = GEOM_GET_DIRTY_IDX(g);
		int geomIdx = GEOM_GET_GEOM_IDX(g);
		// must be in one list, not in both
		dUASSERT(
				dirtyIdx==GEOM_INVALID_IDX && geomIdx>=0 && geomIdx<GeomList.size() ||
				geomIdx==GEOM_INVALID_IDX && dirtyIdx>=0 && dirtyIdx<DirtyList.size(),
		"geom indices messed up" );
		if( dirtyIdx != GEOM_INVALID_IDX ) {
			// we're in dirty list, remove
			int dirtySize = DirtyList.size();
			DxGeom lastG = DirtyList.get(dirtySize-1);
			DirtyList.set(dirtyIdx, lastG);
			GEOM_SET_DIRTY_IDX(lastG,dirtyIdx);
			GEOM_SET_DIRTY_IDX(g,GEOM_INVALID_IDX);
			DirtyList.setSize( dirtySize-1 );
		} else {
			// we're in geom list, remove
			int geomSize = GeomList.size();
			DxGeom lastG = GeomList.get(geomSize-1);
			GeomList.set(geomIdx, lastG);
			GEOM_SET_GEOM_IDX(lastG,geomIdx);
			GEOM_SET_GEOM_IDX(g,GEOM_INVALID_IDX);
			GeomList.setSize( geomSize-1 );
		}
		count--;

		// safeguard
		g.parent_space = null;

		// the bounding box of this space (and that of all the parents) may have
		// changed as a consequence of the removal.
		dGeomMoved();
	}

	//void dxSAPSpace::dirty( dxGeom* g )
	@Override
	void dirty( DxGeom g )
	{
		dAASSERT(g);
		dUASSERT(g.parent_space == this,"object is not in this space");

		// check if already dirtied
		int dirtyIdx = GEOM_GET_DIRTY_IDX(g);
		if( dirtyIdx != GEOM_INVALID_IDX )
			return;

		int geomIdx = GEOM_GET_GEOM_IDX(g);
		dUASSERT( geomIdx>=0 && geomIdx<GeomList.size(), "geom indices messed up" );

		// remove from geom list, place last in place of this
		int geomSize = GeomList.size();
		DxGeom lastG = GeomList.get(geomSize-1);
		GeomList.set(geomIdx, lastG);
		GEOM_SET_GEOM_IDX(lastG,geomIdx);
		//GeomList.setSize( geomSize-1 );
		GeomList.remove(geomSize-1);

		// add to dirty list
		GEOM_SET_GEOM_IDX( g, GEOM_INVALID_IDX );
		GEOM_SET_DIRTY_IDX( g, DirtyList.size() );
		DirtyList.push( g );
	}

	//void dxSAPSpace::computeAABB()
	@Override
	void computeAABB()
	{
		// TODO?
	}

	//void dxSAPSpace::cleanGeoms()
	@Override
	public void cleanGeoms()
	{
		int dirtySize = DirtyList.size();
		if( dirtySize==0 )
			return;

		// compute the AABBs of all dirty geoms, clear the dirty flags,
		// remove from dirty list, place into geom list
		lock_count++;

		int geomSize = GeomList.size();
		GeomList.setSize( geomSize + dirtySize ); // ensure space in geom list

		for( int i = 0; i < dirtySize; ++i ) {
			DxGeom g = DirtyList.get(i);
			if( g instanceof DxSpace ) {//IS_SPACE(g) ) {
				((DxSpace)g).cleanGeoms();
			}
			g.recomputeAABB();
			g._gflags &= (~(GEOM_DIRTY|GEOM_AABB_BAD));
			// remove from dirty list, add to geom list
			GEOM_SET_DIRTY_IDX( g, GEOM_INVALID_IDX );
			GEOM_SET_GEOM_IDX( g, geomSize + i );
			GeomList.set(geomSize+i, g);
		}
		// clear dirty list
		DirtyList.setSize( 0 );

		lock_count--;
	}

	//void dxSAPSpace::collide( void *data, dNearCallback *callback )
	@Override
	public void collide( Object data, DNearCallback callback )
	{
		dAASSERT (callback);

		lock_count++;

		cleanGeoms();

		// by now all geoms are in GeomList, and DirtyList must be empty
		int geom_count = GeomList.size();
		dUASSERT( geom_count == count, "geom counts messed up" );

		// separate all ENABLED geoms into infinite AABBs and normal AABBs
		TmpGeomList.clear();//setSize(0);
		TmpInfGeomList.setSize(0);
		int axis0max = ax0id;// + 1;
		for( int i = 0; i < geom_count; ++i ) {
			DxGeom g = GeomList.get(i);
			if( !GEOM_ENABLED(g) ) // skip disabled ones
				continue;
			final double amax = g._aabb.getMax(axis0max);
			if( amax == dInfinity ) // HACK? probably not...
				TmpInfGeomList.push( g );
			else
				TmpGeomList.add( g );//push( g );
		}

		// do SAP on normal AABBs
		ArrayList< Pair > overlapBoxes = new ArrayList<Pair>();
		int tmp_geom_count = TmpGeomList.size();
		if ( tmp_geom_count > 0 )
		{
			// Size the poslist (+1 for infinity end cap)
			//poslist.setSize( tmp_geom_count + 1 );
			//TODO TZ not used at the moment
			//poslist = new float[ tmp_geom_count + 1 ];
			//poslist = new float[ tmp_geom_count ];

			// Generate a list of overlapping boxes
			//BoxPruning( tmp_geom_count, (final dxGeom**)TmpGeomList.data(), overlapBoxes );
			BoxPruning( tmp_geom_count, TmpGeomList, overlapBoxes );
		}

		// collide overlapping
		int overlapCount = overlapBoxes.size();
		for( int j = 0; j < overlapCount; ++j )
		{
			final Pair pair = overlapBoxes.get( j );
			//TODO clean up
//			dxGeom g1 = TmpGeomList.get( pair.id0 );
//			dxGeom g2 = TmpGeomList.get( pair.id1 );
//			collideGeomsNoAABBs( g1, g2, data, callback );
			collideGeomsNoAABBs( pair.g0, pair.g1, data, callback );
		}

		int infSize = TmpInfGeomList.size();
		int normSize = TmpGeomList.size();
		int m, n;

		for ( m = 0; m < infSize; ++m )
		{
			DxGeom g1 = TmpInfGeomList.get( m );

			// collide infinite ones
			for( n = m+1; n < infSize; ++n ) {
				DxGeom g2 = TmpInfGeomList.get(n);
				collideGeomsNoAABBs( g1, g2, data, callback );
			}

			// collide infinite ones with normal ones
			for( n = 0; n < normSize; ++n ) {
				DxGeom g2 = TmpGeomList.get(n);
				collideGeomsNoAABBs( g1, g2, data, callback );
			}
		}

		lock_count--;
	}

	//void dxSAPSpace::collide2( void *data, dxGeom *geom, dNearCallback *callback )
	@Override
	void collide2( Object data, DxGeom geom, DNearCallback callback )
	{
		dAASSERT (geom!=null && callback!=null);

		// TODO: This is just a simple N^2 implementation

		lock_count++;

		cleanGeoms();
		geom.recomputeAABB();

		// intersect bounding boxes
		int geom_count = GeomList.size();
		for ( int i = 0; i < geom_count; ++i ) {
			DxGeom g = GeomList.get(i);
			if ( GEOM_ENABLED(g) )
				collideAABBs (g,geom,data,callback);
		}

		lock_count--;
	}

	
	private class GeomComparator implements Comparator<DxGeom> {
		@Override
		public int compare(DxGeom arg0, DxGeom arg1) {
			double a0 = arg0._aabb.getMin(ax0id);
			double a1 = arg1._aabb.getMin(ax0id);
			return a1 > a0 ? -1 : (a1 < a0 ? 1 : 0);
		}
	}
	
	/**
	 *	Complete box pruning.
	 *  Returns a list of overlapping pairs of boxes, each box of the pair
	 *  belongs to the same set.
	 *  
	 *  TZ: This is a new implementation for Java. It uses the Java internal 
	 *  merge-sort instead of a custom radix-sort. The first is between 
	 *  O(N) and O(N*log(N)), the second is always O(N*log(N)), so not much lost.
	 *  This greatly simplifies the code.
	 *
	 *	@param	count	[in] number of boxes.
	 *	@param	geoms	[in] geoms of boxes.
	 *	@param	pairs	[out] array of overlapping pairs.
	 */
	//void dxSAPSpace::BoxPruning( int count, const dxGeom** geoms, dArray< Pair >& pairs )
	void BoxPruning( int count, final ArrayList<DxGeom> geoms, ArrayList< Pair > pairs )
	{
		// 1) Build main list using the primary axis
		//  NOTE: uses floats instead of dReals because that's what radix sort wants
		//TZ: not required in this implementation

		// 2) Sort the list
		ArrayList<DxGeom> buffer = new ArrayList<DxGeom>(geoms);
		Collections.sort(buffer, new GeomComparator());

		// 3) Prune the list
		for (int i = 0; i < buffer.size(); i++) {
			DxGeom g0 = buffer.get(i);
			DAABB aabb0 = g0._aabb;
			final double idx0ax0max = aabb0.getMax(ax0id);//(ax0idx+1);
			for (int j = i+1; j < buffer.size(); j++) {
				DxGeom g1 = buffer.get(j);
				if (g1._aabb.getMin(ax0id) > idx0ax0max) {
					//This and following elements can not intersect with g1.
					break;
				}
//				if ( aabb0.get(ax1idx+1) >= g1._aabb.get(ax1idx)) 
//					if (g1._aabb.get(ax1idx+1) >= aabb0.get(ax1idx) )
//						if ( aabb0.get(ax2idx+1) >= g1._aabb.get(ax2idx))
//							if (g1._aabb.get(ax2idx+1) >= aabb0.get(ax2idx) )
				if ( aabb0.getMax(ax1id) >= g1._aabb.getMin(ax1id)) 
					if (g1._aabb.getMax(ax1id) >= aabb0.getMin(ax1id) )
						if ( aabb0.getMax(ax2id) >= g1._aabb.getMin(ax2id))
							if (g1._aabb.getMax(ax2id) >= aabb0.getMin(ax2id) )
								pairs.add(new Pair(g0, g1));
			}
		}
	}

	//Commented out, see above (TZ)
//	/**
//	 *	Complete box pruning.
//	 *  Returns a list of overlapping pairs of boxes, each box of the pair
//	 *  belongs to the same set.
//	 *
//	 *	@param	count	[in] number of boxes.
//	 *	@param	geoms	[in] geoms of boxes.
//	 *	@param	pairs	[out] array of overlapping pairs.
//	 */
//	//void dxSAPSpace::BoxPruning( int count, const dxGeom** geoms, dArray< Pair >& pairs )
//	void BoxPruning( int count, final ArrayList<dxGeom> geoms, ArrayList< Pair > pairs )
//	{
//		// 1) Build main list using the primary axis
//		//  NOTE: uses floats instead of dReals because that's what radix sort wants
//		for( int i = 0; i < count; ++i )
//			poslist[i] = (float)TmpGeomList.get(i)._aabb.get( ax0idx );
//		//TODO TZ poslist[count++] = Float.MAX_VALUE;//FLT_MAX;
//
//		// 2) Sort the list
//		//final uint32* Sorted = sortContext.RadixSort( poslist.data(), count );
//		//final IntArray Sorted = new IntArray( sortContext.RadixSort( poslist, count ) );
//		//TODO
//		//TODO
//		//TODO
//		ArrayList<dxGeom> buffer = new ArrayList<dxGeom>(geoms);
//		Collections.sort(buffer, new GeomComparator());
//		//final IntArray Sorted = new IntArray( Arrays.sort(poslist) );
//		final IntArray Sorted = new IntArray(poslist.length);
//		for (int i = 0; i < geoms.size(); i++) { //Sorted.setAt(i, i); // TODO optimize
//			dxGeom g1 = geoms.get(i);
//	//		poslist[i] = (float) g1._aabb.get(ax0idx); //TODO really?
//			boolean found = false;
//			for (int j = 0; j < buffer.size(); j++) {
//				if (g1 == buffer.get(j)) {
//					Sorted.setAt(buffer.size()-i-1, j);
//					//Sorted.setAt(i, j);
//					found = true;
//					break;
//				}
//			}
//			if (!found) throw new RuntimeException("i="+i);
//		}
//		//RESULT SO FAR
//		//- Sorting is +- correct, geoms are ordered after aabb[0] from - to +
//		//- poslist does not get modified
//		//- poslist[Sorted] gives elements in correct order
//
//		// 3) Prune the list
//		//	final uint32* const LastSorted = Sorted + count;
//		//	final uint32* RunningAddress = Sorted;
////		final IntArray LastSorted = new IntArray(Sorted, count);
//		final IntArray RunningAddress = new IntArray(Sorted);
//		//while ( RunningAddress < LastSorted && Sorted < LastSorted )
//		//while ( RunningAddress.getAt0() < LastSorted.getAt0() && Sorted.getAt0() < LastSorted.getAt0() )
//		while (RunningAddress.size() > 0 && Sorted.size() > 0)
//		{
//			Pair IndexPair = new Pair();  //TODO push does memcpy !?!?!?!!!!
//			//IndexPair.id0 = *Sorted++;
//			IndexPair.id0 = Sorted.getAt0();
//			Sorted.inc();
//
//			// empty, this loop just advances RunningAddress
//			//while ( poslist[*RunningAddress++] < poslist[IndexPair.id0] ) {}
//			while ( poslist[RunningAddress.getAt0()] < poslist[IndexPair.id0] ) {
//				RunningAddress.inc();
//			}
//			//TZ additional inc to point to failed position
//			RunningAddress.inc();
//
//			//End of list not reached?
//			//if ( RunningAddress.getAt0() < LastSorted.getAt0() )
//			if (RunningAddress.size() > 0)
//			{
//				//final uint32* RunningAddress2 = RunningAddress;
//				final IntArray RunningAddress2 = new IntArray(RunningAddress);
//
//				final double idx0ax0max = geoms.get(IndexPair.id0)._aabb.get(ax0idx+1);
//				final double idx0ax1max = geoms.get(IndexPair.id0)._aabb.get(ax1idx+1);
//				final double idx0ax2max = geoms.get(IndexPair.id0)._aabb.get(ax2idx+1);
//
//				//while ( poslist[ IndexPair.id1 = *RunningAddress2++ ] <= idx0ax0max )
//				while ( true )
//				{
//					IndexPair.id1 = RunningAddress2.getAt0();
//					RunningAddress2.inc();
//					if (! (poslist[IndexPair.id1] <= idx0ax0max) || RunningAddress2.size()<0) {
//						break;
//					}
//					
//					final dVector6 aabb0 = geoms.get( IndexPair.id0 )._aabb;
//					final dVector6 aabb1 = geoms.get( IndexPair.id1 )._aabb;
//
//					// Intersection?
//					if ( idx0ax1max >= aabb1.get(ax1idx) && aabb1.get(ax1idx+1) >= aabb0.get(ax1idx) )
//						if ( idx0ax2max >= aabb1.get(ax2idx) && aabb1.get(ax2idx+1) >= aabb0.get(ax2idx) )
//						{
//							System.out.println("P: " + IndexPair.id0 + " / " + IndexPair.id1 );
//							pairs.add( IndexPair );
//						}
//				}
//			}
//
//		}; // while ( RunningAddress < LastSorted && Sorted < LastSorted )
//	}


	//==============================================================================

	//------------------------------------------------------------------------------
	// Radix Sort
	//------------------------------------------------------------------------------

	// --------------------------------------------------------------------------
	//  Radix Sort Context
	// --------------------------------------------------------------------------

	//TZ removed. May be added in future again.
}