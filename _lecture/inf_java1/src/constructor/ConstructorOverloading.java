package constructor;

public class ConstructorOverloading {
    String value1;
    String value2;

    ConstructorOverloading(String value1, String value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    ConstructorOverloading(String value1) {
        this(value1, "default");
//        this.value1 = value1;
//        this.value2 = "default";
    }

    @Override
    public String toString() {
        return "ConstructorOverloading{" +
                "value1='" + value1 + '\'' +
                ", value2='" + value2 + '\'' +
                '}';
    }
}
