package visibility;

public class OuterClassSampleClass {

    public class InnerClassSampleClass {
        private void func() { }
    }

    void func() {
        new InnerClassSampleClass().func();
    }
}
