package info.evopedia;

import java.util.ArrayList;
import java.util.Iterator;

public class MergingTitleIterator implements Iterator<Title> {
	private ArrayList<TitleIterator> iterators;

	public MergingTitleIterator(ArrayList<TitleIterator> iterators) {
		this.iterators = iterators;
	}

	@Override
	public boolean hasNext() {
		for (TitleIterator it : iterators) {
			if (it.hasNext()) return true;
		}
		return false;
	}

	@Override
	public Title next() {
		Title n = null;
		TitleIterator nit = null;

		for (TitleIterator it : iterators) {
		    if (!it.hasNext())
		        continue;
			Title t = it.peekNext();
			if (n == null || t.compareTo(n) < 0) {
				n = t;
				nit = it;
			}
		}

		if (nit != null)
			nit.next();

		return n;
	}

	@Override
	public void remove() {
	}
}
