package ref;

// primitiveType, referenceType 메서드 내 값 변경 차이
public class Ref1 {
    public static void main(String[] args) {
        int a = 10;
        System.out.println("a="+a); // 10
        update(a);
        System.out.println("a="+a); // 10

        Data data = new Data();
        data.data = 10;
        System.out.println("data=" + data.data); // 10
        update(data);
        System.out.println("data=" + data.data); // 20

    }

    static void update(int b) {
        b = 20;
    }

    static void update(Data data) {
        data.data = 20;
    }
}
