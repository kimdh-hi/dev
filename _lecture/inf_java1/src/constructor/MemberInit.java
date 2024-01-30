package constructor;

public class MemberInit {
    String name;
    int age;

    public MemberInit(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // 클래스 내 생성자가 없는 경우에만 컴파일러에 의해 기본 생성자 자동 생성
    public MemberInit() { }

    public void init(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public String toString() {
        return "MemberInit{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
