package com.github.davidmoten.rtree3d;

public final class Point implements Geometry {

	private final float x, y, z;
	
	Point(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public float x() {
		return x;
	}

	public float y() {
		return y;
	}
	
	public float z() {
		return z;
	}

	@Override
	public float distance(Box box) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Box mbb() {
		return Box.create(x, y, z, x, y, x);
	}

	@Override
	public boolean intersects(Box r) {
		// TODO Auto-generated method stub
		return false;
	}

}
