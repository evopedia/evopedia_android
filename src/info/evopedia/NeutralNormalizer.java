package info.evopedia;

public class NeutralNormalizer implements StringNormalizer {

	public NeutralNormalizer() {
	}

	@Override
	public String normalize(String str) {
		return str;
	}
}
