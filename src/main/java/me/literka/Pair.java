package me.literka;

public class Pair<F, S> {

	private F first;
	private S second;

	public Pair(final F first, final S second) {
		this.first = first;
		this.second = second;
	}

	public static <F, S> Pair<F, S> of(F first, S second) {
		return new Pair<>(first, second);
	}

	public Pair<F, S> setFirst(final F first) {
		this.first = first;
		return this;
	}

	public Pair<F, S> setSecond(final S second) {
		this.second = second;
		return this;
	}

	public F getFirst() {
		return this.first;
	}

	public S getSecond() {
		return this.second;
	}
}