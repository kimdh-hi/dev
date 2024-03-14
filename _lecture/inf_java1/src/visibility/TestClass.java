package visibility;

public class TestClass {

    private ProtectedSampleClass protectedSampleClass;

    private void func() {
        protectedSampleClass.func();
    }
}
