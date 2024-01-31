package constructor;

public class ConstructorOverloading {
    String value1;
    String value2;

    ConstructorOverloading(String value1, String value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    ConstructorOverloading(String value1) {
//        System.out.println("..."); // 생성자 첫줄에 다른 코드가 있는 경우 컴파일 에러
        this(value1, "default"); // 생성자 내 첫줄에만 사용 가능
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
