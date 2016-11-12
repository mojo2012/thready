package at.spot.thready;

@FunctionalInterface
interface AsyncRunnable<RETURNVALUE, PARAMTERTYPE> {
	RETURNVALUE doJob(PARAMTERTYPE actonArgument);
}