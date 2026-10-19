package fonnymunkey.gibbed.util;

public class Triple<L,M,R> {
	public L left;
	public M middle;
	public R right;
	
	public Triple(L left, M middle, R right) {
		this.left = left;
		this.middle = middle;
		this.right = right;
	}
}