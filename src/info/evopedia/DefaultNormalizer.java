package info.evopedia;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Locale;

import android.app.ListActivity;
import android.content.Context;
import android.util.Log;

public class DefaultNormalizer implements StringNormalizer {
	private HashMap<Character, Character> normalizationMap;

	public DefaultNormalizer(InputStream translationTable) {
		initializeNormalizationMap(translationTable);
	}

	private void initializeNormalizationMap(InputStream translationTable) {
		normalizationMap = new HashMap<Character, Character>();

		try {
			DataInputStream s = new DataInputStream(translationTable);

			while (true) {
				/* the table actually contains four-byte unicode characters, but for
				 * all entries, the first two bytes are zero */
				s.readChar();
				char k = s.readChar();
				s.readChar();
				char v = s.readChar();
				normalizationMap.put(k, v);
			}
		} catch (EOFException eofexc) {
		} catch (IOException ioexc) {
			Log.e("Normalizer", "IO Error building normalization map", ioexc);
		}
	}

	@Override
	public String normalize(String str) {
		StringBuilder s = new StringBuilder(str.length());

		str = str.toLowerCase(Locale.US);
		for (int i = 0; i < str.length(); i ++) {
			char c = str.charAt(i);
			Character ct = normalizationMap.get(c);
			if (ct == null) {
				s.append('_');
			} else {
				s.append(ct);
			}
		}
		return s.toString();
	}
}