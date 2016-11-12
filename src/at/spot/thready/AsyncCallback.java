package at.spot.thready;

@FunctionalInterface
public interface AsyncCallback<RETURNVALUE> {
	void callback(RETURNVALUE returnValue);
}
