package class1;

public class ClassStart4 {
    public static void main(String[] args) {
        Student student1 = new Student();
        student1.name = "kim";
        student1.age = 28;
        student1.grade = 100;

        Student student2 = new Student();
        student2.name = "lee";
        student2.age = 25;
        student2.grade = 95;

        Student[] students = new Student[]{student1, student2};
        for (int i = 0; i < students.length; i++) {
            System.out.println("name: " + students[i].name);
        }

        for (Student student: students) {
            System.out.println("name: " + student.name);
        }
    }
}
