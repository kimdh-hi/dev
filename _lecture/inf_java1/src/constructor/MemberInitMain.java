package constructor;

public class MemberInitMain {
    public static void main(String[] args) {
        // 클래스 내 생성자가 없는 경우에만 컴파일러에 의해 기본 생성자 자동 생성
        MemberInit memberInit1 = new MemberInit();
        memberInit1.name = "kim";
        memberInit1.age = 28;
        System.out.println(memberInit1);

        MemberInit memberInit2 = new MemberInit();
        memberInit2.init("kim", 28);
        System.out.println(memberInit2);

        MemberInit memberInit = new MemberInit("kim", 29);
        System.out.println(memberInit);
    }
}
