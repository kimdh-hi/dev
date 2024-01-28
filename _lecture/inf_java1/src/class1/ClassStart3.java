package class1;

public class ClassStart3 {
    public static void main(String[] args) {
        Student student1 = new Student();
        student1.name = "kim";
        student1.age = 28;
        student1.grade = 100;

        Student student2 = new Student();
        student2.name = "lee";
        student2.age = 25;
        student2.grade = 95;

        System.out.println(student1);
        System.out.println(student2);

        Student[] students = new Student[2];
        System.out.println(students[0]); // 초기화하지 않은 경우 nul
        students[0] = student1;
        students[1] = student2;

        System.out.println("student1 name: " + student1.name);
        students[0].name = "updatedKimName"; // 같은 참조값을 가지므로 student1 도 갱신
        System.out.println("student1 name: " + student1.name);

    }
}
