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
    }
}
