package constructor;

public class ConstructorOverloadingMain {
    public static void main(String[] args) {
        ConstructorOverloading constructorOverloading1 = new ConstructorOverloading("value1", "value2");
        ConstructorOverloading constructorOverloading2 = new ConstructorOverloading("value1");

        System.out.println(constructorOverloading1);
        System.out.println(constructorOverloading2);
    }
}
